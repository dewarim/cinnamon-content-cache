## Changelog

### 1.2.1

Make timeouts configurable:

       <lockAcquisitionTimeoutMillis>300000</lockAcquisitionTimeoutMillis>
       <lockAcquisitionCheckPeriodMillis>1000</lockAcquisitionCheckPeriodMillis>       

The first value defines how long a request may wait for content to be downloaded.

The second value defines how long to wait between checking if a download is done. 

### 1.2

Refactored Reaper into a standalone process - it's no longer a background process of the
main server.

