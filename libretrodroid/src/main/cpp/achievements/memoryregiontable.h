#ifndef LIBRETRODROID_MEMORYREGIONTABLE_H
#define LIBRETRODROID_MEMORYREGIONTABLE_H

#include <cstddef>
#include <cstdint>

#include "libretro.h"
#include "rc_libretro.h"

namespace libretrodroid {

/**
 * rcheevos' region table (pointers straight into the core's memory) plus whether it may be read. The pointers are
 * only valid while the core that produced them is loaded, so whoever changes the core, its memory accessors or its
 * memory map must invalidate() the table; a table that is not ready reads nothing and never touches the old pointers.
 */
class MemoryRegionTable {
public:
    /** Drops the current table, then builds one for the core as described now. Returns ready(). */
    bool build(const struct retro_memory_map* map, rc_libretro_get_core_memory_info_func coreMemoryInfo, uint32_t consoleId);
    void invalidate();
    bool ready() const { return isReady; }
    /** Bytes read, 0 when not ready. */
    uint32_t read(uint32_t address, uint8_t* buffer, uint32_t numBytes) const;
    uint32_t count() const { return isReady ? regions.count : 0; }
    size_t totalSize() const { return isReady ? regions.total_size : 0; }

private:
    rc_libretro_memory_regions_t regions {};
    bool isReady = false;
};

}

#endif //LIBRETRODROID_MEMORYREGIONTABLE_H
