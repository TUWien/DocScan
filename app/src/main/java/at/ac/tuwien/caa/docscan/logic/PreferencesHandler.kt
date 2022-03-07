package at.ac.tuwien.caa.docscan.logic

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import at.ac.tuwien.caa.docscan.BuildConfig
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.db.dao.UserDao
import at.ac.tuwien.caa.docscan.db.model.User
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity.IMG_ORIENTATION_90
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Represents all preference instances used in the app.
 * - [defaultSharedPreferences] which are the default preferences, mostly used in the preference views.
 * - [sharedPreferences] other settings related preferences.
 * - [encryptedPref] encrypted preferences for login credentials and session tokens.
 */
@Suppress("PrivatePropertyName")
class PreferencesHandler(val context: Context, private val userDao: UserDao) {
    private val preferencesName: String by lazy { "settings" }
    private val preferencesMode = Context.MODE_PRIVATE

    private val encryptedPref: SharedPreferences

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

    private val KEY_EXTENDED_DEBUG_ERROR_MESSAGES by lazy {
        context.getString(R.string.key_extended_debug_messages)
    }

    private val KEY_UPLOAD_MOBILE_DATA by lazy {
        context.getString(R.string.key_upload_mobile_data)
    }

    private val KEY_EXPORT_DIR by lazy {
        context.getString(R.string.key_pdf_dir)
    }

    private val KEY_HUD_ENABLED by lazy {
        context.getString(R.string.key_hud_enabled)
    }

    private val KEY_SHOW_DEBUG_VIEW by lazy {
        context.getString(R.string.key_show_debug_view)
    }

    private val KEY_TEXT_ORIENTATION by lazy {
        context.getString(R.string.key_text_orientation)
    }

    private val KEY_FOCUS_MEASURE by lazy {
        context.getString(R.string.key_focus_measure)
    }

    private val KEY_FAST_SEGMENTATION by lazy {
        context.getString(R.string.key_fast_segmentation)
    }

    private val KEY_SHOW_GRID by lazy {
        context.getString(R.string.key_show_grid)
    }

    private val KEY_SHOW_FOCUS_VALUES by lazy {
        context.getString(R.string.key_show_focus_values)
    }

    private val KEY_USE_TEST_SERVER by lazy {
        context.getString(R.string.key_use_test_server)
    }

    private val KEY_SEND_CRASH_REPORTS by lazy {
        context.getString(R.string.key_crash_reports)
    }

    private val KEY_GEO_TAGGING by lazy {
        context.getString(R.string.key_geo_tagging)
    }

    companion object {
        const val DEFAULT_INT_VALUE = -1

        private const val TEST_COLLECTION_ID_KEY = "TEST_COLLECTION_ID_KEY"
        private const val COLLECTION_ID_KEY = "COLLECTION_ID_KEY"
        private const val INSTALLED_VERSION_KEY = "INSTALLED_VERSION_KEY"
        private const val SERIES_MODE_ACTIVE_KEY = "SERIES_MODE_ACTIVE_KEY"
        private const val SHOW_INTRO_KEY = "SHOW_INTRO_KEY"
        const val KEY_FIRST_START_DATE = "KEY_FIRST_START_DATE"
        private const val SHOW_TRANSKRIBUS_METADATA_KEY = "SHOW_TRANSKRIBUS_METADATA"
        private const val KEY_SKIP_CROPPING_INFO_DIALOG = "KEY_SKIP_CROPPING_INFO_DIALOG"
        private const val KEY_SHOW_EXPOSURE_LOCK_WARNING = "KEY_SHOW_EXPOSURE_LOCK_WARNING"
        private const val KEY_DPI = "KEY_DPI"
        private const val KEY_DB_MIGRATION = "DB_MIGRATION"
        private const val KEY_TRANSKRIBUS_SESSION_COOKIE = "TRANSKRIBUS_SESSION_COOKIE"
        private const val KEY_TRANSKRIBUS_PASSWORD = "TRANSKRIBUS_PASSWORD"
        private const val KEY_MIGRATION_TO_PREFS_V1_80 = "MIGRATION_TO_PREFS_V1_80"

        // previous deprecated keys that are deleted in the migration
        private const val DEPRECATED_FIRST_NAME_KEY = "firstName"
        private const val DEPRECATED_LAST_NAME_KEY = "lastName"
        private const val DEPRECATED_NAME_KEY = "userName"
        private const val DEPRECATED_TRANSKRIBUS_PASSWORD_KEY = "userPassword"
        private const val DEPRECATED_DROPBOX_TOKEN_KEY = "dropboxToken"
        private const val DEPRECATED_CONNECTION_KEY = "connection"
        private const val DEPRECATED_SERIES_NAME_KEY = "SERIES_NAME_KEY"
        private const val DEPRECATED_SERIES_MODE_PAUSED_KEY = "SERIES_MODE_PAUSED_KEY"
        private const val DEPRECATED_SERVER_CHANGED_SHOWN_KEY = "server_changed_shown_key"
        private const val DEPRECATED_DOCUMENT_HINT_SHOWN_KEY = "DOCUMENT_HINT_SHOWN_KEY"
        private const val DEPRECATED_HIDE_SERIES_DIALOG_KEY = "HIDE_SERIES_DIALOG_KEY"

        private val KEYS_TO_DROP = listOf(
            DEPRECATED_FIRST_NAME_KEY,
            DEPRECATED_LAST_NAME_KEY,
            DEPRECATED_NAME_KEY,
            DEPRECATED_TRANSKRIBUS_PASSWORD_KEY,
            DEPRECATED_CONNECTION_KEY,
            DEPRECATED_DROPBOX_TOKEN_KEY,
            DEPRECATED_SERIES_NAME_KEY,
            DEPRECATED_SERIES_MODE_PAUSED_KEY,
            DEPRECATED_SERVER_CHANGED_SHOWN_KEY,
            DEPRECATED_DOCUMENT_HINT_SHOWN_KEY,
            DEPRECATED_HIDE_SERIES_DIALOG_KEY
        )
    }

