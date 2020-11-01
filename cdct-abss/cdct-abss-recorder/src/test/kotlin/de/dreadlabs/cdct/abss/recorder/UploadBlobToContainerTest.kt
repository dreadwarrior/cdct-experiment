package de.dreadlabs.cdct.abss.recorder

import com.azure.storage.blob.BlobClientBuilder
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.io.BufferedInputStream
import java.nio.file.Files
import java.util.Properties
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UploadBlobToContainerTest {

    private val properties: Properties = javaClass
        .getResource("/recorder.properties")
        .run {
            openStream().use {
                Properties().apply { load(it) }
            }
        }

    private val wiremockServer = WireMockServer(WireMockConfiguration
        .options()
        .bindAddress("127.0.0.1")
        .port(8080))
        .apply {
            start()
        }

    private val wiremock = WireMock(8080).apply {
        startStubRecording(properties.getProperty("record_url"))
    }

    @Test
    fun `check properties loading`() {
        assertTrue(properties.containsKey("connection_string"))
        assertFalse(
            properties.getProperty("connection_string")
                .run { isNullOrEmpty() || isNullOrBlank() },
            "The connection string must not be null, empty or blank."
        )

        assertTrue(properties.containsKey("container_name"))
        assertFalse(
            properties.getProperty("container_name")
                .run { isNullOrEmpty() || isNullOrBlank() },
            "The container name must not be null, empty or blank."
        )

        assertTrue(properties.containsKey("record_url"))
        assertFalse(
            properties.getProperty("record_url")
                .run { isNullOrEmpty() || isNullOrBlank() },
            "The record URL must not be null, empty or blank."
        )
    }

    @Test
    fun `upload blobs to a container is easy`() {
        val fixture = FileFixture("/data", "test.txt", "Test payload")

        with(fixture) {
            val settledFixture = this.settle()

            val blobClient = BlobClientBuilder()
                .connectionString(properties.getProperty("connection_string"))
                .containerName(properties.getProperty("container_name"))
                .blobName(name)
                .buildClient()

            blobClient.upload(
                BufferedInputStream(Files.newInputStream(settledFixture)),
                Files.size(settledFixture)
            )

            Files.delete(settledFixture)
            blobClient.delete()

        }

        wiremock.stopStubRecording()
    }

    @AfterAll
    fun stopRecording() {
        wiremockServer.stop()
    }
}