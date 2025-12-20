// SPDX-License-Identifier: Apache-2.0

package tools.kmp.spayd

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpaydTest {
    private val iban = IBAN.fromString("CZ9106000000000000000123")
    private val bic = BIC.fromString("CEKOCZPP")
    private val prefix = "SPD*1.0*"
    private val acc = "ACC:$iban*"
    private val accBic = "ACC:$iban+$bic*"
    private val validFull = "$prefix${acc}AM:450.00*CC:CZK*MSG:PLATBA ZA ZBOZI*X-VS:1234567890"

    private fun Spayd.Companion.decodeFromString(spayd: String): Spayd = Spayd.Decoder.Builder().build().decode(spayd)
    private fun Spayd.encodeToString(optimizeForQr: Boolean): String =
        Spayd.Encoder.Builder().optimizeForQr(optimizeForQr).build().encode(this)
    @Test
    fun testSplit() {
        assertContentEquals(
            listOf("SPD", "1.0:0:"),
            "SPD:1.0:0:".split(':', limit = 2),
            """Unexpected split result: "SPD:1.0:0:".split(':', limit = 2) != listOf("SPD", "1.0:0:")"""
        )
        assertContentEquals(
            listOf("", "SPD", "1.0", "", ""),
            "*SPD*1.0**".split('*'),
            """Unexpected split result: "SPD*1.0**".split('*') != listOf("SPD", "1.0", "", "")"""
        )
        assertContentEquals(
            listOf("SPD", "1.0", ""),
            "SPD*1.0*".split(Regex("\\*")),
            """Unexpected split result: "SPD*1.0*".split(Regex("\\*")) != listOf("SPD", "1.0", "")"""
        )
    }

    @Test
    fun testDecode() {
        val digits = setOf('A', 'a')
        for (digit in digits) {
            assertEquals(10, digit.digitToIntOrNull(16), "failed for '$digit'")
        }
    }

    @Test
    fun `test parsing`() {
        Spayd.decodeFromString(validFull)
    }

    @Test
    fun `bad prefix throws`() {
        val invalidStrings = listOf(
            // Missing SPD prefix
            "",
            "123*1.0*Content",
            "spd*1.0*Content", // Lowercase prefix
            " SPD*1.0*Content", // Leading space
            "SPD *1.0*Content", // Space after prefix

            // Invalid version format
            "SPD**Content", // Missing version
            "SPD*1,0*Content", // Comma instead of decimal point
            "SPD*1*Content", // No decimal point
            "SPD*.1*Content", // Missing integer part
            "SPD*1.*Content", // Missing decimal part
            "SPD*X.0*Content", // Non-numeric integer part
            "SPD*1.Y*Content", // Non-numeric decimal part

            // Invalid star pattern
            "SPD*1.0", // Missing star after version
            "SPD1.0*Content", // Missing star between SPD and version

            // Empty content (regex requires content after last star)
            "SPD*1.0*",

            // Other invalid patterns
            "ASPDX*1.0*Content", // SPD not at beginning
            "SPD:1.0*Content", // Wrong separator between SPD and version
            "SPD*1:0*Content", // Wrong separator in version
            "SPD*1.0:Content"  // Wrong separator after version
        )

        for ((index, invalidString) in invalidStrings.withIndex()) {
            val exception = assertFailsWith<IllegalArgumentException>(
                message = "Expected IllegalArgumentException for string: '$invalidString'",
                block = { Spayd.decodeFromString(invalidString) }
            )
            assertEquals(
                "Missing required prefix 'SPD*{VERSION}*'",
                exception.message
            )
        }
    }

    @Test
    fun `test missing colon delimiter in key-value pairs`() {
        run {
            val cases = listOf("SPD*1.0*ACC", "SPD*1.0*ACC*")
            for (case in cases) {
                val exception = assertFailsWith<IllegalArgumentException> { Spayd.decodeFromString(case) }
                assertEquals("Invalid key-value pair at index 0: missing ':' delimiter.", exception.message)
            }
        }
        run {
            val cases = listOf("SPD*1.0*X-A:123*MSG", "SPD*1.0*X-A:123*MSG*")
            for (case in cases) {
                val exception = assertFailsWith<IllegalArgumentException> { Spayd.decodeFromString(case) }
                assertEquals("Invalid key-value pair at index 1: missing ':' delimiter.", exception.message)
            }
        }
    }

    @Test
    fun `test illegal characters in key-value pair KEYS`() {
        fun performTest(input: String, idx: Int, c: Char, cidx: Int) {
            val ex = assertFailsWith<IllegalArgumentException> { Spayd.decodeFromString(input) }
            val expected =
                "Key-value at index $idx contains illegal character '$c' at index ${cidx}. Allowed key characters: [A-Z-]"
            assertEquals(expected, ex.message)
        }
        performTest("SPD*1.0*X-A1:a", 0, '1', 3)
        performTest("SPD*1.0*X-A1:a*", 0, '1', 3)
        performTest("SPD*1.0*X-A:a*MSG?:123", 1, '?', 3)
        performTest("SPD*1.0*X-A:a*MSG?:123*", 1, '?', 3)

        Spayd.decodeFromString("$prefix${acc}X--:a*")
    }

    @Test
    fun `test illegal prefix in key-value pair KEYS`() {
        fun performTest(input: String) {
            val ex = assertFailsWith<IllegalArgumentException> { Spayd.decodeFromString(input) }
            val expected = "Custom keys must start with 'X-'."
            assertEquals(expected, ex.message)
        }

        performTest("SPD*1.0*XX-A:a")
        performTest("SPD*1.0*AB:c")
    }

    @Test
    fun `test valid acc`() {
        run {
            val result = Spayd.decodeFromString("$prefix${acc}")

            assertEquals(IbanBic(iban, null), result.account)
        }
        run {
            val result = Spayd.decodeFromString("$prefix${accBic}")
            assertEquals(IbanBic(iban, bic), result.account)
        }
    }

    @Test
    fun `test valid alt-acc`() {
        run {
            val result = Spayd.decodeFromString("$prefix${acc}ALT-ACC:$iban,${iban}+${bic}".also(::println))
            assertEquals(setOf(IbanBic(iban, null), IbanBic(iban, bic)), result.altAccounts)
        }
    }

    @Test
    fun `test real spayd decode`() {
        val string =
            "SPD*1.0*ACC:CZ0608000000192235210247*ALT-ACC:CZ9003000000192235210247,CZ4601000000192235210247*AM:399*CC:CZK*RN:T-Mobile Czech Republic a.s.*X-VS:1113334445*X-SS:11*MSG:T-Mobile - QR platba123%C3%A1%C3%A9 %E2%80%B0%2a"
        val result = Spayd.decodeFromString(string)
        assertEquals("CZ0608000000192235210247", result.account.iban.value)
        assertContentEquals(
            listOf("CZ9003000000192235210247", "CZ4601000000192235210247"),
            result.altAccounts?.map { it.iban.value })
        assertEquals("399", result.amount!!.value)
        assertEquals("CZK", result.currency!!.code)
        assertEquals("T-Mobile Czech Republic a.s.", result.recipient?.value)
        assertEquals("1113334445", result.vs!!.value)
        assertEquals("11", result.ss!!.value)
        assertEquals("T-Mobile - QR platba123áé ‰*", result.message!!.value)
    }

    @Test
    fun `test duplicate error message`() {
        val string =
            "SPD*1.0*ACC:CZ0608000000192235210247*ALT-ACC:CZ9003000000192235210247,CZ4601000000192235210247*AM:399*CC:CZK*RN:T-Mobile Czech Republic a.s.*X-CUSTOM:123*X-VS:1113334445*X-SS:11*MSG:T-Mobile - QR platba123%C3%A1%C3%A9 %E2%80%B0%2a*X-CUSTOM:456*ALT-ACC:CZ9106000000000000000123,CZ9106000000000000000123"

        val ex = assertFailsWith<IllegalArgumentException> { Spayd.decodeFromString(string) }
        assertEquals("Duplicate keys found: [ALT-ACC: at indexes 1, 10; X-CUSTOM: at indexes 5, 9]", ex.message)
    }

    @Test
    fun `test real spayd encode`() {
        val spayd = Spayd.Builder()
            .account(IbanBic.fromString("CZ0608000000192235210247"))
            .altAccount(IbanBic.fromString("CZ9003000000192235210247"))
            .altAccount(IbanBic.fromString("CZ4601000000192235210247"))
            .amount(Amount.fromString("399.01"))
            .currency(Currency.fromString("CZK"))
            .message(Message.fromString("T-Mobile - QR platba123áé ‰*"))
            .recipient(Recipient.fromString("T-Mobile Czech Republic a.s."))
            .vs(VS.fromString("1113334445"))
            .ss(SS.fromString("11"))
            .build()

        val expectedUnoptimized =
            "SPD*1.0*ACC:CZ0608000000192235210247*ALT-ACC:CZ9003000000192235210247,CZ4601000000192235210247*AM:399.01*CC:CZK*MSG:T-Mobile - QR platba123%C3%A1%C3%A9 %E2%80%B0%2A*RN:T-Mobile Czech Republic a.s.*X-VS:1113334445*X-SS:11*"
        assertEquals(expectedUnoptimized, spayd.encodeToString(false))

        val expectedOptimized =
            "SPD*1.0*ACC:CZ0608000000192235210247*ALT-ACC:CZ9003000000192235210247,CZ4601000000192235210247*AM:399.01*CC:CZK*MSG:T-MOBILE - QR PLATBA123%C3%81%C3%89 %E2%80%B0%2A*RN:T-MOBILE CZECH REPUBLIC A.S.*X-VS:1113334445*X-SS:11*"
        assertEquals(expectedOptimized, spayd.encodeToString(true))
    }

}