    /**
     * A flag indicating if the initial DB migrations (since v1_80) should be performed.
     */
    var shouldPerformDBMigration: Boolean
        get() =
            sharedPreferences.getBoolean(KEY_DB_MIGRATION, true)
        // commit is used to avoid race conditions
        @SuppressLint("ApplySharedPref")
        set(value) {
            sharedPreferences.edit()
                .putBoolean(KEY_DB_MIGRATION, value)
                .commit()
        }

    /**
     * A flag indicating if the shared preferences migration should be performed!
     */
    private var shouldPerformV1_80_PrefsMigration: Boolean
        get() =
            sharedPreferences.getBoolean(KEY_MIGRATION_TO_PREFS_V1_80, true)
        // commit is used to avoid race conditions
        @SuppressLint("ApplySharedPref")
        set(value) {
            sharedPreferences.edit()
                .putBoolean(KEY_MIGRATION_TO_PREFS_V1_80, value)
                .commit()
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

    var collectionId: Int?
        get() {
            val id = sharedPreferences.getInt(COLLECTION_ID_KEY, 0)
            return if (id == 0) {
                null
            } else {
                id
            }
        }
        set(value) {
            value?.let {
                sharedPreferences.edit()
                    .putInt(COLLECTION_ID_KEY, value)
                    .apply()
            } ?: let {
                sharedPreferences.edit().remove(COLLECTION_ID_KEY).apply()
            }
        }

    var useTranskribusTestServer: Boolean
        get() {
            return defaultSharedPreferences.getBoolean(
                KEY_USE_TEST_SERVER,
                false
            )
        }
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_USE_TEST_SERVER, value)
                .apply()
        }

    var isGeoTaggingEnabled: Boolean
        get() {
            return defaultSharedPreferences.getBoolean(
                KEY_GEO_TAGGING,
                false
            )
        }
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_GEO_TAGGING, value)
                .apply()
        }

    var showFocusValues: Boolean
        get() {
            return defaultSharedPreferences.getBoolean(
                KEY_SHOW_FOCUS_VALUES,
                false
            )
        }
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_SHOW_FOCUS_VALUES, value)
                .apply()
        }

    var showGrid: Boolean
        get() {
            return defaultSharedPreferences.getBoolean(
                KEY_SHOW_GRID,
                false
            )
        }
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_SHOW_GRID, value)
                .apply()
        }

    var isFastSegmentation: Boolean
        get() {
            return defaultSharedPreferences.getBoolean(
                KEY_FAST_SEGMENTATION,
                false
            )
        }
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_FAST_SEGMENTATION, value)
                .apply()
        }

    var isFocusMeasure: Boolean
        get() {
            return defaultSharedPreferences.getBoolean(
                KEY_FOCUS_MEASURE,
                true
            )
        }
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_FOCUS_MEASURE, value)
                .apply()
        }

    var textOrientation: Int
        get() {
            return defaultSharedPreferences.getInt(
                KEY_TEXT_ORIENTATION,
                IMG_ORIENTATION_90
            )
        }
        set(value) {
            defaultSharedPreferences.edit()
                .putInt(KEY_TEXT_ORIENTATION, value)
                .apply()
        }

    var showDebugView: Boolean
        get() {
            return defaultSharedPreferences.getBoolean(
                KEY_SHOW_DEBUG_VIEW,
                false
            )
        }
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_SHOW_DEBUG_VIEW, value)
                .apply()
        }

    var showHUD: Boolean
        get() {
            return defaultSharedPreferences.getBoolean(
                KEY_HUD_ENABLED,
                false
            )
        }
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_HUD_ENABLED, value)
                .apply()
        }

    var showExposureLockWarning: Boolean
        get() {
            return defaultSharedPreferences.getBoolean(KEY_SHOW_EXPOSURE_LOCK_WARNING, true)
        }
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_SHOW_EXPOSURE_LOCK_WARNING, value)
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

    private var installedVersionCode: Int
        get() {
            // if the installed version key does not exist on the phone, then add the current one immediately, this
            // is important, otherwise migrations would be performed on clean installs too.
            if (!sharedPreferences.contains(INSTALLED_VERSION_KEY)) {
                installedVersionCode = BuildConfig.VERSION_CODE
            }
            return sharedPreferences.getInt(INSTALLED_VERSION_KEY, 0)
        }
        @SuppressLint("ApplySharedPref") set(value) {
            sharedPreferences.edit()
                .putInt(INSTALLED_VERSION_KEY, value)
                .commit()
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

    var isUploadOnMeteredNetworksAllowed: Boolean
        get() =
            defaultSharedPreferences.getBoolean(KEY_UPLOAD_MOBILE_DATA, true)
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_UPLOAD_MOBILE_DATA, value)
                .apply()
        }

    var isCrashReportingEnabled: Boolean
        get() =
            defaultSharedPreferences.getBoolean(KEY_SEND_CRASH_REPORTS, true)
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_SEND_CRASH_REPORTS, value)
                .apply()
        }

    /**
     * @return true if debug messages should be enabled.
     */
    var showExtendedDebugErrorMessages: Boolean
        get() =
            BuildConfig.DEBUG && defaultSharedPreferences.getBoolean(
                KEY_EXTENDED_DEBUG_ERROR_MESSAGES,
                false
            )
        set(value) {
            defaultSharedPreferences.edit()
                .putBoolean(KEY_EXTENDED_DEBUG_ERROR_MESSAGES, value)
                .apply()
        }

    var transkribusCookie: String?
        get() {
            return encryptedPref.getString(KEY_TRANSKRIBUS_SESSION_COOKIE, null)
        }
        set(value) {
            encryptedPref.edit()
                .putString(KEY_TRANSKRIBUS_SESSION_COOKIE, value)
                .apply()
        }

    var transkribusPassword: String?
        get() {
            return encryptedPref.getString(KEY_TRANSKRIBUS_PASSWORD, null)
        }
        set(value) {
            encryptedPref.edit()
                .putString(KEY_TRANSKRIBUS_PASSWORD, value)
                .apply()
        }

    var exportDirectoryUri: String?
        get() {
            return defaultSharedPreferences.getString(KEY_EXPORT_DIR, null)
        }
        set(value) {
            defaultSharedPreferences.edit()
                .putString(KEY_EXPORT_DIR, value)
                .apply()
        }

    init {
        val masterKeyBuilder = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
                .build()
            masterKeyBuilder.setKeyGenParameterSpec(keyGenParameterSpec)
        }

        encryptedPref = EncryptedSharedPreferences.create(
            context,
            "encrypted_",
            masterKeyBuilder.build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // represents a clean install or an already updated version
        if (BuildConfig.VERSION_CODE == installedVersionCode) {
            // disable migration flags, otherwise for a clean install they could be run too.
            shouldPerformDBMigration = false
            shouldPerformV1_80_PrefsMigration = false
        }

        // check for app updates and perform possible migrations
        if (BuildConfig.VERSION_CODE > installedVersionCode) {
            if (shouldPerformV1_80_PrefsMigration) {
                // if the location permission has been given before, then enable the preference option
                val isLocationPermissionGiven = PermissionHandler.isLocationPermissionGiven(context)
                isGeoTaggingEnabled = isLocationPermissionGiven

                // migrate transkribus user data
                val firstName = defaultSharedPreferences.getString(DEPRECATED_FIRST_NAME_KEY, null)
                val lastName = defaultSharedPreferences.getString(DEPRECATED_LAST_NAME_KEY, null)
                val userName = defaultSharedPreferences.getString(DEPRECATED_NAME_KEY, null)
                val userPassword =
                    defaultSharedPreferences.getString(DEPRECATED_TRANSKRIBUS_PASSWORD_KEY, null)

                // the sessionIdCookie has not been previously stored in the app.
                if (userName != null && userPassword != null && firstName != null && lastName != null) {
                    transkribusPassword = userPassword
                    GlobalScope.launch(Dispatchers.IO) {
                        userDao.insertUser(
                            User(
                                firstName = firstName,
                                lastName = lastName,
                                userName = userName
                            )
                        )
                    }
                }

                // drop all deprecated keys
                KEYS_TO_DROP.forEach {
                    defaultSharedPreferences.edit().remove(it).apply()
                    sharedPreferences.edit().remove(it).apply()
                }
                // mark migration as performed
                shouldPerformV1_80_PrefsMigration = false
            }
        }
        // save the installed version code
        installedVersionCode = BuildConfig.VERSION_CODE
    }
}
