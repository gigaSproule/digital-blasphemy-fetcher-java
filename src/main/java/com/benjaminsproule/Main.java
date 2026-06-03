package com.benjaminsproule;

import com.benjaminsproule.digitalblasphemy.client.DigitalBlasphemyClient;
import com.benjaminsproule.digitalblasphemy.client.model.*;
import com.benjaminsproule.digitalblasphemy.client.model.Wallpaper.Resolutions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) {
        String apiKey = getApiKey();
        Cli cli = Cli.parse(args);
        ensureWallpaperPathExists(cli);
        DigitalBlasphemyClient client = new DigitalBlasphemyClient(apiKey);
        GetAccountInformationResponse accountInfo;
        try {
            accountInfo = client.getAccountInformation().get();
        } catch (ResponseException e) {
            throw new RuntimeException("Unable to get account information", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Unable to get account information", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        int currentPage = 1;
        GetWallpapersRequest.Builder query = GetWallpapersRequest.builder()
                .page(currentPage)
                .order(Order.DESCENDING);

        int totalPages;
        do {
            GetWallpapersResponse wallpapersResponse;
            try {
                wallpapersResponse = client.getWallpapers(query.build()).get();
            } catch (ResponseException e) {
                throw new RuntimeException("Unable to get page " + currentPage + " of wallpapers", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Unable to get page " + currentPage + " of wallpapers", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            totalPages = wallpapersResponse.dbCore().totalPages();
            var wallpapers = wallpapersResponse.dbCore().wallpapers().values();
            List<CompletableFuture<Void>> futures = wallpapers.stream()
                    .map(wallpaper -> {
                        try {
                            return fetchWallpaper(cli, client, wallpaper, accountInfo.user().plus());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
            allOf(futures).join();
            currentPage++;
        } while (currentPage <= totalPages);
    }

    private static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futuresList) {
        CompletableFuture<Void> allDone = CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0]));
        return allDone.thenApply(v -> futuresList.stream().map(CompletableFuture::join).toList());
    }

    private static String getApiKey() {
        String key = System.getenv("API_KEY");
        if (key == null) {
            throw new IllegalStateException("API_KEY is not set");
        }
        return key;
    }

    private static void ensureWallpaperPathExists(Cli cli) {
        try {
            if (Files.notExists(cli.wallpaperPath())) {
                Files.createDirectories(cli.wallpaperPath());
            } else if (!Files.isDirectory(cli.wallpaperPath())) {
                throw new IllegalArgumentException(cli.wallpaperPath() + " is not a valid directory");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static CompletableFuture<Void> fetchWallpaper(Cli cli, DigitalBlasphemyClient client, Wallpaper wallpaper,
            boolean plusUser) throws IOException {
        Resolutions.Resolution resolution = getResolution(cli, wallpaper);
        if (resolution == null) {
            return CompletableFuture.completedFuture(null);
        }
        String[] imageSplit = resolution.image().split("/");
        String filename = imageSplit[imageSplit.length - 1];
        Path expectedPath = Paths.get(cli.wallpaperPath().toString(), filename);
        if (Files.exists(expectedPath)) {
            return CompletableFuture.completedFuture(null);
        }
        return downloadWallpaper(cli, client, wallpaper, expectedPath, plusUser);
    }

    private static Resolutions.Resolution getResolution(Cli cli, Wallpaper wallpaper) {
        var resolutions = wallpaper.resolutions();
        if (resolutions == null) {
            return null;
        }
        var resolutionList = switch (cli.wallpaperType()) {
            case SINGLE -> resolutions.single();
            case DUAL -> resolutions.dual();
            case TRIPLE -> resolutions.triple();
            case MOBILE -> resolutions.mobile();
        };

        if (resolutionList == null) {
            return null;
        }

        String width = "" + cli.width();
        String height = "" + cli.height();
        return resolutionList.stream()
                .filter(r -> width.equals(r.width()) && height.equals(r.height()))
                .findFirst()
                .orElse(null);
    }

    private static CompletableFuture<Void> downloadWallpaper(Cli cli, DigitalBlasphemyClient client,
            Wallpaper wallpaper, Path expectedPath, boolean plusUser) {
        // println!(
        // "Need to download {} to {}",
        // &wallpaper.name,
        // expected_wallpaper_path.display()
        // );
        try {
            return client.downloadWallpaper(
                    expectedPath,
                    DownloadWallpaperRequest.builder()
                            .type(cli.wallpaperType())
                            .width(cli.width())
                            .height(cli.height())
                            .wallpaperId(wallpaper.id())
                            .showWatermark(!plusUser)
                            .build());
        } catch (ResponseException e) {
            if (e.getCode() == 404) {
                // info!(
                // "Unable to download wallpaper {}. This is most likely because the API has
                // returned a valid resolution,
                // even though it doesn't actually have it.",
                // &wallpaper.name
                // );
                return CompletableFuture.completedFuture(null);
            } else {
                throw e;
            }
        }
    }
}
