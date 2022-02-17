package at.ac.tuwien.caa.docscan.db.model.state

const val UPLOAD_STATE_ID_UPLOAD_IN_PROGRESS = "UPLOAD_IN_PROGRESS"

enum class UploadState(val id: String) {

    /**
     * There is no upload state.
     */
    NONE("NONE"),

    /**
     * The entity is scheduled for upload and will began shortly or after some conditions are met.
     */
    SCHEDULED("SCHEDULED"),

    /**
     * The entity is being uploaded.
     */
    UPLOAD_IN_PROGRESS(UPLOAD_STATE_ID_UPLOAD_IN_PROGRESS),

    /**
     * The entity is (already) uploaded.
     */
    UPLOADED("UPLOADED");

    companion object {
        fun getUploadStateById(id: String?): UploadState {
            id ?: return NONE
            return values().firstOrNull { state ->
                state.id == id
            } ?: NONE
        }
    }
}
