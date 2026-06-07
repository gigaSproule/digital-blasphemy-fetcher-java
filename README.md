# digitalblasphemy-fetcher

Fetches the latest digital blasphemy wallpapers and downloads them.

## Run the CLI

### Arguments

| Argument    | Description                                                                  | Example         |
|-------------|------------------------------------------------------------------------------|-----------------|
| -h,--height | Required height of wallpapers                                                | 1080            |
| -p,--path   | Path to put wallpapers                                                       | ~/db-wallpapers |
| -t,--type   | Required type of wallpapers. Valid values are [single, dual, triple, mobile] | single          |
| -w,--width  | Required width of wallpapers                                                 | 1920            |

### Via executable JAR (if Java is already installed)

1. Download the latest `digital-blasphemy-fetcher-[version]-bin.jar` from
   the [releases page](https://github.com/gigaSproule/digital-blasphemy-fetcher-java/releases/latest).
2. Get an API key from [Digital Blasphemy](https://digitalblasphemy.com/my-account/db-api-keys/)
3. Set the `API_KEY` environment variable with your new API key
4. Run the CLI
    ```shell
    java -jar digital-blasphemy-fetcher-[version]-bin.jar -w 1920 -h 1080 -t single -p ~/db-wallpapers
    ```

### Via custom JRE (if Java is not already installed)

1. Download the latest `digital-blasphemy-fetcher-[version].zip` from
   the [releases page](https://github.com/gigaSproule/digital-blasphemy-fetcher-java/releases/latest).
2. Unpack the zip folder
3. Get an API key from [Digital Blasphemy](https://digitalblasphemy.com/my-account/db-api-keys/)
4. Set the `API_KEY` environment variable with your new API key
5. Run the CLI
    ```shell
    digital-blasphemy-fetcher-[version]/bin/digital-blasphemy-fetcher -w 1920 -h 1080 -t single -p ~/db-wallpapers
    ```

