# Cinnamon Content Cache

A caching server for Cinnamon 3

## Changelog

## 1.3.0

Upgrade dependencies due to log4j vulnerability.

### 1.2.1

Make timeouts configurable:

       <lockAcquisitionTimeoutMillis>300000</lockAcquisitionTimeoutMillis>
       <lockAcquisitionCheckPeriodMillis>1000</lockAcquisitionCheckPeriodMillis>       

The first value defines how long a request may wait for content to be downloaded.

The second value defines how long to wait between checking if a download is done. 

### 1.2

Refactored Reaper into a standalone process - it's no longer a background process of the
main server.

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
        <!-- timeout for downloads: use higher number on slower connections -->
        <lockAcquisitionTimeoutMillis>300000</lockAcquisitionTimeoutMillis>
        <lockAcquisitionCheckPeriodMillis>1000</lockAcquisitionCheckPeriodMillis>    
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