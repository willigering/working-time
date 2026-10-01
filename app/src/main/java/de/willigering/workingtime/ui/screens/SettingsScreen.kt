package de.willigering.workingtime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.willigering.workingtime.R
import de.willigering.workingtime.core.AppInfo
import de.willigering.workingtime.data.Client
import de.willigering.workingtime.ui.components.ConfirmDeleteDialog
import de.willigering.workingtime.ui.components.GlassCard
import de.willigering.workingtime.ui.components.GoldOutlineButton
import de.willigering.workingtime.ui.components.ScreenHeading
import de.willigering.workingtime.ui.components.SettingsRow
import de.willigering.workingtime.ui.theme.AppColors

@Composable
fun SettingsHome(
    onOpen: (String) -> Unit,
    onExport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        ScreenHeading(stringResource(R.string.tab_settings))
        Spacer(Modifier.height(8.dp))
        SettingsRow(Icons.Rounded.Tune, stringResource(R.string.settings_general), stringResource(R.string.settings_general_sub)) {
            onOpen("general")
        }
        SettingsRow(Icons.Rounded.FileDownload, stringResource(R.string.export_title), stringResource(R.string.settings_export_sub), onExport)
        SettingsRow(Icons.Rounded.Folder, stringResource(R.string.projects_title), stringResource(R.string.settings_projects_sub)) {
            onOpen("projects")
        }
        SettingsRow(Icons.Rounded.Notifications, stringResource(R.string.settings_notifications), stringResource(R.string.settings_notifications_sub)) {
            onOpen("notifications")
        }
        SettingsRow(Icons.Rounded.Shield, stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_sub)) {
            onOpen("privacy")
        }
        SettingsRow(
            Icons.Rounded.Info,
            stringResource(R.string.settings_about),
            stringResource(R.string.settings_about_sub, AppInfo.VERSION),
        ) { onOpen("about") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SettingsSubpage(
    page: String,
    viewModel: de.willigering.workingtime.viewmodel.TimeTrackerViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val title = when (page) {
        "general" -> stringResource(R.string.settings_general)
        "projects" -> stringResource(R.string.projects_title)
        "notifications" -> stringResource(R.string.settings_notifications)
        "privacy" -> stringResource(R.string.settings_privacy)
        else -> stringResource(R.string.settings_about)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .systemBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppColors.TextPrimary)
            }
            Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            when (page) {
                "general" -> GeneralPage(viewModel)
                "projects" -> ProjectsSettingsPage(viewModel)
                "notifications" -> InfoBlock(stringResource(R.string.settings_notify_body))
                "privacy" -> InfoBlock(stringResource(R.string.settings_privacy_body))
                else -> AboutPage()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GeneralPage(viewModel: de.willigering.workingtime.viewmodel.TimeTrackerViewModel) {
    ProfileScreen(viewModel, part = ProfilePart.Identity, embed = true)
    Spacer(Modifier.height(16.dp))
    InfoBlock(stringResource(R.string.settings_appearance_body), stringResource(R.string.settings_appearance_title))
    Spacer(Modifier.height(10.dp))
    InfoBlock(stringResource(R.string.settings_language_body), stringResource(R.string.settings_language_title))
}

@Composable
private fun ProjectsSettingsPage(viewModel: de.willigering.workingtime.viewmodel.TimeTrackerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showClient by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Client?>(null) }
    var pending by remember { mutableStateOf<Client?>(null) }
    val clients = state.clients.filterNot { it.archived }

    Text(stringResource(R.string.settings_clients), color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    Spacer(Modifier.height(8.dp))
    if (clients.isEmpty()) {
        Text(stringResource(R.string.clients_empty_hint), color = AppColors.TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
    }
    clients.forEach { client ->
        val count = state.projects.count { it.clientName.equals(client.name, ignoreCase = true) }
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable { editing = client; showClient = true },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(client.name, color = AppColors.TextPrimary, fontWeight = FontWeight.Medium)
                    Text(
                        if (count == 1) stringResource(R.string.client_project_count_one)
                        else stringResource(R.string.client_project_count, count),
                        color = AppColors.TextMuted,
                        fontSize = 13.sp,
                    )
                }
                Text(
                    stringResource(R.string.delete),
                    color = AppColors.Error,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { pending = client },
                )
            }
        }
    }
    GoldOutlineButton(
        text = stringResource(R.string.add_client),
        icon = Icons.Rounded.Add,
        onClick = { editing = null; showClient = true },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(22.dp))
    Text(stringResource(R.string.profile_logo_title), color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    Spacer(Modifier.height(8.dp))
    ProfileScreen(viewModel, part = ProfilePart.Logo, embed = true)

    if (showClient) {
        ClientDialog(
            clientName = editing?.name,
            knownClients = viewModel.uniqueClientNames(includeArchived = true),
            onDismiss = { showClient = false },
            onSave = { name ->
                val current = editing
                if (current != null) viewModel.updateClient(current.copy(name = name))
                else viewModel.addClient(name)
                showClient = false
            },
        )
    }
    pending?.let { client ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_client_title),
            message = stringResource(R.string.delete_client_message, client.name),
            onConfirm = {
                viewModel.deleteClient(client.id)
                pending = null
            },
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun InfoBlock(body: String, title: String? = null) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
        }
        Text(body, color = AppColors.TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun AboutPage() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.app_name), color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.version_label, AppInfo.VERSION, AppInfo.BUILD_NUMBER),
            color = AppColors.TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.label_developer) + ": " + AppInfo.DEVELOPER, color = AppColors.TextSecondary)
    }
    Spacer(Modifier.height(16.dp))
    SupportProjectSection()
}
