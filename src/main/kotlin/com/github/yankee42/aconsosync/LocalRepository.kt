package com.github.yankee42.aconsosync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.writeBytes

class LocalRepository(private val dir: Path) {
    suspend fun addDocumentIfNotExists(
        document: Document,
        loadDocument: suspend () -> ByteArray,
        eventListener: (event: DocumentSyncEvent) -> Unit
    ) {
        val fileName = dir.resolve(
            "${document.fileName.replaceNonFilenameChars()}_${document.fileId}.pdf"
        )
        if (fileName.exists()) {
            eventListener(DocumentAlreadyExistsEvent(fileName, document))
        } else {
            eventListener(DocumentStartSyncEvent(fileName, document))
            val documentBinary = loadDocument()
            withContext(Dispatchers.IO) {
                @Suppress("BlockingMethodInNonBlockingContext")
                dir.createDirectories()
                @Suppress("BlockingMethodInNonBlockingContext")
                fileName.writeBytes(documentBinary)
            }
            eventListener(DocumentSyncCompleteEvent(fileName, document))
        }
    }
}

private fun String.replaceNonFilenameChars(replacement: String = "-") =
    replace("[<>:;,?\"*|/\\\\]".toRegex(), replacement)

sealed class DocumentSyncEvent(val fileName: Path, val document: Document) : AconsoEvent()
class DocumentAlreadyExistsEvent(fileName: Path, document: Document) : DocumentSyncEvent(fileName, document)
class DocumentStartSyncEvent(fileName: Path, document: Document) : DocumentSyncEvent(fileName, document)
class DocumentSyncCompleteEvent(fileName: Path, document: Document) : DocumentSyncEvent(fileName, document)
