package com.example.aulasegura.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.theme.Background
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

private val GRADOS = listOf("1", "2", "3", "4", "5", "6")
private val GRUPOS = listOf("A", "B", "C", "D", "E", "F")

/**
 * The director reviews the QR payload, assigns grade/group and either accepts
 * (creating the student and linking the tutor) or rejects the request.
 */
@Composable
fun ReviewStudentScreen(
    uid: String,
    alumno: String,
    onBack: () -> Unit,
) {
    val container = rememberAppContainer()
    val vm: ReviewStudentViewModel = viewModel<ReviewStudentViewModel>(
        factory = viewModelFactory {
            initializer { container.viewModelFactory.reviewStudentViewModel(uid, alumno) }
        },
    )
    val ui by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Regresar",
                tint = Primary,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Spacer(Modifier.padding(horizontal = 8.dp))
            Text(
                text = "Revisar alumno",
                color = Primary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        }
        Spacer(Modifier.height(20.dp))

        ReviewCard(title = "Datos del Alumno") {
            Text(text = alumno, fontFamily = DmSans, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        ReviewCard(title = "Tutor Registrado") {
            Text(text = "UID: $uid", fontFamily = DmSans, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Correo verificado",
                color = Primary,
                fontFamily = DmSans,
                fontSize = 12.sp,
            )
        }

        val duplicate = ui.duplicate
        if (duplicate != null) {
            Spacer(Modifier.height(12.dp))
            ReviewCard(title = "Posible coincidencia de alumno") {
                Text(
                    text = "Ya existe un alumno con este nombre: ${duplicate.name}",
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = ui.form.linkAsSecondTutor,
                        onCheckedChange = {
                            vm.onFormChange(ui.form.copy(linkAsSecondTutor = it))
                        },
                    )
                    Text(
                        text = "Vincular como segundo tutor",
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Asignación de aula",
            color = Primary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(text = "Grado", color = OnSurfaceVariant, fontFamily = DmSans, fontSize = 12.sp)
        ChipRow(GRADOS, ui.form.grado) { vm.onFormChange(ui.form.copy(grado = it)) }
        Spacer(Modifier.height(10.dp))
        Text(text = "Grupo", color = OnSurfaceVariant, fontFamily = DmSans, fontSize = 12.sp)
        ChipRow(GRUPOS, ui.form.grupo) { vm.onFormChange(ui.form.copy(grupo = it)) }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = ui.form.sinGrupo,
                onCheckedChange = { vm.onFormChange(ui.form.copy(sinGrupo = it)) },
            )
            Text(
                text = "Dejar sin grupo por ahora",
                fontFamily = DmSans,
                fontSize = 13.sp,
            )
        }

        if (ui.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(text = ui.error.orEmpty(), color = Error, fontFamily = DmSans, fontSize = 13.sp)
        }
        if (ui.message != null) {
            Spacer(Modifier.height(12.dp))
            Text(text = ui.message.orEmpty(), color = Primary, fontFamily = DmSans, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))
        PillButton(
            text = if (ui.loading) "Guardando." else "Aceptar",
            onClick = { vm.approve() },
            containerColor = Primary,
        )
        Spacer(Modifier.height(12.dp))
        PillButton(
            text = "Rechazar",
            onClick = { vm.reject() },
            containerColor = SecondaryContainer,
        )
    }
}

@Composable
private fun ReviewCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            color = Primary,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun ChipRow(
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        items.forEach { item ->
            Text(
                text = item,
                color = if (item == selected) SurfaceContainerLowest else Primary,
                fontFamily = DmSans,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (item == selected) Primary else OutlineVariant.copy(alpha = 0.3f))
                    .clickable { onSelect(item) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Spacer(Modifier.padding(horizontal = 3.dp))
        }
    }
}
