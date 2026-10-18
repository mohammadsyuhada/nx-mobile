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

enum class ShaderScaleType(val value: Int) { NONE(0), SOURCE(1), VIEWPORT(2), ABSOLUTE(3) }

enum class ShaderWrap(val value: Int) { EDGE(0), REPEAT(1), MIRRORED(2) }

/** One pass of a RetroArch GLSL preset; [source] has its #pragma lines already stripped. */
data class ShaderPassData(
    val source: String,
    /** Null leaves the filter to the Screen Sharpness setting. */
    val filterLinear: Boolean?,
    val wrap: ShaderWrap,
    val scaleTypeX: ShaderScaleType,
    val scaleTypeY: ShaderScaleType,
    val scaleX: Float,
    val scaleY: Float,
    /** 0 = no modulo. */
    val frameCountMod: Int,
    /** Empty = no alias. */
    val alias: String,
)

/** A preset lookup texture, decoded to [width] x [height] RGBA8 in [rgba]. */
class ShaderLutData(
    val id: String,
    val width: Int,
    val height: Int,
    val rgba: ByteArray,
    val linear: Boolean,
    val wrap: ShaderWrap,
)

data class ShaderChainData(
    val passes: List<ShaderPassData>,
    val luts: List<ShaderLutData>,
    val params: Map<String, Float>,
)
