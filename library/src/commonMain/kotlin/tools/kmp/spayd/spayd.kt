// SPDX-License-Identifier: Apache-2.0
/**
 * @author Ondřej Karmazín
 */
package tools.kmp.spayd

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

public class Spayd private constructor(
    /** ACC: Account, the only required attribute */
    public val account: IbanBic,
    /** ALT-ACC */
    public val altAccounts: Set<IbanBic>?,
    /** AM */
    public val amount: Amount?,
    /** CC */
    public val currency: Currency?,
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

    public val customAttributes: List<CustomAttribute>?,
) {
    public companion object;

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Spayd) return false

        if (account != other.account) return false
        if (altAccounts != other.altAccounts) return false
        if (amount != other.amount) return false
        if (currency != other.currency) return false
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
        result = 31 * result + altAccounts.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + currency.hashCode()
        result = 31 * result + dueDate.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + notificationType.hashCode()
        result = 31 * result + notificationAddress.hashCode()
        result = 31 * result + paymentType.hashCode()
        result = 31 * result + senderReference.hashCode()
        result = 31 * result + recipient.hashCode()
        result = 31 * result + vs.hashCode()
        result = 31 * result + ss.hashCode()
        result = 31 * result + ks.hashCode()
        result = 31 * result + retryDays.hashCode()
        result = 31 * result + paymentId.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + customAttributes.hashCode()
        return result
    }

    public class Builder {
        private var acc: IbanBic? = null
        private var altAccs = mutableSetOf<IbanBic>()
        private var amount: Amount? = null
        private var currency: Currency? = null
        private var dueDate: DueDate? = null
        private var message: Message? = null
        private var notificationType: NotificationType? = null
        private var notificationAddress: NotificationAddress? = null
        private var paymentType: PaymentType? = null
        private var senderReference: SenderReference? = null
        private var recipient: Recipient? = null
        private var vs: VS? = null
        private var ss: SS? = null
        private var ks: KS? = null
        private var retryDays: CzRetryDays? = null
        private var paymentId: CzPaymentId? = null
        private var url: URL? = null
        private val customAttrs = mutableListOf<CustomAttribute>()

        public fun account(account: IbanBic): Builder = apply { acc = account }
        public fun altAccount(altAccount: IbanBic): Builder = apply { altAccs.add(altAccount) }
        public fun amount(amount: Amount): Builder = apply { this.amount = amount }
        public fun currency(currency: Currency): Builder = apply { this.currency = currency }
        public fun dueDate(dueDate: DueDate): Builder = apply { this.dueDate = dueDate }
        public fun message(message: Message): Builder = apply { this.message = message }
        public fun notification(notificationType: NotificationType, notificationAddress: NotificationAddress): Builder =
            apply {
                this.notificationType = notificationType
                this.notificationAddress = notificationAddress
            }

        public fun paymentType(paymentType: PaymentType): Builder = apply { this.paymentType = paymentType }
        public fun senderReference(senderReference: SenderReference): Builder =
            apply { this.senderReference = senderReference }

        public fun recipient(recipient: Recipient): Builder = apply { this.recipient = recipient }
        public fun vs(vs: VS): Builder = apply { this.vs = vs }
        public fun ss(ss: SS): Builder = apply { this.ss = ss }
        public fun ks(ks: KS): Builder = apply { this.ks = ks }
        public fun retryDays(retryDays: CzRetryDays): Builder = apply { this.retryDays = retryDays }
        public fun paymentId(paymentId: CzPaymentId): Builder = apply { this.paymentId = paymentId }
        public fun url(url: URL): Builder = apply { this.url = url }
        public fun customAttribute(customAttribute: CustomAttribute): Builder =
            apply { customAttrs.add(customAttribute) }

        @Throws(SpaydException::class)
        public fun build(): Spayd {
            val acc = acc ?: throw SpaydException("ACC: Account attribute is required.")

            return Spayd(
                acc,
                altAccs.takeIf { it.isNotEmpty() }?.toSet(),
                amount, currency, dueDate, message, notificationType, notificationAddress, paymentType,
                senderReference, recipient, vs, ss, ks, retryDays, paymentId, url,
                customAttrs.takeIf { it.isNotEmpty() }
            )
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    public class Encoder private constructor(
        public val optimizeForQr: Boolean,
        public val logger: Logger?,
    ) {

        @Throws(SpaydException::class)
        public fun encode(spayd: Spayd): String = buildString {
            append("SPD*1.0*")
            val parts = mutableListOf<Pair<String, String>>()
            parts.add("ACC" to spayd.account.encode())
            if (!spayd.altAccounts.isNullOrEmpty()) {
                parts.add("ALT-ACC" to spayd.altAccounts.joinToString(",") { it.encode() })
            }
            if (spayd.amount != null) parts.add("AM" to spayd.amount.encode())
            if (spayd.currency != null) parts.add("CC" to spayd.currency.encode())
            if (spayd.dueDate != null) parts.add("DT" to spayd.dueDate.encode())
            if (spayd.message != null) parts.add("MSG" to spayd.message.encode(optimizeForQr))
            if (spayd.notificationType != null) parts.add("NT" to spayd.notificationType.encode())
            if (spayd.notificationAddress != null) {
                parts.add("NTA" to spayd.notificationAddress.encode(optimizeForQr))
            }
            if (spayd.paymentType != null) parts.add("PT" to spayd.paymentType.encode())
            if (spayd.senderReference != null) parts.add("RF" to spayd.senderReference.encode(optimizeForQr))
            if (spayd.recipient != null) parts.add("RN" to spayd.recipient.encode(optimizeForQr))
            if (spayd.vs != null) parts.add("X-VS" to spayd.vs.encode())
            if (spayd.ss != null) parts.add("X-SS" to spayd.ss.encode())
            if (spayd.ks != null) parts.add("X-KS" to spayd.ks.encode())
            if (spayd.retryDays != null) parts.add("X-PER" to spayd.retryDays.encode())
            if (spayd.paymentId != null) parts.add("X-ID" to spayd.paymentId.encode(optimizeForQr))
            if (spayd.url != null) parts.add("X-URL" to spayd.url.encode(optimizeForQr))

            for (attr in spayd.customAttributes.orEmpty()) {
                parts.add(attr.key to attr.encode(optimizeForQr))
            }
            parts.sortWith(compareBy({ it.first }, { it.second }))
            logger?.debug("Sorted attributes: $parts")
            append(parts.joinToString("") { "${it.first}:${it.second}*" })
        }

        private inline fun Amount.encode(): String = value
        private inline fun Currency.encode(): String = code.uppercase()
        private inline fun Message.encode(optimizeForQr: Boolean): String = spaydPercentEncode(value, optimizeForQr)
        private inline fun Recipient.encode(optimizeForQr: Boolean): String = spaydPercentEncode(value, optimizeForQr)
        private inline fun VS.encode(): String = value
        private inline fun SS.encode(): String = value
        private inline fun KS.encode(): String = value
        private inline fun CzRetryDays.encode(): String = value.toString()
        private inline fun CzPaymentId.encode(optimizeForQr: Boolean): String = spaydPercentEncode(value, optimizeForQr)
        private inline fun URL.encode(optimizeForQr: Boolean): String = spaydPercentEncode(value, optimizeForQr)
        private inline fun IbanBic.encode(): String {
            val content = iban.value + (bic?.value?.let { "+$it" } ?: "")
            return spaydPercentEncode(content, true)
        }

        private inline fun DueDate.encode(): String =
            "$year${monthNumber.toString().padStart(2, '0')}${dayOfMonth.toString().padStart(2, '0')}"

        private inline fun NotificationType.encode(): String = when (this) {
            NotificationType.PHONE -> "P"
            NotificationType.EMAIL -> "E"
        }

        private inline fun NotificationAddress.encode(optimizeForQr: Boolean): String =
            spaydPercentEncode(value, optimizeForQr)

        private inline fun PaymentType.encode(): String = when (this) {
            is PaymentType.Custom -> value.uppercase()
            PaymentType.InstantPayment -> "IP"
        }

        private inline fun SenderReference.encode(optimizeForQr: Boolean): String =
            spaydPercentEncode(value, optimizeForQr)

        private inline fun CustomAttribute.encode(optimizeForQr: Boolean): String =
            spaydPercentEncode(value, optimizeForQr)

        public class Builder {
            private var logger: Logger? = null
            private var optimizeForQr: Boolean = false

            public fun logger(logger: Logger?): Builder = apply { this.logger = logger }
            public fun optimizeForQr(optimizeForQr: Boolean): Builder = apply { this.optimizeForQr = optimizeForQr }

            public fun build(): Encoder = Encoder(optimizeForQr, logger)
        }
    }

    public class Decoder private constructor(
        public val logger: Logger? = null,
    ) {

        @Throws(SpaydException::class)
        public fun decode(spayd: String): Spayd = decodeSpayd(spayd, logger)

        public class Builder {
            private var logger: Logger? = null

            public fun logger(logger: Logger?): Builder = apply { this.logger = logger }

            public fun build(): Decoder = Decoder()
        }
    }
}

public interface Logger {
    public fun debug(message: String, throwable: Throwable? = null)
    public fun info(message: String, throwable: Throwable? = null)
    public fun warn(message: String, throwable: Throwable? = null)
    public fun error(message: String, throwable: Throwable? = null)

    public companion object {
        public val PRINTLN: Logger = object : Logger {
            private fun log(prefix: String, message: String, throwable: Throwable?) {
                val throwableMessage = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
                println("$prefix $message$throwableMessage")
            }

            override fun debug(message: String, throwable: Throwable?): Unit = log("[DEBUG]", message, throwable)
            override fun info(message: String, throwable: Throwable?): Unit = log("[INFO]", message, throwable)
            override fun warn(message: String, throwable: Throwable?): Unit = log("[WARN]", message, throwable)
            override fun error(message: String, throwable: Throwable?): Unit = log("[ERROR]", message, throwable)
        }
    }
}

/**
 * Base exception thrown from the library. If a thrown exception is not an instance of [SpaydException],
 * it is likely a bug and should be reported.
 */
public class SpaydException(message: String?, cause: Throwable? = null) : IllegalArgumentException(message, cause)

private inline fun req(value: Boolean, lazyMessage: () -> Any) = try {
    require(value, lazyMessage)
} catch (e: IllegalArgumentException) {
    throw SpaydException(e.message, e)
}

public data class IbanBic(val iban: IBAN, val bic: BIC? = null) {
    internal companion object {
        @JvmStatic
        @Throws(SpaydException::class)
        fun fromString(value: String): IbanBic {
            val parts = value.split('+', limit = 2)
            val iban = parts[0]
            val bic = parts.getOrNull(1)
            return IbanBic(IBAN.fromString(iban), bic?.let(BIC::fromString))
        }
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
        @Throws(SpaydException::class)
        public fun fromString(string: String): IBAN {
            val string = string.filterNot { it.isWhitespace() }
            req(string.length in MIN_LENGTH..MAX_LENGTH) {
                "IBAN length (${string.length}) is not in the allowed range $MIN_LENGTH..$MAX_LENGTH."
            }
            req(string.take(2).all { it in 'A'..'Z' }) { "Invalid country code." }
            req(string.drop(2).take(2).all { it in '0'..'9' }) { "Invalid check digits." }

            val rearrangedDigits = buildString {
                for (c in string.drop(4) + string.take(4)) {
                    if (c.isDigit()) {
                        append(c.toString())
                    } else {
                        append((c.code - 'A'.code + 10).toString())
                    }
                }
            }

            req(mod97(rearrangedDigits) == 1) { "Invalid IBAN: did not pass mod97 check." }

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
        @Throws(SpaydException::class)
        public fun fromString(string: String): BIC {
            // From Wikipedia:
            // 4 letters, 2 letters, 2 alphanum, 3 alphanum (optional)
            req(string.length == 8 || string.length == 11) {
                "BIC must be 8 OR 11 characters long, was ${string.length}."
            }
            val letters = string.take(6)
            val alphanum = string.drop(6)
            req(letters.all { it in 'A'..'Z' }) { "BIC must start with 6 uppercase letters." }
            req(alphanum.all { it in 'A'..'Z' || it in '0'..'9' }) { "BIC must end with 2 or 5 alphanumeric characters." }
            return BIC(string)
        }
    }
}

@ConsistentCopyVisibility
public data class CZBankAccount private constructor(
    val accountNumber: CZBankAccountNumber,
    val bankCode: BankCode
) {
    public override fun toString(): String = "$accountNumber/$bankCode"

    internal companion object {

        @JvmStatic
        @Throws(SpaydException::class)
        fun fromString(string: String): CZBankAccount {
            val string = string.trim()
            val splits = string.split('/')
            req(splits.size == 2) {
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
        @Throws(SpaydException::class)
        public fun fromString(string: String): CZBankAccountNumber {
            val string = string.trim()
            req(string.matches(regex)) { "Invalid CZ bank account number format." }
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
                    throw SpaydException("CZ bank number regex matched, but split into more than 2 parts (${parts.size}).")
                }
            }
        }

        private fun validatePart(part: String, maxLength: Int, minNotZeroDigits: Int) {
            req(part.length <= maxLength) { "Invalid CZ bank account number part length (${part.length}). Must be <= $maxLength." }
            var notZeroCount = 0
            val sum = part.padStart(maxLength, '0').reversed().foldIndexed(0) { index, acc, c ->
                if (c != '0') notZeroCount++
                acc + c.digitToInt() * weights[index]
            }
            req(notZeroCount >= minNotZeroDigits) { "Invalid CZ bank account number - must have at least $minNotZeroDigits non-zero digits." }
            req(sum % 11 == 0) { "Invalid CZ bank account number - check digit is invalid." }
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
        @Throws(SpaydException::class)
        public fun fromString(string: String): BankCode {
            req(string.length == 4 && string.all { it in '0'..'9' }) {
                "Invalid bank code. Must be exactly 4 digits. Input has either bad length, or contains non-digits."
            }
            return BankCode(string)
        }
    }
}

@JvmInline
public value class Amount private constructor(public val value: String) {
    public companion object {
        // @formatter:off
        public const val ERR_NAN: String = "AM: Amount must not be empty and must only contain digits or up to 1 decimal point. Negative amounts are not allowed."
        public const val ERR_SINGLE_DECIMAL_POINT: String = "AM: Input must not be a single decimal point '.': While this could be interpreted as a zero decimal amount, it is ambiguous and no reasonable string representation of a decimal number would output this. Leading decimal point is only allowed if it is followed by at least 1 digit."
        public const val ERR_TOO_MANY_DECIMALS: String = "AM: Amount must have at most 2 decimal places."
        public const val ERR_TOO_LONG: String = "AM: Amount must be 1 to 10 characters after normalization. SPAYD requires 1..10 characters including the decimal point and decimal expansion i.e. <10>, <8>.<1>, <7>.<2>"
        // @formatter:on

        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): Amount {
            val isNumber = value.isNotEmpty() && value.all { isAsciiDigit(it) || it == '.' }
            req(isNumber) { ERR_NAN }
            req(value != ".") { ERR_SINGLE_DECIMAL_POINT }
            val parts = value.split('.')
            req(parts.size <= 2) { ERR_NAN }
            val integerPart = parts[0].dropWhile { it == '0' }.ifEmpty { "0" }
            val decimalPart = parts.getOrNull(1).orEmpty().dropLastWhile { it == '0' }

            req(decimalPart.length <= 2) { ERR_TOO_MANY_DECIMALS }
            val decimalSuffix = if (decimalPart.isEmpty()) "" else ".$decimalPart"
            val normalized = "$integerPart$decimalSuffix"
            req(normalized.length in 1..10) { ERR_TOO_LONG }
            return Amount(integerPart + decimalSuffix)
        }
    }
}

@JvmInline
public value class Currency private constructor(public val code: String) {
    public companion object {
        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): Currency {
            req(value.length == 3 && value.uppercase().all { it in 'A'..'Z' }) {
                "CC: Currency code must be exactly 3 letters."
            }
            return Currency(value)
        }
    }
}

@ConsistentCopyVisibility
public data class DueDate private constructor(val year: Int, val monthNumber: Int, val dayOfMonth: Int) {
    /**
     * ISO 8601 format: YYYY-MM-DD
     */
    override fun toString(): String =
        "$year-${monthNumber.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}"

    public companion object {

        @JvmStatic
        @Throws(SpaydException::class)
        public fun of(year: Int, monthNumber: Int, dayOfMonth: Int): DueDate = create(year, monthNumber, dayOfMonth)

        @JvmStatic
        @Throws(SpaydException::class)
        internal fun fromString(value: String): DueDate {
            req(value.length == 8 && value.all { it in '0'..'9' }) { "Date must be exactly 8 digits (YYYYMMDD)." }
            val year = value.take(4).toInt()
            val month = value.drop(4).take(2).toInt()
            val day = value.takeLast(2).toInt()
            return create(year, month, day)
        }

        private fun create(year: Int, month: Int, day: Int): DueDate {
            req(year >= 1900) { "Unreasonable year: $year." }
            req(month in 1..12) { "Month number must be between 1 and 12." }
            req(day in 1..daysInMonth(year, month)) { "Invalid day of month: $day" }
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
            else -> throw SpaydException("Invalid month number: $monthNumber")
        }

        private fun isLeapYear(year: Int): Boolean = (year % 400 == 0) || (year % 4 == 0 && year % 100 != 0)
    }
}

@JvmInline
public value class Message private constructor(public val value: String) {
    public companion object {
        public const val MAX_LENGTH: Int = 60

        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): Message {
            req(value.length <= MAX_LENGTH) { "MSG: Message must not exceed $MAX_LENGTH characters." }
            return Message(value)
        }
    }
}

