package com.benjaminsproule.digitalblasphemy.fetcher;

import com.benjaminsproule.digitalblasphemy.client.model.WallpaperType;
import org.apache.commons.cli.*;
import java.nio.file.Path;
import java.util.Arrays;

public class Cli {
    private final int width;
    private final int height;
    private final WallpaperType wallpaperType;
    private final Path wallpaperPath;

    public Cli(int width, int height, WallpaperType wallpaperType, Path wallpaperPath) {
        this.width = width;
        this.height = height;
        this.wallpaperType = wallpaperType;
        this.wallpaperPath = wallpaperPath;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public WallpaperType wallpaperType() {
        return wallpaperType;
    }

    public Path wallpaperPath() {
        return wallpaperPath;
    }

    public static Cli parse(String[] args) {
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        Options options = getOptions();
        try {
            CommandLine cmd = parser.parse(options, args);
            int width = cmd.getParsedOptionValue("width");
            int height = cmd.getParsedOptionValue("height");
            WallpaperType type = cmd.getParsedOptionValue("type");
            Path path = cmd.getParsedOptionValue("path");
            return new Cli(width, height, type, path);
        } catch (ParseException e) {
            formatter.printHelp("digital-blasphemy-fetcher", options);
            throw new RuntimeException(e);
        }
    }

    private static Options getOptions() {
        Options opts = new Options();
        opts.addOption(Option.builder("w")
                .longOpt("width")
                .hasArg()
                .desc("Required width of wallpapers")
                .required()
                .type(Integer.class)
                .get());

        opts.addOption(Option.builder("h")
                .longOpt("height")
                .hasArg()
                .desc("Required height of wallpapers")
                .required()
                .type(Integer.class)
                .get());

        opts.addOption(Option.builder("t")
                .longOpt("type")
                .hasArg()
                .desc("Required type of wallpapers. Valid values are " + Arrays.toString(WallpaperType.values()))
                .required()
                .type(WallpaperType.class)
                .converter(WallpaperType::of)
                .get());

        opts.addOption(Option.builder("p")
                .longOpt("path")
                .hasArg()
                .desc("Path to put wallpapers")
                .required()
                .type(Path.class)
                .get());
        return opts;
    }
}
