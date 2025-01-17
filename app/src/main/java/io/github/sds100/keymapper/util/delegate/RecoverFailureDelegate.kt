package io.github.sds100.keymapper.util.delegate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.KeyboardUtils
import io.github.sds100.keymapper.util.PackageUtils
import io.github.sds100.keymapper.util.PermissionUtils
import io.github.sds100.keymapper.util.result.*
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 22/10/20.
 */

class RecoverFailureDelegate(
    keyPrefix: String,
    resultRegistry: ActivityResultRegistry,
    lifecycleOwner: LifecycleOwner,
    private val mOnSuccessfulRecover: () -> Unit
) {

    private val mStartActivityForResultLauncher =
        resultRegistry.register(
            "$keyPrefix.start_activity",
            lifecycleOwner,
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                mOnSuccessfulRecover.invoke()
            }
        }

    private val mRequestPermissionLauncher =
        resultRegistry.register(
            "$keyPrefix.request_permission",
            lifecycleOwner,
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                mOnSuccessfulRecover.invoke()
            }
        }

    fun recover(activity: FragmentActivity, failure: RecoverableFailure) {
        when (failure) {
            is PermissionDenied -> {
                when (failure.permission) {
                    Manifest.permission.WRITE_SETTINGS ->
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            PermissionUtils.requestWriteSettings(activity)
                        }

                    Manifest.permission.CAMERA ->
                        PermissionUtils.requestStandardPermission(mRequestPermissionLauncher, Manifest.permission.CAMERA)

                    Manifest.permission.BIND_DEVICE_ADMIN ->
                        PermissionUtils.requestDeviceAdmin(activity, mStartActivityForResultLauncher)

                    Manifest.permission.READ_PHONE_STATE ->
                        PermissionUtils.requestStandardPermission(
                            mRequestPermissionLauncher,
                            Manifest.permission.READ_PHONE_STATE
                        )

                    Manifest.permission.ACCESS_NOTIFICATION_POLICY ->
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            PermissionUtils.requestAccessNotificationPolicy(mStartActivityForResultLauncher)
                        }

                    Manifest.permission.WRITE_SECURE_SETTINGS ->
                        PermissionUtils.requestWriteSecureSettingsPermission(activity)

                    Constants.PERMISSION_ROOT -> PermissionUtils.requestRootPermission(activity)

                    else -> throw Exception("Don't know how to ask for permission ${failure.permission}")
                }
            }

            is GoogleAppNotFound -> recover(activity, AppNotFound(activity.str(R.string.google_app_package_name)))
            is AppNotFound -> PackageUtils.viewAppOnline(failure.packageName)

            is AppDisabled -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${failure.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                    activity.startActivity(this)
                }
            }

            is NoCompatibleImeEnabled -> KeyboardUtils.enableCompatibleInputMethods()
            is NoCompatibleImeChosen -> KeyboardUtils.chooseCompatibleInputMethod(activity)
        }
    }
}