package com.example.aulasegura.ui.screens.access

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.ui.components.AulaField
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.OnTertiaryFixedVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.PrimaryFixedDim
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryFixed
import com.example.aulasegura.ui.theme.TertiaryFixedDim
import kotlinx.coroutines.delay

private sealed interface ForgotState {
    data object Request : ForgotState
    data class Sent(val email: String) : ForgotState
}

/**
 * 1.6 — Recuperar Contraseña.
 * Ported 1:1 from Stitch screen `198d897b3c6c45e7a67832e5393649cc`.
 *
 * Two states live in a single Composable:
 *  - [ForgotState.Request] — email form (State 1 in Stitch)
 *  - [ForgotState.Sent]    — success / check-email view (State 2 in Stitch)
 */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }

    val state: ForgotState = uiState.sentTo
        ?.let { ForgotState.Sent(it) }
        ?: ForgotState.Request

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        // Dot watermark pattern
        ShieldPattern(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.05f,
            color = PrimaryContainer,
        )
        // Ghost shield shapes (decorative — matches Register & Login)
        ForgotShieldAccents()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Spacer(Modifier.height(statusBarPadding))

            ForgotTopBar(onBack = onBack)

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    if (targetState is ForgotState.Sent) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "forgot_state",
            ) { currentState ->
                when (currentState) {
                    is ForgotState.Request -> RequestState(
                        email = email,
                        onEmailChange = { email = it },
                        onSend = { viewModel.sendResetEmail(email) },
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onBackToLogin = onBackToLogin,
                    )
                    is ForgotState.Sent -> SentState(
                        email = currentState.email,
                        onBackToLogin = onBackToLogin,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top App Bar
// ---------------------------------------------------------------------------

@Composable
private fun ForgotTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Back arrow — circular white pill
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceContainerLowest.copy(alpha = 0.8f))
                .border(1.dp, OutlineVariant.copy(alpha = 0.4f), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Volver",
                tint = PrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }

        // Institutional brand badge (coral pulse dot + "AulaSegura")
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceContainerLow)
                .border(1.dp, OutlineVariant.copy(alpha = 0.3f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val pulseTransition = rememberInfiniteTransition(label = "badge_pulse")
            val pulseAlpha by pulseTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "pulse_alpha",
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SecondaryContainer.copy(alpha = pulseAlpha)),
            )
            Text(
                text = "AulaSegura",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Primary,
                letterSpacing = (-0.3).sp,
            )
        }

        // "Omitir" text button
        Text(
            text = "Omitir",
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Primary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = {})
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// State 1 — Request form
// ---------------------------------------------------------------------------

@Composable
private fun RequestState(
    email: String,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBackToLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Reveal(delayMillis = 60) {
            EnvelopeIllustration()
        }

        Spacer(Modifier.height(24.dp))

        Reveal(delayMillis = 100) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.4).sp,
                    color = Primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Escribe tu correo y te enviaremos un enlace para crear una nueva contraseña.",
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Reveal(delayMillis = 140) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Label row: field name + trailing hint in mono
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Correo electrónico",
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Primary,
                    )
                    Text(
                        text = "Institucional o tutor",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = Outline,
                    )
                }
                AulaField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = null,
                    placeholder = "ejemplo@correo.com",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Formato admitido: correo personal o corporativo",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = Outline,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Send button — coral (#FC6955)
        Reveal(delayMillis = 180) {
            PillButton(
                text = if (isLoading) "Enviando…" else "Enviar enlace →",
                onClick = { if (!isLoading) onSend() },
                containerColor = SecondaryContainer,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMessage,
                fontFamily = DmSans,
                fontSize = 13.sp,
                color = Color(0xFFB3261E),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))

        Reveal(delayMillis = 220) {
            BackToLoginLink(onClick = onBackToLogin)
        }
    }
}

// ---------------------------------------------------------------------------
// State 2 — Sent / success
// ---------------------------------------------------------------------------

