package com.clearpath.xray_compose.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

object AppAnimation {
    const val Speed = 300

    fun <T> spec() = tween<T>(
        durationMillis = Speed,
        easing = FastOutSlowInEasing
    )

    val DefaultTransition: ContentTransform
        get() = fadeIn(spec()) togetherWith fadeOut(spec())

    val ForwardTransition: ContentTransform
        get() = (slideInHorizontally(spec()) { it } + fadeIn(spec())) togetherWith
                (slideOutHorizontally(spec()) { -it / 3 } + fadeOut(spec()))

    val PopTransition: ContentTransform
        get() = (slideInHorizontally(spec()) { -it / 3 } + fadeIn(spec())) togetherWith
                (slideOutHorizontally(spec()) { it } + fadeOut(spec()))
}
