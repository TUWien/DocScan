package at.ac.tuwien.caa.docscan.koin

import android.os.Bundle
import androidx.work.WorkManager
import at.ac.tuwien.caa.docscan.BuildConfig
import at.ac.tuwien.caa.docscan.DocScanApp
import at.ac.tuwien.caa.docscan.db.AppDatabase
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import at.ac.tuwien.caa.docscan.repository.migration.MigrationRepository
import at.ac.tuwien.caa.docscan.api.transkribus.TranskribusAPIService
import at.ac.tuwien.caa.docscan.api.transkribus.TranskribusHeaderInterceptor
import at.ac.tuwien.caa.docscan.logic.notification.NotificationHandler
import at.ac.tuwien.caa.docscan.repository.*
import at.ac.tuwien.caa.docscan.ui.base.UserViewModel
import at.ac.tuwien.caa.docscan.ui.camera.CameraViewModel
import at.ac.tuwien.caa.docscan.ui.crop.CropViewModel
import at.ac.tuwien.caa.docscan.ui.dialog.DialogViewModel
import at.ac.tuwien.caa.docscan.ui.dialog.ModalActionSheetViewModel
import at.ac.tuwien.caa.docscan.ui.document.CreateDocumentViewModel
import at.ac.tuwien.caa.docscan.ui.document.EditDocumentViewModel
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerViewModel
import at.ac.tuwien.caa.docscan.ui.docviewer.documents.DocumentsViewModel
import at.ac.tuwien.caa.docscan.ui.docviewer.images.ImagesViewModel
import at.ac.tuwien.caa.docscan.ui.gallery.ImageViewModel
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideViewModel
import at.ac.tuwien.caa.docscan.ui.segmentation.SegmentationViewModel
import at.ac.tuwien.caa.docscan.ui.start.StartViewModel
import at.ac.tuwien.caa.docscan.ui.account.LoginViewModel
import at.ac.tuwien.caa.docscan.ui.account.logout.LogoutViewModel
import at.ac.tuwien.caa.docscan.ui.docviewer.documents.selector.SelectDocumentViewModel
import at.ac.tuwien.caa.docscan.ui.docviewer.pdf.PdfViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
    single { androidApplication() as DocScanApp }
    single { AppDatabase.buildDatabase(get()) }
    single { PreferencesHandler(get()) }
    single { FileHandler(get()) }
    single { MigrationRepository(get(), get(), get()) }
    single { ImageProcessorRepository(get(), get(), get(), get()) }
    single { WorkManager.getInstance(get()) }
    single { NotificationHandler(get()) }
}

val daoModule = module {
    single { (get() as AppDatabase).documentDao() }
    single { (get() as AppDatabase).pageDao() }
    single { (get() as AppDatabase).userDao() }
    single { (get() as AppDatabase).exportFileDao() }
}

val viewModelModule = module {
    viewModel { (extras: Bundle) -> SegmentationViewModel(extras, get(), get()) }
    viewModel { StartViewModel(get(), get(), get()) }
    viewModel { CameraViewModel(get(), get(), get()) }
    viewModel { DocumentsViewModel(get()) }
    viewModel { DocumentViewerViewModel(get(), get()) }
    viewModel { CreateDocumentViewModel(get()) }
    viewModel { (extras: Bundle) -> EditDocumentViewModel(extras, get()) }
    viewModel { (extras: Bundle) -> PageSlideViewModel(extras, get(), get()) }
    viewModel { (extras: Bundle) -> ImageViewModel(extras, get()) }
    viewModel { (extras: Bundle) -> CropViewModel(extras, get(), get(), get()) }
    viewModel { ImagesViewModel(get()) }
    viewModel { DialogViewModel() }
    viewModel { ModalActionSheetViewModel() }
    viewModel { LoginViewModel(get()) }
    viewModel { UserViewModel(get()) }
    viewModel { LogoutViewModel(get()) }
    viewModel { SelectDocumentViewModel(get()) }
    viewModel { PdfViewModel(get(), get(), get()) }
}

val repositoryModule = module {
    single { DocumentRepository(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { UserRepository(get(), get(), get(), get(), get(), get()) }
    single { UploadRepository(get(), get(), get(), get(), get()) }
    single { ExportRepository(get(), get(), get(), get(), get(), get()) }
    single { ExportFileRepository(get()) }
}

val networkModule = module {
    single {
        GsonBuilder()
            .setLenient()
            .create()
    }
    single {
        TranskribusHeaderInterceptor(get())
    }
    single {
        provideOkHttp(get())
    }
    single {
        provideService(
            TranskribusAPIService.BASE_URL,
            get(),
            get(),
            TranskribusAPIService::class.java
        )
    }
}

private fun provideOkHttp(
    transkribusHeaderInterceptor: TranskribusHeaderInterceptor
): OkHttpClient {
    val okHttpBuilder = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
    if (BuildConfig.DEBUG) {
        okHttpBuilder.addNetworkInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
    }
    okHttpBuilder.addInterceptor(transkribusHeaderInterceptor)
    return okHttpBuilder.build()
}

private fun <T> provideService(
    @Suppress("SameParameterValue") baseUrl: String,
    gson: Gson,
    okHttpClient: OkHttpClient,
    type: Class<out T>
): T {
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient)
        .build()
        .create(type)
}
