/*
 *     Copyright (C) 2026  Mohammad Syuhada
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.swordfish.libretrodroid

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.view.Surface

/**
 * A second display's [Surface], drawn from the GL thread's own context after each step: the frame's secondary regions
 * ([LibretroDroid.renderSecondary]) go to an EGL window surface made for it, then the main surface is made current again.
 * GL thread only.
 */
internal class SecondaryOutput {
    private var surface: Surface? = null
    private var width = 0
    private var height = 0

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var swapIntervalSet = false

    // Set when the surface went bad (or could not be made): nothing is drawn until a surface is set again.
    private var dropped = false

    /** A new target; null tears down. A different [surface] drops the old EGL surface here, on the GL thread. */
    fun setSurface(surface: Surface?, width: Int, height: Int) {
        if (surface !== this.surface) destroyEglSurface()
        this.surface = surface
        this.width = width
        this.height = height
        dropped = false
    }

    /**
     * The GL context was re-created (GLSurfaceView terminated the display with the old one): the EGL surface went with
     * it, or is destroyed here if it somehow survived. Re-created on the next [draw].
     */
    fun contextLost() {
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            val size = IntArray(1)
            val alive = eglDisplay != EGL14.EGL_NO_DISPLAY &&
                EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_WIDTH, size, 0) &&
                eglSurface != EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW) &&
                eglSurface != EGL14.eglGetCurrentSurface(EGL14.EGL_READ)
            if (alive) EGL14.eglDestroySurface(eglDisplay, eglSurface) else EGL14.eglGetError()   // clear the query's error
        }
        forgetEglSurface()
        dropped = false
    }

    /** Draws one frame on the second surface, if one is set; the main surface is current again afterwards. */
    fun draw() {
        val target = surface ?: return
        if (dropped || width <= 0 || height <= 0) return

        val display = EGL14.eglGetCurrentDisplay()
        val context = EGL14.eglGetCurrentContext()
        val mainDraw = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
        val mainRead = EGL14.eglGetCurrentSurface(EGL14.EGL_READ)
        if (display == EGL14.EGL_NO_DISPLAY || context == EGL14.EGL_NO_CONTEXT) return

        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            eglSurface = createWindowSurface(display, context, target) ?: run {
                dropped = true
                return
            }
            eglDisplay = display
            swapIntervalSet = false
        }

        if (!EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) {
            onEglError("eglMakeCurrent", drop = true)
            restore(display, mainDraw, mainRead, context)
            return
        }
        try {
            if (!swapIntervalSet) {
                // Never wait for the second display's vsync: the main surface paces the emulation.
                EGL14.eglSwapInterval(display, 0)
                swapIntervalSet = true
            }
            GLES20.glViewport(0, 0, width, height)
            LibretroDroid.renderSecondary(width, height)
            if (!EGL14.eglSwapBuffers(display, eglSurface)) onEglError("eglSwapBuffers", drop = false)
        } finally {
            restore(display, mainDraw, mainRead, context)
        }
    }

    private fun restore(display: EGLDisplay, draw: EGLSurface, read: EGLSurface, context: EGLContext) {
        if (!EGL14.eglMakeCurrent(display, draw, read, context)) {
            NxLog.e(TAG, "restoring the main surface failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
        }
    }

    // A window surface with the current context's own config, so the context can be made current on it.
    private fun createWindowSurface(display: EGLDisplay, context: EGLContext, target: Surface): EGLSurface? {
        val configId = IntArray(1)
        if (!EGL14.eglQueryContext(display, context, EGL14.EGL_CONFIG_ID, configId, 0)) {
            NxLog.e(TAG, "eglQueryContext(EGL_CONFIG_ID) failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            return null
        }
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        val attribs = intArrayOf(EGL14.EGL_CONFIG_ID, configId[0], EGL14.EGL_NONE)
        if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, count, 0) || count[0] < 1 || configs[0] == null) {
            NxLog.e(TAG, "no EGL config with id ${configId[0]}: 0x${Integer.toHexString(EGL14.eglGetError())}")
            return null
        }
        val created = try {
            EGL14.eglCreateWindowSurface(display, configs[0], target, intArrayOf(EGL14.EGL_NONE), 0)
        } catch (e: IllegalArgumentException) {
            NxLog.e(TAG, "eglCreateWindowSurface rejected the surface", e)   // a released Surface
            return null
        }
        if (created == null || created == EGL14.EGL_NO_SURFACE) {
            NxLog.e(TAG, "eglCreateWindowSurface failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            return null
        }
        return created
    }

    /**
     * Logs a failed [call] once and stops drawing until a surface is set again ([setSurface]). [drop] false (a swap):
     * a lost context is left to [contextLost], which re-creates the surface with the new context.
     */
    private fun onEglError(call: String, drop: Boolean) {
        val error = EGL14.eglGetError()
        NxLog.e(TAG, "$call failed on the second surface: 0x${Integer.toHexString(error)}")
        if (drop || error != EGL14.EGL_CONTEXT_LOST) {
            destroyEglSurface()
            dropped = true
        }
    }

    private fun destroyEglSurface() {
        if (eglSurface != EGL14.EGL_NO_SURFACE && eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
        }
        forgetEglSurface()
    }

    private fun forgetEglSurface() {
        eglSurface = EGL14.EGL_NO_SURFACE
        eglDisplay = EGL14.EGL_NO_DISPLAY
        swapIntervalSet = false
    }

    private companion object {
        const val TAG = "SecondaryOutput"
    }
}
