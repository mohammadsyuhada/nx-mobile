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

#ifndef LIBRETRODROID_TEARDOWNCONTEXT_H
#define LIBRETRODROID_TEARDOWNCONTEXT_H

#include <EGL/egl.h>

namespace libretrodroid {

// GLSurfaceView pauses before the lifecycle's ON_DESTROY: it destroys the window surface and leaves no context current,
// but keeps the context (preserveEGLContextOnPause). Queued events still run then, so a teardown queued on the GL
// thread can make that context current again (surfaceless, or on a 1x1 pbuffer) for the core's context_destroy and
// our own glDelete* calls. GL thread only. Save states use it too: a core may run emulation inside retro_serialize_size
// or retro_unserialize (Azahar), which issues GL calls, also while the view is paused (onStop's auto state).
class TeardownContext {
public:
    // Called from onSurfaceCreated with the new context current: the display and context a later teardown uses.
    static void remember();
    // After the teardown: the context is GLSurfaceView's, which destroys it when its thread exits.
    static void forget();

    // Makes the remembered context current when none is; restores "no context" on destruction. Does nothing when a
    // context is already current, on any thread but the one that remembered it, or when the remembered context is
    // gone (its calls fail with EGL_BAD_CONTEXT).
    TeardownContext();
    ~TeardownContext();
    TeardownContext(const TeardownContext&) = delete;
    TeardownContext& operator=(const TeardownContext&) = delete;

    // A context is current on this thread (ours or one made current here).
    bool isCurrent() const { return current; }

private:
    bool current = false;
    bool madeCurrent = false;
    // Its own copy: forget() may run before this is destroyed.
    EGLDisplay display = EGL_NO_DISPLAY;
    EGLSurface surface = EGL_NO_SURFACE;
};

}

#endif //LIBRETRODROID_TEARDOWNCONTEXT_H
