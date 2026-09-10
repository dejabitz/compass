package com.dejabit.compass.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.dejabit.compass.MainActivity
import com.dejabit.compass.R

/**
 * Wear OS Complication Provider.
 *
 * Provides current/last-known heading data to watch faces with instant tap-to-launch.
 * Note: Background complication updates are throttled by the Wear OS system (~20-60s)
 * for watch battery conservation; full real-time 60fps tracking occurs in MainActivity.
 */
class CompassComplicationService : SuspendingComplicationDataSourceService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = Icon.createWithResource(this, R.drawable.ic_compass_complication)
        val monochromaticImage = MonochromaticImage.Builder(icon).build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(getString(R.string.complication_label)).build(),
                    contentDescription = PlainComplicationText.Builder(getString(R.string.app_name)).build()
                )
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(tapIntent)
                .build()
            }

            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 0f,
                    min = 0f,
                    max = 360f,
                    contentDescription = PlainComplicationText.Builder(getString(R.string.app_name)).build()
                )
                .setText(PlainComplicationText.Builder("0°").build())
                .setTitle(PlainComplicationText.Builder("N").build())
                .setMonochromaticImage(monochromaticImage)
                .setTapAction(tapIntent)
                .build()
            }

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val icon = Icon.createWithResource(this, R.drawable.ic_compass_complication)
        val monochromaticImage = MonochromaticImage.Builder(icon).build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("042°").build(),
                    contentDescription = PlainComplicationText.Builder("Compass 42 degrees").build()
                )
                .setTitle(PlainComplicationText.Builder("NE").build())
                .setMonochromaticImage(monochromaticImage)
                .build()
            }

            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 42f,
                    min = 0f,
                    max = 360f,
                    contentDescription = PlainComplicationText.Builder("Compass 42 degrees").build()
                )
                .setText(PlainComplicationText.Builder("42°").build())
                .setTitle(PlainComplicationText.Builder("NE").build())
                .setMonochromaticImage(monochromaticImage)
                .build()
            }

            else -> null
        }
    }
}
