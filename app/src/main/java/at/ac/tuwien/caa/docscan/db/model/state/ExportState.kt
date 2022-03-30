package at.ac.tuwien.caa.docscan.db.model.state

enum class ExportState(val id: String) {

    /**
     * The non
     */
    NONE("NONE"),

    /**
     * The page is being currently exported.
     */
    EXPORTING("EXPORTING"),

    /**
     * The page exporting is done.
     */
    DONE("DONE");

    companion object {
        fun getExportStateById(id: String?): ExportState {
            id ?: return NONE
            values().forEach { exportState ->
                if (exportState.id == id) {
                    return exportState
                }
            }
            return NONE
        }
    }
}
