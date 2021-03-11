package at.ac.tuwien.caa.docscan.koin

import android.os.Bundle
import at.ac.tuwien.caa.docscan.DocScanApp
import at.ac.tuwien.caa.docscan.ui.segmentation.SegmentationViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { androidApplication() as DocScanApp }
}

val viewModelModule = module {
    viewModel { (extras: Bundle) -> SegmentationViewModel(extras, get()) }
}
