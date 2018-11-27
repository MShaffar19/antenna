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
package org.eclipse.sw360.antenna.sw360.rest.resource.licenses;

import org.eclipse.sw360.antenna.sw360.rest.resource.Embedded;
import org.eclipse.sw360.antenna.sw360.rest.resource.LinkObjects;
import org.eclipse.sw360.antenna.sw360.rest.resource.SW360HalResource;
import org.eclipse.sw360.antenna.sw360.rest.resource.SW360HalResourceUtility;

public class SW360SparseLicense extends SW360HalResource<LinkObjects, Embedded> {
    private String fullName;

    public String getFullName() {
        return this.fullName;
    }

    public SW360SparseLicense setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getShortName() {
        return SW360HalResourceUtility.getLastIndexOfLinkObject(get_Links()).orElse("");
    }
}
