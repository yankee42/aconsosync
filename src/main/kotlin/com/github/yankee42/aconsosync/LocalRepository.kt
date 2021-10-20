package com.github.yankee42.aconsosync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

class LocalRepository(private val dir: Path, private val fileNameTemplate: PathTemplate) {
    suspend fun addDocumentIfNotExists(
        document: Document,
        loadDocument: suspend () -> ByteArray,
        eventListener: (event: DocumentSyncEvent) -> Unit
    ) {
        val fileName = dir.resolve(fileNameTemplate.getPath(document))
        if (fileName.exists()) {
            eventListener(DocumentAlreadyExistsEvent(fileName, document))
        } else {
            eventListener(DocumentStartSyncEvent(fileName, document))
            val documentBinary = loadDocument()
            withContext(Dispatchers.IO) {
                @Suppress("BlockingMethodInNonBlockingContext")
                fileName.parent.createDirectories()
                @Suppress("BlockingMethodInNonBlockingContext")
                fileName.writeBytes(documentBinary)
            }
            eventListener(DocumentSyncCompleteEvent(fileName, document))
        }
    }
}

sealed class DocumentSyncEvent(val fileName: Path, val document: Document) : AconsoEvent()
class DocumentAlreadyExistsEvent(fileName: Path, document: Document) : DocumentSyncEvent(fileName, document)
class DocumentStartSyncEvent(fileName: Path, document: Document) : DocumentSyncEvent(fileName, document)
class DocumentSyncCompleteEvent(fileName: Path, document: Document) : DocumentSyncEvent(fileName, document)
