package com.example.awesomephotoframe

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.awesomephotoframe.data.repository.GooglePhotosApi
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.bumptech.glide.Glide
import com.example.awesomephotoframe.data.repository.MediaItem
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.awesomephotoframe.data.repository.PhotoRepository
import com.example.awesomephotoframe.viewmodel.MainViewModel
import com.example.awesomephotoframe.viewmodel.ViewModelFactory
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority



class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val RC_SIGN_IN = 9001
    }

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var ivPhotoFrame: ImageView
//    private lateinit var tvUser: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var lastDateString: String = ""
    private lateinit var viewModel: MainViewModel
    private val repository = PhotoRepository()

    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Lifecycle", "onCreate START")
        setContentView(R.layout.activity_main)

        ivPhotoFrame = findViewById(R.id.iv_photo_frame)
        tvDate = findViewById(R.id.tv_date)
        tvTime = findViewById(R.id.tv_time)

        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(repository)
        )[MainViewModel::class.java]

        viewModel.photoUrl.observe(this) { url ->
            if (url != null) {
                Glide.with(this)
                    .load(url)
                    .placeholder(ivPhotoFrame.drawable)
                    .fitCenter()
                    .into(ivPhotoFrame)
            }
        }

        MobileAds.initialize(this) {}

        val adView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        checkPremiumStatus(this) { isPremium ->
            if (isPremium) showPremiumUI() else showFreeUI()
        }

        // Google Sign-In 設定
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/photoslibrary.readonly"))
            .requestServerAuthCode(BuildConfig.GOOGLE_CLIENT_ID, false)
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)
            }
        }

        val billingManager = BillingManager(this)
        billingManager.startConnection {
            billingManager.queryPurchases { isPremium ->
                if (isPremium) {
                    showPremiumUI()
                } else {
                    showFreeUI()
                }
            }
        }

        findViewById<Button>(R.id.btn_remove_ads).setOnClickListener {
            launchPurchaseFlow(this)
        }

        updateDateTime() // 初期表示
        startClockUpdater() // 毎分更新（必要なら）

        Log.d("TEST", "Start permission check")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            Log.d("TEST", "Permission granted")
        } else {
            Log.d("TEST", "Permission NOT granted")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }

        // silent sign-in（ViewModel経由で処理）
        googleSignInClient.silentSignIn().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val account = task.result
                invalidateOptionsMenu()
                viewModel.handleSignInResult(account)
            } else {
                Log.w(TAG, "Silent sign-in failed", task.exception)
                invalidateOptionsMenu()
            }
        }

        val btnMenu = findViewById<ImageButton>(R.id.btn_menu)
        btnMenu.setOnClickListener { view ->
            showPopupMenu(view)
        }

    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.handleSignInResult(account)
        } catch (e: ApiException) {
            Log.w("SignIn", "Sign-in failed", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "位置情報パーミッションが許可されました")
                getCurrentLocation { lat, lon ->
                    Log.d("Location", "取得成功: lat=$lat, lon=$lon")
                    // 天気取得処理ここで呼んでもOK
                }
            } else {
                Log.w("Permission", "位置情報パーミッションが拒否されました")
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_in -> {
                signIn()
                true
            }
            R.id.action_sign_out -> {
                signOut()
                true
            }
            R.id.action_show_user -> {
                showUserInfoDialog()
                true
            }
            R.id.action_upgrade -> {
                launchPurchaseFlow(this); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showUserInfoDialog() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            val imageUrl = account.photoUrl?.toString()
            val dialogView = layoutInflater.inflate(R.layout.dialog_user_info, null)

            val ivUserIcon = dialogView.findViewById<ImageView>(R.id.iv_user_icon)
            val tvName = dialogView.findViewById<TextView>(R.id.tv_user_name)
            val tvEmail = dialogView.findViewById<TextView>(R.id.tv_user_email)

            tvName.text = account.displayName
            tvEmail.text = account.email
            // ✅ Glideでアイコン画像を表示
            if (imageUrl != null) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.baseline_assignment_ind_24) // 読み込み中やnull用に適当なdrawable
                    .error(R.drawable.baseline_assignment_ind_24) // 読み込み失敗用
                    .circleCrop() // 丸くしたい場合
                    .into(ivUserIcon)
            }

            AlertDialog.Builder(this)
                .setTitle("User Info")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show()
        } else {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(callback: (Double, Double) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                callback(location.latitude, location.longitude)
            } else {
                // fallback: force request update
                val locationRequest = LocationRequest.create().apply {
                    priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    interval = 0
                    numUpdates = 1
                }

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val newLoc = result.lastLocation
                        if (newLoc != null) {
                            callback(newLoc.latitude, newLoc.longitude)
                        } else {
                            Log.w("Location", "fallbackでも位置が取得できませんでした")
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }.addOnFailureListener {
            Log.e("Location", "位置情報の取得に失敗しました", it)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        menu?.findItem(R.id.action_sign_in)?.isVisible = account == null
        menu?.findItem(R.id.action_sign_out)?.isVisible = account != null
        menu?.findItem(R.id.action_show_user)?.isVisible = account != null
        return super.onPrepareOptionsMenu(menu)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            invalidateOptionsMenu()
            // デフォルト写真に戻す
            ivPhotoFrame.setImageResource(R.drawable.sample_photo)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    fun createPhotosApi(): GooglePhotosApi {
        return Retrofit.Builder()
            .baseUrl("https://photoslibrary.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GooglePhotosApi::class.java)
    }


    suspend fun fetchPhotosWithAccessToken(accessToken: String) {
        try {
            val api = createPhotosApi()
            val allItems = mutableListOf<MediaItem>()
            var pageToken: String? = null

            do {
                val response = api.listMediaItems("Bearer $accessToken", pageToken)
                response.mediaItems?.let { allItems.addAll(it) }
                pageToken = response.nextPageToken
            } while (pageToken != null)

            Log.d("PhotosAPI", "Total media items fetched: ${allItems.size}")

            if (allItems.isNotEmpty()) {
//                val imageUrl = allItems.random().baseUrl + "=w2048-h2048" // 安牌
                val imageUrl = allItems.random().baseUrl + "=w4096-h4096" // 高画質

                withContext(Dispatchers.Main) {
                    Glide.with(this@MainActivity)
                        .load(imageUrl)
                        .placeholder(ivPhotoFrame.drawable) // 現在表示中の画像をプレースホルダーとして使う
                        .fitCenter()
                        .into(ivPhotoFrame)
                }
            }

        } catch (e: Exception) {
            Log.e("PhotosAPI", "Failed to fetch photos", e)
        }
    }



    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_main, popup.menu)

        val account = GoogleSignIn.getLastSignedInAccount(this)

        // 🔽 ユーザー情報を表示専用のメニュー項目にセット
        val userItem = popup.menu.findItem(R.id.action_user_info)
        if (account != null) {
            userItem.title = "${account.displayName}"
            userItem.isVisible = true
        } else {
            userItem.title = "Not signed in"
            userItem.isVisible = true
        }

        // 他のメニュー表示切り替え
        popup.menu.findItem(R.id.action_sign_in)?.isVisible = account == null
        popup.menu.findItem(R.id.action_sign_out)?.isVisible = account != null
        popup.menu.findItem(R.id.action_show_user)?.isVisible = account != null

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sign_in -> {
                    signIn(); true
                }
                R.id.action_sign_out -> {
                    signOut(); true
                }
                R.id.action_show_user -> {
                    showUserInfoDialog(); true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun updateDateTime() {
        val currentTime = Calendar.getInstance()

        // 時刻の表示（例: 8:13 PM）
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeText = timeFormat.format(currentTime.time)
        tvTime.text = timeText

        // 日付の表示（例: Thu, Mar 28）
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val newDate = dateFormat.format(currentTime.time)
        Log.d("TimeCheck", "Formatted: $newDate $timeText")
        if (newDate != lastDateString) {
            tvDate.text = newDate
            lastDateString = newDate
        }
    }

    private fun startClockUpdater() {
        handler.post(object : Runnable {
            override fun run() {
                updateDateTime()
                // 1分ごとに再実行（60,000ミリ秒）
                handler.postDelayed(this, 60_000L)
            }
        })
    }

    private fun startPhotoUpdater(accessToken: String) {
        handler.post(object : Runnable {
            override fun run() {
                CoroutineScope(Dispatchers.Main).launch {
                    fetchPhotosWithAccessToken(accessToken)
                }
//                handler.postDelayed(this, 10 * 60 * 1000L) // 10分後
                handler.postDelayed(this, 30 * 1000L) // 30秒
            }
        })
    }

    private fun showPremiumUI() {
        // 広告非表示 + 有料機能を有効に
        findViewById<View>(R.id.adView)?.visibility = View.GONE
        findViewById<View>(R.id.ad_container)?.visibility = View.GONE
    }

    private fun showFreeUI() {
        // 広告表示
        findViewById<View>(R.id.adView)?.visibility = View.VISIBLE
        findViewById<View>(R.id.ad_container)?.visibility = View.VISIBLE
    }

    fun checkPremiumStatus(context: Context, onResult: (Boolean) -> Unit) {
        val billingClient = BillingClient.newBuilder(context)
            .setListener { _, _ -> }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
                    ) { billingResult, purchasesList ->
                        val hasPremium = purchasesList.any { it.products.contains("premium_upgrade") }
                        onResult(hasPremium)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                onResult(false)
            }
        })
    }


    // 有料にアップデートする
    fun launchPurchaseFlow(activity: Activity) {
        val billingClient = BillingClient.newBuilder(activity)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
                    showPremiumUI()
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetailsParams = QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("premium_upgrade")// TODO
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()

                    billingClient.queryProductDetailsAsync(
                        QueryProductDetailsParams.newBuilder().setProductList(listOf(productDetailsParams)).build()
                    ) { _, productDetailsList ->
                        val productDetails = productDetailsList.firstOrNull()
                        if (productDetails != null) {
                            val billingFlowParams = BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(
                                    listOf(
                                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(productDetails)
                                            .build()
                                    )
                                )
                                .build()
                            billingClient.launchBillingFlow(activity, billingFlowParams)
                        }
                    }
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

}