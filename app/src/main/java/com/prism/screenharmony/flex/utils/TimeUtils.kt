package com.prism.screenharmony.flex.utils

import kotlin.math.roundToInt

fun formatDelay(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return when {
        m > 0 && s > 0 -> "$m min $s sec"
        m > 0 -> "$m min"
        else -> "$s sec"
    }
}

fun sliderToSeconds(v: Float): Int {
    return if (v <= 0.5f) {
        (v / 0.5f * 60f).roundToInt()
    } else {
        (60f + (v - 0.5f) / 0.5f * 540f).roundToInt()
    }
}

fun secondsToSlider(sec: Int): Float {
    return if (sec <= 60) {
        (sec.toFloat() / 60f) * 0.5f
    } else {
        0.5f + ((sec - 60).toFloat() / 540f) * 0.5f
    }
}
