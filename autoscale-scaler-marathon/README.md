# autoscale-scaler-marathon

---

 This is an implementation of a `ServiceScaler` that uses the Marathon REST API
 to scale up and down services.


## Configuration

 The configuration source for this module is [MarathonAutoscaleConfiguration](https://github.hpe.com/caf/autoscale-container/blob/develop/example-configs/cfg_autoscaler_marathon_MarathonAutoscaleConfiguration).
 The following configuration options are present:

 - endpoint: the fully qualified URL to the Marathon endpoint including port.
  Must not be null and must not be empty.
 - maximumInstances: the absolute upper ceiling for number of instances of a
  service. Minimum 1.


## Usage

 This module mostly relies on performing REST API calls to Marathon and
 retrieving applications by named reference (Marathon ID). This module has a
 working health check to ping the Marathon REST endpoint.


## Failure modes

 The following scenarios will prevent the module from initialising:

 - The marathon endpoint was not set or not a valid URL

 The following scenarios have been identified as possible runtime failure modes
 for this module:

 - Network failures to the Marathon endpoint


## Maintainers

 The following people are contacts for developing and maintaining this module:

 - Richard Hickman (Cambridge, UK, richard.hickman@hp.com)
