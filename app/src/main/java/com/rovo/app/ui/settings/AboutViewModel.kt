package com.rovo.app.ui.settings

import androidx.lifecycle.ViewModel
import com.rovo.app.data.update.AppUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    val updateManager: AppUpdateManager
) : ViewModel()
