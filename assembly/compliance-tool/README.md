# COMPLIANCE TOOL

This is a frontend of antenna that can be used as a compliance manager tool.
This is especially targeted to create clean and approved metadata in an SW360 instance.

## <a name="SW360Exporter">SW360 Exporter</a>
The SW360 Exporter requests all components from a given SW360 instance and filters the releases of the components to check if they have a state that shows them as "cleared" or "approved".
It then creates a list with the data of those releases and writes them sorted by creation date (from latest to oldest) in a csv file.

### Properties
- `csvFilePath`: Path and name where the csv file should be saved
- `proxyHost`: If a proxy is in use, supply the host name
- `proxyPort`: If a proxy is in use, supply the port
- `proxyUse`: If a proxy is in use, this should be set to false
- `sw360restServerUrl`: Link to the rest server url of your SW360 instance
- `sw360authServerUrl`: Link to the authentication server url of your SW360 instance
- `sw360user`: SW360 Username
- `sw360password`: SW360 User Password
- `sw360clientId`: SW360 Client Id
- `sw360clientPassword`: SW360 Client Password

## SW360 Updater
The SW360Updater takes a given csv file with release information and maps the contents on SW360Release objects that are posted into the given SW360 instance. 
It has the ability to update release information of already existing releases. 

### Properties
- `csvFilePath`: Path and name to the csv file with the release information
- `delimiter`: Delimiter used in the csv file to separate columns (by default it is `,`)
- `encoding`: Encoding of the csv file, normally `UTF-8` 
- `proxyHost`: If a proxy is in use, supply the host name
- `proxyPort`: If a proxy is in use, supply the port
- `proxyUse`: If a proxy is in use, this should be set to false
- `sw360restServerUrl`: Link to the rest server url of your SW360 instance
- `sw360authServerUrl`: Link to the authentication server url of your SW360 instance
- `sw360user`: SW360 Username
- `sw360password`: SW360 User Password
- `sw360clientId`: SW360 Client Id
- `sw360clientPassword`: SW360 Client Password
- `sw360updateReleases`: Boolean value that determines if release data is patched should new information be added to an already existent release
- `sw360uploadSources`: Boolean value that determines if source files of a release are uploaded should they be present.

## Notes to Usage
- This updater is part of a round trip with the [SW360 Exporter](#SW360Exporter). 
  The created csv file from the exporter can be enriched with data and then used for the updater.
- The csv functionality and mapping is done with the [CSV Analyzer](https://eclipse.github.io/antenna/1.0.0-RC6/analyzers/csv-analyzer-step.html). 
  The column names and their meanings can be checked there. 

### Environment Properties
It is possible to give credentials via environment properties. 
If you want one of the properties in your properties file to be resolved to an environment variable, 
add the `$` prefix to it and wrap it in curly braces `{}`, additionally to the variable name being written in upper case letters:
    
    ```
    key=${ENV_VARIABLE}
    ```
  