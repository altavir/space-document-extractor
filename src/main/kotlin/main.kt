package ru.mipt.npm.space.documentextractor

import io.ktor.client.engine.cio.CIO
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlinx.coroutines.coroutineScope
import space.jetbrains.api.runtime.SpaceAppInstance
import space.jetbrains.api.runtime.SpaceAuth
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.ktorClientForSpace
import space.jetbrains.api.runtime.resources.projects
import space.jetbrains.api.runtime.types.FolderIdentifier
import space.jetbrains.api.runtime.types.ProjectIdentifier
import java.nio.file.Files
import java.nio.file.Path

suspend fun main(args: Array<String>) {
    val parser = ArgParser("space-document-extractor")

    val spaceUrl by parser.option(
        ArgType.String,
        description = "Url of the space instance like 'https://mipt-npm.jetbrains.space'"
    ).required()

    val project by parser.option(
        ArgType.String,
        description = "The key of the exported project"
    ).required()

    val path: String? by parser.option(ArgType.String, description = "Target directory. Default is './output/project-key'.")

    val folderId: String? by parser.option(
        ArgType.String,
        description = "FolderId for the folder to export. By default uses project root."
    )

    val clientId by parser.option(
        ArgType.String,
        description = "Space application client ID (if not defined, use environment value 'space.clientId')"
    )

    val clientSecret by parser.option(
        ArgType.String,
        description = "Space application client secret (if not defined, use environment value 'space.clientSecret')"
    )

    parser.parse(args)

    val target: Path = path?.let { Path.of(path) } ?: Path.of("output/$project")

    Files.createDirectories(target)

    val space: SpaceClient = SpaceClient(
        ktorClientForSpace(CIO),
        SpaceAppInstance(
            clientId ?: System.getProperty("space.clientId"),
            clientSecret ?: System.getProperty("space.clientSecret"),
            spaceUrl
        ),
        SpaceAuth.ClientCredentials()
    )

    coroutineScope {
        println("Processing project \"${space.projects.getProject(ProjectIdentifier.Key(project)).name}\"")
        space.downloadAndProcessDocumentsInProject(
            target,
            ProjectIdentifier.Key(project),
            folderId?.let { FolderIdentifier.Id(it) } ?: FolderIdentifier.Root
        )
    }
}