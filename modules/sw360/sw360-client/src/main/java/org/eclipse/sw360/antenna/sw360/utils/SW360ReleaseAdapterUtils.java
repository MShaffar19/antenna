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

import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360Release;

public class SW360ReleaseAdapterUtils {
    public static boolean isValidRelease(SW360Release release) {
        if (release.getName() == null || release.getName().isEmpty()) {
            return false;
        }
        return release.getVersion() != null && !release.getVersion().isEmpty();
    }
}
