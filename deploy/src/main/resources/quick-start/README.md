# Quick Start

## Introduction

The Autoscaler service provides on-demand scaling of services, allowing you to efficiently dedicate resources where they are needed most in your Mesos cluster, and minimizing costs and ensuring user satisfaction. 

## Deployment Repository

This repository provides the necessary files to easily get started using the Autoscaler Service.

### Prerequisites


- Docker must be available on the system
- Docker Swarm and RabbitMQ are deployed

The deployment files are written in [Docker Compose v3] format and is compatible with Docker Stack.

## Demonstration



### Usage

1. Download the files from the repository. You can clone this repository using Git or download the files in a tar.gz file from [here](https://github.com/Autoscaler/autoscaler/releases).

2. Configure the external parameters if required. The following parameters may be set:

<table>
      <tr>
        <th>Environment Variable</th>
        <th>Default</th>
        <th>Description</th>
      </tr>
      <tr>
        <td>DOCKER_HOST</td>
        <td>unix:///var/run/docker.sock</td>
        <td>Used to specify the Docker Swarm REST endpoint. Supports unix sockets and tcp type connections e.g. http://machine:2375</td>
      </tr>
	  <tr>
        <td>CAF_RABBITMQ_HOST</td>
        <td>rabbitmq</td>
        <td>Used to specify the RabbitMQ Management API Endpoint. e.g http://rabbitmq:15672</td>
      </tr>
	  <tr>
        <td>CAF_RABBITMQ_PORT</td>
        <td>5672</td>
        <td>Used to specify the RabbitMQ Management API Endpoint. e.g http://rabbitmq:15672</td>
      </tr>
	  <tr>
        <td>CAF_RABBITMQ_USERNAME</td>
        <td>guest</td>
        <td>Used to specify the username used to connect to RabbitMQ.</td>
      </tr>
	  <tr>
        <td>CAF_RABBITMQ_PASSWORD</td>
        <td>guest</td>
        <td>Used to specify the password used to connect to RabbitMQ.</td>
      </tr>
	  <tr>
        <td>CAF_AUTOSCALER_MAXIMUM_INSTANCES</td>
        <td>100</td>
        <td>Used to specify the maximum number of instances that any worker can be scaled to.</td>
      </tr>
	  <tr>
        <td>CAF_DOCKER_SWARM_TIMEOUT</td>
        <td>30</td>
        <td>Used to specify the max length of time in seconds that a docker REST call can take before a timeout out occurs.</td>
      </tr>
	  <tr>
        <td>CAF_DOCKER_SWARM_HEALTHCHECK_TIMEOUT</td>
        <td>5</td>
        <td>Used to specify the max length of time in seconds that the Docker endpoint healthcheck can take before a timeout occurs.</td>
      </tr>
	  <tr>
        <td>HTTP_PROXY</td>
        <td><b>No Default</b></td>
        <td>Optional. Used to specify an HTTP based proxy, which is used during the Docker REST endpoint communication.</td>
      </tr>
	  <tr>
        <td>HTTPS_PROXY</td>
        <td><b>No Default</b></td>
        <td>Optional. Used to specify an HTTPS based proxy, which is used during the Docker REST endpoint communication.</td>
      </tr>
	  <tr>
        <td>NO_PROXY</td>
        <td><b>No Default</b></td>
        <td>Optional. Used to specify an ignore list for HTTP based proxy communication.</td>
      </tr>
    </table> 

3. Deploy the services
 
 	First navigate to the folder where you have downloaded the files to and then run one of the following commands, depending on whether you are using Docker Compose or Docker Stack

	<table>
      <tr>
        <td><b>Docker Compose</b></td>
        <td>
            docker-compose up  <small>(docker-compose defaults to use a file called <i><b>docker-compose.yml</b></i>)</small><br />
            docker-compose up -d <small>(<i><b>-d</b></i> flag is for "detached mode" i.e. run containers in the background)</small>
        </td>
      </tr>
      <tr>
        <td><b>Docker Stack</b></td>
        <td>docker stack deploy --compose-file=docker-compose.yml autoscalerdemo</td>
      </tr>
    </table>

