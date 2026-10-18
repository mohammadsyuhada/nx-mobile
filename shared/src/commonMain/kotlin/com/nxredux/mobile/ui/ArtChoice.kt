package com.nxredux.mobile.ui

import com.nxredux.mobile.settings.BackdropArt

/** What a game's box slot draws: the 3D box art, a flat cover (2D front or SteamGridDB grid) in the case, or the placeholder case. */
sealed interface BoxShown {
    data class Art3d(val url: String) : BoxShown
    data class Cover(val url: String) : BoxShown
    /** A wheel (logo) whole, fitted in the slot: never cut into a case. */
    data class Wheel(val url: String) : BoxShown
    data object Placeholder : BoxShown
}

/**
 * The box chain: the 3D box art, else the 2D front, else the grid, the last two drawn in the case; none of them, the
 * placeholder. [failed]: urls that did not load, skipped so a broken file falls to the next link, never a hole.
 */
internal fun boxShown(boxart: String?, box2d: String?, grid: String?, failed: Set<String> = emptySet()): BoxShown {
    boxart?.takeIf { it !in failed }?.let { return BoxShown.Art3d(it) }
    val cover = listOfNotNull(box2d, grid).firstOrNull { it !in failed } ?: return BoxShown.Placeholder
    return BoxShown.Cover(cover)
}

/**
 * A full-screen background: the game's screenshot; without one the SteamGridDB hero when the screen is landscape, the
 * grid when [portrait]. Never the other shape's stand-in: a wide hero cropped to portrait (or a tall grid to landscape)
 * reads as noise, so that case is the plain background.
 */
internal fun backgroundArt(screenshot: String?, hero: String?, grid: String?, portrait: Boolean): String? =
    screenshot ?: if (portrait) grid else hero

/**
 * How a tile draws its picture: [CROP] scales it to fill and centre-crops (a screenshot, a grid, the abstract art);
 * [HERO] scales a SteamGridDB hero to the tile's full height (no top or bottom cut) and anchors it right, where a
 * hero's subject usually is, the left part running off the tile.
 */
enum class TileFit { CROP, HERO }

/** A tile's picture: its [url] and how it fills the tile ([fit]). */
data class TileArt(val url: String, val fit: TileFit)

/**
 * A Grid, Carousel or Home pin game tile's picture: the screenshot; without one the SteamGridDB stand-in nearer the
 * [width] x [height] tile's shape: the hero for a tile wider than tall (the Carousel's 340x240, the game-list Grid's
 * wide tiles), drawn [TileFit.HERO], the grid for a taller or square one (the main menu Grid's 140x154). No cross
 * fallback, as [backgroundArt]; last, the game's generated [abstract] picture, so a tile is never a plain title tile. An
 * unmeasured tile (0 x 0) takes neither. The Home Continue card has its own chain ([continuePictures]); RetroAchievements
 * and Game Time stay screenshot-only.
 */
internal fun tileArt(width: Float, height: Float, screenshot: String?, hero: String?, grid: String?, abstract: String? = null): TileArt? {
    if (screenshot != null) return TileArt(screenshot, TileFit.CROP)
    if (width <= 0f || height <= 0f) return null
    if (height >= width) return (grid ?: abstract)?.let { TileArt(it, TileFit.CROP) }
    return hero?.let { TileArt(it, TileFit.HERO) } ?: abstract?.let { TileArt(it, TileFit.CROP) }
}

/**
 * A Backdrop row's box by Layouts → Backdrop art ([BackdropGame.art]): 3D box art, the box chain ([boxShown]); 2D box
 * art in the case; a wheel fitted whole. A game without it (or whose file failed, [failed]) shows its 3D box art, else
 * the placeholder box (the handheld's buildBoxart).
 */
internal fun backdropBoxShown(game: BackdropGame, failed: Set<String> = emptySet()): BoxShown {
    fun ok(url: String?) = url?.takeIf { it !in failed }
    when (game.art) {
        BackdropArt.BOX3D -> return boxShown(game.boxartUrl, game.box2dUrl, game.gridUrl, failed)
        BackdropArt.BOX2D -> ok(game.box2dUrl)?.let { return BoxShown.Cover(it) }
        BackdropArt.WHEEL -> ok(game.wheelUrl)?.let { return BoxShown.Wheel(it) }
    }
    return ok(game.boxartUrl)?.let { BoxShown.Art3d(it) } ?: BoxShown.Placeholder
}
