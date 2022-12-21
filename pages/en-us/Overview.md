---
layout: default
title: CAF Job Service Overview
banner: 
    icon: 'assets/img/autoscaler-graphic.png'
    title: Autoscaler
    subtitle: Automatic on-demand scaling of microservices
    links:
        - title: GitHub 
          url: https://github.com/Autoscaler/autoscaler 
---

# Overview

The Worker Autoscaler is a microservice that monitors Worker input queues and scales Workers up and down according to their workloads.

## Introduction
Containerized microservices are typically created so they can scale easily and quickly to meet the demand of an application. With many customers in many timezones, a single application may experience rapidly fluctuating load demands. The problem is not only scaling up to meet the demand, but scaling back down to better optimize the use of resources, as well as lower costs.

To achieve these goals, an Autoscaler needs the following:

* A method to retrieve the services it needs to scale, and their state
* A mechanism to measure the workload of the services
* A method to trigger the scaling of the services

Ideally, services may have different ways to measure their workload and different profiles representing a varying "quality of service" assigned to them.

## Autoscale Application
The Worker Autoscaler automatically scales workers which pass messages asynchronously using RabbitMQ, and which are orchestrated using the Mesos Marathon platform. However fundamentally the Autoscaler is built on a broader Autoscaler Framework, and the module which monitors RabbitMQ can be swapped out for a module which monitors a different messaging system.  Similarly if an alternative orchestration platform is being used instead of Marathon, then an appropriate implementation can be supplied to replace the Marathon implementation.

The `autoscale-core` module contains the core autoscaling application code.  It is responsible for acquiring a collection of services to monitor from a `ServiceSource`, validating them and scheduling `WorkloadAnalyser` runs periodically to get scaling recommendations. The application can trigger scaling via a `ServiceScaler` if the analyzer recommends an action be taken and constraints of the `ScalingConfiguration` allow it.

Before scaling a service up or down, scaling actions returned from the analyzer are passed to the `Governor` who performs a check to see if there are other registered services that have not yet their minimum instances. If so the scaling action may be adjusted to scale the service down in order to scale others up to their minimum. This prevents services from monopolizing environment resources and preventing other services from reaching their minimum instance count.

The application will periodically refresh its set of monitored services from the `ServiceSource`, add new ones, remove old ones and update ones that have changed (which involves canceling the scheduled operation of the `WorkloadAnalyser`) and create a new schedule.
 
Finally, the `autoscale-core` module has (optional) support for multiple instances in an active/standby configuration. All instances will monitor and perform analysis of the services, but only the active node will actually trigger the scaling. This means that if the active node fails, the standby nodes have historical data to perform scaling. If your container manager supports re-instancing of failed applications, you may not need multi-instance support. You must be willing to accept that the new instance may have to retrieve workload data for some amount of time before triggering scaling again.