public enum class NotificationType {
    /** NT:P */
    PHONE,

    /** NT:E */
    EMAIL;

    public companion object {
        @JvmStatic
        @Throws(SpaydException::class)
        internal fun fromString(value: String): NotificationType = when (value) {
            "P" -> PHONE
            "E" -> EMAIL
            else -> throw SpaydException("NT: Invalid notification type. Must be one of [P, E]")
        }
    }
}

@JvmInline
public value class NotificationAddress private constructor(public val value: String) {
    public companion object {
        public const val MAX_LENGTH: Int = 320

        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): NotificationAddress {
            req(value.length <= MAX_LENGTH) { "NTA: Notification address must be at most $MAX_LENGTH characters long." }
            return NotificationAddress(value)
        }
    }
}

public sealed class PaymentType {
    /**
     * PT:IP
     *
     * Instant payment, if possible.
     */
    public data object InstantPayment : PaymentType()

    public data class Custom(val value: String) : PaymentType()

    public companion object {
        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): PaymentType {
            req(value.length in 1..3) { "PT: Payment type must be 1 to 3 characters long." }
            return when (value) {
                "IP" -> InstantPayment
                else -> Custom(value)
            }
        }
    }
}

@JvmInline
public value class SenderReference private constructor(public val value: String) {
    public companion object {
        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): SenderReference {
            req(value.length in 1..16 && isAsciiDigits(value)) {
                "RF: Sender reference must be a string of digits from 1 to 16 characters long."
            }
            return SenderReference(value)
        }
    }
}

