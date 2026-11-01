package io.github.bedcoding.hangulime

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** 설정 화면. 지금은 빌드 번호와 키보드를 켜는 버튼만 있다. */
class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (24 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            addView(TextView(context).apply {
                text = getString(R.string.build_info, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)
                textSize = 18f
            })
            addView(TextView(context).apply {
                setText(R.string.settings_hint)
                setPadding(0, padding / 2, 0, padding / 2)
            })
            addView(Button(context).apply {
                setText(R.string.open_keyboard_list)
                setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
            })
            addView(Button(context).apply {
                setText(R.string.choose_keyboard)
                setOnClickListener {
                    getSystemService(InputMethodManager::class.java).showInputMethodPicker()
                }
            })
        }
        // 안드로이드 15부터는 화면이 상태 표시줄 밑까지 그려지므로 시스템 막대만큼 띄운다.
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(content)
        })
    }
}
