package io.github.bedcoding.hangulime

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** 설정 화면. 지금은 빌드 번호, 키보드를 켜는 버튼, 키보드를 쳐 보는 시험 칸만 있다. */
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
            addView(TextView(context).apply {
                setText(R.string.try_field_label)
                setPadding(0, padding, 0, 0)
            })
            // 메모 앱을 열지 않고 여기서 키보드를 쳐 본다. CI의 가상 폰 확인도 이 칸을 눌러 키보드를 띄운다.
            // 친 내용은 화면 상태 저장, 자동 완성, 다른 키보드의 학습 어디에도 남기지 않는다.
            addView(EditText(context).apply {
                setHint(R.string.try_field_hint)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                minLines = 3
                gravity = Gravity.TOP or Gravity.START
                isSaveEnabled = false
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            })
        }
        // 안드로이드 15부터는 화면이 상태 표시줄 밑까지 그려지므로 시스템 막대만큼 띄운다.
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(content)
        })
    }
}
