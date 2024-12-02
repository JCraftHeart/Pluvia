package com.OxGames.Pluvia.ui.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OxGames.Pluvia.PluviaApp
import com.OxGames.Pluvia.SteamService
import com.OxGames.Pluvia.enums.LoginResult
import com.OxGames.Pluvia.enums.LoginScreen
import com.OxGames.Pluvia.events.AndroidEvent
import com.OxGames.Pluvia.events.SteamEvent
import com.OxGames.Pluvia.ui.data.UserLoginState
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

class UserLoginViewModel : ViewModel() {
    private val _loginState = MutableStateFlow(UserLoginState())
    val loginState: StateFlow<UserLoginState> = _loginState.asStateFlow()

    private val submitChannel = Channel<String>()

    val authenticator = object : IAuthenticator {
        override fun acceptDeviceConfirmation(): CompletableFuture<Boolean> {
            Log.d("UserLoginViewModel", "acceptDeviceConfirmation")
            _loginState.update { currentState ->
                currentState.copy(
                    loginResult = LoginResult.DeviceConfirm,
                    loginScreen = LoginScreen.TWO_FACTOR,
                    isLoggingIn = false
                )
            }
            return CompletableFuture.completedFuture(true)
        }

        override fun getDeviceCode(previousCodeWasIncorrect: Boolean): CompletableFuture<String> {
            Log.d("UserLoginViewModel", "getDeviceCode")
            _loginState.update { currentState ->
                currentState.copy(
                    loginResult = LoginResult.DeviceAuth,
                    loginScreen = LoginScreen.TWO_FACTOR,
                    isLoggingIn = false,
                    previousCodeIncorrect = previousCodeWasIncorrect
                )
            }

            return CompletableFuture<String>().apply {
                viewModelScope.launch {
                    val code = submitChannel.receive()
                    complete(code)
                }
            }
        }

        override fun getEmailCode(
            email: String?,
            previousCodeWasIncorrect: Boolean
        ): CompletableFuture<String> {
            Log.d("UserLoginViewModel", "getEmailCode")

            _loginState.update { currentState ->
                currentState.copy(
                    loginResult = LoginResult.EmailAuth,
                    loginScreen = LoginScreen.TWO_FACTOR,
                    isLoggingIn = false,
                    email = email,
                    previousCodeIncorrect = previousCodeWasIncorrect
                )
            }

            return CompletableFuture<String>().apply {
                viewModelScope.launch {
                    val code = submitChannel.receive()
                    complete(code)
                }
            }
        }
    }

    val onSteamConnected: (SteamEvent.Connected) -> Unit = {
        Log.d("UserLoginScreen", "Received is connected")
        _loginState.update { currentState ->
            currentState.copy(
                isLoggingIn = it.isAutoLoggingIn,
                isSteamConnected = true
            )
        }
    }
    val onSteamDisconnected: (SteamEvent.Disconnected) -> Unit = {
        Log.d("UserLoginScreen", "Received disconnected from Steam")
        _loginState.update { currentState ->
            currentState.copy(isSteamConnected = false)
        }
    }
    val onLogonStarted: (SteamEvent.LogonStarted) -> Unit = {
        _loginState.update { currentState ->
            currentState.copy(isLoggingIn = true)
        }
    }
    val onLogonEnded: (SteamEvent.LogonEnded) -> Unit = {
        Log.d("UserLoginScreen", "Received login result: ${it.loginResult}")
        _loginState.update { currentState ->
            currentState.copy(
                isLoggingIn = false,
                loginResult = it.loginResult
            )
        }
        if (it.loginResult != LoginResult.Success) {
            SteamService.startLoginWithQr()
        }
    }
    val onBackPressed: (AndroidEvent.BackPressed) -> Unit = {
        if (!_loginState.value.isLoggingIn) {
            _loginState.update { currentState ->
                currentState.copy(loginResult = LoginResult.Failed)
            }
        }
    }
    val onQrChallengeReceived: (SteamEvent.QrChallengeReceived) -> Unit = {
        _loginState.update { currentState ->
            currentState.copy(qrCode = it.challengeUrl)
        }
    }
    val onQrAuthEnded: (SteamEvent.QrAuthEnded) -> Unit = {
        _loginState.update { currentState ->
            currentState.copy(isQrFailed = !it.success, qrCode = null)
        }
    }

    init {
        onRetry()

        PluviaApp.events.on<SteamEvent.Connected, Unit>(onSteamConnected)
        PluviaApp.events.on<SteamEvent.Disconnected, Unit>(onSteamDisconnected)
        PluviaApp.events.on<SteamEvent.LogonStarted, Unit>(onLogonStarted)
        PluviaApp.events.on<SteamEvent.LogonEnded, Unit>(onLogonEnded)
        PluviaApp.events.on<AndroidEvent.BackPressed, Unit>(onBackPressed)
        PluviaApp.events.on<SteamEvent.QrChallengeReceived, Unit>(onQrChallengeReceived)
        PluviaApp.events.on<SteamEvent.QrAuthEnded, Unit>(onQrAuthEnded)
    }

    override fun onCleared() {
        Log.d("UserLoginViewModel", "onCleared")

        PluviaApp.events.off<SteamEvent.Connected, Unit>(onSteamConnected)
        PluviaApp.events.off<SteamEvent.Disconnected, Unit>(onSteamDisconnected)
        PluviaApp.events.off<SteamEvent.LogonStarted, Unit>(onLogonStarted)
        PluviaApp.events.off<SteamEvent.LogonEnded, Unit>(onLogonEnded)
        PluviaApp.events.off<AndroidEvent.BackPressed, Unit>(onBackPressed)
        PluviaApp.events.off<SteamEvent.QrChallengeReceived, Unit>(onQrChallengeReceived)
        PluviaApp.events.off<SteamEvent.QrAuthEnded, Unit>(onQrAuthEnded)
        SteamService.stopLoginWithQr()
    }

    fun submit() {
        viewModelScope.launch {
            submitChannel.send(_loginState.value.twoFactorCode)

            _loginState.update { currentState ->
                currentState.copy(isLoggingIn = true)
            }
        }
    }

    fun onRetry() {
        _loginState.update { currentState ->
            currentState.copy(
                isSteamConnected = SteamService.isConnected,
                isLoggingIn = SteamService.isLoggingIn,
                attemptCount = currentState.attemptCount.plus(1),
                isQrFailed = false,
            )
        }
    }

    fun setShowLoginScreen(loginScreen: LoginScreen) {
        onRetry()

        if (loginScreen == LoginScreen.QR) {
            SteamService.startLoginWithQr()
        } else {
            SteamService.stopLoginWithQr()
        }

        _loginState.update { currentState ->
            currentState.copy(loginScreen = loginScreen)
        }
    }

    fun setUsername(username: String) {
        _loginState.update { currentState ->
            currentState.copy(username = username)
        }
    }

    fun setPassword(password: String) {
        _loginState.update { currentState ->
            currentState.copy(password = password)
        }
    }

    fun setRememberPass(rememberPass: Boolean) {
        _loginState.update { currentState ->
            currentState.copy(rememberPass = rememberPass)
        }
    }

    fun setTwoFactorCode(twoFactorCode: String) {
        _loginState.update { currentState ->
            currentState.copy(twoFactorCode = twoFactorCode)
        }
    }
}