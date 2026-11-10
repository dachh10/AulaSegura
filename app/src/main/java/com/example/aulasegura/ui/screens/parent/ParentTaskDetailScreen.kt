package com.example.aulasegura.ui.screens.parent

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.PrimaryContainer

/** 2.4 — Detalle de tarea. Ported from Stitch screen `29ac02e4480a4828a0fe2ba1351e3014`. */

private val IndigoTint = Color(0xFFE8E9F7)
private val IndigoBorder = Color(0xFFD5D7EE)
private val BorderWarm = Color(0xFFE6E1D8)
private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val MintSurface = Color(0xFFF0FAF7)
private val MintBorder = Color(0xFFDCF4EC)
private val MintText = Color(0xFF2E9E6B)
private val PdfSurface = Color(0xFFFEECEB)
private val PdfBorder = Color(0xFFFCD5D2)
private val PdfText = Color(0xFFAE3123)
private val AmberSurface = Color(0xFFFEF3C7)
private val AmberText = Color(0xFF78350F)

@Composable
fun ParentTaskDetailScreen(onBack: () -> Unit) {
    var marked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream)
            .statusBarsPadding(),
    ) {
        DetailTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Reveal(0) { TaskHeaderCard() }
            Reveal(60) { DueDateCard() }
            Reveal(120) { InstructionsSection() }
            Reveal(180) { AttachmentsSection() }
        }

        DetailFooter(
            marked = marked,
            onToggle = { marked = !marked },
        )
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    onClick = onBack,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Detalle de tarea",
                    color = PrimaryContainer,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
            RoundIconButton(
                icon = Icons.Outlined.BookmarkBorder,
                contentDescription = "Guardar marcador",
                onClick = {},
            )
        }
        HorizontalDivider(thickness = 1.dp, color = BorderWarm)
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.9f)
    Box(
        modifier = pressModifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = PrimaryContainer,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun TaskHeaderCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IndigoTint)
            .border(1.dp, IndigoBorder, RoundedCornerShape(24.dp)),
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = PrimaryContainer.copy(alpha = 0.06f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 24.dp)
                .size(130.dp),
        )

        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BrandCoral)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Matemáticas",
                        color = OnPrimary,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    text = "Secundaria 2° B",
                    color = Slate500,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Fracciones, ejercicios 1-10",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 29.sp,
                letterSpacing = (-0.3).sp,
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = IndigoBorder.copy(alpha = 0.8f))
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "RS",
                        color = OnPrimary,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Prof. Ricardo Salinas",
                        color = PrimaryContainer,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "Publicada: lun 3 nov, 10:15",
                        color = Slate600,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DueDateCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, BorderWarm, RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MintSurface)
                    .border(1.dp, MintBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Event,
                    contentDescription = null,
                    tint = MintText,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Fecha de entrega",
                    color = Slate500,
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                )
                Text(
                    text = "Miércoles 5 de noviembre · 08:00",
                    color = PrimaryContainer,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF4EFE7))
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AmberSurface)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = AmberText,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Vence mañana",
                    color = AmberText,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun InstructionsSection() {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Instrucciones",
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, BorderWarm, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Text(
                text = "Resuelve los ejercicios 1 al 10 de la página 84 del libro. " +
                    "Escribe el procedimiento completo y revisa tus resultados antes de entregar.",
                color = Slate700,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun AttachmentsSection() {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Archivos adjuntos",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            Text(
                text = "2 recursos",
                color = Slate500,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AttachmentRow(
                name = "Guia_fracciones.pdf",
                meta = "240 KB · Documento PDF",
                icon = Icons.Filled.PictureAsPdf,
                iconBackground = PdfSurface,
                iconTint = PdfText,
                iconBorder = PdfBorder,
            )
            AttachmentRow(
                name = "Ejemplo_resuelto.jpg",
                meta = "1.2 MB · Imagen",
                icon = Icons.Filled.Image,
                iconBackground = IndigoTint,
                iconTint = PrimaryContainer,
                iconBorder = IndigoBorder,
            )
        }
    }
}

@Composable
private fun AttachmentRow(
    name: String,
    meta: String,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    iconBorder: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, BorderWarm, RoundedCornerShape(24.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBackground)
                    .border(1.dp, iconBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color(0xFF1C1C19),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meta,
                    color = Slate500,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        val (interaction, pressModifier) = rememberPressScale(0.9f)
        Box(
            modifier = pressModifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = {}),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = "Descargar $name",
                tint = PrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DetailFooter(
    marked: Boolean,
    onToggle: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.97f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        BrandCream.copy(alpha = 0.95f),
                        BrandCream,
                    ),
                ),
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 10.dp),
    ) {
        Row(
            modifier = pressModifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = BrandCoral.copy(alpha = 0.3f),
                    spotColor = BrandCoral.copy(alpha = 0.3f),
                )
                .clip(CircleShape)
                .background(if (marked) PrimaryContainer else BrandCoral)
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = OnPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (marked) "Tarea vista" else "Marcar como vista",
                color = OnPrimary,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}
