package com.example.timemanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timemanager.data.entity.ReviewEntity

@Composable
fun ReviewSection(
    review: ReviewEntity?,
    periodLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = periodLabel,
            style = MaterialTheme.typography.titleMedium
        )
        if (review == null) {
            Text(
                text = "本${periodLabel}还没有复盘，去 REVIEW tab 写",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            ReviewField("主要时间投入", review.summaryText)
            ReviewField("时间结构问题", review.mainFindings)
            ReviewField("下一步调整", review.adjustmentPlan)
        }
    }
}

@Composable
private fun ReviewField(label: String, text: String?) {
    if (text.isNullOrBlank()) return
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
