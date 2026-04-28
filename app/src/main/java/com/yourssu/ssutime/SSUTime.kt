package com.yourssu.ssutime

import com.yourssu.ssutime.screen.login.LoginViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.plugin.module.dsl.*
import org.koin.dsl.module
val appModule = module {
    viewModel<LoginViewModel>()
}

// Compose Preview를 위한 koinModule
val previewModule = module {
    viewModel {
        LoginViewModel()
    }
}