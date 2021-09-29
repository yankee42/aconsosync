package com.github.yankee42.aconsosync

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.temporal.TemporalField

@Serializable
class BasicResponse(val success: Boolean)

@Serializable
class DocTree(val documents: List<Document>)

@Serializable
class Document(
    @SerialName("FILE_INDEX") val fileId: String,
    @SerialName("ATT_NAME") val fileName: String,
    @Serializable(with = LocalDateSerializer::class) @SerialName("ATT_DOC_DATE") val documentDate: LocalDate
)

private val LOCAL_DATE_FORMAT = DateTimeFormatterBuilder()
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendLiteral('.')
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendLiteral('.')
    .appendValue(ChronoField.YEAR, 4)
    .toFormatter()

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString(), LOCAL_DATE_FORMAT)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.format(LOCAL_DATE_FORMAT))
    }
}
