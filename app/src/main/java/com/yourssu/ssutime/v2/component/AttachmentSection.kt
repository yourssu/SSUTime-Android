package com.yourssu.ssutime.v2.component

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.ui.theme.N200
import com.yourssu.ssutime.v2.ui.theme.N400
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE

@Composable
fun <T> AttachmentSection(
    attachments: List<T>,
    nameProvider: (index: Int, item: T) -> String,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.common_attachment),
            style = SSUType.Caption1SemiBold,
            color = N400,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            attachments.forEachIndexed { index, attachment ->
                val defaultName = stringResource(R.string.common_attachment_numbered, index + 1)
                val fileName = nameProvider(index, attachment).takeIf { it.isNotBlank() } ?: defaultName
                AttachmentItemCard(
                    fileName = fileName,
                    onClick = { onItemClick(attachment) },
                )
            }
        }
    }
}

@Composable
fun AttachmentItemCard(
    fileName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    useMarquee: Boolean = false,
    downloadContentDescription: String = stringResource(R.string.common_download),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, N200, RoundedCornerShape(8.dp))
            .background(WHITE)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.AttachFile,
            contentDescription = stringResource(R.string.common_attachment),
            tint = N400,
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = fileName,
            style = SSUType.Label2Medium,
            color = N500,
            maxLines = 1,
            overflow = if (useMarquee) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(if (useMarquee) Modifier.basicMarquee() else Modifier),
        )

        if (!trailingText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = trailingText,
                style = SSUType.Caption1Medium,
                color = N400,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Outlined.FileDownload,
            contentDescription = downloadContentDescription,
            tint = N400,
            modifier = Modifier.size(20.dp),
        )
    }
}
