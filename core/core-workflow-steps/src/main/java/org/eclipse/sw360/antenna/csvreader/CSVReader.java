/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.csvreader;

import com.github.packageurl.PackageURL;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.sw360.antenna.api.exceptions.ExecutionException;
import org.eclipse.sw360.antenna.model.artifact.Artifact;
import org.eclipse.sw360.antenna.model.artifact.ArtifactCoordinates;
import org.eclipse.sw360.antenna.model.artifact.ArtifactFactWithPayload;
import org.eclipse.sw360.antenna.model.artifact.facts.*;
import org.eclipse.sw360.antenna.model.coordinates.Coordinate;
import org.eclipse.sw360.antenna.model.coordinates.CoordinateBuilder;
import org.eclipse.sw360.antenna.model.xml.generated.License;
import org.eclipse.sw360.antenna.model.xml.generated.LicenseInformation;
import org.eclipse.sw360.antenna.model.xml.generated.MatchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CSVReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSVReader.class);

    private static final String NAME = "Artifact Id";
    private static final String GROUP = "Group Id";
    private static final String VERSION = "Version";
    private static final String COORDINATE_TYPE = "Coordinate Type";
    private static final String EFFECTIVE_LICENSE = "Effective License";
    private static final String DECLARED_LICENSE = "Declared License";
    private static final String OBSERVED_LICENSE = "Observed License";
    private static final String COPYRIGHTS = "Copyrights";
    private static final String HASH = "Hash";
    private static final String SOURCE_URL = "Source URL";
    private static final String RELEASE_ARTIFACT_URL = "Release Tag URL";
    private static final String SWH_ID = "Software Heritage ID";
    private static final String CLEARING_STATE = "Clearing State";
    private static final String CHANGES_STATUS = "Change Status";
    private static final String CPE = "CPE";
    private static final String PATH_NAME = "File Name";

    private Path csvFile;
    private Charset encoding;
    private char delimiter;
    private Path baseDir;


    public CSVReader(Path csvFile, Charset encoding, char delimiter, Path baseDir) {
        this.csvFile = csvFile;
        this.encoding = encoding;
        this.delimiter = delimiter;
        this.baseDir = baseDir;
    }

    public Path writeArtifactsToCsvFile(Collection<Artifact> artifacts) {
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                     NAME,
                     GROUP,
                     VERSION,
                     COORDINATE_TYPE,
                     EFFECTIVE_LICENSE,
                     DECLARED_LICENSE,
                     OBSERVED_LICENSE,
                     COPYRIGHTS,
                     HASH,
                     SOURCE_URL,
                     RELEASE_ARTIFACT_URL,
                     SWH_ID,
                     CLEARING_STATE,
                     CHANGES_STATUS,
                     CPE,
                     PATH_NAME
             ))
        ) {
            for (Artifact artifact : artifacts) {
                csvPrinter.printRecords(makeCsvRecordsFromArtifact(artifact));
            }
            csvPrinter.flush();
            return csvFile.toAbsolutePath();
        } catch (IOException e) {
            LOGGER.error("Error when writing the csv file", e);
            return null;
        }
    }

    private Object[] makeCsvRecordsFromArtifact(Artifact artifact) {
        List<Object> csvRecords = new ArrayList<>();

        List<Coordinate> coordinates = new ArrayList<>(artifact.getCoordinates());

        if (!coordinates.isEmpty()) {
            Set<String> hashes = artifact.askForAll(ArtifactFilename.class)
                    .stream()
                    .map(ArtifactFilename::getArtifactFilenameEntries)
                    .flatMap(Collection::stream)
                    .map(ArtifactFilename.ArtifactFilenameEntry::getHash)
                    .collect(Collectors.toSet());

            if (hashes.isEmpty()) {
                csvRecords.add(
                        makeCsvRecordFromArtifact(artifact, "", coordinates.get(0)));
            } else {
                for (String hash : hashes) {
                    csvRecords.add(
                            makeCsvRecordFromArtifact(artifact, hash, coordinates.get(0)));
                }
            }
            if (coordinates.size() > 1) {
                coordinates.remove(0);
                for (Coordinate coordinate : coordinates) {
                    Artifact tempArtifact = new Artifact()
                            .addCoordinate(coordinate);
                    csvRecords.add(
                            makeCsvRecordFromArtifact(tempArtifact, "", coordinate));
                }
            }
        } else {
            String name = artifact.getMainCoordinate().map(Coordinate::getName).orElse("No Name");
            String namespace = artifact.getMainCoordinate().map(Coordinate::getNamespace).orElse("");
            LOGGER.debug("{}:{} failed to write to csv file, since it does not have coordinates",
                    namespace, name);
        }
        return csvRecords.toArray();
    }

    private Object[] makeCsvRecordFromArtifact(Artifact artifact, String hash, Coordinate coordinate) {
        List<String> csvRecordString = new ArrayList<>();
        csvRecordString.add(coordinate.getName());
        if (!coordinate.getNamespace().isEmpty()) {
            csvRecordString.add(coordinate.getNamespace());
        } else {
            csvRecordString.add("");
        }
        csvRecordString.add(coordinate.getVersion());
        csvRecordString.add(coordinate.getType());
        csvRecordString.add(mapOverriddenLicenseToString(artifact));
        csvRecordString.add(mapDeclaredLicenseToString(artifact));
        csvRecordString.add(mapObservedLicenseToString(artifact));
        csvRecordString.add(mapCopyrightsToString(artifact));
        csvRecordString.add(hash);
        csvRecordString.add(mapSourceUrlToString(artifact));
        csvRecordString.add(mapReleaseTagUrlToString(artifact));
        csvRecordString.add(mapSoftwareHeritagToString(artifact));
        csvRecordString.add(mapClearingStatusToString(artifact));
        csvRecordString.add(mapChangeStatusToString(artifact));
        csvRecordString.add(mapCPEIdToString(artifact));
        csvRecordString.add(mapFileNameToString(artifact));

        return csvRecordString.toArray();
    }

    public Collection<Artifact> createArtifactsList() {

        List<Artifact> artifacts = new ArrayList<>();
        List<CSVRecord> records = getRecordsFromCsvFile();

        for (CSVRecord record : records) {
            Artifact newArtifact = mapRecordToArtifact(record);

            final Optional<Artifact> oldArtifact = artifactListContainsArtifact(artifacts, newArtifact);
            if(oldArtifact.isPresent()) {
                oldArtifact.get().mergeWith(newArtifact);
            } else {
                artifacts.add(newArtifact);
            }
        }

        return artifacts;
    }

    private Optional<Artifact> artifactListContainsArtifact(List<Artifact> artifacts, Artifact artifact) {
        return artifact.askFor(ArtifactCoordinates.class)
                .flatMap(artifactCoordinates -> artifactListContainsArtifact(artifacts, artifactCoordinates));

    }

    private Optional<Artifact> artifactListContainsArtifact(List<Artifact> artifacts, ArtifactCoordinates coordinates) {
        return artifacts.stream()
                .filter(coordinates::matches)
                .findFirst();
    }

    private Artifact mapRecordToArtifact(CSVRecord record) {
        Artifact artifact = new Artifact("CSV")
                .addCoordinate(createCoordinates(record))
                .addFact(new ArtifactMatchingMetadata(MatchState.EXACT));
        addOptionalArtifactFacts(record, artifact);
        return artifact;
    }

    private void addOptionalArtifactFacts(CSVRecord record, Artifact artifact) {
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, EFFECTIVE_LICENSE)) {
            License license = new License();
            license.setName(record.get(EFFECTIVE_LICENSE));
            artifact.addFact(new OverriddenLicenseInformation(license));
        }
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, DECLARED_LICENSE)) {
            License license = new License();
            license.setName(record.get(DECLARED_LICENSE));
            artifact.addFact(new DeclaredLicenseInformation(license));
        }
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, OBSERVED_LICENSE)) {
            License license = new License();
            license.setName(record.get(OBSERVED_LICENSE));
            artifact.addFact(new ObservedLicenseInformation(license));
        }
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, COPYRIGHTS)) {
            artifact.addFact(new CopyrightStatement(record.get(COPYRIGHTS)));
        }
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, HASH)) {
            artifact.addFact(new ArtifactFilename(null, record.get(HASH)));
        }
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, SOURCE_URL)) {
            artifact.addFact(new ArtifactSourceUrl(record.get(SOURCE_URL)));
        }
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, RELEASE_ARTIFACT_URL)) {
            artifact.addFact(new ArtifactReleaseTagURL(record.get(RELEASE_ARTIFACT_URL)));
        }
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, SWH_ID)) {
            try {
                artifact.addFact(new ArtifactSoftwareHeritageID.Builder(record.get(SWH_ID)).build());
            } catch (IllegalArgumentException e) {
                LOGGER.warn(e.getMessage());
            }
        }
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, CLEARING_STATE)) {
            artifact.addFact(new ArtifactClearingState(
                    ArtifactClearingState.ClearingState.valueOf(record.get(CLEARING_STATE))));
        }
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, CHANGES_STATUS)) {
            artifact.addFact(new ArtifactChangeStatus(
                    ArtifactChangeStatus.ChangeStatus.valueOf(record.get(CHANGES_STATUS))));
        }
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, CPE)) {
            artifact.addFact(new ArtifactCPE(record.get(CPE)));
        }
        if (checkIfRecordIsMappedAndNotEmptyForParameter(record, PATH_NAME)) {
            String pathName = record.get(PATH_NAME);
            Path path = getPathFromPathname(pathName);
            if (Files.exists(path)) {
                artifact.addFact(new ArtifactSourceFile(path));
            } else {
                artifact.getMainCoordinate().ifPresent(coordinate ->
                        LOGGER.debug("The given source file {} for artifact {} does not exist.", path, coordinate));
            }
        }
    }

    private Path getPathFromPathname(String pathName) {
        return Paths.get(pathName).isAbsolute()
                ? Paths.get(pathName)
                : baseDir.resolve(Paths.get(pathName)).toAbsolutePath();
    }

    private boolean checkIfRecordIsMappedAndNotEmptyForParameter(CSVRecord record, String parameter) {
        return record.isMapped(parameter) && !record.get(parameter).isEmpty();
    }

    private Coordinate createCoordinates(CSVRecord record) {
        final CoordinateBuilder builder = Coordinate.builder()
                .withName(record.get(NAME))
                .withVersion(record.get(VERSION));

        if (record.isMapped(COORDINATE_TYPE)) {
            final String type = record.get(COORDINATE_TYPE);
            switch (type) {
                case "mvn":
                case "maven":
                    builder.withType(PackageURL.StandardTypes.MAVEN);
                    builder.withNamespace(record.get(GROUP));
                    break;
                case "nuget":
                case "dotnet":
                    builder.withType(PackageURL.StandardTypes.NUGET);
                    break;
                case "npm":
                case "javascript":
                    builder.withType(PackageURL.StandardTypes.NPM);
                    builder.withNamespace(record.get(GROUP));
                    break;
                case "bundle":
                    builder.withType(Coordinate.Types.P2);
                    break;
                default:
                    if (Coordinate.Types.all.contains(type)) {
                        builder.withType(type);
                    } else {
                        builder.withType(PackageURL.StandardTypes.GENERIC);
                    }

                    if (record.isMapped(GROUP)) {
                        builder.withNamespace(record.get(GROUP));
                    }
                    break;
            }
        } else {
            builder.withType(PackageURL.StandardTypes.GENERIC);
            if (record.isMapped(GROUP)) {
                builder.withNamespace(record.get(GROUP));
            }
        }

        return builder.build();
    }

    private List<CSVRecord> getRecordsFromCsvFile() {
        CSVFormat csvFormat = CSVFormat.DEFAULT;
        csvFormat = csvFormat.withFirstRecordAsHeader();
        csvFormat = csvFormat.withDelimiter(delimiter);
        List<CSVRecord> records;

        try (FileInputStream fs = new FileInputStream(csvFile.toAbsolutePath().toString());
             InputStreamReader isr = new InputStreamReader(fs, encoding);
             CSVParser csvParser = new CSVParser(isr, csvFormat)) {
            records = csvParser.getRecords();
        } catch (FileNotFoundException e) {
            throw new ExecutionException(
                    "Antenna is configured to read a CSV configuration file (" + csvFile.toString() + "), but the file wasn't found",
                    e);
        } catch (IOException e) {
            throw new ExecutionException("Error when attempting to parse CSV configuration file: " + csvFile.toString(), e);
        }

        return records;
    }

    private static String mapOverriddenLicenseToString(Artifact artifact) {
        return artifact.askForGet(OverriddenLicenseInformation.class)
                .map(LicenseInformation::evaluate)
                .orElse("");
    }

    private static String mapDeclaredLicenseToString(Artifact artifact) {
        return artifact.askForGet(DeclaredLicenseInformation.class)
                .map(LicenseInformation::evaluate)
                .orElse("");
    }

    private static String mapObservedLicenseToString(Artifact artifact) {
        return artifact.askForGet(ObservedLicenseInformation.class)
                .map(LicenseInformation::evaluate)
                .orElse("");
    }

    private static String mapSourceUrlToString(Artifact artifact) {
        return artifact.askForGet(ArtifactSourceUrl.class)
                .orElse("");
    }

    private static String mapReleaseTagUrlToString(Artifact artifact) {
        return artifact.askForGet(ArtifactReleaseTagURL.class)
                .orElse("");
    }

    private static String mapSoftwareHeritagToString(Artifact artifact) {
        return artifact.askForGet(ArtifactSoftwareHeritageID.class)
                .orElse("");
    }

    private static String mapClearingStatusToString(Artifact artifact) {
        Optional<ArtifactClearingState.ClearingState> cs = artifact.askForGet(ArtifactClearingState.class);
        return cs.map(ArtifactClearingState.ClearingState::toString)
                .orElse("");
    }

    private static String mapChangeStatusToString(Artifact artifact) {
        Optional<ArtifactChangeStatus.ChangeStatus> cs = artifact.askForGet(ArtifactChangeStatus.class);
        return cs.map(ArtifactChangeStatus.ChangeStatus::toString)
                .orElse("");
    }

    private static String mapCopyrightsToString(Artifact artifact) {
        Optional<CopyrightStatement> cs = artifact.askFor(CopyrightStatement.class);
        return cs.map(CopyrightStatement::toString)
                .orElse("");
    }

    private static String mapCPEIdToString(Artifact artifact) {
        return artifact.askForGet(ArtifactCPE.class)
        .orElse("");
    }

    private static String mapFileNameToString(Artifact artifact) {
        return artifact.askFor(ArtifactSourceFile.class)
                .map(ArtifactFactWithPayload::get)
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .orElse("");
    }
}
