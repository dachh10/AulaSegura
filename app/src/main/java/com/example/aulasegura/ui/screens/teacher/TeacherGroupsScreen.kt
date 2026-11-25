package com.example.aulasegura.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ProgressRing
import com.example.aulasegura.ui.components.TeacherBottomBar
import com.example.aulasegura.ui.components.TeacherTab
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnboardingHeading
import com.example.aulasegura.ui.theme.OnboardingMuted
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** 3.2 — Mis grupos (panel del docente). Ported 1:1 from the Stitch markup. */

/** Hairline borders used across every card in the mockup (#E6E1D8). */
private val Hairline = Color(0xFFE6E1D8)

/** Group badge background and divider tint from the mockup. */
private val BadgeSurface = Color(0xFFEEF0F9)
private val SoftDivider = Color(0xFFF0EDE9)

/** "Lista pendiente" pill. */
private val PendingPillBg = Color(0xFFFEF3C7)
private val PendingPillText = Color(0xFF92400E)

/** "Lista hecha" pill. */
private val DonePillBg = Color(0xFFD1FAE5)
private val DonePillText = Color(0xFF065F46)

private val GroupShape = RoundedCornerShape(24.dp)
private val BadgeShape = RoundedCornerShape(16.dp)

private enum class GroupFilter {
    Todos,
    Hoy,
    Materia,
}

/**
 * The third chip names the subject this teacher actually teaches, not a
 * hardcoded "Matemáticas": the label and the filter it applies are the same
 * string, so they can never disagree.
 */
private fun GroupFilter.labelFor(subject: String?): String = when (this) {
    GroupFilter.Todos -> "Todos"
    GroupFilter.Hoy -> "Hoy"
    GroupFilter.Materia -> subject ?: "Materia"
}

@Composable
fun TeacherGroupsScreen(
    groups: List<TeacherGroupUi> = emptyList(),
    refreshing: Boolean = true,
    notice: String? = null,
    onRetry: () -> Unit = {},
    onIrAInicio: () -> Unit = {},
    onIrAChat: () -> Unit = {},
    onIrAHistorial: () -> Unit = {},
    onIrAPerfil: () -> Unit = {},
    onIrANotificaciones: () -> Unit = {},
    onPasarLista: (String, String) -> Unit = { _, _ -> },
    onVerAlumnos: (String) -> Unit = {},
) {
    var filter by remember { mutableStateOf(GroupFilter.Todos) }

    val subjects = remember(groups) { distinctGroupSubjects(groups) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    val subjectForFilter = selectedSubject ?: subjects.firstOrNull()

    val visible = remember(filter, groups, subjectForFilter) {
        when (filter) {
            GroupFilter.Todos -> groups
            GroupFilter.Hoy -> groups.filter { it.isToday }
            GroupFilter.Materia -> groups.filter { it.subject == subjectForFilter }
        }
    }

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
                .padding(bottom = 96.dp),
        ) {
            Reveal(0) { GroupsHeader(onNotifications = onIrANotificaciones) }

            Reveal(60) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GroupFilter.entries.forEach { option ->
                        FilterChip(
                            label = option.labelFor(subjectForFilter),
                            selected = option == filter,
                            onClick = {
                                filter = option
                                if (option == GroupFilter.Materia && selectedSubject == null) {
                                    selectedSubject = subjects.firstOrNull()
                                }
                            },
                        )
                    }
                }
                if (filter == GroupFilter.Materia && subjects.size > 1) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        subjects.forEach { subject ->
                            FilterChip(
                                label = subject,
                                selected = subject == subjectForFilter,
                                onClick = { selectedSubject = subject },
                            )
                        }
                    }
                }
            }

            Reveal(120) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (notice != null) {
                        GroupsNotice(message = notice, onRetry = onRetry)
                    }

                    if (visible.isEmpty()) {
                        GroupsEmptyState(refreshing = refreshing, hasGroups = groups.isNotEmpty())
                    }

                    visible.forEach { group ->
                        TeacherGroupCard(
                            group = group,
                            onPasarLista = { onPasarLista(group.id, group.subjectId) },
                            onVerAlumnos = { onVerAlumnos(group.id) },
                        )
                    }
                }
            }
        }

        TeacherBottomBar(
            active = TeacherTab.Grupos,
            onSelect = { tab ->
                when (tab) {
                    TeacherTab.Inicio -> onIrAInicio()
                    TeacherTab.Grupos -> Unit
                    TeacherTab.Chat -> onIrAChat()
                    TeacherTab.Historial -> onIrAHistorial()
                    TeacherTab.Perfil -> onIrAPerfil()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun GroupsNotice(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PendingPillBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = PendingPillText,
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(PendingPillText)
                .clickable(onClick = onRetry)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Reintentar",
                color = Color.White,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

/**
 * Three different silences, said out loud: still loading, nothing for this
 * filter, and no groups assigned at all. One generic "no hay datos" would make
 * a teacher with no assignment think the app is broken.
 */
@Composable
private fun GroupsEmptyState(refreshing: Boolean, hasGroups: Boolean) {
    val message = when {
        refreshing && !hasGroups -> "Cargando tus grupos…"
        hasGroups -> "Ningún grupo coincide con este filtro."
        else -> "No tienes grupos asignados. Pídele al directivo que te asigne uno."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GroupShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, GroupShape)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = OnboardingMuted,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// ZONE 1 — Top app bar
@Composable
private fun GroupsHeader(onNotifications: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mis grupos",
                color = OnboardingHeading,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.02).em,
            )
            Text(
                text = "Ciclo escolar 2026-2027",
                color = OnboardingMuted,
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
        ProductNotificationIcon(onClick = onNotifications)
    }
}

@Composable
private fun ProductNotificationIcon(onClick: () -> Unit) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    Box(
        modifier = Modifier
            .size(36.dp)
            .then(pressModifier)
            .clip(CircleShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Notificaciones",
            tint = PrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(BrandCoral)
                .border(2.dp, SurfaceContainerLowest, CircleShape),
        )
    }
}

// ZONE 2 — Filter chips
@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    Box(
        modifier = Modifier
            .height(34.dp)
            .then(pressModifier)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) PrimaryContainer else SurfaceContainerLowest)
            .border(
                width = 1.dp,
                color = if (selected) PrimaryContainer else Hairline,
                shape = RoundedCornerShape(percent = 50),
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = if (selected) 16.dp else 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else OnboardingHeading,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.02.em,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp),
        )
    }
}

