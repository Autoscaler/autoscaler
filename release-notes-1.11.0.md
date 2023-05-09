
!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
693063: Staging queues can now be considered when making a scaling decision.
- An optional environment variable names `CAF_AUTOSCALER_STAGING_QUEUE_INDICATOR` can now be provided.
- If this is provided, queue names that follow the naming pattern `"^" + scalingTarget + stagingQueueIndicator + ".+$"` will be considered
  staging queues and the statistics for these staging queues will be taken into account alongside the statistics for the target queue
  (scalingTarget) when making a scaling decision.

#### Known Issues
- None
