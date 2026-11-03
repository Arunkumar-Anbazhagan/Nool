package com.nool.app.data.index

import com.nool.core.model.NoteTitleAndPath as ModelNoteTitleAndPath

object LinkResolver {
    fun resolveLink(
        rawTarget: String,
        sourceNotePath: String,
        allNotes: List<NoteTitleAndPath>
    ): String? {
        val modelNotes = allNotes.map { ModelNoteTitleAndPath(it.title, it.relativePath, it.aliases) }
        return com.nool.core.index.LinkResolver.resolveLink(rawTarget, sourceNotePath, modelNotes)
    }
}
