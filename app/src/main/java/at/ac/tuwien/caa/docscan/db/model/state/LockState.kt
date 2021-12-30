package at.ac.tuwien.caa.docscan.db.model.state

/**
 * The locked state describes restricted writing/editing access to [Document]s and [Page]s.
 *
 * E.g. if an upload or export is performed, the file states should be preserved, otherwise
 * it could easily happen, that while a large document upload is ongoing, someone meanwhile
 * deletes/modifies or adds a new page.
 */
enum class LockState(val id: String) {
    /**
     * Neither a document, nor any of the pages are locked. They can be modified.
     */
    NONE("NONE"),

    /**
     * Some of the document pages are locked, but not the entire document itself, i.e. a page
     * could be added to the document, while another page is being processed.
     */
    PARTIAL_LOCK("PARTIAL_LOCK"),

    /**
     * The entire document, with all of if its pages is locked, it cannot be modified, neither
     * any of its data.
     */
    FULL_LOCK("FULL_LOCK");

    fun isLocked(): Boolean {
        return when (this) {
            NONE -> false
            PARTIAL_LOCK, FULL_LOCK -> true
        }
    }

    companion object {
        fun getLockStateById(id: String?): LockState {
            id ?: return NONE
            values().forEach { value ->
                if (value.id == id) {
                    return value
                }
            }
            return NONE
        }
    }
}
