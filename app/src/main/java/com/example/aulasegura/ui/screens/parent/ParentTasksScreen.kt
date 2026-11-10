package com.example.aulasegura.ui.screens.parent

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TaskAlt
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ParentBottomBar
import com.example.aulasegura.ui.components.ParentTab
import com.example.aulasegura.ui.components.StudentPill
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.Secondary
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** 2.3 — Tareas. Ported from Stitch screen `58f3dbb8b1f2486abcfd8efe5dde189b`. */

private val Emerald50 = Color(0xFFECFDF5)
private val Emerald200 = Color(0xFFA7F3D0)
private val Emerald700 = Color(0xFF047857)
private val Emerald800 = Color(0xFF065F46)
private val Amber50 = Color(0xFFFFFBEB)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber200 = Color(0xFFFDE68A)
private val Amber700 = Color(0xFFB45309)
private val Amber900 = Color(0xFF78350F)

private enum class TaskFilter(val label: String) {
    Pendientes("Pendientes"),
    Entregadas("Entregadas"),
    Vencidas("Vencidas"),
    Todas("Todas"),
}

private enum class TaskStatus { Pending, Delivered }

private sealed interface FooterLeft {
    data class Teacher(val name: String) : FooterLeft
    data class Plain(val text: String) : FooterLeft
    data class Attachment(val name: String) : FooterLeft
    data class Reviewed(val text: String) : FooterLeft
}

private data class SubjectTask(
    val subject: String,
    val title: String,
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color,
    val footerLeft: FooterLeft,
    val footerRight: String,
    val footerRightIcon: ImageVector,
    val footerRightColor: Color,
    val footerRightIconColor: Color,
    val status: TaskStatus,
    val section: String,
)

private fun sampleTasks(): List<SubjectTask> = listOf(
    SubjectTask(
        subject = "Matemáticas",
        title = "Fracciones, ejercicios 1-10",
        icon = Icons.Outlined.Calculate,
        iconBackground = SecondaryFixed,
        iconTint = Secondary,
        footerLeft = FooterLeft.Teacher("Prof. Ricardo Salinas"),
        footerRight = "Mañana, 08:00",
        footerRightIcon = Icons.Outlined.Alarm,
        footerRightColor = SecondaryContainer,
        footerRightIconColor = SecondaryContainer,
        status = TaskStatus.Pending,
        section = "PARA MAÑANA",
    ),
    SubjectTask(
        subject = "Español",
        title = "Resumen del cuento 'La Luna'",
        icon = Icons.Outlined.AutoStories,
        iconBackground = PrimaryFixed,
        iconTint = PrimaryContainer,
        footerLeft = FooterLeft.Plain("Lectura comprensiva"),
        footerRight = "Vie 7 nov",
        footerRightIcon = Icons.Outlined.CalendarToday,
        footerRightColor = OnSurfaceVariant,
        footerRightIconColor = Outline,
        status = TaskStatus.Pending,
        section = "ESTA SEMANA",
    ),
    SubjectTask(
        subject = "Ciencias",
        title = "Maqueta del sistema solar",
        icon = Icons.Outlined.Biotech,
        iconBackground = Emerald50,
        iconTint = Emerald800,
        footerLeft = FooterLeft.Attachment("Guía_Maqueta.pdf"),
        footerRight = "Lun 10 nov",
        footerRightIcon = Icons.Outlined.CalendarToday,
        footerRightColor = OnSurfaceVariant,
        footerRightIconColor = Outline,
        status = TaskStatus.Pending,
        section = "ESTA SEMANA",
    ),
    SubjectTask(
        subject = "Historia",
        title = "Línea del tiempo",
        icon = Icons.Outlined.HistoryEdu,
        iconBackground = Amber100.copy(alpha = 0.5f),
        iconTint = Amber900,
        footerLeft = FooterLeft.Reviewed("Revisada por docente"),
        footerRight = "Entregada ayer",
        footerRightIcon = Icons.Outlined.History,
        footerRightColor = Outline,
        footerRightIconColor = Outline,
        status = TaskStatus.Delivered,
        section = "ESTA SEMANA",
    ),
)

