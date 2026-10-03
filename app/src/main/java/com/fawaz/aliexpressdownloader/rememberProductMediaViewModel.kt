package com.fawaz.aliexpressdownloader

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fawaz.aliexpressdownloader.ui.viewmodel.ProductMediaViewModel

@Composable
fun rememberProductMediaViewModel(): ProductMediaViewModel {
    val context = LocalContext.current
    return viewModel(factory = ProductMediaViewModel.Factory(context))
}
