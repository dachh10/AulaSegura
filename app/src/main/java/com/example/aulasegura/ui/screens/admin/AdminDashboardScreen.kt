package com.example.aulasegura.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.AdminBottomBar
import com.example.aulasegura.ui.components.AdminTab
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryFixedDim

/** 4.1 — Panel Directivo. Ported 1:1 from the Stitch markup of the admin dashboard. */

/** Hairline borders used across every card in the mockup (#E6E1D8). */
private val Hairline = Color(0xFFE6E1D8)

/** Mockup accents that are not part of the shared M3 palette. */
private val DangerRed = Color(0xFFE5484D)
private val AttentionAmber = Color(0xFFF59E0B)
private val SuccessPillBg = Color(0xFFEAF6F0)
private val SuccessPillText = Color(0xFF2E9E6B)
private val PendingPillBg = Color(0xFFFEF3C7)
private val PendingPillText = Color(0xFFD97706)
private val AttentionCardBg = Color(0xFFFFFBEB)
private val AttentionCardBorder = Color(0xFFFDE68A)
private val AttentionText = Color(0xFF92400E)
private val AttentionChevron = Color(0xFFB45309)

private val TileShape = RoundedCornerShape(20.dp)
private val HeroShape = RoundedCornerShape(24.dp)

@Composable
fun AdminDashboardScreen(
    onNotificaciones: () -> Unit = {},
    onUsuarios: () -> Unit = {},
    onGrupos: () -> Unit = {},
    onReportes: () -> Unit = {},
    onMas: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Reveal(0) { AdminTopBar(onNotificaciones = onNotificaciones) }
            Reveal(60) { TodayAttendanceCard() }
            Reveal(120) { CampusNumbers() }
            Reveal(180) { TodayGroups(onVerDetalles = onGrupos) }
            Reveal(240) { AttentionRequired() }
        }

        AdminBottomBar(
            active = AdminTab.Panel,
            onSelect = { tab ->
                when (tab) {
                    AdminTab.Panel -> Unit
                    AdminTab.Usuarios -> onUsuarios()
                    AdminTab.Grupos -> onGrupos()
                    AdminTab.Reportes -> onReportes()
                    AdminTab.Mas -> onMas()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AdminTopBar(onNotificaciones: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryFixed),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "PR",
                    color = Primary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Hola, directora Patricia",
                    color = Primary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                )
                Text(
                    text = "Primaria Benito Juárez",
                    color = Outline,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.5f), CircleShape)
                    .clickable(onClick = onNotificaciones),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notificaciones",
                    tint = Primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Box(
                modifier = Modifier
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(SecondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "4",
                    color = OnPrimary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun TodayAttendanceCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .background(PrimaryContainer)
            .border(1.dp, Primary.copy(alpha = 0.20f), HeroShape),
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = OnPrimary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 32.dp, y = (-32).dp)
                .size(176.dp)
                .alpha(0.06f),
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ASISTENCIA DE HOY · MAR 3 NOV",
                color = OnPrimary.copy(alpha = 0.70f),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AttendanceRing(percent = 0.93f)
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AttendanceMetric(color = Color(0xFF5ED3B0), label = "79 presentes")
                    AttendanceMetric(color = DangerRed, label = "4 faltas")
                    AttendanceMetric(color = AttentionAmber, label = "2 retardos")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OnPrimary.copy(alpha = 0.15f)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudDone,
                    contentDescription = null,
                    tint = Color(0xFF5ED3B0),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Actualizado hace 2 min",
                    color = OnPrimary.copy(alpha = 0.70f),
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun AttendanceRing(percent: Float) {
    Box(
        modifier = Modifier.size(88.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = OnPrimary.copy(alpha = 0.20f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = TertiaryFixedDim,
                startAngle = -90f,
                sweepAngle = 360f * percent,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "93%",
                color = OnPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 26.sp,
            )
            Text(
                text = "TOTAL",
                color = OnPrimary.copy(alpha = 0.70f),
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 0.6.sp,
            )
        }
    }
}

@Composable
private fun AttendanceMetric(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = OnPrimary,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun CampusNumbers() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Plantel en números",
            color = OnSurface,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile(
                icon = Icons.Filled.School,
                value = "85",
                label = "Alumnos",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            StatTile(
                icon = Icons.Filled.Badge,
                value = "4",
                label = "Docentes",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile(
                icon = Icons.Filled.Groups,
                value = "3",
                label = "Grupos",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            StatTile(
                icon = Icons.Filled.Warning,
                value = "2",
                label = "Incidentes nuevos",
                chipColor = SecondaryFixed.copy(alpha = 0.5f),
                chipContentColor = SecondaryContainer,
                valueColor = Color(0xFFFF6B57),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    chipColor: Color = PrimaryFixed.copy(alpha = 0.6f),
    chipContentColor: Color = Primary,
    valueColor: Color = OnSurface,
) {
    Column(
        modifier = modifier
            .clip(TileShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.6f), TileShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(chipColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = chipContentColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = valueColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
        )
        Text(
            text = label,
            color = Outline,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

private data class TodayGroupRow(
    val badge: String,
    val teacher: String,
    val percent: Int?,
    val done: Boolean,
)

private val todayGroups = listOf(
    TodayGroupRow(badge = "3° B", teacher = "Prof. Salinas", percent = 94, done = true),
    TodayGroupRow(badge = "3° A", teacher = "Profa. Mendoza", percent = 91, done = true),
    TodayGroupRow(badge = "2° C", teacher = "Prof. Vega", percent = null, done = false),
)

@Composable
private fun TodayGroups(onVerDetalles: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Grupos hoy",
                color = OnSurface,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Text(
                text = "Ver detalles",
                color = Primary,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                modifier = Modifier.clickable(onClick = onVerDetalles),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HeroShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.6f), HeroShape),
        ) {
            todayGroups.forEachIndexed { index, group ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(OutlineVariant.copy(alpha = 0.4f)),
                    )
                }
                TodayGroupRowItem(group)
            }
        }
    }
}

@Composable
private fun TodayGroupRowItem(group: TodayGroupRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (group.done) PrimaryFixed else PrimaryFixed.copy(alpha = 0.5f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = group.badge,
                    color = if (group.done) Primary else Primary.copy(alpha = 0.8f),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = group.teacher,
                    color = OnSurface,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (group.percent != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(SurfaceContainer),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(group.percent / 100f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(PrimaryContainer),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${group.percent}%",
                            color = OnSurface,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                        )
                    }
                } else {
                    Text(
                        text = "—",
                        color = Outline,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        if (group.done) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(SuccessPillBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SuccessPillText,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Lista hecha",
                    color = SuccessPillText,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PendingPillBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = PendingPillText,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pendiente",
                    color = PendingPillText,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun AttentionRequired() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Requiere atención",
            color = OnSurface,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TileShape)
                .background(AttentionCardBg)
                .border(1.dp, AttentionCardBorder, TileShape)
                .padding(12.dp),
        ) {
            AttentionRow(
                icon = Icons.Filled.Error,
                text = buildAnnotatedString {
                    append("Alumnos con 3 o más faltas este mes: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("3") }
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .height(1.dp)
                    .background(AttentionCardBorder.copy(alpha = 0.6f)),
            )
            AttentionRow(
                icon = Icons.Filled.CloudOff,
                text = buildAnnotatedString {
                    append("Registros sin sincronizar: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("1") }
                },
            )
        }
    }
}

@Composable
private fun AttentionRow(icon: ImageVector, text: AnnotatedString) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PendingPillText,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = AttentionText,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = AttentionChevron,
            modifier = Modifier.size(20.dp),
        )
    }
}
