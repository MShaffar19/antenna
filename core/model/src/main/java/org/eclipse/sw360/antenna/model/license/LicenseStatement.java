package org.eclipse.sw360.antenna.model.license;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LicenseStatement implements LicenseInformation {
    private List<LicenseInformation> licenses;
    private LicenseOperator op;

    public LicenseStatement() {
        this(new ArrayList<>(), null);
    }

    public LicenseStatement(List<LicenseInformation> licenses, LicenseOperator op) {
        this.licenses = licenses;
        this.op = op;
    }

    public void setLicenses(List<LicenseInformation> licenses) {
        this.licenses = licenses;
    }

    public LicenseOperator getOp() {
        return op;
    }

    public void setOp(LicenseOperator operator) {
        this.op = operator;
    }

    public boolean addLicenseInformation(LicenseInformation license) {
        return licenses.add(license);
    }

    @Override
    public String evaluate() {
        if (isEmpty()) {
            return "";
        }
        return "( " +
                licenses
                        .stream()
                        .map(LicenseInformation::evaluate)
                        .collect(Collectors.joining(" " + this.op.toString() + " "))
                + " )";
    }

    @Override
    public String evaluateLong() {
        if (isEmpty()) {
            return "";
        }
        return "( " +
                licenses
                    .stream()
                    .map(LicenseInformation::evaluateLong)
                    .collect(Collectors.joining(" " + this.op.toString() + " "))
                + " )";
    }

    @Override
    public boolean isEmpty() {
        return licenses == null || licenses.isEmpty() || licenses.stream().allMatch(LicenseInformation::isEmpty);
    }

    @Override
    public List<License> getLicenses() {
        if (licenses == null || this.isEmpty()) {
            licenses =  new ArrayList<>();
        }
        return licenses
                .stream()
                .map(LicenseInformation::getLicenses)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LicenseStatement that = (LicenseStatement) o;
        return Objects.equals(licenses, that.licenses) &&
                op == that.op;
    }

    @Override
    public int hashCode() {
        return Objects.hash(licenses, op);
    }

    @Override
    public String toString() {
        return evaluate();
    }
}
