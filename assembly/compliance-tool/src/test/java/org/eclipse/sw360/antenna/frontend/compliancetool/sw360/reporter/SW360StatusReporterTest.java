/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.frontend.compliancetool.sw360.reporter;

import org.eclipse.sw360.antenna.frontend.compliancetool.sw360.SW360Configuration;
import org.eclipse.sw360.antenna.frontend.stub.cli.AbstractAntennaCLIOptions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SW360StatusReporterTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private SW360Configuration configurationMock = mock(SW360Configuration.class);

    private Set<String> parameter;

    @Test(expected = NullPointerException.class)
    public void testConfigurationMustNotBeNull() {
        new SW360StatusReporter(null, parameter);
    }

    @Test(expected = NullPointerException.class)
    public void testParameterMustNotBeNull() {
        new SW360StatusReporter(configurationMock, null);
    }

    @Test
    public void testStatusReporter() throws IOException {
        File csvFile = folder.newFile("result.csv");
        when(configurationMock.getCsvFilePath())
                .thenReturn(csvFile.toPath());

        parameter = new HashSet<>(Collections.singletonList(ReporterParameterParser.REPORTER_PARAMETER_PREFIX + AbstractAntennaCLIOptions.PARAMETER_IDENTIFIER + new IRGetClearedReleases().getInfoParameter()));
        SW360StatusReporter statusReporter = new SW360StatusReporter(configurationMock, parameter);

        statusReporter.setInfoRequest(InfoRequest.emptyInfoRequest());

        statusReporter.execute();
        assertThat(csvFile).exists();
    }

    @Test(expected = IllegalStateException.class)
    public void testStatusReporterWithMissingParameters() {
        parameter = new HashSet<>(Collections.singletonList(ReporterParameterParser.REPORTER_PARAMETER_PREFIX + AbstractAntennaCLIOptions.PARAMETER_IDENTIFIER + new IRGetReleasesOfProjects().getInfoParameter()));
        SW360StatusReporter statusReporter = new SW360StatusReporter(configurationMock, parameter);

        statusReporter.setInfoRequest(InfoRequest.emptyInfoRequest());

        statusReporter.execute();

        fail("Should have failed due to missing parameters");
    }
}
