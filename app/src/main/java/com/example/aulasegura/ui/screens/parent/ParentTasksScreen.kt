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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ParentBottomBar
import com.example.aulasegura.ui.components.ParentTab
import com.example.aulasegura.ui.components.StudentPill
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
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
private val Amber200 = Color(0xFFFDE68A)
private val Amber700 = Color(0xFFB45309)
private val CoralTint = Color(0xFFFFEBE8)

/**
 * The tasks the school published for the child on screen.
 *
 * Nothing here depends on a task being handed in: `tasks/{id}` has no per-student
 * state, so the only two states a card can paint are "Pendiente" and "Vencida".
 */
@Composable
fun ParentTasksScreen(
    ui: ParentTasksUiState,
    onOpenTask: (String) -> Unit,
    onSelectFilter: (ParentTaskFilter) -> Unit,
    onCycleChild: () -> Unit,
    onRetry: () -> Unit,
    onSelectTab: (ParentTab) -> Unit,
) {
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
            when (ui) {
                is ParentTasksUiState.Loaded -> {
                    Reveal(0) {
                        TasksTopBar(
                            childName = ui.childName,
                            groupLabel = ui.groupLabel,
                            childCount = ui.childCount,
                            onCycleChild = onCycleChild,
                        )
                    }
                    Reveal(60) {
                        FilterRow(selected = ui.filter, onSelect = onSelectFilter)
                    }
                    Reveal(120) {
                        TaskList(ui = ui, onOpenTask = onOpenTask)
                    }
                }

                else -> {
                    Reveal(0) { TasksTopBar() }
                    Reveal(60) { TasksPlaceholder(ui = ui, onRetry = onRetry) }
                }
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
private fun TasksTopBar(
    childName: String? = null,
    groupLabel: String = "",
    childCount: Int = 0,
    onCycleChild: () -> Unit = {},
) {
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
        if (childName != null) {
            StudentPill(
                name = childName,
                grade = groupLabel,
                onClick = if (childCount > 1) onCycleChild else ({}),
            )
        }
    }
}

@Composable
private fun FilterRow(
    selected: ParentTaskFilter,
    onSelect: (ParentTaskFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ParentTaskFilter.entries.forEach { filter ->
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
    ui: ParentTasksUiState.Loaded,
    onOpenTask: (String) -> Unit,
) {
    if (ui.sections.isEmpty()) {
        EmptyTasks(message = ui.emptyMessage)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ui.sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(label = section.label, count = section.tasks.size)
                section.tasks.forEach { task ->
                    TaskCard(
                        task = task,
                        onClick = { onOpenTask(task.taskId) },
                    )
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
private fun TaskCard(task: ParentTaskRowUi, onClick: () -> Unit) {
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
                        .background(if (task.overdue) CoralTint else SecondaryFixed),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = if (task.overdue) BrandCoral else Secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    if (task.subject.isNotBlank()) {
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
                    }
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
            StatusChip(overdue = task.overdue)
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(thickness = 1.dp, color = OutlineVariant.copy(alpha = 0.2f))
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterLeft(
                attachmentName = task.attachmentName,
                teacher = task.teacher,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        task.overdue -> Icons.Outlined.History
                        task.dueSoon -> Icons.Outlined.Alarm
                        else -> Icons.Outlined.CalendarToday
                    },
                    contentDescription = null,
                    tint = when {
                        task.overdue -> BrandCoral
                        task.dueSoon -> Amber700
                        else -> Outline
                    },
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = task.dueLabel,
                    color = when {
                        task.overdue -> BrandCoral
                        task.dueSoon -> Amber700
                        else -> OnSurfaceVariant
                    },
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/**
 * The attached PDF wins over the author name: it is the one detail the tutor
 * needs before opening the task, and the detail screen already names the teacher.
 */
@Composable
private fun FooterLeft(
    attachmentName: String?,
    teacher: String,
    modifier: Modifier = Modifier,
) {
    if (attachmentName != null) {
        Row(
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
                text = attachmentName,
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        return
    }

    if (teacher.isBlank()) return

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.School,
            contentDescription = null,
            tint = Outline,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = teacher,
            color = Outline,
            fontFamily = DmSans,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusChip(overdue: Boolean) {
    if (!overdue) {
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
        return
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(CoralTint)
            .border(1.dp, BrandCoral.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            tint = BrandCoral,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = "Vencida",
            color = BrandCoral,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun EmptyTasks(message: String) {
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
            text = message,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
    }
}

/** Loading, no linked child, or a failed read. */
@Composable
private fun TasksPlaceholder(ui: ParentTasksUiState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        when (ui) {
            ParentTasksUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PrimaryContainer)
            }

            ParentTasksUiState.NoChildren -> {
                Text(
                    text = "Aún no hay hijos vinculados",
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Pide a la escuela el código del alumno y regístrate de nuevo " +
                        "para ver sus tareas.",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }

            is ParentTasksUiState.Failed -> {
                Text(
                    text = "No se pudieron cargar",
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = ui.message,
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(14.dp))
                RetryButton(onRetry = onRetry)
            }

            is ParentTasksUiState.Loaded -> Unit
        }
    }
}

@Composable
private fun RetryButton(onRetry: () -> Unit) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    Box(
        modifier = pressModifier
            .clip(CircleShape)
            .background(PrimaryContainer)
            .clickable(interactionSource = interaction, indication = null, onClick = onRetry)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Reintentar",
            color = SurfaceContainerLowest,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}
