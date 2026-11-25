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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.aulasegura.domain.repository.unassignedSubjectsForGrade
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.domain.model.AdminGroup
import com.example.aulasegura.domain.model.AdminScheduleSlot
import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.model.AdminUser
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

/** 4.11 — Detalle de grupo (Administración). Real group, teachers and schedule. */

private val Hairline = Color(0xFFE6E1D8)
private val RowDivider = Color(0xFFF0EBE1)
private val Muted = Color(0xFF6B7280)
private val Ink = Color(0xFF111827)
private val TrackShape = RoundedCornerShape(percent = 50)

private enum class GroupTab(val label: String) {
    Alumnos("Alumnos"),
    Docentes("Docentes"),
    Horario("Horario"),
}

/** The two dialogs the header menu can open. */
private enum class GroupMenuDialog { Edit, Delete }

@Composable
fun AdminGroupDetailScreen(
    groupId: String = "",
    groupLabel: String = "3° B",
    onBack: () -> Unit = {},
    onStudentClick: () -> Unit = {},
) {
    val viewModel: AdminGroupDetailViewModel = viewModel(
        factory = rememberAppContainer().viewModelFactory,
    )
    val ui by viewModel.state.collectAsState()

    var tab by rememberSaveable { mutableStateOf(GroupTab.Docentes) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var menuDialog by remember { mutableStateOf<GroupMenuDialog?>(null) }
    var editForm by remember { mutableStateOf(AdminGroupForm()) }
    var destinationChoice by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(groupId) {
        if (groupId.isNotBlank()) viewModel.load(groupId)
    }

    // A successful deletion leaves no group to display; return to the list.
    LaunchedEffect(ui.deleted) {
        if (ui.deleted) onBack()
    }

    val group = ui.group
    val label = group?.name?.ifBlank { groupLabel } ?: groupLabel
    val currentGroupId: String = group?.id?.takeIf { it.isNotBlank() } ?: groupId

    val dismissMenu: () -> Unit = { menuExpanded = false }
    val openEdit: () -> Unit = {
        editForm = AdminGroupForm(
            name = group?.name.orEmpty(),
            grade = group?.grade?.toString().orEmpty(),
            section = group?.section.orEmpty(),
        )
        menuExpanded = false
        viewModel.errorShown()
        menuDialog = GroupMenuDialog.Edit
    }
    val openDelete: () -> Unit = {
        menuExpanded = false
        destinationChoice = null
        viewModel.errorShown()
        menuDialog = GroupMenuDialog.Delete
    }
    val teacherRows = teacherRows(
        users = ui.users,
        assignedIds = group?.teacherIds.orEmpty(),
        query = query,
    )
    val teacherOptions = teacherPickerOptions(ui.users, group?.teacherIds.orEmpty().toSet())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                ui.loading -> {
                    Reveal(0) {
                        GroupHeader(
                            groupLabel = label,
                            onBack = onBack,
                            onMoreClick = { menuExpanded = true },
                            menuExpanded = menuExpanded,
                            onMenuDismiss = dismissMenu,
                            onEditGroup = openEdit,
                            onDeleteGroup = openDelete,
                        )
                    }
                    Text(
                        text = "Cargando grupo…",
                        color = Muted,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                group == null -> {
                    Reveal(0) {
                        GroupHeader(
                            groupLabel = label,
                            onBack = onBack,
                            onMoreClick = { menuExpanded = true },
                            menuExpanded = menuExpanded,
                            onMenuDismiss = dismissMenu,
                            onEditGroup = openEdit,
                            onDeleteGroup = openDelete,
                        )
                    }
                    Text(
                        text = ui.error ?: "No se encontró el grupo.",
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                    TextButton(onClick = { if (groupId.isNotBlank()) viewModel.load(groupId) }) {
                        Text(text = "Reintentar", fontFamily = DmSans)
                    }
                }

                else -> {
                    Reveal(0) {
                        GroupHeader(
                            groupLabel = label,
                            onBack = onBack,
                            onMoreClick = { menuExpanded = true },
                            menuExpanded = menuExpanded,
                            onMenuDismiss = dismissMenu,
                            onEditGroup = openEdit,
                            onDeleteGroup = openDelete,
                        )
                    }

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
                            GroupStat(
                                value = group.studentCount.toString(),
                                label = "Alumnos",
                                valueColor = PrimaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            GroupStat(
                                value = group.teacherIds.size.toString(),
                                label = "Docentes",
                                valueColor = Color(0xFF059669),
                                modifier = Modifier.weight(1f),
                            )
                            GroupStat(
                                value = group.schedule.size.toString(),
                                label = "Bloques",
                                valueColor = SecondaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        GroupTabs(selected = tab, onSelect = { tab = it })

                        Spacer(modifier = Modifier.height(12.dp))

                        ui.message?.let { message ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEAF6F0))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = message, color = Color(0xFF00513F), fontFamily = DmSans, fontSize = 13.sp)
                                TextButton(onClick = { viewModel.messageShown() }) {
                                    Text(text = "Aceptar", color = Color(0xFF00513F), fontFamily = DmSans)
                                }
                            }
                        }

                        ui.error?.let { error ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontFamily = DmSans,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { viewModel.errorShown() }) {
                                    Text(text = "Cerrar", fontFamily = DmSans)
                                }
                            }
                        }

                        when (tab) {
                            GroupTab.Alumnos -> StudentsTab(
                                students = ui.students,
                                loading = ui.studentsLoading,
                                onRenumber = { viewModel.renumberStudents() },
                            )
                            GroupTab.Docentes -> TeachersTab(
                                query = query,
                                onQueryChange = { query = it; viewModel.setQuery(it) },
                                rows = teacherRows,
                                onToggle = { viewModel.toggleTeacher(it) },
                            )
                            GroupTab.Horario -> ScheduleTab(
                                slots = ui.slots,
                                teacherOptions = teacherOptions,
                                users = ui.users,
                                onAdd = { viewModel.addSlot() },
                                onRemove = { viewModel.removeSlot(it) },
                                onChange = { index, slot -> viewModel.setSlot(index, slot) },
                                onSave = { viewModel.saveSchedule() },
                            )
                        }
                    }
                }
            }
        }

        ui.pendingSubject?.let { pending ->
            val freeSubjects: List<com.example.aulasegura.domain.model.Subject> =
                ui.group?.let { group ->
                    unassignedSubjectsForGrade(group.grade, group.subjectTeachers)
                } ?: emptyList()
            SubjectAssignmentDialog(
                teacherName = pending.teacherName,
                groupName = ui.group?.name.orEmpty(),
                subjects = freeSubjects,
                onChoose = { viewModel.assignTeacher(pending.teacherUid, it) },
                onDismiss = viewModel::cancelPendingSubject,
            )
        }

        when (menuDialog) {
            GroupMenuDialog.Edit -> EditGroupDialog(
                form = editForm,
                validationError = validationError(editForm),
                serverError = ui.error,
                saving = ui.saving,
                onFormChange = { editForm = it },
                onDismiss = {
                    menuDialog = null
                    viewModel.errorShown()
                },
                onSave = {
                    parseGrade(editForm.grade)?.let { grade ->
                        viewModel.updateGroup(currentGroupId, editForm.name, grade, editForm.section)
                        menuDialog = null
                    }
                },
            )

            GroupMenuDialog.Delete -> DeleteGroupDialog(
                needsDestination = needsDestinationGroup(group?.studentCount ?: 0),
                destinationOptions = ui.destinationOptions,
                selectedDestination = destinationChoice,
                onDestinationChange = { destinationChoice = it },
                serverError = ui.error,
                saving = ui.saving,
                onDismiss = {
                    menuDialog = null
                    viewModel.errorShown()
                },
                onConfirm = {
                    viewModel.deleteGroup(currentGroupId, destinationChoice)
                    menuDialog = null
                },
            )

            null -> Unit
        }
    }
}

