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

package org.eclipse.sw360.antenna.workflow.generators;

import org.eclipse.sw360.antenna.api.IAttachable;
import org.eclipse.sw360.antenna.api.IProject;
import org.eclipse.sw360.antenna.api.exceptions.AntennaConfigurationException;
import org.eclipse.sw360.antenna.api.exceptions.AntennaException;
import org.eclipse.sw360.antenna.api.workflow.AbstractGenerator;
import org.eclipse.sw360.antenna.model.artifact.Artifact;
import org.eclipse.sw360.antenna.sw360.SW360MetaDataUpdater;
import org.eclipse.sw360.antenna.sw360.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360Release;

import java.io.IOException;
import java.util.*;

public class SW360Updater extends AbstractGenerator {
    private static final String REST_SERVER_URL_KEY = "rest.server.url";
    private static final String AUTH_SERVER_URL_KEY = "auth.server.url";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";

    private String sw360RestServerUrl;
    private String sw360AuthServerUrl;
    private String sw360User;
    private String sw360Password;
    private IProject project;

    @Override
    public void configure(Map<String, String> configMap) throws AntennaConfigurationException {
        project = context.getProject();
        sw360RestServerUrl = getConfigValue(REST_SERVER_URL_KEY, configMap);
        sw360AuthServerUrl = getConfigValue(AUTH_SERVER_URL_KEY, configMap);
        sw360User = getConfigValue(USERNAME_KEY, configMap);
        sw360Password = getConfigValue(PASSWORD_KEY, configMap);
    }

    @Override
    public Map<String, IAttachable> produce(Collection<Artifact> intermediates) throws AntennaException {
        SW360MetaDataUpdater sw360MetaDataUpdater = new SW360MetaDataUpdater(sw360RestServerUrl, sw360AuthServerUrl, sw360User, sw360Password);

        try {
            List<SW360Release> releases = new ArrayList<>();
            for (Artifact artifact : intermediates) {
                Set<String> licenses = sw360MetaDataUpdater.getOrCreateLicenses(artifact);
                SW360Component component = sw360MetaDataUpdater.getOrCreateComponent(artifact);
                releases.add(sw360MetaDataUpdater.getOrCreateRelease(artifact, licenses, component));
            }
            sw360MetaDataUpdater.createProject(project, releases);
        } catch (IOException e) {
            throw new AntennaException("Problem occurred during updating SW360.", e);
        }
        return Collections.emptyMap();
    }
}
