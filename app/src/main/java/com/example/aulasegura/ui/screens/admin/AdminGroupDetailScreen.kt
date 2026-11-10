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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.shieldPattern
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** 4.11 — Detalle de grupo (Administración). Ported 1:1 from the Stitch mockup. */

private val Hairline = Color(0xFFE6E1D8)
private val RowDivider = Color(0xFFF0EBE1)
private val Muted = Color(0xFF6B7280)
private val Ink = Color(0xFF111827)
private val TrackShape = RoundedCornerShape(percent = 50)

private val GreenChipBg = Color(0xFFECFDF5)
private val GreenChipBorder = Color(0xFFA7F3D0)
private val GreenChipInk = Color(0xFF047857)
private val AmberChipBg = Color(0xFFFFFBEB)
private val AmberChipBorder = Color(0xFFFDE68A)
private val AmberChipInk = Color(0xFFB45309)
private val RedChipBg = Color(0xFFFEF2F2)
private val RedChipBorder = Color(0xFFFECACA)
private val RedChipInk = Color(0xFFDC2626)

private enum class GroupTab(val label: String) {
    Alumnos("Alumnos"),
    Docentes("Docentes"),
    Horario("Horario"),
}

private data class GroupStudent(
    val initials: String,
    val name: String,
    val number: String,
    val attendance: Int,
    val absences: Int,
    val avatarBg: Color,
    val avatarBorder: Color,
    val avatarInk: Color,
)

private val students = listOf(
    GroupStudent(
        "AA", "Ana Sofía Aguilar", "N° 01", 98, 0,
        Color(0xFFEEF0FB), Color(0xFFDCDFF7), PrimaryContainer,
    ),
    GroupStudent(
        "BC", "Bruno Castillo", "N° 02", 96, 0,
        Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color(0xFF1E40AF),
    ),
    GroupStudent(
        "CD", "Camila Domínguez", "N° 03", 78, 5,
        Color(0xFFFEF2F2), Color(0xFFFEE2E2), Color(0xFFDC2626),
    ),
    GroupStudent(
        "DE", "Diego Escobar", "N° 04", 92, 0,
        Color(0xFFF0FDFA), Color(0xFFCCFBF1), Color(0xFF0F766E),
    ),
    GroupStudent(
        "EF", "Elena Fuentes", "N° 05", 90, 0,
        Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFF92400E),
    ),
    GroupStudent(
        "MG", "Mariana Gómez Ruiz", "N° 06", 95, 0,
        Color(0xFFF5F3FF), Color(0xFFEDE9FE), Color(0xFF6D28D9),
    ),
)

private data class GroupTeacher(val initials: String, val name: String, val role: String)

private val teachers = listOf(
    GroupTeacher("RS", "Ricardo Salinas", "Titular · Matemáticas"),
    GroupTeacher("LM", "Laura Mendoza", "Español"),
    GroupTeacher("HV", "Héctor Vega", "Ciencias Naturales"),
)

private data class ScheduleSlot(val time: String, val subject: String, val teacher: String)

private val schedule = listOf(
    ScheduleSlot("07:30 – 08:20", "Matemáticas", "Prof. Salinas"),
    ScheduleSlot("08:20 – 09:10", "Español", "Prof. Mendoza"),
    ScheduleSlot("09:10 – 10:00", "Ciencias Naturales", "Prof. Vega"),
    ScheduleSlot("10:30 – 11:20", "Historia", "Prof. Salinas"),
    ScheduleSlot("11:20 – 12:10", "Educación Física", "Prof. Alarcón"),
)

