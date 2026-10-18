#include "memoryregiontable.h"

namespace libretrodroid {

bool MemoryRegionTable::build(const struct retro_memory_map* map, rc_libretro_get_core_memory_info_func coreMemoryInfo,
                              uint32_t consoleId) {
    invalidate();
    isReady = rc_libretro_memory_init(&regions, map, coreMemoryInfo, consoleId) != 0;
    if (!isReady) rc_libretro_memory_destroy(&regions);
    return isReady;
}

void MemoryRegionTable::invalidate() {
    rc_libretro_memory_destroy(&regions);   // zeroes every pointer and size
    isReady = false;
}

uint32_t MemoryRegionTable::read(uint32_t address, uint8_t* buffer, uint32_t numBytes) const {
    if (!isReady) return 0;
    return rc_libretro_memory_read(&regions, address, buffer, numBytes);
}

}
