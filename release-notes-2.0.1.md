#### Version Number
${version-number}

#### Breaking Changes
709003: RabbitMQ disk space will now be monitored by default.

#### New Features
693063: Staging queues can now be considered when making a scaling decision.

#### Bug Fixes
709003: An issue preventing memory overload alerts from being dispatched when the messaging platform was experiencing high memory usage has been fixed.  

709003: An issue preventing instances from being scaled down in Kubernetes when the messaging platform was experiencing high memory usage has been fixed.  
- This was caused by the Autoscaler attempting to read the `autoscale.shutdownPriority` label instead of the `autoscale.shutdownpriority` label.

#### Known Issues
- None
