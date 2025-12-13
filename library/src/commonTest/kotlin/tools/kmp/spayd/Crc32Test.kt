// SPDX-License-Identifier: Apache-2.0

package tools.kmp.spayd

import kotlin.test.Test
import kotlin.test.assertEquals

class Crc32Test {

    @Test
    fun crc32() {
        val result2 = Crc32Computer.compute("Orang Utan".encodeToByteArray())
        assertEquals(0x385171D6, result2)
    }

    @Test
    fun emptyInput() {
        val result = Crc32Computer.compute("".encodeToByteArray())
        assertEquals(0x0, result)
    }
}
