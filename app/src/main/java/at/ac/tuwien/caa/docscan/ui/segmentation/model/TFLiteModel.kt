package at.ac.tuwien.caa.docscan.ui.segmentation.model

import androidx.annotation.Keep
import at.ac.tuwien.caa.docscan.logic.FileHandler
import com.google.gson.annotations.SerializedName
import java.io.File

/**
 * @author matejbartalsky
 */
@Keep
data class TFLiteModel(
    @SerializedName("vsc_job_id")
    val vscJobId: Int,
    @SerializedName("image_size")
    val inputSize: Int,
    @SerializedName("tflite_model_file_name")
    val modelFileName: String
) {

    /**
     * A readable representation of the model.
     */
    val title: String
        get() {
            return "${vscJobId}_${inputSize}"
        }

    /**
     * @return [modelFileName] with the full relative path to the asset folder.
     */
    fun getRelativeAssetFolderPath(): String {
        return FileHandler.ASSET_FOLDER_SEGMENTATION_MODELS + File.separator + modelFileName
    }
}
