package com.thewayoflife.rosary.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/** The sound effects (spec 13), named after their files in assets/sounds. */
enum class Sound(val file: String) {
    QUESTION_RIGHT("question_answered_correctly.mp3"),
    QUESTION_WRONG("question_wrong.mp3"),
    CHALLENGE_PASSED("passed_challenge.mp3"),
    PENANCE_COMPLETE("pennance_complete.mp3"),
    VICTORY("final_victory.mp3"),
}

/** Plays each sound once. The victory music can be stopped when the player leaves the victory screen. */
class Sounds(context: Context) {
    private val assets = context.applicationContext.assets
    private val playing = mutableSetOf<MediaPlayer>()

    fun play(sound: Sound) {
        runCatching {
            val player = MediaPlayer()
            assets.openFd("sounds/${sound.file}").use { fd -> player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
            player.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
            player.setOnCompletionListener { done ->
                playing.remove(done)
                done.release()
            }
            player.prepare()
            player.start()
            playing.add(player)
        }
    }

    /** Stops anything still playing, such as the victory music. */
    fun stopAll() {
        playing.forEach { runCatching { it.stop() }; it.release() }
        playing.clear()
    }
}
