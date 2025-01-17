package io.github.sds100.keymapper.util

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.result.FileAccessDenied
import io.github.sds100.keymapper.util.result.GenericFailure
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by sds100 on 17/06/2020.
 */
object BackupUtils {

    const val DEFAULT_AUTOMATIC_BACKUP_NAME = "keymapper_keymaps.json"

    //DON'T CHANGE THESE. Used for serialization and parsing.
    private const val NAME_KEYMAP_LIST = "keymap_list"
    private const val NAME_DEVICE_INFO = "device_info"

    val backupAutomatically: Boolean
        get() = AppPreferences.automaticBackupLocation.isNotBlank()

    suspend fun backup(outputStream: OutputStream,
                       keymapList: List<KeyMap>,
                       allDeviceInfo: List<DeviceInfo>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deferred = async(Dispatchers.IO) {
                //delete the contents of the file
                (outputStream as FileOutputStream).channel.truncate(0)
                val deviceInfoIdsToBackup = mutableSetOf<String>()

                keymapList.forEach { keymap ->
                    keymap.id = 0

                    keymap.trigger.keys.forEach { key ->
                        if (key.deviceId != Trigger.Key.DEVICE_ID_ANY_DEVICE
                            && key.deviceId != Trigger.Key.DEVICE_ID_THIS_DEVICE) {
                            deviceInfoIdsToBackup.add(key.deviceId)
                        }
                    }
                }

                val deviceInfoList = deviceInfoIdsToBackup.map { id -> allDeviceInfo.single { it.descriptor == id } }

                outputStream.bufferedWriter().use { bufferedWriter ->
                    val json = Gson().toJson(BackupModel(keymapList, deviceInfoList))

                    bufferedWriter.write(json)
                }
            }

            deferred.await()
        } catch (e: Exception) {
            return@withContext GenericFailure(e)
        }

        return@withContext Success(Unit)
    }

    suspend fun restore(inputStream: InputStream): Result<RestoreModel> = withContext(Dispatchers.IO) {
        try {
            inputStream.bufferedReader().use { bufferedReader ->

                val json = bufferedReader.readText()
                val parser = JsonParser()

                val gson = GsonBuilder()
                    .registerTypeAdapter(KeyMap.DESERIALIZER)
                    .registerTypeAdapter(Trigger.Key.DESERIALIZER)
                    .registerTypeAdapter(Trigger.DESERIALIZER)
                    .registerTypeAdapter(Action.DESERIALIZER)
                    .registerTypeAdapter(Constraint.DESERIALIZER)
                    .registerTypeAdapter(DeviceInfo.DESERIALIZER)
                    .registerTypeAdapter(Extra.DESERIALIZER).create()

                val rootElement = parser.parse(json)
                val keymapListJsonArray by rootElement.byArray(NAME_KEYMAP_LIST)
                val keymapList = gson.fromJson<List<KeyMap>>(keymapListJsonArray)

                val deviceInfoJsonArray by rootElement.byArray(NAME_DEVICE_INFO)
                val deviceInfoList = gson.fromJson<List<DeviceInfo>>(deviceInfoJsonArray)

                return@withContext Success(RestoreModel(keymapList, deviceInfoList))
            }

        } catch (e: Exception) {
            return@withContext GenericFailure(e)
        }
    }

    fun createFileName(): String {
        val formattedDate = FileUtils.createFileDate()
        return "keymaps_$formattedDate.json"
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getAutomaticBackupLocation(): Result<String> {
        val uri = Uri.parse(AppPreferences.automaticBackupLocation)

        return Success(uri.path!!)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun createAutomaticBackupOutputStream(context: Context): Result<OutputStream> {
        val uri = Uri.parse(AppPreferences.automaticBackupLocation)
        val contentResolver = context.contentResolver

        return try {
            val outputStream = contentResolver.openOutputStream(uri)!!

            Success(outputStream)
        } catch (e: Exception) {
            when (e) {
                is SecurityException -> FileAccessDenied()
                else -> GenericFailure(e)
            }
        }
    }

    class RestoreModel(val keymapList: List<KeyMap>, val deviceInfo: List<DeviceInfo>)

    private class BackupModel(@SerializedName(NAME_KEYMAP_LIST)
                              val keymapList: List<KeyMap>,

                              @SerializedName(NAME_DEVICE_INFO)
                              val deviceInfo: List<DeviceInfo>)
}