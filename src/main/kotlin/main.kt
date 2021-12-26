package ru.mipt.npm.space.documentextractor

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import space.jetbrains.api.runtime.SpaceHttpClient
import space.jetbrains.api.runtime.SpaceHttpClientWithCallContext
import space.jetbrains.api.runtime.withServiceAccountTokenSource
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

suspend fun main(args: Array<String>) {
    val parser = ArgParser("space-document-extractor")
    val path by parser.option(ArgType.String, description = "Input file or directory").required()

    val spaceUrl by parser.option(
        ArgType.String,
        description = "Url of the space instance like 'https://mipt-npm.jetbrains.space'"
    ).required()

    val clientId by parser.option(
        ArgType.String,
        description = "Space application client ID (if not defined, use environment value 'space.clientId')"
    )

    val clientSecret by parser.option(
        ArgType.String,
        description = "Space application client secret (if not defined, use environment value 'space.clientSecret')"
    )


    parser.parse(args)

    val pathValue: Path = Path.of(path)

    if (!pathValue.exists()) {
        error("File or directory not found at $path")
    }

    val client = HttpClient(CIO)
    val space: SpaceHttpClientWithCallContext = SpaceHttpClient(client).withServiceAccountTokenSource(
        clientId = clientId ?: System.getProperty("space.clientId"),
        clientSecret = clientSecret ?: System.getProperty("space.clientSecret"),
        serverUrl = "https://mipt-npm.jetbrains.space"
    )

    if (pathValue.isDirectory()) {
        space.processDirectory(client, spaceUrl, pathValue)
    } else {
        space.processDocument(client, spaceUrl, pathValue)
    }

}