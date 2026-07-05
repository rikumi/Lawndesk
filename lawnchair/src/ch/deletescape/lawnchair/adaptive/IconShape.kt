/*
 *     Copyright (C) 2019 paphonb@xda
 *
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.deletescape.lawnchair.adaptive

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.support.v4.graphics.PathParser
import ch.deletescape.lawnchair.util.extensions.e
import com.android.launcher3.R
import com.android.launcher3.Utilities
import java.lang.Exception
import kotlin.math.min

open class IconShape(val topLeft: Corner,
                     val topRight: Corner,
                     val bottomLeft: Corner,
                     val bottomRight: Corner) {

    constructor(topLeftShape: IconCornerShape,
                topRightShape: IconCornerShape,
                bottomLeftShape: IconCornerShape,
                bottomRightShape: IconCornerShape,
                topLeftScale: Float,
                topRightScale: Float,
                bottomLeftScale: Float,
                bottomRightScale: Float) : this(Corner(topLeftShape, topLeftScale),
                                                Corner(topRightShape, topRightScale),
                                                Corner(bottomLeftShape, bottomLeftScale),
                                                Corner(bottomRightShape, bottomRightScale))

    constructor(topLeftShape: IconCornerShape,
                topRightShape: IconCornerShape,
                bottomLeftShape: IconCornerShape,
                bottomRightShape: IconCornerShape,
                topLeftScale: PointF,
                topRightScale: PointF,
                bottomLeftScale: PointF,
                bottomRightScale: PointF) : this(Corner(topLeftShape, topLeftScale),
                                                Corner(topRightShape, topRightScale),
                                                Corner(bottomLeftShape, bottomLeftScale),
                                                Corner(bottomRightShape, bottomRightScale))

    constructor(shape: IconShape) : this(
            shape.topLeft, shape.topRight, shape.bottomLeft, shape.bottomRight)

    private val isCircle =
            topLeft == Corner.fullArc &&
            topRight == Corner.fullArc &&
            bottomLeft == Corner.fullArc &&
            bottomRight == Corner.fullArc

    private val tmpPoint = PointF()
    open val qsbEdgeRadius = 0

    open fun getMaskPath(): Path {
        return Path().also { addToPath(it, 0f, 0f, 100f, 100f, 50f) }
    }

    open fun addShape(path: Path, x: Float, y: Float, radius: Float) {
        if (isCircle) {
            path.addCircle(x + radius, y + radius, radius, Path.Direction.CW)
        } else {
            val size = radius * 2
            addToPath(path, x, y, x + size, y + size, radius)
        }
    }

    @JvmOverloads
    open fun addToPath(path: Path, left: Float, top: Float, right: Float, bottom: Float,
                       size : Float = 50f, endSize: Float = size, progress: Float = 0f) {
        val topLeftSizeX = Utilities.mapRange(progress, topLeft.scale.x * size, endSize)
        val topLeftSizeY = Utilities.mapRange(progress, topLeft.scale.y * size, endSize)
        val topRightSizeX = Utilities.mapRange(progress, topRight.scale.x * size, endSize)
        val topRightSizeY = Utilities.mapRange(progress, topRight.scale.y * size, endSize)
        val bottomLeftSizeX = Utilities.mapRange(progress, bottomLeft.scale.x * size, endSize)
        val bottomLeftSizeY = Utilities.mapRange(progress, bottomLeft.scale.y * size, endSize)
        val bottomRightSizeX = Utilities.mapRange(progress, bottomRight.scale.x * size, endSize)
        val bottomRightSizeY = Utilities.mapRange(progress, bottomRight.scale.y * size, endSize)

        // Start from the bottom right corner
        path.moveTo(right, bottom - bottomRightSizeY)
        bottomRight.shape.addCorner(path, IconCornerShape.Position.BottomRight,
                                    tmpPoint.apply {
                                        x = bottomRightSizeX
                                        y = bottomRightSizeY
                                    },
                                    progress,
                                    right - bottomRightSizeX,
                                    bottom - bottomRightSizeY)

        // Move to bottom left
        addLine(path,
                right - bottomRightSizeX, bottom,
                left + bottomLeftSizeX, bottom)
        bottomLeft.shape.addCorner(path, IconCornerShape.Position.BottomLeft,
                                   tmpPoint.apply {
                                       x = bottomLeftSizeX
                                       y = bottomLeftSizeY
                                   },
                                   progress,
                                   left,
                                   bottom - bottomLeftSizeY)

        // Move to top left
        addLine(path,
                left, bottom - bottomLeftSizeY,
                left, top + topLeftSizeY)
        topLeft.shape.addCorner(path, IconCornerShape.Position.TopLeft,
                                tmpPoint.apply {
                                    x = topLeftSizeX
                                    y = topLeftSizeY
                                },
                                progress,
                                left,
                                top)

        // And then finally top right
        addLine(path,
                left + topLeftSizeX, top,
                right - topRightSizeX, top)
        topRight.shape.addCorner(path, IconCornerShape.Position.TopRight,
                                 tmpPoint.apply {
                                     x = topRightSizeX
                                     y = topRightSizeY
                                 },
                                 progress,
                                 right - topRightSizeX,
                                 top)

        path.close()
    }

    private fun addLine(path: Path, x1: Float, y1: Float, x2: Float, y2: Float) {
        if (x1 == x2 && y1 == y2) return
        path.lineTo(x2, y2)
    }

    override fun toString(): String {
        return "v1|$topLeft|$topRight|$bottomLeft|$bottomRight"
    }

    data class Corner(val shape: IconCornerShape, val scale: PointF) {

        constructor(shape: IconCornerShape, scale: Float) : this(shape, PointF(scale, scale))

        override fun toString(): String {
            return "$shape,${scale.x},${scale.y}"
        }

        companion object {

            val fullArc = Corner(IconCornerShape.arc, 1f)

            fun fromString(value: String): Corner {
                val parts = value.split(",")
                val scaleX = parts[1].toFloat()
                val scaleY = if (parts.size >= 3) parts[2].toFloat() else scaleX
                if (scaleX !in 0f..1f) error("scaleX must be in [0, 1]")
                if (scaleY !in 0f..1f) error("scaleY must be in [0, 1]")
                return Corner(IconCornerShape.fromString(parts[0]), PointF(scaleX, scaleY))
            }
        }
    }

    object Circle : IconShape(IconCornerShape.arc,
                              IconCornerShape.arc,
                              IconCornerShape.arc,
                              IconCornerShape.arc,
                              1f, 1f, 1f, 1f) {

        override fun toString(): String {
            return "circle"
        }
    }

    object Square : IconShape(IconCornerShape.arc,
                              IconCornerShape.arc,
                              IconCornerShape.arc,
                              IconCornerShape.arc,
                              .16f, .16f, .16f, .16f) {

        override val qsbEdgeRadius = R.dimen.qsb_radius_square

        override fun toString(): String {
            return "square"
        }
    }

    object RoundedSquare : IconShape(IconCornerShape.arc,
                                     IconCornerShape.arc,
                                     IconCornerShape.arc,
                                     IconCornerShape.arc,
                                     .6f, .6f, .6f, .6f) {

        override val qsbEdgeRadius = R.dimen.qsb_radius_square

        override fun toString(): String {
            return "roundedSquare"
        }
    }

    object Squircle : IconShape(IconCornerShape.squircle,
                                IconCornerShape.squircle,
                                IconCornerShape.squircle,
                                IconCornerShape.squircle,
                                1f, 1f, 1f, 1f) {

        override val qsbEdgeRadius = R.dimen.qsb_radius_squircle

        override fun toString(): String {
            return "squircle"
        }
    }

    object IOS : IconShape(IconCornerShape.arc,
                           IconCornerShape.arc,
                           IconCornerShape.arc,
                           IconCornerShape.arc,
                           0f, 0f, 0f, 0f) {

        // Apple's continuous-corner (squircle) icon mask, extracted from the
        // official iOS icon template SVG (1024x1024). Only the inner
        // rounded-rectangle subpath is kept; the outer frame subpath is
        // discarded so the path describes the filled icon shape itself.
        // The corners blend G2-continuously into the straight edges with a
        // short transition distance, matching real iOS app icons exactly.
        private const val PATH_DATA =
                "M1024 651C1024 665.24 1024 679.48 1023.92 693.73" +
                "C1023.85 705.73 1023.71 717.72 1023.39 729.71" +
                "C1022.68 755.84 1021.14 782.2 1016.5 808.05" +
                "C1011.79 834.27 1004.1 858.67 991.97 882.49" +
                "C980.05 905.9 964.48 927.33 945.9 945.9" +
                "C927.32 964.47 905.9 980.05 882.49 991.97" +
                "C858.67 1004.09 834.27 1011.79 808.05 1016.5" +
                "C782.21 1021.15 755.85 1022.68 729.71 1023.39" +
                "C717.72 1023.72 705.72 1023.85 693.73 1023.92" +
                "C679.49 1024.01 665.25 1024 651 1024" +
                "H373C358.76 1024 344.52 1024 330.27 1023.92" +
                "C318.27 1023.85 306.28 1023.71 294.29 1023.39" +
                "C268.16 1022.68 241.8 1021.14 215.95 1016.5" +
                "C189.73 1011.79 165.33 1004.1 141.51 991.97" +
                "C118.1 980.05 96.6703 964.48 78.1003 945.9" +
                "C59.5303 927.32 43.9503 905.9 32.0303 882.49" +
                "C19.9103 858.67 12.2103 834.27 7.50033 808.05" +
                "C2.85033 782.21 1.32033 755.85 0.610331 729.71" +
                "C0.280331 717.72 0.150331 705.72 0.0803306 693.73" +
                "C-0.00966939 679.49 0.000330575 665.25 0.000330575 651" +
                "V373C0.000330575 358.76 0.00033062 344.52 0.0803306 330.27" +
                "C0.150331 318.27 0.290331 306.28 0.610331 294.29" +
                "C1.32033 268.16 2.86033 241.8 7.50033 215.95" +
                "C12.2103 189.73 19.9003 165.33 32.0303 141.51" +
                "C43.9503 118.1 59.5203 96.6703 78.1003 78.1003" +
                "C96.6803 59.5303 118.1 43.9503 141.51 32.0303" +
                "C165.33 19.9103 189.73 12.2103 215.95 7.50033" +
                "C241.79 2.85033 268.15 1.32033 294.29 0.610331" +
                "C306.28 0.280331 318.28 0.150331 330.27 0.0803306" +
                "C344.51 -0.00966939 358.75 0 373 0" +
                "H651C665.24 0 679.48 0 693.73 0.0803306" +
                "C705.73 0.150331 717.72 0.290331 729.71 0.610331" +
                "C755.84 1.32033 782.2 2.86033 808.05 7.50033" +
                "C834.27 12.2103 858.67 19.9003 882.49 32.0303" +
                "C905.9 43.9503 927.33 59.5203 945.9 78.1003" +
                "C964.47 96.6803 980.05 118.1 991.97 141.51" +
                "C1004.09 165.33 1011.79 189.73 1016.5 215.95" +
                "C1021.15 241.79 1022.68 268.15 1023.39 294.29" +
                "C1023.72 306.28 1023.85 318.28 1023.92 330.27" +
                "C1024.01 344.51 1024 358.75 1024 373" +
                "V651Z"

        private val basePath by lazy {
            PathParser.createPathFromPathData(PATH_DATA)
                    ?: throw IllegalStateException("Failed to parse iOS icon mask path")
        }
        private val tmpPath = Path()
        private val tmpMatrix = Matrix()

        override val qsbEdgeRadius = R.dimen.qsb_radius_ios

        override fun getMaskPath(): Path {
            // Normalize from 1024x1024 to the 0..100 unit space used internally
            // for icon-mask comparison (see IconShapeManager.findNearestShape).
            tmpPath.reset()
            tmpMatrix.reset()
            tmpMatrix.setScale(100f / 1024f, 100f / 1024f)
            tmpPath.addPath(basePath, tmpMatrix)
            return Path(tmpPath)
        }

        override fun addShape(path: Path, x: Float, y: Float, radius: Float) {
            val scale = (radius * 2) / 1024f
            tmpPath.reset()
            tmpMatrix.reset()
            tmpMatrix.setScale(scale, scale)
            tmpMatrix.postTranslate(x, y)
            tmpPath.addPath(basePath, tmpMatrix)
            path.addPath(tmpPath)
        }

        override fun addToPath(path: Path, left: Float, top: Float, right: Float, bottom: Float,
                               size: Float, endSize: Float, progress: Float) {
            val w = right - left
            val h = bottom - top
            if (w <= 0f || h <= 0f) return
            tmpPath.reset()
            tmpMatrix.reset()
            tmpMatrix.setScale(w / 1024f, h / 1024f)
            tmpMatrix.postTranslate(left, top)
            tmpPath.addPath(basePath, tmpMatrix)
            path.addPath(tmpPath)
        }

        override fun toString(): String {
            return "ios"
        }
    }

    object Teardrop : IconShape(IconCornerShape.arc,
                                IconCornerShape.arc,
                                IconCornerShape.arc,
                                IconCornerShape.arc,
                                1f, 1f, 1f, .3f) {

        override fun toString(): String {
            return "teardrop"
        }
    }

    object Cylinder : IconShape(IconCornerShape.arc,
                                IconCornerShape.arc,
                                IconCornerShape.arc,
                                IconCornerShape.arc,
                                PointF(1f, .6f),
                                PointF(1f, .6f),
                                PointF(1f, .6f),
                                PointF(1f, .6f)) {

        override fun toString(): String {
            return "cylinder"
        }
    }

    companion object {

        fun fromString(value: String): IconShape? {
            return when (value) {
                "circle" -> Circle
                "square" -> Square
                "roundedSquare" -> RoundedSquare
                "squircle" -> Squircle
                "ios" -> IOS
                "teardrop" -> Teardrop
                "cylinder" -> Cylinder
                "" -> null
                else -> try {
                    parseCustomShape(value)
                } catch (ex: Exception) {
                    e("Error creating shape $value", ex)
                    null
                }
            }
        }

        private fun parseCustomShape(value: String): IconShape {
            val parts = value.split("|")
            if (parts[0] != "v1") error("unknown config format")
            if (parts.size != 5) error("invalid arguments size")
            return IconShape(Corner.fromString(parts[1]),
                             Corner.fromString(parts[2]),
                             Corner.fromString(parts[3]),
                             Corner.fromString(parts[4]))
        }
    }
}
