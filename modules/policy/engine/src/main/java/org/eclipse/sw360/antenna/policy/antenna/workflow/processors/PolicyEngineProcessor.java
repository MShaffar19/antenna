/*
 * Copyright (c) Bosch Software Innovations GmbH 2019.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.policy.antenna.workflow.processors;

import org.eclipse.sw360.antenna.api.IEvaluationResult;
import org.eclipse.sw360.antenna.api.IPolicyEvaluation;
import org.eclipse.sw360.antenna.api.exceptions.AntennaConfigurationException;
import org.eclipse.sw360.antenna.api.workflow.WorkflowStepResult;
import org.eclipse.sw360.antenna.model.artifact.Artifact;
import org.eclipse.sw360.antenna.policy.engine.*;
import org.eclipse.sw360.antenna.workflow.stubs.AbstractComplianceChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The workspace step which integrates the {@link PolicyEngine} into the Antenna workflow engine
 */
public class PolicyEngineProcessor extends AbstractComplianceChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyEngineConfigurator.class);

    private PolicyEngine policyEngine;

    private static final String RULESETPROP = "ruleset.classes";

    public PolicyEngineProcessor() {
        this.workflowStepOrder = VALIDATOR_BASE_ORDER + 11000;
    }

    @Override
    public void configure(final Map<String, String> configMap) throws AntennaConfigurationException {
        super.configure(configMap);

        LOGGER.info("Setting up policy engine");

        final String ruleSetClasses = getConfigValue(RULESETPROP, configMap).trim();
        final Collection<String> ruleSetClassesList = Arrays.asList(ruleSetClasses.split(","))
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());

        LOGGER.debug("Configured rule set classes: " + ruleSetClassesList.toString());

        try {
            policyEngine = PolicyEngineConfigurator.configure(ruleSetClassesList);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new AntennaConfigurationException("Could not initialize Policy Engine!", e);
        }
    }

    @Override
    public WorkflowStepResult postProcessResult(final WorkflowStepResult result) {
        WorkflowStepResult pResult = super.postProcessResult(result);
        pResult.addAdditionalReportComment("Evaluated with " + getRulesetDescription());
        return pResult;
    }

    @Override
    public IPolicyEvaluation evaluate(final Collection<Artifact> artifacts) {
        LOGGER.info("Policy Engine execution started");

        final Collection<ThirdPartyArtifact> polengArtifacts = artifacts
                .stream()
                .map(AntennaArtifact::new)
                .collect(Collectors.toList());

        final Collection<ViolationIssue> results = policyEngine.evaluate(polengArtifacts);

        return new PolicyEvaluation(results);
    }

    @Override
    public String getRulesetDescription() {
        StringBuilder resultString = new StringBuilder("Policy Engine, with rule sets:\n");
        policyEngine.getRuleSets().forEach(rs -> resultString.append(prepareRuleSetInfo(rs)));
        return resultString.toString();
    }

    private String prepareRuleSetInfo(RuleSet ruleSet) {
        return String.format(" - Rule set %s in version %s", ruleSet.name(), ruleSet.version());
    }
}

/**
 * Implementation for the Antenna {@link IPolicyEvaluation} result based on the {@link PolicyEngine} data
 */
class PolicyEvaluation implements IPolicyEvaluation {
    private final Set<IEvaluationResult> evalResults;

    PolicyEvaluation(final Collection<ViolationIssue> data) {
        evalResults = data.stream().map(AntennaEvaluationResult::new).collect(Collectors.toSet());
    }

    @Override
    public Set<IEvaluationResult> getEvaluationResults() {
        return evalResults;
    }
}

/**
 * Implementation for the Antenna {@link IEvaluationResult} result based on the {@link PolicyEngine} result data
 */
class AntennaEvaluationResult implements IEvaluationResult {
    private final ViolationIssue violation;

    AntennaEvaluationResult(final ViolationIssue violation) {
        this.violation = violation;
    }

    @Override
    public String getId() {
        return violation.getId();
    }

    @Override
    public String getDescription() {
        return violation.getDescription();
    }

    @Override
    public Severity getSeverity() {
        switch (violation.getSeverity()) {
            case INFO:
                return Severity.INFO;
            case WARN:
                return Severity.WARN;
            case ERROR:
                return Severity.FAIL;
            default:
                throw new IllegalStateException("Programming Error: No other case possible in enum");
        }
    }

    @Override
    public Set<Artifact> getFailedArtifacts() {
        return violation.getFailedArtifacts()
                         .stream()
                         .map(element -> ((AntennaArtifact)element).getArtifact())
                         .collect(Collectors.toSet());
    }
}