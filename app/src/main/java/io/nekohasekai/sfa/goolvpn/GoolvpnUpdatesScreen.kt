package io.nekohasekai.sfa.goolvpn

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.BuildConfig
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.update.UpdateState
import io.nekohasekai.sfa.vendor.Vendor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoolvpnUpdatesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isChecking by UpdateState.isChecking
    val isDownloading by UpdateState.isDownloading
    val updateInfo by UpdateState.updateInfo
    var resultText by remember { mutableStateOf<String?>(null) }

    fun checkForUpdates() {
        scope.launch {
            UpdateState.isChecking.value = true
            resultText = null
            try {
                val result = withContext(Dispatchers.IO) { Vendor.checkUpdateAsync() }
                UpdateState.setUpdate(result)
                resultText = context.getString(
                    if (result == null) R.string.goolvpn_updates_current
                    else R.string.goolvpn_updates_available,
                    result?.versionName.orEmpty(),
                )
            } catch (error: Exception) {
                Log.w("GoolvpnUpdates", "Update check failed", error)
                resultText = context.getString(R.string.goolvpn_updates_check_failed)
            } finally {
                UpdateState.isChecking.value = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.goolvpn_updates)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(android.R.string.cancel))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.goolvpn_updates_installed, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Text(
                            text = stringResource(R.string.goolvpn_updates_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = ::checkForUpdates,
                    enabled = !isChecking && !isDownloading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(stringResource(R.string.goolvpn_updates_check))
                }
            }
            resultText?.let { message ->
                item {
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
            updateInfo?.let { info ->
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.goolvpn_updates_available, info.versionName),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            info.releaseNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        UpdateState.isDownloading.value = true
                                        UpdateState.downloadError.value = null
                                        try {
                                            withContext(Dispatchers.IO) {
                                                Vendor.downloadAndInstall(context, info.downloadUrl)
                                            }
                                        } catch (error: Exception) {
                                            Log.w("GoolvpnUpdates", "Update download failed", error)
                                            UpdateState.downloadError.value = error.message
                                        } finally {
                                            UpdateState.isDownloading.value = false
                                        }
                                    }
                                },
                                enabled = !isChecking && !isDownloading,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                                }
                                Text(stringResource(R.string.goolvpn_updates_install))
                            }
                        }
                    }
                }
            }
            UpdateState.downloadError.value?.let {
                item { Text(stringResource(R.string.goolvpn_updates_download_failed), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
