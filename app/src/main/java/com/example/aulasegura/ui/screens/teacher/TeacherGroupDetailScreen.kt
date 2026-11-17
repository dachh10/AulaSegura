package com.example.aulasegura.ui.screens.teacher

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.RollCallSync
import com.example.aulasegura.ui.components.AulaField
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandMintDeep
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.OnTertiaryFixedVariant
import com.example.aulasegura.ui.theme.OnboardingHeading
import com.example.aulasegura.ui.theme.OnboardingMuted
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.Secondary
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerHighest
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryFixed
import com.example.aulasegura.ui.theme.OnSecondaryFixedVariant

/**
 * 3.3/3.4/3.5 — Detalle de grupo (docente), con las pestañas Alumnos,
 * Asistencia e Incidentes.
 *
 * Lo que se portó del mockup es la estructura y el lenguaje visual; lo que se
 * dejó fuera es lo que el proyecto no tiene: el «Aula 12» (no existe en el
 * esquema), la barra de estado simulada, los botones de calendario/ajustes que
 * no llevan a ninguna parte, el chat por alumno (el docente no abre una
 * conversación desde un alumno) y el resumen de AulaBot (texto que nadie
 * generó). Cada pestaña se llena con datos reales: el roster, las listas
 * cerradas de este grupo y los incidentes reportados para él.
 *
 * La pantalla es sin estado: recibe [ui] ya resuelto y avisa por callbacks.
 */
@Composable
fun TeacherGroupDetailScreen(
    ui: TeacherGroupDetailUi,
    onBack: () -> Unit = {},
    onPasarLista: () -> Unit = {},
    onNuevaTarea: () -> Unit = {},
    onNuevoAviso: () -> Unit = {},
    onRegistrarIncidente: () -> Unit = {},
    onMonthChange: (Int) -> Unit = {},
    onToggleIncident: (String) -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(GroupDetailTab.Alumnos) }
    var query by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 104.dp),
        ) {
            Reveal(0) { GroupHero(ui = ui, onBack = onBack) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Reveal(60) { GroupStats(ui = ui) }

                Reveal(100) {
                    GroupQuickActions(
                        onPasarLista = onPasarLista,
                        onNuevaTarea = onNuevaTarea,
                        onNuevoAviso = onNuevoAviso,
                    )
                }

                if (ui.notice != null) {
                    GroupNotice(message = ui.notice)
                }

                Reveal(140) {
                    GroupTabs(selected = tab, onSelect = { tab = it })
                }

                Crossfade(targetState = tab, animationSpec = tween(220), label = "groupTab") { shown ->
                    when (shown) {
                        GroupDetailTab.Alumnos -> StudentsTab(
                            ui = ui,
                            query = query,
                            onQueryChange = { query = it },
                        )

                        GroupDetailTab.Asistencia -> AttendanceTab(
                            ui = ui,
                            onMonthChange = onMonthChange,
                        )

                        GroupDetailTab.Incidentes -> IncidentsTab(
                            ui = ui,
                            onToggleIncident = onToggleIncident,
                            onRegistrarIncidente = onRegistrarIncidente,
                        )
                    }
                }
            }
        }

        PassListBar(
            onClick = onPasarLista,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private enum class GroupDetailTab(val label: String) {
    Alumnos("Alumnos"),
    Asistencia("Asistencia"),
    Incidentes("Incidentes"),
}

/* ------------------------------------------------------------------------- */
/* Hero                                                                       */
/* ------------------------------------------------------------------------- */

@Composable
private fun GroupHero(ui: TeacherGroupDetailUi, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(PrimaryContainer),
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 4.dp)
                .size(220.dp)
                .alpha(0.06f),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (interaction, press) = rememberPressScale(0.94f)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .then(press)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onBack,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Detalle de grupo",
                    color = Color.White,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceContainerLowest),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ui.label,
                        color = Primary,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        letterSpacing = (-0.01).em,
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ui.subject,
                        color = Color.White,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (ui.slotLabel != null) {
                        Text(
                            text = ui.slotLabel,
                            color = Color.White.copy(alpha = 0.75f),
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ListStateChip(state = ui.listState)
                }
            }
        }
    }
}

