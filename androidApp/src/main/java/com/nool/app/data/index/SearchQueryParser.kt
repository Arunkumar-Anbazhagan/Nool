package com.nool.app.data.index

import com.nool.core.model.IndexedNote

typealias SearchQuery = com.nool.core.index.SearchQuery

object SearchQueryParser {
    fun parse(query: String): SearchQuery = com.nool.core.index.SearchQueryParser.parse(query)

    fun matches(note: IndexedNote, query: SearchQuery): Boolean =
        com.nool.core.index.SearchQueryParser.matches(note, query)

    fun matches(note: NoteEntity, query: SearchQuery): Boolean =
        com.nool.core.index.SearchQueryParser.matches(note.toIndexedNote(), query)
}
