package com.example.awesomephotoframe.permissions

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.awesomephotoframe.R


object PermissionsHelper {
    const val BLUETOOTH_LOCATION_PERMISSION_REQUEST_CODE = 1
    private const val GPS_ENABLE_REQUEST_CODE = 5

    /**
     * 現在の[Context]が指定された[Manifest.permission]を持っているかどうかを判定する
     *
     * @param permissionType チェックするパーミッションの種類
     * @return パーミッションが付与されている場合はtrue そうでなければfalse
     */
    private fun Context.hasPermission(permissionType: String): Boolean {
        Log.d("hasPermission",
            "${permissionType}==${ContextCompat.checkSelfPermission(this, permissionType) ==
                    PackageManager.PERMISSION_GRANTED}")
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * GPSがONかどうかを判定
     */
    fun isGPSEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * GPSをONにする
     */
    fun enableGPS(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.gps_dialog_title)
            .setMessage(R.string.gps_dialog_message)
            .setPositiveButton(R.string.gps_dialog_positive_button) { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                activity.startActivityForResult(intent, GPS_ENABLE_REQUEST_CODE)
            }
            .setNegativeButton(R.string.gps_dialog_negative_button) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 位置情報アクセスのパーミッションをユーザーにリクエストする
     *
     * @param activity リクエストを行うアクティビティ
     * @param requestCode リクエストを識別するためのコード
     */
    private fun requestLocationPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestLocationPermissions(activity, requestCode)
        } else {
            requestLocationPermissionPreQ(activity, requestCode)
        }
    }

    /**
     * Android10以降のデバイスで位置情報アクセスのパーミッションをリクエストする
     *
     * @param activity リクエストを行うアクティビティ
     * @param requestCode リクエストを識別するためのコード
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestLocationPermissions(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            requestCode
        )
    }

    /**
     * Android10以下のデバイスで位置情報アクセスのパーミッションをリクエストする
     *
     * @param activity リクエストを行うアクティビティ
     * @param requestCode リクエストを識別するためのコード
     */
    private fun requestLocationPermissionPreQ(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            requestCode
        )
    }

    /**
     * アプリ起動時に位置情報が許可されていることをチェックし、許可されていなければ、リクエストを出す
     */
    fun checkLocationPermissionAndRequestIfNotGranted(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!activity.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestLocationPermissionPreQ(activity, BLUETOOTH_LOCATION_PERMISSION_REQUEST_CODE)
            }
        } else {
            if (!activity.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestLocationPermissions(activity, BLUETOOTH_LOCATION_PERMISSION_REQUEST_CODE)
            }
        }
    }
}