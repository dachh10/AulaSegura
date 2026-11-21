package com.example.aulasegura.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.OnTertiaryContainer
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * The Stitch pieces the account forms share, ported once.
 *
 * The teacher's forms and the tutor's forms must look like the same product: the
 * tutor never sees a redrawn field, only the same field with a different label.
 * Keeping the widgets here is also what lets the password screen serve both roles
 * without a copy.
 */

internal val FieldShape = RoundedCornerShape(16.dp)
internal val CardShape = RoundedCornerShape(24.dp)

@Composable
internal fun ShieldWatermark(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val step = 72.dp.toPx()
        val tint = Primary.copy(alpha = 0.04f)
        val stroke = Stroke(width = 2.dp.toPx())
        var y = 0f
        while (y < size.height + step) {
            var x = 0f
            while (x < size.width + step) {
                val path = Path().apply {
                    moveTo(x + 36f, y + 6f)
                    lineTo(x + 60f, y + 16f)
                    lineTo(x + 60f, y + 38f)
                    quadraticTo(x + 60f, y + 52f, x + 36f, y + 66f)
                    quadraticTo(x + 12f, y + 52f, x + 12f, y + 38f)
                    lineTo(x + 12f, y + 16f)
                    close()
                }
                drawPath(path, color = tint, style = stroke)
                val check = Path().apply {
                    moveTo(x + 26f, y + 36f)
                    lineTo(x + 33f, y + 43f)
                    lineTo(x + 47f, y + 28f)
                }
                drawPath(check, color = tint, style = stroke)
                x += step
            }
            y += step
        }
    }
}

@Composable
internal fun StitchTopBar(
    title: String,
    onBack: () -> Unit,
    trailingBadge: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (_, press) = rememberPressScale(0.95f)
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(press)
                .clip(CircleShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.4f), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = OnSurface,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.weight(1f),
        )
        if (trailingBadge) {
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = SurfaceContainerLow) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Badge,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun StitchField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String? = null,
    imeAction: ImeAction,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Outline,
                    modifier = Modifier.size(20.dp),
                )
            },
            singleLine = true,
            shape = FieldShape,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainerLow,
                unfocusedContainerColor = SurfaceContainerLow,
                focusedBorderColor = PrimaryContainer,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Primary,
            ),
            textStyle = TextStyle(
                fontFamily = DmSans,
                fontSize = 16.sp,
                color = OnSurface,
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = imeAction),
        )
    }
}

@Composable
internal fun LockedField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(SurfaceContainerHigh.copy(alpha = 0.6f), FieldShape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Email,
                contentDescription = null,
                tint = OutlineVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = value.ifBlank { "—" },
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(18.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Para cambiarlo, contacta a la administración.",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
internal fun StitchPasswordField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    visible: Boolean,
    onToggle: () -> Unit,
    imeAction: ImeAction,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            leadingIcon = {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Outline,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = FieldShape,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainerLow,
                unfocusedContainerColor = SurfaceContainerLow,
                focusedBorderColor = PrimaryContainer,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Primary,
            ),
            textStyle = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = 14.sp,
                color = OnSurface,
                letterSpacing = 0.5.sp,
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
            ),
        )
    }
}

@Composable
internal fun StitchDock(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    saving: Boolean,
    saved: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val (interaction, press) = rememberPressScale(0.98f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .then(press)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    color = if (saved) OnTertiaryContainer else SecondaryContainer,
                    shape = RoundedCornerShape(percent = 50),
                )
                .clickable(
                    enabled = !saving,
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (saving) {
                CircularProgressIndicator(
                    color = OnPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(icon, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = OnPrimary,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
        }
        TextButton(onClick = onCancel) {
            Text(text = "Cancelar", color = Primary, fontFamily = DmSans, fontSize = 14.sp)
        }
        Spacer(
            Modifier
                .size(width = 128.dp, height = 4.dp)
                .background(OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(percent = 50)),
        )
    }
}

@Composable
internal fun StitchToast(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = Primary,
        shadowElevation = 8.dp,
        modifier = modifier.padding(top = 64.dp).fillMaxWidth(0.9f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = OnPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                color = OnPrimary,
                fontFamily = DmSans,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text("Listo", color = OnPrimary, fontFamily = DmSans, fontSize = 12.sp)
            }
        }
    }
}

/** The avatar block both edit forms open with: picture plus "Cambiar foto". */
@Composable
internal fun ProfilePhotoBlock(
    photoUrl: String?,
    initials: String,
    onChangePhoto: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Surface(
                modifier = Modifier.size(104.dp),
                shape = CircleShape,
                color = SurfaceContainerLowest,
                shadowElevation = 4.dp,
            ) {
                Box(Modifier.padding(4.dp)) {
                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().clip(CircleShape).background(PrimaryFixed),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initials,
                                color = Primary,
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                            )
                        }
                    }
                }
            }
            val (_, press) = rememberPressScale(0.90f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .then(press)
                    .clip(CircleShape)
                    .background(SecondaryContainer)
                    .clickable(onClick = onChangePhoto),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "Cambiar foto",
                    tint = OnPrimary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        TextButton(onClick = onChangePhoto) {
            Text(
                text = "Cambiar foto",
                color = SecondaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
    }
}
