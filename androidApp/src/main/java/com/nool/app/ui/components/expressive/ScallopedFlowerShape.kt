package com.nool.app.ui.components.expressive

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Expressive Scalloped / Flower-shaped Shape:
 * Renders an organic 8-to-12 lobed wavy flower outline commonly seen in modern
 * Material You / Android Expressive floating action buttons.
 *
 * @param lobes The number of petals/lobes (typically 8, 10, or 12).
 * @param scallopDepth The depth ratio of the indentations (0.05f to 0.18f).
 */
class ScallopedFlowerShape(
    val lobes: Int = 12,
    val scallopDepth: Float = 0.10f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = minOf(centerX, centerY)

        if (maxRadius <= 0f) {
            return Outline.Generic(path)
        }

        val numLobes = lobes.coerceIn(6, 16)
        val numPoints = numLobes * 2
        val angleStep = (2.0 * PI) / numPoints

        val points = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until numPoints) {
            val theta = i * angleStep - (PI / 2.0) // Start from top
            val isPeak = i % 2 == 0
            val radius = if (isPeak) maxRadius else maxRadius * (1f - scallopDepth)
            val x = (centerX + radius * cos(theta)).toFloat()
            val y = (centerY + radius * sin(theta)).toFloat()
            points.add(x to y)
        }

        // Draw smooth closed curve connecting points with cubic Bézier segments
        path.moveTo(points[0].first, points[0].second)

        for (i in 0 until numPoints) {
            val p0 = points[(i - 1 + numPoints) % numPoints]
            val p1 = points[i]
            val p2 = points[(i + 1) % numPoints]
            val p3 = points[(i + 2) % numPoints]

            // Catmull-Rom to Cubic Bézier conversion
            val c1x = p1.first + (p2.first - p0.first) / 6f
            val c1y = p1.second + (p2.second - p0.second) / 6f

            val c2x = p2.first - (p3.first - p1.first) / 6f
            val c2y = p2.second - (p3.second - p1.second) / 6f

            path.cubicTo(c1x, c1y, c2x, c2y, p2.first, p2.second)
        }

        path.close()
        return Outline.Generic(path)
    }
}
