package page.smirnov.pcm_demo

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.min

class PCMPlayer(private val context: Context) {
    private val sampleRate = 48000
    private val channelCount = 2
    private val bytesPerSample = 2
    private val frameSize = 480 // samples per channel
    private val bufferSize = frameSize * channelCount * bytesPerSample

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    private fun createAudioTrack(): AudioTrack {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        return AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(
                AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            )
            .build()
    }

    fun play() {
        if (isPlaying) return

        isPlaying = true
        audioTrack = createAudioTrack().apply { play() }

        Thread {
            try {
                val buffer = ByteArray(bufferSize)
                context.assets.open("violin.wav").use { inputStream ->
                    // Skip WAV header (44 bytes)
                    inputStream.skip(44)

                    while (isPlaying && inputStream.read(buffer) != -1) {
                        val start = System.currentTimeMillis()
                        audioTrack?.write(buffer, 0, bufferSize)
                        val time = System.currentTimeMillis() - start

                        val sleepTime = maxOf(10 - time, 1)
                        Thread.sleep(sleepTime)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stop()
            }
        }.start()
    }

    fun stop() {
        isPlaying = false
        audioTrack?.apply {
            stop()
            flush()
            release()
        }
        audioTrack = null
    }
}