@Composable
private fun ListStateChip(state: GroupListState) {
    val text = when (state) {
        GroupListState.Pending -> "Lista pendiente"
        GroupListState.Done -> "Lista hecha"
        GroupListState.NoClass -> "Hoy no hay clases"
        GroupListState.Loading -> return
    }

    val dot = when (state) {
        GroupListState.Done -> TertiaryFixed
        else -> BrandCoral
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state == GroupListState.Pending) {
            PulsingDot(color = dot)
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = when (state) {
                GroupListState.Done -> Icons.Filled.CheckCircle
                GroupListState.NoClass -> Icons.Filled.CalendarToday
                else -> Icons.Filled.Schedule
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            color = Color.White,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.03.em,
        )
    }
}

/** The one animation the mockup really has: the pending-list dot breathing. */
@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "pendingPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pendingAlpha",
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color),
    )
}

/* ------------------------------------------------------------------------- */
/* Stats and quick actions                                                    */
/* ------------------------------------------------------------------------- */

@Composable
private fun GroupStats(ui: TeacherGroupDetailUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(
            // Mientras el roster llega, un "0" diría que el grupo está vacío.
            value = if (ui.refreshing && ui.students.isEmpty()) "—" else ui.students.size.toString(),
            label = "Alumnos",
            color = Primary,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = ui.attendancePercent?.let { "$it%" } ?: "—",
            label = "Asistencia",
            color = BrandMintDeep,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = ui.absencesToday?.toString() ?: "—",
            label = "Faltas hoy",
            color = Secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * A number with a name under it.
 *
 * `—` is a real answer: without roll calls there is no attendance to average,
 * and without a session today there are no absences to report. Printing `0`
 * would say the opposite.
 */
@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, HairlineColor, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = color,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
        Text(
            text = label,
            color = OnboardingMuted,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.02.em,
        )
    }
}

@Composable
private fun GroupQuickActions(
    onPasarLista: () -> Unit,
    onNuevaTarea: () -> Unit,
    onNuevoAviso: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickAction(
            icon = Icons.AutoMirrored.Filled.FactCheck,
            label = "Pasar lista",
            tint = BrandCoral,
            container = BrandCoral.copy(alpha = 0.14f),
            strong = true,
            onClick = onPasarLista,
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            icon = Icons.AutoMirrored.Filled.Assignment,
            label = "Tarea",
            tint = Primary,
            container = PrimaryFixed,
            onClick = onNuevaTarea,
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            icon = Icons.Filled.Campaign,
            label = "Aviso",
            tint = Primary,
            container = PrimaryFixed,
            onClick = onNuevoAviso,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
) {
    val (interaction, press) = rememberPressScale(0.95f)
    Column(
        modifier = modifier
            .then(press)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, HairlineColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            color = if (strong) OnboardingHeading else OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 16.sp,
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Tabs                                                                       */
/* ------------------------------------------------------------------------- */

@Composable
private fun GroupTabs(selected: GroupDetailTab, onSelect: (GroupDetailTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(SurfaceContainer)
            .padding(4.dp),
    ) {
        GroupDetailTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val background by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSelected) Primary else Color.Transparent,
                animationSpec = tween(200),
                label = "tabBackground",
            )
            val textColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSelected) Color.White else OnSurfaceVariant,
                animationSpec = tween(200),
                label = "tabText",
            )
            val (interaction, press) = rememberPressScale(0.97f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(press)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(background)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onSelect(tab) },
                    )
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    color = textColor,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Alumnos                                                                    */
/* ------------------------------------------------------------------------- */

