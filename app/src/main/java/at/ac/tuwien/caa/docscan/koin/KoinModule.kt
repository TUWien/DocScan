package at.ac.tuwien.caa.docscan.koin

import android.os.Bundle
import at.ac.tuwien.caa.docscan.DocScanApp
import at.ac.tuwien.caa.docscan.db.AppDatabase
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.repository.ImageProcessorRepository
import at.ac.tuwien.caa.docscan.repository.migration.MigrationRepository
import at.ac.tuwien.caa.docscan.ui.camera.CameraViewModel
import at.ac.tuwien.caa.docscan.ui.crop.CropViewModel
import at.ac.tuwien.caa.docscan.ui.dialog.DialogViewModel
import at.ac.tuwien.caa.docscan.ui.dialog.ModalActionSheetViewModel
import at.ac.tuwien.caa.docscan.ui.document.CreateDocumentViewModel
import at.ac.tuwien.caa.docscan.ui.document.EditDocumentViewModel
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerViewModel
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentsViewModel
import at.ac.tuwien.caa.docscan.ui.docviewer.ImagesViewModel
import at.ac.tuwien.caa.docscan.ui.gallery.newPackage.ImageViewModel
import at.ac.tuwien.caa.docscan.ui.gallery.newPackage.PageSlideViewModel
import at.ac.tuwien.caa.docscan.ui.segmentation.SegmentationViewModel
import at.ac.tuwien.caa.docscan.ui.start.StartActivityViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { androidApplication() as DocScanApp }
    single { AppDatabase.buildDatabase(get()) }
    single { PreferencesHandler(get()) }
    single { FileHandler(get()) }
    single { MigrationRepository(get(), get(), get()) }
    single { ImageProcessorRepository(get(), get()) }
}

val daoModule = module {
    single { (get() as AppDatabase).documentDao() }
    single { (get() as AppDatabase).pageDao() }
}

val viewModelModule = module {
    viewModel { (extras: Bundle) -> SegmentationViewModel(extras, get(), get()) }
    viewModel { StartActivityViewModel(get(), get(), get()) }
    viewModel { CameraViewModel(get(), get(), get()) }
    viewModel { DocumentsViewModel(get()) }
    viewModel { DocumentViewerViewModel(get()) }
    viewModel { CreateDocumentViewModel(get()) }
    viewModel { (extras: Bundle) -> EditDocumentViewModel(extras, get()) }
    viewModel { (extras: Bundle) -> PageSlideViewModel(extras, get(), get()) }
    viewModel { (extras: Bundle) -> ImageViewModel(extras, get()) }
    viewModel { (extras: Bundle) -> CropViewModel(extras, get(), get(), get(), get()) }
    viewModel { ImagesViewModel(get()) }
    viewModel { DialogViewModel() }
    viewModel { ModalActionSheetViewModel() }
}

val repositoryModule = module {
    single { DocumentRepository(get(), get(), get(), get(), get()) }
}