@Composable
private fun EditGroupDialog(
    form: AdminGroupForm,
    validationError: String?,
    serverError: String?,
    saving: Boolean,
    onFormChange: (AdminGroupForm) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar grupo",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onFormChange(form.copy(name = it)) },
                    label = { Text("Nombre (ej. 3° B)", fontFamily = DmSans) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.grade,
                    onValueChange = { onFormChange(form.copy(grade = it)) },
                    label = { Text("Grado (1 a 6)", fontFamily = DmSans) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.section,
                    onValueChange = { onFormChange(form.copy(section = it)) },
                    label = { Text("Sección (A, B, C…)", fontFamily = DmSans) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                validationError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                    )
                }
                serverError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = validationError == null && !saving,
            ) {
                Text(
                    text = if (saving) "Guardando…" else "Guardar",
                    fontFamily = DmSans,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", fontFamily = DmSans)
            }
        },
    )
}

@Composable
private fun DeleteGroupDialog(
    needsDestination: Boolean,
    destinationOptions: List<AdminGroup>,
    selectedDestination: String?,
    onDestinationChange: (String) -> Unit,
    serverError: String?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Eliminar grupo",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (needsDestination) {
                    Text(
                        text = "Este grupo tiene alumnos. Se moverán al grupo que elijas; " +
                            "no se darán de baja ni se eliminarán. También se borrará su horario.",
                        color = Ink,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    Picker(
                        label = "Selecciona el grupo de destino",
                        options = destinationOptions.map { it.id to it.name },
                        selectedValue = selectedDestination.orEmpty(),
                        onSelect = onDestinationChange,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (destinationOptions.isEmpty()) {
                        Text(
                            text = "No hay otros grupos en la escuela. Crea uno antes de eliminar este.",
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = DmSans,
                            fontSize = 12.sp,
                        )
                    }
                } else {
                    Text(
                        text = "¿Eliminar este grupo? También se borrará su horario. " +
                            "Los alumnos no se eliminan.",
                        color = Ink,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
                serverError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                    )
                }
            }
        },
        // An explicit destination is required: the confirm button stays disabled
        // until the principal picks a group whenever there are students to move.
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !saving && (!needsDestination || selectedDestination != null),
            ) {
                Text(
                    text = if (saving) "Eliminando…" else "Eliminar",
                    fontFamily = DmSans,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", fontFamily = DmSans)
            }
        },
    )
}

