package com.example.awesomephotoframe.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awesomephotoframe.BuildConfig

import com.example.awesomephotoframe.data.repository.PhotoRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch

class MainViewModel(private val repository: PhotoRepository) : ViewModel() {

    private val _photoUrl = MutableLiveData<String?>()
    val photoUrl: LiveData<String?> = _photoUrl

    fun handleSignInResult(account: GoogleSignInAccount) {
        val authCode = account.serverAuthCode ?: return
        viewModelScope.launch {
            val token = repository.exchangeAuthCodeForAccessToken(
                authCode,
                BuildConfig.GOOGLE_CLIENT_ID,
                BuildConfig.GOOGLE_CLIENT_SECRET
            )
            if (token != null) {
                fetchPhoto(token)
            }
        }
    }

    private fun fetchPhoto(accessToken: String) {
        viewModelScope.launch {
            val imageUrl = repository.fetchPhotoUrl(accessToken)
            _photoUrl.postValue(imageUrl)
        }
    }
}
