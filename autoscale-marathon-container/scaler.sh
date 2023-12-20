#!/bin/bash
#
# Copyright 2015-2024 Open Text.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# If the CAF_APPNAME and CAF_CONFIG_PATH environment variables are not set, then use the
# JavaScript-encoded config files that are built into the container
if [ -z "$CAF_APPNAME" ] && [ -z "$CAF_CONFIG_PATH" ];
then
  export CAF_APPNAME=caf/autoscaler
  export CAF_CONFIG_PATH=/maven/config
  export CAF_CONFIG_DECODER=JavascriptDecoder
  export CAF_CONFIG_ENABLE_SUBSTITUTOR=false
fi

cd /maven
java $CAF_AUTOSCALER_JAVA_OPTS -cp "*" com.github.autoscaler.core.AutoscaleApplication server scaler.yaml
