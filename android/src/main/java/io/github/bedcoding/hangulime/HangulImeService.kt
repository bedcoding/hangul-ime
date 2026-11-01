package io.github.bedcoding.hangulime

import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** 아직 자판이 없는 키보드. 자판은 3단계에서 그린다. 지금은 다른 키보드로 바꾸는 버튼만 있다. */
class HangulImeService : InputMethodService() {

    override fun onCreateInputView(): View {
        val padding = (16 * resources.displayMetrics.density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
            addView(TextView(context).apply {
                setText(R.string.ime_placeholder)
                gravity = Gravity.CENTER
            })
            addView(Button(context).apply {
                setText(R.string.choose_keyboard)
                setOnClickListener {
                    getSystemService(InputMethodManager::class.java).showInputMethodPicker()
                }
            })
        }
    }
}
