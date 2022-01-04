package at.ac.tuwien.caa.docscan.db.model.state

/**
 * Represents the [Page]'s post processing state.
 */
enum class PostProcessingState(val id: String) {
    /**
     * Page is drafted, not yet processed.
     */
    DRAFT("DRAFT"),

    /**
     * Page is being processed
     */
    PROCESSING("PROCESSING"),

    /**
     * Page processing is done
     */
    DONE("DONE");

    companion object {

        /**
         * @return [PostProcessingState] by id, fallbacks to [PostProcessingState.DRAFT]
         */
        fun getProcessingStateById(id: String?): PostProcessingState {
            id ?: return DRAFT
            return values().firstOrNull { state ->
                state.id == id
            } ?: DRAFT
        }
    }
}
