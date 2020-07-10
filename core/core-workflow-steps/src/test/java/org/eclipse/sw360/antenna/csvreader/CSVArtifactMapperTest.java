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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.sw360.antenna.model.artifact.Artifact;
import org.eclipse.sw360.antenna.model.artifact.facts.*;
import org.eclipse.sw360.antenna.model.coordinates.Coordinate;
import org.eclipse.sw360.antenna.model.license.LicenseInformation;
import org.eclipse.sw360.antenna.model.xml.generated.MatchState;
import org.eclipse.sw360.antenna.util.LicenseSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class CSVArtifactMapperTest {
    private static final String ARTIFACT_DOWNLOAD_URL = "https://organisation-test.org/";
    private static final String ARTIFACT_CLEARING_STATE = "PROJECT_APPROVED";
    private static final String ARTIFACT_TAG_URL = "https://gitTool.com/project/repository";
    private static final String ARTIFACT_MAVEN_COORDINATES = "pkg:maven/test/test1@1.0.0";
    private static final String ARTIFACT_SOFTWAREHERITAGE_ID = "swh:1:rel:1234512345123451234512345123451234512345";
    private static final String ARTIFACT_CHANGESTATUS = "AS_IS";
    private static final String ARTIFACT_COPYRIGHT = "Copyright xxxx Some Copyright Enterprise";
    private static final char DELIMITER = ',';
    private static final String CLEARING_DOC_NAME = "clearing.doc";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File csvFile;

    @Before
    public void setUp() throws IOException {
        csvFile = folder.newFile("csvTest.csv");
    }

    private String[] csvColumns = {
            "Artifact Id",
            "Group Id",
            "Version",
            "Coordinate Type",
            "Effective License",
            "Declared License",
            "Observed License",
            "Copyrights",
            "Hash",
            "Source URL",
            "Release Tag URL",
            "Software Heritage ID",
            "Clearing State",
            "Clearing Document",
            "Change Status",
            "CPE",
            "File Name"};

    @Test
    public void testRoundTripWriteRead() {
        List<Artifact> artifacts = new ArrayList<>();
        artifacts.add(mkArtifact("test", false).addFact(new ArtifactMatchingMetadata(MatchState.EXACT)));
        CSVArtifactMapper csvArtifactMapper = new CSVArtifactMapper(csvFile.toPath(), StandardCharsets.UTF_8, DELIMITER, csvFile.getParentFile().toPath());
        csvArtifactMapper.writeArtifactsToCsvFile(artifacts);
        Collection<Artifact> csvReaderArtifacts = csvArtifactMapper.createArtifactsList();
        assertThat(csvReaderArtifacts).isEqualTo(artifacts);
    }

    @Test
    public void writeReleaseListToCSVFileTest() throws IOException {
        List<Artifact> artifacts = new ArrayList<>();
        artifacts.add(mkArtifact("test", false));
        artifacts.add(mkArtifact("test1", false)
                .addCoordinate(new Coordinate("pkg:maven/test/test1@1.2.3"))
                .addCoordinate(new Coordinate("pkg:p2/test/test1@1.2.3")));
        CSVArtifactMapper csvArtifactMapper = new CSVArtifactMapper(csvFile.toPath(), StandardCharsets.UTF_8, DELIMITER, csvFile.getParentFile().toPath());
        csvArtifactMapper.writeArtifactsToCsvFile(artifacts);

        assertThat(csvFile.exists()).isTrue();

        CSVParser csvParser = getCsvParser(csvFile);
        assertThat(csvParser.getRecords().size()).isEqualTo(3);
    }

    @Test
    public void writeAbsoluteFilenameToCSVFileTest() throws URISyntaxException {
        Artifact artifact =
                mkArtifact("test", false)
                        .addFact(new ArtifactSourceFile(
                                Paths.get(this.getClass().getClassLoader().getResource("CsvAnalyzerTest/test_source.txt").toURI())));
        List<Artifact> artifacts = Collections.singletonList(artifact);

        CSVArtifactMapper csvArtifactMapper = new CSVArtifactMapper(csvFile.toPath(), StandardCharsets.UTF_8, DELIMITER, csvFile.getParentFile().toPath());
        csvArtifactMapper.writeArtifactsToCsvFile(artifacts);

        assertThat(csvFile.exists()).isTrue();

        List<Artifact> artifactsList = (List<Artifact>) csvArtifactMapper.createArtifactsList();
        assertThat(artifactsList).hasSize(1);
        assertThat(artifactsList.get(0).askFor(ArtifactSourceFile.class)).isPresent();
    }

    @Test
    public void writeRelativeFilenameToCSVFileTest() throws IOException {
        String sourceFolderName = "sources";
        String sourceFileName = "my-sources-jar";
        Path sourceFolder = folder.newFolder(sourceFolderName).toPath();
        Path sourceFile = Files.write(sourceFolder.resolve(sourceFileName),
                "some source".getBytes(StandardCharsets.UTF_8));
        Path baseDir = folder.getRoot().toPath();
        Artifact artifact =
                mkArtifact("test", false)
                        .addFact(new ArtifactSourceFile(sourceFile));
        List<Artifact> artifacts = Collections.singletonList(artifact);

        CSVArtifactMapper csvArtifactMapper = new CSVArtifactMapper(csvFile.toPath(), StandardCharsets.UTF_8, DELIMITER,
                baseDir);
        csvArtifactMapper.writeArtifactsToCsvFile(artifacts);

        List<Artifact> artifactsList = (List<Artifact>) csvArtifactMapper.createArtifactsList();
        assertThat(artifactsList.get(0).askFor(ArtifactSourceFile.class)).isPresent();
    }

    @Test
    public void writeNonExistentFilenameToCSVFileTest() {
        Artifact artifact =
                mkArtifact("test", false)
                        .addFact(new ArtifactSourceFile(Paths.get("non-existent-source-file.tgz")));
        List<Artifact> artifacts = Collections.singletonList(artifact);

        CSVArtifactMapper csvArtifactMapper = new CSVArtifactMapper(csvFile.toPath(), StandardCharsets.UTF_8, DELIMITER,
                csvFile.getParentFile().toPath());
        csvArtifactMapper.writeArtifactsToCsvFile(artifacts);

        assertThat(csvFile.exists()).isTrue();

        List<Artifact> artifactsList = (List<Artifact>) csvArtifactMapper.createArtifactsList();
        assertThat(artifactsList).hasSize(1);
        assertThat(artifactsList.get(0).askFor(ArtifactSourceFile.class)).isNotPresent();
    }

    @Test
    public void writeSingleReleaseListToCSVFileTest() throws IOException {
        List<Artifact> oneArtifact = Collections.singletonList(mkArtifact("test:test", true)
                .addFact(new ArtifactCPE("cpeId")));
        CSVArtifactMapper csvArtifactMapper = new CSVArtifactMapper(csvFile.toPath(), StandardCharsets.UTF_8, DELIMITER, csvFile.getParentFile().toPath());
        csvArtifactMapper.writeArtifactsToCsvFile(oneArtifact);

        assertThat(csvFile.exists()).isTrue();

        CSVParser csvParser = getCsvParser(csvFile);
        List<CSVRecord> records = csvParser.getRecords();
        assertThat(records.size()).isEqualTo(1);
        CSVRecord csvRecord = records.get(0);
        for (String csvColumn : csvColumns) {
            if (!csvColumn.equals("File Name")) {
                assertThat(csvRecord.get(csvColumn).isEmpty()).isFalse();
            }
        }
    }

    @Test
    public void writeEmptyReleaseListToCSVFileTest() throws IOException {
        List<Artifact> emptyArtifacts = new ArrayList<>();
        CSVArtifactMapper csvArtifactMapper = new CSVArtifactMapper(csvFile.toPath(), StandardCharsets.UTF_8, DELIMITER, csvFile.getParentFile().toPath());
        csvArtifactMapper.writeArtifactsToCsvFile(emptyArtifacts);

        assertThat(csvFile.exists()).isTrue();

        CSVParser csvParser = getCsvParser(csvFile);
        assertThat(csvParser.getHeaderMap().size()).isEqualTo(csvColumns.length);
        assertThat(csvParser.getRecordNumber()).isEqualTo(0);
    }

    private Artifact mkArtifact(String name, boolean withOverridden) {
                Artifact artifact = new Artifact("CSV");
        artifact.addCoordinate(new Coordinate(ARTIFACT_MAVEN_COORDINATES));

        addLicenseFact(Optional.of("Declared-1.0"), artifact, DeclaredLicenseInformation::new, artifact.askFor(DeclaredLicenseInformation.class).isPresent());
        addLicenseFact(Optional.of("Observed-2.0"), artifact, ObservedLicenseInformation::new, artifact.askFor(ObservedLicenseInformation.class).isPresent());
        if (withOverridden) {
            addLicenseFact(Optional.of("Overridden-1.2"), artifact, OverriddenLicenseInformation::new, artifact.askFor(OverriddenLicenseInformation.class).isPresent());
        }
        artifact.addFact(new ArtifactSourceUrl(ARTIFACT_DOWNLOAD_URL));
        artifact.addFact(new ArtifactReleaseTagURL(ARTIFACT_TAG_URL));
        artifact.addFact(new ArtifactSoftwareHeritageID.Builder(ARTIFACT_SOFTWAREHERITAGE_ID).build());
        artifact.addFact(new ArtifactFilename(null, ("12345678" + name)));
        artifact.addFact(new ArtifactFilename(null, ("12345678" + name)));
        artifact.addFact(new ArtifactClearingState(ArtifactClearingState.ClearingState.valueOf(ARTIFACT_CLEARING_STATE)));
        artifact.addFact(new ArtifactClearingDocument(createClearingDocument()));
        artifact.addFact(new ArtifactChangeStatus(ArtifactChangeStatus.ChangeStatus.valueOf(ARTIFACT_CHANGESTATUS)));
        artifact.addFact(new CopyrightStatement(ARTIFACT_COPYRIGHT));

        return artifact;
    }

    private Path createClearingDocument() {
        Path clearingDoc = folder.getRoot().toPath().resolve(CLEARING_DOC_NAME);
        if (!Files.exists(clearingDoc)) {
            try {
                folder.newFile(CLEARING_DOC_NAME);
            } catch (IOException e) {
                throw new AssertionError("Could not create clearing document file", e);
            }
        }
        return clearingDoc;
    }

    private void addLicenseFact(Optional<String> licenseRawData, Artifact artifact, Function<LicenseInformation, ArtifactLicenseInformation> licenseCreator, boolean isAlreadyPresent) {
        licenseRawData.map(LicenseSupport::parseSpdxExpression)
                .map(licenseCreator)
                .ifPresent(artifact::addFact);
    }

    private static CSVParser getCsvParser(File currentCsvFile) throws IOException {
        FileInputStream fs = new FileInputStream(currentCsvFile);
        InputStreamReader isr = new InputStreamReader(fs, StandardCharsets.UTF_8);
        CSVFormat csvFormat = CSVFormat.DEFAULT;
        csvFormat = csvFormat.withFirstRecordAsHeader();
        csvFormat = csvFormat.withDelimiter(',');
        return new CSVParser(isr, csvFormat);
    }
}