@Composable
private fun SentState(
    email: String,
    onBackToLogin: () -> Unit,
) {
    var countdown by remember { mutableIntStateOf(45) }
    var resendEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1_000L)
            countdown--
        }
        resendEnabled = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Reveal(delayMillis = 60) {
            MintSuccessBadge()
        }

        Spacer(Modifier.height(24.dp))

        // "Enlace despachado con éxito" mono chip
        Reveal(delayMillis = 100) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(TertiaryFixed.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = OnTertiaryFixedVariant,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "Enlace despachado con éxito",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = OnTertiaryFixedVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Reveal(delayMillis = 130) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Revisa tu correo",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.4).sp,
                    color = Primary,
                )
                Spacer(Modifier.height(10.dp))
                val body = buildAnnotatedString {
                    append("Enviamos un enlace a ")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = Primary,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) { append(email) }
                    append(". Si no lo ves, revisa tu carpeta de spam.")
                }
                Text(
                    text = body,
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // "Abrir correo" indigo pill button
        Reveal(delayMillis = 170) {
            PillButton(
                text = "Abrir correo",
                onClick = { /* open mail client */ },
                containerColor = PrimaryContainer,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Resend card with 45s countdown
        Reveal(delayMillis = 200) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "¿No recibiste el mensaje?",
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    color = OnSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable(enabled = resendEnabled) {},
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = if (resendEnabled) Primary else Outline,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Reenviar enlace",
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (resendEnabled) Primary else Outline,
                    )
                    if (!resendEnabled) {
                        val seconds = if (countdown < 10) "0$countdown" else "$countdown"
                        Text(
                            text = "(00:$seconds)",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Primary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Security note card: lock icon + "15 minutos" bold
        Reveal(delayMillis = 230) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = TertiaryFixedDim,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 1.dp),
                )
                val note = buildAnnotatedString {
                    append("Por tu seguridad, el enlace caduca en ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("15 minutos") }
                    append(" y solo puede utilizarse una vez.")
                }
                Text(
                    text = note,
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = OnSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Reveal(delayMillis = 260) {
            BackToLoginLink(onClick = onBackToLogin)
        }
    }
}

// ---------------------------------------------------------------------------
// Shared link component
// ---------------------------------------------------------------------------

@Composable
private fun BackToLoginLink(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.ArrowBack,
            contentDescription = null,
            tint = PrimaryContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Volver a iniciar sesión",
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = PrimaryContainer,
        )
    }
}

// ---------------------------------------------------------------------------
// Illustrations
// ---------------------------------------------------------------------------

/**
 * 160dp circular illustration: open envelope + key + coral shield with checkmark.
 * Drawn with Canvas, matching the SVG in the Stitch HTML (viewBox 0 0 120 120).
 */
