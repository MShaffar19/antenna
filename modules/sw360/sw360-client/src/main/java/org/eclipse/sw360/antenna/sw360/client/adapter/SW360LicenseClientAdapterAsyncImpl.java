/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.eclipse.sw360.antenna.sw360.client.rest.SW360LicenseClient;
import org.eclipse.sw360.antenna.sw360.rest.resource.licenses.SW360License;
import org.eclipse.sw360.antenna.sw360.rest.resource.licenses.SW360SparseLicense;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils.optionalFuture;

/**
 * Adapter implementation for the SW360 licenses endpoint.
 */
class SW360LicenseClientAdapterAsyncImpl implements SW360LicenseClientAdapterAsync {
    private final SW360LicenseClient licenseClient;

    public SW360LicenseClientAdapterAsyncImpl(SW360LicenseClient client) {
        licenseClient = client;
    }

    @Override
    public SW360LicenseClient getLicenseClient() {
        return licenseClient;
    }

    @Override
    public CompletableFuture<Boolean> isLicenseOfArtifactAvailable(String license) {
        return getLicenseClient().getLicenses()
                .thenApply(sw360Licenses -> sw360Licenses.stream()
                        .map(SW360SparseLicense::getShortName)
                        .anyMatch(n -> n.equals(license)));
    }

    @Override
    public CompletableFuture<Optional<SW360License>> getSW360LicenseByAntennaLicense(String license) {
        return optionalFuture(getLicenseClient().getLicenseByName(license));
    }

    @Override
    public CompletableFuture<Optional<SW360License>> getLicenseDetails(SW360SparseLicense sparseLicense) {
        return optionalFuture(getLicenseClient().getLicenseByName(sparseLicense.getShortName()));
    }
}
