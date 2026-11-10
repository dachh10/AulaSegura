package com.example.aulasegura.ui.screens.access

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.theme.AulaTextStyles
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.OnboardingCoral
import com.example.aulasegura.ui.theme.OnboardingCream
import com.example.aulasegura.ui.theme.OnboardingHeading
import com.example.aulasegura.ui.theme.OnboardingMuted
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val InactiveDot = Color(0xFFC5C8E6)
private val HeroLavender = Color(0xFFE8E9F7)

private data class OnboardingPageData(
    val title: String,
    val description: String,
    val cta: String,
    val heroBackground: Color,
    val heroBorder: Color,
)

private val pages = listOf(
    OnboardingPageData(
        title = "Pasa lista en segundos",
        description = "Registra la asistencia desde tu celular, validada con la ubicación de la escuela.",
        cta = "Siguiente",
        heroBackground = HeroLavender,
        heroBorder = Color(0xFFD5D8F0),
    ),
    OnboardingPageData(
        title = "Las familias, siempre enteradas",
        description = "Faltas, tareas y avisos llegan al instante, sin cuadernos de recados.",
        cta = "Siguiente",
        heroBackground = Color(0xFFE9E9FB),
        heroBorder = Color(0xFFD5D8F0),
    ),
    OnboardingPageData(
        title = "Funciona incluso sin internet",
        description = "Guarda todo en tu dispositivo y se sincroniza solo cuando vuelve la conexión.",
        cta = "Comenzar",
        heroBackground = HeroLavender,
        heroBorder = Color(0xFFC5C8E6),
    ),
)

/**
 * 1.2 / 1.3 / 1.4 — Onboarding (3 páginas).
 *
 * Ported from Stitch screens:
 *   457d7441afca4caf8e7cedd89038e01e (Pasa lista)
 *   d9bd36fa9fb64a04b7c77c884560a5ef (Familias enteradas)
 *   28503734721f4b0a984df90c4e8e9e97 (Modo Offline)
 *
 * The mockups use three different card radii (32/8/12 dp) for the same hero
 * block; normalised here to 32 dp so the pager reads as one continuous screen.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingCream),
    ) {
        ShieldPattern(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.04f,
            color = Primary,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            OnboardingTopBar(onSkip = onFinished)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                OnboardingPageContent(page = page, data = pages[page])
            }

            OnboardingFooter(
                page = pagerState.currentPage,
                cta = pages[pagerState.currentPage].cta,
                onNext = {
                    if (pagerState.currentPage == pages.lastIndex) {
                        onFinished()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
            )
        }
    }
}

@Composable
private fun OnboardingTopBar(onSkip: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(PrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "AulaSegura",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = (-0.4).sp,
                color = Primary,
            )
        }

        Text(
            text = "Omitir",
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = Primary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onSkip)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun OnboardingPageContent(page: Int, data: OnboardingPageData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Reveal(delayMillis = 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(370.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(data.heroBackground)
                    .border(1.dp, data.heroBorder, RoundedCornerShape(32.dp)),
            ) {
                when (page) {
                    0 -> IllustrationRollCall(Modifier.fillMaxSize())
                    1 -> IllustrationFamilyNotices(Modifier.fillMaxSize())
                    else -> IllustrationOffline(Modifier.fillMaxSize())
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Reveal(delayMillis = 90) {
            Column {
                Text(
                    text = data.title,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    letterSpacing = (-0.6).sp,
                    color = OnboardingHeading,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = data.description,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    color = OnboardingMuted,
                )
            }
        }
    }
}

@Composable
private fun OnboardingFooter(
    page: Int,
    cta: String,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 20.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp),
        ) {
            repeat(pages.size) { index ->
                val active = index == page
                val width by animateDpAsState(
                    targetValue = if (active) 24.dp else 8.dp,
                    animationSpec = tween(280),
                    label = "dotWidth$index",
                )
                val color by androidx.compose.animation.animateColorAsState(
                    targetValue = if (active) OnboardingCoral else InactiveDot,
                    animationSpec = tween(280),
                    label = "dotColor$index",
                )
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(8.dp)
                        .background(color, RoundedCornerShape(percent = 50)),
                )
            }
        }

        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.98f else 1f,
            animationSpec = tween(150),
            label = "buttonScale",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = OnboardingCoral,
                    spotColor = OnboardingCoral,
                )
                .clip(CircleShape)
                .background(OnboardingCoral)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClick = onNext,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = cta,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Subtle staggered entrance: fade in and slide up once, when the page is first composed.
 */
@Composable
internal fun Reveal(
    delayMillis: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(320),
        label = "revealAlpha",
    )
    val offset by animateDpAsState(
        targetValue = if (visible) 0.dp else 16.dp,
        animationSpec = tween(320),
        label = "revealOffset",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offset.toPx()
        },
    ) {
        content()
    }
}

/**
 * Modifier flavour of [Reveal]: fades in and slides up the node itself after
 * [delayMillis]. Use it on containers that cannot be wrapped (for example a
 * child holding `Modifier.weight()`), so every screen can carry the same
 * "loading" entrance without restructuring its layout.
 */
@Composable
fun Modifier.revealOnEnter(delayMillis: Int = 0): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(320),
        label = "revealModifierAlpha",
    )
    val offset by animateDpAsState(
        targetValue = if (visible) 0.dp else 16.dp,
        animationSpec = tween(320),
        label = "revealModifierOffset",
    )
    return this.graphicsLayer {
        this.alpha = alpha
        translationY = offset.toPx()
    }
}
