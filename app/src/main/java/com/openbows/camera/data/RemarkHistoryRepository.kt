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
