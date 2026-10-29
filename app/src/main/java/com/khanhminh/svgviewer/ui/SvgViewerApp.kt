package com.khanhminh.svgviewer.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.khanhminh.svgviewer.BuildConfig
import com.khanhminh.svgviewer.R
import com.khanhminh.svgviewer.data.AppLanguage
import com.khanhminh.svgviewer.data.CanvasBackground
import com.khanhminh.svgviewer.data.IncomingSvg
import com.khanhminh.svgviewer.data.LanguageStore
import com.khanhminh.svgviewer.data.RecentFile
import com.khanhminh.svgviewer.data.RecentStore
import com.khanhminh.svgviewer.data.SettingsStore
import com.khanhminh.svgviewer.data.SvgDocument
import com.khanhminh.svgviewer.data.SvgLoader
import com.khanhminh.svgviewer.data.ThemeMode
import com.khanhminh.svgviewer.ui.haptics.AndroidHaptics
import com.khanhminh.svgviewer.ui.haptics.LocalHaptics
import com.khanhminh.svgviewer.ui.theme.AppIcons
import com.khanhminh.svgviewer.ui.theme.SvgViewerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SvgViewerApp(incoming: IncomingSvg? = null) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val languageStore = remember { LanguageStore(context) }
    val recentStore = remember { RecentStore(context) }
    val snackbar = remember { SnackbarHostState() }

    val haptics = remember(view) {
        AndroidHaptics(context, view) { store.hapticsEnabled }
    }

    var recents by remember { mutableStateOf(recentStore.list()) }
    var document by remember { mutableStateOf<SvgDocument?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPaste by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ripple by remember { mutableStateOf<RippleState?>(null) }
    var themeAnchor by remember { mutableStateOf(Offset.Zero) }

    val darkTheme = when (store.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    fun refreshRecents() {
        recents = recentStore.list()
    }

    fun openUri(uri: Uri) {
        scope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) {
                runCatching { SvgLoader.fromUri(context, uri) }
            }
            loading = false
            result.onSuccess { doc ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                // Lưu kèm bản sao nội dung: mở lại từ danh sách luôn được kể cả khi
                // quyền Uri hết hạn hoặc tệp gốc bị di chuyển.
                recentStore.add(uri, doc.name, doc.markup)
                refreshRecents()
                document = doc
            }.onFailure {
                errorMessage = context.getString(R.string.error_read)
            }
        }
    }

    fun openRecent(recent: RecentFile) {
        scope.launch {
            loading = true
            val doc = withContext(Dispatchers.IO) {
                val cached = recentStore.readCached(recent) ?: return@withContext null
                runCatching { SvgLoader.fromCached(cached, recent.name) }.getOrNull()
            }
            loading = false
            if (doc != null) {
                document = doc
            } else {
                openUri(recent.uri)
            }
        }
    }

    fun openSample(sample: SampleSvg) {
        val displayName = context.getString(sample.titleRes)
        scope.launch {
            loading = true
            val doc = withContext(Dispatchers.IO) {
                runCatching {
                    SvgLoader.fromAsset(context, sample.asset, "$displayName.svg")
                }.getOrNull()
            }
            loading = false
            if (doc != null) {
                document = doc
            } else {
                errorMessage = context.getString(R.string.error_sample, displayName)
            }
        }
    }

    fun openText(text: String, displayName: String) {
        scope.launch {
            loading = true
            val doc = withContext(Dispatchers.IO) {
                runCatching { SvgLoader.fromText(text, displayName) }.getOrNull()
            }
            loading = false
            if (doc != null) {
                document = doc
            } else {
                errorMessage = context.getString(R.string.error_not_svg)
            }
        }
    }

    fun openIncoming(payload: IncomingSvg) {
        val uri = payload.uri
        val text = payload.text
        when {
            uri != null -> openUri(uri)
            text != null -> openText(text, "shared.svg")
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) openUri(uri)
    }

    LaunchedEffect(incoming?.token) {
        if (incoming != null) openIncoming(incoming)
    }

    SvgViewerTheme(darkTheme = darkTheme) {
        val scheme = MaterialTheme.colorScheme

        // Status bar lấy đúng màu đang nằm sau nó: nền app ở màn chính,
        // nền canvas thật ở màn xem — nên icon luôn tương phản.
        val behindBars = if (document == null) scheme.background else Color(store.background.argb)
        SystemBars(lightIcons = behindBars.isLight())

        CompositionLocalProvider(LocalHaptics provides haptics) {
            Surface(
                color = scheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = document,
                        transitionSpec = {
                            if (targetState != null) screenTransition(1) else screenTransition(-1)
                        },
                        label = "home-viewer"
                    ) { current ->
                        if (current == null) {
                            HomeScreen(
                                recents = recents,
                                themeMode = store.themeMode,
                                onOpenFile = {
                                    picker.launch(
                                        arrayOf(
                                            "image/svg+xml",
                                            "image/svg",
                                            "text/xml",
                                            "application/xml",
                                            "text/plain",
                                            "application/octet-stream",
                                            "*/*"
                                        )
                                    )
                                },
                                onPaste = {
                                    haptics.click()
                                    showPaste = true
                                },
                                onOpenSample = { openSample(it) },
                                onOpenRecent = { openRecent(it) },
                                onTogglePin = { file ->
                                    recentStore.setPinned(file.uri, !file.pinned)
                                    refreshRecents()
                                },
                                onRemoveRecent = {
                                    recentStore.remove(it.uri)
                                    refreshRecents()
                                },
                                onClearRecents = {
                                    recentStore.clear()
                                    recents = emptyList()
                                },
                                onToggleTheme = {
                                    store.updateThemeMode(
                                        if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK
                                    )
                                    ripple = RippleState(themeAnchor)
                                },
                                onThemeAnchorChange = { themeAnchor = it },
                                onOpenSettings = { showSettings = true }
                            )
                        } else {
                            ViewerScreen(
                                document = current,
                                settings = store,
                                onThumbnail = { uri, bytes ->
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            recentStore.writeThumb(uri, bytes)
                                        }
                                        refreshRecents()
                                    }
                                },
                                onBack = { document = null }
                            )
                        }
                    }

                    // Lan toả từ nút đổi theme
                    val activeRipple = ripple
                    if (activeRipple != null) {
                        RippleOverlay(state = activeRipple) { ripple = null }
                    }

                    AnimatedVisibility(
                        visible = loading,
                        enter = fadeIn(tween(160)),
                        exit = fadeOut(tween(200))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(scheme.scrim.copy(alpha = 0.42f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = scheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(26.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(30.dp),
                                        color = scheme.onSurface,
                                        trackColor = scheme.surfaceContainerHighest,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = stringResource(R.string.viewer_loading),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    SnackbarHost(
                        hostState = snackbar,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(16.dp)
                    )
                }
            }

            if (showPaste) {
                ModalBottomSheet(
                    onDismissRequest = { showPaste = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = scheme.surface
                ) {
                    PasteSheet(
                        onDismiss = { showPaste = false },
                        onOpen = { text ->
                            showPaste = false
                            openText(text, "pasted.svg")
                        }
                    )
                }
            }

            if (showSettings) {
                ModalBottomSheet(
                    onDismissRequest = { showSettings = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = scheme.surface
                ) {
                    SettingsSheet(
                        store = store,
                        languageStore = languageStore,
                        onLanguageChange = { language ->
                            languageStore.update(language)
                            context.findActivity()?.recreate()
                        },
                        onClose = { showSettings = false }
                    )
                }
            }

            val message = errorMessage
            if (message != null) {
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    confirmButton = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text(stringResource(R.string.action_ok))
                        }
                    },
                    title = { Text(stringResource(R.string.error_open_title)) },
                    text = { Text(message) },
                    containerColor = scheme.surface
                )
            }
        }
    }
}

