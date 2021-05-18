package com.teoking.canvasdemos.ui.spectrogram

import android.app.Application
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.teoking.canvasdemos.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception

class SpectrogramViewModel(application: Application) : AndroidViewModel(application) {

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var visualizer: Visualizer

    private val _bytes = MutableLiveData<ByteArray>()
    private val _fftBytes = MutableLiveData<ByteArray>()

    val bytes: LiveData<ByteArray> = _bytes
    val fftBytes: LiveData<ByteArray> = _fftBytes

    private var isPlayerReady = false

    fun play() {
        scope.launch {
            if (isPlayerReady.not()) {
                mediaPlayer = MediaPlayer.create(getApplication(), R.raw.sample_music)
                mediaPlayer.isLooping = true
                isPlayerReady = true
            }
            mediaPlayer.start()

            try {
                visualizer = Visualizer(mediaPlayer.audioSessionId)
                visualizer.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        Log.d(
                            TAG,
                            "onWaveFormDataCapture waveform=${waveform?.size}, sr=$samplingRate"
                        )
                        waveform?.let {
                            _bytes.value = it
                        }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        Log.d(TAG, "onFftDataCapture waveform=${fft?.size}, sr=$samplingRate")
                        fft?.let {
                            _fftBytes.value = it
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                visualizer.captureSize = Visualizer.getCaptureSizeRange().first()
                visualizer.enabled = true

                mediaPlayer.setOnCompletionListener {
                    visualizer.enabled = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Create visualizer failed", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (this::mediaPlayer.isInitialized) {
            mediaPlayer.pause()
            mediaPlayer.stop()
            mediaPlayer.release()
        }

        if (this::visualizer.isInitialized) {
            visualizer.release()
        }
    }

    companion object {
        const val TAG = "SpectrogramViewModel"
    }
}