/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.frontend.testProjects;

import org.eclipse.sw360.antenna.model.xml.generated.WorkflowStep;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.sw360.antenna.frontend.testProjects.TestProjectUtils.mkDeactivatedWorkflowStep;
import static org.eclipse.sw360.antenna.frontend.testProjects.TestProjectUtils.mkWorkflowStep;

public class BasicConfiguration {
    public List<WorkflowStep> getProcessors() {
        WorkflowStep confHandler = mkWorkflowStep("Processing of antenna configuration", "org.eclipse.sw360.antenna.workflow.processors.AntennaConfHandler");
        // enricher
        WorkflowStep enricher1 = mkWorkflowStep("Maven Artifact Resolver", "org.eclipse.sw360.antenna.workflow.processors.enricher.MavenArtifactResolver");
        WorkflowStep enricher2 = mkDeactivatedWorkflowStep("P2 Artifact Resolver", "org.eclipse.sw360.antenna.workflow.processors.enricher.P2ArtifactResolverMetaStep");
        WorkflowStep enricher3 = mkWorkflowStep("Child Jar Resolver", "org.eclipse.sw360.antenna.workflow.processors.enricher.ChildJarResolver");
        WorkflowStep enricher4 = mkWorkflowStep("Manifest Resolver", "org.eclipse.sw360.antenna.workflow.processors.enricher.ManifestResolver");
        WorkflowStep enricher5 = mkWorkflowStep("License Resolver", "org.eclipse.sw360.antenna.workflow.processors.enricher.LicenseResolver");
        WorkflowStep enricher6 = mkWorkflowStep("License Knowledgebase Resolver", "org.eclipse.sw360.antenna.workflow.processors.enricher.LicenseKnowledgeBaseResolver");
        // validators
        WorkflowStep validator1 = mkWorkflowStep("Coordinates Validator", "org.eclipse.sw360.antenna.workflow.processors.CoordinatesValidator",
                "failOnMissingCoordinates", "WARN");
        WorkflowStep validator2 = mkWorkflowStep("Source Validator", "org.eclipse.sw360.antenna.workflow.processors.SourceValidator",
                "missingSourcesSeverity", "WARN",
                "incompleteSourcesSeverity", "WARN");

        Map<String,String> configMap = new HashMap<>();
        configMap.put("forbiddenLicenseSeverity", "FAIL");
        configMap.put("missingLicenseInformationSeverity", "WARN");
        configMap.put("missingLicenseTextSeverity", "WARN");
        configMap.put("forbiddenLicenses", "");
        configMap.put("ignoredLicenses", "");
        WorkflowStep validator3 = mkWorkflowStep("License Validator", "org.eclipse.sw360.antenna.workflow.processors.LicenseValidator",
                              configMap);
        WorkflowStep validator4 = mkWorkflowStep("Match State Validator", "org.eclipse.sw360.antenna.workflow.processors.MatchStateValidator",
                "severityOfSIMILAR", "INFO",
                "severityOfUNKNOWN", "WARN");
        configMap = new HashMap<>();
        configMap.put("forbiddenSecurityIssueStatusSeverity", "FAIL");
        configMap.put("securityIssueSeverityLimitSeverity", "FAIL");
        configMap.put("forbiddenSecurityIssueStatuses", "Open");
        configMap.put("securityIssueSeverityLimit", "5.0");
        WorkflowStep validator5 = mkWorkflowStep("Security Issue Validator", "org.eclipse.sw360.antenna.workflow.processors.SecurityIssueValidator",
                configMap);
        // sort
        WorkflowStep sorter = mkWorkflowStep("Sort Artifacts", "org.eclipse.sw360.antenna.workflow.processors.ArtifactSorter");
        return Stream.of(confHandler,
                enricher1, enricher2, enricher3, enricher4, enricher5, enricher6,
                validator1, validator2, validator3, validator4, validator5,
                sorter).collect(Collectors.toList());
    }

    public List<WorkflowStep> getGenerators(String projectRoot) {
        WorkflowStep generator1 = mkWorkflowStep("SW360 Report Generator", "org.eclipse.sw360.antenna.workflow.generators.SW360DisclosureDocumentGenerator",
                "disclosure.doc.formats", "docx,txt,html");
        WorkflowStep generator2 = mkWorkflowStep("Source Zip Writer", "org.eclipse.sw360.antenna.workflow.generators.SourceZipWriter");
        return Stream.of(generator1, generator2).collect(Collectors.toList());
    }
}
