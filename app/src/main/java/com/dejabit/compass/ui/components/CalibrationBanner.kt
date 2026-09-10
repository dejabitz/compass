package com.dejabit.compass.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.dejabit.compass.R
import com.dejabit.compass.ui.theme.WarningYellow

@Composable
fun CalibrationBanner(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC2A2A00))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.calibrate_banner_text),
                color = WarningYellow,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}
