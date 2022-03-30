package at.ac.tuwien.caa.docscan.logic.notification

enum class DocumentNotificationType {
    EXPORT,
    UPLOAD;

    fun getChannel(): DocScanNotificationChannel {
        return when (this) {
            EXPORT -> DocScanNotificationChannel.CHANNEL_EXPORT
            UPLOAD -> DocScanNotificationChannel.CHANNEL_UPLOAD
        }
    }
}