@JvmInline
public value class Recipient private constructor(public val value: String) {
    public companion object {
        public const val MAX_LENGTH: Int = 35

        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): Recipient {
            req(value.length <= MAX_LENGTH) { "RN: Recipient must be at most $MAX_LENGTH characters long." }
            return Recipient(value)
        }
    }
}

@JvmInline
public value class VS private constructor(public val value: String) {
    public companion object {
        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): VS {
            req(value.length in 1..10 && isAsciiDigits(value)) { "X-VS: VS must be 1 to 10 digits." }
            return VS(value)
        }
    }
}

@JvmInline
public value class SS private constructor(public val value: String) {
    public companion object {
        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): SS {
            req(value.length in 1..10 && isAsciiDigits(value)) { "X-SS: SS must be 1 to 10 digits." }
            return SS(value)
        }
    }
}

@JvmInline
public value class KS private constructor(public val value: String) {
    public companion object {
        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): KS {
            req(value.length in 1..10 && isAsciiDigits(value)) { "X-KS: KS must be 1 to 10 digits." }
            return KS(value)
        }
    }
}

@JvmInline
public value class CzRetryDays private constructor(public val value: Int) {
    public companion object {
        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): CzRetryDays {
            req(value.length in 1..2 && isAsciiDigits(value) && value.toInt() in 1..30) {
                "X-PER: Retry days must be a number from 1 to 30"
            }
            return CzRetryDays(value.toInt())
        }
    }
}

