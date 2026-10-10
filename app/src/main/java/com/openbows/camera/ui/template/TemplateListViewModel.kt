package com.openbows.camera.ui.template

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openbows.camera.OpenbowsApplication
import com.openbows.camera.data.CoordSystem
import com.openbows.camera.data.WatermarkTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplateListViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as OpenbowsApplication
    private val repo = app.container.templateRepository
    private val prefs = app.container.preferences

    val templates: StateFlow<List<WatermarkTemplate>> = repo.templates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val current: StateFlow<WatermarkTemplate?> = repo.currentTemplate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val coordSystem: StateFlow<CoordSystem> = prefs.coordSystemFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoordSystem.WGS84)

    fun setCoordSystem(system: CoordSystem) = viewModelScope.launch { prefs.setCoordSystem(system) }

    fun select(id: Long) = viewModelScope.launch { repo.setCurrent(id) }

    fun create(onCreated: (Long) -> Unit) = viewModelScope.launch {
        onCreated(repo.create())
    }

    fun duplicate(template: WatermarkTemplate) = viewModelScope.launch {
        repo.duplicate(template)
    }

    fun delete(template: WatermarkTemplate) = viewModelScope.launch {
        repo.delete(template)
    }

    fun rename(template: WatermarkTemplate, name: String) = viewModelScope.launch {
        repo.save(template.copy(name = name.trim().ifEmpty { template.name }))
    }
}
