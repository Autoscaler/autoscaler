#### Version Number
${version-number}

#### New Features  
- [SCMOD-8333](https://portal.digitalsafe.net/browse/SCMOD-8333): Allow rescaling to ensure services can scale up from zero  
  Autoscaler has been modified to allow it to scale down a service to make room for a service attempting to scale up when it determines that the service attempting to scale up requires the resources more.  

#### Known Issues
