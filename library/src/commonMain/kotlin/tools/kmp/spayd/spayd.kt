// SPDX-License-Identifier: Apache-2.0

package tools.kmp.spayd

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

public class Spayd(
    /** ACC: Account, the only required attribute */
    public val account: Account,
    /** ALT-ACC */
    public val altAccounts: AltAccounts?,
    /** AM */
    public val amount: Amount?,
    /** CC */
    public val currency: Currency?,
    /** CRC32 */
    public val crc32: CRC32?,
    /** DT: Datum splatnosti */
    public val dueDate: DueDate?,
    /** MSG */
    public val message: Message?,
    /** NT */
    public val notificationType: NotificationType?,
    /** NTA: Notification recipient. Either a phone number, or an email address depending on the notification type. */
    public val notificationAddress: NotificationAddress?,
    /** PT */
    public val paymentType: PaymentType?,
    /** RF */
    public val senderReference: SenderReference?,
    /** RN */
    public val recipient: Recipient?,

    /** X-VS */
    public val vs: VS?,
    /** X-SS */
    public val ss: SS?,
    /** X-KS */
    public val ks: KS?,
    /** X-PER */
    public val retryDays: CzRetryDays?,
    /** X-ID */
    public val paymentId: CzPaymentId?,
    /** X-URL */
    public val url: URL?,

    public val customAttributes: List<CustomAttribute>,
) {
    public fun encodeToString(optimizeForQr: Boolean): String = encode(this, optimizeForQr)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Spayd) return false

        if (account != other.account) return false
        if (altAccounts != other.altAccounts) return false
        if (amount != other.amount) return false
        if (currency != other.currency) return false
        if (crc32 != other.crc32) return false
        if (dueDate != other.dueDate) return false
        if (message != other.message) return false
        if (notificationType != other.notificationType) return false
        if (notificationAddress != other.notificationAddress) return false
        if (paymentType != other.paymentType) return false
        if (senderReference != other.senderReference) return false
        if (recipient != other.recipient) return false
        if (vs != other.vs) return false
        if (ss != other.ss) return false
        if (ks != other.ks) return false
        if (retryDays != other.retryDays) return false
        if (paymentId != other.paymentId) return false
        if (url != other.url) return false
        if (customAttributes != other.customAttributes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = account.hashCode()
        result = 31 * result + (altAccounts?.hashCode() ?: 0)
        result = 31 * result + (amount?.hashCode() ?: 0)
        result = 31 * result + (currency?.hashCode() ?: 0)
        result = 31 * result + (crc32?.hashCode() ?: 0)
        result = 31 * result + (dueDate?.hashCode() ?: 0)
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (notificationType?.hashCode() ?: 0)
        result = 31 * result + (notificationAddress?.hashCode() ?: 0)
        result = 31 * result + (paymentType?.hashCode() ?: 0)
        result = 31 * result + (senderReference?.hashCode() ?: 0)
        result = 31 * result + (recipient?.hashCode() ?: 0)
        result = 31 * result + (vs?.hashCode() ?: 0)
        result = 31 * result + (ss?.hashCode() ?: 0)
        result = 31 * result + (ks?.hashCode() ?: 0)
        result = 31 * result + (retryDays?.hashCode() ?: 0)
        result = 31 * result + (paymentId?.hashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + customAttributes.hashCode()
        return result
    }

    public companion object Companion {
        @Throws(IllegalArgumentException::class)
        @JvmStatic
        public fun decodeFromString(spaydString: String): Spayd = decode(spaydString)
    }
}

internal interface SpaydAttribute {
    val key: String
    fun encodedValue(optimizeForQr: Boolean = true): String
}

@JvmInline
public value class Account(public val value: IbanBic) : SpaydAttribute {
    override val key: String get() = "ACC"

    override fun encodedValue(optimizeForQr: Boolean): String = value.encodedValue()

    internal companion object {
        @JvmStatic
        fun fromString(value: String): Account = Account(IbanBic.fromString(value))
    }
}

