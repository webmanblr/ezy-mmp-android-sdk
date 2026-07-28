package io.ezyurl.mmp

import android.content.Context
import android.os.Build
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * EzyMMP - A lightweight SDK for attributing app installs and tracking events back to short links.
 *
 * Requires the Google Play Install Referrer Library for deterministic attribution:
 *   implementation("com.android.installreferrer:installreferrer:2.2")
 */
class EzyMMP private constructor(
    private val context: Context,
    private val apiKey: String,
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    private val prefs = context.getSharedPreferences("EzyMMPPrefs", Context.MODE_PRIVATE)
    private var deviceId: String

    init {
        // Retrieve or generate a persistent device ID
        var id = prefs.getString("device_id", null)
        val isFreshInstall = id == null
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        deviceId = id

        // On a fresh install, fetch the Google Play Install Referrer (for deterministic
        // attribution) and then notify the backend. Falls back to IP-based attribution
        // server-side if the referrer is unavailable.
        if (isFreshInstall) {
            fetchReferrerAndTrackInstall()
        }
    }

    private fun fetchReferrerAndTrackInstall() {
        val tracked = java.util.concurrent.atomic.AtomicBoolean(false)
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                var referrer: String? = null
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    try {
                        referrer = referrerClient.installReferrer.installReferrer
                    } catch (e: Exception) {
                        Log.e("EzyMMP", "Failed to read install referrer", e)
                    }
                } else {
                    Log.w("EzyMMP", "Install referrer unavailable (code $responseCode)")
                }
                try {
                    referrerClient.endConnection()
                } catch (e: Exception) { /* no-op */ }
                if (tracked.compareAndSet(false, true)) trackInstall(referrer)
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Connection lost before we could read the referrer; track without it.
                if (tracked.compareAndSet(false, true)) trackInstall(null)
            }
        })
    }

    private fun trackInstall(installReferrer: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$baseUrl/install")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-api-key", apiKey)
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("os", "Android")
                    put("deviceModel", Build.MODEL)
                    if (!installReferrer.isNullOrEmpty()) {
                        put("installReferrer", installReferrer)
                    }
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

                val responseCode = conn.responseCode
                Log.d("EzyMMP", "Install track response: $responseCode")
            } catch (e: Exception) {
                Log.e("EzyMMP", "Failed to track install", e)
            }
        }
    }

    /**
     * Track a post-install event
     * @param eventName Name of the event (e.g., "signup", "purchase")
     * @param eventData Map of extra properties
     */
    @JvmOverloads
    fun trackEvent(eventName: String, eventData: Map<String, Any>? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$baseUrl/event")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-api-key", apiKey)
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("eventName", eventName)
                    if (eventData != null) {
                        put("eventData", JSONObject(eventData))
                    }
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

                val responseCode = conn.responseCode
                Log.d("EzyMMP", "Event track response: $responseCode")
            } catch (e: Exception) {
                Log.e("EzyMMP", "Failed to track event: $eventName", e)
            }
        }
    }

    /**
     * Standardized method to track a purchase event
     * @param revenue The amount of the purchase
     * @param currency The 3-letter currency code (e.g., "USD")
     * @param transactionId A unique identifier for the transaction
     * @param extraData Optional extra properties
     */
    @JvmOverloads
    fun trackPurchase(revenue: Double, currency: String, transactionId: String, extraData: Map<String, Any>? = null) {
        val eventData = mutableMapOf<String, Any>(
            "revenue" to revenue,
            "currency" to currency,
            "transactionId" to transactionId
        )
        if (extraData != null) {
            eventData.putAll(extraData)
        }
        trackEvent("purchase", eventData)
    }

    companion object {
        const val DEFAULT_BASE_URL: String = "https://ezyurl.io/api/v1/sdk"

        @Volatile
        private var instance: EzyMMP? = null

        /**
         * Initialize the SDK in your Application.onCreate()
         */
        @JvmStatic
        @JvmOverloads
        fun init(context: Context, apiKey: String, baseUrl: String? = null): EzyMMP {
            return instance ?: synchronized(this) {
                instance ?: EzyMMP(context.applicationContext, apiKey, baseUrl ?: DEFAULT_BASE_URL).also { instance = it }
            }
        }

        /**
         * Get the instance to track events from anywhere in the app
         */
        @JvmStatic
        fun getInstance(): EzyMMP {
            return instance ?: throw IllegalStateException("EzyMMP is not initialized. Call init() first.")
        }
    }
}
