package com.yourssu.ssutime

import com.yourssu.ssutime.screen.login.LoginViewModel
import com.yourssu.ssutime.screen.onboarding.OnBoardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

const val CHANNEL_ID = "ASSIGNMENT"
val appModule = module {
    viewModel<LoginViewModel>()
    viewModel<OnBoardingViewModel>()
}

// Compose Preview를 위한 koinModule
val previewModule = module {
    viewModel {
        LoginViewModel()
    }
}