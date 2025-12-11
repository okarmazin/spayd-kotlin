# spayd-kotlin

Zero-dependency Kotlin Multiplatform library for reading and writing SPAYD (Short Payment Descriptor) strings - the Czech
pay-by-qr standard for bank transfers, a.k.a. [QR Platba](https://qr-platba.cz/)

## Installation

The library is published to Maven Central.

```
implementation("tools.kmp:spayd:0.0.2") // check the latest version in Releases --->
```

Alternatively, you can simply copy the file [`spayd.kt`](library/src/commonMain/kotlin/tools/kmp/spayd/spayd.kt) into
your project!
<p>The library is purposefully written as a single file to enable convenient copying.
Since this library has no external dependencies, you can freely copy the source into your project and it will just work.</p>

## Supported platforms
Supports all Kotlin targets, all of them should be enabled. If there's a missing platform, please open an issue 
and it will get added.

## Serialization

```kotlin
object SpaydSerializer : KSerializer<Spayd> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("tools.kmp.spayd.Spayd", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Spayd) = encoder.encodeString(value.encodeToString(true))

    override fun deserialize(decoder: Decoder): Spayd = Spayd.decodeFromString(decoder.decodeString())
}
```