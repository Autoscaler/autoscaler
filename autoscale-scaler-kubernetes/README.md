# autoscale-scaler-kubernetes

---

 This is an implementation of a `ServiceScaler` that uses the Kubernetes Client API 
 to scale up and down services.
`https://github.com/kubernetes-client/java/`

## Configuration

 The configuration source for this module is [K8sAutoscaleConfiguration](https://github.com/Autoscaler/autoscaler/blob/develop/autoscale-kubernetes-container/src/main/config/cfg~caf~autoscaler~K8sAutoscaleConfiguration.js).
 The following configuration options are present:

 - namespace: the Kubernetes namespace which contains the target deployments.
  Must not be null and must not be empty.
 - maximumInstances: the absolute upper ceiling for number of instances of a
  service. Minimum 1.

## Usage

 This module relies on performing API calls to Kubernetes and
 retrieving intance info on deployments which have a `metadata.label.autoscale.metric: rabbitmq`.   
 This module has a dummy health check to comply with the existing interface.


## Failure modes

