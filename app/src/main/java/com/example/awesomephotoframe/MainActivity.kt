package com.example.awesomephotoframe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.awesomephotoframe.data.repository.GooglePhotosApi
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.squareup.picasso.Picasso
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

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val RC_SIGN_IN = 9001
    }

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var ivPhotoFrame: ImageView
//    private lateinit var tvUser: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivPhotoFrame = findViewById(R.id.iv_photo_frame)

        // Google Sign-In 設定
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/photoslibrary.readonly"))
            .requestServerAuthCode(BuildConfig.GOOGLE_CLIENT_ID, false)
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ログイン状態確認
        val account = GoogleSignIn.getLastSignedInAccount(this)
        updateUI(account)

        val btnMenu = findViewById<ImageButton>(R.id.btn_menu)
        btnMenu.setOnClickListener { view ->
            showPopupMenu(view)
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showUserInfoDialog() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        val message = account?.let {
            "Name: ${it.displayName}\nEmail: ${it.email}"
        } ?: "Not signed in."

        AlertDialog.Builder(this)
            .setTitle("User Info")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
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
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            updateUI(null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "Sign-in successful: ${account.email}")
            updateUI(account)

            val authCode = account.serverAuthCode
            if (authCode != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    val accessToken = exchangeAuthCodeForAccessToken(
                        authCode,
                        BuildConfig.GOOGLE_CLIENT_ID,
                        BuildConfig.GOOGLE_CLIENT_SECRET
                    )

                    if (accessToken != null) {
                        fetchPhotosWithAccessToken(accessToken)
                    }
                }
            }

        } catch (e: ApiException) {
            Log.w(TAG, "Sign-in failed", e)
            updateUI(null)
        }
    }


    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null) {
//            tvUser.text = "Welcome, ${account.displayName}"
//            findViewById<View>(R.id.sign_in_button).visibility = View.GONE
//            findViewById<View>(R.id.sign_out_button).visibility = View.VISIBLE
        } else {
//            tvUser.text = "Not signed in"
//            findViewById<View>(R.id.sign_in_button).visibility = View.VISIBLE
//            findViewById<View>(R.id.sign_out_button).visibility = View.GONE
        }

        invalidateOptionsMenu()
    }

    suspend fun exchangeAuthCodeForAccessToken(
        authCode: String,
        clientId: String,
        clientSecret: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://oauth2.googleapis.com/token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val postData = listOf(
                "code" to authCode,
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "redirect_uri" to "",
                "grant_type" to "authorization_code"
            ).joinToString("&") { "${it.first}=${URLEncoder.encode(it.second, "UTF-8")}" }

            conn.outputStream.use { it.write(postData.toByteArray()) }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            return@withContext json.getString("access_token")
        } catch (e: Exception) {
            Log.e("TokenExchange", "Failed to get access token", e)
            return@withContext null
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
            val response = api.listMediaItems("Bearer $accessToken")
            val mediaItems = response.mediaItems
            if (!mediaItems.isNullOrEmpty()) {
                val imageUrl = mediaItems.random().baseUrl + "=w1024-h768"

                withContext(Dispatchers.Main) {
                    Picasso.get().load(imageUrl).into(ivPhotoFrame)
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
            userItem.title = "👤 ${account.displayName}"
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


}