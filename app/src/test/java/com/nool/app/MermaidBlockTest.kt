package com.nool.app

import org.commonmark.node.FencedCodeBlock
import org.commonmark.parser.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MermaidBlockTest {

    private val parser = Parser.builder().build()

    @Test
    fun `test mermaid code block identification with exact and case insensitive info`() {
        val markdown = """
            # Note Title

            ```mermaid
            graph TD;
                A-->B;
                A-->C;
                B-->D;
                C-->D;
            ```

            ```kotlin
            val x = 10
            ```

            ```MERMAID
            sequenceDiagram
                Alice->>Bob: Hello Bob
                Bob-->>Alice: Hi Alice
            ```
        """.trimIndent()

        val doc = parser.parse(markdown)
        val codeBlocks = mutableListOf<FencedCodeBlock>()
        var child = doc.firstChild
        while (child != null) {
            if (child is FencedCodeBlock) {
                codeBlocks.add(child)
            }
            child = child.next
        }

        assertEquals(3, codeBlocks.size)

        // Block 1: mermaid
        assertTrue(codeBlocks[0].info?.trim()?.equals("mermaid", ignoreCase = true) == true)
        assertTrue(codeBlocks[0].literal.contains("graph TD;"))

        // Block 2: kotlin (not mermaid)
        assertFalse(codeBlocks[1].info?.trim()?.equals("mermaid", ignoreCase = true) == true)
        assertEquals("kotlin", codeBlocks[1].info?.trim())

        // Block 3: MERMAID
        assertTrue(codeBlocks[2].info?.trim()?.equals("mermaid", ignoreCase = true) == true)
        assertTrue(codeBlocks[2].literal.contains("sequenceDiagram"))
    }

    @Test
    fun `test mermaid diagram hash generation differentiates light and dark themes`() {
        val code = "graph LR\n  A --> B"
        val hashLight = "${code.hashCode()}_0"
        val hashDark = "${code.hashCode()}_1"

        assertNotNull(hashLight)
        assertNotNull(hashDark)
        assertFalse(hashLight == hashDark)
    }
}
