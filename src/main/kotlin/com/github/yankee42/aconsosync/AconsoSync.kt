package com.github.yankee42.aconsosync

import HttpXsrf
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.ResponseException
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.accept
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.clone
import io.ktor.http.parametersOf
import io.ktor.http.takeFrom
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.nio.file.Path

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

fun aconsoSync(
    rootUrl: Url,
    localDir: Path,
    username: String,
    password: String,
    fileNameTemplate: PathTemplate,
    eventListener: (event: AconsoEvent) -> Unit
) {
    runBlocking {
        createHttpClient(rootUrl).use { httpClient ->
            AconsoSync(
                httpClient,
                rootUrl,
                LocalRepository(localDir, fileNameTemplate),
                eventListener
            )
                .syncDocuments(username, password)
        }
    }
}

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
