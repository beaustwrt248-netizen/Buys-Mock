package com.buysloans.hub

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun iconBuilder(name: String) = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
)

private fun ImageVector.Builder.strokePath(block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
    path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block
    )
}

internal object MorleyIcons {
    val Home: ImageVector = iconBuilder("Home").apply {
        strokePath {
            moveTo(3f, 11.5f); lineTo(12f, 4f); lineTo(21f, 11.5f)
            moveTo(5.5f, 10f); lineTo(5.5f, 20f); lineTo(18.5f, 20f); lineTo(18.5f, 10f)
            moveTo(9.5f, 20f); lineTo(9.5f, 14f); lineTo(14.5f, 14f); lineTo(14.5f, 20f)
        }
    }.build()

    val Computer: ImageVector = iconBuilder("Computer").apply {
        strokePath {
            moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 16f); lineTo(4f, 16f); close()
            moveTo(8f, 20f); lineTo(16f, 20f)
            moveTo(12f, 16f); lineTo(12f, 20f)
        }
    }.build()

    val Categories: ImageVector = iconBuilder("Categories").apply {
        strokePath {
            moveTo(4f, 4f); lineTo(10f, 4f); lineTo(10f, 10f); lineTo(4f, 10f); close()
            moveTo(14f, 4f); lineTo(20f, 4f); lineTo(20f, 10f); lineTo(14f, 10f); close()
            moveTo(4f, 14f); lineTo(10f, 14f); lineTo(10f, 20f); lineTo(4f, 20f); close()
            moveTo(14f, 14f); lineTo(20f, 14f); lineTo(20f, 20f); lineTo(14f, 20f); close()
        }
    }.build()

    val Console: ImageVector = iconBuilder("Console").apply {
        strokePath {
            moveTo(7f, 9f); lineTo(17f, 9f)
            curveTo(19.5f, 9f, 21f, 11f, 21f, 14.5f)
            lineTo(21f, 17f)
            curveTo(21f, 18.7f, 19f, 19.4f, 18f, 18f)
            lineTo(16.5f, 16f); lineTo(7.5f, 16f); lineTo(6f, 18f)
            curveTo(5f, 19.4f, 3f, 18.7f, 3f, 17f)
            lineTo(3f, 14.5f)
            curveTo(3f, 11f, 4.5f, 9f, 7f, 9f)
            close()
            moveTo(8f, 11.5f); lineTo(8f, 15f)
            moveTo(6.25f, 13.25f); lineTo(9.75f, 13.25f)
            moveTo(16.5f, 12.5f); lineTo(16.6f, 12.5f)
            moveTo(18.5f, 14.5f); lineTo(18.6f, 14.5f)
        }
    }.build()

    val Phone: ImageVector = iconBuilder("Phone").apply {
        strokePath {
            moveTo(7f, 3f); lineTo(17f, 3f); curveTo(18.1f, 3f, 19f, 3.9f, 19f, 5f)
            lineTo(19f, 19f); curveTo(19f, 20.1f, 18.1f, 21f, 17f, 21f)
            lineTo(7f, 21f); curveTo(5.9f, 21f, 5f, 20.1f, 5f, 19f)
            lineTo(5f, 5f); curveTo(5f, 3.9f, 5.9f, 3f, 7f, 3f); close()
            moveTo(10f, 6f); lineTo(14f, 6f)
            moveTo(11.5f, 18f); lineTo(12.5f, 18f)
        }
    }.build()

    val Money: ImageVector = iconBuilder("Money").apply {
        strokePath {
            moveTo(12f, 3f); lineTo(12f, 21f)
            moveTo(16f, 7.5f)
            curveTo(15.2f, 6.5f, 13.8f, 6f, 12f, 6f)
            curveTo(9.8f, 6f, 8f, 7.2f, 8f, 9f)
            curveTo(8f, 12f, 16f, 10.5f, 16f, 15f)
            curveTo(16f, 16.8f, 14.2f, 18f, 12f, 18f)
            curveTo(10.2f, 18f, 8.8f, 17.5f, 8f, 16.5f)
        }
    }.build()

    val Menu: ImageVector = iconBuilder("Menu").apply {
        strokePath {
            moveTo(4f, 7f); lineTo(20f, 7f)
            moveTo(4f, 12f); lineTo(20f, 12f)
            moveTo(4f, 17f); lineTo(20f, 17f)
        }
    }.build()

    val Laptop: ImageVector = iconBuilder("Laptop").apply {
        strokePath {
            moveTo(5f, 6f); lineTo(19f, 6f); lineTo(19f, 16f); lineTo(5f, 16f); close()
            moveTo(3f, 18f); lineTo(21f, 18f)
        }
    }.build()
}

@Composable
internal fun MorleyIcon(imageVector: ImageVector, contentDescription: String, tint: Color, modifier: Modifier = Modifier) {
    Icon(imageVector = imageVector, contentDescription = contentDescription, tint = tint, modifier = modifier)
}
