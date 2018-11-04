package com.lgsdiamond.lgsutility

import android.media.MediaPlayer

class LgsSoundUtil {
    companion object {
        val soundTick: MediaPlayer by lazy {
            MediaPlayer.create(gAppContext, R.raw.tick)
        }

        val soundClick: MediaPlayer by lazy {
            MediaPlayer.create(gAppContext, R.raw.click)
        }

        val soundList: MediaPlayer by lazy {
            MediaPlayer.create(gAppContext, R.raw.list)
        }

        val soundMessage: MediaPlayer by lazy {
            MediaPlayer.create(gAppContext, R.raw.message)
        }

        val soundOpening: MediaPlayer by lazy {
            MediaPlayer.create(gAppContext, R.raw.openning)
        }

        val soundSliding: MediaPlayer by lazy {
            MediaPlayer.create(gAppContext, R.raw.sliding)
        }

        val soundBook: MediaPlayer by lazy {
            MediaPlayer.create(gAppContext, R.raw.book)
        }
    }
}