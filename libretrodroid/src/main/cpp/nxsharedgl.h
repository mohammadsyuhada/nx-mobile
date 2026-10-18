#pragma once
#include <stdbool.h>
// nx-mobile private libretro environment call: the frontend fills *data (struct nx_shared_gl_interface*) and returns true.
// Only the nx-mobile runner answers it; only nx-mobile's patched cores send it.
#define NX_ENVIRONMENT_GET_SHARED_GL (0x20000 /* RETRO_ENVIRONMENT_PRIVATE */ | 0x4e58)
#define NX_SHARED_GL_INTERFACE_VERSION 1
struct nx_shared_gl_interface {
    unsigned version;                     // set by the frontend: NX_SHARED_GL_INTERFACE_VERSION
    void* (*create)(void);                // GL thread: a context sharing the core's objects, or NULL
    bool (*make_current)(void* handle);   // any one thread at a time
    void (*release)(void* handle);        // unbinds it from the calling thread
    void (*destroy)(void* handle);        // releases first if current on the calling thread
};
