package com.nool.core.model

data class GraphNode(
    val id: String, // relativePath
    val label: String, // title
    val folder: String = "",
    val tags: List<String> = emptyList(),
    val noteType: String = "Note",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val degree: Int = 0,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f
)

data class GraphEdge(
    val source: String, // source relativePath
    val target: String, // target relativePath or targetTitle
    val isEmbed: Boolean = false,
    val isResolved: Boolean = true
)

data class GraphData(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList()
)

data class GraphFilter(
    val searchQuery: String = "",
    val selectedTag: String? = null,
    val selectedType: String? = null,
    val showOrphansOnly: Boolean = false,
    val localNotePath: String? = null,
    val localDepth: Int = 1
)
