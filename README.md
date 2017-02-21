# AutoScaler

The [Autoscaler](https://github.hpe.com/caf/autoscaler) service provides on-demand scaling of services, allowing you to efficiently dedicate resources where they are needed most in your Mesos cluster, and minimizing costs and ensuring user satisfaction. 

The Autoscaler is an extensible framework, which allows you to provide your own modules to retrieve services to scale, metrics to make scaling decisions and instigate a scaling action. 

The Autoscaler service provides a source for Marathon that identifies services to scale using Marathon labels. A RabbitMQ workload analyzer retrieves details of RabbitMQ queues to make scaling decisions. A Marathon application scaler issues commands to the Marathon REST API to scale up and down a service.

Manifest of the components which make up the AutoScaler:

* autoscale-container
* autoscale-marathon-shared
* autoscale-workload-rabbit
* autoscale-scaler-marathon
* autoscale-source-marathon
* autoscale-core
* autoscale-api