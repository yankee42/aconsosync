package com.github.yankee42.aconsosync.cli

import com.github.yankee42.aconsosync.Document
import com.github.yankee42.aconsosync.DocumentAlreadyExistsEvent
import com.github.yankee42.aconsosync.DocumentSyncCompleteEvent
import com.github.yankee42.aconsosync.aconsoSync
import io.ktor.http.Url
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.io.path.createDirectories

@Command(name = "headless", showAtFileInUsageHelp = true)
class Cli : Callable<Int> {
    @Option(
        names = ["--local-dir"],
        description = ["downloaded files will be placed in this directory"],
        required = true
    )
    lateinit var localDir: Path

    @Option(
        names = ["--url"],
        description = ["URL of the aconso document box.", "E.g. https://YOUR_COMPANY.hr-document-box.com/"],
        required = true
    )
    lateinit var url: String

    @Option(names = ["--username"], required = true)
    lateinit var username: String

    @Option(
        names = ["--password"],
        description = ["will prompt if no value given"],
        interactive = true,
        arity = "0..1",
        required = true
    )
    lateinit var password: String

    override fun call(): Int {
        localDir.createDirectories()
        val documentsSynced: MutableList<Document> = mutableListOf()
        val documentsUnchanged: MutableList<Document> = mutableListOf()
        aconsoSync(Url(url), localDir, username, password) { event ->
            if (event is DocumentAlreadyExistsEvent) {
                documentsUnchanged += event.document
            } else if (event is DocumentSyncCompleteEvent) {
                documentsSynced += event.document
            }
        }
        println("The following documents were downloaded:")
        documentsSynced.print()
        println("The following documents already exist:")
        documentsUnchanged.print()
        return 0
    }
}

private fun List<Document>.print() {
    if (isEmpty()) {
        println("  -- NONE --")
    } else {
        forEach { println("  ${it.fileName}") }
    }
}
