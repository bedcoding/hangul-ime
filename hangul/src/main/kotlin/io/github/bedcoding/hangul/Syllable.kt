package io.github.bedcoding.hangul

/** 유니코드 완성형 한글 음절의 첫 글자 '가'의 코드. */
const val SYLLABLE_BASE = 0xAC00

/**
 * 초성, 중성, 종성 번호로 완성형 한글 음절을 만든다.
 * 번호는 유니코드 순서를 따른다: 초성 0~18, 중성 0~20, 종성 0~27 (종성 0은 받침 없음).
 */
fun composeSyllable(cho: Int, jung: Int, jong: Int = 0): Char {
    require(cho in 0..18) { "초성 번호가 범위를 벗어남: $cho" }
    require(jung in 0..20) { "중성 번호가 범위를 벗어남: $jung" }
    require(jong in 0..27) { "종성 번호가 범위를 벗어남: $jong" }
    return Char(SYLLABLE_BASE + (cho * 21 + jung) * 28 + jong)
}
