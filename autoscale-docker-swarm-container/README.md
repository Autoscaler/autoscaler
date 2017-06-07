# Autoscaler Docker Swarm Service Deployment

This repository consists of the source to build a pre-defined CAF approved
container that includes the CAF autoscale application along with
standard modules required to scale a service based on rabbitmq metrics.

To get the autoscale application up and running look at the [Quick Start](#quick-start) section below.


### Container deployment

### Configuration

Configuration of the AutoScaler is supported through the following environment variables:

 - `DOCKER_HOST`  
    Default: `unix:///var/run/docker.sock`  
    Used to specify the Docker Swarm REST endpoint.  Supports unix sockets and tcp type connections e.g. http://machine:2375.

 - `CAF_RABBITMQ_MGMT_URL`  
    Default: `http://rabbitmq:15678`  
     Used to specify the RabbitMQ Management API Endpoint.  Alternatively `CAF_RABBITMQ_HOST` and `CAF_RABBITMQ_MGMT_PORT` may instead be specified individually.

 - `CAF_RABBITMQ_MGMT_USERNAME`  
    Default: `guest`  
    Used to specify the username used to connect to RabbitMQ.  If `CAF_RABBITMQ_MGMT_USERNAME` is not specified then `CAF_RABBITMQ_USERNAME` will also be checked before falling back to the default.

 - `CAF_RABBITMQ_MGMT_PASSWORD`  
    Default: `guest`  
    Used to specify the password used to connect to RabbitMQ.  If `CAF_RABBITMQ_MGMT_PASSWORD` is not specified then `CAF_RABBITMQ_PASSWORD` will also be checked before falling back to the default.

 - `CAF_AUTOSCALER_DOCKER_SWARM_STACK`  
    Required property used to specify the docker stack(s) which the autoscaler is monitoring.  Only applications deployed directly or indirectly within this group are auto-scaled.
    To specify multiple stacks use a comma delimted list e.g. "JobService,DataProcessing".

 - `CAF_AUTOSCALER_SCALING_DELAY`  
    Minimum: `1`  
    Default: `10`  
    Used to specify the number of queue statistics records that must be acquired sequentially before performing analysis upon them and issuing a scaling recommendation. This means the period in time between making scaling recommendations is the service interval multiplied by the scalingDelay. This number should be small enough to respond to varying load but big enough to avoid erratic behaviour.

 - `CAF_AUTOSCALER_BACKLOG_GOAL`  
    Default: `300`  
    Used to specify the amount of time, in seconds, that we ideally want to complete the current backlog of messages in. This is effectively a quality of service parameter, where lower will trigger more aggressive scaling.

 - `CAF_AUTOSCALER_MAXIMUM_INSTANCES`  
    Default: `100`  
    Used to specify the maximum number of instances that any worker can be scaled to.

 - `CAF_DOCKER_SWARM_TIMEOUT`  
    Default: `30`  
    Used to specify the max length of time in seconds that a docker REST call can take before a timeout out occurs.  

 -  `CAF_DOCKER_SWARM_HEALTHCHECK_TIMEOUT`  
    Default: `5`  
    Used to specify the max length of time in seconds that the Docker endpoint healthcheck can take before a timeout occurs.

 - `HTTP_PROXY`  
    (Optional)
    Used to specify an HTTP based proxy, for the Docker REST endpoint communication. 

### Health checks

The [autoscale-core](https://github.com/Autoscaler/autoscaler/tree/develop/autoscale-core) application inherently exposes standard and
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
being run on Docker Swarm, this will ensure the container is restarted if it
crashes. It is recommended you do not attempt to monitor and scale a very
large number of applications with a single instance. While this should work
with an arbitrary number of target services, the monitoring and scaling
actions may begin to lag behind the requested period once the container goes
beyond its normal capability.


### Resource requirements

The container uses very little memory and CPU, and effectively no disk load.
It does, however, generate some network traffic through monitoring and scaling
of services. Docker Swarm will be the target of most of the network traffic, though
the RabbitMQ management host will also be polled. The frequency of the traffic
depends upon the number of services being monitored and the service template
requested frequency. Each individual network call should be no more than a
handful of kilobytes, though the 15-minute scheduled refresh of all Docker swarm
services may be more if Docker Swarm is running a large number of services.
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

For more information on deployment against Docker Swarm refer to [autoscale-deploy](https://github.com/Autoscaler/autoscaler-deploy/)

Assuming Docker Swarm and RabbitMQ are already deployed, take the sample
deployment files present in the autoscale-deploy repository [docker-compose.yml](https://github.com/Autoscaler/autoscaler-deploy/tree/develop/docker-compose.yml). 

Change the endpoints to match your deployments. Make sure the StackId property in the compose file is in the same stack id/name as the services that it should scale. Put the deployment files on your swarm node. 

For each of the services to scale its docker-compose template must be updated to
include labels such as the following:

```
   labels:
      - autoscale.metric=rabbitmq
      - autoscale.scalingtarget=globfilter-in
      - autoscale.scalingprofile=default
      - autoscale.maxinstances=4
      - autoscale.mininstances=1
      - autoscale.interval=30
      - autoscale.backoff=10
    
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

Deploy/redeploy the services and the autoscale container. After one or two
minutes the autoscale container should find the services and start monitoring.
