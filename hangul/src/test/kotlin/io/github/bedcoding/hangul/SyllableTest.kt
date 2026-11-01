package io.github.bedcoding.hangul

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SyllableTest {
    @Test
    fun `첫 음절과 마지막 음절`() {
        assertEquals('가', composeSyllable(0, 0))
        assertEquals('힣', composeSyllable(18, 20, 27))
    }

    @Test
    fun `받침이 있는 음절`() {
        // ㅎ은 초성 18, ㅏ는 중성 0, ㄱ은 종성 1
        assertEquals('학', composeSyllable(18, 0, 1))
    }

    @Test
    fun `범위를 벗어난 번호는 받지 않는다`() {
        assertFailsWith<IllegalArgumentException> { composeSyllable(19, 0) }
        assertFailsWith<IllegalArgumentException> { composeSyllable(0, 21) }
        assertFailsWith<IllegalArgumentException> { composeSyllable(0, 0, 28) }
    }
}