@JvmInline
public value class CzPaymentId private constructor(public val value: String) {
    public companion object {
        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): CzPaymentId {
            req(value.length in 1..20) { "X-ID must be 1 to 20 characters long." }
            return CzPaymentId(value)
        }
    }
}

@JvmInline
public value class URL private constructor(public val value: String) {
    public companion object {
        public const val MAX_LENGTH: Int = 140

        @JvmStatic
        @Throws(SpaydException::class)
        public fun fromString(value: String): URL {
            req(value.length <= MAX_LENGTH) { "X-URL: URL must be at most $MAX_LENGTH characters long." }
            return URL(value)
        }
    }
}

@ConsistentCopyVisibility
public data class CustomAttribute private constructor(val key: String, val value: String) {
    public companion object {
        public val reservedKeys: Set<String> = setOf("X-VS", "X-SS", "X-KS", "X-PER", "X-ID", "X-URL")

        @JvmStatic
        @Throws(SpaydException::class)
        public fun create(key: String, value: String): CustomAttribute {
            req(key !in reservedKeys) { "Custom attribute key '$key' is reserved. Please use a different key for your own attribute." }
            req(key.startsWith("X-")) { "Custom attribute key must start with 'X-'." }
            return CustomAttribute(key, value)
        }
    }
}

