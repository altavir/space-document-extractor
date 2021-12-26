package ru.mipt.npm.space.documentextractor

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.SpaceHttpClientWithCallContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.streams.toList

suspend fun SpaceHttpClientWithCallContext.extractImage(
    client: HttpClient,
    spaceUrl: String,
    parent: Path,
    imageId: String,
    imageFileName: String,
) {
    val request = HttpRequestBuilder().apply {
        val token = callContext.tokenSource.token()
        url("$spaceUrl/d/$imageId")
        method = HttpMethod.Get
        header(HttpHeaders.Authorization, "Bearer ${token.accessToken}")
    }
    val response = client.request<HttpResponse>(request)
    val file = parent.resolve("images/$imageFileName")
    file.writeBytes(response.readBytes())
}

private val regex = """!\[(?<fileName>.*)]\(/d/(?<id>.*)\?f=0""".toRegex()

suspend fun SpaceHttpClientWithCallContext.processDocument(client: HttpClient, spaceUrl: String, path: Path) {
    val documentBody = path.readText()
    val logger = LoggerFactory.getLogger("space-document-extractor")
    logger.info("Processing file $path...")
    coroutineScope {
        val newText = documentBody.replace(regex) {
            val id = it.groups["id"]?.value ?: error("Unexpected reference format: ${it.value}")
            val fileName = it.groups["fileName"]?.value ?: id
            launch {
                logger.info("Downloading image $id")
                extractImage(client, spaceUrl, path.parent, id, fileName)
            }
            "![](images/$fileName"
        }
        path.writeText(newText)
    }
}

suspend fun SpaceHttpClientWithCallContext.processDirectory(
    client: HttpClient,
    spaceUrl: String,
    path: Path,
    fileExtension: String = ".md",
    recursive: Boolean = true,
) {
    Files.list(path).toList().forEach {
        if (it.toString().endsWith(fileExtension)) {
            processDocument(client, spaceUrl, it)
        } else if (recursive && it.isDirectory()) {
            processDirectory(client, spaceUrl, it, fileExtension)
        }
    }
}