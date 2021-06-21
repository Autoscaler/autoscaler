# Autoscaler Kubernetes Module

This repository consists of the source to build a pre-defined CAF approved
container that includes the CAF autoscale application. Sample configuration is also supplied in the code base.

To get the kubernetes autoscaler module up and running look at the [Quick Start](#quick-start) section below.

### Installing the Autoscaler.
The autoscaler can be installed to Kubernetes by using the kubectl command line tool.  
 - `kubectl apply -f ./autoscaler.yaml`

### Configuration

Configuration of the AutoScaler is supported through the following environment variables:
 - `CAF_RABBITMQ_MGMT_URL`  
    Default: `http://rabbitmq:15672`  
    Used to specify the RabbitMQ Management API Endpoint.  Alternatively `CAF_RABBITMQ_HOST` and `CAF_RABBITMQ_MGMT_PORT` may instead be specified individually.

 - `CAF_RABBITMQ_MGMT_USERNAME`  
    Default: `guest`  
    Used to specify the username used to connect to RabbitMQ.  If `CAF_RABBITMQ_MGMT_USERNAME` is not specified then `CAF_RABBITMQ_USERNAME` will also be checked before falling back to the default.

 - `CAF_RABBITMQ_MGMT_PASSWORD`  
    Default: `guest`  
    Used to specify the password used to connect to RabbitMQ.  If `CAF_RABBITMQ_MGMT_PASSWORD` is not specified then `CAF_RABBITMQ_PASSWORD` will also be checked before falling back to the default.  

 - `CAF_AUTOSCALER_RABBITMQ_MEMORY_QUERY_FREQ`  
    Default: `10`  
    Number of whole seconds that the service should wait between sending RabbitMQ memory status requests.

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

 - `CAF_AUTOSCALER_METRIC`  
    Default: `rabbitmq`  

 - `CAF_AUTOSCALER_KUBERNETES_NAMESPACES`
    Default: `default`
    Used to specify the Kubernetes namespaces, comma separated, to search for deployments.
 
 - `CAF_AUTOSCALER_RESOURCE_ID_SEPARATOR`
    Default: `:`
    Description: The separator used when storing namespace/deployment source configuration. 
  
 - `CAF_LOG_LEVEL`  
    Default: `INFO`  
    Used to specify the required level of logging.

## Messaging platform based back off configuration

This functionality will only work on services that have the label `autoscale.shutdownPriority` set in their app definitions. These labels are then used to determine which application should be shutdown when the messaging platform begins to run low on resources.

`autoscale.shutdownPriority` should be set to positive integer values.

Configuration supported through the following environment variables:

- `CAF_AUTOSCALER_ALERT_DISPATCH_THRESHOLD`  
  Default: `CAF_AUTOSCALER_MESSAGING_RESOURCE_LIMIT_STAGE_1`  
  Description: This setting indicates the threshold that can be reached before starting to send out alerts when the messaging platform is beginning to run out of resources.  This value can be set to between 0 - 100 (0%-100%). If this configuration is not set then a message will be dispatched from the stage one alert threshold.  

- `CAF_AUTOSCALER_MESSAGING_RESOURCE_LIMIT_STAGE_1`  
  Default: `70`  
  Description: The percentage of available resources the messaging platform can use before the Autoscaler should take stage 1 action. Stage 1 action will involve shutting down any services with a shutdown priority of less than or equal to "CAF_AUTOSCALER_MESSAGING_STAGE_1_SHUTDOWN_THRESHOLD" or 1 if the environment variable is not set.  

- `CAF_AUTOSCALER_MESSAGING_RESOURCE_LIMIT_STAGE_2`  
  Default: `80`  
  Description: The percentage of available resources the messaging platform can use before the Autoscaler should take stage 2 action. Stage 2 action will involve shutting down any services with a shutdown priority of less than or equal to "CAF_AUTOSCALER_MESSAGING_STAGE_2_SHUTDOWN_THRESHOLD" or 3 if the environment variable is not set.  

- `CAF_AUTOSCALER_MESSAGING_RESOURCE_LIMIT_STAGE_3`  
  Default: `90`  
  Description: The percentage of available resources the messaging platform can use before the Autoscaler should take stage 3 action. Stage 3 action will involve shutting down any services with a shutdown priority of less than or equal to "CAF_AUTOSCALER_MESSAGING_STAGE_3_SHUTDOWN_THRESHOLD" or 5 if the environment variable is not set.  

- `CAF_AUTOSCALER_MESSAGING_STAGE_1_SHUTDOWN_THRESHOLD`  
  Default: `1`  
  Description: The priority threshold of services to shutdown in the event the messaging platform has used up to its stage 1 resource limit. Any service with a shutdown priority of or less than this value will be shutdown.  

- `CAF_AUTOSCALER_MESSAGING_STAGE_2_SHUTDOWN_THRESHOLD`  
  Default: `3`  
  Description: The priority threshold of services to shutdown in the event the messaging platform has used up to its stage 2 resource limit. Any service with a shutdown priority of or less than this value will be shutdown.  

- `CAF_AUTOSCALER_MESSAGING_STAGE_3_SHUTDOWN_THRESHOLD`  
  Default: `5`  
  Description: The priority threshold of services to shutdown in the event the messaging platform has used up to its stage 3 resource limit. Any service with a shutdown priority of or less than this value will be shutdown.  

### Alert Configuration

Configuration supported through the following environment variables:

- `CAF_AUTOSCALER_ALERT_DISABLED`  
  Default: `false`  
  Description: This switch can be used to disable alerts. If it is not set then alerts will be sent.  

- `CAF_AUTOSCALER_ALERT_FREQUENCY`  
  Default: `20`  
  Description: This will determine how long in minutes the autoscaler will wait between dispatching alerts.  

### Email Alert Configuration

Configuration for the Email alert functionality is supported through the following environment variables:

- `CAF_AUTOSCALER_SMTP_HOST`  
Default: n/a  
Description: SMTP server host address.  

- `CAF_AUTOSCALER_SMTP_PORT`  
Default: n/a  
Description: SMTP server port.  

- `CAF_AUTOSCALER_SMTP_USERNAME`  
Default: "" (Empty String)  
Description: SMTP server username.  

- `CAF_AUTOSCALER_SMTP_PASSWORD`  
Default: "" (Empty String)  
Description: SMTP server password.  

- `CAF_AUTOSCALER_SMTP_EMAIL_ADDRESS_TO`  
Default: n/a  
Description: The monitored email address to send alert emails to. If this property is not set the autoscaler will not attempt to use the email alert functionality.  
  
- `CAF_AUTOSCALER_SMTP_EMAIL_ADDRESS_FROM`  
Default: `apollo-autoscaler@microfocus.com`  
Description: The email address to send alert emails from. 

### Health checks
TODO

### Logging

Currently the logging is accessible via the kubectl api or via the Kubernetes dashboard if installed. The default log level in this container is `INFO` and this can be configured by supplying the required level in the `CAF_LOG_LEVEL` environment variable.


### Scaling

This container is not election-enabled. This means there should only be one
instance per set of deployments the container is scaling. If the container is
being run on Kubernetes this will ensure the container is restarted if it
crashes. It is recommended you do not attempt to monitor and scale a very
large number of applications with a single instance. While this should work
with an arbitrary number of target deployments, the monitoring and scaling
actions may begin to lag behind the requested period once the container goes
beyond its normal capability.


### Resource requirements

The container uses very little memory and CPU, and effectively no disk load.
It does, however, generate some network traffic through monitoring and scaling
of services. Kubernetes will be the target of most of the network traffic, though
the RabbitMQ management host will also be polled. The frequency of the traffic
depends upon the number of deployments being monitored and the requested frequency. 

- 128-196MB of RAM
- 0.1 CPUs per 10 services monitored
- Slow I/O is acceptable


### Failure modes

Since this container is a composite of other CAF components, see the failure
modes in the documentation for each module for further information on this
topic.


### Upgrade procedures

TODO


### Quick start
Assuming Kubernetes is enabled, follow the instructions in [example-dev-setup](https://github.com/Autoscaler/autoscaler/tree/develop/autoscale-kubernetes-container/example-dev-setup/README.md). 
Note that the [consumer](https://github.com/Autoscaler/autoscaler/tree/develop/autoscale-kubernetes-container/example-dev-setup/example-dev-setup/consumers.yaml) deployment 
and [publisher](https://github.com/Autoscaler/autoscaler/tree/develop/autoscale-kubernetes-container/example-dev-setup/example-dev-setup/publisher.yaml) job are 
are purely to demonstrate the scaling functionality

For each of the deployments to scale its labels must be updated to
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

Finally the `autoscale.profile` can be an arbitrary string, but one that should exist in the [RabbitWorkloadAnalyserConfiguration](https://github.com/Autoscaler/autoscaler/blob/develop/autoscale-kubernetes-container/autoscale-kubernetes-container/src/main/config/cfg~caf~autoscaler~RabbitWorkloadAnalyserConfiguration.js) resource deployed inside the autoscale container.

Deploy/redeploy the deployments and the autoscale container. After one or two 
minutes the autoscale container should find the deployments and start monitoring.
