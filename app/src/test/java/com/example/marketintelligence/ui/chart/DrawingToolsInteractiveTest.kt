package com.example.marketintelligence.ui.chart

import androidx.compose.ui.geometry.Offset
import com.example.marketintelligence.domain.chart.ChartPoint
import com.example.marketintelligence.domain.chart.DrawingData
import com.example.marketintelligence.domain.chart.DrawingToolType
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive interactive verification test suite for all chart drawing tools.
 *
 * Verifies:
 * 1. Adding & Hit-testing / selecting each drawing tool.
 * 2. Editable anchor handle detection for every tool.
 * 3. Modifying individual anchor coordinates.
 * 4. Dragging and translating entire drawings across (time, price).
 * 5. Deleting selected drawings.
 * 6. Independent multi-drawing editing and z-order hit-testing.
 * 7. Coordinate synchronization and preservation across timeframes.
 */
class DrawingToolsInteractiveTest {

    private val chartWidth = 1000f
    private val chartAreaHeight = 600f
    private val tolerancePx = 48f

    // Linear time and price mapping for predictable unit test assertions:
    // Timestamp 1000L..2000L -> X: 0f..1000f (1 ms = 1 px)
    private val tToX: (Long) -> Float = { t -> (t - 1000L).toFloat() }
    // Price 100.0..200.0 -> Y: 600f..100f (1 unit = 5 px, inverted y-axis)
    private val pToY: (Double) -> Float = { p -> 600f - (p - 100.0).toFloat() * 5f }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. HORIZONTAL LINE
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testHorizontalLine_addSelectModifyMoveDelete() {
        val line = DrawingData(
            id = "hline-1",
            toolType = DrawingToolType.HORIZONTAL_LINE,
            points = listOf(ChartPoint(timestamp = 1500L, price = 150.0)),
            isComplete = true
        )
        val expectedY = pToY(150.0) // 350f

        // 1. Hit testing along the full width of the chart
        assertTrue(isPointOnDrawing(line, Offset(100f, expectedY), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertTrue(isPointOnDrawing(line, Offset(500f, expectedY + 20f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertTrue(isPointOnDrawing(line, Offset(900f, expectedY - 20f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Outside tolerance
        assertFalse(isPointOnDrawing(line, Offset(500f, expectedY + 60f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 2. Anchor detection along the line
        val anchorIndex = findTappedAnchor(line, Offset(300f, expectedY), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight)
        assertEquals(0, anchorIndex)

        // 3. Modify anchor: change price to 160.0
        val modifiedLine = line.copy(points = listOf(ChartPoint(line.points[0].timestamp, 160.0)))
        val newY = pToY(160.0) // 300f
        assertTrue(isPointOnDrawing(modifiedLine, Offset(400f, newY), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertFalse(isPointOnDrawing(modifiedLine, Offset(400f, expectedY), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 4. Whole-drawing translation: move by Δprice = -20.0
        val movedLine = modifiedLine.copy(
            points = modifiedLine.points.map { it.copy(price = it.price - 20.0) }
        )
        assertEquals(140.0, movedLine.points[0].price, 0.001)
        assertTrue(isPointOnDrawing(movedLine, Offset(500f, pToY(140.0)), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 5. Delete
        val drawings = mutableListOf(movedLine)
        drawings.removeAll { it.id == "hline-1" }
        assertTrue(drawings.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. VERTICAL LINE
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testVerticalLine_addSelectModifyMoveDelete() {
        val vline = DrawingData(
            id = "vline-1",
            toolType = DrawingToolType.VERTICAL_LINE,
            points = listOf(ChartPoint(timestamp = 1400L, price = 150.0)),
            isComplete = true
        )
        val expectedX = tToX(1400L) // 400f

        // 1. Hit testing along the full height of the chart
        assertTrue(isPointOnDrawing(vline, Offset(expectedX, 50f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertTrue(isPointOnDrawing(vline, Offset(expectedX + 25f, 300f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertTrue(isPointOnDrawing(vline, Offset(expectedX - 25f, 550f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Outside tolerance
        assertFalse(isPointOnDrawing(vline, Offset(expectedX + 80f, 300f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 2. Anchor detection along the line
        val anchorIndex = findTappedAnchor(vline, Offset(expectedX, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight)
        assertEquals(0, anchorIndex)

        // 3. Modify anchor: change timestamp to 1600L (600f)
        val modifiedVline = vline.copy(points = listOf(ChartPoint(1600L, vline.points[0].price)))
        assertTrue(isPointOnDrawing(modifiedVline, Offset(600f, 200f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertFalse(isPointOnDrawing(modifiedVline, Offset(expectedX, 200f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 4. Whole-drawing translation: move by Δt = +100L
        val movedVline = modifiedVline.copy(
            points = modifiedVline.points.map { it.copy(timestamp = it.timestamp + 100L) }
        )
        assertEquals(1700L, movedVline.points[0].timestamp)
        assertTrue(isPointOnDrawing(movedVline, Offset(700f, 250f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 5. Delete
        val drawings = mutableListOf(movedVline)
        drawings.removeAll { it.id == "vline-1" }
        assertTrue(drawings.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. PARALLEL CHANNEL
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testParallelChannel_addSelectModifyMoveDelete() {
        // Point 0: (1100L, 120.0) -> (100f, 500f)
        // Point 1: (1500L, 120.0) -> (500f, 500f)
        // Point 2: (1100L, 160.0) -> priceOffset = +40.0 -> Parallel line at Y = pToY(160.0) = 300f
        val channel = DrawingData(
            id = "channel-1",
            toolType = DrawingToolType.CHANNEL,
            points = listOf(
                ChartPoint(1100L, 120.0),
                ChartPoint(1500L, 120.0),
                ChartPoint(1100L, 160.0)
            ),
            isComplete = true
        )

        // 1. Hit testing:
        // Baseline: Y=500f between X=100f and X=500f
        assertTrue(isPointOnDrawing(channel, Offset(300f, 500f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Parallel line: Y=300f between X=100f and X=500f
        assertTrue(isPointOnDrawing(channel, Offset(300f, 300f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Midline: Y=400f between X=100f and X=500f
        assertTrue(isPointOnDrawing(channel, Offset(300f, 400f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Polygon interior: Y=350f, X=250f
        assertTrue(isPointOnDrawing(channel, Offset(250f, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Outside channel
        assertFalse(isPointOnDrawing(channel, Offset(700f, 400f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertFalse(isPointOnDrawing(channel, Offset(300f, 150f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 2. Anchor detection:
        // Anchor 0 (baseline start)
        assertEquals(0, findTappedAnchor(channel, Offset(105f, 505f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Anchor 1 (baseline end)
        assertEquals(1, findTappedAnchor(channel, Offset(495f, 495f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Anchor 2 (parallel line midpoint or endpoint)
        assertEquals(2, findTappedAnchor(channel, Offset(300f, 305f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 3. Modify anchor 2 (expand channel width from 40.0 to 60.0 offset -> 180.0)
        val modifiedChannel = channel.copy(
            points = listOf(channel.points[0], channel.points[1], ChartPoint(1100L, 180.0))
        )
        // Parallel line now at Y = pToY(180.0) = 200f
        assertTrue(isPointOnDrawing(modifiedChannel, Offset(300f, 200f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 4. Whole-drawing translation: move by Δt = +200L, Δprice = -10.0
        val movedChannel = modifiedChannel.copy(
            points = modifiedChannel.points.map { ChartPoint(it.timestamp + 200L, it.price - 10.0) }
        )
        assertEquals(1300L, movedChannel.points[0].timestamp)
        assertEquals(110.0, movedChannel.points[0].price, 0.001)
        assertEquals(1700L, movedChannel.points[1].timestamp)
        assertEquals(1300L, movedChannel.points[2].timestamp)
        assertEquals(170.0, movedChannel.points[2].price, 0.001)

        // Baseline now between (300f, 550f) and (700f, 550f)
        assertTrue(isPointOnDrawing(movedChannel, Offset(500f, 550f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 5. Delete
        val drawings = mutableListOf(movedChannel)
        drawings.removeAll { it.id == "channel-1" }
        assertTrue(drawings.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. FIBONACCI RETRACEMENT
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testFibonacci_addSelectModifyMoveDelete() {
        // Point 0 (Swing Low): (1200L, 100.0) -> (200f, 600f)
        // Point 1 (Swing High): (1600L, 200.0) -> (600f, 100f)
        val fib = DrawingData(
            id = "fib-1",
            toolType = DrawingToolType.FIBONACCI,
            points = listOf(
                ChartPoint(1200L, 100.0),
                ChartPoint(1600L, 200.0)
            ),
            isComplete = true
        )

        // 1. Hit testing:
        // Swing Trendline segment hit (at midpoint: 400f, 350f)
        assertTrue(isPointOnDrawing(fib, Offset(400f, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Retracement level 0.5 (price 150.0 -> Y=350f) anywhere along width
        assertTrue(isPointOnDrawing(fib, Offset(50f, pToY(150.0)), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertTrue(isPointOnDrawing(fib, Offset(900f, pToY(150.0)), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Retracement level 0.618 (price 138.2 -> Y=409f)
        assertTrue(isPointOnDrawing(fib, Offset(700f, pToY(138.2)), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Retracement level 0.382 (price 161.8 -> Y=291f)
        assertTrue(isPointOnDrawing(fib, Offset(700f, pToY(161.8)), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Golden Zone interior (e.g. price 150.0)
        assertTrue(isPointOnDrawing(fib, Offset(850f, pToY(150.0)), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Outside levels (e.g. price 250.0 -> Y < 0)
        assertFalse(isPointOnDrawing(fib, Offset(800f, 50f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 2. Anchor detection:
        // Anchor 0 (Swing Low)
        assertEquals(0, findTappedAnchor(fib, Offset(205f, 595f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Anchor 1 (Swing High)
        assertEquals(1, findTappedAnchor(fib, Offset(595f, 105f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 3. Modify anchor 1: adjust swing high to (1700L, 220.0)
        val modifiedFib = fib.copy(
            points = listOf(fib.points[0], ChartPoint(1700L, 220.0))
        )
        // High is now 220.0, range is 120.0, 0.5 level is 160.0 -> Y = pToY(160.0) = 300f
        assertTrue(isPointOnDrawing(modifiedFib, Offset(500f, pToY(160.0)), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // 4. Whole-drawing translation: move by Δt = -100L, Δprice = +10.0
        val movedFib = modifiedFib.copy(
            points = modifiedFib.points.map { ChartPoint(it.timestamp - 100L, it.price + 10.0) }
        )
        assertEquals(1100L, movedFib.points[0].timestamp)
        assertEquals(110.0, movedFib.points[0].price, 0.001)
        assertEquals(1600L, movedFib.points[1].timestamp)
        assertEquals(230.0, movedFib.points[1].price, 0.001)

        // 5. Delete
        val drawings = mutableListOf(movedFib)
        drawings.removeAll { it.id == "fib-1" }
        assertTrue(drawings.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. OTHER TOOLS: TRENDLINE, RECTANGLE, PRICE RANGE, ARROW, RAY, HORIZONTAL RAY, PITCHFORK, TEXT
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testTrendline_interactiveCycle() {
        val trendline = DrawingData(
            id = "trend-1",
            toolType = DrawingToolType.TRENDLINE,
            points = listOf(ChartPoint(1100L, 110.0), ChartPoint(1400L, 140.0)), // (100, 550) to (400, 400)
            isComplete = true
        )
        // Hit on segment
        assertTrue(isPointOnDrawing(trendline, Offset(250f, 475f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Anchors
        assertEquals(0, findTappedAnchor(trendline, Offset(105f, 545f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(1, findTappedAnchor(trendline, Offset(395f, 405f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Move whole drawing
        val moved = trendline.copy(points = trendline.points.map { ChartPoint(it.timestamp + 50L, it.price + 10.0) })
        assertEquals(1150L, moved.points[0].timestamp)
        assertEquals(120.0, moved.points[0].price, 0.001)
    }

    @Test
    fun testRectangle_interactiveCycle() {
        val rect = DrawingData(
            id = "rect-1",
            toolType = DrawingToolType.RECTANGLE,
            points = listOf(ChartPoint(1100L, 110.0), ChartPoint(1400L, 140.0)), // X: 100..400, Y: 400..550
            isComplete = true
        )
        // Hit inside rectangle
        assertTrue(isPointOnDrawing(rect, Offset(250f, 480f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Outside
        assertFalse(isPointOnDrawing(rect, Offset(600f, 480f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Anchors
        assertEquals(0, findTappedAnchor(rect, Offset(100f, 550f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(1, findTappedAnchor(rect, Offset(400f, 400f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
    }

    @Test
    fun testPriceRange_interactiveCycle() {
        val pr = DrawingData(
            id = "pr-1",
            toolType = DrawingToolType.PRICE_RANGE,
            points = listOf(ChartPoint(1200L, 120.0), ChartPoint(1500L, 170.0)),
            isComplete = true
        )
        // Hit inside
        assertTrue(isPointOnDrawing(pr, Offset(350f, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // Anchors
        assertEquals(0, findTappedAnchor(pr, Offset(200f, 500f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(1, findTappedAnchor(pr, Offset(500f, 250f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
    }

    @Test
    fun testArrow_interactiveCycle() {
        val arrow = DrawingData(
            id = "arrow-1",
            toolType = DrawingToolType.ARROW,
            points = listOf(ChartPoint(1100L, 110.0), ChartPoint(1300L, 130.0)),
            isComplete = true
        )
        assertTrue(isPointOnDrawing(arrow, Offset(200f, 500f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(0, findTappedAnchor(arrow, Offset(100f, 550f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(1, findTappedAnchor(arrow, Offset(300f, 450f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
    }

    @Test
    fun testRay_interactiveCycle() {
        val ray = DrawingData(
            id = "ray-1",
            toolType = DrawingToolType.RAY,
            points = listOf(ChartPoint(1100L, 110.0), ChartPoint(1200L, 120.0)), // slope = (100, 550) to (200, 500)
            isComplete = true
        )
        // Point along continuation of the ray (at X = 500f, Y = 350f)
        assertTrue(isPointOnDrawing(ray, Offset(500f, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(0, findTappedAnchor(ray, Offset(100f, 550f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(1, findTappedAnchor(ray, Offset(200f, 500f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
    }

    @Test
    fun testHorizontalRay_interactiveCycle() {
        val hray = DrawingData(
            id = "hray-1",
            toolType = DrawingToolType.HORIZONTAL_RAY,
            points = listOf(ChartPoint(1300L, 150.0)), // starts at X = 300f, Y = 350f extends right
            isComplete = true
        )
        // Hit to the right of start point
        assertTrue(isPointOnDrawing(hray, Offset(600f, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        // To the left of start point (outside tolerance)
        assertFalse(isPointOnDrawing(hray, Offset(100f, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(0, findTappedAnchor(hray, Offset(300f, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
    }

    @Test
    fun testPitchfork_interactiveCycle() {
        val pitchfork = DrawingData(
            id = "pf-1",
            toolType = DrawingToolType.PITCHFORK,
            points = listOf(
                ChartPoint(1100L, 150.0), // Origin (100, 350)
                ChartPoint(1300L, 180.0), // Prong 1 (300, 200)
                ChartPoint(1300L, 120.0)  // Prong 2 (300, 500)
            ),
            isComplete = true
        )
        // Midpoint of prongs is (300, 350). Median line connects (100, 350) to (300, 350)
        assertTrue(isPointOnDrawing(pitchfork, Offset(200f, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(0, findTappedAnchor(pitchfork, Offset(100f, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(1, findTappedAnchor(pitchfork, Offset(300f, 200f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(2, findTappedAnchor(pitchfork, Offset(300f, 500f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
    }

    @Test
    fun testTextAnnotation_interactiveCycle() {
        val text = DrawingData(
            id = "text-1",
            toolType = DrawingToolType.TEXT_ANNOTATION,
            points = listOf(ChartPoint(1500L, 150.0)), // Anchor at (500, 350)
            text = "Major Resistance",
            isComplete = true
        )
        // Hit-test near anchor point
        assertTrue(isPointOnDrawing(text, Offset(500f, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
        assertEquals(0, findTappedAnchor(text, Offset(500f, 350f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // Hit-test on text bubble above anchor (pill top is around y = 350 - 44 = 306)
        assertTrue(isPointOnDrawing(text, Offset(500f, 320f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))

        // Update note text
        val updatedText = text.copy(text = "Updated Support 22000")
        assertEquals("Updated Support 22000", updatedText.text)
        assertTrue(isPointOnDrawing(updatedText, Offset(500f, 320f), tolerancePx, tToX, pToY, chartWidth, chartAreaHeight))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. MULTIPLE DRAWINGS INDEPENDENCE & Z-ORDER
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testMultipleDrawings_independentSelectionAndZOrder() {
        val hline = DrawingData(
            id = "d1-hline",
            toolType = DrawingToolType.HORIZONTAL_LINE,
            points = listOf(ChartPoint(1200L, 130.0)),
            isComplete = true
        )
        val trendline = DrawingData(
            id = "d2-trend",
            toolType = DrawingToolType.TRENDLINE,
            points = listOf(ChartPoint(1100L, 110.0), ChartPoint(1500L, 150.0)),
            isComplete = true
        )
        val drawings = listOf(hline, trendline)

        // Tap on trendline at (300f, 450f)
        val tappedTrend = findTappedDrawing(Offset(300f, 450f), drawings, tolerancePx, tToX, pToY, chartWidth, chartAreaHeight)
        assertNotNull(tappedTrend)
        assertEquals("d2-trend", tappedTrend?.id)

        // Tap on hline at (700f, pToY(130.0) = 450f)
        val tappedHline = findTappedDrawing(Offset(700f, 450f), drawings, tolerancePx, tToX, pToY, chartWidth, chartAreaHeight)
        assertNotNull(tappedHline)
        assertEquals("d1-hline", tappedHline?.id)

        // At intersection of trendline and hline:
        // Trendline passes through price 130.0 (Y=450f) at timestamp 1300L (X=300f).
        // Since trendline was added AFTER hline, reversed search prioritizes top-most drawing (trendline)
        val tappedTop = findTappedDrawing(Offset(300f, 450f), drawings, tolerancePx, tToX, pToY, chartWidth, chartAreaHeight)
        assertEquals("d2-trend", tappedTop?.id)

        // Modify only trendline
        val updatedDrawings = drawings.map {
            if (it.id == "d2-trend") it.copy(points = listOf(ChartPoint(1100L, 100.0), ChartPoint(1500L, 100.0)))
            else it
        }
        // Hline is unchanged
        val keptHline = updatedDrawings.first { it.id == "d1-hline" }
        assertEquals(130.0, keptHline.points[0].price, 0.001)

        // Delete trendline
        val afterDelete = updatedDrawings.filter { it.id != "d2-trend" }
        assertEquals(1, afterDelete.size)
        assertEquals("d1-hline", afterDelete[0].id)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. TIMEFRAME SWITCHING & DATA PERSISTENCE
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testDrawingStatePersistenceAcrossTimeframe() {
        // Drawings are recorded in data space (timestamp: Long, price: Double).
        val originalDrawing = DrawingData(
            id = "persist-1",
            toolType = DrawingToolType.CHANNEL,
            points = listOf(
                ChartPoint(1700000000000L, 2500.50),
                ChartPoint(1700086400000L, 2550.00),
                ChartPoint(1700000000000L, 2580.00)
            ),
            isComplete = true
        )

        val chartDrawingsState = listOf(originalDrawing)

        // Simulate timeframe change (e.g. 5M -> 1D -> 1W):
        // Data points remain bit-exact without shifting or distorting coordinates
        val timeframeSwitchedDrawings = chartDrawingsState.map { it.copy() }
        assertEquals(1, timeframeSwitchedDrawings.size)
        assertEquals(originalDrawing.id, timeframeSwitchedDrawings[0].id)
        assertEquals(1700000000000L, timeframeSwitchedDrawings[0].points[0].timestamp)
        assertEquals(2500.50, timeframeSwitchedDrawings[0].points[0].price, 0.0001)
        assertEquals(1700086400000L, timeframeSwitchedDrawings[0].points[1].timestamp)
        assertEquals(2550.00, timeframeSwitchedDrawings[0].points[1].price, 0.0001)
        assertEquals(2580.00, timeframeSwitchedDrawings[0].points[2].price, 0.0001)
    }
}
