package at.ac.tuwien.caa.docscan.logic

enum class ExportFormat(val id: String) {
    ZIP("ZIP"),
    PDF("PDF"),
    PDF_WITH_OCR("PDF_WITH_OCR");

    fun getFileType(): PageFileType {
        return when (this) {
            ZIP -> PageFileType.ZIP
            PDF, PDF_WITH_OCR -> PageFileType.PDF
        }
    }

    companion object {
        fun getExportFormatById(id: String?): ExportFormat {
            values().forEach { format ->
                if (format.id == id) {
                    return format
                }
            }
            return PDF
        }
    }
}
