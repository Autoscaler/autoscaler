!not-ready-for-release!

#### Version Number
${version-number}

#### Breaking Changes
- **US922204**: Autoscaler is only available using Kubernetes
  - Support for Marathon and Docker Swarm have been removed.

#### New Features
- US929026: Updated to run on Java 21.
- US952036: Image is now built on Oracle Linux.

#### Bug Fixes
- US957002: The number of `running` and `pending` pods will now be retrieved from the querying the pods.  
  - Previously, these values were not retrieved from the querying the pods as they should have been. The cause of this bug 
    was because the Autoscaler was looking for a label named `app` in `metadata.labels`, when actually the label was located in 
    `spec.selector.matchLabels` and `spec.selector.matchLabels`. As such, the Autoscaler defaulted to retrieving the number of `running`
    pods from `spec.replicas`, and used a value of `0` for the number of `pending` pods.
  - This has now been resolved, and the number of `running` and `pending` pods will now be retrieved from the querying the pods.

#### Known Issues
