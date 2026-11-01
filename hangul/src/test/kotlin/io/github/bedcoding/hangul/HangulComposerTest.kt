package io.github.bedcoding.hangul

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** testcases/hangul.txt로 나타내기 어려운 것: 정확한 시각, 입력창에 할 일의 모양, DEL 단계, 조합 끝내기. */
class HangulComposerTest {
    private fun editor(t: Long = 250) = FakeEditor(HangulComposer(t))

    /** 자모 문자열을 [step] 간격으로 친다. */
    private fun FakeEditor.typeAll(jamos: String, start: Long = 0, step: Long = 1000): FakeEditor {
        jamos.forEachIndexed { i, jamo -> type(jamo, start + i * step) }
        return this
    }

    @Test
    fun `간격이 T보다 1ms 짧으면 쌍자음, 정확히 T면 따로`() {
        assertEquals("ㄲ", editor().apply { type('ㄱ', 0); type('ㄱ', 249) }.text)
        assertEquals("ㄱㄱ", editor().apply { type('ㄱ', 0); type('ㄱ', 250) }.text)
    }

    @Test
    fun `T를 바꾸면 바로 적용된다`() {
        val editor = editor()
        editor.composer.doubleTapMillis = 100
        editor.type('ㄱ', 0)
        editor.type('ㄱ', 150)
        assertEquals("ㄱㄱ", editor.text)
    }

    @Test
    fun `두 탭 사이에 DEL이 끼면 빨라도 합치지 않는다`() {
        val editor = editor().apply { type('ㄱ', 0); type('ㄱ', 10) } // ㄲ
        editor.delete() // ㄱ
        editor.type('ㄱ', 20)
        assertEquals("ㄱㄱ", editor.text)
    }

    @Test
    fun `두 탭 사이에 조합을 끝내면 빨라도 합치지 않는다`() {
        val editor = editor().apply { type('ㄱ', 0); finish(); type('ㄱ', 10) }
        assertEquals("ㄱㄱ", editor.text)
    }

    @Test
    fun `두 탭 사이에 다른 키가 끼면 빨라도 합치지 않는다`() {
        assertEquals("ㄱㄴㄱ", editor().apply { type('ㄱ', 0); type('ㄴ', 10); type('ㄱ', 20) }.text)
    }

    @Test
    fun `받침이 될 수 없는 쌍자음은 앞 글자를 확정하고 다음 글자로 넘어간다`() {
        val composer = HangulComposer()
        "ㅎㅏㄹㄱ".forEachIndexed { i, jamo -> composer.type(jamo, i * 1000L) }
        assertEquals(Edit("할", "ㄲ"), composer.type('ㄱ', 3100))

        val composer2 = HangulComposer()
        "ㅇㅓㄷ".forEachIndexed { i, jamo -> composer2.type(jamo, i * 1000L) }
        assertEquals(Edit("어", "ㄸ"), composer2.type('ㄷ', 2100))
    }

    @Test
    fun `받침 뒤 모음은 받침을 데려가고 앞 글자를 확정한다`() {
        val composer = HangulComposer()
        "ㄱㅏㅣㅁ".forEachIndexed { i, jamo -> composer.type(jamo, i * 1000L) }
        assertEquals(Edit("개", "미"), composer.type('ㅣ', 4000))
    }

    @Test
    fun `DEL은 조합 중인 글자 안에서 한 단계씩 되돌린다`() {
        val editor = editor().typeAll("ㄱㅗㅏㅣㄴ")
        assertEquals("괜", editor.text)
        val steps = mutableListOf<String>()
        repeat(5) {
            editor.delete()
            steps += editor.text
        }
        assertEquals(listOf("괘", "과", "고", "ㄱ", ""), steps)
        assertFalse(editor.composer.isComposing)
    }

    @Test
    fun `DEL은 앞 글자로 넘어가지 않는다`() {
        // 앉 다음 ㅏ면 안 + 자. 자에서 DEL은 앉으로 돌아가지 않고 ㅈ만 남긴다.
        val editor = editor().typeAll("ㅇㅏㄴㅈㅏ")
        assertEquals("안자", editor.text)
        editor.delete()
        assertEquals("안ㅈ", editor.text)
        editor.delete()
        assertEquals("안", editor.text)
    }

    @Test
    fun `조합 중이 아닐 때 DEL은 앱에 맡긴다`() {
        val composer = HangulComposer()
        assertNull(composer.delete())
        composer.type('ㄱ', 0)
        composer.finish()
        assertNull(composer.delete())
    }

    @Test
    fun `finish는 조합 중이던 글자를 돌려주고 reset은 버린다`() {
        val composer = HangulComposer()
        "ㅎㅏㄴ".forEachIndexed { i, jamo -> composer.type(jamo, i * 1000L) }
        assertEquals("한", composer.finish())
        assertEquals("", composer.composing)

        "ㄱㅏ".forEachIndexed { i, jamo -> composer.type(jamo, 10_000 + i * 1000L) }
        composer.reset()
        assertEquals("", composer.composing)
        assertEquals(Edit("", "ㄱ"), composer.type('ㄱ', 20_000))
    }

    @Test
    fun `자판에 없는 자모는 받지 않는다`() {
        assertFailsWith<IllegalArgumentException> { HangulComposer().type('ㄲ', 0) }
        assertFailsWith<IllegalArgumentException> { HangulComposer().type('ㅐ', 0) }
        assertFailsWith<IllegalArgumentException> { HangulComposer().type('a', 0) }
    }
}
