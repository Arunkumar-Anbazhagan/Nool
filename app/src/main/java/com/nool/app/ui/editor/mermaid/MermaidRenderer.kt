package com.nool.app.ui.editor.mermaid

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.collection.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nool.app.ui.theme.Dimens
import org.json.JSONObject

object MermaidCache {
    val svgCache = LruCache<String, String>(50)
    val heightCache = LruCache<String, Int>(50)
}

class MermaidBridge(
    private val onSvgSuccess: (hash: String, svg: String, height: Int) -> Unit,
    private val onSvgError: (hash: String, error: String) -> Unit
) {
    @JavascriptInterface
    fun onRenderSuccess(hash: String, svg: String, height: Int) {
        onSvgSuccess(hash, svg, height)
    }

    @JavascriptInterface
    fun onRenderError(hash: String, error: String) {
        onSvgError(hash, error)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MermaidDiagramCard(
    code: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()

    val diagramHash = remember(code, isDark) {
        "${code.hashCode()}_${if (isDark) 1 else 0}"
    }

    var renderedSvg by remember(diagramHash) {
        mutableStateOf(MermaidCache.svgCache.get(diagramHash))
    }
    var renderedHeight by remember(diagramHash) {
        mutableIntStateOf(MermaidCache.heightCache.get(diagramHash) ?: 180)
    }
    var errorMessage by remember(diagramHash) {
        mutableStateOf<String?>(null)
    }
    var isLoading by remember(diagramHash) {
        mutableStateOf(renderedSvg == null)
    }
    var showFullscreen by remember { mutableStateOf(false) }

    // If already cached, don't re-render
    LaunchedEffect(diagramHash) {
        val cached = MermaidCache.svgCache.get(diagramHash)
        if (cached != null) {
            renderedSvg = cached
            renderedHeight = MermaidCache.heightCache.get(diagramHash) ?: 180
            isLoading = false
            errorMessage = null
        }
    }

    if (showFullscreen && renderedSvg != null) {
        MermaidFullscreenDialog(
            svg = renderedSvg!!,
            onDismiss = { showFullscreen = false }
        )
    }

    Surface(
        shape = RoundedCornerShape(Dimens.CornerMedium),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Dimens.CornerMedium))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Title and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mermaid Diagram",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(code.trim())) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Mermaid Code",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (renderedSvg != null && errorMessage == null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showFullscreen = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen Diagram",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error display
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(Dimens.CornerSmall),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Syntax Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mermaid Syntax Error",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(Dimens.CornerXSmall),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = code.trim(),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            } else {
                // Interactive Diagram View
                val targetHeight = renderedHeight.coerceIn(120, 600).dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(targetHeight)
                        .clip(RoundedCornerShape(Dimens.CornerSmall))
                        .clickable(enabled = renderedSvg != null) { showFullscreen = true },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(AndroidColor.TRANSPARENT)
                                settings.javaScriptEnabled = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE

                                addJavascriptInterface(
                                    MermaidBridge(
                                        onSvgSuccess = { hash, svg, height ->
                                            post {
                                                MermaidCache.svgCache.put(hash, svg)
                                                MermaidCache.heightCache.put(hash, height)
                                                renderedSvg = svg
                                                renderedHeight = height
                                                isLoading = false
                                                errorMessage = null
                                            }
                                        },
                                        onSvgError = { _, err ->
                                            post {
                                                isLoading = false
                                                errorMessage = err
                                            }
                                        }
                                    ),
                                    "AndroidBridge"
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        val escapedCode = JSONObject.quote(code)
                                        evaluateJavascript("renderMermaid('$diagramHash', $escapedCode, $isDark);", null)
                                    }
                                }

                                loadUrl("file:///android_asset/mermaid/mermaid_runner.html")
                            }
                        },
                        update = { webView ->
                            if (renderedSvg == null) {
                                val escapedCode = JSONObject.quote(code)
                                webView.evaluateJavascript("renderMermaid('$diagramHash', $escapedCode, $isDark);", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MermaidFullscreenDialog(
    svg: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 6f)
                            offset += pan
                        }
                    }
            ) {
                // Rendered SVG with zoom/pan transforms
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(AndroidColor.TRANSPARENT)
                                settings.javaScriptEnabled = false
                                val html = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                      <style>
                                        body { margin: 0; padding: 16px; background: transparent; display: flex; justify-content: center; align-items: center; min-height: 100vh; }
                                        svg { width: 100% !important; height: auto !important; }
                                      </style>
                                    </head>
                                    <body>$svg</body>
                                    </html>
                                """.trimIndent()
                                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Controls: Close and Reset
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                        tonalElevation = 4.dp
                    ) {
                        IconButton(onClick = { scale = 1f; offset = Offset.Zero }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom")
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                        tonalElevation = 4.dp
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Fullscreen")
                        }
                    }
                }
            }
        }
    }
}
