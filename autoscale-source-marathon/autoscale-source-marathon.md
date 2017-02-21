# autoscale-scaler-marathon

---

 This is an implementation of a `ServiceSource` that uses the Marathon REST API
 to acquire a list of scalable services.


## Configuration

 The configuration source for this module is `MarathonAutoscaleConfiguration`.
 The following configuration options are present:

 - endpoint: the fully qualified URL to the Marathon endpoint including port.
  Must not be null and must not be empty.
 - maximumInstances: the absolute upper ceiling for number of instances of a
  service. Minimum 1.


## Usage

 This module mostly relies on performing REST API calls to Marathon. It will
 only ever return Marathon tasks from the same group the autoscaler is in. It
 expects services to request their scaling configuration via the use of labels
 within the Marathon template. The following labels are used:

 - autoscale.metric: the key/name of the `WorkloadAnalyser` to use for
  computing workload and issuing scaling recommendations. This must be set and
  be a valid and available analyser for an autoscale application to pick up
  this service
 - autoscale.scalingtarget: the scaling target key for this service. For queue
  based services, this is typically the queue name to monitor
 - autoscale.scalingprofile: the name of the profile to use for scaling, if the
  `WorkloadAnalyser` supports profiles
 - autoscale.maxinstances: maximum number of instances. This must be set and be
  a positive integer
 - autoscale.mininstances: minimum number of instances. This must be set and be
  at least 0

 This module has a working health check to ping the Marathon REST endpoint.


## Failure modes

 The following scenarios will prevent the module from initialising:

 - The marathon endpoint was not set or not a valid URL

 The following scenarios have been identified as possible runtime failure modes
 for this module:

 - Network failures to the Marathon endpoint


## Maintainers

 The following people are contacts for developing and maintaining this module:

 - Richard Hickman (Cambridge, UK, richard.hickman@hp.com)
