package com.nool.core.model

data class TagTreeNode(
    val name: String,
    val fullPath: String,
    val directCount: Int = 0,
    val totalCount: Int = 0,
    val children: List<TagTreeNode> = emptyList()
)

object TagHierarchyBuilder {

    /**
     * Builds a tree of [TagTreeNode] from a map of tag full paths (e.g. "work/project/alpha") to their occurrence count.
     */
    fun buildTree(tagCounts: Map<String, Int>): List<TagTreeNode> {
        if (tagCounts.isEmpty()) return emptyList()

        class MutableNode(
            val name: String,
            val fullPath: String,
            var directCount: Int = 0,
            val childrenMap: MutableMap<String, MutableNode> = sortedMapOf()
        ) {
            fun toImmutable(): TagTreeNode {
                val childNodes = childrenMap.values.map { it.toImmutable() }
                val total = directCount + childNodes.sumOf { it.totalCount }
                return TagTreeNode(
                    name = name,
                    fullPath = fullPath,
                    directCount = directCount,
                    totalCount = total,
                    children = childNodes
                )
            }
        }

        val rootNodes = sortedMapOf<String, MutableNode>()

        for ((tag, count) in tagCounts) {
            val cleanTag = tag.trim().removePrefix("#").trim()
            if (cleanTag.isEmpty()) continue

            val segments = cleanTag.split("/").map { it.trim() }.filter { it.isNotEmpty() }
            if (segments.isEmpty()) continue

            val firstSeg = segments[0]
            val rootNode = rootNodes.getOrPut(firstSeg) {
                MutableNode(name = firstSeg, fullPath = firstSeg)
            }

            var curr = rootNode
            var currPath = firstSeg

            for (i in 1 until segments.size) {
                val seg = segments[i]
                currPath = "$currPath/$seg"
                curr = curr.childrenMap.getOrPut(seg) {
                    MutableNode(name = seg, fullPath = currPath)
                }
            }

            curr.directCount += count
        }

        return rootNodes.values.map { it.toImmutable() }
    }
}
