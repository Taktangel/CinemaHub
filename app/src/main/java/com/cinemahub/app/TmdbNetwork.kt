package com.cinemahub.app

import android.content.Context
import coil.ImageLoader
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * TMDB-only DNS fallback using Quad9 (unfiltered) DNS-over-HTTPS.
 * HTTPS hostname validation and certificate verification are retained.
 * Does NOT bypass a blocked TCP/TLS connection.
 */
object TmdbNetwork {
    private val standard = Dns.SYSTEM
    private val quad9: Dns by lazy {
        DnsOverHttps.Builder()
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(8, TimeUnit.SECONDS)
                    .build()
            )
            .url("https://dns10.quad9.net/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("9.9.9.10"),
                InetAddress.getByName("149.112.112.10")
            )
            .build()
    }

    private fun isTmdb(host: String) =
        host == "api.themoviedb.org" ||
            host == "image.tmdb.org"

    val dns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
        if (!isTmdb(hostname)) {
            standard.lookup(hostname)
        } else {
            val systemResult = try {
                standard.lookup(hostname).filterNot { it.isLoopbackAddress || it.isAnyLocalAddress }
            } catch (_: IOException) {
                emptyList()
            }
            if (systemResult.isNotEmpty()) {
                systemResult
            } else {
                val secureResult = quad9.lookup(hostname).filterNot {
                    it.isLoopbackAddress || it.isAnyLocalAddress
                }
                if (secureResult.isEmpty()) {
                    throw java.net.UnknownHostException("TMDB DNS: no public addresses from Quad9")
                }
                secureResult
            }
        }
    }

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(dns)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun readJson(url: String): String {
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException(
                    when (response.code) {
                        401 -> "Неверный TMDB API Key (v3)"
                        429 -> "TMDB временно ограничил количество запросов"
                        else -> "Ошибка TMDB: HTTP ${response.code}"
                    }
                )
            }
            return response.body?.string() ?: throw IOException("TMDB вернул пустой ответ")
        }
    }

    fun imageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context.applicationContext)
            .okHttpClient(httpClient)
            .build()
}
