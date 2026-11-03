package com.nool.core.graph

import com.nool.core.model.GraphData
import com.nool.core.model.GraphEdge
import com.nool.core.model.GraphNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object GraphLayoutEngine {

    private const val REPULSION_FORCE = 12000f
    private const val SPRING_LENGTH = 140f
    private const val SPRING_FORCE = 0.05f
    private const val CENTER_GRAVITY = 0.015f
    private const val DAMPING = 0.85f
    private const val MAX_VELOCITY = 25f

    /**
     * Runs force-directed physics iterations off the main thread.
     * Keeps previous node positions if present to maintain layout stability between opens.
     */
    suspend fun computeLayout(
        data: GraphData,
        iterations: Int = 100,
        cachedPositions: Map<String, Pair<Float, Float>> = emptyMap()
    ): GraphData = withContext(Dispatchers.Default) {
        if (data.nodes.isEmpty()) return@withContext data

        val nodes = data.nodes.map { node ->
            val cached = cachedPositions[node.id]
            if (cached != null) {
                node.copy(x = cached.first, y = cached.second, vx = 0f, vy = 0f)
            } else {
                // Seed on a circle with pseudo-random jitter based on node id
                val rng = Random(node.id.hashCode().toLong())
                val angle = rng.nextFloat() * 2f * 3.1415926535f
                val radius = 200f + rng.nextFloat() * 400f
                node.copy(
                    x = cos(angle) * radius,
                    y = sin(angle) * radius,
                    vx = 0f,
                    vy = 0f
                )
            }
        }.toMutableList()

        val nodeIndexMap = nodes.mapIndexed { index, node -> node.id to index }.toMap()
        val edgeList = data.edges.mapNotNull { edge ->
            val sIdx = nodeIndexMap[edge.source]
            val tIdx = nodeIndexMap[edge.target]
            if (sIdx != null && tIdx != null && sIdx != tIdx) {
                sIdx to tIdx
            } else null
        }

        // Run simulation steps
        for (step in 0 until iterations) {
            val stepCooling = (1f - (step.toFloat() / iterations.toFloat())).coerceIn(0.1f, 1f)

            // 1. Repulsion between all node pairs (Coulomb force)
            for (i in 0 until nodes.size) {
                val nodeA = nodes[i]
                for (j in i + 1 until nodes.size) {
                    val nodeB = nodes[j]
                    val dx = nodeA.x - nodeB.x
                    val dy = nodeA.y - nodeB.y
                    val distSq = max(dx * dx + dy * dy, 400f) // Prevent division by near-zero
                    val dist = sqrt(distSq)

                    val force = (REPULSION_FORCE / distSq) * stepCooling
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force

                    nodeA.vx += fx
                    nodeA.vy += fy
                    nodeB.vx -= fx
                    nodeB.vy -= fy
                }
            }

            // 2. Attraction along edges (Hooke's spring force)
            for ((sIdx, tIdx) in edgeList) {
                val nodeA = nodes[sIdx]
                val nodeB = nodes[tIdx]

                val dx = nodeB.x - nodeA.x
                val dy = nodeB.y - nodeA.y
                val dist = max(sqrt(dx * dx + dy * dy), 1f)
                val displacement = dist - SPRING_LENGTH

                val force = displacement * SPRING_FORCE * stepCooling
                val fx = (dx / dist) * force
                val fy = (dy / dist) * force

                nodeA.vx += fx
                nodeA.vy += fy
                nodeB.vx -= fx
                nodeB.vy -= fy
            }

            // 3. Central gravity and position integration
            for (node in nodes) {
                // Gravity towards origin (0, 0)
                node.vx -= node.x * CENTER_GRAVITY * stepCooling
                node.vy -= node.y * CENTER_GRAVITY * stepCooling

                // Apply damping
                node.vx *= DAMPING
                node.vy *= DAMPING

                // Clamp maximum step velocity to prevent unstable physics explosion
                val speed = sqrt(node.vx * node.vx + node.vy * node.vy)
                if (speed > MAX_VELOCITY) {
                    node.vx = (node.vx / speed) * MAX_VELOCITY
                    node.vy = (node.vy / speed) * MAX_VELOCITY
                }

                // Update positions
                node.x += node.vx
                node.y += node.vy
            }
        }

        // Return updated GraphData
        data.copy(nodes = nodes)
    }
}
