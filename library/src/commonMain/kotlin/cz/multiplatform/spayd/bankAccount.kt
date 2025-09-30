// SPDX-License-Identifier: Apache-2.0

package cz.multiplatform.spayd

import kotlin.jvm.JvmStatic

@ConsistentCopyVisibility
data class CZBankAccount private constructor(
    val accountNumber: CZBankAccountNumber,
    val bankCode: BankCode
) {
    override fun toString() = "$accountNumber/$bankCode"

    companion object Companion {

        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun fromString(string: String): CZBankAccount {
            val string = string.trim()
            val splits = string.split('/')
            require(splits.size == 2) {
                "Invalid account number with bank code. Expected 2 parts separated by '/', but has ${splits.size} parts."
            }
            val number = CZBankAccountNumber.fromString(splits[0])
            val bankCode = BankCode.fromString(splits[1])

            return CZBankAccount(number, bankCode)
        }

        fun fromParts(number: CZBankAccountNumber, bankCode: BankCode) = CZBankAccount(number, bankCode)
    }
}

@ConsistentCopyVisibility
data class CZBankAccountNumber private constructor(val prefix: String, val accountNumber: String) {

    override fun toString(): String = "${if (prefix.isNotBlank()) "$prefix-" else ""}$accountNumber"

    companion object {
        // https://www.cnb.cz/export/sites/cnb/cs/legislativa/.galleries/vyhlasky/vyhlaska_169_2011.pdf
        private val weights = listOf(1, 2, 4, 8, 5, 10, 9, 7, 3, 6)
        private val regex = Regex("^\\d{2,10}$|^\\d{2,6}-\\d{2,10}$")

        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun fromString(string: String): CZBankAccountNumber {
            val string = string.trim()
            require(string.matches(regex)) { "Invalid CZ bank account number format." }
            val parts = string.split("-")
            return when (parts.size) {
                1 -> {
                    validatePart(parts[0], 10, 2)
                    CZBankAccountNumber("", parts[0].dropWhile { it == '0' })
                }

                2 -> {
                    validatePart(parts[0], 6, 0)
                    validatePart(parts[1], 10, 2)
                    CZBankAccountNumber(parts[0].dropWhile { it == '0' }, parts[1].dropWhile { it == '0' })
                }

                else -> {
                    // If this ever happens, the regex engine has a bug.
                    throw IllegalStateException("CZ bank number regex matched, but split into more than 2 parts (${parts.size}).")
                }
            }
        }

        private fun validatePart(part: String, maxLength: Int, minNotZeroDigits: Int) {
            require(part.length <= maxLength) { "Invalid CZ bank account number part length (${part.length}). Must be <= $maxLength." }
            var notZeroCount = 0
            val sum = part.padStart(maxLength, '0').reversed().foldIndexed(0) { index, acc, c ->
                if (c != '0') notZeroCount++
                acc + c.digitToInt() * weights[index]
            }
            require(notZeroCount >= minNotZeroDigits) { "Invalid CZ bank account number - must have at least $minNotZeroDigits non-zero digits." }
            require(sum % 11 == 0) { "Invalid CZ bank account number - check digit is invalid." }
        }
    }
}

/**
 * Cesky kod banky (kod platebniho styku) - 4 cislice
 */
@ConsistentCopyVisibility
data class BankCode private constructor(val value: String) {
    override fun toString() = value

    companion object {
        private val bankCodeRegex = Regex("^\\d{4}$")

        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun fromString(string: String): BankCode {
            require(string.matches(bankCodeRegex)) {
                "Invalid bank code. Must be exactly 4 digits. Input has either bad length, or contains non-digits."
            }
            return BankCode(string)
        }
    }
}

@ConsistentCopyVisibility
data class IBAN private constructor(val value: String) {
    override fun toString(): String = value

    fun readable(): String = value.chunked(4).joinToString(" ")

    companion object {
        const val MIN_LENGTH = 16
        const val MAX_LENGTH = 34

        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun fromString(string: String): IBAN {
            val string = string.filterNot { it.isWhitespace() }
            require(string.length in MIN_LENGTH..MAX_LENGTH) {
                "IBAN length (${string.length}) is not in the allowed range $MIN_LENGTH..$MAX_LENGTH."
            }
            require(string.take(2).all { it in 'A'..'Z' }) { "Invalid country code." }
            require(string.drop(2).take(2).all { it in '0'..'9' }) { "Invalid check digits." }

            val rearrangedDigits = buildString {
                for (c in string.drop(4) + string.take(4)) {
                    if (c.isDigit()) {
                        append(c.toString())
                    } else {
                        append((c.code - 'A'.code + 10).toString())
                    }
                }
            }

            require(mod97(rearrangedDigits) == 1) { "Invalid IBAN: did not pass mod97 check." }

            return IBAN(string)
        }

        fun from(czBankAccount: CZBankAccount): IBAN {
            val bankCode = czBankAccount.bankCode.value
            val prefix = czBankAccount.accountNumber.prefix.padStart(6, '0')
            val account = czBankAccount.accountNumber.accountNumber.padStart(10, '0')
            // 123500 = CZ00
            val rearrangedDigits = "$bankCode$prefix${account}123500"
            val checkNum = 98 - mod97(rearrangedDigits)
            val checkDigits = checkNum.toString().padStart(2, '0')
            return IBAN("CZ$checkDigits$bankCode$prefix$account")
        }

        private fun mod97(digits: String): Int {
            var remainder = 0
            for (char in digits) {
                val digit = char - '0'
                remainder = (remainder * 10 + digit) % 97
            }

            return remainder
        }
    }
}
