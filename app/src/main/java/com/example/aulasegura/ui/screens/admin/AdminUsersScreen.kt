package com.example.aulasegura.ui.screens.admin

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.repository.AccountRole
import com.example.aulasegura.ui.components.AdminBottomBar
import com.example.aulasegura.ui.components.AdminTab
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSecondaryFixedVariant
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.SurfaceVariant

/** 4.2 — Usuarios (Administración). Ported 1:1 from the Stitch markup of the users list. */

private val Hairline = Color(0xFFE6E1D8)
private val ActivePillBg = Color(0xFFEAF6F0)
private val ActivePillText = Color(0xFF10B981)
private val PendingPillBg = Color(0xFFFEF3C7)
private val PendingPillText = Color(0xFFD97706)
private val HintBg = BrandMint.copy(alpha = 0.15f)
private val HintBorder = BrandMint.copy(alpha = 0.40f)
private val HintIcon = Color(0xFF00513F)
private val HintText = Color(0xFF004031)

private val ListShape = RoundedCornerShape(24.dp)
private val InputShape = RoundedCornerShape(16.dp)

private enum class AvatarStyle {
    Indigo,
    DeepIndigo,
    Neutral,
    Coral,
}

/** Display-only row; the domain row is mapped to this in [AdminUserRowUi.toDisplayRow]. */
private data class UserRow(
    val uid: String,
    val initials: String,
    val name: String,
    val subtitle: String,
    val active: Boolean,
    val avatar: AvatarStyle,
)

