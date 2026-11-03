package com.nool.app.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nool.core.model.GraphData
import com.nool.core.model.GraphFilter
import com.nool.core.model.GraphNode
import com.nool.app.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GraphUiState(
    val isLoading: Boolean = true,
    val graphData: GraphData = GraphData(),
    val rawGraphData: GraphData = GraphData(),
    val filter: GraphFilter = GraphFilter(),
    val selectedNode: GraphNode? = null,
    val availableTags: List<String> = emptyList(),
    val isLocalGraph: Boolean = false,
    val localDepth: Int = 1
)

class GraphViewModel(
    private val repository: VaultRepository,
    private val initialNotePath: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GraphUiState(
            isLocalGraph = initialNotePath != null,
            filter = GraphFilter(localNotePath = initialNotePath)
        )
    )
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    private val cachedPositions = mutableMapOf<String, Pair<Float, Float>>()

    init {
        loadGraph()
    }

    fun loadGraph(notePath: String? = initialNotePath) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getGraphData(notePath, _uiState.value.localDepth)
                .onSuccess { rawData ->
                    // Remember old positions for stability
                    for (node in _uiState.value.graphData.nodes) {
                        cachedPositions[node.id] = node.x to node.y
                    }

                    // Layout simulation
                    val simulated = GraphLayoutEngine.computeLayout(
                        data = rawData,
                        iterations = 100,
                        cachedPositions = cachedPositions
                    )

                    // Update positions cache
                    for (node in simulated.nodes) {
                        cachedPositions[node.id] = node.x to node.y
                    }

                    val tags = rawData.nodes.flatMap { it.tags }.distinct().sorted()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            rawGraphData = simulated,
                            graphData = applyFilters(simulated, it.filter),
                            availableTags = tags,
                            selectedNode = simulated.nodes.find { n -> n.id == notePath }
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun setSearchQuery(query: String) {
        val newFilter = _uiState.value.filter.copy(searchQuery = query)
        _uiState.update {
            it.copy(
                filter = newFilter,
                graphData = applyFilters(it.rawGraphData, newFilter)
            )
        }
    }

    fun setSelectedTag(tag: String?) {
        val nextTag = if (_uiState.value.filter.selectedTag == tag) null else tag
        val newFilter = _uiState.value.filter.copy(selectedTag = nextTag)
        _uiState.update {
            it.copy(
                filter = newFilter,
                graphData = applyFilters(it.rawGraphData, newFilter)
            )
        }
    }

    fun toggleOrphansOnly() {
        val next = !_uiState.value.filter.showOrphansOnly
        val newFilter = _uiState.value.filter.copy(showOrphansOnly = next)
        _uiState.update {
            it.copy(
                filter = newFilter,
                graphData = applyFilters(it.rawGraphData, newFilter)
            )
        }
    }

    fun setLocalDepth(depth: Int) {
        val safeDepth = depth.coerceIn(1, 3)
        _uiState.update { it.copy(localDepth = safeDepth) }
        loadGraph(_uiState.value.filter.localNotePath)
    }

    fun selectNode(node: GraphNode?) {
        _uiState.update { it.copy(selectedNode = node) }
    }

    private fun applyFilters(raw: GraphData, filter: GraphFilter): GraphData {
        var filteredNodes = raw.nodes

        if (filter.showOrphansOnly) {
            filteredNodes = filteredNodes.filter { it.degree == 0 }
        }

        if (!filter.selectedTag.isNullOrBlank()) {
            filteredNodes = filteredNodes.filter { it.tags.contains(filter.selectedTag) }
        }

        val nodeIds = filteredNodes.map { it.id }.toSet()
        val filteredEdges = raw.edges.filter { it.source in nodeIds && it.target in nodeIds }

        return GraphData(nodes = filteredNodes, edges = filteredEdges)
    }
}
