package com.buysloans.hub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** Android 16 adaptive dashboard primitives.
 * Compact windows keep bottom navigation; wider windows use a rail so
 * content does not stretch across tablets, foldables, desktop windows,
 * landscape, or split-screen configurations.
 */
internal enum class MorleyAdaptiveSize { Compact, Medium, Expanded }

@Composable
internal fun morleyAdaptiveSize(): MorleyAdaptiveSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> MorleyAdaptiveSize.Compact
        widthDp < 840 -> MorleyAdaptiveSize.Medium
        else -> MorleyAdaptiveSize.Expanded
    }
}

@Composable
internal fun AdaptiveContentFrame(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxContentWidth = when {
            maxWidth >= 1200.dp -> 1120.dp
            maxWidth >= 840.dp -> 960.dp
            maxWidth >= 600.dp -> 760.dp
            else -> maxWidth
        }
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = maxContentWidth)
                .align(Alignment.TopCenter)
        ) { content() }
    }
}

@Composable
internal fun AdaptiveBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Uses Activity's supported back dispatcher rather than legacy onBackPressed.
    // On API 36 this participates in the platform predictive-back model.
    BackHandler(enabled = enabled, onBack = onBack)
}

internal data class AdaptiveNavItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
internal fun MorleyAdaptiveNavigation(
    size: MorleyAdaptiveSize,
    items: List<AdaptiveNavItem>,
    compact: @Composable () -> Unit
) {
    if (size == MorleyAdaptiveSize.Compact) {
        compact()
        return
    }
    NavigationRail(containerColor = MorleySurface) {
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            NavigationRailItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { MorleyIcon(item.icon, item.label, if (item.selected) MorleyAccent else MorleyTextSecondary) },
                label = { Text(item.label) },
                alwaysShowLabel = size == MorleyAdaptiveSize.Expanded,
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MorleyAccentSoft,
                    selectedIconColor = MorleyAccent,
                    selectedTextColor = MorleyAccent,
                    unselectedIconColor = MorleyTextSecondary,
                    unselectedTextColor = MorleyTextSecondary
                )
            )
        }
    }
}
