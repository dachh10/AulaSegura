package com.example.aulasegura.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Returns an interaction source plus a modifier that subtly scales the element
 * down while pressed. Mirrors the `active:scale-95` affordance used by the
 * Stitch mockups. Pair it with `Modifier.clickable(interactionSource = ...)`.
 */
@Composable
fun rememberPressScale(scaleDown: Float = 0.97f): Pair<MutableInteractionSource, Modifier> {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "pressScale",
    )
    val modifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    return source to modifier
}
