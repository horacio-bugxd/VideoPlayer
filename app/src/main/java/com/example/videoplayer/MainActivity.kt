package com.example.videoplayer

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : ComponentActivity() {

    private var exo: ExoPlayer? = null
    private var inPip by mutableStateOf(false)
    private var incomingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val player = ExoPlayer.Builder(this).build()
        exo = player
        incomingUri = intent?.data

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PlayerScreen(
                    exo = player,
                    inPip = inPip,
                    incomingUri = incomingUri,
                    onConsumed = { incomingUri = null },
                    onEnterPip = { enterPip() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingUri = intent.data
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (exo?.isPlaying == true) enterPip()
    }

    fun enterPip() {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        enterPictureInPictureMode(params)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
    }

    override fun onStop() {
        super.onStop()
        if (!isInPictureInPictureMode) exo?.pause()
    }

    override fun onDestroy() {
        exo?.release()
        exo = null
        super.onDestroy()
    }
}

@Composable
fun PlayerScreen(
    exo: ExoPlayer,
    inPip: Boolean,
    incomingUri: Uri?,
    onConsumed: () -> Unit,
    onEnterPip: () -> Unit
) {
    val activity = LocalContext.current as ComponentActivity
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var hasMedia by remember { mutableStateOf(exo.currentMediaItem != null) }
    var url by remember { mutableStateOf("") }
    var speed by remember { mutableFloatStateOf(1f) }
    var error by remember { mutableStateOf<String?>(null) }

    fun play(uri: Uri) {
        error = null
        exo.setMediaItem(MediaItem.fromUri(uri))
        exo.setPlaybackSpeed(speed)
        exo.prepare()
        exo.playWhenReady = true
        hasMedia = true
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) play(uri) }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                error = "No se pudo reproducir (${e.errorCodeName})"
            }
        }
        exo.addListener(listener)
        onDispose { exo.removeListener(listener) }
    }

    LaunchedEffect(incomingUri) {
        incomingUri?.let {
            play(it)
            onConsumed()
        }
    }

    // Pantalla completa automática en horizontal o en PiP
    val fullscreen = hasMedia && (landscape || inPip)
    LaunchedEffect(fullscreen) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (fullscreen) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
        contentColor = Color.White
    ) {
        Column(
            modifier = if (fullscreen) Modifier.fillMaxSize()
            else Modifier.fillMaxSize().statusBarsPadding()
        ) {
            if (hasMedia) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = exo
                            setShowSubtitleButton(true)
                        }
                    },
                    update = { it.useController = !inPip },
                    modifier = if (fullscreen) Modifier.fillMaxSize()
                    else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                )
            }

            if (!fullscreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Video Player", style = MaterialTheme.typography.headlineSmall)

                    Button(
                        onClick = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Elegir video del dispositivo") }

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Enlace del video (https://...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { play(Uri.parse(url.trim())) },
                        enabled = url.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Reproducir enlace") }

                    if (hasMedia) {
                        Text("Velocidad", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.5f, 1f, 1.5f, 2f).forEach { s ->
                                FilterChip(
                                    selected = speed == s,
                                    onClick = {
                                        speed = s
                                        exo.setPlaybackSpeed(s)
                                    },
                                    label = { Text("${s}x") }
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onEnterPip,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Imagen en imagen (PiP)") }
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
