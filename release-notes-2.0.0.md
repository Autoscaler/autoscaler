
!not-ready-for-release!

#### Version Number
${version-number}

#### Breaking Changes
709003: RabbitMQ disk space will now be monitored by default.

The following environment variables have been renamed:
- `CAF_AUTOSCALER_RABBITMQ_MEMORY_QUERY_FREQ` renamed to `CAF_AUTOSCALER_RABBITMQ_RESOURCE_QUERY_FREQ`
- `CAF_AUTOSCALER_MESSAGING_RESOURCE_LIMIT_STAGE_1` renamed to `CAF_AUTOSCALER_MESSAGING_MEMORY_USED_PERCENT_LIMIT_STAGE_1`
- `CAF_AUTOSCALER_MESSAGING_RESOURCE_LIMIT_STAGE_2` renamed to `CAF_AUTOSCALER_MESSAGING_MEMORY_USED_PERCENT_LIMIT_STAGE_2`
- `CAF_AUTOSCALER_MESSAGING_RESOURCE_LIMIT_STAGE_3` renamed to `CAF_AUTOSCALER_MESSAGING_MEMORY_USED_PERCENT_LIMIT_STAGE_3`
- `CAF_AUTOSCALER_ALERT_DISPATCH_THRESHOLD` renamed to `CAF_AUTOSCALER_MEMORY_USED_PERCENT_ALERT_DISPATCH_THRESHOLD`

The following environment variables have been added:

- `CAF_AUTOSCALER_DISK_FREE_MB_ALERT_DISPATCH_THRESHOLD`
  - The amount of disk space (MB) remaining on the messaging platform that will trigger the Autoscaler to dispatch alerts indicating that the messaging platform is running out of disk space.
  - If this is not set then a message will be dispatched from the `CAF_AUTOSCALER_MESSAGING_DISK_FREE_MB_LIMIT_STAGE_1` value, or 400 if `CAF_AUTOSCALER_MESSAGING_DISK_FREE_MB_LIMIT_STAGE_1` is not set.

- `CAF_AUTOSCALER_MESSAGING_DISK_FREE_MB_LIMIT_STAGE_1`
  - The amount of disk space (MB) remaining on the messaging platform that will trigger the Autoscaler to take stage 1 action.
  - Stage 1 action will involve shutting down any services with a shutdown priority of less than or equal to `CAF_AUTOSCALER_MESSAGING_STAGE_1_SHUTDOWN_THRESHOLD` or 1 if the environment variable is not set.
  - Default value is 400

- `CAF_AUTOSCALER_MESSAGING_DISK_FREE_MB_LIMIT_STAGE_2`
  - The amount of disk space (MB) remaining on the messaging platform that will trigger the Autoscaler to take stage 2 action.
  - Stage 2 action will involve shutting down any services with a shutdown priority of less than or equal to `CAF_AUTOSCALER_MESSAGING_STAGE_2_SHUTDOWN_THRESHOLD` or 3 if the environment variable is not set.
  - Default value is 200

- `CAF_AUTOSCALER_MESSAGING_DISK_FREE_MB_LIMIT_STAGE_3`
  - The amount of disk space (MB) remaining on the messaging platform that will trigger the Autoscaler to take stage 3 action.
  - Stage 3 action will involve shutting down any services with a shutdown priority of less than or equal to `CAF_AUTOSCALER_MESSAGING_STAGE_3_SHUTDOWN_THRESHOLD` or 5 if the environment variable is not set.
  - Default value is 100

#### New Features
693063: Staging queues can now be considered when making a scaling decision.
- An optional environment variable named `CAF_AUTOSCALER_STAGING_QUEUE_INDICATOR` can now be provided.
- If this is provided, queue names that follow the naming pattern `"^" + scalingTarget + stagingQueueIndicator + ".+$"` will be considered
  staging queues and the statistics for these staging queues will be taken into account alongside the statistics for the target queue
  (scalingTarget) when making a scaling decision.

#### Bug Fixes
709003: An issue preventing memory overload alerts from being dispatched when the message platform was experiencing high memory usage has been fixed.  

709003: An issue preventing instances being scaled down in Kubernetes when the message platform was experiencing high memory usage has been fixed.  
- This was caused by the Autoscaler attempting to read the `autoscale.shutdownPriority` label instead of the `autoscale.shutdownpriority` label.

#### Known Issues
- None