@Composable
private fun StudentsTab(
    ui: TeacherGroupDetailUi,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val visible = remember(ui.students, query) { searchGroupStudents(ui.students, query) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AulaField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Buscar alumno por nombre o número…",
            leadingIcon = Icons.Filled.Search,
            height = 48.dp,
            shape = RoundedCornerShape(16.dp),
        )

        if (visible.isEmpty()) {
            EmptyCard(
                message = when {
                    ui.students.isEmpty() && ui.refreshing -> "Cargando el grupo…"
                    ui.students.isEmpty() -> "Este grupo aún no tiene alumnos en la lista."
                    else -> "Ningún alumno coincide con la búsqueda."
                },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, HairlineColor, RoundedCornerShape(20.dp)),
            ) {
                visible.forEachIndexed { index, student ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SoftDivider),
                        )
                    }
                    StudentRow(student = student, index = index)
                }
            }
        }
    }
}

@Composable
private fun StudentRow(student: GroupStudentUi, index: Int) {
    val (avatarBackground, avatarText) = AVATAR_COLORS[index % AVATAR_COLORS.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = student.initials,
                color = avatarText,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.fullName,
                color = OnboardingHeading,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "N° ${student.number}",
                color = OnboardingMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Asistencia                                                                 */
/* ------------------------------------------------------------------------- */

@Composable
private fun AttendanceTab(
    ui: TeacherGroupDetailUi,
    onMonthChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CalendarCard(
            calendar = ui.calendar,
            canGoNextMonth = ui.canGoNextMonth,
            onMonthChange = onMonthChange,
        )
        SessionsCard(sessions = ui.sessions)
    }
}

@Composable
private fun CalendarCard(
    calendar: GroupCalendarUi,
    canGoNextMonth: Boolean,
    onMonthChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, HairlineColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MonthArrow(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                label = "Mes anterior",
                enabled = true,
                onClick = { onMonthChange(-1) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = calendar.monthLabel,
                    color = OnboardingHeading,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
            }
            MonthArrow(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                label = "Mes siguiente",
                enabled = canGoNextMonth,
                onClick = { onMonthChange(1) },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAYS.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    color = OnboardingMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Monday–Friday grid, five cells per row; the first week is pushed by
        // `leadingBlanks` so the days land on their real weekday.
        val totalCells = calendar.leadingBlanks + calendar.days.size
        val rows = (totalCells + WEEKDAYS.size - 1) / WEEKDAYS.size

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(rows) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(WEEKDAYS.size) { column ->
                        val cellIndex = row * WEEKDAYS.size + column - calendar.leadingBlanks
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            calendar.days.getOrNull(cellIndex)?.let { day ->
                                CalendarDay(day)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SoftDivider),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LegendItem(background = TertiaryFixed.copy(alpha = 0.6f), border = TertiaryFixed, label = "≥95%")
            LegendItem(
                background = SecondaryFixed.copy(alpha = 0.4f),
                border = SecondaryFixed,
                label = "85-94%",
            )
            LegendItem(background = SecondaryFixed, border = Secondary, label = "<85%")
        }
    }
}

@Composable
private fun MonthArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, press) = rememberPressScale(0.92f)
    Box(
        modifier = Modifier
            .size(34.dp)
            .then(press)
            .clip(CircleShape)
            .background(if (enabled) SurfaceContainer else Color.Transparent)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) OnboardingHeading else OnboardingMuted.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CalendarDay(day: GroupCalendarDayUi) {
    val percent = day.percent
    val background = when {
        percent == null -> SurfaceContainer
        percent >= HIGH_ATTENDANCE -> TertiaryFixed.copy(alpha = 0.55f)
        percent >= MID_ATTENDANCE -> SecondaryFixed.copy(alpha = 0.4f)
        else -> SecondaryFixed
    }
    val foreground = when {
        percent == null -> Outline
        percent >= HIGH_ATTENDANCE -> OnTertiaryFixedVariant
        else -> OnSecondaryFixedVariant
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (day.isToday) {
                    Modifier.border(2.dp, Primary, CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.day.toString(),
                color = foreground,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 14.sp,
            )
            Text(
                text = percent?.let { "$it%" } ?: "--",
                color = foreground,
                fontFamily = JetBrainsMono,
                fontSize = 9.sp,
                lineHeight = 11.sp,
            )
        }
    }
}

@Composable
private fun LegendItem(background: Color, border: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(background)
                .border(2.dp, border, CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = OnboardingMuted,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            lineHeight = 12.sp,
        )
    }
}

