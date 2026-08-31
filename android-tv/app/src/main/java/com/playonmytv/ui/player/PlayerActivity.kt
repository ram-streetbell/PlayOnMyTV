package com.playonmytv.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.playonmytv.R
import com.playonmytv.data.local.db.AppDatabase
import com.playonmytv.data.repository.LocalPlaybackRepositoryImpl
import com.playonmytv.player.playback.PlaybackCoordinator
import com.playonmytv.player.scheduler.ScheduleEvaluator
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
        val syncPanel = findViewById<View>(R.id.sync_panel)
        val syncProgress = findViewById<ProgressBar>(R.id.sync_progress)
        val syncTitle = findViewById<TextView>(R.id.sync_title)
        val syncDetail = findViewById<TextView>(R.id.sync_detail)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "playonmytv.db",
        ).fallbackToDestructiveMigration().build()

        val playbackRepository = LocalPlaybackRepositoryImpl(
            localPlaylistDao = database!!.localPlaylistDao(),
            localScheduleDao = database!!.localScheduleDao(),
            mediaDao = database!!.mediaDao(),
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

        lifecycleScope.launch {
            var waitingCycles = 0
            while (isActive) {
                val media = database?.mediaDao()?.findAll().orEmpty()
                val total = media.size
                val completed = media.count { it.status == "COMPLETED" && it.path.isNotBlank() }
                val active = media.count { it.status == "QUEUED" || it.status == "DOWNLOADING" }
                val failed = media.count { it.status == "FAILED" }

                if (completed > 0) {
                    syncPanel.visibility = View.GONE
                } else {
                    syncPanel.visibility = View.VISIBLE
                    syncTitle.text = if (active > 0 || total == 0) "Syncing content…" else "Preparing content…"
                    syncProgress.progress = if (total > 0) ((completed * 100) / total).coerceIn(0, 100) else 0
                    syncDetail.text = when {
                        active > 0 -> "Downloaded $completed of $total media"
                        failed > 0 -> "Downloaded $completed of $total • $failed failed"
                        waitingCycles < 4 -> "Connecting to your content…"
                        total == 0 -> "Waiting for content from the dashboard…"
                        else -> "Preparing media…"
                    }
                }

                waitingCycles++
                delay(750)
            }
        }
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
