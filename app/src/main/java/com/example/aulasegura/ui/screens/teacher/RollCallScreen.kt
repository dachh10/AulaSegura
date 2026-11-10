package com.example.aulasegura.ui.screens.teacher

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.location.distanceMeters
import com.example.aulasegura.location.rememberCurrentLocation
import com.example.aulasegura.ui.components.CampusGeo
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnboardingHeading
import com.example.aulasegura.ui.theme.OnboardingMuted
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * 3.3 — Pase de lista (docente).
 *
 * Reproduce el mockup de Stitch tal cual: barra superior, banner GPS, chips de
 * conteo, fila de acción rápida, tarjeta de alumnos con control segmentado por
 * renglón y barra inferior fija con el CTA "Guardar asistencia".
 *
 * GPS-first: esta pantalla sólo se alcanza después de que
 * `TeacherLocationVerifyingScreen` confirmó que el docente está dentro del
 * radio del plantel. El banner lee la lectura real del GPS y muestra la
 * distancia al centro del campus, no un valor fijo.
 */

private val Hairline = Color(0xFFE6E1D8)
private val MutedBorder = Color(0xFFD5D2CB)
private val Coral = Color(0xFFFF6B57)
private val Mint = Color(0xFF5ED3B0)
private val SuccessGreen = Color(0xFF2E9E6B)
private val DangerRed = Color(0xFFE5484D)
private val WarningAmber = Color(0xFFD9871D)
private val UnmarkedGrey = Color(0xFF6B6E8F)

private enum class Attendance { Present, Late, Absent }

private data class RollCallStudent(
    val number: String,
    val name: String,
    val initials: String,
    val avatarBackground: Color,
    val avatarForeground: Color,
    val status: Attendance?,
    val detail: String,
)

private fun sampleStudents(): List<RollCallStudent> = listOf(
    RollCallStudent("01", "Ana Sofía Aguilar", "AA", Color(0xFFE8E9F7), Color(0xFF2B2F77), Attendance.Present, "01"),
    RollCallStudent("02", "Bruno Castillo", "BC", Color(0xFFE0F2FE), Color(0xFF0369A1), Attendance.Present, "02"),
    RollCallStudent("03", "Camila Domínguez", "CD", Color(0xFFFEE2E2), Color(0xFFB91C1C), Attendance.Absent, "03 · Justificante pendiente"),
    RollCallStudent("04", "Diego Escobar", "DE", Color(0xFFFEF3C7), Color(0xFFB45309), Attendance.Late, "04 · Llegada 08:12"),
    RollCallStudent("05", "Elena Fuentes", "EF", Color(0xFFF3E8FF), Color(0xFF7E22CE), null, "05 · Sin registrar"),
    RollCallStudent("06", "Mariana Gómez Ruiz", "MG", Color(0xFFCCFBF1), Color(0xFF0F766E), Attendance.Present, "06"),
)

@Composable
fun RollCallScreen(
    groupId: String = "3B",
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    onOpenMap: () -> Unit = {},
) {
    var students by remember { mutableStateOf(sampleStudents()) }
    val location = rememberCurrentLocation()
    val distance = location.fix?.let {
        distanceMeters(
            it.latitude,
            it.longitude,
            CampusGeo.SanAgustin.latitude,
            CampusGeo.SanAgustin.longitude,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream)
            .statusBarsPadding(),
    ) {
        // Scrollable canvas: leaves room for the sticky footer below.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Reveal(0) {
                TopBar(subtitle = "$groupId · Matemáticas · Mar 3 nov", onBack = onBack)
            }
            Reveal(60) {
                GpsBanner(
                    distanceMeters = distance,
                    accuracyMeters = location.fix?.accuracyMeters,
                    onOpenMap = onOpenMap,
                )
            }
            Reveal(120) { CountersRow() }
            Reveal(180) {
                QuickActionRow(onMarkAllPresent = {
                    students = students.map { it.copy(status = Attendance.Present) }
                })
            }
            Reveal(240) {
                StudentList(
                    students = students,
                    onStatusChange = { index, status ->
                        students = students.toMutableList().also {
                            it[index] = it[index].copy(status = status)
                        }
                    },
                )
            }
        }

        BottomBar(onSave = onSave)
    }
}

// ZONE 1 — Top app bar
@Composable
private fun TopBar(subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            size = 44.dp,
            background = SurfaceContainerLowest,
            border = Hairline,
            onClick = onBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = OnboardingHeading,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pase de lista",
                color = OnboardingHeading,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = OnboardingMuted,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ZONE 2 — GPS status banner (fed by the real location fix)
@Composable
private fun GpsBanner(
    distanceMeters: Float?,
    accuracyMeters: Float?,
    onOpenMap: () -> Unit,
) {
    val detail = buildString {
        append(
            if (distanceMeters != null) {
                "${distanceMeters.toInt()} m del centro"
            } else {
                "Ubicación verificada"
            },
        )
        append(" · radio ${CampusGeo.RadiusMeters.toInt()} m")
        if (accuracyMeters != null) append(" · ±${accuracyMeters.toInt()} m")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE3F7F1))
            .border(1.dp, Color(0xFFB4E8DA), RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenMap)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF0E6245)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp),
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Estás dentro de la escuela",
                color = Color(0xFF0E6245),
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                color = Color(0xFF1E8262),
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = null,
            tint = Color(0xFF1E8262),
            modifier = Modifier.size(20.dp),
        )
    }
}

