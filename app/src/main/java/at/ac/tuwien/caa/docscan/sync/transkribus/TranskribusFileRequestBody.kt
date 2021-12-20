package at.ac.tuwien.caa.docscan.sync.transkribus

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.InputStream

open class TranskribusFileRequestBody(private val file: File) : RequestBody() {

    override fun contentType(): MediaType = "application/octet-stream".toMediaType()

    override fun writeTo(sink: BufferedSink) {
        file.inputStream().writeToBufferSink(sink)
    }

    private fun InputStream.writeToBufferSink(sink: BufferedSink) {
        use {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            var totalRead = 0
            while (read(buffer).also { read = it; totalRead += it } != -1) {
                sink.write(buffer, 0, read)
            }
        }
    }
}
