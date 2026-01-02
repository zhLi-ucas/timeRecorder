package com.example.timemanager.data

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Tag(
    val name: String,
    val colorArgb: Int
) {
    companion object {
        fun create(name: String): Tag {
            return Tag(name, randomColorArgb())
        }

        private fun randomColorArgb(): Int {
            // 生成柔和的随机颜色 (避免太亮或太暗)
            val red = Random.nextInt(100, 230)
            val green = Random.nextInt(100, 230)
            val blue = Random.nextInt(100, 230)
            // 构造 ARGB int (Alpha = 255)
            return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
        }
    }
}
