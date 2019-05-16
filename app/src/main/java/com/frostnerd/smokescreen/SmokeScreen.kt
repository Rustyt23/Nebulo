package com.frostnerd.smokescreen

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.frostnerd.smokescreen.activity.ErrorDialogActivity
import com.frostnerd.smokescreen.activity.PinActivity
import com.frostnerd.smokescreen.database.AppDatabase
import com.frostnerd.smokescreen.util.Notifications
import com.github.anrwatchdog.ANRWatchDog
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import io.sentry.event.User
import java.util.*
import kotlin.system.exitProcess

/*
 * Copyright (C) 2019 Daniel Wolf (Ch4t4r)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can contact the developer at daniel.wolf@frostnerd.com.
 */
class SmokeScreen : Application() {
    companion object {
        const val NOTIFICATION_ID_APP_CRASH = 3
    }

    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
    val customUncaughtExceptionHandler: Thread.UncaughtExceptionHandler =
        Thread.UncaughtExceptionHandler { t, e ->
            e.printStackTrace()
            log(e)
            val isPrerelease =
                BuildConfig.VERSION_NAME.contains("alpha", true) || BuildConfig.VERSION_NAME.contains("beta", true)
            if (isPrerelease && getPreferences().loggingEnabled && !getPreferences().crashReportingEnabled) {
                startActivity(Intent(this, ErrorDialogActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else if (isPrerelease && getPreferences().crashReportingEnabled) {
                showCrashNotification()
            }
            closeLogger()
            defaultUncaughtExceptionHandler?.uncaughtException(t, e)
            exitProcess(0)
        }

    private fun showCrashNotification() {
        val notification = NotificationCompat.Builder(this, Notifications.noConnectionNotificationChannelId(this))
            .setSmallIcon(R.drawable.ic_cloud_warn)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 1,
                    Intent(this, PinActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setContentTitle(getString(R.string.notification_appcrash_title))
        notification.setStyle(NotificationCompat.BigTextStyle(notification).bigText(getString(R.string.notification_appcrash_message)))
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            NOTIFICATION_ID_APP_CRASH,
            notification.build()
        )
    }

    override fun onCreate() {
        initSentry()
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(customUncaughtExceptionHandler)
        super.onCreate()
        log("Application created.")
        ANRWatchDog().setANRListener {
            log(RuntimeException(it))
        }
    }

    fun initSentry(forceEnabled: Boolean = false) {
        if (forceEnabled || getPreferences().crashReportingEnabled) {
            Sentry.init(
                "https://fadeddb58abf408db50809922bf064cc@sentry.frostnerd.com:443/2",
                AndroidSentryClientFactory(this)
            )
            Sentry.getContext().user = User(getPreferences().crashReportingUUID, null, null, null)
            Sentry.getStoredClient().addTag("user.language", Locale.getDefault().displayLanguage)
            Sentry.getStoredClient().addTag("app.database_version", AppDatabase.currentVersion.toString())
            Sentry.getStoredClient().addTag("app.dns_server_name", getPreferences().dnsServerConfig.name)
            Sentry.getStoredClient()
                .addTag("app.dns_server_primary", getPreferences().dnsServerConfig.servers[0].address.formatToString())
            Sentry.getStoredClient().addTag(
                "app.dns_server_secondary",
                getPreferences().dnsServerConfig.servers.getOrNull(1)?.address?.formatToString()
            )
            Sentry.getStoredClient().addTag("app.debug", BuildConfig.DEBUG.toString())
            Sentry.getStoredClient()
                .addTag("app.installer_package", packageManager.getInstallerPackageName(packageName))
        } else {
            Sentry.close()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        log("The system seems to have low memory")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        log("Memory has been trimmed with level $level")
    }
}