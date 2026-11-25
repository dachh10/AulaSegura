package com.example.aulasegura.ui.screens.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.repository.TeacherRequestFilter
import com.example.aulasegura.domain.repository.teacherRequestStateLabel
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** Director's teacher-request queue (Stitch 08): filters, cards, batch accept. */
@Composable
fun TeacherRequestsScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val vm: TeacherRequestsViewModel = viewModel(factory = container.viewModelFactory)
    val ui by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Regresar",
                tint = Primary,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Spacer(Modifier.padding(horizontal = 8.dp))
            Text(
                text = "Solicitudes de docentes",
                color = Primary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TeacherRequestFilter.entries.forEach { filter ->
                val label = filter.label + when (filter) {
                    TeacherRequestFilter.PENDIENTES -> " (${ui.pendingCount})"
                    else -> ""
                }
                Text(
                    text = label,
                    color = if (filter == ui.filter) SurfaceContainerLowest else Primary,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (filter == ui.filter) Primary else Outline.copy(alpha = 0.25f))
                        .clickable { vm.onFilterChange(filter) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandCoral.copy(alpha = 0.12f))
                .clickable { vm.acceptAll() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Aceptar todas",
                color = BrandCoral,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(16.dp))

        if (ui.visible.isEmpty()) {
            Text(
                text = when (ui.filter) {
                    TeacherRequestFilter.PENDIENTES -> "No hay solicitudes pendientes verificadas."
                    TeacherRequestFilter.ACEPTADAS -> "Todavía no hay docentes aprobados."
                    TeacherRequestFilter.RECHAZADAS -> "No hay solicitudes rechazadas."
                },
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 14.sp,
            )
        } else {
            ui.visible.forEach { teacher ->
                TeacherRequestCard(
                    teacher = teacher,
                    stateLabel = teacherRequestStateLabel(ui.filter),
                    onAccept = { vm.decide(teacher.uid, approve = true) },
                    onReject = { vm.decide(teacher.uid, approve = false) },
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        if (ui.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(text = ui.error.orEmpty(), color = Error, fontFamily = DmSans, fontSize = 13.sp)
        }
        if (ui.message != null) {
            Spacer(Modifier.height(12.dp))
            Text(text = ui.message.orEmpty(), color = Primary, fontFamily = DmSans, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TeacherRequestCard(
    teacher: AdminUser,
    stateLabel: String? = null,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .padding(16.dp),
    ) {
        Text(
            text = teacher.fullName.ifBlank { teacher.uid },
            color = Primary,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = teacher.email, color = OnSurfaceVariant, fontFamily = DmSans, fontSize = 12.sp)
        if (teacher.subjects.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = teacher.subjects.joinToString(", "),
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Correo verificado",
            color = Primary,
            fontFamily = DmSans,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(12.dp))
        if (stateLabel != null) {
            Text(
                text = stateLabel,
                color = if (stateLabel == "Rechazada") Error else Primary,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    text = "Rechazar",
                    onClick = onReject,
                    containerColor = SecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Aceptar",
                    onClick = onAccept,
                    containerColor = Primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
