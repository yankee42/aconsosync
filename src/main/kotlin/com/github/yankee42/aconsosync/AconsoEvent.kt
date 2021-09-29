package com.github.yankee42.aconsosync

sealed class AconsoEvent
class DocumentListDownloadStartedEvent : AconsoEvent()
class DocumentListDownloadedEvent(val documentList: DocTree) : AconsoEvent()
