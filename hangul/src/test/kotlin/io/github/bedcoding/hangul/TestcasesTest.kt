package io.github.bedcoding.hangul

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * testcases/hangul.txt의 모든 줄을 돌린다. ~가 붙은 키는 바로 전 키와 T - 1ms, 없으면 정확히 T 간격으로 누른다.
 * 경계값으로 누르므로 '간격 < T면 합치고 ≥ T면 따로'가 그대로 검사된다. 표기가 T와 상관없는지 보려고 T를 여러 값으로 돌린다.
 */
class TestcasesTest {
    private data class Case(val line: Int, val expected: String, val keys: List<String>, val source: String)

    private val file = File(System.getProperty("testcases.dir") ?: "../testcases", "hangul.txt")

    private fun readCases(): List<Case> = file.readLines().withIndex()
        .filter { (_, text) -> text.isNotBlank() && !text.trimStart().startsWith("#") }
        .map { (index, text) ->
            val parts = text.split("|").map { it.trim() }
            if (parts.size != 3) fail("${index + 1}행의 형식이 틀렸다: $text")
            Case(index + 1, parts[0], parts[1].split(Regex("\\s+")), parts[2])
        }

    private fun run(case: Case, doubleTapMillis: Long): String {
        val editor = FakeEditor(HangulComposer(doubleTapMillis))
        var time = 0L
        for (token in case.keys) {
            val fast = token.startsWith("~")
            val key = token.removePrefix("~")
            time += if (fast) doubleTapMillis - 1 else doubleTapMillis
            if (key == "⌫") editor.delete() else editor.type(key.single(), time)
        }
        return editor.text
    }

    @Test
    fun `목록의 모든 줄이 기대 결과대로 나온다`() {
        val cases = readCases()
        assertTrue(cases.size >= 40, "목록을 제대로 읽지 못했다: ${cases.size}줄, ${file.absolutePath}")
        val failures = listOf(100L, 250L, 400L).flatMap { t ->
            cases.mapNotNull { case ->
                val actual = run(case, t)
                if (actual == case.expected) null
                else "${case.line}행 (T=$t): ${case.keys.joinToString(" ")} → 기대 ${case.expected}, 실제 $actual"
            }
        }
        assertTrue(failures.isEmpty(), "틀린 줄 ${failures.size}개:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `출처는 실측, 규칙, 임시 중 하나다`() {
        val wrong = readCases().filter { it.source !in setOf("실측", "규칙", "임시") }
        assertTrue(wrong.isEmpty(), wrong.joinToString("\n") { "${it.line}행: ${it.source}" })
    }
}
