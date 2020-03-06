/*
 * Copyright (c) Bosch Software Innovations GmbH 2018-2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.adapter;

import org.eclipse.sw360.antenna.sw360.client.rest.SW360ReleaseClient;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException;
import org.eclipse.sw360.antenna.sw360.rest.resource.SW360HalResourceUtility;
import org.eclipse.sw360.antenna.sw360.rest.resource.attachments.SW360AttachmentType;
import org.eclipse.sw360.antenna.sw360.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.antenna.sw360.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.rest.resource.components.SW360ComponentEmbedded;
import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360SparseRelease;
import org.eclipse.sw360.antenna.sw360.utils.SW360ComponentAdapterUtils;
import org.eclipse.sw360.antenna.sw360.utils.SW360ReleaseAdapterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils.block;
import static org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils.optionalFuture;

public class SW360ReleaseClientAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SW360ReleaseClientAdapter.class);

    private final SW360ReleaseClient releaseClient;
    private final SW360ComponentClientAdapter sw360ComponentClientAdapter;

    public SW360ReleaseClientAdapter(SW360ReleaseClient client, SW360ComponentClientAdapter componentClientAdapter) {
        releaseClient = client;
        sw360ComponentClientAdapter = componentClientAdapter;
    }

    public SW360ReleaseClient getReleaseClient() {
        return releaseClient;
    }

    public SW360ComponentClientAdapter getComponentAdapter() {
        return sw360ComponentClientAdapter;
    }

    public SW360Release getOrCreateRelease(SW360Release sw360ReleaseFromArtifact, boolean updateReleases) {
        // NOTE: this code does now always merge with the SW360Release used for querying
        return getRelease(sw360ReleaseFromArtifact)
                .map(sw360ReleaseFromArtifact::mergeWith)
                .map(sw360Release -> {
                    if(updateReleases) {
                        return block(getReleaseClient().patchRelease(sw360Release));
                    } else {
                        return sw360Release;
                    }
                })
                .orElseGet(() -> createRelease(sw360ReleaseFromArtifact));
    }

    /*
     * Create a release in SW360
     */
    public SW360Release createRelease(SW360Release releaseFromArtifact) {
        if (! SW360ReleaseAdapterUtils.isValidRelease(releaseFromArtifact)) {
            throw new SW360ClientException("Can not write invalid release for " + releaseFromArtifact.getName() + "-" + releaseFromArtifact.getVersion());
        }
        if (releaseFromArtifact.getReleaseId() != null) {
            throw new SW360ClientException("Can not write release which already has the id " + releaseFromArtifact.getReleaseId());
        }

        if (releaseFromArtifact.getComponentId() == null) {
            final SW360Component componentFromRelease = SW360ComponentAdapterUtils.createFromRelease(releaseFromArtifact);
            final Optional<SW360Component> componentFromSW360 = getComponentAdapter().getOrCreateComponent(componentFromRelease);
            componentFromSW360.ifPresent(cfs -> {
                if (cfs.get_Embedded().getReleases().stream()
                        .map(SW360SparseRelease::getVersion)
                        .anyMatch(releaseFromArtifact.getVersion()::equals)) {
                    throw new SW360ClientException("The release already exists in the found component");
                }
                releaseFromArtifact.setComponentId(cfs.getComponentId());
            });
        }

        return block(getReleaseClient().createRelease(releaseFromArtifact));
    }

    public SW360Release uploadAttachments(SW360Release sw360item, Map<Path, SW360AttachmentType> attachments) {
        for(Map.Entry<Path, SW360AttachmentType> attachment : attachments.entrySet()) {
            if (!attachmentIsPotentialDuplicate(attachment.getKey(), sw360item.get_Embedded().getAttachments())) {
                sw360item = block(getReleaseClient().uploadAndAttachAttachment(sw360item, attachment.getKey(), attachment.getValue()));
            }
        }
        return sw360item;
    }

    private boolean attachmentIsPotentialDuplicate(Path attachment, List<SW360SparseAttachment> attachments) {
        return attachments.stream()
                .anyMatch(attachment1 -> attachment1.getFilename().equals(attachment.getFileName().toString()));
    }

    public Optional<SW360Release> getReleaseById(String releaseId) {
        return block(optionalFuture(getReleaseClient().getRelease(releaseId)));
    }

    public Optional<SW360Release> enrichSparseRelease(SW360SparseRelease sparseRelease) {
        return getReleaseById(sparseRelease.getReleaseId());
    }

    public Optional<SW360SparseRelease> getSparseRelease(SW360Release sw360ReleaseFromArtifact) {
        final Optional<SW360SparseRelease> releaseByExternalId = getReleaseByExternalIds(sw360ReleaseFromArtifact.getExternalIds());
        if (releaseByExternalId.isPresent()) {
            return releaseByExternalId;
        }
        return getReleaseByNameAndVersion(sw360ReleaseFromArtifact);
    }

    public Optional<SW360Release> getRelease(SW360Release sw360ReleaseFromArtifact) {
        return getSparseRelease(sw360ReleaseFromArtifact)
                .map(this::enrichSparseRelease)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Optional<SW360SparseRelease> getReleaseByExternalIds(Map<String, ?> externalIds) {
        final List<SW360SparseRelease> releasesByExternalIds =
                block(getReleaseClient().getReleasesByExternalIds(externalIds));
        if (releasesByExternalIds.isEmpty()) {
            return Optional.empty();
        } else if (releasesByExternalIds.size() == 1) {
            return Optional.of(releasesByExternalIds.get(0));
        } else {
            LOGGER.error("Multiple releases in SW360 matched by externalIDs");
            return Optional.empty();
        }
    }

    public Optional<SW360SparseRelease> getReleaseByNameAndVersion(SW360Release sw360ReleaseFromArtifact) {
        return getComponentAdapter().getComponentByName(sw360ReleaseFromArtifact.getName())
                .map(SW360Component::get_Embedded)
                .map(SW360ComponentEmbedded::getReleases)
                .flatMap(releases -> releases.stream()
                        .filter(release -> sw360ReleaseFromArtifact.getVersion().equals(release.getVersion()))
                        .findFirst());
    }

    public Optional<SW360Release> getReleaseByVersion(SW360Component component, String releaseVersion) {
        if (component != null &&
                component.get_Embedded() != null &&
                component.get_Embedded().getReleases() != null) {

            List<SW360SparseRelease> releases = component.get_Embedded().getReleases();
            Optional<String> releaseId = releases.stream()
                    .filter(release -> release.getVersion().equals(releaseVersion))
                    .findFirst()
                    .flatMap(release -> SW360HalResourceUtility.getLastIndexOfSelfLink(release.get_Links()));
            if (releaseId.isPresent()) {
                return getReleaseById(releaseId.get());
            }
        }
        return Optional.empty();
    }

    public Optional<Path> downloadAttachment(SW360Release release, SW360SparseAttachment attachment, Path downloadPath) {
        return Optional.ofNullable(release.get_Links().getSelf())
                .flatMap(self -> block(optionalFuture(getReleaseClient().downloadAttachment(self.getHref(), attachment, downloadPath))));
    }
}
