package com.example.aulasegura.ui.screens.admin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSecondary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.PrimaryFixedDim
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.SurfaceVariant

/** 4.3 — Nuevo alumno. Ported 1:1 from the Stitch markup of the new-student form. */

private val FieldShape = RoundedCornerShape(16.dp)
private val CardShape = RoundedCornerShape(24.dp)
private val FooterHairline = Color(0xFFE6E1D8)
private val CodeCardBg = PrimaryFixed.copy(alpha = 0.60f)
private val CodeCardBorder = PrimaryFixedDim.copy(alpha = 0.70f)
private val CameraShadow = Color(0x331B1D3A)
private val SaveShadow = Color(0x59FC6955)

private val grupos = listOf("3° B", "1° A", "1° B", "2° A", "2° B", "3° A")

@Composable
fun AdminNewStudentScreen(
    onBack: () -> Unit = {},
    onGuardar: () -> Unit = {},
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var apellidos by rememberSaveable { mutableStateOf("") }
    var numeroLista by rememberSaveable { mutableStateOf("") }
    var grupo by rememberSaveable { mutableStateOf(grupos.first()) }
    var curp by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream)
            .imePadding(),
    ) {
        Reveal(0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackCircleButton(onBack = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Nuevo alumno",
                        color = OnSurface,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        letterSpacing = (-0.4).sp,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .revealOnEnter(60)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PhotoHeader()

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FormField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = "Nombre(s)",
                    icon = Icons.Filled.Person,
                )
                FormField(
                    value = apellidos,
                    onValueChange = { apellidos = it },
                    placeholder = "Apellidos",
                    icon = Icons.Filled.Badge,
                )
                FormField(
                    value = numeroLista,
                    onValueChange = { numeroLista = it.filter { c -> c.isDigit() } },
                    placeholder = "Número de lista",
                    icon = Icons.Filled.Sell,
                    mono = true,
                )
                GroupField(
                    grupo = grupo,
                    onGrupoChange = { grupo = it },
                )
                FormField(
                    value = curp,
                    onValueChange = { curp = it.uppercase() },
                    placeholder = "CURP (opcional)",
                    icon = Icons.Filled.AssignmentInd,
                    mono = true,
                )
            }

            LinkingCodeCard()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandCream.copy(alpha = 0.95f))
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(FooterHairline.copy(alpha = 0.40f)),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SaveButton(onClick = onGuardar)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Cancelar",
                    color = PrimaryContainer,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun BackCircleButton(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(8.dp, CircleShape, ambientColor = CameraShadow, spotColor = CameraShadow)
            .clip(CircleShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Volver",
            tint = PrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PhotoHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(PrimaryFixed)
                    .border(2.dp, SurfaceContainerLowest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = PrimaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .shadow(6.dp, CircleShape, ambientColor = CameraShadow, spotColor = CameraShadow)
                    .clip(CircleShape)
                    .background(SecondaryContainer)
                    .clickable { },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Subir foto del alumno",
                    tint = OnSecondary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Foto del alumno (opcional)",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.24.sp,
        )
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    mono: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(FieldShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, FieldShape)
            .padding(start = 14.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Outline,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Outline,
                    fontFamily = if (mono) JetBrainsMono else DmSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = if (mono) 13.sp else 14.sp,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = OnSurface,
                    fontFamily = if (mono) JetBrainsMono else DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = if (mono) 13.sp else 14.sp,
                ),
                cursorBrush = SolidColor(Primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GroupField(
    grupo: String,
    onGrupoChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(FieldShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, SurfaceVariant, FieldShape)
                .clickable { expanded = true }
                .padding(start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Groups,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = grupo,
                color = OnSurface,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(20.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceContainerLowest),
        ) {
            grupos.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = OnSurface,
                            fontFamily = DmSans,
                            fontWeight = if (option == grupo) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp,
                        )
                    },
                    onClick = {
                        onGrupoChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun LinkingCodeCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CodeCardBg)
            .border(1.dp, CodeCardBorder, CardShape),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CÓDIGO DE VINCULACIÓN",
                color = PrimaryContainer,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "AS-4F7K-92",
                color = PrimaryContainer,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = 1.2.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CodeActionPill(icon = Icons.Filled.ContentCopy, label = "Copiar")
                CodeActionPill(icon = Icons.Filled.Share, label = "Compartir")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "El padre de familia lo usa al registrarse para ver a su hijo.",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.22.sp,
            )
        }
    }
}

@Composable
private fun CodeActionPill(icon: ImageVector, label: String) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "codePillScale",
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(2.dp, RoundedCornerShape(percent = 50))
            .clip(RoundedCornerShape(percent = 50))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.60f), RoundedCornerShape(percent = 50))
            .clickable(interactionSource = source, indication = null) { }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryContainer,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.24.sp,
        )
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "saveScale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(14.dp, RoundedCornerShape(percent = 50), ambientColor = SaveShadow, spotColor = SaveShadow)
            .clip(RoundedCornerShape(percent = 50))
            .background(SecondaryContainer)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Guardar alumno",
            color = OnSecondary,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.28.sp,
        )
    }
}
