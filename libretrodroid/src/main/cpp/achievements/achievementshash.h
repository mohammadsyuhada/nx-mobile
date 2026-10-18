#ifndef LIBRETRODROID_ACHIEVEMENTSHASH_H
#define LIBRETRODROID_ACHIEVEMENTSHASH_H

#include <cstdint>
#include <string>
#include <utility>
#include <vector>

#include "rc_hash.h"

namespace libretrodroid {

/**
 * rc_hash over a set of (virtual path, fd) pairs, the same shape loadGameFromVirtualFiles takes. Paths a cue/m3u/ccd
 * name are resolved against the set: exact virtual path first, then the base name case-insensitively. CHD images go
 * through libchdr; other discs through rcheevos' default reader. The fds are closed when hash() returns.
 */
class AchievementsHash {
public:
    static std::string hash(uint32_t consoleId, std::vector<std::pair<std::string, int>> files);
    /** Where the Dolphin core is. Nothing is loaded until a Wii/GameCube hash, and only for that hash. */
    static void setDiscLibraryPath(const std::string& path);
    /** Filereader + cdreader for rc_client_set_hash_callbacks (uses the set given to the last hash() call). */
    static const rc_hash_callbacks_t* callbacks();
    /**
     * The first data track's [sectors] (relative to its start) of the CHD on [fd], 2048 bytes of user data each (an empty
     * vector: unreadable). [ok] false when [fd] is not a CHD libchdr opens. The fd is closed.
     */
    static std::vector<std::vector<uint8_t>> chdSectors(int fd, const std::vector<uint32_t>& sectors, bool* ok);
    /** The fd for a path the readers ask for, or -1. */
    static int lookup(const char* path);
};

}

#endif //LIBRETRODROID_ACHIEVEMENTSHASH_H