@Composable
fun AdminGroupDetailScreen(
    groupLabel: String = "3° B",
    onBack: () -> Unit = {},
    onMore: () -> Unit = {},
    onAddStudent: () -> Unit = {},
    onStudentClick: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(GroupTab.Alumnos) }
    var query by rememberSaveable { mutableStateOf("") }

    val filteredStudents = remember(query) {
        if (query.isBlank()) {
            students
        } else {
            students.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Reveal(0) { GroupHeader(groupLabel = groupLabel, onBack = onBack, onMore = onMore) }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .revealOnEnter(60)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GroupStat(value = "28", label = "Alumnos", valueColor = PrimaryContainer, modifier = Modifier.weight(1f))
                    GroupStat(value = "94%", label = "Asistencia", valueColor = Color(0xFF059669), modifier = Modifier.weight(1f))
                    GroupStat(value = "1", label = "Incidentes", valueColor = SecondaryContainer, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                GroupTabs(selected = tab, onSelect = { tab = it })

                Spacer(modifier = Modifier.height(12.dp))

                when (tab) {
                    GroupTab.Alumnos -> StudentsTab(
                        query = query,
                        onQueryChange = { query = it },
                        items = filteredStudents,
                        onAddStudent = onAddStudent,
                        onStudentClick = onStudentClick,
                    )
                    GroupTab.Docentes -> TeachersTab()
                    GroupTab.Horario -> ScheduleTab()
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(groupLabel: String, onBack: () -> Unit, onMore: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(PrimaryContainer)
            .shieldPattern(alpha = 0.07f, color = Color.White)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleHeaderButton(onClick = onBack, icon = Icons.Filled.ArrowBack, description = "Volver")
            Text(
                text = "3° Primaria - Grupo B",
                color = Color.White,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = (-0.18).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            CircleHeaderButton(onClick = onMore, icon = Icons.Filled.MoreVert, description = "Opciones")
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = groupLabel,
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.36).sp,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Grupo $groupLabel",
                    color = Color.White,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.36).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Titular: Prof. Ricardo Salinas",
                    color = Color.White.copy(alpha = 0.80f),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Ciclo escolar 2026-2027",
                    color = Color.White.copy(alpha = 0.70f),
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun CircleHeaderButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "groupHeaderButtonScale",
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun GroupStat(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = valueColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.32).sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Muted,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.24.sp,
        )
    }
}

@Composable
private fun GroupTabs(selected: GroupTab, onSelect: (GroupTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(TrackShape)
            .background(SurfaceContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GroupTab.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(TrackShape)
                    .background(if (isSelected) PrimaryContainer else Color.Transparent)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    color = if (isSelected) Color.White else Color(0xFF4B5563),
                    fontFamily = DmSans,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.24.sp,
                )
            }
        }
    }
}

@Composable
private fun StudentsTab(
    query: String,
    onQueryChange: (String) -> Unit,
    items: List<GroupStudent>,
    onAddStudent: () -> Unit,
    onStudentClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, Hairline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Buscar alumno",
                            color = Color(0xFF9CA3AF),
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Ink,
                            fontFamily = DmSans,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        ),
                        cursorBrush = SolidColor(PrimaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.95f else 1f,
                animationSpec = tween(durationMillis = 150),
                label = "addStudentScale",
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(PrimaryContainer)
                    .clickable(interactionSource = interaction, indication = null, onClick = onAddStudent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = "Agregar alumno",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLowest)
                .border(1.dp, Hairline, RoundedCornerShape(24.dp)),
        ) {
            if (items.isEmpty()) {
                Text(
                    text = "Sin resultados para \"$query\"",
                    color = Muted,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            } else {
                items.forEachIndexed { index, student ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(RowDivider),
                        )
                    }
                    StudentRow(student = student, onClick = onStudentClick)
                }
            }
        }
    }
}

@Composable
private fun StudentRow(student: GroupStudent, onClick: () -> Unit) {
    val muted = student.attendance < 80

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(student.avatarBg)
                    .border(1.dp, student.avatarBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = student.initials,
                    color = student.avatarInk,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = student.name,
                    color = Ink,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = student.number,
                        color = Muted,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    )
                    if (student.absences > 0) {
                        Text(
                            text = " · ${student.absences} faltas",
                            color = RedChipInk.copy(alpha = 0.85f),
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            AttendanceChip(percent = student.attendance, warning = muted)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AttendanceChip(percent: Int, warning: Boolean) {
    val bg: Color
    val border: Color
    val ink: Color
    when {
        warning -> {
            bg = RedChipBg
            border = RedChipBorder
            ink = RedChipInk
        }
        percent < 95 -> {
            bg = AmberChipBg
            border = AmberChipBorder
            ink = AmberChipInk
        }
        else -> {
            bg = GreenChipBg
            border = GreenChipBorder
            ink = GreenChipInk
        }
    }

    Row(
        modifier = Modifier
            .clip(TrackShape)
            .background(bg)
            .border(1.dp, border, TrackShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (warning) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = "$percent%",
            color = ink,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun TeachersTab() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLowest)
                .border(1.dp, Hairline, RoundedCornerShape(24.dp)),
        ) {
            teachers.forEachIndexed { index, teacher ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(RowDivider),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF0FB))
                                .border(1.dp, Color(0xFFDCDFF7), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = teacher.initials,
                                color = PrimaryContainer,
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = teacher.name,
                                color = Ink,
                                fontFamily = DmSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                            )
                            Text(
                                text = teacher.role,
                                color = Muted,
                                fontFamily = DmSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleTab() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, RoundedCornerShape(24.dp)),
    ) {
        schedule.forEachIndexed { index, slot ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(RowDivider),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = slot.time,
                    color = PrimaryContainer,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.width(104.dp),
                )
                Column {
                    Text(
                        text = slot.subject,
                        color = Ink,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    Text(
                        text = slot.teacher,
                        color = Muted,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}
