### Set up for local dev testing
Disable the proxies on your windows box and disconnect from VPN.  
Enable Kubernetes on Docker for Desktop.  

If building the autoscaler locally update the image in [autoscaler yaml](./autoscaler.yaml)

### Install the dashboard
`kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.2.0/aio/deploy/recommended.yaml`  

**Enables anonymous login**  
`kubectl patch deployment kubernetes-dashboard -n kubernetes-dashboard --type 'json' -p '[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--enable-skip-login"}]'`  

**Start the proxy**  
`kubectl proxy`  

**View the dashboard**  
`http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/#/workloads?namespace=default`  

### Install rabbitmq to Kubernetes
`kubectl create namespace rabbit`  
`helm repo add bitnami https://charts.bitnami.com/bitnami`  
`helm install mu-rabbit --namespace rabbit --set auth.username=guest,auth.password=guest,rabbitmq.erlangCookie=secretcookie bitnami/rabbitmq`  
`kubectl port-forward --namespace rabbit svc/mu-rabbit-rabbitmq 15672:15672`  

**Log into RabbitMQ**  
*Start the proxy if not already started*.  
`kubectl proxy`  
`http://localhost:15672/`  

### Path to the directory
`cd autoscaler/autoscale-kubernetes-container/example-dev-setup`

### Create a job to publish messages to RabbitMQ
`kubectl apply -f ./publisher.yaml`  
This will publish a number of messages to the `hello` queue.  
To publish again rename the `metadata.name` from that same file and run again (This will create a new job)
 
### Create 2 deployments in different namespaces to consume messages from RabbitMQ
`kubectl apply -f ./consumers.yaml`  
This will create 2 deployments that will consume 1 message per second and per pod from the `hello` queue.  

### Deploy the Autoscaler
`kubectl apply -f ./autoscaler.yaml` .

### Observing the scaling in action
If the dashboard is installed the scaling can be observed on the deployments panel and also by viewing the pods panel.  
Alternatively the autoscaler will log output scaling actions and can be viewed either by `kubectl logs <autoscaler-pod>` 
or via the dashboard logging feature. 

### '_deployments.apps_' is forbidden issues
If you get:  
`message: deployments.apps is forbidden: User \"system:serviceaccount:default:default\" cannot list resource \"deployments\" in API group \"apps\" in the namespace \"default\"`  
 
Run `kubectl create clusterrolebinding serviceaccounts-cluster-admin \
--clusterrole=cluster-admin \
--group=system:serviceaccounts`  
(**WARNING**: This allows any user with read access to secrets or the ability to create a pod to access super-user credentials.)

### Create dashboard credentials
If you can't see your deployments in the dashboard, you may need to create a user, then log in.

1- Create a new ServiceAccount  
`kubectl apply -f - <<EOF  
apiVersion: v1  
kind: ServiceAccount
metadata:
name: admin-user
namespace: kubernetes-dashboard
EOF`  

2- Create a ClusterRoleBinding for the ServiceAccount  
`kubectl apply -f - <<EOF
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
name: admin-user
roleRef:
apiGroup: rbac.authorization.k8s.io
kind: ClusterRole
name: cluster-admin
subjects: - kind: ServiceAccount name: admin-user namespace: kubernetes-dashboard
EOF`

3- Get the Token for the ServiceAccount  
`kubectl -n kubernetes-dashboard describe secret $(kubectl -n kubernetes-dashboard get secret | grep admin-user | awk '{print $1}')`  
4- Copy the token and copy it into the Dashboard login and press "Sign in"
(Note that the token does expire after a while, so you may have to repeat steps 3-4)
  
[Source](https://kubernetes.io/blog/2020/05/21/wsl-docker-kubernetes-on-the-windows-desktop/)
