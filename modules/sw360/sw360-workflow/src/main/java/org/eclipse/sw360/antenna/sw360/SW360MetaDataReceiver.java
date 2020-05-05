/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.sw360;

import org.eclipse.sw360.antenna.api.exceptions.ExecutionException;
import org.eclipse.sw360.antenna.model.artifact.Artifact;
import org.eclipse.sw360.antenna.sw360.client.adapter.SW360ComponentClientAdapter;
import org.eclipse.sw360.antenna.sw360.client.adapter.SW360Connection;
import org.eclipse.sw360.antenna.sw360.client.adapter.SW360LicenseClientAdapter;
import org.eclipse.sw360.antenna.sw360.client.adapter.SW360ReleaseClientAdapter;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360License;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360SparseLicense;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.utils.ArtifactToComponentUtils;
import org.eclipse.sw360.antenna.sw360.utils.ArtifactToReleaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;

public class SW360MetaDataReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SW360MetaDataReceiver.class);

    // rest service adapters
    private SW360ComponentClientAdapter componentClientAdapter;
    private SW360ReleaseClientAdapter releaseClientAdapter;
    private SW360LicenseClientAdapter licenseClientAdapter;

    public SW360MetaDataReceiver(SW360Connection sw360ConnectionConfiguration) {
        componentClientAdapter = sw360ConnectionConfiguration.getComponentAdapter();
        releaseClientAdapter = sw360ConnectionConfiguration.getReleaseAdapter();
        licenseClientAdapter = sw360ConnectionConfiguration.getLicenseAdapter();
    }

    public Optional<SW360Release> findReleaseForArtifact(Artifact artifact) {
        Optional<SW360Component> component = getComponentByArtifact(artifact);
        if (component.isPresent()) {

            String releaseVersionOfArtifact = ArtifactToReleaseUtils.createSW360ReleaseVersion(artifact);
            return releaseClientAdapter.getReleaseByVersion(component.get(), releaseVersionOfArtifact);
        }
        return Optional.empty();
    }

    public Optional<SW360License> getLicenseDetails(SW360SparseLicense sparseLicense) {
        return licenseClientAdapter.getLicenseDetails(sparseLicense);
    }

    public Optional<Path> downloadAttachment(SW360Release release, SW360SparseAttachment attachment, Path downloadPath) {
        return releaseClientAdapter.downloadAttachment(release, attachment, downloadPath);
    }

    private Optional<SW360Component> getComponentByArtifact(Artifact artifact) {
        try {
            String componentName = ArtifactToComponentUtils.createComponentName(artifact);
            return componentClientAdapter.getComponentByName(componentName);
        } catch (ExecutionException e) {
            LOGGER.debug("No component found for {}. Reason: {}", artifact.prettyPrint(), e.getMessage());
            LOGGER.debug("Error: ", e);
            return Optional.empty();
        }
    }

}