private data class ParsedSpaydEntry(val index: Int, val key: String, val percentDecodedValue: String) {
    companion object {
        fun fromIndexedValue(value: IndexedValue<String>, logger: Logger?): ParsedSpaydEntry {
            // specialized message for empty values, it is more informative than "missing delimiter"
            req(value.value.isNotEmpty()) { "Key-value pair at index ${value.index} is empty (**)" }
            val parts = value.value.split(':', limit = 2)
            req(parts.size == 2) { "Invalid key-value pair at index ${value.index}: missing ':' delimiter." }
            checkKey(value.index, parts[0], logger)
            val decoded = try {
                spaydPercentDecode(parts[1])
            } catch (e: SpaydException) {
                throw SpaydException("Invalid key-value pair at index ${value.index}: ${e.message}", e)
            }
            return ParsedSpaydEntry(index = value.index, key = parts[0], percentDecodedValue = decoded)
        }
    }
}

@Throws(SpaydException::class)
private fun decodeSpayd(spayd: String, logger: Logger?): Spayd {
    // Conveniently, ISO-8859-1 is the first 256 Unicode code points - 0x00..0xFF!
    for ((index, char) in spayd.withIndex()) {
        if (char > '\u00FF') {
            throw SpaydException("Illegal character at index $index. SPAYD requires ISO-8859-1 charset.")
        }
    }
    val basicRegex = Regex("^SPD\\*[0-9]+\\.[0-9]+\\*.+$")
    req(spayd.matches(basicRegex)) { "Missing required prefix 'SPD*{VERSION}*'" }
    val spayd = preprocessForDecoding(spayd)
    val spaydEntries = spayd.split('*').drop(2).withIndex().map { ParsedSpaydEntry.fromIndexedValue(it, logger) }
    val duplicates = spaydEntries.groupBy { it.key }.filter { it.value.size > 1 }
    req(duplicates.isEmpty()) {
        val report = duplicates.entries.joinToString(
            separator = "; ",
            prefix = "[",
            postfix = "]"
        ) { (key, values) -> "$key: at indexes ${values.joinToString { it.index.toString() }}" }
        "Duplicate keys found: $report"
    }

    var receivedNt: String? = null
    var receivedNta: String? = null
    var receivedCrc32: String? = null
    var receivedAltAccounts: List<IbanBic>? = null
    val result = Spayd.Builder()

    for (entry in spaydEntries) {
        val value = entry.percentDecodedValue
        when (entry.key) {
            "ACC" -> result.account(IbanBic.fromString(value))
            "ALT-ACC" -> {
                try {
                    receivedAltAccounts = value.split(',').map(IbanBic::fromString)
                } catch (e: SpaydException) {
                    throw SpaydException("Cannot parse ALT-ACC: ${e.message}", e)
                }
            }

            "AM" -> result.amount(Amount.fromString(value))
            "CC" -> result.currency(Currency.fromString(value))
            "CRC32" -> receivedCrc32 = value
            "DT" -> result.dueDate(DueDate.fromString(value))
            "MSG" -> result.message(Message.fromString(value))
            "NT" -> receivedNt = value
            "NTA" -> receivedNta = value
            "PT" -> result.paymentType(PaymentType.fromString(value))
            "RF" -> result.senderReference(SenderReference.fromString(value))
            "RN" -> result.recipient(Recipient.fromString(value))
            // Czech extension attrs
            "X-VS" -> result.vs(VS.fromString(value))
            "X-SS" -> result.ss(SS.fromString(value))
            "X-KS" -> result.ks(KS.fromString(value)) // Az 10 symbolu pro jednoduchost, realne max 4 cislice
            "X-PER" -> result.retryDays(CzRetryDays.fromString(value))
            "X-ID" -> result.paymentId(CzPaymentId.fromString(value))
            "X-URL" -> result.url(URL.fromString(value))
            // Unknown custom attributes
            else -> result.customAttribute(CustomAttribute.create(entry.key, value))
        }
    }
    receivedAltAccounts?.forEach(result::altAccount)
    when {
        receivedNt != null && receivedNta != null -> {
            result.notification(NotificationType.fromString(receivedNt), NotificationAddress.fromString(receivedNta))
        }

        receivedNt != null || receivedNta != null -> {
            throw SpaydException("NT/NTA: Both NT and NTA attributes must be specified, or neither. Having just one makes no real sense")
        }
    }
    // TODO check CRC32
    return result.build()
}

