package com.benjaminsproule;

import com.benjaminsproule.digitalblasphemy.client.model.WallpaperType;
import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.util.Arrays;

import static java.util.stream.Collectors.joining;

public record Cli(int width, int height, WallpaperType wallpaperType, Path wallpaperPath) {
    private static final CommandLineParser parser = new DefaultParser();
    private static final HelpFormatter formatter = new HelpFormatter();
    private static final Options options = getOptions();

    static Cli parse(String[] args) throws ParseException {
        try {
            final CommandLine cmd = parser.parse(options, args);
            final int width = cmd.getParsedOptionValue("width");
            final int height = cmd.getParsedOptionValue("height");
            final WallpaperType wallpaperType = cmd.getParsedOptionValue("type");
            final Path wallpaperPath = cmd.getParsedOptionValue("path");
            return new Cli(width, height, wallpaperType, wallpaperPath);
        } catch (ParseException e) {
            formatter.printHelp("utility-name", options);
            throw e;
        }
    }

    private static Options getOptions() {
        final Options options = new Options();
        options.addOption(
                Option.builder("w")
                        .longOpt("width")
                        .hasArg()
                        .desc("Required width of wallpapers")
                        .required()
                        .type(Integer.class)
                        .build()
        );

        options.addOption(
                Option.builder("h")
                        .longOpt("height")
                        .hasArg()
                        .desc("Required height of wallpapers")
                        .required()
                        .type(Integer.class)
                        .build()
        );

        options.addOption(
                Option.builder("t")
                        .longOpt("type")
                        .hasArg()
                        .desc("Required type of wallpapers. Valid values are %s.".formatted(Arrays.stream(WallpaperType.values()).map(String::valueOf).collect(joining(", "))))
                        .required()
                        .type(WallpaperType.class)
                        .converter(WallpaperType::of)
                        .build()
        );

        options.addOption(Option.builder("p")
                .longOpt("path")
                .hasArg()
                .desc("Path to put wallpapers")
                .required()
                .type(Path.class)
                .build());
        return options;
    }
}
