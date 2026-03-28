package io.github.kibruh;

import javax.management.ObjectName;

record CheckerConfig(
    JmxClient jmxClient,
    String jmxUrl,
    String objectName,
    String attribute,
    String path,
    String statefile,
    int meanRateInterval,
    int minRateInterval,
    int rateWindowMultiplier,
    Threshold warningThreshold,
    Threshold criticalThreshold,
    String warningRegex,
    String criticalRegex,
    boolean warningNegated,
    boolean criticalNegated,
    String uom,
    double divisor
) {
    public ObjectName objectNameAsObjectName() {
        try {
            return new ObjectName(objectName);
        } catch (javax.management.MalformedObjectNameException e) {
            throw new IllegalArgumentException("Invalid object name: " + objectName, e);
        }
    }
}
