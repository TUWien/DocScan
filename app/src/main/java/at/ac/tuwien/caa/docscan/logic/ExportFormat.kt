package at.ac.tuwien.caa.docscan.logic

enum class ExportFormat(val id: String) {
    PDF("PDF"),
    PDF_WITH_OCR("PDF_WITH_OCR");

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
