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
package org.eclipse.sw360.antenna.sw360.utils;

import org.eclipse.sw360.antenna.model.artifact.Artifact;
import org.eclipse.sw360.antenna.model.artifact.facts.ArtifactSourceFile;

import java.nio.file.Path;
import java.util.Optional;

public class ArtifactToAttachmentUtils {

    private ArtifactToAttachmentUtils() {}

    public static Optional<Path> getSourceAttachmentFromArtifact(Artifact artifact) {
        return artifact.askForGet(ArtifactSourceFile.class);
    }

}