// ZONE 3 — Group cards
@Composable
private fun TeacherGroupCard(
    group: TeacherGroupUi,
    onPasarLista: () -> Unit,
    onVerAlumnos: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GroupShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, GroupShape)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (group.time != null) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = OnboardingMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = group.time,
                        color = OnboardingMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                    )
                }
            }
            if (group.rollCallDone != null) {
                RollCallPill(done = group.rollCallDone)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(BadgeShape)
                    .background(BadgeSurface)
                    .border(1.dp, PrimaryContainer.copy(alpha = 0.10f), BadgeShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = group.label,
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.subject,
                    color = OnboardingHeading,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                )
                Text(
                    text = "${group.students} alumnos",
                    color = OnboardingMuted,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            }
            if (group.attendance != null) {
                Spacer(modifier = Modifier.width(8.dp))
                AttendanceGauge(percent = group.attendance)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SoftDivider),
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PassListChip(onClick = onPasarLista)
            SeeStudentsButton(onClick = onVerAlumnos)
        }
    }
}

@Composable
private fun RollCallPill(done: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (done) DonePillBg else PendingPillBg)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (done) Icons.Filled.Check else Icons.Filled.Schedule,
            contentDescription = null,
            tint = if (done) DonePillText else PendingPillText,
            modifier = Modifier.size(13.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (done) "Lista hecha" else "Lista pendiente",
            color = if (done) DonePillText else PendingPillText,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.03.em,
        )
    }
}

@Composable
private fun AttendanceGauge(percent: Int) {
    ProgressRing(
        progress = percent / 100f,
        trackColor = SoftDivider,
        progressColor = PrimaryContainer,
        size = 48.dp,
        strokeWidth = 3.5.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                color = OnboardingHeading,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 12.sp,
            )
            Text(
                text = "ASIST.",
                color = OnboardingMuted,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                letterSpacing = (-0.02).em,
            )
        }
    }
}

@Composable
private fun PassListChip(onClick: () -> Unit) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    Row(
        modifier = Modifier
            .height(32.dp)
            .then(pressModifier)
            .clip(RoundedCornerShape(percent = 50))
            .background(BrandCoral)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Checklist,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Pasar lista",
            color = Color.White,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.02.em,
        )
    }
}

@Composable
private fun SeeStudentsButton(onClick: () -> Unit) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    Row(
        modifier = Modifier
            .then(pressModifier)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Ver alumnos",
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.02.em,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PrimaryContainer,
            modifier = Modifier.size(17.dp),
        )
    }
}
