package com.playonmytv.ui.pairing

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.playonmytv.R
import com.playonmytv.app.config.AppConfig
import com.playonmytv.app.di.ServiceLocator
import com.playonmytv.databinding.ActivityPairingBinding
import com.playonmytv.sync.DeviceSyncService
import com.playonmytv.ui.player.PlayerActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

class PairingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPairingBinding
    private val deviceRepository by lazy { ServiceLocator.provideDeviceRepository(applicationContext) }
    private var pairingPollJob: Job? = null
    private var expiresAt: OffsetDateTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (deviceRepository.getDeviceToken() != null) {
            DeviceSyncService.requestImmediateSync(applicationContext)
            navigateToPlayer()
            return
        }

        binding.appNameLabel.text = getString(R.string.app_name_uppercase)
        binding.waitingLabel.text = getString(R.string.waiting_for_pairing)

        lifecycleScope.launch {
            requestPairingCode()
            startPolling()
        }
    }

    override fun onDestroy() {
        pairingPollJob?.cancel()
        super.onDestroy()
    }

    private suspend fun requestPairingCode() {
        setLoading(true)

        try {
            val result = deviceRepository.requestPairing(
                deviceName = android.os.Build.MODEL ?: "Android TV",
                appVersion = com.playonmytv.BuildConfig.VERSION_NAME
            )

            expiresAt = result.expiresAt
            binding.deviceCodeValue.text = result.pairingCode
            binding.waitingLabel.text = getString(R.string.waiting_for_pairing)
            binding.errorLabel.visibility = View.GONE
        } catch (exception: Exception) {
            binding.errorLabel.visibility = View.VISIBLE
            binding.errorLabel.text = exception.message ?: getString(R.string.generic_error)
        } finally {
            setLoading(false)
        }
    }

    private fun startPolling() {
        pairingPollJob?.cancel()
        pairingPollJob = lifecycleScope.launch {
            while (isActive) {
                if (expiresAt == null) {
                    requestPairingCode()
                    delay(AppConfig.pairingPollIntervalMillis)
                    continue
                }

                val expiry = expiresAt

                if (expiry != null && OffsetDateTime.now().isAfter(expiry)) {
                    requestPairingCode()
                    delay(AppConfig.pairingPollIntervalMillis)
                    continue
                }

                try {
                    val status = deviceRepository.checkPairingStatus()

                    if (!status.waiting && !status.deviceToken.isNullOrBlank()) {
                        deviceRepository.savePairedDevice(status.deviceToken, status.deviceName)
                        DeviceSyncService.requestImmediateSync(applicationContext)
                        navigateToPlayer()
                        break
                    }
                } catch (exception: Exception) {
                    binding.errorLabel.visibility = View.VISIBLE
                    binding.errorLabel.text = exception.message ?: getString(R.string.generic_error)
                }

                delay(AppConfig.pairingPollIntervalMillis)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun navigateToPlayer() {
        startActivity(PlayerActivity.newIntent(this))
        finish()
    }
}
