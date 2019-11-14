package com.breadwallet.ui.settings.nodeselector

data class NodeSelectorModel(
    val mode: Mode? = null,
    val currentNode: String = "",
    val connected: Boolean = false
) {
    enum class Mode { AUTOMATIC, MANUAL }

    companion object {
        fun createDefault() = NodeSelectorModel()
    }
}