@Composable
fun ParentTasksScreen(
    onOpenTask: () -> Unit,
    onSelectTab: (ParentTab) -> Unit,
) {
    var filter by remember { mutableStateOf(TaskFilter.Pendientes) }
    val tasks = remember { sampleTasks() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 104.dp),
        ) {
            Reveal(0) { TasksTopBar() }
            Reveal(60) {
                FilterRow(selected = filter, onSelect = { filter = it })
            }
            Reveal(120) {
                TaskList(filter = filter, tasks = tasks, onOpenTask = onOpenTask)
            }
        }

        ParentBottomBar(
            active = ParentTab.Tareas,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TasksTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Tareas",
            color = Primary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.5).sp,
        )
        StudentPill(name = "Mariana", grade = "3° B")
    }
}

@Composable
private fun FilterRow(
    selected: TaskFilter,
    onSelect: (TaskFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TaskFilter.entries.forEach { filter ->
            FilterChip(
                label = filter.label,
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    val background by animateColorAsState(
        targetValue = if (selected) PrimaryContainer else SurfaceContainerLowest,
        animationSpec = tween(200),
        label = "filterChipBackground",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) SurfaceContainerLowest else PrimaryContainer,
        animationSpec = tween(200),
        label = "filterChipForeground",
    )

    Box(
        modifier = pressModifier
            .height(32.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, OutlineVariant.copy(alpha = 0.5f), CircleShape)
                },
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foreground,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun TaskList(
    filter: TaskFilter,
    tasks: List<SubjectTask>,
    onOpenTask: () -> Unit,
) {
    val visible = when (filter) {
        TaskFilter.Pendientes -> tasks.filter { it.status == TaskStatus.Pending }
        TaskFilter.Entregadas -> tasks.filter { it.status == TaskStatus.Delivered }
        TaskFilter.Vencidas -> emptyList()
        TaskFilter.Todas -> tasks
    }

    if (visible.isEmpty()) {
        EmptyTasks()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        listOf("PARA MAÑANA", "ESTA SEMANA").forEach { section ->
            val sectionTasks = visible.filter { it.section == section }
            if (sectionTasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        label = section,
                        count = sectionTasks.size,
                    )
                    sectionTasks.forEach { task ->
                        TaskCard(task = task, onClick = onOpenTask)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Outline,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
        )
        Text(
            text = if (count == 1) "1 tarea" else "$count tareas",
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun TaskCard(task: SubjectTask, onClick: () -> Unit) {
    val (interaction, pressModifier) = rememberPressScale(0.99f)
    Column(
        modifier = pressModifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(task.iconBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = task.icon,
                        contentDescription = null,
                        tint = task.iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = task.subject,
                        color = SecondaryContainer,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.4.sp,
                        lineHeight = 12.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = task.title,
                        color = OnSurface,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(status = task.status)
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(thickness = 1.dp, color = OutlineVariant.copy(alpha = 0.2f))
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterLeftContent(footer = task.footerLeft, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = task.footerRightIcon,
                    contentDescription = null,
                    tint = task.footerRightIconColor,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = task.footerRight,
                    color = task.footerRightColor,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun FooterLeftContent(footer: FooterLeft, modifier: Modifier = Modifier) {
    when (footer) {
        is FooterLeft.Teacher -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.School,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = footer.name,
                color = Outline,
                fontFamily = DmSans,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        is FooterLeft.Plain -> Text(
            text = footer.text,
            color = Outline,
            fontFamily = DmSans,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )

        is FooterLeft.Attachment -> Row(
            modifier = modifier
                .clip(CircleShape)
                .background(PrimaryFixed.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.AttachFile,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = footer.name,
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        is FooterLeft.Reviewed -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.TaskAlt,
                contentDescription = null,
                tint = Emerald700,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = footer.text,
                color = Emerald700,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusChip(status: TaskStatus) {
    if (status == TaskStatus.Pending) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Amber50)
                .border(1.dp, Amber200.copy(alpha = 0.6f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = "Pendiente",
                color = Amber700,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            )
        }
    } else {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Emerald50)
                .border(1.dp, Emerald200.copy(alpha = 0.6f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Emerald800,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = "Entregada",
                color = Emerald800,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun EmptyTasks() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SurfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Sin tareas en este filtro",
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Cuando haya novedades aparecerán aquí.",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
    }
}