@JvmInline
public value class AltAccounts(public val accounts: List<IbanBic>) : SpaydAttribute {
    override val key: String get() = "ALT-ACC"

    override fun encodedValue(optimizeForQr: Boolean): String = accounts.joinToString(",") { it.encodedValue() }

    internal companion object {
        @JvmStatic
        fun fromString(value: String): AltAccounts = try {
            AltAccounts(value.split(',').map(IbanBic::fromString))
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Cannot parse ALT-ACC: ${e.message}", e)
        }
    }
}

public data class IbanBic(val iban: IBAN, val bic: BIC? = null) {
    internal fun encodedValue(): String {
        val content = iban.value + (bic?.value?.let { "+$it" } ?: "")
        return spaydPercentEncode(content, true)
    }

    internal companion object {
        @JvmStatic
        fun fromString(value: String): IbanBic {
            val parts = value.split('+', limit = 2)
            val iban = parts[0]
            val bic = parts.getOrNull(1)
            return IbanBic(IBAN.fromString(iban), bic?.let(BIC::fromString))
        }

        internal fun fromParts(iban: IBAN, bic: BIC? = null): IbanBic = IbanBic(iban, bic)
    }
}

@ConsistentCopyVisibility
public data class IBAN private constructor(val value: String) {
    override fun toString(): String = value

    public fun readable(): String = value.chunked(4).joinToString(" ")

