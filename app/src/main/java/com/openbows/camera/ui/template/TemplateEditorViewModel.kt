/*
 * Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)
 * Openbows-camera is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at: http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.openbows.camera.ui.template

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openbows.camera.OpenbowsApplication
import com.openbows.camera.data.CoordSystem
import com.openbows.camera.data.FontChoice
import com.openbows.camera.data.FontSizeChoice
import com.openbows.camera.data.WatermarkTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplateEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as OpenbowsApplication
    private val repo = app.container.templateRepository

    private val _template = MutableStateFlow<WatermarkTemplate?>(null)
    val template: StateFlow<WatermarkTemplate?> = _template

    val coordSystem: StateFlow<CoordSystem> = app.container.preferences.coordSystemFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoordSystem.WGS84)

    fun load(id: Long) {
        viewModelScope.launch {
            _template.value = repo.get(id)
        }
    }

    fun update(block: (WatermarkTemplate) -> WatermarkTemplate) {
        val current = _template.value ?: return
        val next = block(current)
        _template.value = next
        viewModelScope.launch { repo.save(next) }
    }

    fun setFont(choice: FontChoice) = update { it.copy(fontChoice = choice) }
    fun setSize(size: FontSizeChoice) = update { it.copy(fontSize = size) }
    fun setColor(argb: Int) = update { it.copy(fontColorArgb = argb) }
    fun setBgColor(argb: Int) = update { it.copy(bgColorArgb = argb) }
}