@Composable
private fun SessionsCard(sessions: List<GroupSessionUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, HairlineColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.HistoryEdu,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Últimas listas",
                    color = OnboardingHeading,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
            }
            Text(
                text = if (sessions.size == 1) "1 registrada" else "${sessions.size} registradas",
                color = OnboardingMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (sessions.isEmpty()) {
            Text(
                text = "Aún no hay listas guardadas en este mes.",
                color = OnboardingMuted,
                fontFamily = DmSans,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
            return@Column
        }

        sessions.forEachIndexed { index, session ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SoftDivider),
                )
            }
            SessionRow(session)
        }
    }
}

@Composable
private fun SessionRow(session: GroupSessionUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.dayLabel,
                    color = OnboardingHeading,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = session.timeLabel,
                    color = OnboardingMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CountChip(count = session.present, suffix = "P", tone = CountTone.Present)
                CountChip(count = session.absent, suffix = "F", tone = CountTone.Absent)
                CountChip(count = session.late, suffix = "R", tone = CountTone.Late)
                if (session.justified > 0) {
                    CountChip(count = session.justified, suffix = "J", tone = CountTone.Justified)
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        SyncPill(sync = session.sync)
    }
}

private enum class CountTone { Present, Absent, Late, Justified }

@Composable
private fun CountChip(count: Int, suffix: String, tone: CountTone) {
    val (background, textColor) = when (tone) {
        CountTone.Present -> TertiaryFixed.copy(alpha = 0.5f) to OnTertiaryFixedVariant
        CountTone.Absent -> SecondaryFixed to OnSecondaryFixedVariant
        CountTone.Late -> SecondaryFixed.copy(alpha = 0.4f) to OnSecondaryFixedVariant
        CountTone.Justified -> PrimaryFixed to Primary
    }

    Text(
        text = "$count $suffix",
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = textColor,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
    )
}

/**
 * `Sincronizado` only when Room says the session reached Firestore; anything
 * else keeps its real phase, with the same wording the history uses.
 */
