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

import android.graphics.Path
import android.graphics.PointF
import com.android.launcher3.Utilities

abstract class IconCornerShape {

    abstract fun addCorner(path: Path, position: Position, size: PointF, progress: Float, offsetX: Float, offsetY: Float)

    abstract class BaseBezierPath : IconCornerShape() {

        protected abstract val controlDistance: Float
        protected val roundControlDistance = .447771526f

        private fun getControl1X(position: Position, controlDistance: Float): Float {
            return Utilities.mapRange(controlDistance, position.controlX, position.startX)
        }

        private fun getControl1Y(position: Position, controlDistance: Float): Float {
            return Utilities.mapRange(controlDistance, position.controlY, position.startY)
        }

        private fun getControl2X(position: Position, controlDistance: Float): Float {
            return Utilities.mapRange(controlDistance, position.controlX, position.endX)
        }

        private fun getControl2Y(position: Position, controlDistance: Float): Float {
            return Utilities.mapRange(controlDistance, position.controlY, position.endY)
        }

        override fun addCorner(path: Path, position: Position, size: PointF, progress: Float,
                               offsetX: Float, offsetY: Float) {
            val controlDistance = Utilities.mapRange(progress, controlDistance, roundControlDistance)
            path.cubicTo(
                    getControl1X(position, controlDistance) * size.x + offsetX,
                    getControl1Y(position, controlDistance) * size.y + offsetY,
                    getControl2X(position, controlDistance) * size.x + offsetX,
                    getControl2Y(position, controlDistance) * size.y + offsetY,
                    position.endX * size.x + offsetX,
                    position.endY * size.y + offsetY)
        }
    }

    class Cut : BaseBezierPath() {

        override val controlDistance = 1f

        override fun addCorner(path: Path, position: Position, size: PointF, progress: Float,
                               offsetX: Float, offsetY: Float) {
            if (progress == 0f) {
                path.lineTo(
                        position.endX * size.x + offsetX,
                        position.endY * size.y + offsetY)
            } else {
                super.addCorner(path, position, size, progress, offsetX, offsetY)
            }
        }

        override fun toString(): String {
            return "cut"
        }
    }

    class Squircle : BaseBezierPath() {

        override val controlDistance = .2f

        override fun toString(): String {
            return "squircle"
        }
    }

    class Arc : BaseBezierPath() {

        override val controlDistance = roundControlDistance

        override fun toString(): String {
            return "arc"
        }
    }

    /**
     * True superellipse corner: samples |x/r|^n + |y/r|^n = 1 (n > 2) with
     * multiple cubic Bezier segments. Unlike the single-segment [Squircle],
     * this yields G2-continuous curvature at the seam with the straight edges
     * (curvature is zero at both endpoints), so the transition between the
     * rounded corner and the straight sides is smooth with no visible kink.
     *
     * Under Lawnchair's scale = 1.0 (full-corner) geometry the superellipse
     * spans the entire edge, so a larger exponent makes the corners sharper
     * (n -> infinity approaches a square). The default exponent n = 3 keeps
     * the corner visibly rounded while preserving the continuous-corner
     * smoothness characteristic of iOS app icons.
     */
    class Superellipse(private val exponent: Float = 3f) : IconCornerShape() {

