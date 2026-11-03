package com.nool.app.ui.editor.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    var currentOutputFile: File? = null
        private set

    fun startRecording(targetDir: File): File {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(targetDir, "Audio_$timestamp.m4a")
        currentOutputFile = file

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        mediaRecorder = recorder
        return file
    }

    fun stopRecording(): File? {
        val file = currentOutputFile
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            // In case of error (e.g., recorded for < 1 second)
        } finally {
            mediaRecorder = null
        }
        return file
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
        } finally {
            mediaRecorder = null
            currentOutputFile?.delete()
            currentOutputFile = null
        }
    }
}
