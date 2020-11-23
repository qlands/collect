package org.odk.collect.audiorecorder.recording.internal

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.odk.collect.async.Scheduler
import org.odk.collect.audiorecorder.getComponent
import org.odk.collect.audiorecorder.recorder.Output
import org.odk.collect.audiorecorder.recorder.Recorder
import java.util.Locale
import javax.inject.Inject

class AudioRecorderService : Service() {

    @Inject
    internal lateinit var recorder: Recorder

    @Inject
    internal lateinit var recordingRepository: RecordingRepository

    @Inject
    internal lateinit var scheduler: Scheduler

    private var recordingNotification: RecordingNotification? = null

    override fun onCreate() {
        super.onCreate()
        getComponent().inject(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                val output = intent.getSerializableExtra(EXTRA_OUTPUT) as Output

                if (!recorder.isRecording() && sessionId != null) {
                    recordingRepository.start(sessionId)
                    recorder.start(output)

                    recordingNotification = RecordingNotification(this, scheduler).apply { show() }
                }
            }

            ACTION_STOP -> {
                stopRecording()
            }

            ACTION_CLEAN_UP -> {
                cleanUp()
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        cleanUp()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun stopRecording() {
        recordingNotification?.dismiss()

        val file = recorder.stop()
        recordingRepository.recordingReady(file)

        stopSelf()
    }

    private fun cleanUp() {
        recordingNotification?.dismiss()

        recorder.cancel()
        recordingRepository.clear()
        stopSelf()
    }

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_CLEAN_UP = "CLEAN_UP"

        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_OUTPUT = "EXTRA_OUTPUT"
    }
}

object LengthFormatter {
    const val ONE_HOUR = 3600000
    const val ONE_MINUTE = 60000
    const val ONE_SECOND = 1000

    fun formatLength(milliseconds: Long): String {
        val hours = milliseconds / ONE_HOUR
        val minutes = milliseconds % ONE_HOUR / ONE_MINUTE
        val seconds = milliseconds % ONE_MINUTE / ONE_SECOND
        return if (milliseconds < ONE_HOUR) {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
}
