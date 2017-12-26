# Introduction

Johann is a simple Java library that communicates with docker-compose CLI.

# Getting started

Just add Johann as a maven/gradle dependency and start using it. See [Example usage](#example-usage) for more information.

Maven dependency:
```xml
<dependency>
    <groupId>io.brachu</groupId>
    <artifactId>johann</artifactId>
    <version>0.4.0</version>
</dependency>
```

Gradle dependency:
```groovy
compile 'io.brachu:johann:0.4.0'
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
DockerCompose compose = DockerCompose.builder().classpath("/custom-compose-file.yml").build();
```

#### Running docker-compose.yml file in one of the subdirectories of the classpath

```java
DockerCompose compose = DockerCompose.builder().classpath("/path/to/docker-compose.yml").build();
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

#### Stopping a single service within compose cluster

```java
compose.stop("postgresql");
```

#### Starting previously stopped service

```java
compose.start("postgresql");
compose.waitForService("postgresql", 1, TimeUnit.MINUTES);
```

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

### Integration with docker-compose-maven-plugin

As of version 0.2.0, Johann will first try to detect if it's being run in the context of
[docker-compose-maven-plugin](https://github.com/br4chu/docker-compose-maven-plugin) before generating random project name. If it is, it will reuse the project
name set during the `up` goal of docker-compose-maven-plugin. If it does not, only then it will generate a random project name.

Internally this works by trying to read the value of `maven.dockerCompose.project` system property that is set during `up` goal of docker-compose-maven-plugin.

Please note that it will not work by default due to how maven-failsafe-plugin executes its test suite.
In order to make this integration work, you will need to either disable forking in maven-failesafe-plugin with `forkCount` property:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>${failsafe-plugin.version}</version>
    <configuration>
        <forkCount>0</forkCount>
    </configuration>
</plugin>
```

or pass docker-compose-maven-plugin's system property manually to forked process:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>${failsafe-plugin.version}</version>
    <configuration>
        <systemPropertyVariables>
            <maven.dockerCompose.project>${maven.dockerCompose.project}</maven.dockerCompose.project>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

#### Retrieving container's IP address

Assuming following docker-compose.yml file:

```yaml
version: '2.3'

services:
  rabbitmq:
    image: rabbitmq:3.6.10-alpine
    ports:
      - "5672"
```

You can retrieve container's IP address for rabbitmq service as follows:

```java
String ip = compose.ip("rabbitmq");
```

If a container is bound to multiple networks, you can pass the network name as a second argument to the `ip` method as follows:

```java
String ip = compose.ip("rabbitmq", "my_custom_network");
```

#### Retrieving host port of a container

Assuming following docker-compose.yml file:

```yaml
version: '2.3'

services:
  rabbitmq:
    image: rabbitmq:3.6.10-alpine
    ports:
      - "5672"
```

You can retrieve a host port bound to container's 5672 port by invoking Johann's `port` method as follows:

```java
ContainerPort containerPort = compose.port("rabbitmq", 5672);
int port = containerPort.getPort();
```

You can even use `ContainerPort::format` method to create proper URL address with one-liner:

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
