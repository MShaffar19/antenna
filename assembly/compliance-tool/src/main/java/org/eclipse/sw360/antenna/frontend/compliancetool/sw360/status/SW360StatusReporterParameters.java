package org.eclipse.sw360.antenna.frontend.compliancetool.sw360.status;

import org.eclipse.sw360.antenna.frontend.compliancetool.main.AntennaComplianceToolOptions;
import org.eclipse.sw360.antenna.frontend.stub.cli.AbstractAntennaCLIOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Class responsible for parsing status reporter parameters
 * and creating the {@code InfoRequest} the status reporter
 * uses.
 */
class SW360StatusReporterParameters {
    private static final Logger LOGGER = LoggerFactory.getLogger(SW360StatusReporterParameters.class);

    /**
     * The long prefix to identify a parameter indication the desired info parameter of the reporter.
     */
    static final String REPORTER_PARAMETER_PREFIX = AbstractAntennaCLIOptions.SWITCH_PREFIX + "-info" + AbstractAntennaCLIOptions.PARAMETER_IDENTIFIER;

    /**
     * The long parameter prefix to determine the output format of the reporter
     */
    static final String OUTPUT_FORMAT_PREFIX_LONG = AbstractAntennaCLIOptions.SWITCH_PREFIX + "-output";

    /**
     * The short parameter prefix to determine the output format of the reporter
     */
    static final String OUTPUT_FORMAT_PREFIX_SHORT = AbstractAntennaCLIOptions.SWITCH_PREFIX + "o";

    /**
     * The default output format for the reporter output
     */
    private static final String DEFAULT_OUTPUT_FORMAT = "CSV";

    /**
     * Creates a string representing the info parameter of an{@link InfoRequest}
     * @param parameters set of parameters to be parsed
     * @return parsed {@code InfoRequest} with parsed additional parameters
     * @throws IllegalArgumentException when invalid or misconfigured parameters are given or are missing.
     */
    static String getInfoParameterFromParameters(Set<String> parameters) {
        if (parameters.isEmpty()) {
            LOGGER.error("No parameters provided for the status reporter.");
            LOGGER.info(helpMessage());
            throw new IllegalArgumentException("No parameters provided for the status reporter.");
        }
        if (parameters.stream().filter(p -> p.contains(REPORTER_PARAMETER_PREFIX)).count() != 1) {
            LOGGER.error("Too many information requests were made in this status report. ");
            LOGGER.info(helpMessage());
            throw new IllegalArgumentException("Too many information requests were made in this status report. ");
        }
        return parameters.stream().filter(p -> p.contains(REPORTER_PARAMETER_PREFIX)).findFirst().orElse("");
    }

    /**
     * Parses a parameter value from a parameter key value pair separated by {@code AbstractAntennaCLIOptions.PARAMETER_IDENTIFIER}
     * @param parameter String that contains a parameter key and a value plus identifiers.
     * @return String representing a parameter value
     *      * @throws IllegalArgumentException if more than one or no
     *      * {@code AbstractAntennaCLIOptions.PARAMETER_IDENTIFIER} is used.
     */
    private static String getParameterValueFromParameter(String parameter) {
        String[] parameterParts = parameter.split(AbstractAntennaCLIOptions.PARAMETER_IDENTIFIER);
        if (parameterParts.length != 2) {
            throw new IllegalArgumentException(
                    "The provided parameter " + parameter + " does not adhere to the structure --<parameter>=<value> and is therefore invalid.");
        }
        return parameterParts[1];
    }

    /**
     * Parses a parameter key from a parameter key value pair separated by {@code AbstractAntennaCLIOptions.PARAMETER_IDENTIFIER}
     * @param parameter String that contains a parameter key and a value plus identifiers.
     * @return String representing a parameter key
     * @throws IllegalArgumentException if more than one or no
     * {@code AbstractAntennaCLIOptions.PARAMETER_IDENTIFIER} is used.
     */
    private static String getParameterKeyFromParameter(String parameter) {
        String[] parameterParts = parameter.split(AbstractAntennaCLIOptions.PARAMETER_IDENTIFIER);
        if (parameterParts.length != 2) {
            throw new IllegalArgumentException(
                    "The provided parameter " + parameter + " does not adhere to the structure --<parameter>=<value> and is therefore invalid.");
        }
        return parameterParts[0];
    }

    /**
     * Parses the parameter value of a parameter key from a set of parameters.
     * @param parameters Set of parameters searched for parameter key
     * @param parameterKey parameter key of the parameter key value pair
     * @return parameter value of the parameter key if contained in parameter set, otherwise null
     */
    static String parseParameterValueFromListOfParameters(Set<String> parameters, String parameterKey) {
        return parameters.stream()
                .filter(p -> getParameterKeyFromParameter(p).equals(parameterKey))
                .findFirst()
                .map(SW360StatusReporterParameters::getParameterValueFromParameter)
                .orElse(null);
    }

    /**
     * Returns a help message that describes the parameter options supported
     * by the status reporter.
     *
     * @return the help message
     */
    static String helpMessage() {
        String cr = System.lineSeparator();
        return "Usage: java -jar compliancetool.jar " + AntennaComplianceToolOptions.SWITCH_REPORTER + "[options] <complianceMode> <propertiesFilePath>" + cr + cr +
                "The reporter create a csv file for every run with the requested information statement. " + cr + cr +
                "The reporter info statements: (only one can be set)" + cr +
                new IRGetClearedReleases().getInfoParameter() + ":   Gives a list of all releases in a given sw360 instances that are cleared." + cr +
                new IRGetNotClearedReleases().getInfoParameter() + ":   Gives a list of all releases in a given sw360 instances that are not cleared." + cr +
                new IRGetReleasesOfProjects().getInfoParameter() + ":   Gives a list of all releases of a given project in a given sw360 instances." + cr;
    }

    /**
     * Returns the parameter value of the output format key
     * @param parameters Set of parameters that are searched for the output format parameter
     * @return String of the output format parameter value
     */
    static String getOutputFormat(Set<String> parameters) {
        final String outputLong = parseParameterValueFromListOfParameters(parameters, OUTPUT_FORMAT_PREFIX_LONG);
        if (outputLong != null) {
            return outputLong;
        } else {
            return Optional.ofNullable(parseParameterValueFromListOfParameters(parameters, OUTPUT_FORMAT_PREFIX_SHORT))
                    .orElseGet(() -> {
                        LOGGER.warn("No output format is given.");
                        return DEFAULT_OUTPUT_FORMAT;
                    });
        }
    }
}
