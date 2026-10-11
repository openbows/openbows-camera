/*
 * Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)
 * Openbows-camera is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at: http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.openbows.camera.data

class RemarkHistoryRepository(
    private val dao: RemarkHistoryDao,
) {
    val history = dao.observeAll()

    suspend fun remember(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val existing = dao.findByText(trimmed)
        if (existing != null) {
            dao.update(existing.copy(updatedAt = System.currentTimeMillis()))
        } else {
            dao.insert(RemarkHistory(text = trimmed))
        }
    }

    suspend fun delete(item: RemarkHistory) {
        dao.delete(item)
    }
}
