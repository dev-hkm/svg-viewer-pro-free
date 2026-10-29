package com.khanhminh.svgviewer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.khanhminh.svgviewer.data.IncomingSvg
import com.khanhminh.svgviewer.ui.SvgViewerApp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val incoming = MutableStateFlow<IncomingSvg?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incoming.value = IncomingSvg.fromIntent(intent)

        setContent {
            val payload by incoming.collectAsState()
            SvgViewerApp(incoming = payload)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incoming.value = IncomingSvg.fromIntent(intent)
    }
}
