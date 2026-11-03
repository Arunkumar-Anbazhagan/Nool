package com.nool.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GitFolderAndTaskToggleTest {

    private fun extractRepoName(url: String): String {
        val clean = url.trim().removeSuffix("/").removeSuffix(".git")
        val candidate = clean.substringAfterLast("/").substringAfterLast(":")
        return candidate.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifEmpty { "notes" }
    }

    @Test
    fun testExtractRepoNameFromVariousUrls() {
        assertEquals("my-repo", extractRepoName("https://github.com/user/my-repo.git"))
        assertEquals("my-repo", extractRepoName("https://github.com/user/my-repo/"))
        assertEquals("my-repo", extractRepoName("https://github.com/user/my-repo"))
        assertEquals("vault", extractRepoName("git@github.com:username/vault.git"))
        assertEquals("vault", extractRepoName("git@gitlab.com:org/subgroup/vault.git"))
        assertEquals("notes", extractRepoName(""))
    }

    @Test
    fun testTaskToggleLogic() {
        val original = """
            # Tasks
            - [ ] Buy groceries
            - [x] Read chapter 1
            * [ ] Call mom
        """.trimIndent()

        val lines = original.lines().toMutableList()

        // Toggle first task to checked
        val line0 = lines[1]
        val updated0 = line0.replaceFirst("- [ ] ", "- [x] ")
        lines[1] = updated0
        assertEquals("- [x] Buy groceries", lines[1])

        // Toggle second task to unchecked
        val line1 = lines[2]
        val updated1 = line1.replaceFirst("- [x] ", "- [ ] ")
        lines[2] = updated1
        assertEquals("- [ ] Read chapter 1", lines[2])

        // Toggle third task to checked
        val line2 = lines[3]
        val updated2 = line2.replaceFirst("* [ ] ", "* [x] ")
        lines[3] = updated2
        assertEquals("* [x] Call mom", lines[3])
    }

    @Test
    fun testAudioAttachmentEmbedRegex() {
        val text = "![[attachments/Audio_20261030_120000.m4a]]"
        val embedMatch = Regex("""^!\[\[([^\]\n]+)\]\]$""").matchEntire(text)
        assertTrue(embedMatch != null)
        val target = embedMatch!!.groupValues[1].trim()
        assertEquals("attachments/Audio_20261030_120000.m4a", target)
        assertTrue(target.endsWith(".m4a", true))
    }
}