@Composable
private fun SettingsSheet(
    store: SettingsStore,
    languageStore: LanguageStore,
    onLanguageChange: (AppLanguage) -> Unit,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconAction(
                icon = AppIcons.Close,
                contentDescription = stringResource(R.string.settings_cd_close),
                onClick = onClose,
                tint = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.settings_appearance))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                ChoicePill(
                    text = stringResource(mode.labelRes),
                    selected = store.themeMode == mode,
                    onClick = { store.updateThemeMode(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel(stringResource(R.string.settings_language))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { language ->
                ChoicePill(
                    text = languageLabel(language),
                    selected = languageStore.language == language,
                    onClick = {
                        if (languageStore.language != language) onLanguageChange(language)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel(stringResource(R.string.settings_canvas))
        Spacer(Modifier.height(10.dp))
        CanvasBackground.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { bg ->
                    ChoicePill(
                        text = stringResource(bg.labelRes),
                        selected = store.background == bg,
                        onClick = {
                            haptics.selection()
                            store.updateBackground(bg)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(14.dp))
        Hairline()
        SettingRow(
            title = stringResource(R.string.settings_haptics),
            subtitle = stringResource(R.string.settings_haptics_desc)
        ) {
            Switch(
                checked = store.hapticsEnabled,
                onCheckedChange = {
                    haptics.selection()
                    store.updateHaptics(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = scheme.background,
                    checkedTrackColor = scheme.onBackground,
                    checkedBorderColor = Color.Transparent
                )
            )
        }
        Hairline()

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLogo(size = 24.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_footer),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.VIETNAMESE -> stringResource(R.string.language_vi)
    AppLanguage.ENGLISH -> stringResource(R.string.language_en)
}