        override fun addCorner(path: Path, position: Position, size: PointF, progress: Float,
                               offsetX: Float, offsetY: Float) {
            val startX = position.startX * size.x + offsetX
            val startY = position.startY * size.y + offsetY
            val endX = position.endX * size.x + offsetX
            val endY = position.endY * size.y + offsetY
            val cornerX = position.controlX * size.x + offsetX
            val cornerY = position.controlY * size.y + offsetY

            // Edge vectors radiating from the corner point C toward S and E.
            val d1x = startX - cornerX
            val d1y = startY - cornerY
            val d2x = endX - cornerX
            val d2y = endY - cornerY
            val sizeD1 = Math.sqrt((d1x * d1x + d1y * d1y).toDouble()).toFloat()
            val sizeD2 = Math.sqrt((d2x * d2x + d2y * d2y).toDouble()).toFloat()
            if (sizeD1 <= 0f || sizeD2 <= 0f) {
                path.lineTo(endX, endY)
                return
            }
            val u1x = d1x / sizeD1
            val u1y = d1y / sizeD1
            val u2x = d2x / sizeD2
            val u2y = d2y / sizeD2

            // During shape-change animations (progress 0 -> 1), morph toward a
            // plain circular arc (n = 2) to match the other shapes' behavior.
            val n = Utilities.mapRange(progress, exponent, 2f)
            val invN = 2.0 / n.toDouble()

            // Sample the superellipse arc. With u = cos(t)^(2/n) and
            // v = sin(t)^(2/n) satisfying u^n + v^n = 1, we map it to the
            // corner as r1 = sizeD1 * (1 - v) and r2 = sizeD2 * (1 - u), so
            // t = 0 -> S and t = pi/2 -> E, with the curve bowing toward C
            // (deeper than a circular arc when n > 2).
            val segments = 8
            val pointCount = segments + 1
            val px = FloatArray(pointCount)
            val py = FloatArray(pointCount)
            for (i in 0 until pointCount) {
                val t = (Math.PI / 2) * (i.toDouble() / segments)
                val sinT = Math.sin(t)
                val cosT = Math.cos(t)
                val r1 = sizeD1 * (1.0 - Math.pow(sinT, invN))
                val r2 = sizeD2 * (1.0 - Math.pow(cosT, invN))
                px[i] = (cornerX + r1 * u1x + r2 * u2x).toFloat()
                py[i] = (cornerY + r1 * u1y + r2 * u2y).toFloat()
            }

            // Stitch the samples into a C1-continuous spline using
            // Catmull-Rom -> cubic Bezier conversion, with reflected ghost
            // points at the endpoints so the spline tangents there follow
            // the straight edges (tangent along -u1 at S, along +u2 at E).
            for (i in 0 until segments) {
                val p1x = px[i]
                val p1y = py[i]
                val p2x = px[i + 1]
                val p2y = py[i + 1]
                val p0x = if (i == 0) 2 * p1x - p2x else px[i - 1]
                val p0y = if (i == 0) 2 * p1y - p2y else py[i - 1]
                val p3x = if (i + 2 >= pointCount) 2 * p2x - p1x else px[i + 2]
                val p3y = if (i + 2 >= pointCount) 2 * p2y - p1y else py[i + 2]
                val c1x = p1x + (p2x - p0x) / 6f
                val c1y = p1y + (p2y - p0y) / 6f
                val c2x = p2x - (p3x - p1x) / 6f
                val c2y = p2y - (p3y - p1y) / 6f
                path.cubicTo(c1x, c1y, c2x, c2y, p2x, p2y)
            }
        }

        override fun toString(): String {
            return "superellipse"
        }
    }

    sealed class Position {

        abstract val startX: Float
        abstract val startY: Float

        abstract val controlX: Float
        abstract val controlY: Float

        abstract val endX: Float
        abstract val endY: Float

        object TopLeft : Position() {

            override val startX = 0f
            override val startY = 1f
            override val controlX = 0f
            override val controlY = 0f
            override val endX = 1f
            override val endY = 0f
        }

        object TopRight : Position() {

            override val startX = 0f
            override val startY = 0f
            override val controlX = 1f
            override val controlY = 0f
            override val endX = 1f
            override val endY = 1f
        }

        object BottomRight : Position() {

            override val startX = 1f
            override val startY = 0f
            override val controlX = 1f
            override val controlY = 1f
            override val endX = 0f
            override val endY = 1f
        }

        object BottomLeft : Position() {

            override val startX = 1f
            override val startY = 1f
            override val controlX = 0f
            override val controlY = 1f
            override val endX = 0f
            override val endY = 0f
        }
    }

    companion object {

        val cut = Cut()
        val squircle = Squircle()
        val arc = Arc()
        val superellipse = Superellipse()

        fun fromString(value: String): IconCornerShape {
            return when (value) {
                "cut" -> cut
                "cubic", "squircle" -> squircle
                "arc" -> arc
                "superellipse", "ios" -> superellipse
                else -> error("invalid corner shape $value")
            }
        }
    }
}
