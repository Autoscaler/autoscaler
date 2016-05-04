# autoscale-core

---

 This project contains the container autoscaling applcation code and various
 module implementations that have been designed to work with it.
 

## Requirements for automatically scaling services

 Containerised microservices are typically created so they can scale easily and
 quickly to meet the demand of an application. If the load is known, or changes
 slowly or infrequently, it is not a problem to make this a manual operation.
 However with many customers in many timezones, a single application may
 experience rapidly fluctuating load demands. The problem is not simply scaling
 up to meet the demand, but scaling back down to better optimise the use of
 resources (and lower costs) as well.
 
 To achieve these goals, an autoscaler needs the following:
 
 - A method to retrieve the services it needs to scale, and their state
 - A mechanism to measure the workload of the services
 - A method to trigger the scaling of the services
    
 Ideally, services may have different ways to measure their workload, and 
 different profiles representing a varying "quality of service" assigned
 to them.
    

## The autoscale application

 The `autoscale-core` application is responsible for acquiring a collection of
 services to monitor from a `ServiceSource`, validate them, and then schedule
 runs of a `WorkloadAnalyser` periodically to get scaling recommendations. The
 application may then trigger scaling via a `ServiceScaler` if the analyser
 recommends an action be taken and constraints of the `ScalingConfiguration`
 allow it.
 
 The application will periodically refresh its set of monitored services from
 the `ServiceSource`, adding new ones, removing old ones, and updating ones
 that have changed (which involves cancelling the scheduled operation of the
 `WorkloadAnalyser` and creating a new schedule).
 
 Finally it should be noted that `autoscale-core` has (optional) support for
 multiple instances in a master/slave configuration. All instances will monitor
 and perform analysis of the services, but only the master will actually
 trigger the scaling. This means that in the case of the failed master, the
 slaves have historical data to perform scaling with. If your container manager
 supports re-instancing of failed applications, you may not need multi-instance
 support if you are willing to accept that the new instance may have to
 retrieve workload data for some amount of time before triggering scaling
 again.
 
### Configuration

 No environment variables are required to be set to start `autoscale-core`, if
 you are running on the Marathon platform. If you are not, you will need to set
 `caf.appname` to an appropriate value to specify the namespace/grouping for
 the particular autoscaler instance.

 The following configuration can (optionally) be set in the yaml file:

 - sourceRefreshPeriod (integer), the time in seconds between refreshing the
  available services to autoscale, defaults to 900
 - executorThreads (integer), the number of simultaneous execution threads in
  the scheduled thread pool, defaults to 5
    
### Starting the application

 The following command-line should start the application:
 
 ```
 java -cp "*" com.hpe.caf.autoscale.core.AutoscaleApplication server [yaml]
 ```
 
### The ServiceSource component

 The following classes and interfaces are relevant to the `ServiceSource`
 component:
 
 - ServiceSource: abstract class that acts as a base implementation.
 - ServiceSourceProvider: interface for acquiring a ServiceSource.
 - ScalingConfiguration: returned by a ServiceSource to the application.
    
 An implementation of `ServiceSource` acquires service information and produces
 a set of `ScalingConfiguration` objects which are passed back to the
 application which will then be validated and if so, monitored and autoscaled.
 The component will be asked periodically for the latest set of services.
 
### The ServiceScaler component

 The following classes and interfaces are relevant to the `ServiceSource`
 component:
 
 - ServiceScaler: abstract class that acts as a base implementation.
 - ServiceScalerProvider: interface for acquiring a ServiceScaler.
 - InstanceInfo: container for information about the running instances of a
  service.
 - ServiceHost: container for information about the host of a specific service
  instance.
    
 An implementation of `ServiceScaler` should be able to report upon the current
 number of running instances of a service (and where they are) and also be
 able to trigger the scaling up or down of the service. The `ServiceScaler` is
 not required to enforce the minimum/maximum instances that a service requests.
 The application itself will perform these checks.
 
