package com.benjaminsproule

import com.benjaminsproule.digitalblasphemy.client.DigitalBlasphemyClient
import com.benjaminsproule.digitalblasphemy.client.model.*
import com.benjaminsproule.digitalblasphemy.client.model.Wallpaper.Resolutions
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.allOf
import java.util.concurrent.ExecutionException


fun main(args: Array<String>) {
    val apiKey: String = apiKey

    val cli = Cli.parse(args)

    ensureWallpaperPathExists(cli)

    val client = DigitalBlasphemyClient(apiKey)

    val getAccountInformationResponse: GetAccountInformationResponse
    try {
        getAccountInformationResponse = client.getAccountInformation().get()
    } catch (e: ResponseException) {
        throw RuntimeException("Unable to get account information", e)
    } catch (e: ExecutionException) {
        throw RuntimeException("Unable to get account information", e)
    }

    var currentPage = 1
    val query = GetWallpapersRequest.builder()
        .page(currentPage)
        .order(Order.DESCENDING)

    var totalPages: Int
    do {
        val getWallpapersResponse: GetWallpapersResponse
        try {
            getWallpapersResponse = client.getWallpapers(query.build()).get()
        } catch (e: ResponseException) {
            throw RuntimeException("Unable to get page $currentPage of wallpapers", e)
        } catch (e: ExecutionException) {
            throw RuntimeException("Unable to get page $currentPage of wallpapers", e)
        }

        totalPages = getWallpapersResponse.dbCore().totalPages()

        val wallpapers = getWallpapersResponse.dbCore().wallpapers().values

        val futures = wallpapers
            .map { wallpaper: Wallpaper ->
                try {
                    return@map fetchWallpaper(
                        cli,
                        client,
                        wallpaper,
                        getAccountInformationResponse.user().plus()
                    )
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
            .toList()

        allOf(futures).join()

        currentPage++
    } while (currentPage <= totalPages)
}

fun <T> allOf(futuresList: List<CompletableFuture<T>>): CompletableFuture<List<T>> {
    val allFuturesResult =
        allOf(*futuresList.toTypedArray<CompletableFuture<*>?>())
    return allFuturesResult.thenApply<List<T>> {
        futuresList.map<CompletableFuture<T>, T> { it.join() }
    }
}

private val apiKey: String
    get() {
        val apiKey = System.getenv("API_KEY")
        requireNotNull(apiKey) { "API_KEY is not set" }
        return apiKey
    }

private fun ensureWallpaperPathExists(cli: Cli) {
    if (Files.notExists(cli.wallpaperPath)) {
        Files.createDirectories(cli.wallpaperPath)
    } else check(Files.isDirectory(cli.wallpaperPath)) { "${cli.wallpaperPath} is not a valid directory" }
}

private fun fetchWallpaper(
    cli: Cli,
    client: DigitalBlasphemyClient,
    wallpaper: Wallpaper,
    plusUser: Boolean
): CompletableFuture<Void> {
    val resolution = getResolution(cli, wallpaper) ?: return CompletableFuture.completedFuture(null)

    val imageSplit: Array<String> =
        resolution.image().split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val filename = imageSplit[imageSplit.size - 1]

    val expectedWallpaperPath = Paths.get(cli.wallpaperPath.toString(), filename)
    if (Files.exists(expectedWallpaperPath)) {
        return CompletableFuture.completedFuture(null)
    }
    return downloadWallpaper(cli, client, wallpaper, expectedWallpaperPath, plusUser)
}

private fun getResolution(cli: Cli, wallpaper: Wallpaper): Resolutions.Resolution? {
    val resolutions = wallpaper.resolutions() ?: return null

    val resolutionList = when (cli.wallpaperType) {
        WallpaperType.SINGLE -> resolutions.single()
        WallpaperType.DUAL -> resolutions.dual()
        WallpaperType.TRIPLE -> resolutions.triple()
        WallpaperType.MOBILE -> resolutions.mobile()
    }

    if (resolutionList == null) {
        return null
    }

    val width = cli.width.toString()
    val height = cli.height.toString()

    return resolutionList.firstOrNull { resolution: Resolutions.Resolution -> resolution.width() == width && resolution.height() == height }
}

private fun downloadWallpaper(
    cli: Cli,
    client: DigitalBlasphemyClient,
    wallpaper: Wallpaper,
    expectedWallpaperPath: Path,
    plusUser: Boolean
): CompletableFuture<Void> {
//        println!(
//                "Need to download {} to {}",
//        &wallpaper.name,
//                expected_wallpaper_path.display()
//    );
    try {
        return client.downloadWallpaper(
            expectedWallpaperPath,
            DownloadWallpaperRequest.builder()
                .type(cli.wallpaperType)
                .width(cli.width)
                .height(cli.height)
                .wallpaperId(wallpaper.id())
                .showWatermark(!plusUser)
                .build()
        )
    } catch (responseException: ResponseException) {
        if (responseException.code == 404) {
//                info!(
//                        "Unable to download wallpaper {}. This is most likely because the API has returned a valid resolution,
//                        even though it doesn't actually have it.",
//                &wallpaper.name
//            );
        } else {
            throw responseException
        }
    }
    return CompletableFuture.completedFuture(null)
}
