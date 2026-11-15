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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.AttendanceMark
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
 * Pase de lista (docente).
 *
 * Reproduce el mockup de Stitch: barra superior, banner GPS, chips de conteo,
 * fila de acción rápida, tarjeta de alumnos con control segmentado por renglón
 * y barra inferior fija con el CTA "Guardar asistencia".
 *
 * La pantalla es sin estado: recibe [ui] ya resuelto y avisa por callbacks. El
 * roster real, los conteos y el guardado viven en `RollCallViewModel`; aquí no
 * queda ningún alumno de ejemplo.
 *
 * El banner lee la lectura real del GPS. Mientras el Bloque 3 no conecte la
 * geocerca contra `schools.location`, la distancia se calcula contra el centro
 * del plantel de demostración y el texto dice "fuera" cuando corresponde: un
 * "estás dentro" fijo sería exactamente el tipo de dato que nadie debe creer.
 */
@Composable
fun RollCallScreen(
    ui: RollCallUi,
    onBack: () -> Unit = {},
    onMark: (studentId: String, mark: AttendanceMark) -> Unit = { _, _ -> },
    onMarkAllPresent: () -> Unit = {},
    onSave: () -> Unit = {},
    onOpenMap: () -> Unit = {},
) {
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
                TopBar(subtitle = ui.subtitle, onBack = onBack)
            }
            Reveal(60) {
                GpsBanner(
                    distanceMeters = distance,
                    accuracyMeters = location.fix?.accuracyMeters,
                    onOpenMap = onOpenMap,
                )
            }
            Reveal(120) { CountersRow(counts = ui.counts, unmarked = ui.unmarked) }
            Reveal(180) {
                QuickActionRow(onMarkAllPresent = onMarkAllPresent)
            }
            ui.message?.let { message ->
                Reveal(200) { MessageStrip(message = message, saved = ui.saved) }
            }
            Reveal(240) {
                when {
                    ui.loading -> LoadingCard()
                    ui.students.isEmpty() -> EmptyCard()
                    else -> StudentList(students = ui.students, onMark = onMark)
                }
            }
        }

        BottomBar(
            saving = ui.saving,
            saved = ui.saved,
            enabled = !ui.loading && ui.students.isNotEmpty() && !ui.saving && !ui.saved,
            onSave = onSave,
        )
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
    val inside = distanceMeters != null && distanceMeters <= CampusGeo.RadiusMeters
    val title = when {
        distanceMeters == null -> "Ubicación sin verificar"
        inside -> "Estás dentro de la escuela"
        else -> "Estás fuera de la escuela"
    }
    val detail = buildString {
        append(
            if (distanceMeters != null) {
                "${distanceMeters.toInt()} m del centro"
            } else {
                "Sin lectura de GPS"
            },
        )
        append(" · radio ${CampusGeo.RadiusMeters.toInt()} m")
        if (accuracyMeters != null) append(" · ±${accuracyMeters.toInt()} m")
    }

    val container = if (inside) Color(0xFFE3F7F1) else Color(0xFFFEF6E9)
    val border = if (inside) Color(0xFFB4E8DA) else Color(0xFFF2D8A8)
    val accent = if (inside) Color(0xFF0E6245) else Color(0xFF8A5A12)
    val accentSoft = if (inside) Color(0xFF1E8262) else Color(0xFFA9741F)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenMap)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (inside) Icons.Filled.Check else Icons.Filled.Info,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp),
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = accent,
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                color = accentSoft,
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
            tint = accentSoft,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ZONE 3 — Counters row, derived from the rows actually marked
//
// Five chips, not four. The row has to add up to the roster, and "justificado"
// is a real status in the schema (`CONTEXT.md` §6.2): with only four chips the
// numbers silently totalled 27 on a 28-student group, which is exactly the kind
// of counter a teacher reads and then distrusts. The icons were dropped to make
// room; colour, container and border still tell the chips apart.
@Composable
private fun CountersRow(counts: AttendanceCounts, unmarked: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CounterChip(
            label = "Pres.",
            count = counts.present.toString(),
            accent = SuccessGreen,
            container = Color(0xFFEAF6F0),
            border = SuccessGreen.copy(alpha = 0.30f),
            modifier = Modifier.weight(1f),
        )
        CounterChip(
            label = "Falt.",
            count = counts.absent.toString(),
            accent = DangerRed,
            container = Color(0xFFFEECEB),
            border = DangerRed.copy(alpha = 0.30f),
            modifier = Modifier.weight(1f),
        )
        CounterChip(
            label = "Ret.",
            count = counts.late.toString(),
            accent = WarningAmber,
            container = Color(0xFFFEF6E9),
            border = Color(0xFFF2A93B).copy(alpha = 0.40f),
            modifier = Modifier.weight(1f),
        )
        CounterChip(
            label = "Just.",
            count = counts.justified.toString(),
            accent = JustifiedBlue,
            container = Color(0xFFF2F6FF),
            border = JustifiedBlue.copy(alpha = 0.30f),
            modifier = Modifier.weight(1f),
        )
        CounterChip(
            label = "Sin m.",
            count = unmarked.toString(),
            accent = UnmarkedGrey,
            container = Color(0xFFF1EFEA),
            border = MutedBorder,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CounterChip(
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
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            color = accent,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count,
            color = accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
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

// ZONE 4b — Result of the last save attempt
@Composable
private fun MessageStrip(message: String, saved: Boolean) {
    val accent = if (saved) Color(0xFF0E6245) else Color(0xFF8A5A12)
    val container = if (saved) Color(0xFFE3F7F1) else Color(0xFFFEF6E9)
    val border = if (saved) Color(0xFFB4E8DA) else Color(0xFFF2D8A8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (saved) Icons.Filled.CheckCircle else Icons.Filled.Info,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(17.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            color = accent,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.5.sp,
            lineHeight = 16.sp,
        )
    }
}

// ZONE 5 — Student list card
@Composable
private fun StudentList(
    students: List<RollCallRowUi>,
    onMark: (studentId: String, mark: AttendanceMark) -> Unit,
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
                palette = AvatarPalette[index % AvatarPalette.size],
                onMark = { mark -> onMark(student.id, mark) },
            )
        }
    }
}