// ZONE 3 — Counters row
@Composable
private fun CountersRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CounterChip(
            icon = Icons.Filled.Check,
            label = "Pres.",
            count = "24",
            accent = SuccessGreen,
            container = Color(0xFFEAF6F0),
            border = SuccessGreen.copy(alpha = 0.30f),
            modifier = Modifier.weight(1f),
        )
        CounterChip(
            icon = Icons.Filled.Close,
            label = "Faltas",
            count = "2",
            accent = DangerRed,
            container = Color(0xFFFEECEB),
            border = DangerRed.copy(alpha = 0.30f),
            modifier = Modifier.weight(1f),
        )
        CounterChip(
            icon = Icons.Filled.Schedule,
            label = "Ret.",
            count = "1",
            accent = WarningAmber,
            container = Color(0xFFFEF6E9),
            border = Color(0xFFF2A93B).copy(alpha = 0.40f),
            modifier = Modifier.weight(1f),
        )
        CounterChip(
            icon = null,
            label = "Sin m.",
            count = "1",
            accent = UnmarkedGrey,
            container = Color(0xFFF1EFEA),
            border = MutedBorder,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CounterChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    count: String,
    accent: Color,
    container: Color,
    border: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .border(1.dp, border, RoundedCornerShape(percent = 50))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = label,
            color = accent,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count,
            color = accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

// ZONE 4 — Quick action row (button + search, no shared container)
@Composable
private fun QuickActionRow(onMarkAllPresent: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (interaction, pressModifier) = rememberPressScale(0.96f)

        Row(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .then(pressModifier)
                .clip(RoundedCornerShape(percent = 50))
                .background(SurfaceContainerLowest)
                .border(1.dp, Color(0xFF2B2F77), RoundedCornerShape(percent = 50))
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onMarkAllPresent,
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DoneAll,
                contentDescription = null,
                tint = Color(0xFF2B2F77),
                modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Marcar todos presentes",
                color = Color(0xFF2B2F77),
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        CircleIconButton(
            size = 36.dp,
            background = SurfaceContainerLowest,
            border = Hairline,
            onClick = {},
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Buscar alumno",
                tint = OnboardingHeading,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ZONE 5 — Student list card
@Composable
private fun StudentList(
    students: List<RollCallStudent>,
    onStatusChange: (index: Int, status: Attendance) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, RoundedCornerShape(24.dp)),
    ) {
        students.forEachIndexed { index, student ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Hairline.copy(alpha = 0.70f)),
                )
            }
            StudentRow(
                student = student,
                onStatusChange = { onStatusChange(index, it) },
            )
        }
    }
}

@Composable
private fun StudentRow(
    student: RollCallStudent,
    onStatusChange: (Attendance) -> Unit,
) {
    val rowTint = when (student.status) {
        Attendance.Absent -> Color(0xFFFEF2F2)
        Attendance.Late -> Color(0xFFFFFBEB)
        else -> SurfaceContainerLowest
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(rowTint)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(student.avatarBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = student.initials,
                color = student.avatarForeground,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name,
                color = OnboardingHeading,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = student.detail,
                color = when (student.status) {
                    Attendance.Absent -> Color(0xFF991B1B)
                    Attendance.Late -> Color(0xFFB45309)
                    else -> UnmarkedGrey
                },
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusButton(
                icon = Icons.Filled.Check,
                description = "Marcar presente",
                selected = student.status == Attendance.Present,
                accent = SuccessGreen,
                onClick = { onStatusChange(Attendance.Present) },
            )
            StatusButton(
                icon = Icons.Filled.Schedule,
                description = "Marcar retardo",
                selected = student.status == Attendance.Late,
                accent = Color(0xFFF2A93B),
                onClick = { onStatusChange(Attendance.Late) },
            )
            StatusButton(
                icon = Icons.Filled.Close,
                description = "Marcar falta",
                selected = student.status == Attendance.Absent,
                accent = DangerRed,
                onClick = { onStatusChange(Attendance.Absent) },
            )
        }
    }
}

@Composable
private fun StatusButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.92f)

    Box(
        modifier = Modifier
            .size(32.dp)
            .then(pressModifier)
            .clip(CircleShape)
            .background(if (selected) accent else Color.White)
            .border(1.dp, if (selected) accent else MutedBorder, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (selected) Color.White else Color(0xFF9A98A8),
            modifier = Modifier.size(if (selected) 18.dp else 17.dp),
        )
    }
}

// ZONE 6 — Sticky bottom bar
@Composable
private fun BottomBar(onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest)
            .navigationBarsPadding()
            .padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Hairline),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CloudSync,
                contentDescription = null,
                tint = UnmarkedGrey,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Se guardará sin conexión si es necesario",
                color = UnmarkedGrey,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val (interaction, pressModifier) = rememberPressScale(0.98f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp)
                .then(pressModifier)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(percent = 50),
                    ambientColor = Coral.copy(alpha = 0.30f),
                    spotColor = Coral.copy(alpha = 0.30f),
                )
                .clip(RoundedCornerShape(percent = 50))
                .background(Coral)
                .clickable(interactionSource = interaction, indication = null, onClick = onSave),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Guardar asistencia",
                color = Color.White,
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Android gesture pill
        Box(
            modifier = Modifier
                .width(112.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(OnboardingHeading.copy(alpha = 0.25f)),
        )
    }
}

@Composable
private fun CircleIconButton(
    size: androidx.compose.ui.unit.Dp,
    background: Color,
    border: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.94f)

    Box(
        modifier = Modifier
            .size(size)
            .then(pressModifier)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, border, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
