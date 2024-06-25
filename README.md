# AutoScaler

The [Autoscaler](https://github.com/Autoscaler/autoscaler) service provides on-demand scaling of services, allowing you to efficiently dedicate resources where they are needed most in your Mesos cluster, and minimizing costs and ensuring user satisfaction. 

The Autoscaler is an extensible framework, which allows you to provide your own modules to retrieve services to scale, metrics to make scaling decisions and instigate a scaling action. 

The Autoscaler service provides a source for Kubernetes that identifies services to scale using Kubernetes labels. A RabbitMQ workload analyzer retrieves details of RabbitMQ queues to make scaling decisions. A Kubernetes application scaler issues commands to the Kubernetes REST API to scale up and down a service.

Manifest of the components which make up the AutoScaler:

* autoscale-container
* autoscale-kubernetes-shared
* autoscale-workload-rabbit
* autoscale-scaler-kubernetes
* autoscale-source-kubernetes
* autoscale-core
* autoscale-api