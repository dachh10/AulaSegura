package com.example.aulasegura.ui.screens.access

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.AulaField
import com.example.aulasegura.ui.components.GooglePillButton
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.CardShape
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

private val FieldBorder = Color(0xFFE6E1D8)

/**
 * 1.5 — Iniciar Sesión.
 * Ported 1:1 from Stitch screen `ae9f321aff24410a9a383e5ff002bcb9`.
 *
 * Deliberate deviation: the mockup paints a fake Android status bar. The real
 * system bar is used instead, and the header grows by the inset height so the
 * 220 dp composition is preserved.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLogin: (email: String, password: String) -> Unit,
    onForgotPassword: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        LoginBackdrop()
        ShieldPattern(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.05f,
            color = PrimaryContainer,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            LoginHeader()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp),
            ) {
                Reveal(delayMillis = 60) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 2.dp,
                                shape = CardShape,
                                ambientColor = Color(0xFF1B1D3A).copy(alpha = 0.03f),
                                spotColor = Color(0xFF1B1D3A).copy(alpha = 0.05f),
                            )
                            .background(SurfaceContainerLowest, CardShape)
                            .border(1.dp, FieldBorder, CardShape)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AulaField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "correo@ejemplo.com",
                            leadingIcon = Icons.Outlined.Mail,
                            label = "Correo electrónico",
                            height = 50.dp,
                            containerColor = SurfaceContainerLow,
                            focusedContainerColor = SurfaceContainerLowest,
                            borderColor = FieldBorder,
                            keyboardType = KeyboardType.Email,
                        )

                        AulaField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "••••••••",
                            leadingIcon = Icons.Outlined.Lock,
                            label = "Contraseña",
                            height = 50.dp,
                            containerColor = SurfaceContainerLow,
                            focusedContainerColor = SurfaceContainerLowest,
                            borderColor = FieldBorder,
                            keyboardType = KeyboardType.Password,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { passwordVisible = !passwordVisible },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Outlined.VisibilityOff
                                        } else {
                                            Icons.Outlined.Visibility
                                        },
                                        contentDescription = if (passwordVisible) {
                                            "Ocultar contraseña"
                                        } else {
                                            "Mostrar contraseña"
                                        },
                                        tint = Outline,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Text(
                                text = "¿Olvidaste tu contraseña?",
                                color = BrandCoral,
                                fontFamily = DmSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable(onClick = onForgotPassword),
                            )
                        }

                        PillButton(
                            text = if (isLoading) "Ingresando…" else "Iniciar sesión",
                            onClick = { if (!isLoading) onLogin(email, password) },
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage,
                                color = Error,
                                fontFamily = DmSans,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                Reveal(delayMillis = 150) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(FieldBorder),
                            )
                            Text(
                                text = "o",
                                color = Outline,
                                fontFamily = DmSans,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .background(BrandCream)
                                    .padding(horizontal = 12.dp),
                            )
                        }

                        GooglePillButton(
                            text = "Continuar con Google",
                            onClick = { if (!isLoading) onLogin(email, password) },
                        )
                    }
                }
            }

            Reveal(delayMillis = 220) {
                LoginFooter(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 24.dp, bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun LoginBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = BrandCream)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(PrimaryContainer.copy(alpha = 0.030f), Color.Transparent),
                center = Offset(size.width * 0.10f, size.height * 0.20f),
                radius = size.minDimension * 0.60f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(PrimaryContainer.copy(alpha = 0.025f), Color.Transparent),
                center = Offset(size.width * 0.90f, size.height * 0.80f),
                radius = size.minDimension * 0.60f,
            ),
        )
    }
}

@Composable
private fun LoginHeader() {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp + statusBarTop)
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(PrimaryContainer),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 32.dp),
        ) {
            HeaderShieldWatermark()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = statusBarTop + 12.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Hola de nuevo",
                color = Color.White,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.6).sp,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Inicia sesión para continuar",
                color = Color.White.copy(alpha = 0.75f),
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun HeaderShieldWatermark() {
    Canvas(modifier = Modifier.size(width = 220.dp, height = 240.dp)) {
        val sx = size.width / 100f
        val sy = size.height / 115f
        val stroke = 6f * minOf(sx, sy)
        val paint = Color.White.copy(alpha = 0.06f)

        val shield = Path().apply {
            moveTo(50f * sx, 5f * sy)
            lineTo(90f * sx, 22f * sy)
            lineTo(90f * sx, 55f * sy)
            cubicTo(90f * sx, 82f * sy, 72f * sx, 105f * sy, 50f * sx, 112f * sy)
            cubicTo(28f * sx, 105f * sy, 10f * sx, 82f * sy, 10f * sx, 55f * sy)
            lineTo(10f * sx, 22f * sy)
            close()
        }
        drawPath(
            path = shield,
            color = paint,
            style = Stroke(width = stroke, join = StrokeJoin.Round),
        )

        val check = Path().apply {
            moveTo(35f * sx, 55f * sy)
            lineTo(45f * sx, 66f * sy)
            lineTo(65f * sx, 44f * sy)
        }
        drawPath(
            path = check,
            color = paint,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun LoginFooter(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.widthIn(max = 300.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Familias, docentes y administradores reciben su acceso del plantel.",
                color = Outline,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
