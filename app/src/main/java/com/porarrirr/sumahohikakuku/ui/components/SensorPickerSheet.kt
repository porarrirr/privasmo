package com.porarrirr.sumahohikakuku.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.model.SensorSpec
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SensorPickerSheet(
    availableSensors: List<SensorSpec>,
    selectedSensorValue: String,
    onSelectSensor: (String) -> Unit,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var manufacturerFilter by remember { mutableStateOf<String?>(null) }

    val manufacturerOptions = remember(availableSensors) {
        val seen = linkedSetOf<String>()
        availableSensors.forEach { sensor ->
            if (!sensor.isManual && sensor.manufacturer.isNotBlank()) {
                seen.add(sensor.manufacturer)
            }
        }
        seen.toList()
    }

    val filteredSensors = remember(availableSensors, query, manufacturerFilter) {
        val tokens = query.trim()
            .replace('\u3000', ' ')
            .lowercase(Locale.US)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        availableSensors.filter { sensor ->
            if (sensor.isManual) return@filter true
            if (manufacturerFilter != null && sensor.manufacturer != manufacturerFilter) {
                return@filter false
            }
            if (tokens.isEmpty()) return@filter true
            val haystack = "${sensor.name} ${sensor.manufacturer}".lowercase(Locale.US)
            tokens.all { token -> haystack.contains(token) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.title_sensor_picker),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.action_close))
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.label_search)) },
                placeholder = { Text(stringResource(R.string.placeholder_search_sensor)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
                trailingIcon = {
                    if (query.isNotBlank()) {
                        TextButton(onClick = { query = "" }) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                }
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = manufacturerFilter == null,
                        onClick = { manufacturerFilter = null },
                        label = { Text(stringResource(R.string.label_all)) }
                    )
                }
                items(manufacturerOptions, key = { it }) { manufacturer ->
                    FilterChip(
                        selected = manufacturerFilter == manufacturer,
                        onClick = {
                            manufacturerFilter =
                                if (manufacturerFilter == manufacturer) null else manufacturer
                        },
                        label = { Text(manufacturer) }
                    )
                }
            }

            HorizontalDivider()

            if (filteredSensors.isEmpty()) {
                Text(
                    text = stringResource(R.string.text_no_matching_sensors),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                ) {
                    items(filteredSensors, key = { it.value }) { sensor ->
                        val isSelected = sensor.value == selectedSensorValue
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (sensor.isManual) {
                                        stringResource(R.string.label_manual_input)
                                    } else {
                                        sensor.name
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                onSelectSensor(sensor.value)
                                onClose()
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Text(
                                        text = stringResource(R.string.label_selected_short),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
