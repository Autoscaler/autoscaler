# Autoscaler Marathon Service Container

This repository consists of the source to build a pre-defined CAF approved
container that includes the CAF autoscale application along with
standard modules. Sample configuration is also supplied in the code base.

To get the autoscale application up and running look at the [Quick Start](../autoscale-marathon-container/README.md#quick-start).


### Container deployment

### Configuration

For standard configuration of the AutoScaler that is supported through environment variables see [here](../autoscale-marathon-container/README.md#configuration).


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

The Health check information for this container can be found [here](../autoscale-marathon-container/README.md#health-checks).


### Logging

This containers logging configurations and information can be found [here](../autoscale-marathon-container/README.md#logging).


### Scaling

For information on this container's scaling capabilities see [here](../autoscale-marathon-container/README.md#scaling).


### Resource requirements

The containers resource requirements can be found [here](../autoscale-marathon-container/README.md#resource-requirements).


### Failure modes

Since this container is a composite of other CAF components, see the failure
modes in the documentation for each module for further information on this
topic.


### Upgrade procedures

For upgrade procedures for this container see [here](../autoscale-marathon-container/README.md#upgrade-procedures).

