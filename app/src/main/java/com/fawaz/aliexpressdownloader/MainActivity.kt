package com.fawaz.aliexpressdownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fawaz.aliexpressdownloader.ui.ProductMediaScreen
import com.fawaz.aliexpressdownloader.ui.theme.AliExpressMediaDownloaderTheme
import com.fawaz.aliexpressdownloader.ui.viewmodel.ProductMediaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ProductMediaViewModel by viewModels {
        ProductMediaViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AliExpressMediaDownloaderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ProductMediaScreen(
                        viewModel = viewModel,
                        context = this
                    )
                }
            }
        }
    }
}
