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
        SPAYD.decodeFromString(validFull)
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
                block = { SPAYD.decodeFromString(invalidString) }
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
                val exception = assertFailsWith<IllegalArgumentException> { SPAYD.decodeFromString(case) }
                assertEquals("Invalid key-value pair at index 0: missing ':' delimiter.", exception.message)
            }
        }
        run {
            val cases = listOf("SPD*1.0*X-A:123*MSG", "SPD*1.0*X-A:123*MSG*")
            for (case in cases) {
                val exception = assertFailsWith<IllegalArgumentException> { SPAYD.decodeFromString(case) }
                assertEquals("Invalid key-value pair at index 1: missing ':' delimiter.", exception.message)
            }
        }
    }

    @Test
    fun `test illegal characters in key-value pair KEYS`() {
        fun performTest(input: String, idx: Int, c: Char, cidx: Int) {
            val ex = assertFailsWith<IllegalArgumentException> { SPAYD.decodeFromString(input) }
            val expected =
                "Key-value at index $idx contains illegal character '$c' at index ${cidx}. Allowed key characters: [A-Z-]"
            assertEquals(expected, ex.message)
        }
        performTest("SPD*1.0*X-A1:a", 0, '1', 3)
        performTest("SPD*1.0*X-A1:a*", 0, '1', 3)
        performTest("SPD*1.0*X-A:a*MSG?:123", 1, '?', 3)
        performTest("SPD*1.0*X-A:a*MSG?:123*", 1, '?', 3)

        SPAYD.decodeFromString("$prefix${acc}X--:a*")
    }

    @Test
    fun `test illegal prefix in key-value pair KEYS`() {
        fun performTest(input: String) {
            val ex = assertFailsWith<IllegalArgumentException> { SPAYD.decodeFromString(input) }
            val expected = "Custom keys must start with 'X-'."
            assertEquals(expected, ex.message)
        }

        performTest("SPD*1.0*XX-A:a")
        performTest("SPD*1.0*AB:c")
    }

    @Test
    fun `test valid acc`() {
        run {
            val result = SPAYD.decodeFromString("$prefix${acc}")

            assertEquals(Account(IbanBic(iban, null)), result.account)
        }
        run {
            val result = SPAYD.decodeFromString("$prefix${accBic}")
            assertEquals(Account(IbanBic(iban, bic)), result.account)
        }
    }

    @Test
    fun `test valid alt-acc`() {
        run {
            val result = SPAYD.decodeFromString("$prefix${acc}ALT-ACC:$iban,${iban}+${bic}".also(::println))
            assertEquals(AltAccounts(listOf(IbanBic(iban, null), IbanBic(iban, bic))), result.altAccounts)
        }
    }

    @Test
    fun `test real spayd decode`() {
        val string =
            "SPD*1.0*ACC:CZ0608000000192235210247*ALT-ACC:CZ9003000000192235210247,CZ4601000000192235210247*AM:399*CC:CZK*RN:T-Mobile Czech Republic a.s.*X-VS:1113334445*X-SS:11*MSG:T-Mobile - QR platba123%C3%A1%C3%A9 %E2%80%B0%2a"
        val result = SPAYD.decodeFromString(string)
        assertEquals("CZ0608000000192235210247", result.account.value.iban.value)
        assertContentEquals(
            listOf("CZ9003000000192235210247", "CZ4601000000192235210247"),
            result.altAccounts?.accounts?.map { it.iban.value })
        assertEquals("399.00", result.amount!!.value)
        assertEquals("CZK", result.currency!!.code)
        assertEquals("T-Mobile Czech Republic a.s.", result.recipient?.value)
        assertEquals("1113334445", result.vs!!.value)
        assertEquals("11", result.ss!!.value)
        assertEquals("T-Mobile - QR platba123áé ‰*", result.message!!.value)
    }

    @Test
    fun `test real spayd encode`() {
        val spayd = SPAYD(
            Account.fromString("CZ0608000000192235210247"),
            AltAccounts(
                listOf(
                    IbanBic.fromString("CZ9003000000192235210247"),
                    IbanBic.fromString("CZ4601000000192235210247")
                )
            ),
            Amount.fromString("399.00"),
            Currency.fromString("CZK"),
            null,
            null,
            Message.fromString("T-Mobile - QR platba123áé ‰*"),
            null,
            null,
            null,
            null,
            Recipient.fromString("T-Mobile Czech Republic a.s."),
            VS.fromString("1113334445"),
            SS.fromString("11"),
            null,
            null,
            null,
            null,
            emptyList()
        )
        val expectedUnoptimized =
            "SPD*1.0*ACC:CZ0608000000192235210247*ALT-ACC:CZ9003000000192235210247,CZ4601000000192235210247*AM:399.00*CC:CZK*MSG:T-Mobile - QR platba123%C3%A1%C3%A9 %E2%80%B0%2A*RN:T-Mobile Czech Republic a.s.*X-VS:1113334445*X-SS:11*"
        assertEquals(expectedUnoptimized, spayd.encodeToString(false))

        val expectedOptimized =
            "SPD*1.0*ACC:CZ0608000000192235210247*ALT-ACC:CZ9003000000192235210247,CZ4601000000192235210247*AM:399.00*CC:CZK*MSG:T-MOBILE - QR PLATBA123%C3%81%C3%89 %E2%80%B0%2A*RN:T-MOBILE CZECH REPUBLIC A.S.*X-VS:1113334445*X-SS:11*"
        assertEquals(expectedOptimized, spayd.encodeToString(true))
    }


}
