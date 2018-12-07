# AutoScaler

The [Autoscaler](https://github.com/Autoscaler/autoscaler) service provides on-demand scaling of services, allowing you to efficiently dedicate resources where they are needed most in your Mesos cluster, and minimizing costs and ensuring user satisfaction. 

The Autoscaler is an extensible framework, which allows you to provide your own modules to retrieve services to scale, metrics to make scaling decisions and instigate a scaling action. 

The Autoscaler service provides a source for Marathon and Docker Swarm that identifies services to scale using labels. A RabbitMQ workload analyzer retrieves details of RabbitMQ queues to make scaling decisions. A application scaler issues commands to the orchestrator REST API to scale up and down a service.

Manifest of the components which make up the AutoScaler:

Note: As the Autoscaler has two orchestrator variants currently, the manifest is split into 
- Marathan or Docker Swarm Assets
- Common Assets.


### Marthan Variants
* autoscale-marathon-container
* autoscale-marathon-shared
* autoscale-scaler-marathon
* autoscale-source-marathon

### Docker Swarm Variants
* autoscale-dockerswarm-container
* autoscale-dockerswarm-shared
* autoscale-scaler-dockerswarm
* autoscale-source-dockerswarm

### Common Assets
* autoscale-workload-rabbit
* autosacle-email-alert-dispatcher
* autoscale-core
* autoscale-api
