package de.willigering.workingtime.ui.screens

import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import de.willigering.workingtime.util.LogoImages
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import de.willigering.workingtime.R
import de.willigering.workingtime.ui.components.GlassCard
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel
import java.io.File

@Composable
fun ProfileScreen(viewModel: TimeTrackerViewModel) {
    val state by viewModel.state.collectAsState()
    val profile = state.userProfile
    val context = LocalContext.current

    var name by rememberSaveable(profile.displayName) { mutableStateOf(profile.displayName) }
    var company by rememberSaveable(profile.companyName) { mutableStateOf(profile.companyName) }
    var logoVersion by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var turns by remember { mutableStateOf(0) }
    var crop by remember { mutableStateOf(false) }
    var previewFailed by remember { mutableStateOf(false) }

    val logoBitmap by produceState<ImageBitmap?>(null, profile.logoPath, logoVersion) {
        value = withContext(Dispatchers.IO) {
            try {
                if (profile.logoPath.isBlank()) null
                else LogoImages.decode(context, Uri.fromFile(File(profile.logoPath)), 384)?.asImageBitmap()
            } catch (_: Exception) { null } catch (_: OutOfMemoryError) { null }
        }
    }
    val preview by produceState<ImageBitmap?>(null, pendingUri, turns, crop) {
        value = null
        previewFailed = false
        value = withContext(Dispatchers.IO) {
            try { pendingUri?.let { LogoImages.decode(context, it, 512, turns, crop)?.asImageBitmap() } }
            catch (_: Exception) { null } catch (_: OutOfMemoryError) { null }
        }
        previewFailed = pendingUri != null && value == null
    }

    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { pendingUri = uri; turns = 0; crop = false }
    }

    if (pendingUri != null) AlertDialog(
        onDismissRequest = { if (!busy) pendingUri = null },
        title = { Text(stringResource(R.string.logo_preview)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth().height(160.dp).background(Color.White), contentAlignment = Alignment.Center) {
                    val image = preview
                    if (image != null) Image(image, stringResource(R.string.profile_logo_title),
                        Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit)
                    else Text(stringResource(if (previewFailed) R.string.profile_logo_failed else R.string.logo_loading), color = Color.DarkGray)
                }
                TextButton(enabled = !busy, onClick = { turns = (turns + 1) % 4 }) {
                    Text(stringResource(R.string.logo_rotate))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = crop, enabled = !busy, onCheckedChange = { crop = it })
                    Text(stringResource(R.string.logo_crop))
                }
                Text(stringResource(R.string.logo_white_hint), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(enabled = !busy && preview != null, onClick = {
                val uri = pendingUri ?: return@Button
                val rotation = turns
                val cropped = crop
                busy = true
                scope.launch {
                    try {
                        val ok = viewModel.setUserLogoFromUri(uri, rotation, cropped)
                        if (ok) { logoVersion++; pendingUri = null }
                        Toast.makeText(context, if (ok) R.string.profile_logo_saved else R.string.profile_logo_failed, Toast.LENGTH_SHORT).show()
                    } finally { busy = false }
                }
            }) { Text(stringResource(if (busy) R.string.export_working else R.string.save)) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = { pendingUri = null }) { Text(stringResource(R.string.cancel)) } },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.profile_title),
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.TextPrimary,
        )
        Text(
            stringResource(R.string.profile_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
        )
        Spacer(Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.profile_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text(stringResource(R.string.profile_company)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    viewModel.updateUserProfile(name, company)
                    Toast.makeText(context, R.string.profile_saved, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
        }

        Spacer(Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), accentColor = AppColors.Accent) {
            Text(
                stringResource(R.string.profile_logo_title),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.profile_logo_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, AppColors.GlassBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (logoBitmap != null) {
                        Image(
                            bitmap = logoBitmap,
                            contentDescription = stringResource(R.string.profile_logo_title),
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            tint = AppColors.TextMuted,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { pickLogo.launch("image/*") },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.profile_logo_pick))
                    }
                    if (logoBitmap != null) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                busy = true
                                scope.launch {
                                    try { viewModel.clearUserLogo(); logoVersion++ }
                                    finally { busy = false }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = AppColors.Error,
                                modifier = Modifier.size(18.dp),
                            )
                            Text("  ${stringResource(R.string.profile_logo_remove)}")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.logo_export_preview), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(profile.issuerTitle(), color = Color.Black, maxLines = 2)
                Text(profile.companyName, color = Color.DarkGray, maxLines = 2)
            }
            val image = logoBitmap
            if (image != null) Image(image, stringResource(R.string.profile_logo_title),
                Modifier.size(width = 100.dp, height = 44.dp), contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.height(16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.profile_logo_recommend_title),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.profile_logo_recommend_body),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
            )
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.Accent,
    unfocusedBorderColor = AppColors.GlassBorder,
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    focusedLabelColor = AppColors.Accent,
    unfocusedLabelColor = AppColors.TextMuted,
    cursorColor = AppColors.Accent,
)
