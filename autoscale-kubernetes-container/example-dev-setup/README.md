### Set up for local dev testing
Disable the proxies on your windows box and disconnect from VPN.  
Enable Kubenetes on Docker for Desktop.  

###Install the dashboard
`kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.2.0/aio/deploy/recommended.yaml`    

**Enables anonymous login**  
`kubectl patch deployment kubernetes-dashboard -n kubernetes-dashboard --type 'json' -p '[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--enable-skip-login"}]'`  

**Start the proxy**  
`kubectl proxy`  

**View the dashboard**  
`http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/#/workloads?namespace=default`  

###Install a rabbitmq to Kubernetes
`kubectl create namespace rabbit`  
`helm repo add bitnami https://charts.bitnami.com/bitnami`  
`helm install mu-rabbit bitnami/rabbitmq --namespace rabbit`
`kubectl port-forward --namespace rabbit svc/mu-rabbit-rabbitmq 15672:15672`  

**Username**
`user`  
**Get the password**  
`kubectl get secret --namespace rabbit mu-rabbit-rabbitmq -o jsonpath="{.data.rabbitmq-password}" | base64 --decode`  
Configure amqp/http connections in `publisher.yaml` `consumer.yaml` `autoscaler.yaml`.   

**Log into RabbitMQ**  
*Start the proxy if not already started*  
`kubectl proxy`
`http://localhost:15672/`

###Create a job to publish messages to RabbitMQ  
`kubectl apply -f ./publisher.yaml`  
This will publish a number of messages to the `hello` queue.   
To publish again rename the `metadata.name` and run again. 
 
###Create 2 deployments in different namespaces to consume messages from RabbitMQ. 
`kubectl apply -f ./consumers.yaml`  
This will create 2 deployments that will consume 1 message per second per pod from the `hello` queue.  

###Deploy the autoscaler  
`kubectl apply -f ./autoscaler.yaml` 

###Observing the scaling in action
If the dashboard is installed the scaling can be observed on the deployments panel and also by viewing the pods panel.  
Alternatively the autoscaler will log output scaling actions and can be viewed either by `kubectl logs <autoscaler-pod>` 
or via the dashboard logging feature. 



