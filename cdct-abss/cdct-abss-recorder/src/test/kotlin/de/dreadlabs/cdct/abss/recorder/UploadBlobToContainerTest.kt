package de.dreadlabs.cdct.abss.recorder

import com.azure.storage.blob.BlobServiceClientBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Properties
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UploadBlobToContainerTest {

    private val properties: Properties = javaClass.getResource("/recorder.properties").run {
        openStream().use {
            Properties().apply { load(it) }
        }
    }

    init {
        WireMock.startRecording("http://127.0.0.1:10000")
    }

    @Test
    fun `check properties loading`() {
        assertTrue(properties.containsKey("connection_string"))
        assertFalse(properties.getProperty("connection_string").run { isNullOrEmpty() || isNullOrBlank() }, "The connection string must not be null, empty or blank.")
    }

    @Test
    fun `upload blobs to a container is easy`() {
        val testTxt = assumeUnixFileSystem().getPath("/data").let {
            Files.createDirectory(it)

            it.resolve("test.txt")
        }.also {
            Files.write(it, listOf("Test payload"), StandardCharsets.UTF_8)
        }

        val client = BlobServiceClientBuilder().connectionString(properties.getProperty("connection_string")).buildClient()
        val containerClient = client.createBlobContainer("cdct-storage-container")
        val blobClient = containerClient.getBlobClient("test.txt")

        blobClient.uploadFromFile(testTxt.fileName.toString())
    }

    private fun assumeUnixFileSystem() = Jimfs.newFileSystem(Configuration.unix())

    @AfterAll
    fun stopRecording() {
        WireMock.stopRecording()
    }
}