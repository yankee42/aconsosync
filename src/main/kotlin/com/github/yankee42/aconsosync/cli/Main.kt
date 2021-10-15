package com.github.yankee42.aconsosync.cli

import picocli.CommandLine

fun main(args: Array<String>) {
    CommandLine(Main()).execute(*args)
}
