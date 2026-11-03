package com.nool.app.ui.sidebar

import com.nool.app.data.index.NoteEntity
import java.io.File

data class TreeNode(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val children: MutableList<TreeNode> = mutableListOf(),
    val noteEntity: NoteEntity? = null
)

object FolderTreeBuilder {

    fun buildTree(notes: List<NoteEntity>, folders: List<String>): TreeNode {
        val root = TreeNode(
            name = "Vault",
            relativePath = "",
            isDirectory = true
        )

        val folderNodes = mutableMapOf<String, TreeNode>()
        folderNodes[""] = root

        // Ensure all folders exist in map
        val sortedFolders = folders.filter { it.isNotBlank() }.sortedBy { it.count { c -> c == '/' } }
        for (folder in sortedFolders) {
            val parts = folder.split("/")
            var currentPath = ""
            var parent = root
            for (part in parts) {
                currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
                val existing = folderNodes[currentPath]
                if (existing != null) {
                    parent = existing
                } else {
                    val newNode = TreeNode(
                        name = part,
                        relativePath = currentPath,
                        isDirectory = true
                    )
                    parent.children.add(newNode)
                    folderNodes[currentPath] = newNode
                    parent = newNode
                }
            }
        }

        // Add notes into corresponding folders
        for (note in notes) {
            val folderPath = note.folderPath.trim().removePrefix("/").removeSuffix("/")
            val parentNode = folderNodes[folderPath] ?: run {
                // If folder wasn't in explicit folders list, find or create
                root
            }
            parentNode.children.add(
                TreeNode(
                    name = note.title,
                    relativePath = note.relativePath,
                    isDirectory = false,
                    noteEntity = note
                )
            )
        }

        // Sort children: folders first alphabetically, then notes
        fun sortChildren(node: TreeNode) {
            node.children.sortWith(
                compareBy<TreeNode> { !it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
            for (child in node.children) {
                if (child.isDirectory) {
                    sortChildren(child)
                }
            }
        }

        sortChildren(root)
        return root
    }
}
