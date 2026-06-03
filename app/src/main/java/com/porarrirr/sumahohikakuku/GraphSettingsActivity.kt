package com.porarrirr.sumahohikakuku

import android.content.Context
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
    private var appliedLanguageTag: String = ""

    override fun attachBaseContext(newBase: Context) {
        appliedLanguageTag = AppLanguageController.getLanguageTag(newBase)
        super.attachBaseContext(AppLanguageController.wrap(newBase))
    }

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

    override fun onResume() {
        super.onResume()
        if (AppLanguageController.getLanguageTag(this) != appliedLanguageTag) {
            recreate()
        }
    }
}
