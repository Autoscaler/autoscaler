#### Version Number
${version-number}

#### New Features  
- **SCMOD-8752**: Support for independent scaling action backoffs  
Scaling backoffs have been split into 3 separate backoff values. These backoffs allow for the application to backoff different amounts depending on if the previous scaling action was either a scale up or scale down action.
- **SCMOD-9230**: Updated to use security hardened JRE/JDK base image version of java.
- **SCMOD-9780**: Updated images to use Java 11

#### Bug Fixes
- **SCMOD-9987**: Better handling of unavailable RabbitMQ nodes during workload analysis.

#### Known Issues
- None
