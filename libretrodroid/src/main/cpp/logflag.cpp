#include "logflag.h"

#include <atomic>

// Read on every gated log call from any thread; relaxed is enough for an on/off switch.
static std::atomic<bool> logEnabled { false };

extern "C" bool nx_log_enabled(void) { return logEnabled.load(std::memory_order_relaxed); }

extern "C" void nx_log_set_enabled(bool enabled) { logEnabled.store(enabled, std::memory_order_relaxed); }
