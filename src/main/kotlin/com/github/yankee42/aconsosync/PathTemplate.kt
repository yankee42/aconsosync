package com.github.yankee42.aconsosync

import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter


class PathTemplate(templateString: String) {
    private val template = parseTemplate(templateString)

    fun getPath(document: Document): Path {
        return Paths.get(StringBuilder().apply { template.forEach { it(document) } }.replaceNonFilenameChars())
    }
}

private typealias TemplateSegment = StringBuilder.(document: Document) -> Unit

private fun CharSequence.replaceNonFilenameChars(replacement: String = "-") =
    replace("[<>:;,?\"*|\\\\]".toRegex(), replacement)

private fun parseTemplate(templateString: String): List<TemplateSegment> {
    val matcher = "\\{([^}:]+)(?::([^}]+))?}".toPattern().matcher(templateString)
    var previousEnd = 0
    val result = ArrayList<TemplateSegment>()
    while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()
        if (start > previousEnd) {
            result += templateString.literalSegment(previousEnd, start)
        }
        result += templateString.dynamicSegment(matcher.start(1), matcher.end(1), matcher.start(2), matcher.end(2))
        previousEnd = end
    }
    if (templateString.length > previousEnd) {
        result += templateString.literalSegment(previousEnd)
    }
    return result
}

private fun String.literalSegment(start: Int, end: Int = length): TemplateSegment =
    { append(this@literalSegment, start, end) }

private fun String.dynamicSegment(nameStart: Int, nameEnd: Int, argStart: Int, argEnd: Int): TemplateSegment {
    when {
        regionMatches(nameStart, "name") -> {
            if (argStart >= 0) {
                val argument = substring(argStart, argEnd)
                val match = "^s/(.*?[^\\\\])/(.*)/(i?)$".toRegex().find(argument)
                    ?: throw IllegalArgumentException("Illegal argument <$argument>")
                val regexString = match.groupValues[1]
                val replacementString = match.groupValues[2]
                val optionsString = match.groupValues[3]
                val regex =
                    if (optionsString.isEmpty()) regexString.toRegex() else regexString.toRegex(RegexOption.IGNORE_CASE)
                return { append(it.fileName.replace('/', '-').replace(regex, replacementString)) }
            }
            return { append(it.fileName.replace('/', '-')) }
        }
        regionMatches(nameStart, "id") -> return { append(it.fileId) }
        regionMatches(nameStart, "date") -> {
            val format = DateTimeFormatter.ofPattern(if (argStart >= 0) substring(argStart, argEnd) else "YYYY-MM-DD")
            return { format.formatTo(it.documentDate, this) }
        }
    }
    throw IllegalArgumentException("unknown variable <${substring(nameStart, nameEnd)}>")
}

private fun String.regionMatches(start: Int, compare: String): Boolean =
    regionMatches(start, compare, 0, compare.length)
