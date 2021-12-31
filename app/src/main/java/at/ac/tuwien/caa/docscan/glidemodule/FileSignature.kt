package at.ac.tuwien.caa.docscan.glidemodule

import com.bumptech.glide.load.Key
import java.security.MessageDigest

/**
 * Represents a file signature for Glide.
 */
class FileSignature(val pageHash: String) : Key {

    override fun equals(other: Any?): Boolean {
        return other is String && other == pageHash
    }

    override fun hashCode(): Int {
        return pageHash.hashCode()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(pageHash.toByteArray())
    }
}
