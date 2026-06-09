package com.xirpl2.SASMobile.helper

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer

class MockServer {

    private val server = MockWebServer()
    private val responses = mutableMapOf<String, MockResponse>()

    val baseUrl: String get() = server.url("/").toString()

    fun start() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val key = "${request.method}:${request.requestUrl?.encodedPath ?: "/"}"
                responses.forEach { (pattern, response) ->
                    if (pattern.contains(":") && request.method == pattern.substringBefore(":")
                        && request.requestUrl?.encodedPath?.contains(pattern.substringAfter(":")) == true) {
                        return response
                    }
                }
                return responses[key] ?: responses[request.method + ":*"]
                ?: MockResponse().setResponseCode(404).setBody("""{"status":"error","message":"Not found"}""")
            }
        }
        try { server.start() } catch (_: Exception) {}
    }

    fun shutdown() {
        try { server.shutdown() } catch (_: Exception) {}
    }

    fun addResponse(methodAndPath: String, jsonBody: String, code: Int = 200) {
        responses[methodAndPath] = MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(jsonBody)
    }

    fun addResponse(methodAndPath: String, bytes: ByteArray, code: Int = 200) {
        responses[methodAndPath] = MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "image/png")
            .setBody(Buffer().apply { write(bytes) })
    }
}
