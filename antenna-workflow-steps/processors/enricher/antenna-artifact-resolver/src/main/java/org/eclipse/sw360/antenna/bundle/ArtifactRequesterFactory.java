/*
 * Copyright (c) Bosch Software Innovations GmbH 2016-2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.bundle;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.sw360.antenna.api.configuration.AntennaContext;
import org.eclipse.sw360.antenna.api.configuration.ToolConfiguration;
import org.eclipse.sw360.antenna.api.exceptions.AntennaException;

import java.net.URL;
import java.util.Optional;

/**
 * Returns classes for requesting the jars of Artifacts.
 */
public class ArtifactRequesterFactory {
    public static IArtifactRequester getArtifactRequester(AntennaContext context, URL sourcesRepositoryUrl) throws AntennaException {
        ToolConfiguration toolConfig = context.getToolConfiguration();

        if (toolConfig.isMavenInstalled()) {
            return useMavenIfRunning(context, Optional.of(sourcesRepositoryUrl)).orElse(new MavenInvokerRequester(context, sourcesRepositoryUrl));
        }
        return new HttpRequester(context, sourcesRepositoryUrl);
    }

    public static IArtifactRequester getArtifactRequester(AntennaContext context) throws AntennaException {
        ToolConfiguration toolConfig = context.getToolConfiguration();

        if (toolConfig.isMavenInstalled()) {
            return useMavenIfRunning(context, Optional.empty()).orElse(new MavenInvokerRequester(context));
        }
        return new HttpRequester(context);
    }

    /*
     * Must only be used if Maven installation can be found on system, will result in ClassNotFoundError otherwise
     */
    private static Optional<IArtifactRequester> useMavenIfRunning(AntennaContext context, Optional<URL> sourcesRepositoryUrl) {
        Optional<RepositorySystem> optionalRepositorySystem = context.getGeneric(RepositorySystem.class);
        Optional<MavenProject> optionalMavenProject = context.getGeneric(MavenProject.class);
        Optional<LegacySupport> optionalLegacySupport = context.getGeneric(LegacySupport.class);
        if (optionalRepositorySystem.isPresent() &&
                optionalMavenProject.isPresent() &&
                optionalLegacySupport.isPresent()) {
            ArtifactRepository localRepository = optionalLegacySupport.get().getSession().getLocalRepository();
            if (localRepository != null) {
                return Optional.of(new MavenRuntimeRequester(context, optionalRepositorySystem.get(), localRepository, optionalMavenProject.get().getRemoteArtifactRepositories(), sourcesRepositoryUrl));
            }
        }
        return Optional.empty();
    }
}
