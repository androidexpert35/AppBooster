package com.tony.appbooster.presentation.tools

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Enumerates the three navigation layout modes used to adapt the app shell
 * to different form factors.
 *
 * - [BottomBar] – phones in portrait; classic Material bottom navigation bar.
 * - [NavigationRail] – medium-width devices (small tablets, large phones in landscape);
 *   vertical rail on the leading edge.
 * - [NavigationDrawer] – large tablets and desktops; permanent navigation drawer.
 */
enum class NavigationLayoutType {
    /** Phone-sized portrait layout with a bottom navigation bar. */
    BottomBar,

    /** Medium-width layout (tablet portrait / phone landscape) with a navigation rail. */
    NavigationRail,

    /** Large-tablet or desktop layout with a permanent navigation drawer. */
    NavigationDrawer
}

/**
 * Determines the appropriate [NavigationLayoutType] from the provided [WindowSizeClass].
 *
 * Decision table:
 * - `Compact` width → [NavigationLayoutType.BottomBar]
 * - `Medium` width → [NavigationLayoutType.NavigationRail]
 * - `Expanded` width → [NavigationLayoutType.NavigationDrawer]
 *
 * @return The navigation layout type that best fits the current window size.
 */
fun WindowSizeClass.navigationLayoutType(): NavigationLayoutType = when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> NavigationLayoutType.BottomBar
    WindowWidthSizeClass.Medium -> NavigationLayoutType.NavigationRail
    else -> NavigationLayoutType.NavigationDrawer
}

/**
 * Returns `true` when the current window is wide enough to display a two-pane
 * (master–detail) layout side-by-side.
 *
 * The two-pane threshold is `Medium` or `Expanded` width.
 */
fun WindowSizeClass.isTwoPaneLayout(): Boolean =
    widthSizeClass != WindowWidthSizeClass.Compact

/**
 * Returns `true` when the window is in a compact height mode (i.e. a phone held landscape).
 * Used to suppress certain decorative elements that waste vertical space.
 */
fun WindowSizeClass.isCompactHeight(): Boolean =
    heightSizeClass == WindowHeightSizeClass.Compact

/**
 * Composition local holding the current [WindowSizeClass] so child composables can
 * read it without threading it explicitly through every call site.
 *
 * Must be provided at the root of the composition via [androidx.compose.runtime.CompositionLocalProvider].
 *
 * @throws IllegalStateException if accessed before being provided.
 */
val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    error("LocalWindowSizeClass not provided. Wrap your content with AppBoosterTheme or provide it explicitly.")
}

/**
 * Convenience composable helper that reads the [LocalWindowSizeClass] and returns
 * whether the app is running on a tablet-class device (medium or expanded width).
 *
 * @return `true` when the window width is `Medium` or `Expanded`.
 */
@Composable
fun isTabletLayout(): Boolean {
    val windowSizeClass = LocalWindowSizeClass.current
    return windowSizeClass.isTwoPaneLayout()
}

