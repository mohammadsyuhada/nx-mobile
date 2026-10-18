/**
 * chdreader.h - CHD disc reader for rcheevos hashing (ported from nx-redux chd_reader.h)
 *
 * CD reader entry points that let rcheevos hash CHD disc images. The CHD is found through
 * AchievementsHash::lookup, so it is read from an fd rather than opened by path.
 */

#ifndef LIBRETRODROID_CHDREADER_H
#define LIBRETRODROID_CHDREADER_H

#include <cstddef>
#include <cstdint>

namespace libretrodroid {

/**
 * Open a track from a CHD image.
 *
 * @param path Path the hasher asks for (resolved through AchievementsHash::lookup)
 * @param track Track number (1-based) or RC_HASH_CDTRACK_* special value
 * @return Track handle, or nullptr if the path is not a CHD or on error
 */
void* chdreader_open_track(const char* path, uint32_t track);

/**
 * Read a sector from an open CHD track.
 *
 * @param track_handle Handle returned by chdreader_open_track
 * @param sector Absolute disc LBA
 * @param buffer Buffer to read data into
 * @param requested_bytes Number of bytes to read
 * @return Number of bytes actually read
 */
size_t chdreader_read_sector(void* track_handle, uint32_t sector, void* buffer, size_t requested_bytes);

/** Close a CHD track handle (closes the CHD and the FILE it was opened on). */
void chdreader_close_track(void* track_handle);

/** Absolute disc LBA of the track's first sector (0 for track 1 of a CD, 45000 for a GD-ROM high-density track). */
uint32_t chdreader_first_track_sector(void* track_handle);

}

#endif //LIBRETRODROID_CHDREADER_H
