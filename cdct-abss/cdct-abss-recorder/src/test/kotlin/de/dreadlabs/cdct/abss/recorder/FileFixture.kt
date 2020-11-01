package de.dreadlabs.cdct.abss.recorder

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

data class FileFixture(
    val inPath: String,
    val name: String,
    val content: String
)

fun FileFixture.settle(): Path =
    assumeUnixFileSystem().getPath(inPath).let {
        Files.createDirectory(it)

        it.resolve(name)
    }.also {
        Files.write(it, listOf(content), StandardCharsets.UTF_8)
    }

private fun assumeUnixFileSystem() =
    Jimfs.newFileSystem(Configuration.unix())