package com.nool.core.graph

import com.nool.core.model.GraphData
import com.nool.core.model.GraphEdge
import com.nool.core.model.GraphNode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraphLayoutEngineTest {

    @Test
    fun testEmptyGraph() = runTest {
        val result = GraphLayoutEngine.computeLayout(GraphData())
        assertTrue(result.nodes.isEmpty())
        assertTrue(result.edges.isEmpty())
    }

    @Test
    fun testTwoNodesSeparation() = runTest {
        val nodeA = GraphNode(id = "a.md", label = "A")
        val nodeB = GraphNode(id = "b.md", label = "B")
        val edge = GraphEdge(source = "a.md", target = "b.md")

        val data = GraphData(nodes = listOf(nodeA, nodeB), edges = listOf(edge))
        val result = GraphLayoutEngine.computeLayout(data, iterations = 50)

        assertEquals(2, result.nodes.size)
        val resA = result.nodes.find { it.id == "a.md" }!!
        val resB = result.nodes.find { it.id == "b.md" }!!

        assertFalse(resA.x.isNaN())
        assertFalse(resA.y.isNaN())
        assertFalse(resB.x.isNaN())
        assertFalse(resB.y.isNaN())

        // Distance should be reasonable and finite
        val dx = resA.x - resB.x
        val dy = resA.y - resB.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        assertTrue(dist > 10f)
    }
}