@Composable
private fun GroupHeader(
    groupLabel: String,
    onBack: () -> Unit,
    onMoreClick: () -> Unit,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
) {
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
                text = "Detalle de grupo",
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
            Box {
                CircleHeaderButton(
                    onClick = onMoreClick,
                    icon = Icons.Filled.MoreVert,
                    description = "Opciones",
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = onMenuDismiss,
                ) {
                    DropdownMenuItem(
                        text = { Text(text = "Editar grupo", fontFamily = DmSans, fontSize = 14.sp) },
                        onClick = {
                            onMenuDismiss()
                            onEditGroup()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "Eliminar grupo", fontFamily = DmSans, fontSize = 14.sp) },
                        onClick = {
                            onMenuDismiss()
                            onDeleteGroup()
                        },
                    )
                }
            }
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
    icon: ImageVector,
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

/** Real students of this group, read from the school-wide student list. */
@Composable
private fun StudentsTab(
    students: List<AdminStudent>,
    loading: Boolean,
    onRenumber: () -> Unit,
) {
    val ordered = remember(students) { students.sortedBy { it.listNumber } }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Button(onClick = onRenumber) {
            Icon(
                imageVector = Icons.Filled.FormatListNumbered,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Asignar N° de lista", fontFamily = DmSans, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            loading -> Text(
                text = "Cargando alumnos…",
                color = Muted,
                fontFamily = DmSans,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            ordered.isEmpty() -> Text(
                text = "Este grupo todavía no tiene alumnos.",
                color = Muted,
                fontFamily = DmSans,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, Hairline, RoundedCornerShape(24.dp)),
            ) {
                ordered.forEachIndexed { index, student ->
                    if (index > 0) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RowDivider))
                    }
                    StudentRow(student = student)
                }
            }
        }
    }
}

@Composable
private fun StudentRow(student: AdminStudent) {
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
                .background(Color(0xFFEEF0FB))
                .border(1.dp, Color(0xFFDCDFF7), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials(student.displayName),
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.displayName,
                color = Ink,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "N° ${student.listNumber}",
                color = Muted,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        StudentStatusBadge(active = student.active)
    }
}

