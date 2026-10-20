package com.qingqian.chonglema.ui

import kotlin.math.roundToInt

/** 占比文案，例如 "64%"。total 为 0 时返回 "0%"。 */
internal fun percentOf(count: Int, total: Int): String {
    if (total <= 0 || count <= 0) return "0%"
    return (count * 100f / total).roundToInt().toString() + "%"
}
