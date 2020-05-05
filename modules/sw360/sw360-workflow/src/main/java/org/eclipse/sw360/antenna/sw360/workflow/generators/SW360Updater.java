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

package org.eclipse.sw360.antenna.sw360.workflow.generators;

import org.eclipse.sw360.antenna.api.IAttachable;
import org.eclipse.sw360.antenna.api.workflow.AbstractGenerator;
import org.eclipse.sw360.antenna.model.artifact.Artifact;
import org.eclipse.sw360.antenna.sw360.SW360MetaDataUpdater;
import org.eclipse.sw360.antenna.sw360.client.adapter.SW360Connection;
import org.eclipse.sw360.antenna.sw360.workflow.SW360ConnectionConfigurationFactory;

import java.util.Collection;
import java.util.Map;

public class SW360Updater extends AbstractGenerator {
    private static final String UPDATE_RELEASES = "update_releases";
    private static final String UPLOAD_SOURCES = "upload_sources";

    private final SW360ConnectionConfigurationFactory connectionFactory;

    private String projectName;
    private String projectVersion;
    private SW360MetaDataUpdater sw360MetaDataUpdater;
    private SW360UpdaterImpl updaterImpl;

    public SW360Updater() {
        this(new SW360ConnectionConfigurationFactory());
    }

    /**
     * Creates a new instance of {@code SW360Updater} and sets the factory for
     * creating the SW360 connection. This constructor is used for testing
     * purposes.
     *
     * @param factory the factory for the SW360 connection
     */
    SW360Updater(SW360ConnectionConfigurationFactory factory) {
        connectionFactory = factory;
        this.workflowStepOrder = 1100;
    }

    @Override
    public void configure(Map<String, String> configMap) {
        projectName = getProjectName();
        projectVersion = getProjectVersion();

        SW360Connection sw360Connection =
                getConnectionFactory().createConnection(key -> getConfigValue(key, configMap),
                        context.getHttpClient(), context.getObjectMapper());

        // General configuration
        final boolean updateReleases = getBooleanConfigValue(UPDATE_RELEASES, configMap);
        Boolean uploadSources = getBooleanConfigValue(UPLOAD_SOURCES, configMap);

        sw360MetaDataUpdater = new SW360MetaDataUpdater(sw360Connection, updateReleases, uploadSources);
    }

    private String getProjectVersion() {
        return context.getToolConfiguration().getVersion() != null ?
                context.getToolConfiguration().getVersion() :
                context.getProject().getVersion();
    }

    private String getProjectName() {
        return (context.getToolConfiguration().getProductFullName() != null) ?
                context.getToolConfiguration().getProductFullName() :
                context.getProject().getProjectId();
    }

    @Override
    public Map<String, IAttachable> produce(Collection<Artifact> intermediates) {
        return updaterImpl == null ?
                new SW360UpdaterImpl(sw360MetaDataUpdater,projectName, projectVersion)
                        .produce(intermediates) :
                updaterImpl.produce(intermediates);
    }

    public void setUpdaterImpl(SW360UpdaterImpl updaterImpl) {
        this.updaterImpl = updaterImpl;
    }

    SW360ConnectionConfigurationFactory getConnectionFactory() {
        return connectionFactory;
    }
}
