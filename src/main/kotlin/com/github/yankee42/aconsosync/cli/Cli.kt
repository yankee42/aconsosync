package com.github.yankee42.aconsosync.cli

import com.github.yankee42.aconsosync.Document
import com.github.yankee42.aconsosync.DocumentAlreadyExistsEvent
import com.github.yankee42.aconsosync.DocumentSyncCompleteEvent
import com.github.yankee42.aconsosync.PathTemplate
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

    @Option(
        names = ["--file-name-pattern"],
        defaultValue = "{name}_{id}.pdf",
        description = [
            "pattern to use to generate file names for the downloaded documents",
            "Default if not supplied: \${DEFAULT-VALUE}",
            "The following variables can be used:",
            "{name} - The name of the document",
            "{name:s/PATTERN/REPLACEMENT/OPTIONS} the name of the document transformed by a regular expression. The format is intended to be similar to the format used by `sed`. Only one option is supported: `i` for case insensitive.",
            "{id} - The file id reported by Aconso",
            "{date} - The date of the document in format YYYY-MM-DD",
            "{date:YYYYMMDD} - The date of the document using a custom format as used by Java's DateTimeFormatter",
            "Example: --file-name-pattern='{date:YYYY}/{name}.pdf' (uses one directory for each year)",
        ]
    )
    lateinit var fileNamePattern: String

    override fun call(): Int {
        localDir.createDirectories()
        val documentsSynced: MutableList<Document> = mutableListOf()
        val documentsUnchanged: MutableList<Document> = mutableListOf()
        aconsoSync(Url(url), localDir, username, password, PathTemplate(fileNamePattern)) { event ->
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