@Composable
private fun SyncPill(sync: RollCallSync) {
    val synced = sync == RollCallSync.SYNCED
    val (background, textColor) = if (synced) {
        TertiaryFixed.copy(alpha = 0.5f) to OnTertiaryFixedVariant
    } else {
        SecondaryFixed.copy(alpha = 0.6f) to OnSecondaryFixedVariant
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (synced) Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(13.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = sessionSyncLabel(sync),
            color = textColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 12.sp,
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Incidentes                                                                 */
/* ------------------------------------------------------------------------- */

@Composable
private fun IncidentsTab(
    ui: TeacherGroupDetailUi,
    onToggleIncident: (String) -> Unit,
    onRegistrarIncidente: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (ui.incidents.isEmpty()) {
            EmptyCard(
                message = if (ui.refreshing) {
                    "Cargando los incidentes del grupo…"
                } else {
                    "No has registrado incidentes en este grupo."
                },
            )
        }

        ui.incidents.forEach { incident ->
            IncidentCard(
                incident = incident,
                onToggle = { onToggleIncident(incident.id) },
            )
        }

        val (interaction, press) = rememberPressScale(0.98f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .then(press)
                .clip(RoundedCornerShape(percent = 50))
                .border(1.dp, Primary, RoundedCornerShape(percent = 50))
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onRegistrarIncidente,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Registrar incidente",
                color = Primary,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun IncidentCard(incident: GroupIncidentUi, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, HairlineColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onToggle)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainerHighest)
                    .border(1.dp, HairlineColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (incident.mediaCount > 0) Icons.Filled.PhotoCamera else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Outline,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (incident.severityLabel != null) {
                        SeverityChip(label = incident.severityLabel, severity = incident.severity)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = incident.occurredLabel,
                        color = OnboardingMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = incident.title,
                    color = OnboardingHeading,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = incident.studentName,
                    color = OnboardingMuted,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SoftDivider),
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = if (incident.locationLabel != null) BrandMintDeep else Outline,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = incident.locationLabel ?: "Sin ubicación registrada",
                    color = if (incident.locationLabel != null) BrandMintDeep else OnboardingMuted,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (incident.expanded) "Ocultar" else "Ver",
                    color = Primary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        if (!incident.expanded) return@Column

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = incident.description,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (incident.categoryLabel.isNotBlank()) {
                MetaChip(text = incident.categoryLabel)
            }
            MetaChip(text = incident.statusLabel)
            if (incident.mediaCount > 0) {
                MetaChip(
                    text = if (incident.mediaCount == 1) {
                        "1 archivo"
                    } else {
                        "${incident.mediaCount} archivos"
                    },
                )
            }
        }
    }
}

@Composable
private fun SeverityChip(label: String, severity: IncidentSeverity?) {
    val (background, textColor) = when (severity) {
        IncidentSeverity.LEVE -> SecondaryFixed.copy(alpha = 0.45f) to OnSecondaryFixedVariant
        IncidentSeverity.MODERADO -> SecondaryFixed to Secondary
        IncidentSeverity.GRAVE -> Secondary to Color.White
        null -> SurfaceContainer to OnSurfaceVariant
    }

    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = textColor,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
    )
}

@Composable
private fun MetaChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(SurfaceContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = OnSurfaceVariant,
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
    )
}

/* ------------------------------------------------------------------------- */
/* Shared pieces                                                              */
/* ------------------------------------------------------------------------- */

@Composable
private fun GroupNotice(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WarningSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = WarningText,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            color = WarningText,
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun EmptyCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, HairlineColor, RoundedCornerShape(20.dp))
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

@Composable
private fun PassListBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.96f)),
    ) {
        val (interaction, press) = rememberPressScale(0.98f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(48.dp)
                .then(press)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(percent = 50),
                    ambientColor = BrandCoral.copy(alpha = 0.35f),
                    spotColor = BrandCoral.copy(alpha = 0.35f),
                )
                .clip(RoundedCornerShape(percent = 50))
                .background(BrandCoral)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Checklist,
                contentDescription = null,
                tint = OnSecondaryContainerInk,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Pasar lista",
                color = OnSecondaryContainerInk,
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Constants                                                                  */
/* ------------------------------------------------------------------------- */

/** Hairline of every card, the same tint the group cards use (#E6E1D8). */
private val HairlineColor = Color(0xFFE6E1D8)

/** Divider between rows, from the mockup. */
private val SoftDivider = Color(0xFFF0EDE9)

private val WarningSurface = Color(0xFFFEF3C7)
private val WarningText = Color(0xFF92400E)

/** Coral buttons print dark ink, as in the mockup. */
private val OnSecondaryContainerInk = Color(0xFF690000)

private val WEEKDAYS = listOf("L", "M", "M", "J", "V")

/** Attendance thresholds of the calendar legend. */
private const val HIGH_ATTENDANCE = 95
private const val MID_ATTENDANCE = 85

private val AVATAR_COLORS = listOf(
    TertiaryFixed to OnTertiaryFixedVariant,
    PrimaryFixed to Primary,
    SurfaceContainerHighest to OnSurfaceVariant,
)
