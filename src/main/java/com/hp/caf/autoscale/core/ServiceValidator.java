package com.hp.caf.autoscale.core;


import com.hp.caf.api.autoscale.ScalingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


/**
 * A ServiceValidator is designed for use with the autoscaler, and will verify ScalingConfiguration
 * objects are relevant and valid to pass to an autoscaler instance.
 */
public class ServiceValidator
{
    private final Collection<String> metricNames;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private static final Logger LOG = LoggerFactory.getLogger(ServiceValidator.class);


    /**
     * Create a ServiceValidator.
     * @param metricNames the valid names of metrics supported by the autoscaler
     */
    public ServiceValidator(final Collection<String> metricNames)
    {
        this.metricNames = Objects.requireNonNull(metricNames);
    }


    /**
     * Given a set of ScalingConfiguration objects, return the valid and applicable ones.
     * To be valid, a service must have the same application, and must pass all the
     * validation annotations present in ScalingConfiguration.
     * @param input a set of ScalingConfiguration objects, generally from a ServiceSource
     * @return the validated and applicable ScalingConfiguration objects
     */
    public Set<ScalingConfiguration> getValidatedServices(final Set<ScalingConfiguration> input)
    {
        Set<ScalingConfiguration> ret = new HashSet<>();
        for ( ScalingConfiguration s : input ) {
            LOG.debug("Source reported service {}", s);
            if ( validateMetric(s) ) {
                Set<ConstraintViolation<ScalingConfiguration>> violations = validator.validate(s);
                if ( violations.isEmpty() ) {
                    LOG.debug("Service {} valid", s);
                    ret.add(s);
                } else {
                    LOG.warn("Service {} invalid: {}", s.getId(), violations);
                }
            }
        }
        return ret;
    }


    private boolean validateMetric(final ScalingConfiguration s)
    {
        boolean ret = false;
        if ( metricNames.contains(s.getWorkloadMetric()) ) {
            ret = true;
        } else {
            LOG.warn("Service {} invalid: requested unknown workload metric {}", s.getId(), s.getWorkloadMetric());
        }
        return ret;
    }

}