private fun preprocessForDecoding(spayd: String): String {
    var spayd = spayd

    // Kotlin split produces an empty String if the value ends with the delimiter.
    // SPAYD spec states that all key-value pairs end with a '*'.
    // Therefore, a compliant string will always end with a star.
    // In reality, SPAYD generators often omit the ending delimiter.
    if (spayd.endsWith('*')) {
        spayd = spayd.dropLast(1)
    }

    return spayd
}

@Suppress("NOTHING_TO_INLINE")
private inline fun isAsciiDigit(char: Char): Boolean = char in '0'..'9'

@Suppress("NOTHING_TO_INLINE")
private inline fun isAsciiDigits(string: String): Boolean = string.all(::isAsciiDigit)

private val predefinedKeys =
    setOf("ACC", "ALT-ACC", "AM", "CC", "CRC32", "DT", "MSG", "NT", "NTA", "PT", "RF", "RN")

private fun checkKey(index: Int, key: String, logger: Logger?) {
    val validChars = ('A'..'Z') + '-'
    for ((cindex, c) in key.withIndex()) {
        req(c in validChars) {
            "Key-value at index $index contains illegal character '$c' at index ${cindex}. Allowed key characters: [A-Z-]"
        }
    }

    req(key in predefinedKeys || key.startsWith("X-")) { "Custom keys must start with 'X-'." }
    if (key.contains("--")) {
        logger?.warn("User-defined key '$key' contains '--'. This is allowed but suspicious.")
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
        ?: throw SpaydException("Invalid hexadecimal digit: '${content[pos]}'")

    fun readEncodedByte(): Byte {
        req(pos + 2 < content.length) { "Invalid percent-encoded byte: Missing hexadecimal digit after '%'" }
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
