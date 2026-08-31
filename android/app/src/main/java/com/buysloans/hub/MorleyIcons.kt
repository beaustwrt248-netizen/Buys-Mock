package com.buysloans.hub

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.addPath
import androidx.compose.ui.unit.dp

internal object MorleyIcons {
    private fun icon(name: String, pathData: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(PathParser().parsePathString(pathData).toNodes(), fill = null, stroke = androidx.compose.ui.graphics.SolidColor(Color.White), strokeLineWidth = 1.8f)
    }.build()

    val Home = icon("Home", "M3 11.5 L12 4 L21 11.5 M5.5 10 V20 H18.5 V10 M9.5 20 V14 H14.5 V20")
    val Computer = icon("Computer", "M4 5 H20 V16 H4 Z M8 20 H16 M12 16 V20")
    val Console = icon("Console", "M7 9 H17 C19.5 9 21 11 21 14.5 V17 C21 18.7 19 19.4 18 18 L16.5 16 H7.5 L6 18 C5 19.4 3 18.7 3 17 V14.5 C3 11 4.5 9 7 9 Z M8 11.5 V15 M6.25 13.25 H9.75 M16.5 12.5 H16.6 M18.5 14.5 H18.6")
    val Money = icon("Money", "M12 3 V21 M16 7.5 C15.2 6.5 13.8 6 12 6 C9.8 6 8 7.2 8 9 C8 12 16 10.5 16 15 C16 16.8 14.2 18 12 18 C10.2 18 8.8 17.5 8 16.5")
    val Menu = icon("Menu", "M4 7 H20 M4 12 H20 M4 17 H20")
    val Laptop = icon("Laptop", "M5 6 H19 V16 H5 Z M3 18 H21")
}

@Composable
internal fun MorleyIcon(imageVector: ImageVector, contentDescription: String, tint: Color, modifier: Modifier = Modifier) {
    Icon(imageVector = imageVector, contentDescription = contentDescription, tint = tint, modifier = modifier)
}
