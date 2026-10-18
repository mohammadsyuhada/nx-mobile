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

#include "sharedcontexts.h"

#ifndef NX_SHAREDCONTEXTS_NO_EGL
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <cstring>
#include <mutex>
#include "log.h"
#endif

namespace libretrodroid {

int SharedContextSlots::acquire() {
    for (size_t i = 0; i < used.size(); i++) if (!used[i]) { used[i] = true; return (int) i; }
    return -1;
}

bool SharedContextSlots::release(int slot) {
    if (slot < 0 || slot >= (int) used.size() || !used[slot]) return false;
    used[slot] = false;
    return true;
}

int SharedContextSlots::live() const {
    int n = 0;
    for (bool u : used) if (u) n++;
    return n;
}

#ifndef NX_SHAREDCONTEXTS_NO_EGL
namespace {

struct Shared {
    int slot;
    EGLDisplay display;
    EGLContext context;
    EGLSurface surface;   // EGL_NO_SURFACE when surfaceless
};

std::mutex lock;
SharedContextSlots slots(SharedContexts::kMaxSharedContexts);
Shared* live[SharedContexts::kMaxSharedContexts] = {};

// Called by the core on its GL thread, with the session's context current there.
void* create() {
    EGLDisplay display = eglGetCurrentDisplay();
    EGLContext main = eglGetCurrentContext();
    if (display == EGL_NO_DISPLAY || main == EGL_NO_CONTEXT) { LOGW("Shared GL: no current context to share"); return nullptr; }
    std::lock_guard<std::mutex> guard(lock);
    int slot = slots.acquire();
    if (slot < 0) { LOGW("Shared GL: all %d contexts in use", SharedContexts::kMaxSharedContexts); return nullptr; }

    EGLint configId = 0, clientVersion = 3;
    eglQueryContext(display, main, EGL_CONFIG_ID, &configId);
    eglQueryContext(display, main, EGL_CONTEXT_CLIENT_VERSION, &clientVersion);
    const EGLint configAttributes[] = { EGL_CONFIG_ID, configId, EGL_NONE };
    EGLConfig config = nullptr;
    EGLint configs = 0;
    if (!eglChooseConfig(display, configAttributes, &config, 1, &configs) || configs < 1) {
        slots.release(slot); LOGW("Shared GL: no config (0x%x)", eglGetError()); return nullptr;
    }
    const EGLint contextAttributes[] = { EGL_CONTEXT_CLIENT_VERSION, clientVersion, EGL_NONE };
    EGLContext context = eglCreateContext(display, config, main, contextAttributes);
    if (context == EGL_NO_CONTEXT) { slots.release(slot); LOGW("Shared GL: create failed (0x%x)", eglGetError()); return nullptr; }

    const char* extensions = eglQueryString(display, EGL_EXTENSIONS);
    bool surfaceless = extensions != nullptr && std::strstr(extensions, "EGL_KHR_surfaceless_context") != nullptr;
    EGLSurface surface = EGL_NO_SURFACE;
    if (!surfaceless) {
        const EGLint surfaceAttributes[] = { EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE };
        surface = eglCreatePbufferSurface(display, config, surfaceAttributes);
        if (surface == EGL_NO_SURFACE) {
            eglDestroyContext(display, context); slots.release(slot);
            LOGW("Shared GL: no pbuffer (0x%x)", eglGetError()); return nullptr;
        }
    }
    auto* s = new Shared { slot, display, context, surface };
    live[slot] = s;
    LOGI("Shared GL: context %d created (%s)", slot, surfaceless ? "surfaceless" : "pbuffer");
    return s;
}

bool makeCurrent(void* handle) {
    auto* s = static_cast<Shared*>(handle);
    return s != nullptr && eglMakeCurrent(s->display, s->surface, s->surface, s->context);
}

void release(void* handle) {
    auto* s = static_cast<Shared*>(handle);
    if (s != nullptr && eglGetCurrentContext() == s->context)
        eglMakeCurrent(s->display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
}

void destroyLocked(Shared* s) {
    if (eglGetCurrentContext() == s->context) eglMakeCurrent(s->display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(s->display, s->context);   // deferred by EGL while current on another thread
    if (s->surface != EGL_NO_SURFACE) eglDestroySurface(s->display, s->surface);
    live[s->slot] = nullptr;
    slots.release(s->slot);
    delete s;
}

void destroy(void* handle) {
    if (handle == nullptr) return;
    std::lock_guard<std::mutex> guard(lock);
    destroyLocked(static_cast<Shared*>(handle));
}

}

void SharedContexts::fill(nx_shared_gl_interface* out) {
    out->version = NX_SHARED_GL_INTERFACE_VERSION;
    out->create = create;
    out->make_current = makeCurrent;
    out->release = release;
    out->destroy = destroy;
}

void SharedContexts::destroyAll() {
    std::lock_guard<std::mutex> guard(lock);
    for (auto* s : live) if (s != nullptr) { LOGW("Shared GL: context %d left by the core", s->slot); destroyLocked(s); }
}
#endif

}
