/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.sw360;

import org.eclipse.sw360.antenna.api.IProject;
import org.eclipse.sw360.antenna.api.exceptions.AntennaException;
import org.eclipse.sw360.antenna.model.Artifact;
import org.eclipse.sw360.antenna.model.xml.generated.License;
import org.eclipse.sw360.antenna.sw360.adapter.*;
import org.eclipse.sw360.antenna.sw360.rest.SW360AuthenticationClient;
import org.eclipse.sw360.antenna.sw360.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.rest.resource.licenses.SW360License;
import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.rest.resource.users.SW360User;
import org.eclipse.sw360.antenna.sw360.utils.SW360ComponentAdapterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SW360MetaDataUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(SW360MetaDataUpdater.class);

    // rest service adapters
    private SW360AuthenticationClient authenticationClient;
    private SW360ProjectClientAdapter projectClientAdapter;
    private SW360LicenseClientAdapter licenseClientAdapter;
    private SW360ComponentClientAdapter componentClientAdapter;
    private SW360ReleaseClientAdapter releaseClientAdapter;
    private SW360UserClientAdapter userClientAdapter;

    private String userId;
    private String password;

    public SW360MetaDataUpdater(String restServerUrl, String authServerUrl, String userId, String password) {
        this.userId = userId;
        this.password = password;

        authenticationClient = new SW360AuthenticationClient(authServerUrl);
        projectClientAdapter = new SW360ProjectClientAdapter(restServerUrl);
        licenseClientAdapter = new SW360LicenseClientAdapter(restServerUrl);
        componentClientAdapter = new SW360ComponentClientAdapter(restServerUrl);
        releaseClientAdapter = new SW360ReleaseClientAdapter(restServerUrl);
        userClientAdapter = new SW360UserClientAdapter(restServerUrl);
    }

    public Set<String> getOrCreateLicenses(Artifact artifact) throws IOException, AntennaException {
        HttpHeaders header = createHttpHeaders(userId, password);
        Set<String> licenseIds = new HashSet<>();

        List<License> licenses = flattenedLicenses(artifact);
        for (License license : licenses) {
            SW360License sw360License;
            if (!licenseClientAdapter.isLicenseOfArtifactAvailable(license, header)) {
                sw360License = licenseClientAdapter.addLicense(license, header);
            } else {
                LOGGER.debug("License [" + license.getName() + "] already exists in SW360.");
                sw360License = licenseClientAdapter.getSW360LicenseByAntennaLicense(license, header);
            }
            licenseIds.add(sw360License.getShortName());
        }
        return licenseIds;
    }

    public SW360Release getOrCreateRelease(Artifact artifact, Set<String> licenseIds, SW360Component component) throws IOException, AntennaException {
        HttpHeaders header = createHttpHeaders(userId, password);

        if (!releaseClientAdapter.isArtifactAvailableAsRelease(artifact, component, header)) {
            return releaseClientAdapter.addRelease(artifact, component, licenseIds, header);
        } else {
            Optional<SW360Release> release = releaseClientAdapter.getReleaseByArtifact(component, artifact, header);
            if (release.isPresent()) {
                return release.get();
            } else {
                throw new AntennaException("No release found for the artifact [" +
                        SW360ComponentAdapterUtils.createComponentName(artifact.getArtifactIdentifier()) + "]");
            }
        }
    }

    public SW360Component getOrCreateComponent(Artifact artifact) throws IOException, AntennaException {
        HttpHeaders header = createHttpHeaders(userId, password);

        if (!componentClientAdapter.isArtifactAvailableAsComponent(artifact, header)) {
            return componentClientAdapter.addComponent(artifact, header);
        } else {
            Optional<SW360Component> component = componentClientAdapter.getComponentByArtifact(artifact, header);
            if (component.isPresent()) {
                return component.get();
            } else {
                throw new AntennaException("No component found for the artifact [" +
                        SW360ComponentAdapterUtils.createComponentName(artifact.getArtifactIdentifier()) + "]");
            }
        }
    }

    public void createProject(IProject project, Collection<SW360Release> releases) throws AntennaException, IOException {
        String id;
        HttpHeaders header = createHttpHeaders(userId, password);
        SW360User user = userClientAdapter.getUserById(userId, header);

        Optional<String> projectId = projectClientAdapter.getProjectIdByNameAndVersion(project, header);

        if (projectId.isPresent()) {
            // TODO: Needs endpoint on sw360 to update project on sw360
            LOGGER.debug("Could not update project " + projectId.get() + ", because the endpoint is not available.");
            id = projectId.get();
        } else {
            id = projectClientAdapter.addProject(project, user, header);
        }
        projectClientAdapter.addSW360ReleasesToSW360Project(id, releases, header);
    }

    private List<License> flattenedLicenses(Artifact artifact) {
        return Stream
                .of(artifact.getFinalLicenses().getLicenses())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(l -> Objects.nonNull(l.getName()))
                .distinct()
                .collect(Collectors.toList());
    }

    private HttpHeaders createHttpHeaders(String userId, String password) throws AntennaException {
        return authenticationClient.getHeadersWithBearerToken(authenticationClient.getOAuth2AccessToken(userId, password));
    }
}
