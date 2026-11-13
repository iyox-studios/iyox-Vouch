package com.iyox.vouch.feature.settings

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.core.text.htmlEncode
import com.iyox.vouch.R
import com.iyox.vouch.ui.components.VouchBackButton

@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val policy = remember(context) {
        val markdown = context.assets.open("PRIVACY.md").bufferedReader().use { it.readText() }
        // The policy uses headings and paragraphs. Keep the repository Markdown as the source.
        val html = markdown.trim().split(Regex("\\n\\s*\\n")).joinToString("") { block ->
            val (tag, text) = when {
                block.startsWith("## ") -> "h2" to block.removePrefix("## ")
                block.startsWith("# ") -> "h1" to block.removePrefix("# ")
                else -> "p" to block
            }
            "<$tag>${text.htmlEncode()}</$tag>"
        }
        HtmlCompat.fromHtml(
            html,
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text(stringResource(R.string.settings_privacy_policy)) },
                navigationIcon = { VouchBackButton(onClick = onNavigateBack) }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            AndroidView(
                factory = { viewContext ->
                    TextView(viewContext).apply {
                        textSize = 16f
                        setTextIsSelectable(true)
                        movementMethod = LinkMovementMethod.getInstance()
                    }
                },
                update = { view ->
                    view.text = policy
                    Linkify.addLinks(view, Linkify.EMAIL_ADDRESSES)
                    view.setTextColor(textColor)
                    view.setLinkTextColor(linkColor)
                }
            )
        }
    }
}