@Composable
private fun StudentStatusBadge(active: Boolean) {
    val background = if (active) Color(0xFFEAF6F0) else Color(0xFFFFF7E6)
    val foreground = if (active) Color(0xFF047857) else Color(0xFFB45309)
    val borderColor = if (active) Color(0xFFA7F3D0) else Color(0xFFFCD34D)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(percent = 50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(foreground),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (active) "Activo" else "Inactivo",
            color = foreground,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun TeachersTab(
    query: String,
    onQueryChange: (String) -> Unit,
    rows: List<AdminTeacherRowUi>,
    onToggle: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Ink,
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLowest)
                .border(1.dp, Hairline, RoundedCornerShape(24.dp)),
        ) {
            if (rows.isEmpty()) {
                Text(
                    text = if (query.isBlank()) "No hay docentes registrados en la escuela."
                    else "Sin resultados para \"$query\"",
                    color = Muted,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            } else {
                rows.forEachIndexed { index, teacher ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(1.dp).background(RowDivider),
                        )
                    }
                    TeacherRow(teacher = teacher, onToggle = { onToggle(teacher.uid) })
                }
            }
        }
    }
}

@Composable
private fun TeacherRow(teacher: AdminTeacherRowUi, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEF0FB))
                    .border(1.dp, Color(0xFFDCDFF7), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials(teacher.name),
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = teacher.name,
                    color = Ink,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = teacher.subtitle,
                    color = Muted,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onToggle) {
            Text(
                text = if (teacher.assigned) "Desasignar" else "Asignar",
                fontFamily = DmSans,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ScheduleTab(
    slots: List<AdminScheduleSlot>,
    teacherOptions: List<Pair<String, String>>,
    users: List<AdminUser>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onChange: (Int, AdminScheduleSlot) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        if (teacherOptions.isEmpty()) {
            Text(
                text = "Asigná docentes en la pestaña Docentes para usarlos en el horario.",
                color = Muted,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        if (slots.isEmpty()) {
            Text(
                text = "Todavía no hay bloques en el horario.",
                color = Muted,
                fontFamily = DmSans,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            slots.forEachIndexed { index, slot ->
                SlotEditor(
                    slot = slot,
                    teacherOptions = teacherOptions,
                    users = users,
                    onChange = { onChange(index, it) },
                    onRemove = { onRemove(index) },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onAdd) {
                Text(text = "Agregar bloque", fontFamily = DmSans, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onSave) {
                Text(text = "Guardar horario", fontFamily = DmSans, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Cada bloque se identifica por día y hora de inicio; guardar de nuevo no duplica los existentes.",
            color = Muted,
            fontFamily = DmSans,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun SlotEditor(
    slot: AdminScheduleSlot,
    teacherOptions: List<Pair<String, String>>,
    users: List<AdminUser>,
    onChange: (AdminScheduleSlot) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Picker(
                label = weekdayLabel(slot.weekday),
                options = (1..7).map { it.toString() to weekdayLabel(it) },
                selectedValue = slot.weekday.toString(),
                onSelect = { onChange(slot.copy(weekday = it.toIntOrNull() ?: 1)) },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Quitar bloque",
                tint = Color(0xFFDC2626),
                modifier = Modifier.clickable(onClick = onRemove).size(20.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = slot.startTime,
                onValueChange = { onChange(slot.copy(startTime = it)) },
                label = { Text("Inicio", fontFamily = DmSans) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = slot.endTime,
                onValueChange = { onChange(slot.copy(endTime = it)) },
                label = { Text("Fin", fontFamily = DmSans) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        val subjectOptions = subjectOptionsForTeacher(slot.teacherId, users)
        if (subjectOptions.isEmpty()) {
            // A teacher without a catalogue must not block the principal: keep a
            // free-text field so the slot can still be saved.
            OutlinedTextField(
                value = slot.subject,
                onValueChange = { onChange(slot.copy(subject = it)) },
                label = { Text("Materia", fontFamily = DmSans) },
                placeholder = { Text("Este docente no tiene materias asignadas.", fontFamily = DmSans) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Picker(
                label = "Materia",
                options = subjectOptions.map { it to it },
                selectedValue = slot.subject,
                onSelect = { onChange(slot.copy(subject = it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Picker(
            label = "Docente del bloque",
            options = slotTeacherPickerOptions(slot.teacherId, teacherOptions, users),
            selectedValue = slot.teacherId,
            onSelect = { onChange(slot.copy(teacherId = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Small dropdown kept local so editing a slot never touches repository state. */
@Composable
private fun Picker(
    label: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedValue }?.second ?: label

    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = selectedLabel,
                fontFamily = DmSans,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text = text, fontFamily = DmSans, fontSize = 13.sp) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun initials(name: String): String = name.split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
