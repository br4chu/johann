# Introduction

Johann is a simple Java library that communicates with docker-compose CLI.

# Getting started

Once Johann is uploaded to Maven Central, you will be able to add it as a maven/gradle dependency and start using it.

Maven dependency:
```xml
<dependency>
    <groupId>io.brachu</groupId>
    <artifactId>johann</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Gradle dependency:
```groovy
compile 'io.brachu:johann:0.1.0-SNAPSHOT'
```

## Requirements

* Docker Engine 1.12+ (because of health checks)
* Docker Compose 1.14+ (because of `-f -` option)

## Example usage

### Local docker engine

#### Running docker-compose.yml file placed in one of the root directories of classpath

```java
DockerCompose compose = DockerCompose.builder().classpath().build();
```

#### Running docker-compose.yml file with custom name in one of the root directories of classpath

```java
DockerCompose compose = DockerCompose.builder().classpath("custom-compose-file.yml").build();
```

#### Running docker-compose.yml file in one of the subdirectories of the classpath

```java
DockerCompose compose = DockerCompose.builder().classpath("path/to/docker-compose.yml").build();
```

#### Running docker-compose.yml file outside of classpath

```java
DockerCompose compose = DockerCompose.builder().absolute("/path/to/docker-compose.yml").build();
```

#### Starting and waiting for compose cluster to be up

```java
compose.up();
compose.waitForCluster(1, TimeUnit.MINUTES);
```

Calling `up` method is equivalent to executing `docker-compose up -d` command.

`waitForCluster` method waits for all containers within a cluster to be either healthy or running (if they have no health check).
For most consistent results, all your containers should implement health checks.

You can read more about container health checks [here](https://docs.docker.com/engine/reference/builder/#healthcheck).

#### Shutting compose cluster down gracefully

```java
compose.down();
```

Calling `down` method is equivalent to executing `docker-compose down` command.

#### Killing compose cluster (may leave garbage containers)

```java
compose.kill();
```

Calling `kill` method is equivalent to executing `docker-compose kill` command.

#### Passing environment variables to docker-compose

```java
DockerCompose compose = DockerCompose.builder()
    .classpath()
    .env("MY_ENV_VAR", "my value")
    .env("ANOTHER_VAR", "another value")
    .build();
```

#### Assinging project name to your compose cluster

By default, Johann generates random string and passes it to `docker-compose` command as a project name.
You can override this behaviour by passing your own project name to the builder:

```java
DockerCompose compose = DockerCompose.builder()
    .classpath()
    .projectName("customProjectName")
    .build();
```

#### Retrieving host port of a container

Assuming your docker-compose.yml file looks something like this:

```yaml
version: '2.1'

services:
  rabbitmq:
    image: rabbitmq:3.6.10-alpine
    ports:
      - "5672"
```

You can retrieve a host port bound to container's 5672 port by calling Johann's `port` method as follows:

```java
ContainerPort containerPort = compose.port("rabbitmq", 5672);
int port = containerPort.getPort();
```

You can even use `ContainerPort::format` method to create proper URL address in one line:

```java
String url = compose.port("rabbitmq", 5672).format("tcp://$HOST:$PORT");
```

### Remote docker engine

Johann can connect to a remote Docker Engine if `DOCKER_HOST` environment variable is passed to the Java process that runs Johann.
`DOCKER_TLS_VERIFY` and `DOCKER_CERT_PATH` variables are also supported.

You can read more about these variables [here](https://docs.docker.com/compose/production/#running-compose-on-a-single-server).

## Running test suite

In order to run the test suite properly, your local machine must have docker and docker-compose installed.
Version requirements are posted at the top of this README.
