/*
 *     Copyright (C) 2026  nx-mobile contributors
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

#ifndef LIBRETRODROID_GLDEBUG_H
#define LIBRETRODROID_GLDEBUG_H

#include <cstddef>
#include <cstdint>

// Debug-only GL driver output, logged to tag NXGL. Off unless the app turns it on (GLRetroViewData.glDebugOutput).
namespace libretrodroid::gldebug {

void setEnabled(bool enabled);

// On the GL thread with the context current: installs the GL_KHR_debug callback when the driver has one.
void install();

// After retro_run: without a KHR_debug callback, drains glGetError so errors still show up (capped).
void pollErrors();

// Video/audio callbacks: every 5 s logs hw/dupe/software frame counts, the centre pixel of the core's framebuffer
// (was anything drawn into it?) and the audio frames with their peak sample (is the game running?).
void onVideoRefresh(const void* data, unsigned width, unsigned height, unsigned framebuffer);
void onAudio(const int16_t* data, size_t frames);

}

#endif