@Composable
private fun StudentRow(
    student: RollCallRowUi,
    palette: AvatarColors,
    onMark: (AttendanceMark) -> Unit,
) {
    val rowTint = when (student.mark) {
        AttendanceMark.ABSENT -> Color(0xFFFEF2F2)
        AttendanceMark.LATE -> Color(0xFFFFFBEB)
        AttendanceMark.JUSTIFIED -> Color(0xFFF2F6FF)
        else -> SurfaceContainerLowest
    }
    val detailColor = when (student.mark) {
        AttendanceMark.ABSENT -> Color(0xFF991B1B)
        AttendanceMark.LATE -> Color(0xFFB45309)
        AttendanceMark.JUSTIFIED -> Color(0xFF2E5AAC)
        else -> UnmarkedGrey
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
                .size(36.dp)
                .clip(CircleShape)
                .background(palette.background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = student.initials,
                color = palette.foreground,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.width(9.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name,
                color = OnboardingHeading,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = student.detail,
                color = detailColor,
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            StatusButton(
                icon = Icons.Filled.Check,
                description = "Marcar presente",
                selected = student.mark == AttendanceMark.PRESENT,
                accent = SuccessGreen,
                onClick = { onMark(AttendanceMark.PRESENT) },
            )
            StatusButton(
                icon = Icons.Filled.Schedule,
                description = "Marcar retardo",
                selected = student.mark == AttendanceMark.LATE,
                accent = WarningAmber,
                onClick = { onMark(AttendanceMark.LATE) },
            )
            StatusButton(
                icon = Icons.Filled.Close,
                description = "Marcar falta",
                selected = student.mark == AttendanceMark.ABSENT,
                accent = DangerRed,
                onClick = { onMark(AttendanceMark.ABSENT) },
            )
            StatusButton(
                icon = Icons.Filled.Info,
                description = "Marcar justificado",
                selected = student.mark == AttendanceMark.JUSTIFIED,
                accent = JustifiedBlue,
                onClick = { onMark(AttendanceMark.JUSTIFIED) },
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
            .size(30.dp)
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
            modifier = Modifier.size(if (selected) 16.dp else 15.dp),
        )
    }
}

@Composable
private fun LoadingCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, RoundedCornerShape(24.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = OnboardingMuted,
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Cargando alumnos…",
            color = OnboardingMuted,
            fontFamily = DmSans,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun EmptyCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Este grupo no tiene alumnos en el dispositivo.",
            color = OnboardingMuted,
            fontFamily = DmSans,
            fontSize = 13.sp,
        )
    }
}

// ZONE 6 — Sticky bottom bar
@Composable
private fun BottomBar(
    saving: Boolean,
    saved: Boolean,
    enabled: Boolean,
    onSave: () -> Unit,
) {
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
        val label = when {
            saved -> "Asistencia guardada"
            saving -> "Guardando…"
            else -> "Guardar asistencia"
        }
        val container = if (enabled) Coral else Coral.copy(alpha = 0.45f)

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
                .background(container)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onSave,
                ),
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
                text = label,
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

private data class AvatarColors(val background: Color, val foreground: Color)

private val AvatarPalette = listOf(
    AvatarColors(Color(0xFFE8E9F7), Color(0xFF2B2F77)),
    AvatarColors(Color(0xFFE0F2FE), Color(0xFF0369A1)),
    AvatarColors(Color(0xFFFEE2E2), Color(0xFFB91C1C)),
    AvatarColors(Color(0xFFFEF3C7), Color(0xFFB45309)),
    AvatarColors(Color(0xFFF3E8FF), Color(0xFF7E22CE)),
    AvatarColors(Color(0xFFCCFBF1), Color(0xFF0F766E)),
)

private val Hairline = Color(0xFFE6E1D8)
private val MutedBorder = Color(0xFFD5D2CB)
private val Coral = Color(0xFFFF6B57)
private val SuccessGreen = Color(0xFF2E9E6B)
private val DangerRed = Color(0xFFE5484D)
private val WarningAmber = Color(0xFFD9871D)
private val JustifiedBlue = Color(0xFF2E5AAC)
private val UnmarkedGrey = Color(0xFF6B6E8F)
