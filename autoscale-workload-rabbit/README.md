# autoscale-workload-rabbit

---

 This is an implementation of a `WorkloadAnalyser` that uses a RabbitMQ
 management server to determine the workload of a service using a RabbitMQ
 queue.

 The `autoscale.metric` key name for this module is `rabbitmq`.


## Configuration

 The configuration source for this module is
 [RabbitWorkloadAnalyserConfiguration](https://github.com/Autoscaler/autoscaler/blob/develop/autoscale-container/example-configs/cfg_autoscaler_marathon_RabbitWorkloadAnalyserConfiguration). The following configuration options are
 present:

 - rabbitManagementEndpoint: a valid URL that is the HTTP endpoint of the
  RabbitMQ management server
 - rabbitManagementUser: the RabbitMQ management user, which is used for basic
  HTTP authentication
 - rabbitManagementPassowrd: the RabbitMQ management user's password, which is
  used for basic HTTP authentication. This configuration parameter is assumed
  to be encrypted (if you are using a Cipher module)
 - vhost: the RabbitMQ vhost that contains the queues
 - profiles: map of profile name to `RabbitWorkloadProfile` objects, which
  represent different scaling profiles for this implementation (see below).
  Note there *must* be a profile named "default"  
 - memoryQueryRequestFrequency: The number of seconds that the service should wait between issuing requests to check rabbitmq's
  current memory consumption. 
 - stagingQueueIndicator: A string that is used to identify queues that are
  staging queues. If this is provided, queue names that follow the naming pattern
  `"^" + scalingTarget + stagingQueueIndicator + ".+$"` will be considered staging
  queues and the statistics for these staging queues will be taken into account
  alongside the statistics for the target queue (scalingTarget) when making
  a scaling decision.


## Usage

 Given a queue name and number of instances of a service running and staging,
 the module will use the following logic to make a scaling recommendation:

 - If there is any instances staging, don't perform any analysis, return
  no scaling recommendation
 - If there are no instances staging, query the rabbit management server for
  queue statistics, and keep the last `n` statistics records, where `n` is
  the `scalingDelay` from the `RabbitWorkloadProfile`
 - If there are no instances at all but a non-negative number of messages in
  the queue, immediately trigger a scale up of 1 instance
 - If we have performed an integer multiple of `scalingDelay` iterations,
  average the statistics recorded
 - If the average consumption rate is zero, publish rate is zero, and the
  average number of messages is exactly zero, scale all the way down
 - If the average consumption rate is zero, but there are messages being
  worked upon, do not scale yet until we have some idea about consumption rate
 - If the average consumption rate is above zero, determine how many workers
  are needed at the current rate to complete the current backlog of messages
  in the time `backlogGoal` from the specified `RabbitWorkloadProfile` and
  scale appropriately

### The RabbitWorkloadProfile

 Each profile must be named and has the following properties:

 - scalingDelay: integer that specifies the number of queue statistics records
  that must be acquired sequentially before performing analysis upon them and
  issuing a scaling recommendation. This means the period in time between
  making scaling recommendations is the service interval multiplied by the
  scalingDelay. This number should be small enough to respond to varying load
  but big enough to avoid erratic behaviour. Minimum is 1
 - backlogGoal: the amount of time in seconds that we ideally want to complete
  the current backlog of messages in. This is effectively a quality of service
  parameter, where lower will trigger more aggressive scaling


## Failure modes

 The following scenarios will prevent the module from initialising:

 - The configuration cannot be retrieved
 - The configuration is not valid
 - There is no entry in `profiles` in the configuration with the key "default"

 The following scenarios have been identified as possible runtime failure modes
 for this module:

 - Non-transient communication failures with the RabbitMQ host
 - The `ServiceScaler` not providing reliable information on the number of
  instances
