package io.github.bedcoding.hangul

/** T(더블탭 인식 시간)의 처음 값. 임시 값이고, 사용자가 설정 화면에서 바꾼다. */
const val DEFAULT_DOUBLE_TAP_MILLIS = 250L

/**
 * 입력창에 할 일. 조합 중이던 글자 자리에 [commit]을 확정해 넣고, 그 뒤에 [composing]을 조합 중인 글자로 둔다.
 * 안드로이드에서는 commit이 있으면 commitText, 그다음 setComposingText(composing)에 해당한다.
 */
data class Edit(val commit: String, val composing: String)

/**
 * 자모 키를 하나씩 받아 한글을 조합한다. 규칙은 CLAUDE.md의 「한글 조합 규칙」을 따른다.
 * 현재 시각을 직접 읽지 않고 키를 누른 시각을 인자로 받는다. 시각은 쌍자음 더블탭 판정에만 쓴다.
 *
 * @param doubleTapMillis 같은 자음 키를 이 시간(ms)보다 짧은 간격으로 다시 누르면 쌍자음이 된다 (T).
 */
class HangulComposer(var doubleTapMillis: Long = DEFAULT_DOUBLE_TAP_MILLIS) {

    /** 조합 중인 한 글자. 자모는 호환용 자모(ㄱ, ㅏ 같은 글자)로 들고, 겹받침은 앞 자음과 뒤 자음을 따로 둔다. */
    private data class Block(
        val cho: Char? = null,
        val jung: Char? = null,
        val jong: Char? = null,
        val jong2: Char? = null,
    ) {
        fun text(): String {
            if (cho == null || jung == null) return (cho ?: jung)?.toString().orEmpty()
            val finalJamo = when {
                jong == null -> null
                jong2 == null -> jong
                else -> COMPOUND_FINALS.getValue(jong to jong2)
            }
            val jongIndex = if (finalJamo == null) 0 else JONGSEONG.indexOf(finalJamo) + 1
            return composeSyllable(CHOSEONG.indexOf(cho), JUNGSEONG.indexOf(jung), jongIndex).toString()
        }
    }

    private var block: Block? = null

    // 지금 글자를 만드는 동안 거쳐 온 상태. DEL은 여기서 한 단계씩 되돌린다.
    private val history = mutableListOf<Block>()

    // 더블탭 판정용: 바로 전에 누른 자모 키, 누른 시각, 그 키가 쌍자음을 만들었는지.
    // 사이에 DEL이나 다른 입력이 끼면 lastKey를 비워서 합쳐지지 않게 한다.
    private var lastKey: Char? = null
    private var lastKeyTime = 0L
    private var lastKeyDoubled = false

    /** 조합 중인 글자. 없으면 빈 문자열. */
    val composing: String get() = block?.text().orEmpty()

    val isComposing: Boolean get() = block != null

    /** 자모 키를 누른다. [jamo]는 키에 적힌 기본 자모, [timeMillis]는 키를 누른 시각이다. */
    fun type(jamo: Char, timeMillis: Long): Edit {
        require(jamo in CONSONANTS || jamo in VOWELS) { "자판에 없는 자모: $jamo" }
        // 쌍자음으로 바꾼 직후의 같은 키는 다시 합치지 않고 새 자음으로 시작한다 (했습니다).
        val doubleTap = jamo in DOUBLES && jamo == lastKey && !lastKeyDoubled &&
            timeMillis - lastKeyTime < doubleTapMillis
        val edit = when {
            doubleTap -> doubleLastConsonant(jamo)
            jamo in VOWELS -> addVowel(jamo)
            else -> addConsonant(jamo)
        }
        lastKey = jamo
        lastKeyTime = timeMillis
        lastKeyDoubled = doubleTap
        return edit
    }

    /**
     * DEL. 조합 중인 글자 안에서 마지막 입력 한 단계만 되돌리고, 앞 글자로는 넘어가지 않는다.
     * 조합 중이 아니면 null을 돌려준다. 그때는 앱이 입력창에 지우기 키를 보낸다.
     */
    fun delete(): Edit? {
        lastKey = null
        if (block == null) return null
        block = history.removeLastOrNull()
        return Edit("", composing)
    }

    /** 조합을 끝낸다. 스페이스, 숫자, 자판 전환처럼 조합과 상관없는 입력 앞에서 부른다. 조합 중이던 글자를 돌려준다. */
    fun finish(): String {
        val text = composing
        clear()
        return text
    }

    /** 입력창이 바뀌거나 사용자가 커서를 옮겼을 때, 확정하지 않고 모든 상태를 버린다. */
    fun reset() = clear()

    private fun clear() {
        block = null
        history.clear()
        lastKey = null
    }

    private fun addVowel(vowel: Char): Edit {
        val current = block ?: return moveTo(null, Block(jung = vowel))
        return when {
            current.jung == null -> change(current, current.copy(jung = vowel))
            current.jong == null -> {
                val pair = VOWEL_PAIRS[current.jung to vowel]
                if (pair != null) change(current, current.copy(jung = pair)) else moveTo(current, Block(jung = vowel))
            }
            // 받침 뒤에 모음이 오면 받침(겹받침이면 뒤 자음)이 다음 글자의 첫소리로 넘어간다.
            current.jong2 != null -> moveTo(
                current.copy(jong2 = null),
                Block(cho = current.jong2),
                Block(cho = current.jong2, jung = vowel),
            )
            else -> moveTo(
                current.copy(jong = null),
                Block(cho = current.jong),
                Block(cho = current.jong, jung = vowel),
            )
        }
    }