    public companion object {
        public const val MIN_LENGTH: Int = 16
        public const val MAX_LENGTH: Int = 34

        @JvmStatic
        @Throws(IllegalArgumentException::class)
        public fun fromString(string: String): IBAN {
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

        public fun from(czBankAccount: CZBankAccount): IBAN {
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

@ConsistentCopyVisibility
public data class BIC private constructor(val value: String) {
    override fun toString(): String = value

    public companion object {

        @JvmStatic
        @Throws(IllegalArgumentException::class)
        public fun fromString(string: String): BIC {
            // From Wikipedia:
            // 4 letters, 2 letters, 2 alphanum, 3 alphanum (optional)
            require(string.length == 8 || string.length == 11) {
                "BIC must be 8 OR 11 characters long, was ${string.length}."
            }
            val letters = string.take(6)
            val alphanum = string.drop(6)
            require(letters.all { it in 'A'..'Z' }) { "BIC must start with 6 uppercase letters." }
            require(alphanum.all { it in 'A'..'Z' || it in '0'..'9' }) { "BIC must end with 2 or 5 alphanumeric characters." }
            return BIC(string)
        }
    }
}

@ConsistentCopyVisibility
public data class CZBankAccount(
    val accountNumber: CZBankAccountNumber,
    val bankCode: BankCode
) {
    public override fun toString(): String = "$accountNumber/$bankCode"

    internal companion object {

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
public data class CZBankAccountNumber private constructor(val prefix: String, val accountNumber: String) {

    override fun toString(): String = "${if (prefix.isNotBlank()) "$prefix-" else ""}$accountNumber"

    public companion object {
        // https://www.cnb.cz/export/sites/cnb/cs/legislativa/.galleries/vyhlasky/vyhlaska_169_2011.pdf
        private val weights = listOf(1, 2, 4, 8, 5, 10, 9, 7, 3, 6)
        private val regex = Regex("^[0-9]{2,10}$|^[0-9]{2,6}-[0-9]{2,10}$")

        @JvmStatic
        @Throws(IllegalArgumentException::class)
        public fun fromString(string: String): CZBankAccountNumber {
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
public data class BankCode private constructor(val value: String) {
    override fun toString(): String = value

    public companion object {
        @JvmStatic
        @Throws(IllegalArgumentException::class)
        public fun fromString(string: String): BankCode {
            require(string.length == 4 && string.all { it in '0'..'9' }) {
                "Invalid bank code. Must be exactly 4 digits. Input has either bad length, or contains non-digits."
            }
            return BankCode(string)
        }
    }
}

@JvmInline
public value class Amount private constructor(public val value: String) : SpaydAttribute {
    override val key: String get() = "AM"

    override fun encodedValue(optimizeForQr: Boolean): String = value

    public companion object {
        @JvmStatic
        public fun fromString(value: String): Amount {
            require(value.length in 1..10 && value != ".") { "AM: Amount must not exceed 10 characters and must have at least 1 digit" }
            require(value.all { isAsciiDigit(it) || it == '.' }) { "AM: Amount must contain only digits or a decimal point" }
            val parts = value.split('.')
            require(parts.size <= 2) { "AM: Amount must contain at most one decimal point" }
            val integerPart = parts[0].padStart(1, '0')
            val decimalPart = parts.getOrNull(1).orEmpty()

            require(decimalPart.length <= 2) { "AM: Amount must be a decimal number with at most 2 decimal places." }

            return Amount("$integerPart.${decimalPart.padEnd(2, '0')}")
        }
    }
}

@JvmInline
public value class Currency private constructor(public val code: String) : SpaydAttribute {
    override val key: String get() = "CC"

    override fun encodedValue(optimizeForQr: Boolean): String = code.uppercase()

    public companion object {
        @JvmStatic
        public fun fromString(value: String): Currency {
            require(value.length == 3 && value.uppercase().all { it in 'A'..'Z' }) {
                "CC: Currency code must be exactly 3 letters."
            }
            return Currency(value)
        }
    }
}

@JvmInline
public value class CRC32 private constructor(public val value: String) : SpaydAttribute {
    override val key: String get() = "CRC32"

    override fun encodedValue(optimizeForQr: Boolean): String = value.uppercase()

    public companion object {
        private const val LENGTH: Int = 8
        private val hexDigits = ('0'..'9') + ('A'..'F') + ('a'..'f')

        @JvmStatic
        public fun fromString(value: String): CRC32 {
            require(value.length == LENGTH && value.all { it in hexDigits }) {
                "CRC32: CRC32 must be exactly $LENGTH hexadecimal digits."
            }

            return CRC32(value)
        }
    }
}

@ConsistentCopyVisibility
public data class DueDate private constructor(val year: Int, val monthNumber: Int, val dayOfMonth: Int) :
    SpaydAttribute {
    override val key: String get() = "DT"

    override fun encodedValue(optimizeForQr: Boolean): String =
        "$year${monthNumber.toString().padStart(2, '0')}${dayOfMonth.toString().padStart(2, '0')}"

    /**
     * ISO 8601 format: YYYY-MM-DD
     */
    override fun toString(): String =
        "$year-${monthNumber.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}"

    public companion object Companion {

        public fun of(year: Int, monthNumber: Int, dayOfMonth: Int): DueDate = create(year, monthNumber, dayOfMonth)

        @JvmStatic
        internal fun fromString(value: String): DueDate {
            require(value.length == 8 && value.all { it in '0'..'9' }) { "Date must be exactly 8 digits (YYYYMMDD)." }
            val year = value.take(4).toInt()
            val month = value.drop(4).take(2).toInt()
            val day = value.takeLast(2).toInt()
            return create(year, month, day)
        }

        private fun create(year: Int, month: Int, day: Int): DueDate {
            require(year >= 1900) { "Unreasonable year: $year." }
            require(month in 1..12) { "Month number must be between 1 and 12." }
            require(day in 1..daysInMonth(year, month)) { "Invalid day of month: $day" }
            return DueDate(
                year = year,
                monthNumber = month,
                dayOfMonth = day,
            )
        }

        private fun daysInMonth(year: Int, monthNumber: Int): Int = when (monthNumber) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> throw IllegalArgumentException("Invalid month number: $monthNumber")
        }

        private fun isLeapYear(year: Int): Boolean = (year % 400 == 0) || (year % 4 == 0 && year % 100 != 0)
    }
}

@JvmInline
public value class Message private constructor(public val value: String) : SpaydAttribute {
    override val key: String get() = "MSG"

    override fun encodedValue(optimizeForQr: Boolean): String = spaydPercentEncode(value, optimizeForQr)

    public companion object {
        public val MAX_LENGTH: Int = 60

        @JvmStatic
        public fun fromString(value: String): Message {
            require(value.length <= MAX_LENGTH) { "MSG: Message must not exceed $MAX_LENGTH characters." }
            return Message(value)
        }
    }
}

public enum class NotificationType : SpaydAttribute {
    /** NT:P */
    PHONE,

    /** NT:E */
    EMAIL;

    override val key: String get() = "NT"

    override fun encodedValue(optimizeForQr: Boolean): String = when (this) {
        PHONE -> "P"
        EMAIL -> "E"
    }
}

@JvmInline
public value class NotificationAddress private constructor(public val value: String) : SpaydAttribute {
    override val key: String get() = "NTA"

    override fun encodedValue(optimizeForQr: Boolean): String = spaydPercentEncode(value, optimizeForQr)

    public companion object {
        public const val MAX_LENGTH: Int = 320

        @JvmStatic
        public fun fromString(value: String): NotificationAddress {
            require(value.length <= MAX_LENGTH) { "NTA: Notification address must be at most $MAX_LENGTH characters long." }
            return NotificationAddress(value)
        }
    }
}

public sealed class PaymentType : SpaydAttribute {
    override val key: String get() = "PT"

    override fun encodedValue(optimizeForQr: Boolean): String = when (this) {
        is Custom -> value.uppercase()
        InstantPayment -> "IP"
    }

    /**
     * PT:IP
     *
     * Instant payment, if possible.
     */
    public data object InstantPayment : PaymentType()

    public data class Custom(val value: String) : PaymentType()

    public companion object {
        @JvmStatic
        public fun fromString(value: String): PaymentType {
            require(value.length in 1..3) { "PT: Payment type must be 1 to 3 characters long." }
            return when (value) {
                "IP" -> InstantPayment
                else -> Custom(value)
            }
        }
    }
}

@JvmInline
public value class SenderReference private constructor(public val value: String) : SpaydAttribute {
    override val key: String get() = "RF"

    override fun encodedValue(optimizeForQr: Boolean): String = spaydPercentEncode(value, optimizeForQr)

    public companion object {
        @JvmStatic
        public fun fromString(value: String): SenderReference {
            require(value.length in 1..16 && isAsciiDigits(value)) {
                "RF: Sender reference must be a string of digits from 1 to 16 characters long."
            }
            return SenderReference(value)
        }
    }
}

@JvmInline
public value class Recipient private constructor(public val value: String) : SpaydAttribute {
    override val key: String get() = "RN"

    override fun encodedValue(optimizeForQr: Boolean): String = spaydPercentEncode(value, optimizeForQr)

    public companion object {
        public const val MAX_LENGTH: Int = 35

        public fun fromString(value: String): Recipient {
            require(value.length <= MAX_LENGTH) { "RN: Recipient must be at most $MAX_LENGTH characters long." }
            return Recipient(value)
        }
    }
}

@JvmInline
public value class VS private constructor(public val value: String) : SpaydAttribute {
    override val key: String get() = "X-VS"

    override fun encodedValue(optimizeForQr: Boolean): String = value

    public companion object {
        @JvmStatic
        public fun fromString(value: String): VS {
            require(value.length in 1..10 && isAsciiDigits(value)) { "X-VS: VS must be 1 to 10 digits." }
            return VS(value)
        }
    }
}

@JvmInline
public value class SS private constructor(public val value: String) : SpaydAttribute {
    override val key: String get() = "X-SS"

    override fun encodedValue(optimizeForQr: Boolean): String = value

    public companion object {
        @JvmStatic
        public fun fromString(value: String): SS {
            require(value.length in 1..10 && isAsciiDigits(value)) { "X-SS: SS must be 1 to 10 digits." }
            return SS(value)
        }
    }
}

@JvmInline
public value class KS private constructor(public val value: String) : SpaydAttribute {
    override val key: String get() = "X-KS"

    override fun encodedValue(optimizeForQr: Boolean): String = value

    public companion object {
        @JvmStatic
        public fun fromString(value: String): KS {
            require(value.length in 1..10 && isAsciiDigits(value)) { "X-KS: KS must be 1 to 10 digits." }
            return KS(value)
        }
    }
}

@JvmInline
public value class CzRetryDays private constructor(public val value: Int) : SpaydAttribute {
    override val key: String get() = "X-PER"

    override fun encodedValue(optimizeForQr: Boolean): String = value.toString()

    public companion object {
        @JvmStatic
        public fun fromString(value: String): CzRetryDays {
            require(value.length in 1..2 && isAsciiDigits(value) && value.toInt() in 1..30) {
                "X-PER: Retry days must be a number from 1 to 30"
            }
            return CzRetryDays(value.toInt())
        }
    }
}

@JvmInline
public value class CzPaymentId private constructor(public val value: String) : SpaydAttribute {
    override val key: String get() = "X-ID"

    override fun encodedValue(optimizeForQr: Boolean): String = spaydPercentEncode(value, optimizeForQr)

    public companion object {
        @JvmStatic
        public fun fromString(value: String): CzPaymentId {
            require(value.length in 1..20) { "X-ID must be 1 to 20 characters long." }
            return CzPaymentId(value)
        }
    }
}

@JvmInline
public value class URL private constructor(public val value: String) : SpaydAttribute {

    override val key: String get() = "X-URL"

    override fun encodedValue(optimizeForQr: Boolean): String = spaydPercentEncode(value, optimizeForQr)

    public companion object {
        public const val MAX_LENGTH: Int = 140

        @JvmStatic
        public fun fromString(value: String): URL {
            require(value.length <= MAX_LENGTH) { "X-URL: URL must be at most $MAX_LENGTH characters long." }
            return URL(value)
        }
    }
}

@ConsistentCopyVisibility
public data class CustomAttribute private constructor(override val key: String, val value: String) : SpaydAttribute {
    override fun encodedValue(optimizeForQr: Boolean): String = spaydPercentEncode(value, optimizeForQr)

    public companion object {
        @JvmStatic
        public fun create(key: String, value: String): CustomAttribute {
            require(key.startsWith("X-")) { "Custom attribute key must start with 'X-'." }
            return CustomAttribute(key, value)
        }
    }
}

@Throws(IllegalArgumentException::class)
private fun decode(spayd: String): Spayd {
    // Conveniently, ISO-8859-1 is the first 256 Unicode code points - 0x00..0xFF!
    for ((index, char) in spayd.withIndex()) {
        if (char > '\u00FF') {
            throw IllegalArgumentException("Illegal character at index $index. SPAYD requires ISO-8859-1 charset.")
        }
    }
    val basicRegex = Regex("^SPD\\*[0-9]+\\.[0-9]+\\*.+$")
    require(spayd.matches(basicRegex)) { "Missing required prefix 'SPD*{VERSION}*'" }
    val spayd = preprocessForDecoding(spayd)
    val parts = spayd.split('*').drop(2)

    var acc: Account? = null
    var altAccs: AltAccounts? = null
    var amount: Amount? = null
    var currency: Currency? = null
    var crc32: CRC32? = null
    var dueDate: DueDate? = null
    var message: Message? = null
    var notificationType: NotificationType? = null
    var notificationAddress: NotificationAddress? = null
    var paymentType: PaymentType? = null
    var senderReference: SenderReference? = null
    var recipient: Recipient? = null
    var vs: VS? = null
    var ss: SS? = null
    var ks: KS? = null
    var retryDays: CzRetryDays? = null
    var paymentId: CzPaymentId? = null
    var url: URL? = null

    val customAttrs = mutableListOf<CustomAttribute>()

    for ((index, pair) in parts.withIndex()) {
        val kv = pair.split(':', limit = 2)
        require(kv.size == 2) { "Invalid key-value pair at index $index: missing ':' delimiter." }
        checkKey(index, kv[0])
        val key = kv[0]
        val value = spaydPercentDecode(kv[1])

        // TODO SPAYD spec does not specify how duplicate keys should be handled. Therefore, last wins.
        when (key) {
            "ACC" -> acc = Account.fromString(value)
            "ALT-ACC" -> altAccs = AltAccounts.fromString(value)
            "AM" -> amount = Amount.fromString(value)
            "CC" -> currency = Currency.fromString(value)
            "CRC32" -> crc32 = CRC32.fromString(value)
            "DT" -> dueDate = DueDate.fromString(value)
            "MSG" -> message = Message.fromString(value)
            "NT" -> notificationType = parseNotificationType(value)
            "NTA" -> notificationAddress = NotificationAddress.fromString(value)
            "PT" -> paymentType = PaymentType.fromString(value)
            "RF" -> senderReference = SenderReference.fromString(value)
            "RN" -> recipient = Recipient.fromString(value)
            // Czech extension attrs
            "X-VS" -> vs = VS.fromString(value)
            "X-SS" -> ss = SS.fromString(value)
            "X-KS" -> ks = KS.fromString(value) // Az 10 symbolu pro jednoduchost, realne max 4 cislice
            "X-PER" -> retryDays = CzRetryDays.fromString(value)
            "X-ID" -> paymentId = CzPaymentId.fromString(value)
            "X-URL" -> url = URL.fromString(value)
            // Unknown custom attributes
            else -> customAttrs.add(CustomAttribute.create(key, value))
        }
    }
    require(acc != null) { "Missing required attribute 'ACC'." }
    return Spayd(
        account = acc,
        altAccounts = altAccs,
        amount = amount,
        currency = currency,
        crc32 = crc32,
        dueDate = dueDate,
        message = message,
        notificationType = notificationType,
        notificationAddress = notificationAddress,
        paymentType = paymentType,
        senderReference = senderReference,
        recipient = recipient,
        vs = vs,
        ss = ss,
        ks = ks,
        retryDays = retryDays,
        paymentId = paymentId,
        url = url,
        customAttributes = customAttrs,
    )
}

private fun preprocessForDecoding(spayd: String, lenient: Boolean = false): String {
    var spayd = spayd
    if (lenient) {
        spayd = spayd.trim()
    }

    // Kotlin split produces en empty String if the value ends with the delimiter.
    // SPAYD spec states that all key-value pairs end with a '*'.
    // Therefore, a compliant string will always end with a star.
    // In reality, SPAYD generators often omit the ending delimiter.
    if (spayd.endsWith('*')) {
        spayd = spayd.dropLast(1)
    }

    return spayd
}

private fun parseNotificationType(value: String): NotificationType {
    return when (value) {
        "P" -> NotificationType.PHONE
        "E" -> NotificationType.EMAIL
        else -> throw IllegalArgumentException("NT: Invalid notification type. Must be one of [P, E]")
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun isAsciiDigit(char: Char): Boolean = char in '0'..'9'

@Suppress("NOTHING_TO_INLINE")
private inline fun isAsciiDigits(string: String): Boolean = string.all(::isAsciiDigit)

private val predefinedKeys =
    setOf("ACC", "ALT-ACC", "AM", "CC", "CRC32", "DT", "MSG", "NT", "NTA", "PT", "RF", "RN")

private fun checkKey(index: Int, key: String) {
    val validChars = ('A'..'Z') + '-'
    for ((cindex, c) in key.withIndex()) {
        require(c in validChars) {
            "Key-value at index $index contains illegal character '$c' at index ${cindex}. Allowed key characters: [A-Z-]"
        }
    }

    require(key in predefinedKeys || key.startsWith("X-")) { "Custom keys must start with 'X-'." }
    if (key.contains("--")) {
        // While allowed by the spec, this is weird. 'X--', 'X-ABC--DEF'
        // TODO Emit a warning if configured
    }
}

private fun SpaydAttribute.encodeAsAttr(optimizeForQr: Boolean): String {
    return "${key}:${encodedValue(optimizeForQr)}*"
}

private fun encode(spayd: Spayd, optimizeForQr: Boolean): String = buildString {
    append("SPD*1.0*")
    val standardAttributes = listOfNotNull(
        spayd.account,
        spayd.altAccounts,
        spayd.amount,
        spayd.currency,
        spayd.crc32,
        spayd.dueDate,
        spayd.message,
        spayd.notificationType,
        spayd.notificationAddress,
        spayd.paymentType,
        spayd.senderReference,
        spayd.recipient,
        spayd.vs,
        spayd.ss,
        spayd.ks,
        spayd.retryDays,
        spayd.paymentId,
        spayd.url,
    )

    for (attr in standardAttributes) {
        append(attr.encodeAsAttr(optimizeForQr))
    }

    for (attr in spayd.customAttributes) {
        append(attr.key)
        append(':')
        append(spaydPercentEncode(attr.value, optimizeForQr))
        append('*')
    }
}


private val spaydAllowedChars: Set<Char> =
    ('\u0000'..'\u007F')
        .toSet()
        // SPAYD key-value separator
        .minus('*')
        // Percent-encoding control character
        .minus('%')
        // While allowed by the SPAYD spec, it is expected that other libraries will use URL codec functions.
        // URL codecs might treat '+' as a specially encoded space.
        // Therefore, we will percent-encode '+' to remove ambiguity.
        .minus('+')

// QR alphanumeric: 0–9, A–Z (upper-case only), space, $, %, *, +, -, ., /, :
// Using this character set allows more efficient QR codes generation
private val spaydOptimizedAllowedChars: Set<Char> =
    buildSet {
        addAll('0'..'9')
        addAll('A'..'Z')
        " $-./:".forEach(::add)
    }

internal fun spaydPercentEncode(content: String, optimizeForQr: Boolean): String {
    val content = content.trim().thenIf(optimizeForQr) { it.uppercase() }
    val allowedChars = if (optimizeForQr) spaydOptimizedAllowedChars else spaydAllowedChars
    if (content.all { it in allowedChars }) return content
    val bytes = content.encodeToByteArray()
    return buildString {
        for (byte in bytes) {
            val char = byte.toInt().toChar()
            if (char in allowedChars) {
                append(char)
            } else {
                val code = byte.toInt() and 0xff
                append('%')
                append(hexDigitToChar(code shr 4))
                append(hexDigitToChar(code and 0xf))
            }
        }
    }
}

private inline fun String.thenIf(condition: Boolean, block: (String) -> String): String =
    if (condition) block(this) else this


private fun hexDigitToChar(digit: Int): Char = when (digit) {
    in 0..9 -> '0' + digit
    else -> 'A' + digit - 10
}

internal fun spaydPercentDecode(content: String): String {
    if (!content.contains('%')) return content
    val builder = StringBuilder()
    var pos = 0
    fun getHexaDigit(pos: Int): Int = content[pos].digitToIntOrNull(16)
        ?: throw IllegalArgumentException("Invalid hexadecimal digit: '${content[pos]}'")

    fun readEncodedByte(): Byte {
        require(pos + 2 < content.length) { "Invalid percent-encoded byte: Missing hexadecimal digit after '%'" }
        val hi = getHexaDigit(pos + 1)
        val lo = getHexaDigit(pos + 2)
        val result = (hi shl 4) or lo
        pos += 3
        return result.toByte()
    }

    val utf8Buffer = mutableListOf<Byte>()
    while (pos < content.length) {
        val char = content[pos]
        if (char != '%') {
            if (utf8Buffer.isNotEmpty()) {
                builder.append(utf8Buffer.toByteArray().decodeToString())
                utf8Buffer.clear()
            }
            builder.append(char)
            pos++
            continue
        }
        utf8Buffer.add(readEncodedByte())
    }
    if (utf8Buffer.isNotEmpty()) {
        builder.append(utf8Buffer.toByteArray().decodeToString())
    }
    return builder.toString()
}

internal object Crc32Computer {
    private const val POLYNOMIAL = 0xEDB88320.toInt()

    private val lookupTable: IntArray = IntArray(256) { i ->
        var crc = i
        repeat(8) {
            crc = if (crc and 1 == 1) {
                (crc ushr 1) xor POLYNOMIAL
            } else {
                crc ushr 1
            }
        }
        crc
    }

    fun compute(data: ByteArray): Long {
        var crc = 0xFFFFFFFF.toInt()
        for (byte in data) {
            val index = (crc xor byte.toInt()) and 0xFF
            crc = (crc ushr 8) xor lookupTable[index]
        }
        return (crc xor 0xFFFFFFFF.toInt()).toLong() and 0xFFFFFFFFL
    }

    fun computeHex(data: ByteArray): String = compute(data).toString(16).uppercase().padStart(8, '0')

    fun computeHex(text: String): String =
        compute(text.encodeToByteArray()).toString(16).uppercase().padStart(8, '0')
}
