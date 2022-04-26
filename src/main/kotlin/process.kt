package ru.mipt.npm.space.documentextractor

import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.Batch
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.resources.projects
import space.jetbrains.api.runtime.types.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.streams.toList

private val logger by lazy { LoggerFactory.getLogger("space-extractor") }

internal suspend fun SpaceClient.extractImage(
    parent: Path,
    imageId: String,
    imageFileName: String,
) {
    logger.info("Downloading image file $imageFileName to $parent")
    val response = ktorClient.request<HttpResponse> {
        url("${server.serverUrl}/d/$imageId")
        method = HttpMethod.Get
        header(HttpHeaders.Authorization, "Bearer ${token().accessToken}")
    }
    val file = parent.resolve("images/$imageFileName")
    file.parent.createDirectories()
    file.writeBytes(response.readBytes())
}

internal suspend fun SpaceClient.extractDocument(
    parent: Path,
    documentId: String,
    documentFileName: String,
) {
    //https://mipt-npm.jetbrains.space/drive/files/3qe9i43qtPq2
    logger.info("Downloading document file $documentFileName to $parent")
    val response = ktorClient.request<HttpResponse> {
        url("${server.serverUrl}/drive/files/$documentId")
        method = HttpMethod.Get
        header(HttpHeaders.Authorization, "Bearer ${token().accessToken}")
    }
    val file = parent.resolve(documentFileName)
    file.outputStream().use {
        response.content.copyTo(it)
    }
}

private val regex = """!\[(?<alt>.*)]\(/d/(?<id>.*)\?f=0""".toRegex()

internal suspend fun SpaceClient.processMarkdownDocument(path: Path)  = coroutineScope{
    val documentBody = path.readText()
    val logger = LoggerFactory.getLogger("space-document-extractor")
    logger.info("Processing file $path...")
    val newText = documentBody.replace(regex) {
        val id = it.groups["id"]?.value ?: error("Unexpected reference format: ${it.value}")
        val alt = it.groups["alt"]?.value
        logger.info("Downloading image $id as images/$id")
        launch(Dispatchers.IO) {
            extractImage(path.parent, id, id)
        }
        "![$alt](images/$id"
    }
    path.writeText(newText)

}

/**
 * Download images for markdown documents in the directory
 */
internal suspend fun SpaceClient.processMarkdownInDirectory(
    path: Path,
    fileExtension: String = ".md",
    recursive: Boolean = true,
) {
    Files.list(path).toList().forEach {
        if (it.toString().endsWith(fileExtension)) {
            logger.info("Updating links in a markdown $it")
            processMarkdownDocument(it)
        } else if (recursive && it.isDirectory()) {
            processMarkdownInDirectory(it, fileExtension)
        }
    }
}

internal suspend fun SpaceClient.downloadDocument(
    directory: Path,
    document: Document,
) = coroutineScope {
    when (val body = document.documentBody) {
        is FileDocumentBody -> {
            launch(Dispatchers.IO) {
                extractDocument(directory, document.id, document.title)
            }
        }
        is TextDocument -> {
            val markdownFilePath = directory.resolve(document.title + ".md")
            markdownFilePath.writeText(body.text, Charsets.UTF_8)
        }
        else -> {
            LoggerFactory.getLogger("space-extractor")
                .warn("Can't extract document ${document.title} with type ${document.bodyType}")
        }
    }
}

internal suspend fun SpaceClient.downloadDocumentFolder(
    directory: Path,
    projectId: ProjectIdentifier,
    folderId: FolderIdentifier,
) {
    directory.createDirectories()
    logger.info("Processing folder ${folderId.compactId} to $directory")
    val documents = projects.documents.folders.documents.listDocumentsInFolder(projectId, folderId) {
        id()
    }
    documents.data.forEach {
        val document = projects.documents.getDocument(projectId, it.id) {
            id()
            title()
            documentBody()
            bodyType()
        }
        downloadDocument(directory, document)
    }

    val subFolders: Batch<DocumentFolder> = projects.documents.folders.subfolders.listSubfolders(projectId, folderId)
    subFolders.data.forEach {
        val subPath = directory.resolve(it.name)
        downloadDocumentFolder(subPath, projectId, FolderIdentifier.Id(it.id))
    }
}

suspend fun SpaceClient.downloadAndProcessDocumentsInProject(
    directory: Path,
    projectId: ProjectIdentifier,
    rootFolder: FolderIdentifier = FolderIdentifier.Root,
) {
    logger.info("Processing project ${projectId.compactId} to $directory")
    downloadDocumentFolder(directory, projectId, rootFolder)
    processMarkdownInDirectory(directory)
}