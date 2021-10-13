package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import android.content.SharedPreferences
import at.ac.tuwien.caa.docscan.BuildConfig
import at.ac.tuwien.caa.docscan.R

class PreferencesHandler(val context: Context) {
    private val preferencesName: String by lazy { "settings" }
    private val preferencesMode = Context.MODE_PRIVATE

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        preferencesName,
        preferencesMode
    )

    private val defaultSharedPreferences =
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

    private val KEY_FLASH_SERIES_MODE by lazy {
        context.getString(R.string.key_flash_series_mode)
    }

    private val KEY_EXIF_ARTIST by lazy {
        context.getString(R.string.key_exif_artist)
    }

    private val KEY_EXIF_COPYRIGHT by lazy {
        context.getString(R.string.key_exif_copyright)
    }

    // TODO: migrate and delete key "server_changed_shown_key"
    // TODO: migrate and delete key "SERIES_MODE_PAUSED_KEY" (since it's not used anymore)

    companion object {
        const val TEST_COLLECTION_ID_KEY = "TEST_COLLECTION_ID_KEY"
        const val COLLECTION_ID_KEY = "COLLECTION_ID_KEY"
        const val DOCUMENT_HINT_SHOWN_KEY = "DOCUMENT_HINT_SHOWN_KEY"
        const val INSTALLED_VERSION_KEY = "INSTALLED_VERSION_KEY"
        const val HIDE_SERIES_DIALOG_KEY = "HIDE_SERIES_DIALOG_KEY"
        const val SERIES_MODE_ACTIVE_KEY = "SERIES_MODE_ACTIVE_KEY"
        const val SHOW_INTRO_KEY = "SHOW_INTRO_KEY"
        val KEY_FIRST_START_DATE = "KEY_FIRST_START_DATE"
        const val SHOW_TRANSKRIBUS_METADATA_KEY = "SHOW_TRANSKRIBUS_METADATA"
        private const val KEY_SKIP_CROPPING_INFO_DIALOG = "KEY_SKIP_CROPPING_INFO_DIALOG"

        const val DB_MIGRATION_KEY = "DB_MIGRATION_KEY"

        const val KEY_DPI = "KEY_DPI"
        const val DEFAULT_INT_VALUE = -1
    }

    var shouldPerformDBMigration: Boolean
        get() =
            sharedPreferences.getBoolean(DB_MIGRATION_KEY, true)
        set(value) {
            sharedPreferences.edit()
                .putBoolean(DB_MIGRATION_KEY, value)
                .apply()
        }

    var testCollectionId: Int
        get() =
            sharedPreferences.getInt(TEST_COLLECTION_ID_KEY, DEFAULT_INT_VALUE)
        set(value) {
            sharedPreferences.edit()
                .putInt(TEST_COLLECTION_ID_KEY, value)
                .apply()
        }

    var cameraDPI: Int
        get() =
            defaultSharedPreferences.getInt(KEY_DPI, DEFAULT_INT_VALUE)
        set(value) {
            defaultSharedPreferences.edit()
                .putInt(KEY_DPI, value)
                .apply()
        }

    var collectionId: Int
        get() =
            sharedPreferences.getInt(COLLECTION_ID_KEY, DEFAULT_INT_VALUE)
        set(value) {
            sharedPreferences.edit()
                .putInt(COLLECTION_ID_KEY, value)
                .apply()
        }

    var hasShownDocumentHint: Boolean
        get() =
            sharedPreferences.getBoolean(DOCUMENT_HINT_SHOWN_KEY, false)
        set(value) {
            sharedPreferences.edit()
                .putBoolean(DOCUMENT_HINT_SHOWN_KEY, value)
                .apply()
        }

    var hideSeriesDialog: Boolean
        get() =
            sharedPreferences.getBoolean(HIDE_SERIES_DIALOG_KEY, false)
        set(value) {
            sharedPreferences.edit()
                .putBoolean(HIDE_SERIES_DIALOG_KEY, value)
                .apply()
        }

    var isSeriesModeActive: Boolean
        get() =
            sharedPreferences.getBoolean(SERIES_MODE_ACTIVE_KEY, false)
        set(value) {
            sharedPreferences.edit()
                .putBoolean(SERIES_MODE_ACTIVE_KEY, value)
                .apply()
        }

    var showIntro: Boolean
        get() =
            sharedPreferences.getBoolean(SHOW_INTRO_KEY, true)
        set(value) {
            sharedPreferences.edit()
                .putBoolean(SHOW_INTRO_KEY, value)
                .apply()
        }

    var installedVersionCode: Int
        get() =
            sharedPreferences.getInt(INSTALLED_VERSION_KEY, DEFAULT_INT_VALUE)
        set(value) {
            sharedPreferences.edit()
                .putInt(INSTALLED_VERSION_KEY, value)
                .apply()
        }

    var firstStartDate: String?
        get() =
            sharedPreferences.getString(KEY_FIRST_START_DATE, null)
        set(value) {
            sharedPreferences.edit()
                .putString(KEY_FIRST_START_DATE, value)
                .apply()
        }

    var exifArtist: String?
        get() =
            defaultSharedPreferences.getString(KEY_EXIF_ARTIST, null)
        set(value) {
            defaultSharedPreferences.edit()
                .putString(KEY_EXIF_ARTIST, value)
                .apply()
        }

    var exifCopyRight: String?
        get() =
            defaultSharedPreferences.getString(KEY_EXIF_COPYRIGHT, null)
        set(value) {
            defaultSharedPreferences.edit()
                .putString(KEY_EXIF_COPYRIGHT, value)
                .apply()
        }

    var isFlashSeriesMode: Boolean
        get() =
            defaultSharedPreferences.getBoolean(KEY_FLASH_SERIES_MODE, false)
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_FLASH_SERIES_MODE, value)
                .apply()
        }

    var showTranskribusMetaData: Boolean
        get() =
            defaultSharedPreferences.getBoolean(SHOW_TRANSKRIBUS_METADATA_KEY, false)
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(SHOW_TRANSKRIBUS_METADATA_KEY, value)
                .apply()
        }

    var showCroppingInfo: Boolean
        get() =
            defaultSharedPreferences.getBoolean(KEY_SKIP_CROPPING_INFO_DIALOG, true)
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_SKIP_CROPPING_INFO_DIALOG, value)
                .apply()
        }

    init {
        // check for migrations
        if (BuildConfig.VERSION_CODE > 156) {
//            showIntro = installedVersionCode == DEFAULT_INT_VALUE
            // TODO: migrate from default to settings preferences
//            firstStartDate = defaultSharedPreferences.getString(KEY_FIRST_START_DATE, null);
        }

        installedVersionCode = BuildConfig.VERSION_CODE
    }
}
