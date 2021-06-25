# autoscale-source-kubernetes

---

 This is an implementation of a `ServiceSource` that uses the Kubernetes REST API
 to acquire a list of scalable services.
  `https://github.com/kubernetes-client/java/`

## Configuration

 The configuration source for this module is [K8sAutoscaleConfiguration](../autoscale-kubernetes-container/src/main/config/cfg~caf~autoscaler~K8sAutoscaleConfiguration.js).
 The following configuration options are present:

 - maximumInstances: the absolute upper ceiling for number of instances of a
 service. Minimum 1.
 - groupId: the Kubernetes metadata `autoscale.groupid` label value that will identify deployments 
 to scale.  Must not be null and must not be empty.
 - namespaces: the Kubernetes namespaces, comma separated, which contains the target deployments.
 Must not be null and must not be empty.

## Usage

 This module relies on performing API calls to Kubernetes and retrieving instance info on deployments 
 which have the label `autoscale.metric: rabbitmq`.  

## Failure modes


