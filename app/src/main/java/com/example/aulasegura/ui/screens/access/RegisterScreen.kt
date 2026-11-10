package com.example.aulasegura.ui.screens.access

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PhotoCamera
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.AulaField
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.theme.AulaTextStyles
import com.example.aulasegura.ui.theme.Background
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSecondaryFixed
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PillShape
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.Secondary
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.SurfaceVariant
import com.example.aulasegura.ui.theme.TertiaryContainer

/**
 * 1.7 — Registro de Familias.
 * Ported 1:1 from Stitch screen `4ed8ecd6ef044004bce46d44e15fc520`.
 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onCreated: () -> Unit,
    onLogin: () -> Unit,
) {
    var fullName by remember { mutableStateOf("Mariana Gómez Fuentes") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("AulaSegura#2024") }
    var confirmPassword by remember { mutableStateOf("") }
    var studentCode by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        ShieldPattern(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.05f,
            color = PrimaryContainer,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            RegisterTopBar(
                onBack = onBack,
                onSkip = onSkip,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 132.dp),
            ) {
                Reveal(delayMillis = 40) {
                    RegisterHeading()
                }

                Reveal(delayMillis = 90) {
                    AvatarPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 28.dp),
                    )
                }

                Reveal(delayMillis = 140) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AulaField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            placeholder = "Nombre y apellidos",
                            leadingIcon = Icons.Filled.Person,
                            label = "Nombre completo",
                            keyboardType = KeyboardType.Text,
                        )

                        AulaField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "ejemplo@correo.com",
                            leadingIcon = Icons.Outlined.Mail,
                            label = "Correo electrónico",
                            keyboardType = KeyboardType.Email,
                        )

                        AulaField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = "55 1234 5678",
                            leadingIcon = Icons.Outlined.Call,
                            label = "Teléfono de contacto",
                            keyboardType = KeyboardType.Phone,
                            prefixText = "+52",
                        )

                        Column {
                            AulaField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = "••••••••",
                                leadingIcon = Icons.Outlined.Lock,
                                label = "Contraseña",
                                keyboardType = KeyboardType.Password,
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                textStyle = AulaTextStyles.dataMonoMd,
                                trailing = {
                                    PasswordToggle(
                                        visible = passwordVisible,
                                        onToggle = { passwordVisible = !passwordVisible },
                                    )
                                },
                            )
                            PasswordStrength(
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }

                        AulaField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = "Repite tu contraseña",
                            leadingIcon = Icons.Outlined.LockClock,
                            label = "Confirmar contraseña",
                            keyboardType = KeyboardType.Password,
                            isPassword = true,
                            passwordVisible = confirmVisible,
                            trailing = {
                                PasswordToggle(
                                    visible = confirmVisible,
                                    onToggle = { confirmVisible = !confirmVisible },
                                )
                            },
                        )

                        Column {
                            AulaField(
                                value = studentCode,
                                onValueChange = { studentCode = it.uppercase() },
                                placeholder = "ALU-2024-9821",
                                leadingIcon = Icons.Outlined.Badge,
                                label = "Código del alumno",
                                keyboardType = KeyboardType.Text,
                                textStyle = AulaTextStyles.dataMonoMd,
                                labelTrailing = {
                                    Text(
                                        text = "Requerido por plantel",
                                        color = PrimaryContainer,
                                        fontFamily = DmSans,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                    )
                                },
                            )
                            Row(
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = TertiaryContainer,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        append("Te lo proporciona la escuela • ")
                                        withStyle(
                                            SpanStyle(
                                                color = Primary,
                                                fontWeight = FontWeight.Medium,
                                            ),
                                        ) {
                                            append("Ej. ALU-2024-9821")
                                        }
                                    },
                                    color = OnSurfaceVariant,
                                    style = AulaTextStyles.dataMonoSm,
                                )
                            }
                        }

                        TermsRow(
                            accepted = accepted,
                            onToggle = { accepted = !accepted },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        RegisterBottomBar(
            onCreated = onCreated,
            onLogin = onLogin,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun RegisterTopBar(
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = statusBarTop + 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceContainerLow)
                .border(1.dp, OutlineVariant.copy(alpha = 0.30f), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Regresar",
                tint = PrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }

        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.30f), PillShape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = TertiaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "AulaSegura",
                color = Primary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = (-0.2).sp,
            )
        }

        Text(
            text = "Omitir",
            color = Primary,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.2.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSkip)
                .padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun RegisterHeading() {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(SecondaryFixed)
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                tint = OnSecondaryFixed,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Comunidad Escolar",
                color = OnSecondaryFixed,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.3.sp,
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Crea tu cuenta",
            color = Primary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.6).sp,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Para dar seguimiento a la vida escolar de tus hijos de manera clara y protegida.",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun AvatarPicker(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(88.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(PrimaryFixed.copy(alpha = 0.60f))
                    .border(2.dp, SurfaceContainerLowest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = PrimaryContainer.copy(alpha = 0.70f),
                    modifier = Modifier.size(48.dp),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SecondaryContainer)
                    .border(2.dp, SurfaceContainerLowest, CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = "Subir foto de perfil",
                    tint = SurfaceContainerLowest,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Agregar foto (opcional)",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun PasswordToggle(
    visible: Boolean,
    onToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (visible) {
                Icons.Outlined.VisibilityOff
            } else {
                Icons.Outlined.Visibility
            },
            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
            tint = Outline,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PasswordStrength(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(PillShape)
                        .background(
                            if (index < 2) SecondaryContainer else SurfaceVariant,
                        ),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Mínimo 8 caracteres",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.3.sp,
            )
            Text(
                text = "SEGURIDAD MEDIA",
                color = Secondary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SecondaryFixed.copy(alpha = 0.50f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun TermsRow(
    accepted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (accepted) PrimaryContainer else Color.Transparent)
                .border(
                    width = if (accepted) 0.dp else 1.dp,
                    color = OutlineVariant.copy(alpha = 0.60f),
                    shape = RoundedCornerShape(6.dp),
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (accepted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = buildAnnotatedString {
                append("Acepto el ")
                withStyle(
                    SpanStyle(color = Secondary, fontWeight = FontWeight.SemiBold),
                ) {
                    append("aviso de privacidad")
                }
                append(" y los ")
                withStyle(
                    SpanStyle(color = Secondary, fontWeight = FontWeight.SemiBold),
                ) {
                    append("términos de uso")
                }
                append(" de la plataforma escolar.")
            },
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun RegisterBottomBar(
    onCreated: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Background.copy(alpha = 0.95f),
                        Background,
                    ),
                ),
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PillButton(
            text = "Crear cuenta",
            onClick = onCreated,
            containerColor = SecondaryContainer,
            contentColor = SurfaceContainerLowest,
            fontSize = 16.sp,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            shadowColor = SecondaryContainer.copy(alpha = 0.25f),
        )

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "¿Ya tienes cuenta? ",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 13.sp,
            )
            Text(
                text = "Inicia sesión",
                color = Secondary,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onLogin),
            )
        }
    }
}
