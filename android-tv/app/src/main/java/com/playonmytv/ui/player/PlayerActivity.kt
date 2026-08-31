package com.playonmytv.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.playonmytv.R
import com.playonmytv.data.local.db.AppDatabase
import com.playonmytv.data.repository.LocalPlaybackRepositoryImpl
import com.playonmytv.player.playback.PlaybackCoordinator
import com.playonmytv.player.scheduler.ScheduleEvaluator
import androidx.media3.ui.PlayerView
import android.widget.ImageView
import android.widget.TextView

class PlayerActivity : AppCompatActivity() {
    private lateinit var coordinator: PlaybackCoordinator
    private var database: AppDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()
        setContentView(R.layout.activity_player)

        val playerView = findViewById<PlayerView>(R.id.player_view)
        val imageView = findViewById<ImageView>(R.id.image_view)
        val idleText = findViewById<TextView>(R.id.idle_text)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "playonmytv.db",
        ).fallbackToDestructiveMigration().build()

        val playbackRepository = LocalPlaybackRepositoryImpl(
            localPlaylistDao = database!!.localPlaylistDao(),
            localScheduleDao = database!!.localScheduleDao(),
        )

        coordinator = PlaybackCoordinator(
            context = this,
            repository = playbackRepository,
            scheduleEvaluator = ScheduleEvaluator(),
            playerView = playerView,
            imageView = imageView,
            idleTextView = idleText,
        )
        coordinator.start(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    override fun onDestroy() {
        if (::coordinator.isInitialized) coordinator.release()
        database?.close()
        database = null
        super.onDestroy()
    }

    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, PlayerActivity::class.java)
    }
}