@Composable
private fun EnvelopeIllustration() {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(PrimaryFixed)
            .border(2.dp, PrimaryFixedDim.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(112.dp)) {
            val w = size.width
            val h = size.height
            val sx = w / 120f
            val sy = h / 120f

            fun fx(x: Float) = x * sx
            fun fy(y: Float) = y * sy

            // Envelope body back — indigo
            drawRoundRect(
                color = PrimaryContainer,
                topLeft = Offset(fx(22f), fy(38f)),
                size = Size(fx(76f), fy(54f)),
                cornerRadius = CornerRadius(fx(10f)),
            )
            // Open flap — mint
            val flap = Path().apply {
                moveTo(fx(24f), fy(44f))
                lineTo(fx(60f), fy(20f))
                lineTo(fx(96f), fy(44f))
                close()
            }
            drawPath(flap, color = TertiaryFixedDim.copy(alpha = 0.85f))
            // Letter sheet — white
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(fx(30f), fy(26f)),
                size = Size(fx(60f), fy(42f)),
                cornerRadius = CornerRadius(fx(6f)),
            )
            // Note lines
            drawLine(
                color = PrimaryContainer,
                start = Offset(fx(38f), fy(36f)),
                end = Offset(fx(62f), fy(36f)),
                strokeWidth = fx(2.5f),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = OutlineVariant,
                start = Offset(fx(38f), fy(44f)),
                end = Offset(fx(74f), fy(44f)),
                strokeWidth = fx(2f),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = OutlineVariant,
                start = Offset(fx(38f), fy(52f)),
                end = Offset(fx(54f), fy(52f)),
                strokeWidth = fx(2f),
                cap = StrokeCap.Round,
            )
            // Envelope front pocket — dark indigo
            val front = Path().apply {
                moveTo(fx(22f), fy(52f))
                lineTo(fx(60f), fy(74f))
                lineTo(fx(98f), fy(52f))
                lineTo(fx(98f), fy(82f))
                cubicTo(fx(98f), fy(87.5f), fx(93.5f), fy(92f), fx(88f), fy(92f))
                lineTo(fx(32f), fy(92f))
                cubicTo(fx(26.5f), fy(92f), fx(22f), fy(87.5f), fx(22f), fy(82f))
                close()
            }
            drawPath(front, color = Primary)
            // Bottom triangle shadow
            val shadow = Path().apply {
                moveTo(fx(22f), fy(92f))
                lineTo(fx(60f), fy(64f))
                lineTo(fx(98f), fy(92f))
                close()
            }
            drawPath(shadow, color = PrimaryContainer.copy(alpha = 0.6f))
            // Coral shield (bottom-left of illustration)
            val shX = fx(20f)
            val shY = fy(68f)
            val shield = Path().apply {
                moveTo(shX + fx(16f), shY + fy(2f))
                lineTo(shX + fx(30f), shY + fy(8f))
                lineTo(shX + fx(30f), shY + fy(19f))
                cubicTo(shX + fx(30f), shY + fy(27f), shX + fx(16f), shY + fy(32f), shX + fx(16f), shY + fy(32f))
                cubicTo(shX + fx(16f), shY + fy(32f), shX + fx(2f), shY + fy(27f), shX + fx(2f), shY + fy(19f))
                lineTo(shX + fx(2f), shY + fy(8f))
                close()
            }
            drawPath(shield, color = SecondaryContainer)
            // Checkmark inside coral shield
            val checkPath = Path().apply {
                moveTo(shX + fx(10f), shY + fy(16.5f))
                lineTo(shX + fx(14.5f), shY + fy(21f))
                lineTo(shX + fx(22f), shY + fy(13f))
            }
            drawPath(
                checkPath,
                color = Color.White,
                style = Stroke(width = fx(2.8f), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/**
 * 96dp mint badge with pulsing ring animation + small indigo shield badge at bottom-right.
 */
@Composable
private fun MintSuccessBadge() {
    val pulseTransition = rememberInfiniteTransition(label = "mint_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Box(
        modifier = Modifier.size(112.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Pulsing outer ring
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .border(1.dp, TertiaryFixedDim.copy(alpha = pulseAlpha), CircleShape),
        )
        // Soft tint halo
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(TertiaryFixed.copy(alpha = 0.3f)),
        )
        // Solid mint circle
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(TertiaryFixedDim)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
        }
        // Indigo shield badge (bottom-right)
        Box(
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(PrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Decorative background shield shapes
// ---------------------------------------------------------------------------

@Composable
private fun ForgotShieldAccents() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-right ghost shield
        Canvas(
            modifier = Modifier
                .size(192.dp)
                .align(Alignment.TopEnd),
        ) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.5f, 0f)
                lineTo(w, h * 0.17f)
                lineTo(w, h * 0.54f)
                cubicTo(w, h * 0.79f, w * 0.5f, h * 0.96f, w * 0.5f, h * 0.96f)
                cubicTo(w * 0.5f, h * 0.96f, 0f, h * 0.79f, 0f, h * 0.54f)
                lineTo(0f, h * 0.17f)
                close()
            }
            drawPath(path, color = Primary.copy(alpha = 0.06f))
        }
        // Bottom-left ghost shield
        Canvas(
            modifier = Modifier
                .size(224.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 80.dp),
        ) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.5f, 0f)
                lineTo(w, h * 0.17f)
                lineTo(w, h * 0.54f)
                cubicTo(w, h * 0.79f, w * 0.5f, h * 0.96f, w * 0.5f, h * 0.96f)
                cubicTo(w * 0.5f, h * 0.96f, 0f, h * 0.79f, 0f, h * 0.54f)
                lineTo(0f, h * 0.17f)
                close()
            }
            drawPath(path, color = Primary.copy(alpha = 0.05f))
        }
    }
}
