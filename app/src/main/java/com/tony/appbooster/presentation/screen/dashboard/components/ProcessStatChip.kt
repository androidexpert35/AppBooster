package com.tony.appbooster.presentation.screen.dashboard.components

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Describes a single stat chip rendered inside a [ProcessProgressContent] stats row.
 *
 * Each instance maps one statistical counter to its visual representation so the
 * composable can iterate over a list instead of hard-coding three separate chip
 * rendering blocks.
 *
 * @property count Numerical value to display inside the chip.
 * @property label Short descriptive label shown next to the count.
 * @property icon Icon rendered on the leading edge of the chip.
 * @property style Visual colour role applied to the chip surface and text.
 */
data class ProcessStatChip(
    val count: Int,
    val label: String,
    val icon: ImageVector,
    val style: ProcessStatChipStyle
)

/**
 * Colour role variants for a [ProcessStatChip].
 *
 * Each variant maps to a distinct M3 colour token set so chip colour is driven
 * by data rather than branching inside the composable.
 */
enum class ProcessStatChipStyle {
    /** Uses error container tokens – highlights items that still need work. */
    Pending,

    /** Uses surface-variant tokens – neutral, for informational counts. */
    Neutral,

    /** Uses primary container tokens – positive, for completed counts. */
    Done
}

