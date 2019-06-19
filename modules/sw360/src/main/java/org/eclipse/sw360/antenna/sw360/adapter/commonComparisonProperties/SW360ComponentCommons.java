/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.adapter.commonComparisonProperties;

import org.eclipse.sw360.antenna.sw360.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360SparseRelease;

import java.util.ArrayList;
import java.util.List;

public class SW360ComponentCommons implements ICommonComparisonProperties {
    private final SW360Component component;

    public SW360ComponentCommons(SW360Component component) {
        this.component = component;
    }

    public String getName() {
        return component.getName();
    }

    public List<String> getVersions() {
        List<String> versions = new ArrayList<>();
        List<SW360SparseRelease> releases = new ArrayList<>();

        if (component != null &&
                component.get_Embedded() != null &&
                component.get_Embedded().getReleases() != null) {
            releases.addAll(component.get_Embedded().getReleases());
        }
        for (SW360SparseRelease release : releases) {
            versions.add(release.getVersion());
        }
        return versions;
    }
}
