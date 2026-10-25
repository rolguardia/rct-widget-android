package fr.rct.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

class RctWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Affiche immédiatement les dernières données enregistrées.
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, readCachedData(context)))
        }

        // Télécharge les données en arrière-plan si une URL HTTPS est configurée.
        refreshFromRemote(context.applicationContext)
    }

    override fun onReceive(context: Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            refreshFromRemote(context.applicationContext)
        }
    }

    private fun refreshFromRemote(context: Context) {
        val endpoint = context.getString(R.string.match_json_url).trim()
        if (endpoint.isBlank() || !endpoint.startsWith("https://")) {
            Log.i(TAG, "Aucune URL HTTPS configurée : affichage des données de démonstration.")
            return
        }

        executor.execute {
            try {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }
                val jsonText = try {
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } finally {
                    connection.disconnect()
                }
                val json = JSONObject(jsonText)
                val data = MatchData(
                    matchTitle = json.optString("matchTitle", "Prochain match du RCT"),
                    kickoff = json.optString("kickoff", "Date à confirmer"),
                    competition = json.optString("competition", "RUGBY"),
                    channel = json.optString("channel", ""),
                    channelConfirmed = json.optBoolean("channelConfirmed", false),
                    updatedAt = json.optString("updatedAt", "")
                )
                cacheData(context, data)
                Handler(Looper.getMainLooper()).post {
                    val manager = AppWidgetManager.getInstance(context)
                    val component = ComponentName(context, RctWidgetProvider::class.java)
                    manager.getAppWidgetIds(component).forEach { id ->
                        manager.updateAppWidget(id, buildViews(context, data))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Impossible de récupérer le JSON distant ; conservation des dernières données.", e)
            }
        }
    }

    private fun buildViews(context: Context, data: MatchData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.rct_widget)
        views.setTextViewText(R.id.competition, data.competition)
        views.setTextViewText(R.id.match_title, data.matchTitle)
        views.setTextViewText(R.id.match_date, data.kickoff)

        val channelText = when {
            data.channelConfirmed && data.channel.isNotBlank() -> data.channel
            data.channel.isNotBlank() -> "${data.channel} • à confirmer"
            else -> "Diffusion à confirmer"
        }
        views.setTextViewText(R.id.match_channel, channelText)
        views.setTextViewText(
            R.id.updated_at,
            if (data.updatedAt.isBlank()) "Données de démonstration" else "Mis à jour : ${data.updatedAt}"
        )
        return views
    }

    private fun readCachedData(context: Context): MatchData {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return MatchData(
            matchTitle = prefs.getString("matchTitle", "Prochain match du RCT") ?: "Prochain match du RCT",
            kickoff = prefs.getString("kickoff", "Date à configurer") ?: "Date à configurer",
            competition = prefs.getString("competition", "TOP 14") ?: "TOP 14",
            channel = prefs.getString("channel", "") ?: "",
            channelConfirmed = prefs.getBoolean("channelConfirmed", false),
            updatedAt = prefs.getString("updatedAt", "") ?: ""
        )
    }

    private fun cacheData(context: Context, data: MatchData) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("matchTitle", data.matchTitle)
            .putString("kickoff", data.kickoff)
            .putString("competition", data.competition)
            .putString("channel", data.channel)
            .putBoolean("channelConfirmed", data.channelConfirmed)
            .putString("updatedAt", data.updatedAt)
            .apply()
    }

    data class MatchData(
        val matchTitle: String,
        val kickoff: String,
        val competition: String,
        val channel: String,
        val channelConfirmed: Boolean,
        val updatedAt: String
    )

    companion object {
        private const val TAG = "RctWidget"
        private const val PREFS = "rct_match_cache"
        private val executor = Executors.newSingleThreadExecutor()
    }
}
