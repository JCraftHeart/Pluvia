package com.OxGames.Pluvia

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.room.withTransaction
import com.OxGames.Pluvia.data.AppInfo
import com.OxGames.Pluvia.data.BranchInfo
import com.OxGames.Pluvia.data.ConfigInfo
import com.OxGames.Pluvia.data.DepotInfo
import com.OxGames.Pluvia.data.DownloadInfo
import com.OxGames.Pluvia.data.LaunchInfo
import com.OxGames.Pluvia.data.LibraryAssetsInfo
import com.OxGames.Pluvia.data.LibraryCapsuleInfo
import com.OxGames.Pluvia.data.LibraryHeroInfo
import com.OxGames.Pluvia.data.LibraryLogoInfo
import com.OxGames.Pluvia.data.ManifestInfo
import com.OxGames.Pluvia.data.PackageInfo
import com.OxGames.Pluvia.data.SaveFilePattern
import com.OxGames.Pluvia.data.SteamData
import com.OxGames.Pluvia.data.SteamFriend
import com.OxGames.Pluvia.data.UFS
import com.OxGames.Pluvia.data.UserFileInfo
import com.OxGames.Pluvia.db.PluviaDatabase
import com.OxGames.Pluvia.enums.AppType
import com.OxGames.Pluvia.enums.ControllerSupport
import com.OxGames.Pluvia.enums.Language
import com.OxGames.Pluvia.enums.LoginResult
import com.OxGames.Pluvia.enums.OS
import com.OxGames.Pluvia.enums.OSArch
import com.OxGames.Pluvia.enums.PathType
import com.OxGames.Pluvia.enums.ReleaseState
import com.OxGames.Pluvia.enums.SyncResult
import com.OxGames.Pluvia.events.SteamEvent
import com.OxGames.Pluvia.utils.FileUtils
import com.OxGames.Pluvia.utils.SteamUtils
import com.winlator.xenvironment.ImageFs
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.enums.EClientPersonaStateFlag
import `in`.dragonbra.javasteam.enums.EOSType
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.networking.steam3.ProtocolTypes
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChat_RequestFriendPersonaStates_Request
import `in`.dragonbra.javasteam.rpc.service.Chat
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.contentdownloader.ContentDownloader
import `in`.dragonbra.javasteam.steam.contentdownloader.FileManifestProvider
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.discovery.ServerQuality
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.LicenseListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSProductInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.AppFileChangeList
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.AppFileInfo
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendsListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.NicknameListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStatesCallback
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamworkshop.SteamWorkshop
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration
import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.NetHelpers
import `in`.dragonbra.javasteam.util.crypto.CryptoHelper
import `in`.dragonbra.javasteam.util.log.DefaultLogListener
import `in`.dragonbra.javasteam.util.log.LogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Date
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import java.util.zip.ZipInputStream
import javax.inject.Inject
import kotlin.io.path.name
import kotlin.io.path.pathString

@AndroidEntryPoint
class SteamService : Service(), IChallengeUrlChanged {

    @Inject
    lateinit var db: PluviaDatabase

    private var _callbackManager: CallbackManager? = null
    private var _steamClient: SteamClient? = null
    private var _steamUser: SteamUser? = null
    private var _steamApps: SteamApps? = null
    private var _steamFriends: SteamFriends? = null
    private var _steamCloud: SteamCloud? = null
    private var _unifiedMessages: SteamUnifiedMessages? = null
    private var _unifiedChat: Chat? = null

    private val _callbackSubscriptions: ArrayList<Closeable> = ArrayList()

    private var _loginResult: LoginResult = LoginResult.Failed

    private var retryAttempt = 0

    private val packageInfo = ConcurrentHashMap<Int, PackageInfo>()
    private val appInfo = ConcurrentHashMap<Int, AppInfo>()

    private val dbScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val MAX_RETRY_ATTEMPTS = 20
        const val LOGIN_ID = 382945
        const val AVATAR_BASE_URL = "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/"
        const val MISSING_AVATAR_URL = "${AVATAR_BASE_URL}fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"
        const val INVALID_APP_ID: Int = Int.MAX_VALUE
        const val INVALID_PKG_ID: Int = Int.MAX_VALUE
        const val INVALID_DEPOT_ID: Int = Int.MAX_VALUE
        const val INVALID_MANIFEST_ID: Long = Long.MAX_VALUE
        private val PROTOCOL_TYPES = EnumSet.of(ProtocolTypes.TCP, ProtocolTypes.UDP)

        private var _steamData: SteamData? = null
        private var instance: SteamService? = null

        private val downloadJobs = ConcurrentHashMap<Int, DownloadInfo>()

        // var isLoggingOut: Boolean = false
        //     private set

        var isConnecting: Boolean = false
            private set
        var isStopping: Boolean = false
            private set
        var isConnected: Boolean = false
            private set
        var isRunning: Boolean = false
            private set
        var isLoggingIn: Boolean = false
            private set
        val isLoggedIn: Boolean
            get() = instance?._steamClient?.steamID?.run { isValid } == true
        var isWaitingForQRAuth: Boolean = false
            private set
        var isReceivingLicenseList: Boolean = false
            private set
        var isRequestingPkgInfo: Boolean = false
            private set
        var isRequestingAppInfo: Boolean = false
            private set

        private val serverListPath: String
            get() = Paths.get(instance!!.cacheDir.path, "server_list.bin").pathString

        private val depotManifestsPath: String
            get() = Paths.get(instance!!.dataDir.path, "Steam", "depot_manifests.zip").pathString

        private val steamDataPath: String
            get() = Paths.get(instance!!.dataDir.path, "steam_data.json").pathString

        private val defaultAppInstallPath: String
            get() = Paths.get(instance!!.dataDir.path, "Steam", "steamapps", "common").pathString

        private val defaultAppStagingPath: String
            get() = Paths.get(instance!!.dataDir.path, "Steam", "steamapps", "staging").pathString

        private fun loadSteamData() {
            val steamDataStr = FileUtils.readFileAsString(steamDataPath)
            _steamData = if (steamDataStr != null) {
                Json.decodeFromString<SteamData>(steamDataStr)
            } else {
                SteamData(
                    appInstallPath = defaultAppInstallPath,
                    appStagingPath = defaultAppStagingPath
                )
            }
        }

        private fun saveSteamData() {
            FileUtils.writeStringToFile(Json.encodeToString(_steamData), steamDataPath)
        }

        fun requestUserPersona() {
            CoroutineScope(Dispatchers.Default).launch {
                getUserSteamId()?.let {
                    // in order to get user avatar url and other info
                    instance?._steamFriends?.requestFriendInfo(it)
                }
            }
        }

        fun getUserSteamId(): SteamID? = instance?._steamClient?.steamID

        fun getPersonaStateOf(steamId: SteamID): SteamFriend? = runBlocking {
            instance!!.db
                .steamFriendDao()
                .findFriend(steamId.convertToUInt64())
                .first()
        }

        fun getAppList(filter: EnumSet<AppType>): List<AppInfo> =
            instance?.appInfo?.values?.filter { filter.contains(it.type) } ?: emptyList()

        fun getPkgInfoOf(appId: Int): PackageInfo? = instance?.packageInfo?.values?.firstOrNull {
            // Log.d("SteamService", "Pkg (${it.packageId}) apps: ${it.appIds.joinToString(",")}")
            it.appIds.contains(appId)
        }

        fun getAppInfoOf(appId: Int): AppInfo? = instance?.appInfo?.values?.firstOrNull {
            it.appId == appId
        }

        fun getAppDownloadInfo(appId: Int): DownloadInfo? = downloadJobs[appId]

        fun isAppInstalled(appId: Int): Boolean = Files.exists(Paths.get(getAppDirPath(appId)))

        fun getAppRawDirPath(appId: Int): String = Paths.get(
            _steamData?.appInstallPath ?: "",
            getAppInfoOf(appId)?.config?.installDir ?: ""
        ).pathString

        fun getAppDirPath(appId: Int): String {
            val origPath = getAppRawDirPath(appId)
            return origPath.trim().replace(" ", "_")
        }

        fun downloadApp(appId: Int): DownloadInfo? {
            getPkgInfoOf(appId)?.let { pkgInfo ->
                Log.d("SteamService", "App pkg contains ${pkgInfo.depotIds.size} depot(s): [${pkgInfo.depotIds.joinToString(", ")}]")
            }
            return getAppInfoOf(appId)?.let { appInfo ->
                Log.d("SteamService", "App contains ${appInfo.depots.size} depot(s): ${appInfo.depots.keys}")
                appInfo.depots.filter { depotEntry ->
                    val depot = depotEntry.value
                    // depot.sharedInstall == false &&
                    (
                        depot.osList.contains(OS.windows) ||
                        (
                            !depot.osList.contains(OS.linux) &&
                            !depot.osList.contains(OS.macos)
                        )
                    ) &&
                    (depot.osArch == OSArch.Arch64 || depot.osArch == OSArch.Unknown)
                }.let { depotEntries ->
                    downloadApp(appId, depotEntries.keys.toList(), "public")
                }
            }
        }

