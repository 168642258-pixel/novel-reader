package com.novel.reader.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 阅读偏好（轻量 SharedPreferences）。
 * - readingMode: 0=左右翻页 1=上下翻页 2=连续滚动
 * - darkTheme: 是否深色模式（-1=跟随系统 0=浅色 1=深色）
 * - fontSize: 正文字号 sp
 * - lineHeight: 行距倍数
 */
class ReadingPrefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("reading_prefs", Context.MODE_PRIVATE)

    var readingMode: Int
        get() = sp.getInt(KEY_MODE, 0)
        set(v) { sp.edit().putInt(KEY_MODE, v).apply() }

    var darkTheme: Int
        get() = sp.getInt(KEY_THEME, -1)
        set(v) { sp.edit().putInt(KEY_THEME, v).apply() }

    var fontSize: Int
        get() = sp.getInt(KEY_FONT_SIZE, 18)
        set(v) { sp.edit().putInt(KEY_FONT_SIZE, v.coerceIn(12, 36)).apply() }

    var lineHeight: Float
        get() = sp.getFloat(KEY_LINE_HEIGHT, 1.6f)
        set(v) { sp.edit().putFloat(KEY_LINE_HEIGHT, v.coerceIn(1.2f, 2.4f)).apply() }

    private companion object {
        const val KEY_MODE = "reading_mode"
        const val KEY_THEME = "dark_theme"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_LINE_HEIGHT = "line_height"
    }
}
