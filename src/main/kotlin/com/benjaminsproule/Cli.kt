package com.benjaminsproule

import com.benjaminsproule.digitalblasphemy.client.model.WallpaperType
import org.apache.commons.cli.*
import java.nio.file.Path

data class Cli(val width: Int, val height: Int, val wallpaperType: WallpaperType, val wallpaperPath: Path) {
    companion object {
        private val parser: CommandLineParser = DefaultParser()
        private val formatter = HelpFormatter()
        private val options: Options = getOptions()

        fun parse(args: Array<String>): Cli {
            try {
                val cmd: CommandLine = parser.parse(options, args)
                val width = cmd.getParsedOptionValue<Int>("width")
                val height = cmd.getParsedOptionValue<Int>("height")
                val wallpaperType = cmd.getParsedOptionValue<WallpaperType>("type")
                val wallpaperPath = cmd.getParsedOptionValue<Path>("path")
                return Cli(width, height, wallpaperType, wallpaperPath)
            } catch (e: ParseException) {
                formatter.printHelp("utility-name", options)
                throw e
            }
        }

        private fun getOptions(): Options {
            val options = Options()
            options.addOption(
                Option.builder("w")
                    .longOpt("width")
                    .hasArg()
                    .desc("Required width of wallpapers")
                    .required()
                    .type(Integer::class.java)
                    .build()
            )

            options.addOption(
                Option.builder("h")
                    .longOpt("height")
                    .hasArg()
                    .desc("Required height of wallpapers")
                    .required()
                    .type(Integer::class.java)
                    .build()
            )

            options.addOption(
                Option.builder("t")
                    .longOpt("type")
                    .hasArg()
                    .desc(
                        "Required type of wallpapers. Valid values are ${
                            WallpaperType.entries.joinToString(", ") { it.toString() }
                        }."
                    )
                    .required()
                    .type(WallpaperType::class.java)
                    .converter({ WallpaperType.of(it) })
                    .build()
            )

            options.addOption(
                Option.builder("p")
                    .longOpt("path")
                    .hasArg()
                    .desc("Path to put wallpapers")
                    .required()
                    .type(Path::class.java)
                    .build()
            )
            return options
        }
    }
}