### The WorkloadAnalyser component

 The following classes and interfaces are relevant to the `WorkloadAnalyser`
 component:
 
 - WorkloadAnalyser: abstract class that acts as a base implementation for
  performing analysis.
 - WorkloadAnalyserFactory: abstract class for acquiring a WorkloadAnalyser for
  a given service.
 - WorkloadAnalyserFactoryProvider: interface for acquiring a
  WorkloadAnalyserFactory.
 - ScalingAction: represents a recommendation on scaling from a
  WorkloadAnalyser to the application.
 - ScalingOperation: enumeration indicating whether to scale up, down, or not
  at all.
    
 A `WorkloadAnalyser` performs analysis for a specific service, which will be
 instantiated by a `WorkloadAnalyserFactory`. This factory itself is acquired
 by a `WorkloadAnalyserFactoryProvider`, which also must provide a unique key
 to the application which is used to identify this sort of `WorkloadAnalyser`.
 Services will request their method of workload analysis be performed by
 specifying this key. The `analyseWorkload(InstanceInfo)` method of the
 `WorkloadAnalyser` will be called periodically by a `ScalerThread` which is
 scheduled by `autoscale-core`.
 

## The ScalingConfiguration object

 A service needs to specify various information about how it wants its workload
 to be analysed, its scaling profile, and minimum and maximum instances. The
 `ServiceSource` component is responsible for returning a collection of these
 objects back to `autoscale-core`. The `ScalingConfiguration` class has various
 validation annotations to ensure a specific instance of the class can be used.
 The important parts that should be filled in by `ServiceSource` are:
 
 - id: the id, reference, or name of the service. Must not be null or empty.
 - appOwner: name of the owning application of the service. Must not be null or
  empty.
 - interval: period in seconds between performing analysis runs on this
  service. Minimum is 1.
 - minInstances: minimum number of instances for this service. Minimum is 0.
 - maxInstances: maximum number of instances for this service. Minimum is 1.
 - workloadMetric: name (key) of the WorkloadAnalyser that should perform
  analysis for this service. Must not be null or empty.
 - scalingTarget: implementation specific value that refers to the target that
  must be monitored for scaling purposes. Typically with queue-based metrics,
  this will be the name of the queue. This can be null.
 - scalingProfile: name of the profile to use which may be relevant to the
  specific WorkloadAnalyser implementation. This can be null.
 - backoff: the number of workload analysis runs that will be skipped afer the
  analyser triggers a scaling operation. This can help avoid unusual scaling
  behaviour while the system is in-between states
    
 There are additional scenarios which may mean a service may be ignored by an
 instance of `autoscale-core` which are outside basic validation. These are
 the case where the application owner of the `autoscale-core` instance does not
 match the appOwner specified in the `ScalingConfiguration`, and the case where
 the workload metric specified is not available to that instance of
 `autoscale-core`.
 
 
## Creating a docker container

 A container for a worker is generally made as a new subproject in Maven (or
 your preferred build system). It will have no code itself, but will have 
 dependencies upon `worker-core` and an implementation of all the required
 components, together with any start scripts. Fixed configuration parameters
 required at startup can be safely added as Java properties in the command
 line for starting the application. Configuration parameters that need to be
 variable should be left to be set by environment variables in your container
 deployment template. You will need the following dependencies in your project
 to create a fully functioning container:
 
 - [autoscale-core](https://github.hpe.com/caf/autoscale-core)
 - [caf-api](https://github.hpe.com/caf/caf-api)
 - An implementation of ServiceSource (i.e. [autoscale-source-marathon](https://github.hpe.com/caf/autoscale-source-marathon))
 - An implementation of ServiceScaler (i.e. [autoscale-scaler-marathon](https://github.hpe.com/caf/autoscale-scaler-marathon))
 - An implementation of WorkloadAnalyser (i.e. [autoscale-workload-rabbit](https://github.hpe.com/caf/autoscale-workload-rabbit))
 - An implementation of Codec (i.e. [codec-json](https://github.hpe.com/caf/codec-json))
 - An implementation of Cipher (optional) (i.e. [cipher-null](https://github.hpe.com/caf/cipher-null))
 - An implementation of Election (optional) (i.e. [election-null](https://github.hpe.com/caf/election-null))
    
