package com.nool.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TagHierarchyTest {

    @Test
    fun testBuildTreeWithNestedTags() {
        val input = mapOf(
            "work" to 3,
            "work/project" to 2,
            "work/project/alpha" to 5,
            "personal/finance" to 4,
            "life" to 1
        )

        val tree = TagHierarchyBuilder.buildTree(input)
        assertEquals(3, tree.size) // life, personal, work

        // Check "life"
        val life = tree.first { it.name == "life" }
        assertEquals("life", life.fullPath)
        assertEquals(1, life.directCount)
        assertEquals(1, life.totalCount)
        assertTrue(life.children.isEmpty())

        // Check "personal"
        val personal = tree.first { it.name == "personal" }
        assertEquals(0, personal.directCount)
        assertEquals(4, personal.totalCount)
        assertEquals(1, personal.children.size)
        val finance = personal.children[0]
        assertEquals("finance", finance.name)
        assertEquals("personal/finance", finance.fullPath)
        assertEquals(4, finance.directCount)
        assertEquals(4, finance.totalCount)

        // Check "work"
        val work = tree.first { it.name == "work" }
        assertEquals(3, work.directCount)
        assertEquals(10, work.totalCount) // 3 + 2 + 5
        assertEquals(1, work.children.size)
        val proj = work.children[0]
        assertEquals("project", proj.name)
        assertEquals("work/project", proj.fullPath)
        assertEquals(2, proj.directCount)
        assertEquals(7, proj.totalCount) // 2 + 5
        val alpha = proj.children[0]
        assertEquals("alpha", alpha.name)
        assertEquals("work/project/alpha", alpha.fullPath)
        assertEquals(5, alpha.directCount)
        assertEquals(5, alpha.totalCount)
    }
}
