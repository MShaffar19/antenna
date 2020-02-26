/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
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

import org.eclipse.sw360.antenna.sw360.rest.resource.licenses.SW360License;
import org.eclipse.sw360.antenna.sw360.rest.resource.licenses.SW360LicenseList;
import org.eclipse.sw360.antenna.sw360.rest.resource.licenses.SW360SparseLicense;
import org.eclipse.sw360.antenna.sw360.utils.SW360ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.eclipse.sw360.antenna.sw360.utils.SW360ClientUtils.checkRestStatus;
import static org.eclipse.sw360.antenna.sw360.utils.SW360ClientUtils.getSw360SparseLicenses;

public class SW360LicenseClient extends SW360Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(SW360LicenseClient.class);
    private static final String LICENSES_ENDPOINT = "/licenses";
    private final String restUrl;

    public SW360LicenseClient(String restUrl, RestTemplate template) {
        super(template);
        this.restUrl = restUrl;
    }

    @Override
    public String getEndpoint() {
        return restUrl + LICENSES_ENDPOINT;
    }

    public List<SW360SparseLicense> getLicenses(HttpHeaders header) {
        try {
            ResponseEntity<SW360LicenseList> response = doRestGET(getEndpoint(), header, SW360LicenseList.class);

            checkRestStatus(response);
            return getSw360SparseLicenses(response);
        } catch (SW360ClientException e) {
            LOGGER.debug("Request to get all licenses failed with {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<SW360License> getLicenseByName(String name, HttpHeaders header) {
        try {
            ResponseEntity<SW360License> response = doRestGET(getEndpoint() + "/" + name, header,
                    SW360License.class);

            checkRestStatus(response);
            return Optional.ofNullable(response.getBody());
        } catch (SW360ClientException e) {
            LOGGER.debug("Request to get license {} failed with {}",
                    name, e.getMessage());
            return Optional.empty();
        }
    }
}
