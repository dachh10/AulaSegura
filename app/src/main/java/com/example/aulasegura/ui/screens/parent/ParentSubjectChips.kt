package com.example.aulasegura.ui.screens.parent

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** "Todas" first, then one chip per known subject of the month. */
@Composable
fun ParentSubjectChips(
    subjects: List<ParentSubjectUi>,
    selectedSubjectId: String?,
    onSelect: (String?) -> Unit,
) {
    if (subjects.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedSubjectId == null,
            onClick = { onSelect(null) },
            label = { Text("Todas") },
        )
        subjects.forEach { subject ->
            FilterChip(
                selected = selectedSubjectId == subject.id,
                onClick = { onSelect(subject.id) },
                label = { Text(subject.label) },
            )
        }
    }
}
