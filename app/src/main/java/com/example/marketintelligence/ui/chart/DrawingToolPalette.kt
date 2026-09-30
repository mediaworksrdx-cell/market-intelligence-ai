package com.example.marketintelligence.ui.chart

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.chart.DrawingToolType

/**
 * Floating palette for selecting drawing tools.
 */
@Composable
fun DrawingToolPalette(
    activeDrawingTool: DrawingToolType,
    onToolSelected: (DrawingToolType) -> Unit,
    onClearDrawings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "DRAWING TOOLS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = colors.onBackground
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = colors.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Tool Grid ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DrawingToolButton(
                    tool = DrawingToolType.NONE,
                    label = "Select",
                    icon = Icons.Default.TouchApp,
                    isActive = activeDrawingTool == DrawingToolType.NONE,
                    accentColor = colors.onSurface,
                    onClick = { onToolSelected(DrawingToolType.NONE) }
                )
                DrawingToolButton(
                    tool = DrawingToolType.TRENDLINE,
                    label = "Trend",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    isActive = activeDrawingTool == DrawingToolType.TRENDLINE,
                    accentColor = Color(0xFF42A5F5),
                    onClick = { onToolSelected(DrawingToolType.TRENDLINE) }
                )
                DrawingToolButton(
                    tool = DrawingToolType.HORIZONTAL_LINE,
                    label = "H-Line",
                    icon = Icons.Default.HorizontalRule,
                    isActive = activeDrawingTool == DrawingToolType.HORIZONTAL_LINE,
                    accentColor = Color(0xFFFFB74D),
                    onClick = { onToolSelected(DrawingToolType.HORIZONTAL_LINE) }
                )
                DrawingToolButton(
                    tool = DrawingToolType.VERTICAL_LINE,
                    label = "V-Line",
                    icon = Icons.Default.Height,
                    isActive = activeDrawingTool == DrawingToolType.VERTICAL_LINE,
                    accentColor = Color(0xFFFF8A65),
                    onClick = { onToolSelected(DrawingToolType.VERTICAL_LINE) }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DrawingToolButton(
                    tool = DrawingToolType.RAY,
                    label = "Ray",
                    icon = Icons.Default.ArrowForward,
                    isActive = activeDrawingTool == DrawingToolType.RAY,
                    accentColor = Color(0xFFBA68C8),
                    onClick = { onToolSelected(DrawingToolType.RAY) }
                )
                DrawingToolButton(
                    tool = DrawingToolType.FIBONACCI,
                    label = "Fib",
                    icon = Icons.Default.AutoGraph,
                    isActive = activeDrawingTool == DrawingToolType.FIBONACCI,
                    accentColor = Color(0xFFCE93D8),
                    onClick = { onToolSelected(DrawingToolType.FIBONACCI) }
                )
                DrawingToolButton(
                    tool = DrawingToolType.FIBONACCI_EXTENSION,
                    label = "Fib Ext",
                    icon = Icons.Default.Insights,
                    isActive = activeDrawingTool == DrawingToolType.FIBONACCI_EXTENSION,
                    accentColor = Color(0xFF9575CD),
                    onClick = { onToolSelected(DrawingToolType.FIBONACCI_EXTENSION) }
                )
                DrawingToolButton(
                    tool = DrawingToolType.PITCHFORK,
                    label = "Pitch",
                    icon = Icons.Default.ForkRight,
                    isActive = activeDrawingTool == DrawingToolType.PITCHFORK,
                    accentColor = Color(0xFF7986CB),
                    onClick = { onToolSelected(DrawingToolType.PITCHFORK) }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DrawingToolButton(
                    tool = DrawingToolType.RECTANGLE,
                    label = "Rect",
                    icon = Icons.Default.CropSquare,
                    isActive = activeDrawingTool == DrawingToolType.RECTANGLE,
                    accentColor = Color(0xFF81C784),
                    onClick = { onToolSelected(DrawingToolType.RECTANGLE) }
                )
                DrawingToolButton(
                    tool = DrawingToolType.CHANNEL,
                    label = "Channel",
                    icon = Icons.Default.LinearScale,
                    isActive = activeDrawingTool == DrawingToolType.CHANNEL,
                    accentColor = Color(0xFF4FC3F7),
                    onClick = { onToolSelected(DrawingToolType.CHANNEL) }
                )
                DrawingToolButton(
                    tool = DrawingToolType.TEXT_ANNOTATION,
                    label = "Text",
                    icon = Icons.Default.TextFields,
                    isActive = activeDrawingTool == DrawingToolType.TEXT_ANNOTATION,
                    accentColor = Color(0xFFFFD54F),
                    onClick = { onToolSelected(DrawingToolType.TEXT_ANNOTATION) }
                )

                // Clear all drawings
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClearDrawings() }
                        .padding(8.dp)
                        .width(60.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Clear All",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Clear",
                        fontSize = 9.sp,
                        color = Color(0xFFEF5350),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun DrawingToolButton(
    tool: DrawingToolType,
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isActive) accentColor.copy(alpha = 0.15f) else Color.Transparent,
        label = "dtBg"
    )
    val borderColor = if (isActive) accentColor else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
            .width(60.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 9.sp,
            color = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}