    private fun addConsonant(consonant: Char): Edit {
        val current = block ?: return moveTo(null, Block(cho = consonant))
        val attached = when {
            // 모음이 없는 글자에는 받침을 달지 않는다. 자음만 칠 때 겹자음으로 합칠지는 확인 전이라 합치지 않는다.
            current.cho == null || current.jung == null -> null
            current.jong == null -> if (consonant in JONGSEONG) current.copy(jong = consonant) else null
            current.jong2 == null ->
                if ((current.jong to consonant) in COMPOUND_FINALS) current.copy(jong2 = consonant) else null
            else -> null
        }
        return if (attached != null) change(current, attached) else moveTo(current, Block(cho = consonant))
    }

    /** 방금 넣은 자음을 쌍자음으로 바꾼다. 받침이 될 수 없으면 다음 글자의 첫소리로 넘긴다. */
    private fun doubleLastConsonant(consonant: Char): Edit {
        val current = block ?: return addConsonant(consonant)
        val doubled = DOUBLES.getValue(consonant)
        // 방금 넣은 자음은 글자의 가장 뒤쪽 자리에 있으니 겹받침 뒤 자음, 받침, 초성 순서로 찾는다.
        return when (consonant) {
            // 쌍자음이 들어간 겹받침은 없다 (할까, 진짜).
            current.jong2 -> moveTo(current.copy(jong2 = null), Block(cho = doubled))
            // ㄲ ㅆ은 받침이 되지만 ㄸ ㅃ ㅉ은 될 수 없다 (어떻게).
            current.jong ->
                if (doubled in JONGSEONG) change(current, current.copy(jong = doubled))
                else moveTo(current.copy(jong = null), Block(cho = doubled))
            current.cho -> change(current, current.copy(cho = doubled))
            else -> addConsonant(consonant)
        }
    }

    /** 지금 글자를 바꾼다. DEL로 되돌릴 수 있게 바꾸기 전 상태를 남긴다. */
    private fun change(old: Block, new: Block): Edit {
        history += old
        block = new
        return Edit("", new.text())
    }

    /**
     * [done]을 확정하고 새 글자로 넘어간다. [steps]는 새 글자가 거쳐 온 상태이고, 마지막이 지금 상태다.
     * 새 글자의 DEL은 이 단계까지만 되돌린다.
     */
    private fun moveTo(done: Block?, vararg steps: Block): Edit {
        history.clear()
        history += steps.dropLast(1)
        val new = steps.last()
        block = new
        return Edit(done?.text().orEmpty(), new.text())
    }
}

// 유니코드 완성형의 초성, 중성, 종성 순서 (composeSyllable의 번호와 같다). 종성은 받침 없음(0) 다음부터 적었다.
private const val CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
private const val JUNGSEONG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
private const val JONGSEONG = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"

// 자판에 있는 자모
private const val CONSONANTS = "ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ"
private const val VOWELS = "ㅏㅑㅓㅕㅗㅛㅜㅠㅡㅣ"

/** 같은 키를 두 번 빠르게 눌러 만드는 쌍자음. */
private val DOUBLES = mapOf('ㄱ' to 'ㄲ', 'ㄷ' to 'ㄸ', 'ㅂ' to 'ㅃ', 'ㅅ' to 'ㅆ', 'ㅈ' to 'ㅉ')

/**
 * 앞 모음 다음에 이어 치면 합쳐지는 모음.
 * ㅙ와 ㅞ는 ㅣ로도(ㅗ ㅏ ㅣ, ㅜ ㅓ ㅣ), 앞 모음을 한 번 더 쳐서도(ㅗ ㅏ ㅏ, ㅜ ㅓ ㅓ) 만든다. 뒤의 것은 사용자 기억이다.
 */
private val VOWEL_PAIRS = mapOf(
    ('ㅏ' to 'ㅣ') to 'ㅐ', ('ㅑ' to 'ㅣ') to 'ㅒ', ('ㅓ' to 'ㅣ') to 'ㅔ', ('ㅕ' to 'ㅣ') to 'ㅖ',
    ('ㅗ' to 'ㅏ') to 'ㅘ', ('ㅗ' to 'ㅣ') to 'ㅚ', ('ㅘ' to 'ㅣ') to 'ㅙ', ('ㅘ' to 'ㅏ') to 'ㅙ',
    ('ㅜ' to 'ㅓ') to 'ㅝ', ('ㅜ' to 'ㅣ') to 'ㅟ', ('ㅝ' to 'ㅣ') to 'ㅞ', ('ㅝ' to 'ㅓ') to 'ㅞ',
    ('ㅡ' to 'ㅣ') to 'ㅢ',
)

/** 겹받침. */
private val COMPOUND_FINALS = mapOf(
    ('ㄱ' to 'ㅅ') to 'ㄳ', ('ㄴ' to 'ㅈ') to 'ㄵ', ('ㄴ' to 'ㅎ') to 'ㄶ',
    ('ㄹ' to 'ㄱ') to 'ㄺ', ('ㄹ' to 'ㅁ') to 'ㄻ', ('ㄹ' to 'ㅂ') to 'ㄼ', ('ㄹ' to 'ㅅ') to 'ㄽ',
    ('ㄹ' to 'ㅌ') to 'ㄾ', ('ㄹ' to 'ㅍ') to 'ㄿ', ('ㄹ' to 'ㅎ') to 'ㅀ', ('ㅂ' to 'ㅅ') to 'ㅄ',
)
