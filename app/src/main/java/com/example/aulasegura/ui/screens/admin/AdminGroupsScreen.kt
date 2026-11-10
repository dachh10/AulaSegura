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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.AdminBottomBar
import com.example.aulasegura.ui.components.AdminTab
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.Secondary
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.SurfaceVariant

/** 4.4 — Grupos (Administración). Ported 1:1 from the Stitch markup of the groups list. */

private val CardShape = RoundedCornerShape(24.dp)
private val BadgeShape = RoundedCornerShape(16.dp)
private val CardShadow = Color(0x141B1D3A)

private enum class GradeFilter(val label: String, val grade: String?) {
    Todos("Todos", null),
    Primero("1°", "1"),
    Segundo("2°", "2"),
    Tercero("3°", "3"),
}

private data class AdminGroup(
    val name: String,
    val grade: String,
    val titular: String,
    val students: Int,
    val attendance: Int,
    val teachers: Int,
    val incidents: Int,
)

private val adminGroups = listOf(
    AdminGroup("3° B", "3", "Prof. Ricardo Salinas", 28, 94, 5, 1),
    AdminGroup("3° A", "3", "Profa. Laura Mendoza", 30, 91, 5, 0),
    AdminGroup("2° C", "2", "Prof. Héctor Vega", 27, 96, 4, 1),
    AdminGroup("1° A", "1", "Profa. Elena Soto", 25, 89, 4, 0),
)

@Composable
fun AdminGroupsScreen(
    onBack: () -> Unit = {},
    onAgregarGrupo: () -> Unit = {},
    onGrupo: (String) -> Unit = {},
    onPanel: () -> Unit = {},
    onUsuarios: () -> Unit = {},
    onReportes: () -> Unit = {},
    onMas: () -> Unit = {},
) {
    var filter by rememberSaveable { mutableStateOf(GradeFilter.Todos) }

    val visible = remember(filter) {
        adminGroups.filter { filter.grade == null || it.grade == filter.grade }
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
                .padding(start = 16.dp, end = 16.dp, bottom = 112.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(0)
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BackIconButton(onBack = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Grupos",
                            color = PrimaryContainer,
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            letterSpacing = (-0.64).sp,
                        )
                        Text(
                            text = "Ciclo escolar 2026-2027",
                            color = Outline,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }

                AddCircleButton(onClick = onAgregarGrupo)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(60)
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GradeFilter.entries.forEach { option ->
                    GradeChip(
                        label = option.label,
                        selected = option == filter,
                        onClick = { filter = option },
                    )
                }
            }

            Column(
                modifier = Modifier.revealOnEnter(120),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                visible.forEach { group ->
                    GroupCard(group = group, onClick = { onGrupo(group.name) })
                }
            }
        }

        AdminBottomBar(
            active = AdminTab.Grupos,
            onSelect = { tab ->
                when (tab) {
                    AdminTab.Panel -> onPanel()
                    AdminTab.Usuarios -> onUsuarios()
                    AdminTab.Grupos -> Unit
                    AdminTab.Reportes -> onReportes()
                    AdminTab.Mas -> onMas()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun BackIconButton(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Volver",
            tint = OnSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun AddCircleButton(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "addGroupScale",
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(4.dp, CircleShape, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(CircleShape)
            .background(PrimaryContainer)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Agregar grupo",
            tint = OnPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun GradeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "gradeChipScale",
    )

    Box(
        modifier = Modifier
            .height(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) PrimaryContainer else SurfaceContainerLowest)
            .border(
                width = 1.dp,
                color = if (selected) PrimaryContainer else SurfaceVariant,
                shape = RoundedCornerShape(percent = 50),
            )
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) OnPrimary else OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun GroupCard(group: AdminGroup, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.99f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "groupCardScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(4.dp, CardShape, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceVariant, CardShape)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(BadgeShape)
                        .background(PrimaryFixed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = group.name,
                        color = PrimaryContainer,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row {
                        Text(
                            text = "Titular:",
                            color = Outline,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                        )
                        Text(
                            text = " ${group.titular}",
                            color = OnSurface,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${group.students} alumnos",
                        color = Outline,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Opciones del grupo ${group.name}",
                    tint = Outline,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SurfaceContainerHigh),
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            GroupStat(
                value = "${group.attendance}%",
                label = "Asistencia",
                valueColor = PrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            GroupStat(
                value = group.teachers.toString(),
                label = "Docentes",
                valueColor = OnSurface,
                modifier = Modifier.weight(1f),
                dividerStart = true,
                dividerEnd = true,
            )
            GroupStat(
                value = group.incidents.toString(),
                label = "Incidentes",
                valueColor = if (group.incidents > 0) Secondary else OnSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GroupStat(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    dividerStart: Boolean = false,
    dividerEnd: Boolean = false,
) {
    Row(modifier = modifier) {
        if (dividerStart) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(SurfaceContainer),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = Outline,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.33.sp,
            )
        }
        if (dividerEnd) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(SurfaceContainer),
            )
        }
    }
}
