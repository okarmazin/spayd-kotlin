// SPDX-License-Identifier: Apache-2.0

package tools.kmp.spayd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PercentCodecTest {

    @Test
    fun `test percent decoding`() {
        run {
            val encoded = "123%C3%A1%C3%A9 %E2%80%B0%2a %c3%a1%c3%a9%e2%80%b0%2a"
            val expected = "123áé ‰* áé‰*"
            assertEquals(expected, spaydPercentDecode(encoded))
        }
        run {
            val encoded = "123%C3%A1%C3%A9 %E2%80%B0%1"
            val ex = assertFailsWith<IllegalArgumentException> { spaydPercentDecode(encoded) }
            assertEquals("Invalid percent-encoded byte: Missing hexadecimal digit after '%'", ex.message)
        }
        run {
            val encoded = "123%C3%A1%C3%A9 %E2%80%B0%"
            val ex = assertFailsWith<IllegalArgumentException> { spaydPercentDecode(encoded) }
            assertEquals("Invalid percent-encoded byte: Missing hexadecimal digit after '%'", ex.message)
        }
    }

    @Test
    fun `test percent encoding`() {
        run {
            val text = "123áé ‰* áé‰*"
            val expected = "123%C3%A1%C3%A9 %E2%80%B0%2A %C3%A1%C3%A9%E2%80%B0%2A"
            assertEquals(expected, spaydPercentEncode(text, false))
        }
    }

}