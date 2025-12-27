package com.porarrirr.sumahohikakuku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.porarrirr.sumahohikakuku.ui.GraphSettingsScreen
import com.porarrirr.sumahohikakuku.ui.theme.SumahohikakukuTheme

class GraphSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SumahohikakukuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GraphSettingsScreen(onBack = ::finish)
                }
            }
        }
    }
}

