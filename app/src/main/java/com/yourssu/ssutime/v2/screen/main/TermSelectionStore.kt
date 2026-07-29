package com.yourssu.ssutime.v2.screen.main

import io.github.chlwhdtn03.data.Lms.Term
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.ExperimentalTime

/**
 * Keeps a user-selected term only for the current app session.
 *
 * A null value means that the app should use its normal current-term behavior.
 * This state is intentionally not backed by DataStore or saved instance state,
 * and MainActivity clears it when a new app session starts.
 */
@OptIn(ExperimentalTime::class)
class TermSelectionStore {
    private val _selectedTerm = MutableStateFlow<Term?>(null)
    val selectedTerm: StateFlow<Term?> = _selectedTerm.asStateFlow()

    fun select(term: Term?) {
        _selectedTerm.value = term
    }

    fun clear() {
        _selectedTerm.value = null
    }
}
