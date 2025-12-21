# spayd-kotlin

Zero-dependency Kotlin Multiplatform library for reading and writing SPAYD (Short Payment Descriptor) strings - the
Czech pay-by-qr standard for bank transfers, a.k.a. [QR Platba](https://qr-platba.cz/)

## Installation

The library is published to Maven Central.

```
implementation("tools.kmp:spayd:0.0.3") // check the latest version in Releases --->
```

_Alternatively, you can simply copy the file [`spayd.kt`](library/src/commonMain/kotlin/tools/kmp/spayd/spayd.kt) into
your project!_

## Supported platforms

Supports all Kotlin targets, all of them should be enabled. If there's a missing platform, please open an issue
and it will get added.

## Usage

### API Design

The API is designed for consistency and discoverability. You are supposed to be able to search the API surface using
your IDE's autocompletion. As a rule of thumb, objects are constructed using factory functions, or with a `Builder` pattern.

Class `Spayd` is the entry point into the library. The rest of the API is discoverable with `Spayd.<completion>`

### Working with the library

```kotlin
val constructedSpayd: Spayd =
    Spayd.Builder()
        .account(IbanBic.fromString("CZ0608000000192235210247"))
        .altAccount(IbanBic.fromString("CZ9003000000192235210247"))
        .altAccount(IbanBic.fromString("CZ4601000000192235210247"))
        .amount(Amount.fromString("599.00"))
        .currency(Currency.fromString("CZK"))
        .message(Message.fromString("T-Mobile - QR platba"))
        .recipient(Recipient.fromString("T-Mobile Czech Republic a.s."))
        .vs(VS.fromString("1113334445"))
        .ss(SS.fromString("11"))
        .customAttribute(CustomAttribute.create("X-CUSTOM", "orang utan"))
        .build()

// String received from an external source
val receivedSpayd =
    "SPD*1.0*ACC:CZ0608000000192235210247*AM:599*ALT-ACC:CZ9003000000192235210247,CZ4601000000192235210247*CC:CZK*MSG:T-Mobile - QR platba*RN:T-Mobile Czech Republic a.s.*X-CUSTOM:orang utan*X-VS:1113334445*X-SS:11*"
val decoder = Spayd.Decoder.Builder()
    .logger(Logger.PRINTLN)
    .build()
val decodedSpayd = decoder.decode(receivedSpayd)
assertEquals(constructedSpayd, decodedSpayd)

val encoder = Spayd.Encoder.Builder()
    .includeCrc32(true)
    .logger(Logger.PRINTLN)
    .build()
val expectedEncoded =
    "SPD*1.0*ACC:CZ0608000000192235210247*ALT-ACC:CZ9003000000192235210247,CZ4601000000192235210247*AM:599*CC:CZK*MSG:T-Mobile - QR platba*RN:T-Mobile Czech Republic a.s.*X-CUSTOM:orang utan*X-SS:11*X-VS:1113334445*CRC32:B4F793B6*"
val actualEncoded = encoder.encode(constructedSpayd)
assertEquals(expectedEncoded, actualEncoded)

assertEquals(decodedSpayd, decoder.decode(actualEncoded))
assertEquals(constructedSpayd, decoder.decode(actualEncoded))
```

### Error handling
The library throws `SpaydException : IllegalArgumentException` from all decoding and encoding operations, and from
factory functions that fail to construct a valid object (e.g. `Builder.build()` with illegal configuration, 
or `Amount.fromString("Not a number")`).

If you catch an exception that isn't a `SpaydException`, it's a bug. Please open an issue.

### Serialization

`Spayd` is not `@Serializable` to avoid the `kotlinx.serialization` dependency, but the following should do:

```kotlin
object SpaydSerializer : KSerializer<Spayd> {
    // Configure the decoder and encoder to your needs
    private val spaydDecoder = Spayd.Decoder.Builder().build()
    private val spaydEncoder = Spayd.Encoder.Builder().build()

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("tools.kmp.spayd.Spayd", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Spayd) = encoder.encodeString(spaydEncoder.encode(value))

    override fun deserialize(decoder: Decoder): Spayd = spaydDecoder.decode(decoder.decodeString())
}
```