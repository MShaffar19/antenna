/*
 * Copyright (c) Bosch Software Innovations GmbH 2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.frontend.compliancetool.sw360.exporter;

import org.eclipse.sw360.antenna.csvreader.CSVArtifactMapper;
import org.eclipse.sw360.antenna.frontend.compliancetool.sw360.SW360Configuration;
import org.eclipse.sw360.antenna.model.artifact.Artifact;
import org.eclipse.sw360.antenna.model.artifact.facts.ArtifactClearingState;
import org.eclipse.sw360.antenna.model.artifact.facts.ArtifactSourceFile;
import org.eclipse.sw360.antenna.sw360.client.adapter.SW360Connection;
import org.eclipse.sw360.antenna.sw360.rest.resource.SW360HalResource;
import org.eclipse.sw360.antenna.sw360.rest.resource.SW360HalResourceUtility;
import org.eclipse.sw360.antenna.sw360.rest.resource.attachments.SW360AttachmentType;
import org.eclipse.sw360.antenna.sw360.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.antenna.sw360.rest.resource.components.SW360SparseComponent;
import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360ClearingState;
import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360SparseRelease;
import org.eclipse.sw360.antenna.sw360.utils.ArtifactToReleaseUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SW360Exporter {
    private final SW360Configuration configuration;
    private SW360Connection connection;

    public SW360Exporter(SW360Configuration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "Configuration must not be null");
    }

    public void execute() {
        connection = configuration.getConnection();

        Collection<SW360SparseComponent> components = connection.getComponentAdapter().getComponents();

        Collection<SW360SparseRelease> sw360SparseReleases = getReleasesFromComponents(components);

        Collection<SW360Release> sw360ReleasesNotApproved = getNonApprovedReleasesFromSpareReleases(sw360SparseReleases);

        List<Artifact> artifacts = sw360ReleasesNotApproved.stream()
                .map(this::releaseAsArtifact)
                .collect(Collectors.toList());

        File csvFile = configuration.getTargetDir()
                .resolve(configuration.getCsvFileName())
                .toFile();

        CSVArtifactMapper csvArtifactMapper = new CSVArtifactMapper(csvFile.toPath(),
                Charset.forName(configuration.getProperties().get("encoding")),
                configuration.getProperties().get("delimiter").charAt(0),
                Paths.get(configuration.getProperties().get("basedir")));

        csvArtifactMapper.writeArtifactsToCsvFile(artifacts);
    }

    private Artifact releaseAsArtifact(SW360Release release) {
        Artifact artifact = ArtifactToReleaseUtils.convertToArtifactWithoutSourceFile(release, new Artifact("SW360"));
        Set<SW360SparseAttachment> sparseAttachments = getSparseAttachmentsSource(release);
        sparseAttachments.forEach(sparseAttachment -> {
            Optional<Path> path = connection.getReleaseAdapter().downloadAttachment(release, sparseAttachment, configuration.getSourcesPath());
            path.ifPresent(pth -> artifact.addFact(new ArtifactSourceFile(pth)));
        });
        return artifact;
    }

    private Set<SW360SparseAttachment> getSparseAttachmentsSource(SW360Release release) {
        Set<SW360SparseAttachment> attachments = release.get_Embedded().getAttachments();

        return attachments.stream()
                .filter(attachment -> attachment.getAttachmentType() == SW360AttachmentType.SOURCE)
                .collect(Collectors.toSet());
    }

    private Collection<SW360SparseRelease> getReleasesFromComponents(Collection<SW360SparseComponent> components) {
        return components.stream()
                .map(this::getIdFromHalResource)
                .filter(id -> !id.equals(""))
                .map(id -> connection.getComponentAdapter().getComponentById(id))
                .map(component -> component.orElse(null))
                .filter(Objects::nonNull)
                .flatMap(component -> component.get_Embedded().getReleases().stream())
                .collect(Collectors.toList());
    }

    private Collection<SW360Release> getNonApprovedReleasesFromSpareReleases(Collection<SW360SparseRelease> sw360SparseReleases) {
        return sw360SparseReleases.stream()
                .map(this::getIdFromHalResource)
                .filter(id -> !id.equals(""))
                .map(id -> connection.getReleaseAdapter().getReleaseById(id))
                .map(Optional::get)
                .filter(sw360Release -> !isApproved(sw360Release))
                .sorted(Comparator.comparing(SW360Release::getCreatedOn).reversed())
                .collect(Collectors.toList());
    }

    private boolean isApproved(SW360Release sw360Release) {
        return Optional.ofNullable(sw360Release.getClearingState())
                        .map(clearingState -> ArtifactClearingState.ClearingState.valueOf(clearingState) != ArtifactClearingState.ClearingState.INITIAL)
                        .orElse(false) &&
                Optional.ofNullable(sw360Release.getSw360ClearingState())
                        .map(sw360ClearingState -> sw360ClearingState.equals(SW360ClearingState.APPROVED) ||
                                sw360ClearingState.equals(SW360ClearingState.REPORT_AVAILABLE))
                        .orElse(false);
    }

    private <T extends SW360HalResource<?, ?>> String getIdFromHalResource(T halResource) {
        return SW360HalResourceUtility.getLastIndexOfSelfLink(halResource.get_Links().getSelf()).orElse("");
    }
}
