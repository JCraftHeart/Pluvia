package com.OxGames.Pluvia.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.OxGames.Pluvia.SteamService
import com.OxGames.Pluvia.enums.LoginResult
import com.OxGames.Pluvia.events.SteamEvent

@Composable
fun QrLoginScreen(innerPadding: PaddingValues) {
    var url: String? by remember { mutableStateOf(null) }
    var isFailed: Boolean by remember { mutableStateOf(false) }
    var attemptNum: Int by remember { mutableIntStateOf(0) }
    var isLoggingIn by remember { mutableStateOf(SteamService.isLoggingIn) }

    // launched effect makes it so that this only runs any time attemptNum changes
    // without launched effect this block runs everytime a change happens to the ui
    LaunchedEffect(attemptNum) {
        Log.d("QrLoginScreen", "Starting QR authentication")
        isFailed = false
        isLoggingIn = SteamService.isLoggingIn

        // TODO: revisit this to make sure all looks correct
        val onLogonStarted: (SteamEvent.LogonStarted) -> Unit = { isLoggingIn = true }
        val onLogonEnded: (SteamEvent.LogonEnded) -> Unit = {
            isLoggingIn = false
            if (it.loginResult != LoginResult.Success)
                SteamService.startLoginWithQr()
        }
        val onQrChallengeReceived: (SteamEvent.QrChallengeReceived) -> Unit = { url = it.challengeUrl }
        SteamService.events.on<SteamEvent.LogonStarted>(onLogonStarted)
        SteamService.events.on<SteamEvent.LogonEnded>(onLogonEnded)
        SteamService.events.on<SteamEvent.QrChallengeReceived>(onQrChallengeReceived)
        SteamService.events.once<SteamEvent.QrAuthEnded> {
            SteamService.events.off<SteamEvent.LogonStarted>(onLogonStarted)
            SteamService.events.off<SteamEvent.LogonEnded>(onLogonEnded)
            SteamService.events.off<SteamEvent.QrChallengeReceived>(onQrChallengeReceived)
            isFailed = !it.success
            url = null
        }
        if (!isLoggingIn)
            SteamService.startLoginWithQr()
    }

    if (url != null || isFailed || isLoggingIn) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isFailed)
                ElevatedButton(onClick = { attemptNum++ }) { Text("Retry") }
            else
                QrCodeImage(content = url!!, size = 256.dp)
        }
    } else
        LoadingScreen(innerPadding)
}