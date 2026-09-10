package com.dejabit.compass.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.dejabit.compass.R
import com.dejabit.compass.ui.theme.MagneticNorthAccent
import com.dejabit.compass.ui.theme.TrueNorthAccent

@Composable
fun LocationRationaleDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        showDialog = show,
        onDismissRequest = onDismiss
    ) {
        Alert(
            title = {
                Text(
                    text = stringResource(R.string.location_rationale_title),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title3,
                    color = TrueNorthAccent,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            negativeButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "Mag",
                        fontSize = 12.sp,
                        color = MagneticNorthAccent
                    )
                }
            },
            positiveButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.primaryButtonColors(backgroundColor = TrueNorthAccent),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "Allow",
                        fontSize = 12.sp
                    )
                }
            }
        ) {
            Text(
                text = stringResource(R.string.location_rationale_message),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
    }
}
