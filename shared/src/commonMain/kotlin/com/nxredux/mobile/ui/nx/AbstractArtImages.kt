package com.nxredux.mobile.ui.nx

import coil3.Uri
import coil3.fetch.Fetcher
import coil3.key.Keyer

/**
 * Loads an [com.nxredux.mobile.ui.abstractArtUrl] model: the game's generated picture
 * ([com.nxredux.mobile.ui.abstractPlan]), drawn on the first request off the main thread (the image loader's fetch)
 * and kept as an app-private cached picture, so the next request is a plain file read. An
 * [com.nxredux.mobile.ui.abstractAppArtUrl] model: the same in the app's launcher icon's palette with the icon in the
 * middle where the platform can read it, else the plain picture. An [com.nxredux.mobile.ui.abstractAccentArtUrl] model
 * (Home's "Pick a game"): the fixed seed's picture in the accent's palette, cached per colour. Any other model: not this one's.
 */
internal expect fun abstractArtFetcher(): Fetcher.Factory<Uri>

/** The memory cache key for an app's picture (its URL and the installed version), or null where there is none. */
internal expect fun abstractArtKeyer(): Keyer<Uri>?
