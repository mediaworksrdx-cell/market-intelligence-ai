package com.marketintelligence.ai.ui.welcome

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.R
import kotlinx.coroutines.delay

/**
 * 4-Phase Institutional Welcome Screen Animator for Market Intelligence AI
 * Phase 1: Cyber-Falcon Spring & Elevation
 * Phase 2: Ambient Breathing Halo (Continuous)
 * Phase 3: Swiss Executive Typography Stagger
 * Phase 4: Quantitative Shimmer Progress & Completion Handoff
 */
@Composable
fun MarketIntelligenceWelcomeScreen(
    onAnimationComplete: () -> Unit = {}
) {
    // Phase 1: Mascot Spring & Elevation
    val mascotScale = remember { Animatable(0.45f) }
    val mascotAlpha = remember { Animatable(0f) }
    val mascotOffsetY = remember { Animatable(25f) }

    // Phase 3: Typography & Subtitle Slide-Up
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(30f) }
    val subtitleAlpha = remember { Animatable(0f) }

    // Phase 4: Shimmer Loading Progress
    val progressFraction = remember { Animatable(0f) }

    // Phase 2: Ambient Breathing Halo & Radar Rings (Continuous)
    val infiniteTransition = rememberInfiniteTransition(label = "ambientAura")

    val cyanAuraScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cyanAuraScale"
    )
    val cyanAuraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cyanAuraAlpha"
    )

    val goldAuraScale by infiniteTransition.animateFloat(
        initialValue = 1.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "goldAuraScale"
    )
    val goldAuraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "goldAuraAlpha"
    )

    // Concentric Radar Expansion Wave
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    // Institutional Color Palette (Exact Midnight Navy matching user reference)
    val deepNavyBg = Brush.verticalGradient(
        listOf(Color(0xFF0A1A33), Color(0xFF0F2549), Color(0xFF15305C))
    )
    val gold24K = Color(0xFFF5AF19)
    val iceWhite = Color(0xFFFFFFFF)
    val electricCyan = Color(0xFF00F0FF)
    val slateMuted = Color(0xFFCBD5E1)

    // 4-Phase Choreography Pipeline
    LaunchedEffect(Unit) {
        // Phase 1: Mascot Spring & Elevation (0ms - 800ms)
        mascotAlpha.animateTo(1f, tween(500, easing = LinearOutSlowInEasing))
        mascotOffsetY.animateTo(0f, tween(750, easing = FastOutSlowInEasing))
        mascotScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        // Phase 3: Typography Staggered Reveal (500ms - 1200ms)
        titleOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
        titleAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        subtitleAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))

        // Phase 4: Shimmer Loading Bar Fills to 100% (1000ms - 2200ms)
        progressFraction.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )

        // Hand-off to dashboard
        delay(250)
        onAnimationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(deepNavyBg),
        contentAlignment = Alignment.Center
    ) {
        // Background Subtle Technical Coordinate Grid
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.04f)) {
            val step = 48.dp.toPx()
            for (x in 0..size.width.toInt() step step.toInt()) {
                drawLine(
                    color = electricCyan,
                    start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f),
                    end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..size.height.toInt() step step.toInt()) {
                drawLine(
                    color = electricCyan,
                    start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()),
                    end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()),
                    strokeWidth = 1f
                )
            }
        }

        // Concentric Sonic / Radar Rings
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .scale(ringScale)
                .alpha(ringAlpha)
        ) {
            drawCircle(
                color = electricCyan.copy(alpha = 0.4f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Dual Ambient Breathing Halo (Cyan + Gold)
        Canvas(
            modifier = Modifier
                .size(340.dp)
                .scale(cyanAuraScale)
                .alpha(cyanAuraAlpha)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        electricCyan.copy(alpha = 0.45f),
                        electricCyan.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension / 1.5f
                )
            )
        }

        Canvas(
            modifier = Modifier
                .size(280.dp)
                .scale(goldAuraScale)
                .alpha(goldAuraAlpha)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        gold24K.copy(alpha = 0.35f),
                        gold24K.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension / 1.7f
                )
            )
        }

        // Center Mascot & Lockup
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Mascot Emblem
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .offset(y = mascotOffsetY.value.dp)
                    .scale(mascotScale.value)
                    .alpha(mascotAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_falcon_emblem),
                    contentDescription = "Market Intelligence AI Falcon",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Typography: MARKET INTELLIGENCE AI - MI007
            Column(
                modifier = Modifier
                    .offset(y = titleOffsetY.value.dp)
                    .alpha(titleAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MARKET",
                        color = gold24K,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "INTELLIGENCE",
                        color = iceWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AI",
                        color = electricCyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "-",
                        color = slateMuted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "MI 007",
                        color = gold24K,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle / Tagline
            Text(
                text = "AUTONOMOUS MARKET INTELLIGENCE",
                color = slateMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.8.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Phase 4: Quantitative Shimmer Progress Bar
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .alpha(subtitleAlpha.value)
                    .background(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction.value)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(electricCyan, gold24K)
                            ),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
        }

        // Bottom Screen Branding: A SYNTHETIX ANALYTICS PRODUCT
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .alpha(subtitleAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "A SYNTHETIX ANALYTICS PRODUCT",
                color = electricCyan.copy(alpha = 0.85f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.0.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
