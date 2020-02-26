/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2018.
 * Copyright (c) Verifa Oy 2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.sw360.rest;

import org.eclipse.sw360.antenna.sw360.rest.resource.SW360Attributes;
import org.eclipse.sw360.antenna.sw360.rest.resource.projects.SW360Project;
import org.eclipse.sw360.antenna.sw360.rest.resource.projects.SW360ProjectList;
import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360ReleaseList;
import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360SparseRelease;
import org.eclipse.sw360.antenna.sw360.utils.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.sw360.antenna.sw360.utils.SW360ClientUtils.checkRestStatus;
import static org.eclipse.sw360.antenna.sw360.utils.SW360ClientUtils.getSw360Projects;
import static org.eclipse.sw360.antenna.sw360.utils.SW360ClientUtils.getSw360SparseReleases;

public class SW360ProjectClient extends SW360Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(SW360ProjectClient.class);
    private static final String PROJECTS_ENDPOINT = "/projects";
    private final String restUrl;

    public SW360ProjectClient(String restUrl, RestTemplate template) {
        super(template);
        this.restUrl = restUrl;
    }

    @Override
    public String getEndpoint() {
        return restUrl + PROJECTS_ENDPOINT;
    }

    public List<SW360Project> searchByName(String name, HttpHeaders header) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(getEndpoint())
                    .queryParam(SW360Attributes.PROJECT_SEARCH_BY_NAME, name);

            ResponseEntity<SW360ProjectList> response = doRestGET(builder.build(false).toUriString(), header,
                    SW360ProjectList.class);

            checkRestStatus(response);
            return getSw360Projects(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.debug("Request to search for projects with the name {} failed with {}",
                    name, e.getMessage());
            return new ArrayList<>();
        }
    }

    public SW360Project createProject(SW360Project sw360Project, HttpHeaders header) {
        try {
            HttpEntity<String> httpEntity = RestUtils.convertSW360ResourceToHttpEntity(sw360Project, header);

            ResponseEntity<SW360Project> response = doRestPOST(getEndpoint(), httpEntity, SW360Project.class);

            checkRestStatus(response);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.debug("Request to create project {} failed with {}",
                    sw360Project.getName(), e.getStatusCode());
            LOGGER.debug("Error: ", e);
            return sw360Project;
        }
    }

    public void addReleasesToProject(String projectId, List<String> releases, HttpHeaders header) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(getEndpoint())
                    .pathSegment(projectId, SW360Attributes.PROJECT_RELEASES);

            HttpEntity<List<String>> httpEntity = new HttpEntity<>(releases, header);
            ResponseEntity<String> response = doRestCall(builder.build(false).toUriString(), HttpMethod.POST, httpEntity, String.class);
            checkRestStatus(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error("Request to add linked releases to project {} failed with {}",
                    projectId, e.getMessage());
        }
    }

    public List<SW360SparseRelease> getLinkedReleases(String projectId, boolean transitive, HttpHeaders header) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(getEndpoint())
                    .pathSegment(projectId, SW360Attributes.PROJECT_RELEASES)
                    .queryParam(SW360Attributes.PROJECT_RELEASES_TRANSITIVE, transitive);

            ResponseEntity<SW360ReleaseList> response = doRestGET(builder.build(false).toUriString(), header,
                    SW360ReleaseList.class);
            return getSw360SparseReleases(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error("Request to get linked releases of project with id=[{}] failed with {}",
                    projectId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
