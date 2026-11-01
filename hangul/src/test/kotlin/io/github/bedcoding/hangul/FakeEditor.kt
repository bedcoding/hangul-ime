package io.github.bedcoding.hangul

/** 시험용 입력창. 조합기가 돌려준 Edit를 안드로이드 입력창처럼 반영한다. */
class FakeEditor(val composer: HangulComposer = HangulComposer()) {
    private val committed = StringBuilder()

    var composing = ""
        private set

    /** 입력창에 보이는 글자 전체 (확정된 글자 뒤에 조합 중인 글자). */
    val text: String get() = committed.toString() + composing

    fun type(jamo: Char, timeMillis: Long) = applyEdit(composer.type(jamo, timeMillis))

    /** 조합 중이 아니면 앱이 지우기 키를 보낸 것처럼 확정된 글자를 하나 지운다. */
    fun delete() {
        val edit = composer.delete()
        if (edit != null) {
            applyEdit(edit)
        } else if (committed.isNotEmpty()) {
            committed.deleteCharAt(committed.length - 1)
        }
    }

    fun finish() {
        committed.append(composer.finish())
        composing = ""
    }

    private fun applyEdit(edit: Edit) {
        committed.append(edit.commit)
        composing = edit.composing
    }
}
