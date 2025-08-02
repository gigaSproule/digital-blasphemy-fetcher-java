package com.benjaminsproule;

import com.benjaminsproule.digitalblasphemy.client.DigitalBlasphemyClient;
import com.benjaminsproule.digitalblasphemy.client.model.*;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

public class Main {
    public static void main(String[] args) throws ParseException, IOException, InterruptedException {
        final String apiKey = getApiKey();

        Cli cli = Cli.parse(args);

        ensureWallpaperPathExists(cli);

        DigitalBlasphemyClient client = new DigitalBlasphemyClient(apiKey);

        GetAccountInformationResponse getAccountInformationResponse;
        try {
            getAccountInformationResponse = client.getAccountInformation();
        } catch (ResponseException e) {
            throw new RuntimeException("Unable to get account information", e);
        }

        int currentPage = 1;
        GetWallpapersRequest.Builder query = GetWallpapersRequest.builder()
                .page(currentPage)
                .order(Order.DESCENDING);

        int totalPages;
        do {
            GetWallpapersResponse getWallpapersResponse;
            try {
                getWallpapersResponse = client.getWallpapers(query.build());
            } catch (ResponseException e) {
                throw new RuntimeException("Unable to get page %d of wallpapers".formatted(currentPage), e);
            }

            totalPages = getWallpapersResponse.dbCore().totalPages();

            Collection<Wallpaper> wallpapers = getWallpapersResponse.dbCore().wallpapers().values();

            List<Future> futures = wallpapers.stream().map((wallpaper) -> fetchWallpaper(cli, client, wallpaper, getAccountInformationResponse.user().plus()));

            futures.wait();

            currentPage++;
        } while (currentPage <= totalPages);
    }

    private static String getApiKey() {
        final String apiKey = System.getenv("API_KEY");
        if (apiKey == null) {
            throw new IllegalArgumentException("API_KEY is not set");
        }
        return apiKey;
    }

    private static void ensureWallpaperPathExists(Cli cli) throws IOException {
        if (Files.notExists(cli.wallpaperPath())) {
            Files.createDirectories(cli.wallpaperPath());
        } else if (!Files.isDirectory(cli.wallpaperPath())) {
            throw new IllegalStateException("%s is not a valid directory".formatted(cli.wallpaperPath()));
        }
    }

    private static void fetchWallpaper(Cli cli, DigitalBlasphemyClient client, Wallpaper wallpaper, boolean plusUser) throws IOException, ResponseException {
        Optional<Wallpaper.Resolutions.Resolution> resolution = getResolution(cli, wallpaper);
        if (resolution.isEmpty()) {
            return;
        }

        String[] imageSplit = resolution.get().image().split("/");
        String filename = imageSplit[imageSplit.length - 1];

        Path expectedWallpaperPath = Paths.get(cli.wallpaperPath().toString(), filename);
        if (Files.notExists(expectedWallpaperPath)) {
            downloadWallpaper(cli, client, wallpaper, expectedWallpaperPath, plusUser);
        }
    }

    private static Optional<Wallpaper.Resolutions.Resolution> getResolution(Cli cli, Wallpaper wallpaper) throws IOException, ResponseException {
        Wallpaper.Resolutions resolutions = wallpaper.resolutions();

        if (resolutions == null) {
            return Optional.empty();
        }

        List<Wallpaper.Resolutions.Resolution> resolutionList = switch (cli.wallpaperType()) {
            case SINGLE -> resolutions.single();
            case DUAL -> resolutions.dual();
            case TRIPLE -> resolutions.triple();
            case MOBILE -> resolutions.mobile();
        };

        if (resolutionList == null) {
            return Optional.empty();
        }

        String width = String.valueOf(cli.width());
        String height = String.valueOf(cli.height());

        return resolutionList.stream()
                .filter(resolution -> resolution.width().equals(width) && resolution.height().equals(height))
                .findFirst();
    }

    private static void downloadWallpaper(Cli cli, DigitalBlasphemyClient client, Wallpaper wallpaper, Path expectedWallpaperPath, boolean plusUser) throws ResponseException, IOException {
//        println!(
//                "Need to download {} to {}",
//        &wallpaper.name,
//                expected_wallpaper_path.display()
//    );
        try {
            client.downloadWallpaper(
                    expectedWallpaperPath,
                    DownloadWallpaperRequest.builder()
                            .type(cli.wallpaperType())
                            .width(cli.width())
                            .height(cli.height())
                            .wallpaperId(wallpaper.id())
                            .showWatermark(!plusUser)
                            .build()
            );
        } catch (ResponseException responseException) {
            if (responseException.getCode() == 404) {
//                info!(
//                        "Unable to download wallpaper {}. This is most likely because the API has returned a valid resolution, even though it doesn't actually have it.",
//                &wallpaper.name
//            );
            } else {
                throw responseException;
            }
        }
    }
}
