# autoscale-container

This repository consists of the source to build a pre-defined CAF approved
container that includes the CAF autoscale application along with
standard modules. Sample configuration is also supplied in the code base.

To get the autoscale application up and running look at the [Quick Start](#quick-start) section below.


### Container deployment

Since this container uses [config-file](https://github.com/CAFapi/caf-common/tree/develop/config-file), the appropriate configuration files
should either be mounted as a volume or injected into the sandbox via the use
of Marathon URI template configuration. The following configuration resources
are *required*:

- [MarathonAutoscaleConfiguration](https://github.hpe.com/caf/autoscale-container/blob/develop/example-configs/cfg_autoscaler_marathon_MarathonAutoscaleConfiguration)
- [RabbitWorkloadAnalyserConfiguration](https://github.hpe.com/caf/autoscale-container/blob/develop/example-configs/cfg_autoscaler_marathon_RabbitWorkloadAnalyserConfiguration)

See the documentation for the Marathon autoscaler components and [autoscale-workload-rabbit](https://github.hpe.com/caf/autoscale-workload-rabbit) for details about these specific configuration
resources.


### Health checks

The [autoscale-core](https://github.hpe.com/caf/autoscale-core) application inherently exposes standard and
module-specific health checks. If you expose the admin port (default 8081)
then this can be accessed via HTTP to examine metrics and health checks.
The health check REST call will return HTTP 500 if any health check fails.
For details on the health checks for specific components, examine the module
documentation.


### Logging

Currently the logging is accessible via the Marathon sandbox until a CAF
approved logging mechanism is agreed upon. Debug logging is enabled by default
in this container.


### Scaling

This container is not election-enabled. This means there should only be one
instance per set of services the container is scaling. If the container is
being run on Marathon, this will ensure the container is restarted if it
crashes. It is recommended you do not attempt to monitor and scale a very
large number of applications with a single instance. While this should work
with an arbitrary number of target services, the monitoring and scaling
actions may begin to lag behind the requested period once the container goes
beyond its normal capability.


### Resource requirements

The container uses very little memory and CPU, and effectively no disk load.
It does, however, generate some network traffic through monitoring and scaling
of services. Marathon will be the target of most of the network traffic, though
the RabbitMQ management host will also be polled. The frequency of the traffic
depends upon the number of services being monitored and the service template
requested frequency. Each individual network call should be no more than a
handful of kilobytes, though the 15-minute scheduled refresh of all Marathon
services may be more if Marathon is running a large number of services.
Recommended values:

- 128-196MB of RAM
- 0.1 CPUs per 10 services monitored
- Slow I/O is acceptable
- Preferred to be network-local to Marathon and RabbitMQ


### Failure modes

Since this container is a composite of other CAF components, see the failure
modes in the documentation for each module for further information on this
topic.


### Upgrade procedures

Since there is a single instance per scaling group, simply power off the old
container and start the new one. There may be a delay or a minute or two before
scaling starts again. Different scaling groups may have different container
versions.


### Quick start

Assuming Marathon and RabbitMQ are already deployed, take the sample
configuration files present in [example-configs](https://github.hpe.com/caf/autoscale-container/tree/develop/example-configs) and change the endpoints to
match your deployments. Create a Marathon template to deploy making sure the
`id` of this autoscale template is in the same subgroup as the services that
it should scale. Put the configuration files on your configuration server and
then add URIs to the template for all the configuration resources specified
earlier in the documentation. Expose port 8081 using bridge networking for
health checks.

For each of the services to scale its Marathon template must be updated to
include labels such as the following:

```
    "labels": {
        "autoscale.metric": "rabbitmq",
        "autoscale.interval": "10",
        "autoscale.scalingtarget": "testWorkerQueue",
        "autoscale.mininstances": "0",
        "autoscale.maxinstances": "5",
        "autoscale.profile": "default",
        "autoscale.backoff": "1"
    }
```

The `autoscale.metric` should always be `rabbitmq` for this container.
Set the `autoscale.interval` to something sensible given your service workload.
If you set this to a very small value (very frequent) then you will get very
unstable scaling - the number of instances will rapidly increase and decrease
seemingly with no cause. If you set this very large (infrequent) then the
scaling will lag behind the demand significantly. Thus, containers which
rapidly consume messages can have a lower interval. For containers that
consume messages very slowly (whereby the average is tens of minutes or hours)
this algorithm may not be suitable for your workloads and you may wish to
consider writing a `workload` module more suited to your needs and make a
container with it in.

The `autoscale.scalingtarget` should be the RabbitMQ queue name to monitor.
Set the minimum and maximum instances as your quality of service and resources
dictate. The `autoscale.backoff` is the number of intervals to skip monitoring
after a scale up or down command is issued. This prevents unusual values
being considered when the system is in an unstable state.

Finally the `autoscale.profile` can be an arbitrary string, but one that should exist in the [RabbitWorkloadAnalyserConfiguration](https://github.hpe.com/caf/autoscale-container/blob/develop/example-configs/cfg_autoscaler_marathon_RabbitWorkloadAnalyserConfiguration) resource deployed inside the autoscale container.

Deploy/redeploy the services and the autoscale container. After one or two
minutes the autoscale container should find the services and start monitoring.
