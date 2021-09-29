package com.github.yankee42.aconsosync

import HttpXsrf
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.lang.RuntimeException

class AconsoSync(private val http: HttpClient,
                 rootUrl: Url,
                 private val repository: LocalRepository,
                 private val eventListener: (event: AconsoEvent) -> Unit) {
    private val urlBuilder = URLBuilder(rootUrl)
    private val logger = LoggerFactory.getLogger(AconsoSync::class.java)

    suspend fun syncDocuments(username: String, password: String) {
        logger.info("Logging in...")
        logIn(username, password)
        logger.info("Retrieving list of documents...")
        coroutineScope {
            getDocumentListing()
                .documents
                .also { logger.info("Found {} documents", it.size) }
                .asFlow()
                .collect { launch { this@AconsoSync.syncDocument(it) } }
        }
        logger.info("DONE")
    }

    @Suppress("EXPERIMENTAL_API_USAGE_FUTURE_ERROR")
    private suspend fun getDocumentListing(): DocTree {
        eventListener(DocumentListDownloadStartedEvent())

        return http.get<DocTree>(
            resolvePath("api/v1/internal/documents").apply { parameters.apply {
                append("sortField", "ATT_DOC_DATE")
                append("sortOrder", "desc")
                append("limit", "0")
                append("offset", "0")
                append("structure", "true")
            }}.build()
        ).also { eventListener(DocumentListDownloadedEvent(it)) }
    }

    private suspend fun logIn(username: String, password: String) {
        try {
            http.submitForm<BasicResponse>(
                parametersOf(
                    "username" to listOf(username),
                    "password" to listOf(password)
                )
            ) { url.takeFrom(resolvePath("api/v1/external/login")) }
        } catch (e: ResponseException) {
            throw LoginFailedException(e.response.receive(), e)
        }
    }

    private suspend fun syncDocument(document: Document) {
        repository.addDocumentIfNotExists(document, {
            logger.info("downloading document {} (#{})", document.fileName, document.fileId)
            http.get(resolvePath("api/v1/internal/documents/${document.fileId}/pdf").build()) {
                accept(ContentType.Any)
            }
        }) { eventListener(it) }
    }

    private fun resolvePath(path: String) = urlBuilder.clone().apply { encodedPath += path }
}

class LoginFailedException(msg: String, cause: Throwable) : RuntimeException(msg, cause)

const val XSRF_COOKIE = "XSRF-TOKEN"

fun createHttpClient(rootUrl: Url) = HttpClient(CIO) {
    install(HttpCookies) {
        runBlocking {
            // initially we just use a random XSRF Cookie:
            storage.addCookie(
                rootUrl,
                Cookie(
                    XSRF_COOKIE,
                    "3884d786-12bf-4e20-9b8d-19bf557f309a",
                    domain = rootUrl.host,
                    encoding = CookieEncoding.RAW
                )
            )
        }
    }
    install(HttpXsrf) {
        cookieName = XSRF_COOKIE
    }
    install(JsonFeature) {
        serializer = KotlinxSerializer(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
    }
}
