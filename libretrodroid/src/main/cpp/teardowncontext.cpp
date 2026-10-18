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

#include "teardowncontext.h"

#include <cstring>
#include <pthread.h>

#include "log.h"

namespace libretrodroid {

namespace {

EGLDisplay rememberedDisplay = EGL_NO_DISPLAY;
EGLContext rememberedContext = EGL_NO_CONTEXT;
// The GL thread that owns the context. Another thread (the fallback destroy on main once the GL thread is gone) never
// touches it: by then GLSurfaceView has destroyed it, and its handle may be reused by another view's context.
pthread_t rememberedThread {};
bool hasRememberedThread = false;

}

void TeardownContext::remember() {
    rememberedDisplay = eglGetCurrentDisplay();
    rememberedContext = eglGetCurrentContext();
    rememberedThread = pthread_self();
    hasRememberedThread = true;
}

void TeardownContext::forget() {
    rememberedDisplay = EGL_NO_DISPLAY;
    rememberedContext = EGL_NO_CONTEXT;
    hasRememberedThread = false;
}

TeardownContext::TeardownContext() {
    if (eglGetCurrentContext() != EGL_NO_CONTEXT) {
        current = true;
        return;
    }
    if (rememberedDisplay == EGL_NO_DISPLAY || rememberedContext == EGL_NO_CONTEXT) return;
    if (!hasRememberedThread || !pthread_equal(rememberedThread, pthread_self())) return;
    display = rememberedDisplay;

    const char* extensions = eglQueryString(display, EGL_EXTENSIONS);
    bool surfaceless = extensions != nullptr && std::strstr(extensions, "EGL_KHR_surfaceless_context") != nullptr;
    if (!surfaceless) {
        EGLint configId = 0;
        if (!eglQueryContext(display, rememberedContext, EGL_CONFIG_ID, &configId)) {
            LOGW("Teardown: the session's GL context is gone (0x%x)", eglGetError());
            return;
        }
        const EGLint configAttributes[] = { EGL_CONFIG_ID, configId, EGL_NONE };
        EGLConfig config = nullptr;
        EGLint configs = 0;
        if (!eglChooseConfig(display, configAttributes, &config, 1, &configs) || configs < 1) return;
        const EGLint surfaceAttributes[] = { EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE };
        surface = eglCreatePbufferSurface(display, config, surfaceAttributes);
        if (surface == EGL_NO_SURFACE) {
            LOGW("Teardown: no pbuffer for the GL context (0x%x)", eglGetError());
            return;
        }
    }

    if (eglMakeCurrent(display, surface, surface, rememberedContext)) {
        current = true;
        madeCurrent = true;
    } else {
        LOGW("Teardown: cannot make the session's GL context current (0x%x)", eglGetError());
    }
}

TeardownContext::~TeardownContext() {
    if (madeCurrent) eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    if (surface != EGL_NO_SURFACE) eglDestroySurface(display, surface);
}

}
