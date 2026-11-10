package com.example.aulasegura.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape tokens from the AulaSegura Stitch design system (roundness: ROUND_FULL).
 *
 * Measured from the mockup markup:
 *   rounded        ->  4dp  (small inline elements)
 *   rounded-[24px] -> 24dp  (cards)
 *   rounded-[28px] -> 28dp  (large cards, sheets)
 *   rounded-full   -> pill  (buttons, chips, avatars, status badges)
 */
val AulaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Card containers: rounded-[24px] */
val CardShape = RoundedCornerShape(24.dp)

/** Large cards and bottom sheets: rounded-[28px] */
val SheetShape = RoundedCornerShape(28.dp)

/** Inner containers, inputs, thumbnails. */
val InnerShape = RoundedCornerShape(16.dp)

/** Chips and buttons: rounded-full */
val ChipShape = RoundedCornerShape(percent = 50)

/** Status pills and avatars. */
val PillShape = RoundedCornerShape(percent = 50)
