# Cinnamon Content Cache

A caching server for Cinnamon 3

## Usage

### Starting the server

    java -jar target/cinnamon-content-cache-jar-with-dependencies.jar --config config.xml

### Generating an example config file

    java -jar target/cinnamon-content-cache-jar-with-dependencies.jar --write-config example.xml

    Example configuration:
    <CinnamonConfig>
      <!-- cache server config: -->
      <serverConfig>
        <port>9090</port>
        <systemRoot>/opt/cinnamon/cinnamon-system</systemRoot>
        <dataRoot>/opt/cinnamon/cinnamon-cache</dataRoot>
        <!-- how often to check the cache for entries which have been deleted on master: -->
        <reaperIntervalInMillis>300000</reaperIntervalInMillis>
      </serverConfig>
      <!-- logging is not yet configurable yet: -->
      <logbackLoggingConfigPath>/opt/cinnamon/logback.xml</logbackLoggingConfigPath>
      <systemAdministratorEmail/>
      <!-- configuration for reaching the master server: -->
      <remoteConfig>
        <hostname>localhost</hostname>
        <protocol>http</protocol>
        <port>8080</port>
        <contentUrl>/cinnamon/osd/getContentXml</contentUrl>
        <currentUrl>/cinnamon/osd/isCurrent</currentUrl>
        <existsUrl>/cinnamon/osd/exists</existsUrl>
        <!-- access token to call the exists end point. Must be configured on the master server, too -->
        <reaperAccessToken>REAPER_ACCESS_TOKEN</reaperAccessToken>
      </remoteConfig>
    </CinnamonConfig>

## License

LGPL 3 for non-commercial use and non-production evaluation purposes.

## Author

Ingo Wiarda - ingo_wiarda@dewarim.de