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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** Director's "Alumnos sin grupo" bulk-assignment screen (Stitch 11). */
@Composable
fun UngroupedStudentsScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val vm: UngroupedStudentsViewModel = viewModel(factory = container.viewModelFactory)
    val ui by vm.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(BrandCream)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Regresar",
                    tint = Primary,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Spacer(Modifier.padding(horizontal = 8.dp))
                Column {
                    Text(
                        text = "Alumnos sin grupo",
                        color = Primary,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                    Text(
                        text = "${ui.ungrouped.size} pendientes de asignación",
                        color = OnSurfaceVariant,
                        fontFamily = DmSans,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Seleccionar todos",
                    color = Primary,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { vm.toggleAll() },
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::onQueryChange,
                label = { Text("Buscar alumno o tutor") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.ungrouped, key = { it.id }) { student ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainerLowest)
                            .clickable { vm.toggle(student.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = student.id in ui.selected,
                            onCheckedChange = { vm.toggle(student.id) },
                        )
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = student.fullName.ifBlank { student.id },
                                color = Primary,
                                fontFamily = DmSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = "Tutor: ${student.tutorIds.firstOrNull().orEmpty()}",
                                color = OnSurfaceVariant,
                                fontFamily = DmSans,
                                fontSize = 11.sp,
                            )
                        }
                        Text(
                            text = "Sin grupo",
                            color = Outline,
                            fontFamily = DmSans,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            if (ui.error != null) {
                Text(text = ui.error.orEmpty(), color = Error, fontFamily = DmSans, fontSize = 12.sp)
            }
            if (ui.message != null) {
                Text(text = ui.message.orEmpty(), color = Primary, fontFamily = DmSans, fontSize = 12.sp)
            }

            // Bottom bar: selection count, grade/group selectors and assign action.
            if (ui.selected.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLowest)
                        .padding(12.dp),
                ) {
                    Text(
                        text = "${ui.selected.size} seleccionados",
                        color = Primary,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Grado", color = OnSurfaceVariant, fontFamily = DmSans, fontSize = 11.sp)
                    ChipRow(ui.grados.map { it.toString() }, ui.grado?.toString().orEmpty()) { value ->
                        value.toIntOrNull()?.let(vm::onGradoChange)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Grupo", color = OnSurfaceVariant, fontFamily = DmSans, fontSize = 11.sp)
                    ChipRow(ui.secciones, ui.grupo, vm::onGrupoChange)
                    Spacer(Modifier.height(12.dp))
                    PillButton(
                        text = if (ui.loading) "Asignando." else "Asignar a mismo salón",
                        onClick = { vm.assign() },
                        containerColor = Primary,
                    )
                }
            }
        }
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
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (item == selected) Primary else Outline.copy(alpha = 0.25f))
                    .clickable { onSelect(item) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Spacer(Modifier.size(6.dp))
        }
    }
}