        fun downloadApp(appId: Int, depotIds: List<Int>, branch: String): DownloadInfo? {
            if (downloadJobs.contains(appId)) {
                Log.e(
                    "SteamService",
                    "Could not start new download job for $appId since one already exists"
                )

                return getAppDownloadInfo(appId)
            }
            if (depotIds.isEmpty()) {
                Log.e(
                    "SteamService",
                    "No depots to download for $appId"
                )
                return null
            }
            Log.d("SteamService", "Found ${depotIds.size} depot(s) to download: $depotIds")

            val downloadInfo = DownloadInfo().also { downloadInfo ->
                downloadInfo.setDownloadJob(
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            depotIds.forEach { depotId ->
                                ContentDownloader(instance!!._steamClient!!).downloadApp(
                                    appId,
                                    depotId,
                                    _steamData!!.appInstallPath,
                                    _steamData!!.appStagingPath,
                                    branch,
                                    // maxDownloads = 1,
                                    onDownloadProgress = { downloadInfo.setProgress(it) },
                                    parentScope = coroutineContext.job as CoroutineScope
                                ).await()
                            }
                            // rename directory to our specification
                            val origPath = getAppRawDirPath(appId)
                            val newPath = getAppDirPath(appId)
                            if (origPath != newPath && Files.exists(Paths.get(origPath))) {
                                File(origPath).renameTo(File(newPath))
                            }
                        } finally {
                            /* Nothing */
                        }

                        downloadJobs.remove(appId)
                    }
                )
            }

            downloadJobs[appId] = downloadInfo

            return downloadInfo
        }

        /**
         * Default timeout to use when making requests
         */
        var requestTimeout = 10000L

        /**
         * Default timeout to use when reading the response body
         */
        var responseBodyTimeout = 60000L

        var syncInProgress: Boolean = false
        val MAX_USER_FILE_RETRIES = 3

        data class PostSyncInfo(
            val syncResult: SyncResult,
            val remoteTimestamp: Long = 0,
            val localTimestamp: Long = 0,
            val uploadsRequired: Boolean = false,
            val uploadsCompleted: Boolean = false,
        )

        fun beginLaunchApp(
            appId: Int,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            prefixToPath: (String) -> String,
        ) = parentScope.async {
            if (syncInProgress) {
                Log.w("SteamService", "Cannot launch app when sync already in progress")
                return@async
            }
            syncInProgress = true
            _steamData?.clientId?.let { clientId ->
                instance?.let { steamInstance ->
                    steamInstance._steamCloud?.let { steamCloud ->
                        syncUserFiles(
                            appId,
                            clientId,
                            steamInstance,
                            steamCloud,
                            parentScope,
                            prefixToPath
                        ).await()
                        steamCloud.signalAppLaunchIntent(
                            appId = appId,
                            clientId = clientId,
                            osType = EOSType.AndroidUnknown
                        )
                    }
                }
            }
            syncInProgress = false
        }
        fun closeApp(
            appId: Int,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            prefixToPath: (String) -> String,
        ) = parentScope.async {
            if (syncInProgress) {
                Log.w("SteamService", "Cannot close app when sync already in progress")
                return@async
            }
            syncInProgress = true
            _steamData?.clientId?.let { clientId ->
                instance?.let { steamInstance ->
                    steamInstance._steamCloud?.let { steamCloud ->
                        val postSyncInfo = syncUserFiles(
                            appId,
                            clientId,
                            steamInstance,
                            steamCloud,
                            parentScope,
                            prefixToPath
                        ).await()
                        steamCloud.signalAppExitSyncDone(
                            appId = appId,
                            clientId = clientId,
                            uploadsCompleted = postSyncInfo?.uploadsCompleted == true,
                            uploadsRequired = postSyncInfo?.uploadsRequired == true,
                        )
                    }
                }
            }
            syncInProgress = false
        }

        fun getProotTime(context: Context): Long {
            val imageFs = ImageFs.find(context)
            if (!imageFs.rootDir.exists())
                return 0
            val nativeLibraryDir = context.applicationInfo.nativeLibraryDir
            val command = arrayOf(
                "$nativeLibraryDir/libproot.so",
                "--kill-on-exit",
                "--rootfs=${imageFs.rootDir}",
                "--cwd=${ImageFs.USER}",
                "--bind=/dev",
                "--bind=${imageFs.rootDir}/tmp/shm:/dev/shm",
                "--bind=/proc",
                "--bind=/sys",
                "/usr/bin/env",
                "HOME=/home/${ImageFs.USER}",
                "USER=${ImageFs.USER}",
                "TMPDIR=/tmp",
                "LC_ALL=en_US.utf8",
                "PATH=${imageFs.winePath}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",  // Set PATH environment variable
                "LD_LIBRARY_PATH=/usr/lib/aarch64-linux-gnu:/usr/lib/arm-linux-gnueabihf",
                "date",
                "+%s%3N"
            )
            val envVars = arrayOf(
                "PROOT_TMP_DIR=${Paths.get(context.filesDir.absolutePath, "tmp")}",
                "PROOT_LOADER=$nativeLibraryDir/libproot-loader.so",
            )

            val process = Runtime.getRuntime().exec(command, envVars, imageFs.rootDir)

            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val error = errorReader.readLine()
            if (error != null) {
                Log.e("ProotTime", "Error: $error")
            }

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine() ?: ""
            process.waitFor()

            Log.d("ProotTime", "Output: $output")
            return output.toLongOrNull() ?: 0
        }
        data class FileChanges(
            val filesDeleted: List<UserFileInfo>,
            val filesModified: List<UserFileInfo>,
            val filesCreated: List<UserFileInfo>,
        )
        fun syncUserFiles(
            appId: Int,
            clientId: Long,
            steamInstance: SteamService,
            steamCloud: SteamCloud,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            prefixToPath: (String) -> String,
        ): Deferred<PostSyncInfo?> = parentScope.async {
            var postSyncInfo: PostSyncInfo? = null
            _steamData?.let { steamData ->
                steamInstance.appInfo[appId]?.let { appInfo ->
                    Log.d("SteamService", "Retrieving save files of ${appInfo.name}")

                    val printFileChangeList: (AppFileChangeList) -> Unit = { fileList ->
                        Log.d("SteamService",
                            "GetAppFileListChange($appId):" +
                            "\n\tTotal Files: ${fileList.files.size}" +
                            "\n\tCurrent Change Number: ${fileList.currentChangeNumber}" +
                            "\n\tIs Only Delta: ${fileList.isOnlyDelta}" +
                            "\n\tApp BuildID Hwm: ${fileList.appBuildIDHwm}" +
                            "\n\tPath Prefixes: \n\t\t${fileList.pathPrefixes.joinToString("\n\t\t")}" +
                            "\n\tMachine Names: \n\t\t${fileList.machineNames.joinToString("\n\t\t")}" +
                            fileList.files.map {
                                "\n\t${it.filename}:" +
                                "\n\t\tshaFile: ${it.shaFile}" +
                                "\n\t\ttimestamp: ${it.timestamp}" +
                                "\n\t\trawFileSize: ${it.rawFileSize}" +
                                "\n\t\tpersistState: ${it.persistState}" +
                                "\n\t\tplatformsToSync: ${it.platformsToSync}" +
                                "\n\t\tpathPrefixIndex: ${it.pathPrefixIndex}" +
                                "\n\t\tmachineNameIndex: ${it.machineNameIndex}"
                            }.joinToString()
                        )
                    }
                    val getPathTypePairs: (AppFileChangeList) -> List<Pair<String, String>> = { fileList ->
                        fileList.pathPrefixes.map {
                            val matchResults = Regex("%\\w+%").findAll(it).map { it.value }.toList()
                            Log.d("SteamService", "Mapping prefix $it and found $matchResults")
                            matchResults
                        }.flatten()
                        .distinct()
                        .map {
                            Pair(it, prefixToPath(it))
                        }
                    }
                    val convertPrefixes: (AppFileChangeList) -> List<String> = { fileList ->
                        val pathTypePairs = getPathTypePairs(fileList)
                        fileList.pathPrefixes.map { prefix ->
                            var modified = prefix
                            pathTypePairs.forEach {
                                modified = modified.replace(it.first, it.second)
                            }
                            modified
                        }
                    }
                    val getFilePrefix: (AppFileInfo, AppFileChangeList) -> String = { file, fileList ->
                        if (file.pathPrefixIndex < fileList.pathPrefixes.size)
                            Paths.get(fileList.pathPrefixes[file.pathPrefixIndex]).pathString
                        else
                            Paths.get("%${PathType.GameInstall.name}%").pathString
                    }
                    val getFilePrefixPath: (AppFileInfo, AppFileChangeList) -> String = { file, fileList ->
                        Paths.get(getFilePrefix(file, fileList), file.filename).pathString
                    }
                    val getFullFilePath: (AppFileInfo, AppFileChangeList) -> Path = { file, fileList ->
                        val convertedPrefixes = convertPrefixes(fileList)
                        if (file.pathPrefixIndex < fileList.pathPrefixes.size)
                            Paths.get(convertedPrefixes[file.pathPrefixIndex], file.filename)
                        else
                            Paths.get(getAppDirPath(appId), file.filename)
                    }
                    val haveLocalFilesChanged: (List<UserFileInfo>, List<UserFileInfo>) -> Pair<Boolean, FileChanges> = { currentFiles, oldFiles ->
                        val overlappingFiles = currentFiles.filter { currentFile ->
                            oldFiles.any { currentFile.getPrefixPath() == it.getPrefixPath() }
                        }
                        val newFiles = currentFiles.filter { currentFile ->
                            !oldFiles.any { currentFile.getPrefixPath() == it.getPrefixPath() }
                        }
                        val deletedFiles = oldFiles.filter { oldFile ->
                            !currentFiles.any { oldFile.getPrefixPath() == it.getPrefixPath() }
                        }
                        val modifiedFiles = overlappingFiles.filter { file ->
                            oldFiles.first {
                                it.getPrefixPath() == file.getPrefixPath()
                            }.let {
                                Log.d("SteamService", "Comparing SHA of ${it.getPrefixPath()} and ${file.getPrefixPath()}")
                                Log.d("SteamService", "[${it.sha.joinToString(", ")}]\n[${file.sha.joinToString(", ")}]")
                                !it.sha.contentEquals(file.sha)
                            }
                        }
                        val changesExist =
                            newFiles.isNotEmpty() ||
                            deletedFiles.isNotEmpty() ||
                            modifiedFiles.isNotEmpty()
                        Pair(changesExist, FileChanges(deletedFiles, modifiedFiles, newFiles))

                        // if (currentFiles.size != oldFiles.size) true
                        // else {
                        //     val sortedCurrent = currentFiles.sortedBy { it.getPrefixPath() }
                        //     val sortedOld = oldFiles.sortedBy { it.getPrefixPath() }
                        //     sortedCurrent.mapIndexed { index, currentFile ->
                        //         Log.d("SteamService", "Comparing SHA of ${currentFile.getPrefixPath()} and ${sortedOld[index].getPrefixPath()}")
                        //         Log.d("SteamService", "[${currentFile.sha.joinToString(", ")}]\n[${sortedOld[index].sha.joinToString(", ")}]")
                        //         !currentFile.sha.contentEquals(sortedOld[index].sha)
                        //     }.any { it }
                        // }
                    }
                    val hasHashConflicts: (Map<String, List<UserFileInfo>>, AppFileChangeList) -> Boolean = { localUserFiles, fileList ->
                        fileList.files.any { file ->
                            Log.d("SteamService", "Checking for ${getFilePrefix(file, fileList)} in ${localUserFiles.keys}")
                            localUserFiles[getFilePrefix(file, fileList)]?.let { localUserFile ->
                                localUserFile.firstOrNull {
                                    Log.d("SteamService", "Comparing ${file.filename} and ${it.filename}")
                                    it.filename == file.filename
                                }?.let {
                                    Log.d("SteamService", "Comparing SHA of ${getFilePrefixPath(file, fileList)} and ${it.getPrefixPath()}")
                                    Log.d("SteamService", "[${file.shaFile.joinToString(", ")}]\n[${it.sha.joinToString(", ")}]")
                                    !file.shaFile.contentEquals(it.sha)
                                }
                            } == true
                        }
                    }
                    val hasHashConflictsOrRemoteMissingFiles: (Map<String, List<UserFileInfo>>, AppFileChangeList) -> Boolean = { localUserFiles, fileList ->
                        localUserFiles.values.any {
                            it.any { localUserFile ->
                                fileList.files.firstOrNull { cloudFile ->
                                    val cloudFilePath = getFilePrefixPath(cloudFile, fileList)
                                    val localFilePath = Paths.get(localUserFile.getPrefix(), localUserFile.filename).pathString
                                    Log.d("SteamService", "Comparing $cloudFilePath and $localFilePath")
                                    cloudFilePath == localFilePath
                                }?.let {
                                    Log.d("SteamService", "Comparing SHA of ${getFilePrefixPath(it, fileList)} and ${localUserFile.getPrefixPath()}")
                                    Log.d("SteamService", "[${it.shaFile.joinToString(", ")}]\n[${localUserFile.sha.joinToString(", ")}]")
                                    it.shaFile.contentEquals(localUserFile.sha)
                                } != true
                            }
                        }
                    }
                    val getLocalUserFilesAsPrefixMap: () -> Map<String, List<UserFileInfo>> = {
                        appInfo.ufs.saveFilePatterns.filter { userFile ->
                            userFile.root.isWindows()
                        }.map { userFile ->
                            Pair(
                                Paths.get(userFile.getPrefix()).pathString,
                                FileUtils.findFiles(
                                    Paths.get(
                                        prefixToPath(userFile.root.toString()),
                                        userFile.path
                                    ),
                                    userFile.pattern
                                ).map {
                                    val sha = CryptoHelper.shaHash(Files.readAllBytes(it))
                                    Log.d("SteamService", "Found ${it.pathString}\n\tin ${userFile.getPrefix()}\n\twith sha [${sha.joinToString(", ")}]")
                                    UserFileInfo(
                                        userFile.root,
                                        userFile.path,
                                        it.name,
                                        Files.getLastModifiedTime(it).toMillis(),
                                        sha
                                    )
                                    // Pair(it, CryptoHelper.shaHash(Files.readAllBytes(it)))
                                }.collect(Collectors.toList())
                            )
                        }.toMap()
                    }
                    val buildUrl: (Boolean, String, String) -> String = { useHttps, urlHost, urlPath ->
                        val scheme = if (useHttps) "https://" else "http://"
                        "$scheme${urlHost}${urlPath}"
                    }

                    val prootTimestampToDate: (Long) -> Date = { originalTimestamp ->
                        val androidTimestamp = System.currentTimeMillis()
                        val prootTimestamp = getProotTime(steamInstance)
                        val timeDifference = androidTimestamp - prootTimestamp
                        val adjustedTimestamp = originalTimestamp + timeDifference
                        Log.d("SteamService", "Android: $androidTimestamp, PRoot: $prootTimestamp, $originalTimestamp -> $adjustedTimestamp")
                        Date(adjustedTimestamp)
                    }
                    val downloadFiles: (AppFileChangeList, CoroutineScope) -> Deferred<Unit> = { fileList, parentScope ->
                        parentScope.async {
                            // val convertedPrefixes = convertPrefixes(fileList)
                            fileList.files.forEach { file ->
                                val prefixedPath = getFilePrefixPath(file, fileList)
                                val actualFilePath = getFullFilePath(file, fileList)
                                Log.d("SteamService", "$prefixedPath -> $actualFilePath")

                                val fileDownloadInfo = steamCloud.clientFileDownload(appId, prefixedPath)
                                if (fileDownloadInfo.urlHost.isNotEmpty()) {
                                    val httpUrl = buildUrl(
                                        fileDownloadInfo.useHttps,
                                        fileDownloadInfo.urlHost,
                                        fileDownloadInfo.urlPath
                                    )
                                    Log.d("SteamService", "Downloading $httpUrl")
                                    val request = Request.Builder()
                                        .url(httpUrl)
                                        .headers(Headers.headersOf
                                            (*fileDownloadInfo.requestHeaders
                                                .map { listOf(it.name, it.value) }
                                                .flatten()
                                                .toTypedArray()
                                            )
                                        )
                                        .build()
                                    val httpClient = steamInstance._steamClient!!.configuration.httpClient
                                    withTimeout(requestTimeout) {
                                        val response = httpClient.newCall(request).execute()

                                        if (!response.isSuccessful) {
                                            Log.e("SteamService", "File download of $prefixedPath was unsuccessful")
                                            return@withTimeout
                                        }

                                        val copyToFile: (InputStream) -> Unit = { input ->
                                            Files.createDirectories(actualFilePath.parent)
                                            FileOutputStream(actualFilePath.toString()).use { fs ->
                                                val bytesRead = input.copyTo(fs)
                                                if (bytesRead != fileDownloadInfo.rawFileSize.toLong()) {
                                                    Log.e("SteamService", "Bytes read from stream of $prefixedPath does not match expected size")
                                                }
                                            }
                                        }

                                        withTimeout(responseBodyTimeout) {
                                            if (fileDownloadInfo.fileSize != fileDownloadInfo.rawFileSize) {
                                                response.body?.byteStream()?.use { inputStream ->
                                                    ZipInputStream(inputStream).use { zipInput ->
                                                        val entry = zipInput.nextEntry
                                                        if (entry == null) {
                                                            Log.w("SteamService", "Downloaded user file $prefixedPath has no zip entries")
                                                            return@withTimeout
                                                        }

                                                        copyToFile(zipInput)
                                                        if (zipInput.nextEntry != null) {
                                                            Log.e("SteamService", "Downloaded user file $prefixedPath has more than one zip entry")
                                                        }
                                                    }
                                                }
                                            } else {
                                                response.body?.byteStream()?.use { inputStream ->
                                                    copyToFile(inputStream)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Log.w("SteamService", "URL host of $prefixedPath was empty")
                                }
                            }
                        }
                    }
                    val uploadFiles: (FileChanges, CoroutineScope) -> Deferred<Pair<Boolean, Long>> = { fileChanges, parentScope ->
                        parentScope.async {
                            val filesToDelete = fileChanges.filesDeleted.map { it.getPrefixPath() }
                            val filesToUpload = fileChanges.filesCreated.union(fileChanges.filesModified).map { Pair(it.getPrefixPath(), it) }
                            Log.d("SteamService", "Beginning app upload batch with ${filesToDelete.size} file(s) to delete and ${filesToUpload.size} file(s) to upload")
                            val uploadBatchResponse = steamCloud.beginAppUploadBatch(
                                appId = appId,
                                clientId = clientId,
                                filesToDelete = filesToDelete,
                                filesToUpload = filesToUpload.map { it.first },
                                // TODO: have branch be user selected and use that selection here
                                appBuildId = appInfo.branches["public"]?.buildId ?: 0
                            )
                            var uploadBatchSuccess = true
                            filesToUpload.map { it.second }.forEach { file ->
                                val absFilePath = Paths.get(prefixToPath(file.root.toString()), file.path, file.filename)
                                val fileSize = Files.size(absFilePath).toInt()
                                Log.d("SteamService", "Beginning upload of ${file.getPrefixPath()} whose timestamp is ${file.timestamp}")
                                val uploadInfo = steamCloud.beginFileUpload(
                                    appId = appId,
                                    filename = file.getPrefixPath(),
                                    fileSize = fileSize,
                                    rawFileSize = fileSize,
                                    fileSha = file.sha,
                                    // timestamp = prootTimestampToDate(file.timestamp),
                                    timestamp = Date(file.timestamp),
                                    uploadBatchId = uploadBatchResponse.batchID,
                                )
                                var uploadFileSuccess = true
                                RandomAccessFile(absFilePath.pathString, "r").use { fs ->
                                    uploadInfo.blockRequests.forEach { blockRequest ->
                                        val httpUrl = buildUrl(
                                            blockRequest.useHttps,
                                            blockRequest.urlHost,
                                            blockRequest.urlPath
                                        )
                                        Log.d("SteamService", "Uploading to $httpUrl")

                                        Log.d("SteamService",
                                            "Block Request:" +
                                            "\n\tblockOffset: ${blockRequest.blockOffset}" +
                                            "\n\tblockLength: ${blockRequest.blockLength}" +
                                            "\n\trequestHeaders:\n\t\t${blockRequest.requestHeaders.map { "${it.name}: ${it.value}" }.joinToString("\n\t\t")}" +
                                            "\n\texplicitBodyData: [${blockRequest.explicitBodyData.joinToString(", ")}]" +
                                            "\n\tmayParallelize: ${blockRequest.mayParallelize}"
                                        )
                                        val byteArray = ByteArray(blockRequest.blockLength)
                                        fs.seek(blockRequest.blockOffset)
                                        val bytesRead = fs.read(byteArray, 0, blockRequest.blockLength)
                                        Log.d("SteamService", "Read $bytesRead byte(s) for block")
                                        val mediaType = "application/octet-stream".toMediaTypeOrNull()
                                        val requestBody = byteArray.toRequestBody(mediaType)
                                        // val requestBody = byteArray.toRequestBody()

                                        val headers = Headers.headersOf(
                                            *blockRequest.requestHeaders
                                                .map { listOf(it.name, it.value) }
                                                .flatten()
                                                .toTypedArray()
                                        )

                                        // val request = Request.Builder()
                                        //     .url(httpUrl)
                                        //     .post(requestBody)
                                        //     .headers(headers)
                                        //     .build()
                                        val request = Request.Builder()
                                            .url(httpUrl)
                                            .put(requestBody)
                                            .headers(headers)
                                            .addHeader("Accept", "text/html,*/*;q=0.9")
                                            .addHeader("accept-encoding", "gzip,identity,*;q=0")
                                            .addHeader("accept-charset", "ISO-8859-1,utf-8,*;q=0.7")
                                            .addHeader("user-agent", "Valve/Steam HTTP Client 1.0")
                                            .build()
                                        val httpClient = steamInstance._steamClient!!.configuration.httpClient

                                        Log.d("SteamService", "Sending request to ${request.url} using\n$request")
                                        withTimeout(requestTimeout) {
                                            val response = httpClient.newCall(request).execute()

                                            if (!response.isSuccessful) {
                                                Log.e(
                                                    "SteamService",
                                                    "Failed to upload part of ${file.getPrefixPath()}: ${response.message}, ${response.body?.string()}"
                                                )
                                                uploadFileSuccess = false
                                                uploadBatchSuccess = false
                                            }
                                        }
                                    }
                                }
                                val commitSuccess = steamCloud.commitFileUpload(
                                    transferSucceeded = uploadFileSuccess,
                                    appId = appId,
                                    fileSha = file.sha,
                                    filename = file.getPrefixPath(),
                                )
                                Log.d("SteamService", "File ${file.getPrefixPath()} commit success: $commitSuccess")
                            }
                            steamCloud.completeAppUploadBatchBlocking(
                                appId = appId,
                                batchId = uploadBatchResponse.batchID,
                                batchEResult = if (uploadBatchSuccess) EResult.OK else EResult.Fail
                            )
                            Pair(uploadBatchSuccess, uploadBatchResponse.appChangeNumber)
                        }
                    }

                    var syncResult = SyncResult.Success
                    var remoteTimestamp = 0L
                    var localTimestamp = 0L
                    var uploadsRequired = false
                    var uploadsCompleted = false

                    val localAppChangeNumber = steamData.appChangeNumbers[appId] ?: 0
                    // val fullCloudFileList = steamCloud.getAppFileListChange(appId)
                    val appFileListChange = steamCloud.getAppFileListChange(appId, localAppChangeNumber)
                    val cloudAppChangeNumber = appFileListChange.currentChangeNumber
                    Log.d("SteamService", "AppChangeNumber: $localAppChangeNumber -> $cloudAppChangeNumber")

                    // retrieve existing user files from local storage
                    val localUserFilesMap = getLocalUserFilesAsPrefixMap()
                    val allLocalUserFiles = localUserFilesMap.map { it.value }.flatten()

                    if (localAppChangeNumber == cloudAppChangeNumber) {
                        // our app change numbers are the same so the file hashes should match
                        // if they do not then that means we have new user files locally that
                        // need uploading

                        var fileChanges: FileChanges? = null
                        val hasLocalChanges = steamData.appFileChangeLists[appId]?.let { prevLocalUserFiles ->
                            val result = haveLocalFilesChanged(allLocalUserFiles, prevLocalUserFiles)
                            fileChanges = result.second
                            result.first
                        } == true
                        if (hasLocalChanges) {
                            Log.d("SteamService", "Found local changes and no new cloud user files, uploading...")
                            uploadsRequired = fileChanges!!.filesCreated.isNotEmpty() || fileChanges.filesModified.isNotEmpty()
                            val uploadResult = uploadFiles(fileChanges, parentScope).await()
                            uploadsCompleted = uploadsRequired && uploadResult.first
                            if (uploadResult.first) {
                                steamData.appFileChangeLists[appId] = allLocalUserFiles
                                steamData.appChangeNumbers[appId] = uploadResult.second
                                saveSteamData()
                            } else {
                                syncResult = SyncResult.UpdateFail
                            }
                        } else {
                            Log.d("SteamService", "No local changes and no new cloud user files, doing nothing...")
                            syncResult = SyncResult.UpToDate
                        }

                        // val hasConflicts = hasHashConflictsOrRemoteMissingFiles(localUserFiles, fullCloudFileList)
                        // Log.d("SteamService", "Conflicts on equal change numbers: $hasConflicts")

                        // val appFileListChange = steamCloud.getAppFileListChange(appId, localAppChangeNumber)

                        // steamData.appChangeNumbers[appId] = cloudAppChangeNumber
                        // saveSteamData()
                    } else if (localAppChangeNumber < cloudAppChangeNumber) {
                        // our change number is less than the expected, meaning we are behind and
                        // need to download the new user files, but first we should check that
                        // the local user files are not conflicting with their respective change
                        // number or else that would mean that the user made changes locally and
                        // on a separate device and they must choose between the two

                        var hasLocalChanges = steamData.appFileChangeLists[appId]?.let { prevLocalUserFiles ->
                            haveLocalFilesChanged(allLocalUserFiles, prevLocalUserFiles).first
                        } == true
                        if (!hasLocalChanges) {
                            // we can safely download the new changes since no changes have been
                            // made locally

                            Log.d("SteamService", "No local changes but new cloud user files, downloading...")

                            var updatedLocalFiles: Map<String, List<UserFileInfo>>
                            var retries = 0
                            do {
                                downloadFiles(appFileListChange, parentScope).await()
                                updatedLocalFiles = getLocalUserFilesAsPrefixMap()
                                hasLocalChanges = hasHashConflicts(updatedLocalFiles, appFileListChange)
                            } while (hasLocalChanges && retries++ < MAX_USER_FILE_RETRIES)

                            if (hasLocalChanges) {
                                Log.e("SteamService", "Failed to download latest user files after $MAX_USER_FILE_RETRIES tries")
                                syncResult = SyncResult.DownloadFail
                                return@async PostSyncInfo(syncResult)
                            }

                            steamData.appFileChangeLists[appId] = updatedLocalFiles.map { it.value }.flatten()
                            steamData.appChangeNumbers[appId] = cloudAppChangeNumber
                            saveSteamData()
                        } else {
                            // TODO: conflict resolution
                            Log.d("SteamService", "Found local changes and new cloud user files, conflict resolution...")
                            syncResult = SyncResult.Conflict
                            remoteTimestamp = appFileListChange.files.map { it.timestamp.time }.max()
                            localTimestamp = allLocalUserFiles.map { it.timestamp }.max()
                        }
                        // val cloudFileListChange = steamCloud.getAppFileListChange(appId, cloudAppChangeNumber)

                        // Log.d("SteamService", "App change number 0")
                        // printFileChangeList(fullCloudFileList)
                        // val hasConflicts2 = hasHashConflicts(localUserFiles, fullCloudFileList)
                        // Log.d("SteamService", "Conflicts between $localAppChangeNumber and 0: $hasConflicts2")

                        // Log.d("SteamService", "App change number $localAppChangeNumber")
                        // printFileChangeList(appFileListChange)
                        // val hasConflicts = hasHashConflicts(localUserFiles, appFileListChange)
                        // Log.d("SteamService", "Conflicts between $localAppChangeNumber and $localAppChangeNumber: $hasConflicts")

                        // Log.d("SteamService", "App change number $cloudAppChangeNumber")
                        // printFileChangeList(cloudFileListChange)
                        // val hasConflicts3 = hasHashConflicts(localUserFiles, cloudFileListChange)
                        // Log.d("SteamService", "Conflicts between $localAppChangeNumber and $cloudAppChangeNumber: $hasConflicts3")

                        // if (!localConflicts) {

                        // }
                    } else {
                        // our last scenario is if the change number we have is greater than
                        // the change number from the cloud. This scenario should not happen, I
                        // believe, since we get the new app change number after having downloaded
                        // or uploaded from/to the cloud, so we should always be either behind or
                        // on par with the cloud change number, never ahead
                        Log.e("SteamService", "Local change number greater than cloud $localAppChangeNumber > $cloudAppChangeNumber")
                        syncResult = SyncResult.UnknownFail
                    }

                    postSyncInfo = PostSyncInfo(
                        syncResult = syncResult,
                        remoteTimestamp = remoteTimestamp,
                        localTimestamp = localTimestamp,
                        uploadsRequired = uploadsRequired,
                        uploadsCompleted = uploadsCompleted,
                    )
                }
            }

            postSyncInfo
        }

        fun getAvatarURL(avatarHash: String): String {
            return avatarHash.ifEmpty { null }
                ?.takeIf { str -> str.isNotEmpty() && !str.all { it == '0' } }
                ?.let { "${AVATAR_BASE_URL}${it.substring(0, 2)}/${it}_full.jpg" }
                ?: MISSING_AVATAR_URL
        }

        fun printAllKeyValues(parent: KeyValue, depth: Int = 0) {
            var tabString = ""
            for (i in 0..depth) {
                tabString += "\t"
            }

            if (parent.children.isNotEmpty()) {
                Log.d("SteamService", "$tabString${parent.name}")
                for (child in parent.children) {
                    printAllKeyValues(child, depth + 1)
                }
            } else {
                Log.d("SteamService", "$tabString${parent.name}: ${parent.value}")
            }
        }

        private fun login(
            username: String,
            accessToken: String? = null,
            refreshToken: String? = null,
            password: String? = null,
            shouldRememberPassword: Boolean = false,
            twoFactorAuth: String? = null,
            emailAuth: String? = null,
            clientId: Long? = null,
        ) {
            val steamUser = instance!!._steamUser!!

            Log.d(
                "SteamService",
                "Login Information\n\tUsername: $username\n\tAccessToken: $accessToken\n\tRefreshToken: $refreshToken\n\tPassword: $password\n\tShouldRememberPass: $shouldRememberPassword\n\tTwoFactorAuth: $twoFactorAuth\n\tEmailAuth: $emailAuth"
            )

            _steamData!!.accountName = username
            if ((password != null && shouldRememberPassword) || refreshToken != null) {
                if (password != null)
                    _steamData!!.password = password
                if (accessToken != null) {
                    _steamData!!.password = null
                    _steamData!!.accessToken = accessToken
                }
                if (refreshToken != null) {
                    _steamData!!.password = null
                    _steamData!!.refreshToken = refreshToken
                }
                if (clientId != null) {
                    _steamData!!.clientId = clientId
                }
            }

            isLoggingIn = true
            PluviaApp.events.emit(SteamEvent.LogonStarted(username))
            steamUser.logOn(
                LogOnDetails(
                    // Steam strips all non-ASCII characters from usernames and passwords
                    // source: https://github.com/steevp/UpdogFarmer/blob/8f2d185c7260bc2d2c92d66b81f565188f2c1a0e/app/src/main/java/com/steevsapps/idledaddy/LoginActivity.java#L166C9-L168C104
                    // more: https://github.com/winauth/winauth/issues/368#issuecomment-224631002
                    username = SteamUtils.removeSpecialChars(username).trim(),
                    password = if (password != null) SteamUtils.removeSpecialChars(password)
                        .trim() else null,
                    shouldRememberPassword = shouldRememberPassword,
                    twoFactorCode = twoFactorAuth,
                    authCode = emailAuth,
                    accessToken = refreshToken,
                    // Set LoginID to a non-zero value if you have another client connected using the same account,
                    // the same private ip, and same public ip.
                    // source: https://github.com/Longi94/JavaSteam/blob/08690d0aab254b44b0072ed8a4db2f86d757109b/javasteam-samples/src/main/java/in/dragonbra/javasteamsamples/_000_authentication/SampleLogonAuthentication.java#L146C13-L147C56
                    loginID = LOGIN_ID
                )
            )
        }

        fun startLoginWithCredentials(
            username: String,
            password: String,
            shouldRememberPassword: Boolean,
            authenticator: IAuthenticator,
        ) {
            Log.d("SteamService", "Logging in via credentials.")
            CoroutineScope(Dispatchers.IO).launch {
                val steamClient = instance!!._steamClient
                if (steamClient != null) {
                    val authDetails = AuthSessionDetails().apply {
                        this.username = username.trim()
                        this.password = password.trim()
                        this.persistentSession = shouldRememberPassword
                        this.authenticator = authenticator
                    }

                    val authSession = steamClient.authentication
                        .beginAuthSessionViaCredentials(authDetails)

                    PluviaApp.events.emit(SteamEvent.LogonStarted(username))

                    val pollResult = authSession.pollingWaitForResult()

                    if (pollResult.accountName.isNotEmpty() && pollResult.refreshToken.isNotEmpty()) {
                        login(
                            clientId = authSession.clientID,
                            username = pollResult.accountName,
                            accessToken = pollResult.accessToken,
                            refreshToken = pollResult.refreshToken,
                            shouldRememberPassword = shouldRememberPassword,
                        )
                    }
                } else {
                    Log.e("SteamService", "Could not logon: Failed to connect to Steam")
                    PluviaApp.events.emit(SteamEvent.LogonEnded(username, LoginResult.Failed))
                }
            }
        }

        fun startLoginWithQr() {
            Log.d("SteamService", "Logging in via QR.")
            CoroutineScope(Dispatchers.IO).launch {
                val steamClient = instance!!._steamClient
                if (steamClient != null) {
                    isWaitingForQRAuth = true

                    val authSession = steamClient.authentication
                        .beginAuthSessionViaQR(AuthSessionDetails())

                    // Steam will periodically refresh the challenge url, this callback allows you to draw a new qr code.
                    authSession.challengeUrlChanged = instance
                    PluviaApp.events.emit(SteamEvent.QrChallengeReceived(authSession.challengeUrl))

                    Log.d(
                        "SteamService",
                        "PollingInterval: ${authSession.pollingInterval.toLong()}"
                    )
                    var authPollResult: AuthPollResult? = null
                    while (isWaitingForQRAuth && authPollResult == null) {
                        try {
                            authPollResult = authSession.pollAuthSessionStatus()
                        } catch (e: Exception) {
                            Log.w("SteamService", "Poll auth session status error: $e")
                            break
                        }

                        if (authPollResult != null) {
                            Log.d(
                                "SteamService",
                                "AccessToken: ${authPollResult.accessToken}\nAccountName: ${authPollResult.accountName}\nRefreshToken: ${authPollResult.refreshToken}\nNewGuardData: ${authPollResult.newGuardData ?: "No new guard data"}"
                            )
                        } else {
                            // Log.d("SteamService", "AuthPollResult is null")
                        }

                        delay(authSession.pollingInterval.toLong())
                    }

                    isWaitingForQRAuth = false
                    PluviaApp.events.emit(SteamEvent.QrAuthEnded(authPollResult != null))

                    // there is a chance qr got cancelled and there is no authPollResult
                    if (authPollResult != null) {
                        login(
                            clientId = authSession.clientID,
                            username = authPollResult.accountName,
                            accessToken = authPollResult.accessToken,
                            refreshToken = authPollResult.refreshToken
                        )
                    }
                } else {
                    Log.e("SteamService", "Could not start QR logon: Failed to connect to Steam")
                    PluviaApp.events.emit(SteamEvent.QrAuthEnded(false))
                }
            }
        }

        fun stopLoginWithQr() {
            Log.d("SteamService", "Stopping QR polling")
            isWaitingForQRAuth = false
        }

        fun logOut() {
            CoroutineScope(Dispatchers.Default).launch {
                isConnected = false
                // isLoggingOut = true
                performLogOffDuties()
                val steamUser = instance!!._steamUser!!
                steamUser.logOff()
            }
        }

        private fun clearUserData() {
            _steamData!!.cellId = 0
            _steamData!!.accountName = null
            _steamData!!.accessToken = null
            _steamData!!.refreshToken = null
            _steamData!!.password = null
            _steamData!!.clientId = null
            _steamData!!.appChangeNumbers.clear()
            saveSteamData()
            isLoggingIn = false
        }

        private fun performLogOffDuties() {
            val username = _steamData!!.accountName
            clearUserData()
            PluviaApp.events.emit(SteamEvent.LoggedOut(username))
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // to view log messages in logcat
        LogManager.addListener(DefaultLogListener())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            loadSteamData()

            Log.d("SteamService", "Using server list path: $serverListPath")
            val configuration = SteamConfiguration.create {
                it.withProtocolTypes(PROTOCOL_TYPES)
                it.withCellID(_steamData!!.cellId)
                it.withServerListProvider(FileServerListProvider(File(serverListPath)))
                it.withManifestProvider(FileManifestProvider(File(depotManifestsPath)))
            }

            // create our steam client instance
            _steamClient = SteamClient(configuration)

            // remove callbacks we're not using.
            _steamClient!!.removeHandler(SteamGameServer::class.java)
            _steamClient!!.removeHandler(SteamMasterServer::class.java)
            _steamClient!!.removeHandler(SteamWorkshop::class.java)
            _steamClient!!.removeHandler(SteamScreenshots::class.java)
            _steamClient!!.removeHandler(SteamUserStats::class.java)

            // create the callback manager which will route callbacks to function calls
            _callbackManager = CallbackManager(_steamClient!!)
            _unifiedMessages = _steamClient!!.getHandler(SteamUnifiedMessages::class.java)

            // get the different handlers to be used throughout the service
            _steamUser = _steamClient!!.getHandler(SteamUser::class.java)
            _steamApps = _steamClient!!.getHandler(SteamApps::class.java)
            _steamFriends = _steamClient!!.getHandler(SteamFriends::class.java)
            _steamCloud = _steamClient!!.getHandler(SteamCloud::class.java)

            // subscribe to the callbacks we are interested in
            _callbackSubscriptions.add(
                _callbackManager!!.subscribe(
                    ConnectedCallback::class.java,
                    this::onConnected
                )
            )
            _callbackSubscriptions.add(
                _callbackManager!!.subscribe(
                    DisconnectedCallback::class.java,
                    this::onDisconnected
                )
            )
            _callbackSubscriptions.add(
                _callbackManager!!.subscribe(
                    LoggedOnCallback::class.java,
                    this::onLoggedOn
                )
            )
            _callbackSubscriptions.add(
                _callbackManager!!.subscribe(
                    LoggedOffCallback::class.java,
                    this::onLoggedOff
                )
            )
            _callbackSubscriptions.add(
                _callbackManager!!.subscribe(
                    PersonaStatesCallback::class.java,
                    this::onPersonaStateReceived
                )
            )
            _callbackSubscriptions.add(
                _callbackManager!!.subscribe(
                    LicenseListCallback::class.java,
                    this::onLicenseList
                )
            )
            _callbackSubscriptions.add(
                _callbackManager!!.subscribe(
                    PICSProductInfoCallback::class.java,
                    this::onPICSProductInfo
                )
            )
            _callbackSubscriptions.add(
                _callbackManager!!.subscribe(
                    NicknameListCallback::class.java,
                    this::onNicknameList
                )
            )
            _callbackSubscriptions.add(
                _callbackManager!!.subscribe(
                    FriendsListCallback::class.java,
                    this::onFriendsList
                )
            )

            isRunning = true

            // we should use Dispatchers.IO here since we are running a sleeping/blocking function
            // "The idea is that the IO dispatcher spends a lot of time waiting (IO blocked),
            // while the Default dispatcher is intended for CPU intensive tasks, where there
            // is little or no sleep."
            // source: https://stackoverflow.com/a/59040920
            CoroutineScope(Dispatchers.IO).launch {
                while (isRunning) {
                    // Log.d("SteamService", "runWaitCallbacks")
                    try {
                        _callbackManager!!.runWaitCallbacks(1000L)
                    } catch (e: Exception) {
                        Log.e("SteamService", "runWaitCallbacks failed: $e")
                    }
                }
            }

            connectToSteam()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        CoroutineScope(Dispatchers.IO).launch {
            stop()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectToSteam() {
        isConnecting = true
        CoroutineScope(Dispatchers.Default).launch {
            // this call errors out if run on the main thread
            _steamClient!!.connect()

            delay(5000)
            if (!isConnected) {
                Log.w(
                    "SteamService",
                    "Failed to connect to Steam, marking endpoint bad and force disconnecting"
                )
                try {
                    _steamClient!!.servers.tryMark(
                        _steamClient!!.currentEndpoint,
                        PROTOCOL_TYPES,
                        ServerQuality.BAD
                    )
                } catch (e: Exception) {
                    Log.e("SteamService", "Failed to mark endpoint as bad: $e")
                }
                try {
                    _steamClient!!.disconnect()
                } catch (e: Exception) {
                    Log.e("SteamService", "There was an issue when disconnecting: $e")
                }
            }
        }
    }

    private suspend fun stop() {
        Log.d("SteamService", "Stopping Steam service")
        if (_steamClient != null && _steamClient!!.isConnected) {
            isStopping = true
            _steamClient!!.disconnect()
            while (isStopping) {
                delay(200L)
            }
            // the reason we don't clearValues() here is because the onDisconnect
            // callback does it for us
        } else {
            clearValues()
        }
    }

    private fun clearValues() {
        _loginResult = LoginResult.Failed
        isRunning = false
        isConnected = false
        isConnecting = false
        isLoggingIn = false
        isWaitingForQRAuth = false
        isReceivingLicenseList = false
        isRequestingPkgInfo = false
        isRequestingAppInfo = false

        _steamData = SteamData(
            appInstallPath = defaultAppInstallPath,
            appStagingPath = defaultAppStagingPath
        )
        _steamClient = null
        _steamUser = null
        _steamApps = null
        _steamFriends = null
        _steamCloud = null

        for (subscription in _callbackSubscriptions) {
            subscription.close()
        }

        _callbackSubscriptions.clear()
        _callbackManager = null

        _unifiedMessages = null
        _unifiedChat = null

        packageInfo.clear()
        appInfo.clear()

        isStopping = false
        retryAttempt = 0

        PluviaApp.events.clearAllListenersOf<SteamEvent<Any>>()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onConnected(callback: ConnectedCallback) {
        Log.d("SteamService", "Connected to Steam")
        retryAttempt = 0
        isConnecting = false
        isConnected = true

        var isAutoLoggingIn = false

        loadSteamData()

        if (_steamData!!.accountName != null &&
            (_steamData!!.refreshToken != null || _steamData!!.password != null)
        ) {
            isAutoLoggingIn = true
            login(
                username = _steamData!!.accountName!!,
                refreshToken = _steamData!!.refreshToken,
                password = _steamData!!.password,
                shouldRememberPassword = _steamData!!.password != null
            )
        }

        PluviaApp.events.emit(SteamEvent.Connected(isAutoLoggingIn))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onDisconnected(callback: DisconnectedCallback) {
        Log.d("SteamService", "Disconnected from Steam")
        isConnected = false
        if (!isStopping && retryAttempt < MAX_RETRY_ATTEMPTS) {
            retryAttempt++
            Log.d("SteamService", "Attempting to reconnect (retry $retryAttempt)")
            // isLoggingOut = false
            connectToSteam()
        } else {
            PluviaApp.events.emit(SteamEvent.Disconnected)
            clearValues()
            stopSelf()
        }
    }

    private fun reconnect() {
        isConnected = false
        isConnecting = true
        PluviaApp.events.emit(SteamEvent.Disconnected)
        _steamClient!!.disconnect()
    }

    /**
     * Request a fresh state of Friend's PersonaStates
     */
    private fun refreshPersonaStates() {
        val request = CChat_RequestFriendPersonaStates_Request.newBuilder().build()
        _unifiedChat?.requestFriendPersonaStates(request)
    }

    private fun onLoggedOn(callback: LoggedOnCallback) {
        Log.d("SteamService", "Logged onto Steam: ${callback.result}")
        val username = _steamData!!.accountName

        when (callback.result) {
            EResult.TryAnotherCM -> {
                reconnect()
            }

            EResult.OK -> {
                // save the current cellid somewhere. if we lose our saved server list, we can use this when retrieving
                // servers from the Steam Directory.
                _steamData!!.cellId = callback.cellID
                saveSteamData()

                // Create Unified Handlers
                _unifiedChat = _unifiedMessages!!.createService(Chat::class.java)

                // retrieve persona data of logged in user
                requestUserPersona()

                // since we automatically receive the license list from steam on log on
                isReceivingLicenseList = true

                // TODO: Preference last known status?
                // Tell steam we're online, this allows friends to update.
                _steamFriends?.setPersonaState(EPersonaState.Online)

                _loginResult = LoginResult.Success
            }

            else -> {
                clearUserData()
                _loginResult = LoginResult.Failed
                reconnect()
            }
        }

        PluviaApp.events.emit(SteamEvent.LogonEnded(username, _loginResult))
        isLoggingIn = false
    }

    private fun onLoggedOff(callback: LoggedOffCallback) {
        Log.d("SteamService", "Logged off of Steam: ${callback.result}")
        performLogOffDuties()
    }

    override fun onChanged(qrAuthSession: QrAuthSession?) {
        Log.d("SteamService", "QR code changed: ${qrAuthSession?.challengeUrl}")
        if (qrAuthSession != null) {
            PluviaApp.events.emit(SteamEvent.QrChallengeReceived(qrAuthSession.challengeUrl))
        }
    }

    private fun onNicknameList(callback: NicknameListCallback) {
        Log.d("SteamService", "Nickname list called: ${callback.nicknames.size}")
        dbScope.launch {
            db.withTransaction {
                db.steamFriendDao().clearAllNicknames()
                db.steamFriendDao().updateNicknames(callback.nicknames)
            }
        }
    }

    private fun onFriendsList(callback: FriendsListCallback) {
        Log.d("SteamService", "onFriendsList ${callback.friendList.size}")
        dbScope.launch {
            db.withTransaction {
                callback.friendList.filter { friend ->
                    friend.steamID.isIndividualAccount
                }.forEach { filteredFriend ->
                    val friendId = filteredFriend.steamID.convertToUInt64()
                    val friend = db.steamFriendDao().findFriend(friendId).first()

                    if (friend == null) {
                        // Not in the DB, create them.
                        val friendToAdd = SteamFriend(
                            id = filteredFriend.steamID.convertToUInt64(),
                            relation = filteredFriend.relationship.code()
                        )

                        db.steamFriendDao().insert(friendToAdd)
                    } else {
                        // In the DB, update them.
                        db.steamFriendDao().update(
                            friend.copy(relation = filteredFriend.relationship.code())
                        )
                    }
                }

                // Add logged in account if we don't exist yet.
                val selfId = getUserSteamId()!!.convertToUInt64()
                val self = db.steamFriendDao().findFriend(selfId).first()
                if (self == null) {
                    db.steamFriendDao().insert(SteamFriend(id = selfId))
                }
            }

            // NOTE: Our UI could load too quickly on fresh database, our icon will be "?"
            //  unless relaunched or we nav to a new screen.
            refreshPersonaStates()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun onPersonaStateReceived(callback: PersonaStatesCallback) {
        // Ignore accounts that arent individuals
        if (callback.friendID.isIndividualAccount.not()) {
            return
        }

        // Ignore states where the name is blank.
        if (callback.name.isEmpty()) {
            return
        }

        Log.d("SteamService", "Persona state received: ${callback.name}")

        dbScope.launch {
            db.withTransaction {
                val id = callback.friendID.convertToUInt64()
                val friend = db.steamFriendDao().findFriend(id).first()

                if (friend == null) {
                    Log.w("SteamService", "onPersonaStateReceived: " +
                            "failed to find friend to update: $id")
                    return@withTransaction
                }

                db.steamFriendDao().update(
                    friend.copy(
                        statusFlags = EClientPersonaStateFlag.code(callback.statusFlags),
                        state = callback.state.code(),
                        stateFlags = EPersonaStateFlag.code(callback.stateFlags),
                        gameAppID = callback.gameAppID,
                        gameID = callback.gameID.convertToUInt64(),
                        gameName = callback.gameName,
                        gameServerIP = NetHelpers.getIPAddress(callback.gameServerIP),
                        gameServerPort = callback.gameServerPort,
                        queryPort = callback.queryPort,
                        sourceSteamID = callback.sourceSteamID.convertToUInt64(),
                        gameDataBlob = callback.gameDataBlob.decodeToString(),
                        name = callback.name,
                        avatarHash = callback.avatarHash.toHexString(),
                        lastLogOff = callback.lastLogOff.time,
                        lastLogOn = callback.lastLogOn.time,
                        clanRank = callback.clanRank,
                        clanTag = callback.clanTag,
                        onlineSessionInstances = callback.onlineSessionInstances,
                    )
                )
            }
        }

        // Send off a status if we change states.
        if (callback.friendID == getUserSteamId()) {
            Log.d("SteamService", "Emitting PersonaStateReceived")
            dbScope.launch {
                val id = callback.friendID.convertToUInt64()
                val friend = db.steamFriendDao().findFriend(id).first()
                PluviaApp.events.emit(SteamEvent.PersonaStateReceived(friend))
            }
        }
    }

    private fun onLicenseList(callback: LicenseListCallback) {
        Log.d("SteamService", "Received License List ${callback.result}")
        if (callback.result == EResult.OK) {
            for (i in callback.licenseList.indices) {
                val license = callback.licenseList[i]
                packageInfo[license.packageID] = PackageInfo(
                    packageId = license.packageID,
                    receiveIndex = i,
                    ownerAccountId = license.ownerAccountID,
                    lastChangeNumber = license.lastChangeNumber,
                    accessToken = license.accessToken,
                    territoryCode = license.territoryCode,
                    licenseFlags = license.licenseFlags,
                    licenseType = license.licenseType,
                    paymentMethod = license.paymentMethod,
                    purchaseCountryCode = license.purchaseCode,
                    appIds = IntArray(0),
                    depotIds = IntArray(0),
                )
            }

            isRequestingPkgInfo = true
            _steamApps!!.picsGetProductInfo(
                apps = emptyList(),
                packages = callback.licenseList.map { PICSRequest(it.packageID, it.accessToken) }
            )
        }

        isReceivingLicenseList = false
    }

    private fun onPICSProductInfo(callback: PICSProductInfoCallback) {
        // Log.d("SteamService", "Received PICSProductInfo")
        if (callback.packages.isNotEmpty()) {
            for (pkg in callback.packages.values) {
                // Log.d("SteamService", "Received pkg ${pkg.id}")
                packageInfo[pkg.id]?.let { pi ->
                    pi.appIds =
                        pkg.keyValues["appids"].children.map { it.asInteger() }.toIntArray()
                    pi.depotIds =
                        pkg.keyValues["depotids"].children.map { it.asInteger() }.toIntArray()
                }
            }

            isRequestingPkgInfo = false
            isRequestingAppInfo = true
            _steamApps?.picsGetProductInfo(
                apps = packageInfo.values
                    .flatMap { it.appIds.asIterable() }
                    .map { PICSRequest(it) },
                packages = emptyList()
            )
        }

        if (callback.apps.isNotEmpty()) {
            val apps = callback.apps.values.toTypedArray()
            for (i in apps.indices) {
                val app = apps[i]
                val pkg = packageInfo.values.firstOrNull { it.appIds.contains(app.id) }
                // Log.d("SteamService", "Received app ${app.id}")
                val generateManifest: (List<KeyValue>) -> Map<String, ManifestInfo> = {
                    val output = mutableMapOf<String, ManifestInfo>()
                    for (manifest in it) {
                        output[manifest.name] = ManifestInfo(
                            name = manifest.name,
                            gid = manifest["gid"].asLong(),
                            size = manifest["size"].asLong(),
                            download = manifest["download"].asLong(),
                        )
                    }
                    output
                }

                val toLangImgMap: (List<KeyValue>) -> Map<Language, String> = { keyValues ->
                    keyValues.map {
                        val language: Language = try {
                            Language.valueOf(it.name)
                        } catch (_: Exception) {
                            Log.d("SteamService", "Language ${it.name} does not exist in enum")
                            Language.unknown
                        }
                        Pair(language, it.value)
                    }.filter { it.first != Language.unknown }.toMap()
                }
                val launchConfigs = app.keyValues["config"]["launch"].children
                appInfo[app.id] = AppInfo(
                    appId = app.id,
                    receiveIndex = packageInfo.values
                        .filter { it.receiveIndex < (pkg?.receiveIndex ?: Int.MAX_VALUE) }
                        .fold(initial = 0) { accum, pkgInfo -> accum + pkgInfo.appIds.size } + i,
                    packageId = pkg?.packageId ?: INVALID_PKG_ID,
                    depots = app.keyValues["depots"].children
                        .filter { currentDepot ->
                            currentDepot.name.toIntOrNull() != null
                        }.map { currentDepot ->
                            val depotId = currentDepot.name.toInt()

                            // val currentDepot = app.keyValues["depots"]["$depotId"]
                            val manifests = generateManifest(currentDepot["manifests"].children)
                            val encryptedManifests = generateManifest(
                                currentDepot["encryptedManifests"].children
                            )

                            Pair(depotId, DepotInfo(
                                depotId = depotId,
                                depotFromApp = currentDepot["depotfromapp"].asInteger(INVALID_APP_ID),
                                sharedInstall = currentDepot["sharedinstall"].asBoolean(),
                                osList = OS.from(currentDepot["config"]["oslist"].value),
                                osArch = OSArch.from(currentDepot["config"]["osarch"].value),
                                manifests = manifests,
                                encryptedManifests = encryptedManifests,
                            ))
                        }.toMap(),
                    branches = app.keyValues["branches"].children.map { Pair(
                        it.name,
                        BranchInfo(
                            name = it.name,
                            buildId = it["buildid"].asLong(),
                            pwdRequired = it["pwdrequired"].asBoolean(),
                            timeUpdated = java.util.Date(it["timeupdated"].asLong() * 1000L)
                        )
                    ) }.toMap(),
                    name = app.keyValues["common"]["name"].value ?: "",
                    type = AppType.valueOf(
                        app.keyValues["common"]["type"].value?.lowercase() ?: "invalid"
                    ),
                    osList = OS.from(app.keyValues["common"]["oslist"].value),
                    releaseState = ReleaseState.valueOf(
                        app.keyValues["common"]["releasestate"].value ?: "released"
                    ),
                    metacriticScore = app.keyValues["common"]["metacritic_score"].asByte(),
                    metacriticFullUrl = app.keyValues["common"]["metacritic_fullurl"].value ?: "",
                    logoHash = app.keyValues["common"]["logo"].value ?: "",
                    logoSmallHash = app.keyValues["common"]["logo_small"].value ?: "",
                    iconHash = app.keyValues["common"]["icon"].value ?: "",
                    clientIconHash = app.keyValues["common"]["clienticon"].value ?: "",
                    clientTgaHash = app.keyValues["common"]["clienttga"].value ?: "",
                    smallCapsule = toLangImgMap(app.keyValues["common"]["small_capsule"].children),
                    headerImage = toLangImgMap(app.keyValues["common"]["header_image"].children),
                    libraryAssets = LibraryAssetsInfo(
                        libraryCapsule = LibraryCapsuleInfo(
                            image = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_capsule"]["image"].children),
                            image2x = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_capsule"]["image2x"].children)
                        ),
                        libraryHero = LibraryHeroInfo(
                            image = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_hero"]["image"].children),
                            image2x = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_hero"]["image2x"].children)
                        ),
                        libraryLogo = LibraryLogoInfo(
                            image = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_logo"]["image"].children),
                            image2x = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_logo"]["image2x"].children)
                        )
                    ),
                    primaryGenre = app.keyValues["common"]["primary_genre"].asBoolean(),
                    reviewScore = app.keyValues["common"]["review_score"].asByte(),
                    reviewPercentage = app.keyValues["common"]["review_percentage"].asByte(),
                    controllerSupport = ControllerSupport.valueOf(
                        app.keyValues["common"]["controller_support"].value ?: "none"
                    ),
                    demoOfAppId = app.keyValues["common"]["extended"]["demoofappid"].asInteger(),
                    developer = app.keyValues["common"]["extended"]["developer"].value ?: "",
                    publisher = app.keyValues["common"]["extended"]["publisher"].value ?: "",
                    homepageUrl = app.keyValues["common"]["extended"]["homepage"].value ?: "",
                    gameManualUrl = app.keyValues["common"]["extended"]["gamemanualurl"].value ?: "",
                    loadAllBeforeLaunch = app.keyValues["common"]["extended"]["loadallbeforelaunch"].asBoolean(),
                    // dlcAppIds = (app.keyValues["common"]["extended"]["listofdlc"].value).Split(",").Select(uint.Parse).ToArray(),
                    dlcAppIds = IntArray(0),
                    isFreeApp = app.keyValues["common"]["extended"]["isfreeapp"].asBoolean(),
                    dlcForAppId = app.keyValues["common"]["extended"]["dlcforappid"].asInteger(),
                    mustOwnAppToPurchase = app.keyValues["common"]["extended"]["mustownapptopurchase"].asInteger(),
                    dlcAvailableOnStore = app.keyValues["common"]["extended"]["dlcavailableonstore"].asBoolean(),
                    optionalDlc = app.keyValues["common"]["extended"]["optionaldlc"].asBoolean(),
                    gameDir = app.keyValues["common"]["extended"]["gamedir"].value ?: "",
                    installScript = app.keyValues["common"]["extended"]["installscript"].value ?: "",
                    noServers = app.keyValues["common"]["extended"]["noservers"].asBoolean(),
                    order = app.keyValues["common"]["extended"]["order"].asBoolean(),
                    primaryCache = app.keyValues["common"]["extended"]["primarycache"].asInteger(),
                    // validOSList = app.keyValues["common"]["extended"]["validoslist"].value!.Split(",").Select(Enum.Parse<OS>).Aggregate((os1, os2) => os1 | os2),
                    validOSList = EnumSet.of(OS.none),
                    thirdPartyCdKey = app.keyValues["common"]["extended"]["thirdpartycdkey"].asBoolean(),
                    visibleOnlyWhenInstalled = app.keyValues["common"]["extended"]["visibleonlywheninstalled"].asBoolean(),
                    visibleOnlyWhenSubscribed = app.keyValues["common"]["extended"]["visibleonlywhensubscribed"].asBoolean(),
                    launchEulaUrl = app.keyValues["common"]["extended"]["launcheula"].value ?: "",
                    requireDefaultInstallFolder = app.keyValues["common"]["config"]["requiredefaultinstallfolder"].asBoolean(),
                    contentType = app.keyValues["common"]["config"]["contentType"].asInteger(),
                    installDir = app.keyValues["common"]["config"]["installdir"].value ?: "",
                    useLaunchCmdLine = app.keyValues["common"]["config"]["uselaunchcommandline"].asBoolean(),
                    launchWithoutWorkshopUpdates = app.keyValues["common"]["config"]["launchwithoutworkshopupdates"].asBoolean(),
                    useMms = app.keyValues["common"]["config"]["usemms"].asBoolean(),
                    installScriptSignature = app.keyValues["common"]["config"]["installscriptsignature"].value ?: "",
                    installScriptOverride = app.keyValues["common"]["config"]["installscriptoverride"].asBoolean(),
                    config = ConfigInfo(
                        installDir = app.keyValues["config"]["installdir"].value ?: "",
                        launch = launchConfigs.map {
                            LaunchInfo(
                                executable = it["executable"].value ?: "",
                                workingDir = it["workingdir"].value ?: "",
                                description = it["description"].value ?: "",
                                type = it["type"].value ?: "",
                                configOS = OS.from(it["config"]["oslist"].value),
                                configArch = OSArch.from(it["config"]["osarch"].value)
                            )
                        }.toTypedArray(),
                        steamControllerTemplateIndex = app.keyValues["config"]["steamcontrollertemplateindex"].asInteger(),
                        steamControllerTouchTemplateIndex = app.keyValues["config"]["steamcontrollertouchtemplateindex"].asInteger(),
                    ),
                    ufs = UFS(
                        quota = app.keyValues["ufs"]["quota"].asInteger(),
                        maxNumFiles = app.keyValues["ufs"]["maxnumfiles"].asInteger(),
                        saveFilePatterns = app.keyValues["ufs"]["savefiles"].children.map {
                            SaveFilePattern(
                                root = PathType.from(it["root"].value),
                                path = it["path"].value ?: "",
                                pattern = it["pattern"].value ?: ""
                            )
                        }.toTypedArray()
                    )
                )

                // // val isBaba = app.id == 736260
                // // val isNoita = app.id == 881100
                // // val isHades = app.id == 1145360
                // // val isCS2 = app.id == 730
                // // val isPsuedo = app.id == 2365810
                // // val isPathway = app.id == 546430
                // // val isSeaOfStars = app.id == 1244090
                // val isMessenger = app.id == 764790
                // if (isMessenger) {
                // 	Log.d("SteamService", "${app.id}: ${app.keyValues["common"]["name"].value}");
                // 	printAllKeyValues(app.keyValues)
                // 	// getPkgInfoOf(app.id)?.let {
                // 	// 	printAllKeyValues(it.original)
                //     // }
                // }
            }

            isRequestingAppInfo = false
            PluviaApp.events.emit(SteamEvent.AppInfoReceived)
        }
    }
}