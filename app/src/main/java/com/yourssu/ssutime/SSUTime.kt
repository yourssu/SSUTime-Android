package com.yourssu.ssutime

import com.yourssu.ssutime.screen.login.LoginViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val appModule = module {
    viewModel<LoginViewModel>()
}