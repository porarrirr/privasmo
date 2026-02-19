package com.porarrirr.sumahohikakuku.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.porarrirr.sumahohikakuku.R
import com.porarrirr.sumahohikakuku.model.SensorSpec
import com.porarrirr.sumahohikakuku.ui.SensorComparisonActions
import com.porarrirr.sumahohikakuku.viewmodel.DeviceInputState
import com.porarrirr.sumahohikakuku.viewmodel.SensorComparisonUiState

/**
 * Device configuration section using ScrollableTabRow for device switching
 * instead of the previous horizontal carousel/LazyRow.
 */
@Composable
internal fun DeviceConfigSection(
    uiState: SensorComparisonUiState,
    focusDeviceId: Long?,
    actions: SensorComparisonActions
) {
    if (uiState.devices.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        var selectedTabIndex by remember { mutableIntStateOf(0) }

        // When a new device is added and focus is requested, switch to it
        LaunchedEffect(focusDeviceId) {
            val id = focusDeviceId ?: return@LaunchedEffect
            val index = uiState.devices.indexOfFirst { it.id == id }
            if (index >= 0) {
                selectedTabIndex = index
            }
            actions.consumeDeviceFocusRequest()
        }

        // Ensure tab index stays valid after device removal
        LaunchedEffect(uiState.devices.size) {
            if (selectedTabIndex >= uiState.devices.size) {
                selectedTabIndex = (uiState.devices.size - 1).coerceAtLeast(0)
            }
        }

        val defaultDeviceName = stringResource(R.string.label_device_default_name)

        // Tab row with device name + color dot
        if (uiState.devices.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.devices.forEachIndexed { index, device ->
                    val deviceColor = runCatching { Color(AndroidColor.parseColor(device.colorHex)) }
                        .getOrElse { MaterialTheme.colorScheme.primary }
                    Tab(
                        selected = index == selectedTabIndex,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(deviceColor)
                                        .border(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.outline,
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = device.name.ifBlank { defaultDeviceName },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))
        }

        // Show the selected device's card
        val currentDevice = uiState.devices.getOrNull(selectedTabIndex)
        if (currentDevice != null) {
            DeviceCard(
                device = currentDevice,
                availableSensors = uiState.availableSensors,
                availableColors = uiState.availableDeviceColors,
                actions = actions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