@Composable
fun AdminUsersScreen(
    onPanel: () -> Unit = {},
    onGrupos: () -> Unit = {},
    onReportes: () -> Unit = {},
    onMas: () -> Unit = {},
    onAddStudent: () -> Unit = {},
) {
    val viewModel: AdminUsersViewModel = viewModel(
        factory = rememberAppContainer().viewModelFactory,
    )
    val uiState by viewModel.state.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(AdminUsersTab.DOCENTES) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<AdminUserForm?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var createForm by remember { mutableStateOf(CreateAccountForm()) }

    // The tab is the only source of the new account's role. Alumnos never opens
    // this dialog, so a directivo can never be created from the app.
    val createRole = accountRoleFromTab(selectedTab)

    // The form is derived from the loaded users, so a tap never needs a second read.
    val forms = uiState.users.associate { it.uid to formFrom(it) }
    // Real group names first; students with a group outside the loaded map still
    // contribute their id so the filter never hides a row.
    val studentGroups = (uiState.groupNames.keys + uiState.students.map { it.groupId })
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
    val rows = if (selectedTab == AdminUsersTab.ALUMNOS) {
        filterStudents(uiState.students, selectedGroup, query, uiState.groupNames)
            .map { it.toDisplayRow() }
    } else {
        filterUsers(uiState.users, selectedTab, query).map { it.toDisplayRow() }
    }
    val selectedStudent = uiState.students.firstOrNull { it.id == uiState.selectedStudentId }
    val selectedStudentGroup = selectedStudent?.let {
        uiState.groupNames[it.groupId]?.takeIf { name -> name.isNotBlank() }
            ?: groupLabel(it.groupId)
    }.orEmpty()
    // Students load from their own collection: a failure there must not replace
    // the Docentes/Padres loading and error states.
    val tabLoading = if (selectedTab == AdminUsersTab.ALUMNOS) {
        uiState.studentsLoading
    } else {
        uiState.loading
    }
    val tabError = if (selectedTab == AdminUsersTab.ALUMNOS) {
        uiState.studentsError
    } else {
        uiState.error
    }
    val counts = mapOf(
        AdminUsersTab.DOCENTES to filterUsers(uiState.users, AdminUsersTab.DOCENTES, "").size,
        AdminUsersTab.PADRES to filterUsers(uiState.users, AdminUsersTab.PADRES, "").size,
        AdminUsersTab.ALUMNOS to uiState.students.size,
    )

    // Close the dialog once the save has been acknowledged by the repository.
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) editing = null
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
                .padding(bottom = 104.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(0)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Usuarios",
                    color = Primary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    letterSpacing = (-0.8).sp,
                )
            }

            Reveal(60) {
                UsersTabs(
                    selected = selectedTab,
                    counts = counts,
                    onSelect = {
                        if (it != selectedTab) {
                            selectedTab = it
                            query = ""
                            if (it != AdminUsersTab.ALUMNOS) selectedGroup = null
                        }
                    },
                )
            }

            Reveal(120) {
                SearchRow(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = selectedTab.searchHint,
                    showGroupFilter = selectedTab == AdminUsersTab.ALUMNOS,
                    groupFilter = selectedGroup,
                    groupOptions = studentGroups,
                    groupNames = uiState.groupNames,
                    onGroupFilterChange = { selectedGroup = it },
                )
            }

            uiState.message?.let { message ->
                Reveal(160) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ActivePillBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = message,
                            color = HintIcon,
                            fontFamily = DmSans,
                            fontSize = 13.sp,
                        )
                        TextButton(onClick = { viewModel.messageShown() }) {
                            Text(text = "Aceptar", color = HintIcon, fontFamily = DmSans)
                        }
                    }
                }
            }

            Reveal(180) {
                when {
                    tabLoading -> Text(
                        text = if (selectedTab == AdminUsersTab.ALUMNOS) {
                            "Cargando alumnos…"
                        } else {
                            "Cargando usuarios…"
                        },
                        color = OnSurfaceVariant,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    )

                    tabError != null -> Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = tabError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = DmSans,
                            fontSize = 14.sp,
                        )
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(text = "Reintentar", fontFamily = DmSans)
                        }
                    }

                    rows.isEmpty() -> Text(
                        text = adminUsersEmptyMessage(
                            query = query,
                            studentsTab = selectedTab == AdminUsersTab.ALUMNOS,
                            groupFilter = selectedGroup,
                        ),
                        color = OnSurfaceVariant,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    )

                    else -> Column(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                            .fillMaxWidth()
                            .clip(ListShape)
                            .background(SurfaceContainerLowest)
                            .border(1.dp, OutlineVariant.copy(alpha = 0.4f), ListShape),
                    ) {
                        rows.forEachIndexed { index, row ->
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(OutlineVariant.copy(alpha = 0.3f)),
                                )
                            }
                            UserRowItem(
                                row = row,
                                onClick = {
                                    if (selectedTab == AdminUsersTab.ALUMNOS) {
                                        // Students have no editable profile: the row
                                        // opens their detail and baja dialog instead.
                                        viewModel.selectStudent(row.uid)
                                    } else {
                                        forms[row.uid]?.let { editing = it }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Reveal(240) { HintCard(text = selectedTab.hint) }
        }

        AdminBottomBar(
            active = AdminTab.Usuarios,
            onSelect = { tab ->
                when (tab) {
                    AdminTab.Panel -> onPanel()
                    AdminTab.Usuarios -> Unit
                    AdminTab.Grupos -> onGrupos()
                    AdminTab.Reportes -> onReportes()
                    AdminTab.Mas -> onMas()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    editing?.let { form ->
        EditUserDialog(
            form = form,
            isTutor = selectedTab == AdminUsersTab.PADRES,
            assignedGroups = assignedGroupNames(form.groupIds, uiState.groupNames),
            assignedStudents = assignedStudentNames(form.studentIds, uiState.students),
            error = uiState.error,
            saving = uiState.loading,
            onFormChange = { editing = it },
            onDismiss = { editing = null },
            onSave = { viewModel.save(form) },
        )
    }

    selectedStudent?.let { student ->
        StudentDetailDialog(
            student = student,
            groupName = selectedStudentGroup,
            loading = uiState.detailLoading,
            saving = uiState.detailSaving,
            error = uiState.detailError,
            onDeactivate = { viewModel.deactivateStudent() },
            onReactivate = { viewModel.reactivateStudent() },
            onDismiss = { viewModel.dismissStudent() },
        )
    }

    createRole?.let { role ->
        if (showCreate) {
            CreateUserDialog(
                form = createForm,
                role = role,
                validationError = createAccountValidationError(createForm),
                serverError = uiState.createError,
                success = uiState.createSuccess,
                saving = uiState.createLoading,
                onFormChange = { createForm = it },
                onDismiss = {
                    showCreate = false
                    viewModel.createMessageShown()
                },
                onCreate = { viewModel.createAccount(createForm, role) },
            )
        }
    }
}

/** Real in-app account creation. The role is fixed by the tab, never by a dropdown. */
@Composable
private fun CreateUserDialog(
    form: CreateAccountForm,
    role: AccountRole,
    validationError: String?,
    serverError: String?,
    success: String?,
    saving: Boolean,
    onFormChange: (CreateAccountForm) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (role == AccountRole.DOCENTE) "Nuevo docente" else "Nuevo tutor",
                color = Primary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormField("Correo electrónico", form.email) { onFormChange(form.copy(email = it)) }
                FormField("Contraseña temporal", form.password) {
                    onFormChange(form.copy(password = it))
                }
                FormField("Nombre", form.firstName) { onFormChange(form.copy(firstName = it)) }
                FormField("Apellido", form.lastName) { onFormChange(form.copy(lastName = it)) }
                FormField("Teléfono (opcional)", form.phone) { onFormChange(form.copy(phone = it)) }
                if (role == AccountRole.DOCENTE) {
                    FormField("Materias (separadas por coma)", form.subjects) {
                        onFormChange(form.copy(subjects = it))
                    }
                }
                FormField("Grupos, IDs separados por coma (opcional)", form.groupIds) {
                    onFormChange(form.copy(groupIds = it))
                }
                Text(
                    text = "La contraseña es temporal. Compártela con la persona indicada " +
                        "por un canal privado y pídele cambiarla al iniciar sesión.",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                )
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontFamily = DmSans, fontSize = 12.sp)
                }
                serverError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontFamily = DmSans, fontSize = 12.sp)
                }
                success?.let {
                    Text(it, color = HintIcon, fontFamily = DmSans, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = if (success != null) onDismiss else onCreate,
                enabled = success != null || (validationError == null && !saving),
            ) {
                Text(
                    text = when {
                        success != null -> "Cerrar"
                        saving -> "Creando…"
                        else -> "Crear cuenta"
                    },
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
private fun UsersTabs(
    selected: AdminUsersTab,
    counts: Map<AdminUsersTab, Int>,
    onSelect: (AdminUsersTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(SurfaceContainer)
            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(percent = 50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AdminUsersTab.entries.forEach { tab ->
            val active = tab == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (active) PrimaryContainer else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tab.label,
                    color = if (active) OnPrimary else OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (active) Color.White.copy(alpha = 0.20f) else SurfaceVariant,
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = (counts[tab] ?: 0).toString(),
                        color = if (active) OnPrimary else OnSurfaceVariant,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    showGroupFilter: Boolean = false,
    groupFilter: String? = null,
    groupOptions: List<String> = emptyList(),
    groupNames: Map<String, String> = emptyMap(),
    onGroupFilterChange: (String?) -> Unit = {},
) {
    var filterExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(InputShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.5f), InputShape)
                .padding(start = 14.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Outline,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = OnSurface,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                    ),
                    cursorBrush = SolidColor(Primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(InputShape)
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.5f), InputShape)
                    .clickable(enabled = showGroupFilter) { filterExpanded = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Filtrar por grupo",
                    tint = Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = filterExpanded,
                onDismissRequest = { filterExpanded = false },
                modifier = Modifier.background(SurfaceContainerLowest),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Todos los grupos",
                            color = OnSurface,
                            fontFamily = DmSans,
                            fontWeight = if (groupFilter == null) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp,
                        )
                    },
                    onClick = {
                        onGroupFilterChange(null)
                        filterExpanded = false
                    },
                )
                groupOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = groupLabel(option, groupNames),
                                color = OnSurface,
                                fontFamily = DmSans,
                                fontWeight = if (option == groupFilter) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                            )
                        },
                        onClick = {
                            onGroupFilterChange(option)
                            filterExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserRowItem(row: UserRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(row.avatar.background()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.initials,
                    color = row.avatar.content(),
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.name,
                    color = OnSurface,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.subtitle,
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (row.active) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(ActivePillBg)
                        .border(
                            1.dp,
                            ActivePillText.copy(alpha = 0.20f),
                            RoundedCornerShape(percent = 50),
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ActivePillText),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Activo",
                        color = ActivePillText,
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
                        .border(
                            1.dp,
                            Color(0xFFF59E0B).copy(alpha = 0.30f),
                            RoundedCornerShape(percent = 50),
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = PendingPillText,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pendiente",
                        color = PendingPillText,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Full student record plus the logical baja/re-activation action. */
@Composable
private fun StudentDetailDialog(
    student: AdminStudent,
    groupName: String,
    loading: Boolean,
    saving: Boolean,
    error: String?,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Detalle del alumno",
                color = Primary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DetailLine("Nombre", student.displayName)
                DetailLine("CURP", student.curp.ifBlank { "sin CURP" })
                DetailLine("Número de lista", student.listNumber.toString())
                DetailLine("Grupo", groupName.ifBlank { "sin grupo" })
                DetailLine("Estado", if (student.active) "Activo" else "Inactivo")
                DetailLine("Tutores vinculados", student.tutorIds.size.toString())
                error?.let {
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
            // One action, two directions: the same slot undoes the logical baja.
            TextButton(
                onClick = if (student.active) onDeactivate else onReactivate,
                enabled = !saving && !loading,
            ) {
                Text(
                    text = studentStatusAction(active = student.active, saving = saving),
                    fontFamily = DmSans,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cerrar", fontFamily = DmSans)
            }
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String, mono: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
        Text(
            text = value,
            color = OnSurface,
            fontFamily = if (mono) JetBrainsMono else DmSans,
            fontWeight = if (mono) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun EditUserDialog(
    form: AdminUserForm,
    isTutor: Boolean,
    assignedGroups: String,
    assignedStudents: String,
    error: String?,
    saving: Boolean,
    onFormChange: (AdminUserForm) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar usuario",
                color = Primary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormField("Nombre", form.firstName) { onFormChange(form.copy(firstName = it)) }
                FormField("Apellido", form.lastName) { onFormChange(form.copy(lastName = it)) }
                FormField("Teléfono", form.phone) { onFormChange(form.copy(phone = it)) }
                if (isTutor) {
                    ReadOnlyInfo("Alumnos asignados", assignedStudents, "Sin alumnos asignados")
                } else {
                    FormField("Materias (separadas por coma)", form.subjects) {
                        onFormChange(form.copy(subjects = it))
                    }
                    ReadOnlyInfo("Grupos asignados", assignedGroups, "Sin grupos asignados")
                }
                Text(
                    text = if (isTutor) {
                        "Los alumnos se vinculan desde la cuenta del tutor."
                    } else {
                        "La asignación de grupos se gestiona desde Grupos."
                    },
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = form.active,
                        onCheckedChange = { onFormChange(form.copy(active = it)) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Cuenta activa", fontFamily = DmSans, fontSize = 14.sp)
                }
                validationError(form)?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontFamily = DmSans, fontSize = 12.sp)
                }
                error?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontFamily = DmSans, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = isValid(form) && !saving) {
                Text(text = if (saving) "Guardando…" else "Guardar", fontFamily = DmSans)
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
private fun FormField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(text = label, fontFamily = DmSans) },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = DmSans,
            fontSize = 14.sp,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Denormalized links are shown for context only; assignment is a Bloque 3 flow. */
@Composable
private fun ReadOnlyInfo(label: String, value: String, emptyText: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
        Text(
            text = value.ifBlank { emptyText },
            color = OnSurface,
            fontFamily = DmSans,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun HintCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HintBg)
            .border(1.dp, HintBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = HintIcon,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = HintText,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

private fun AdminUserRowUi.toDisplayRow(): UserRow = displayRow(uid, name, subtitle, active)

private fun AdminStudentRowUi.toDisplayRow(): UserRow = displayRow(
    uid = id,
    name = name,
    subtitle = "$group · No. de lista $listNumber",
    active = active,
)

/** Shared visual mapping: the avatar is derived from the id, never stored. */
private fun displayRow(uid: String, name: String, subtitle: String, active: Boolean): UserRow {
    val initials = name.split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
    val avatar = when (uid.hashCode() and 3) {
        0 -> AvatarStyle.Indigo
        1 -> AvatarStyle.DeepIndigo
        2 -> AvatarStyle.Neutral
        else -> AvatarStyle.Coral
    }
    return UserRow(uid, initials, name, subtitle, active, avatar)
}

private fun AvatarStyle.background(): Color = when (this) {
    AvatarStyle.Indigo -> PrimaryFixed
    AvatarStyle.DeepIndigo -> PrimaryContainer
    AvatarStyle.Neutral -> SurfaceVariant
    AvatarStyle.Coral -> SecondaryFixed
}

private fun AvatarStyle.content(): Color = when (this) {
    AvatarStyle.Indigo -> Color(0xFF0B0D5B)
    AvatarStyle.DeepIndigo -> OnPrimary
    AvatarStyle.Neutral -> Primary
    AvatarStyle.Coral -> OnSecondaryFixedVariant
}
