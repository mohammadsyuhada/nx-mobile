package com.nxredux.mobile.runner

import com.nxredux.mobile.options.CoreOption
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.LibretroDroid
import com.swordfish.libretrodroid.NxLog
import com.swordfish.libretrodroid.Variable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

fun Variable.toCoreOption(): CoreOption = CoreOption(
    key = key.orEmpty(),
    value = value.orEmpty(),
    label = label.orEmpty(),
    labelCategorized = labelCategorized.orEmpty(),
    info = info.orEmpty(),
    category = category.orEmpty(),
    categoryLabel = categoryLabel.orEmpty(),
    categoryInfo = categoryInfo.orEmpty(),
    values = values?.toList().orEmpty(),
    valueLabels = valueLabels?.toList().orEmpty(),
    defaultValue = defaultValue.orEmpty(),
    visible = visible,
)

/**
 * Reads the core's options on the emulation (GL) thread, where the core mutates them, after asking the
 * core to refresh which options are visible. Only core-declared options are returned; an empty list means
 * the core has none, null means the read failed.
 */
suspend fun GLRetroView.readCoreOptions(): List<CoreOption>? = suspendCancellableCoroutine { cont ->
    queueEvent {
        val result = runCatching {
            refreshOptionsVisibility(useEmulationThread = false)
            LibretroDroid.getVariables().filter { it.values != null }.map { it.toCoreOption() }
        }.onFailure { NxLog.w("CoreOptions", "reading core options failed", it) }.getOrNull()
        cont.resume(result)
    }
}
