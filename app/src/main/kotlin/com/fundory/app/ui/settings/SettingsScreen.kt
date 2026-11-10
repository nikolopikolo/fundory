package com.fundory.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundory.app.data.backup.BackupValidationResult
import com.fundory.app.ui.components.ConfirmationDialog
import com.fundory.app.ui.theme.FundoryGreen
import com.fundory.app.ui.theme.NegativeRed
import com.fundory.app.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showClearConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedbackMessage()
        }
    }

    // Storage Access Framework JSON Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonString = viewModel.getExportJson()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(jsonString.toByteArray())
                    }
                    snackbarHostState.showSnackbar("Backup exported successfully!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // Storage Access Framework JSON Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().use { it.readText() }
                }
                if (jsonString != null) {
                    viewModel.inspectImportJson(jsonString)
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to open file: ${e.localizedMessage}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Appearance Section
            SettingsSection(title = "APPEARANCE") {
                SettingSwitchRow(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Theme",
                    subtitle = "Near-black aesthetic optimized for AMOLED screens",
                    checked = state.preferences.isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )

                SettingSwitchRow(
                    icon = Icons.Default.ColorLens,
                    title = "Dynamic Colors",
                    subtitle = "Use Material You system palette on supported devices",
                    checked = state.preferences.useDynamicColors,
                    onCheckedChange = { viewModel.toggleDynamicColors(it) }
                )
            }

            // Currency & Exchange Rates Section
            SettingsSection(title = "CURRENCY & CONVERSION") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Display Currency",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Georgian Lari (GEL / ₾)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(FundoryGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🇬🇪 GEL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FundoryGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Exchange Rates",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Last synced: ${state.lastSyncFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.syncExchangeRatesNow() },
                        enabled = !state.isSyncing,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = FundoryGreen)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Notifications Section
            SettingsSection(title = "NOTIFICATIONS") {
                SettingSwitchRow(
                    icon = Icons.Default.Notifications,
                    title = "Milestone Alerts",
                    subtitle = "Notify when reaching 25%, 50%, 75%, 90%, 100% of goal targets",
                    checked = state.preferences.enableMilestoneNotifications,
                    onCheckedChange = { viewModel.toggleMilestoneNotifications(it) }
                )

                SettingSwitchRow(
                    icon = Icons.Default.Savings,
                    title = "Monthly Savings Reminder",
                    subtitle = "Remind to deposit planned savings at the beginning of each month",
                    checked = state.preferences.enableMonthlyReminderNotifications,
                    onCheckedChange = { viewModel.toggleMonthlyReminderNotifications(it) }
                )
            }

            // Backup & Restore Section
            SettingsSection(title = "DATA BACKUP & RESTORE") {
                SettingActionRow(
                    icon = Icons.Default.CloudUpload,
                    title = "Export Backup (JSON)",
                    subtitle = "Safely save all goals, items, and transactions locally",
                    onClick = {
                        val fileName = "Fundory_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.json"
                        exportLauncher.launch(fileName)
                    }
                )

                SettingActionRow(
                    icon = Icons.Default.CloudDownload,
                    title = "Import Backup (JSON)",
                    subtitle = "Restore your savings records from a JSON file",
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/*"))
                    }
                )

                SettingActionRow(
                    icon = Icons.Default.Science,
                    title = "Load Realistic Sample Data",
                    subtitle = "Populate PC Upgrade, Camera, and Guitar goals with sample transactions",
                    onClick = { viewModel.loadSampleData() }
                )

                SettingActionRow(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear All Savings Data",
                    subtitle = "Permanently remove all goals and transaction history",
                    titleColor = NegativeRed,
                    onClick = { showClearConfirmation = true }
                )
            }

            // China to Georgia Shipping Rates Section
            SettingsSection(title = "SHIPPING RATES (CHINA TO GEORGIA)") {
                var boxetteAirText by remember(state.preferences.boxetteAirRateUsd) { mutableStateOf(state.preferences.boxetteAirRateUsd.toString()) }
                var boxetteGroundText by remember(state.preferences.boxetteGroundRateUsd) { mutableStateOf(state.preferences.boxetteGroundRateUsd.toString()) }
                var inexAirText by remember(state.preferences.inexAirRateUsd) { mutableStateOf(state.preferences.inexAirRateUsd.toString()) }
                var inexSeaText by remember(state.preferences.inexSeaRateUsd) { mutableStateOf(state.preferences.inexSeaRateUsd.toString()) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = boxetteAirText,
                            onValueChange = { boxetteAirText = it },
                            label = { Text("Boxette Air ($/kg)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = boxetteGroundText,
                            onValueChange = { boxetteGroundText = it },
                            label = { Text("Boxette Grnd ($/kg)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = inexAirText,
                            onValueChange = { inexAirText = it },
                            label = { Text("Inex Air ($/kg)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = inexSeaText,
                            onValueChange = { inexSeaText = it },
                            label = { Text("Inex Sea ($/kg)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val bAir = boxetteAirText.toDoubleOrNull() ?: 12.90
                            val bGnd = boxetteGroundText.toDoubleOrNull() ?: 5.45
                            val iAir = inexAirText.toDoubleOrNull() ?: 10.50
                            val iSea = inexSeaText.toDoubleOrNull() ?: 4.50
                            viewModel.updateCarrierRates(bAir, bGnd, iAir, iSea)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FundoryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Carrier Rates")
                    }
                }
            }

            // About Fundory
            SettingsSection(title = "ABOUT") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.fundory.app.R.drawable.ic_fundory_logo),
                            contentDescription = "Fundory Emblem",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Fundory Native Android",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Version 1.1.0 · Local-First Architecture",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "Fundory operates completely offline with zero tracking, advertising, or third-party user accounts. All personal savings data remains exclusively on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Confirmation dialog for Clear Data
    if (showClearConfirmation) {
        ConfirmationDialog(
            title = "Clear All Data?",
            message = "This action will permanently delete all your goals, products, and transactions. Make sure you have exported a backup if needed.",
            confirmButtonText = "Clear Everything",
            onConfirm = {
                viewModel.clearAllData()
                showClearConfirmation = false
            },
            onDismiss = { showClearConfirmation = false }
        )
    }

    // Preview Dialog for JSON Import
    state.backupPreview?.let { preview ->
        if (preview.isValid && preview.backupData != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissBackupPreview() },
                title = { Text("Restore Fundory Data", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Backup created on: ${preview.exportDateFormatted}")
                        Text("• ${preview.goalCount} Goals")
                        Text("• ${preview.productCount} Products")
                        Text("• ${preview.transactionCount} Transactions")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Choose whether to Replace all existing data or Merge the imported records into your current database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.executeRestore(preview.backupData, isReplaceMode = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = NegativeRed)
                    ) {
                        Text("Replace All")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { viewModel.dismissBackupPreview() }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = { viewModel.executeRestore(preview.backupData, isReplaceMode = false) }
                        ) {
                            Text("Merge", color = FundoryGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            color = FundoryGreen
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FundoryGreen
            )
        )
    }
}

@Composable
private fun SettingActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == NegativeRed) NegativeRed else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
