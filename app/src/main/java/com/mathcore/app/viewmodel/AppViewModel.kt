package com.mathcore.app.viewmodel

import androidx.lifecycle.ViewModel
import com.mathcore.app.data.QuestionRepository
import com.mathcore.app.data.local.PreferencesManager
import com.mathcore.app.data.repository.AuthRepository
import com.mathcore.app.data.repository.MistakesRepository
import com.mathcore.app.data.repository.ResultsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * [А-1] Shared ViewModel that exposes singleton app-level dependencies to
 * the Compose UI without requiring Activity-scoped creation or passing
 * them down through function parameters.
 *
 * Lives in the HiltViewModelFactory scope — one instance per Activity lifecycle.
 * All properties are themselves singletons (from AppModule) so no extra memory is used.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    val prefs:        PreferencesManager,
    val questionRepo: QuestionRepository,
    val resultsRepo:  ResultsRepository,
    val mistakesRepo: MistakesRepository,
    val authRepo:     AuthRepository
) : ViewModel()
