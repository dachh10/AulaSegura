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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

private enum class UsersTab(val label: String, val count: Int, val searchHint: String, val hint: String) {
    Docentes(
        label = "Docentes",
        count = 4,
        searchHint = "Buscar docente",
        hint = "Los docentes reciben su acceso por correo cuando los das de alta.",
    ),
    Padres(
        label = "Padres",
        count = 62,
        searchHint = "Buscar padre",
        hint = "Las familias reciben su invitación cuando registras al alumno.",
    ),
    Alumnos(
        label = "Alumnos",
        count = 85,
        searchHint = "Buscar alumno",
        hint = "Los alumnos quedan vinculados al registrarse con su tutor.",
    ),
}

private enum class AvatarStyle {
    Indigo,
    DeepIndigo,
    Neutral,
    Coral,
}

private data class UserRow(
    val initials: String,
    val name: String,
    val subtitle: String,
    val active: Boolean,
    val avatar: AvatarStyle,
)

private val docentes = listOf(
    UserRow("RS", "Ricardo Salinas Ortega", "Matemáticas · 3° B, 3° A, 2° C", true, AvatarStyle.Indigo),
    UserRow("LM", "Laura Mendoza Ríos", "Español · 3° A", true, AvatarStyle.DeepIndigo),
    UserRow("HV", "Héctor Vega Luna", "Ciencias · 2° C", true, AvatarStyle.Neutral),
    UserRow("SP", "Sofía Pineda Cruz", "Historia · 3° B", false, AvatarStyle.Coral),
    UserRow("MA", "Manuel Alarcón Cruz", "Educación Física · 1° A, 2° B", true, AvatarStyle.Indigo),
)

private val padres = listOf(
    UserRow("CG", "Carlos Gómez Pérez", "Tutor de Mariana 3° B y Diego 1° A", true, AvatarStyle.Indigo),
    UserRow("AR", "Ana Ruiz Sánchez", "Tutora de Sofía 3° B", true, AvatarStyle.Coral),
    UserRow("LP", "Luis Pineda Mora", "Tutor de Diego 2° C", true, AvatarStyle.DeepIndigo),
    UserRow("RC", "Rocío Cruz Nava", "Tutora de Manuel 1° A", false, AvatarStyle.Neutral),
)

private val alumnos = listOf(
    UserRow("MG", "Mariana Gómez Ruiz", "3° B · Matrícula 3B-014", true, AvatarStyle.Indigo),
    UserRow("DG", "Diego Gómez Ruiz", "1° A · Matrícula 1A-007", true, AvatarStyle.Coral),
    UserRow("XT", "Ximena Torres Díaz", "3° B · Matrícula 3B-021", true, AvatarStyle.Neutral),
    UserRow("EC", "Emiliano Castro Rivas", "2° C · Matrícula 2C-009", false, AvatarStyle.DeepIndigo),
)

@Composable
fun AdminUsersScreen(
    onAgregarUsuario: () -> Unit = {},
    onPanel: () -> Unit = {},
    onGrupos: () -> Unit = {},
    onReportes: () -> Unit = {},
    onMas: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(UsersTab.Docentes) }
    var query by rememberSaveable { mutableStateOf("") }

    val rows = when (selectedTab) {
        UsersTab.Docentes -> docentes
        UsersTab.Padres -> padres
        UsersTab.Alumnos -> alumnos
    }.filter { it.name.contains(query.trim(), ignoreCase = true) }

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
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer)
                        .clickable(onClick = onAgregarUsuario),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "Agregar usuario",
                        tint = OnPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Reveal(60) {
                UsersTabs(
                    selected = selectedTab,
                    onSelect = {
                        if (it != selectedTab) {
                            selectedTab = it
                            query = ""
                        }
                    },
                )
            }

            Reveal(120) {
                SearchRow(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = selectedTab.searchHint,
                )
            }

            Reveal(180) {
                if (rows.isEmpty()) {
                    Text(
                        text = "Sin resultados para \"${query.trim()}\"",
                        color = OnSurfaceVariant,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    )
                } else {
                    Column(
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
                            UserRowItem(row)
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
}

@Composable
private fun UsersTabs(selected: UsersTab, onSelect: (UsersTab) -> Unit) {
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
        UsersTab.entries.forEach { tab ->
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
                        text = tab.count.toString(),
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
) {
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

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(InputShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.5f), InputShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "Filtrar",
                tint = Primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun UserRowItem(row: UserRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { }
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
