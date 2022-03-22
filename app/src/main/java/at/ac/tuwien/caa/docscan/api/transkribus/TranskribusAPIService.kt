@file:Suppress("unused")

package at.ac.tuwien.caa.docscan.api.transkribus

import at.ac.tuwien.caa.docscan.BuildConfig
import at.ac.tuwien.caa.docscan.api.transkribus.TranskribusAPIService.Companion.TRANSKRIBUS_UPLOAD_COLLECTION_NAME
import at.ac.tuwien.caa.docscan.api.transkribus.model.collection.CollectionResponse
import at.ac.tuwien.caa.docscan.api.transkribus.model.collection.DocResponse
import at.ac.tuwien.caa.docscan.api.transkribus.model.login.LoginResponse
import at.ac.tuwien.caa.docscan.api.transkribus.model.uploads.CreateUploadRequestBody
import at.ac.tuwien.caa.docscan.api.transkribus.model.uploads.UploadStatusResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import java.io.File

/**
 * Represents all API calls for the Transkribus REST BE, a file can be uploaded as follows:
 *
 * Authentication: The user needs to authenticate to obtain a session cookie in order to authorize
 * for all further requests, the cookie is set in [TranskribusHeaderInterceptor].
 * Data structure: Transkribus has an own data structure, where a user can have multiple collections, every collection
 * may contain multiple documents and every document may contain multiple pages (files).
 *
 * To upload a file, the following steps needs to be considered:
 *
 * 1) collection: The client always uses the same collection with the name [TRANSKRIBUS_UPLOAD_COLLECTION_NAME], i.e.
 * if there is no collectionId available, then:
 *  - Pull all collections, find the occurrence with the highest collection id that has the name [TRANSKRIBUS_UPLOAD_COLLECTION_NAME].
 *  - If none collection is found, then create a new collection with that name.
 *  - Otherwise, take the id of that collection and store it into the prefs. This id shouldn't change, therefore
 *  for all subsequent uploads, the collection determination can be skipped.
 *
 *  2) document: Basically, for a every document on the client, a new document on the BE has to be created.
 *      - For creating a document, simply an upload has to be created. The uploadId is actually the documentId.
 *
 * From the client's perspective, only a single collection is used for the upload, which has the name
 *
 *
 *
 * @author matejbart
 */
interface TranskribusAPIService {
    companion object {
        const val BASE_URL = BuildConfig.TRANSKRIBUS_BASE_URL

        const val TRANSKRIBUS_UPLOAD_COLLECTION_NAME = "DocScan - Uploads"

        const val PARAM_USER = "user"
        const val PARAM_PW = "pw"

        const val PARAM_URL_UPLOAD_ID = "uploadId"
        const val PARAM_URL_COLLECTION_ID = "collectionId"
        const val PARAM_URL_DOC_ID = "docId"
    }

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field(PARAM_USER) user: String,
        @Field(PARAM_PW) pw: String
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST("auth/logout")
    suspend fun logout(): Response<Void>

    @POST("uploads")
    suspend fun createUpload(
        @Query("collId") collId: Int,
        @Body body: CreateUploadRequestBody
    ): Response<UploadStatusResponse>

    @PUT("uploads/{$PARAM_URL_UPLOAD_ID}")
    suspend fun uploadFile(
        @Path(PARAM_URL_UPLOAD_ID) uploadId: Int,
        @Body body: RequestBody
    ): Response<UploadStatusResponse>

    @GET("uploads/{$PARAM_URL_UPLOAD_ID}")
    suspend fun getUploadStatus(
        @Path(PARAM_URL_UPLOAD_ID) uploadId: Int
    ): Response<UploadStatusResponse>

    @GET("collections/list")
    suspend fun getCollections(): Response<List<CollectionResponse>>

    @GET("collections/{$PARAM_URL_COLLECTION_ID}/{$PARAM_URL_DOC_ID}/list")
    suspend fun getDocById(
        @Path(PARAM_URL_COLLECTION_ID) collectionId: Int,
        @Path(PARAM_URL_DOC_ID) docId: Int
    ): Response<DocResponse>

    @DELETE("uploads/{$PARAM_URL_DOC_ID}")
    suspend fun deleteUpload(
        @Path(PARAM_URL_DOC_ID) docId: Int
    ): Response<Void>

    @POST("collections/createCollection")
    @Headers("Accept: text/plain;charset=utf-8")
    suspend fun createCollection(@Query("collName") collectionName: String): Response<String>
}

fun mapToMultiPartBody(fileName: String, file: File): MultipartBody {
    return MultipartBody.Builder().apply {
        addFormDataPart("img", fileName, TranskribusFileRequestBody(file))
        setType("multipart/form-data".toMediaType())
    }.build()
}
