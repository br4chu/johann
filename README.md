# Introduction

Johann is a simple Java library that communicates with docker-compose CLI.

# Getting started

Just add Johann as a maven/gradle dependency and start using it. See [Example usage](#example-usage) for more information.

Maven dependency:
```xml
<dependency>
    <groupId>io.brachu</groupId>
    <artifactId>johann</artifactId>
    <version>1.3.0</version>
</dependency>
```

Gradle dependency:
```groovy
implementation 'io.brachu:johann:1.3.0'
```

## Requirements

* JDK8+ environment
* Docker Engine 17.06+
* Docker Compose 1.18+

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

#### Choosing custom working directory for the docker-compose process

```java
DockerCompose compose = DockerCompose.builder()
        .classpath()
        .workDir("/my/custom/compose/workdir")
        .build();
```

All relative paths in a compose file are resolved against working directory of docker-compose process. By default working directory is set to `null`
which means that it will be the same as working directory of Java process that starts the docker-compose process.

#### Starting and waiting for compose cluster to be up

```java
compose.up();
compose.waitForCluster(1, TimeUnit.MINUTES);
```

Calling `up` method is equivalent to executing `docker-compose up -d` command.

`waitForCluster` method waits for all containers within a cluster to be either healthy or, if they have no health check, running.
For most consistent results in integration tests, all your containers should implement health checks.

You can read more about container health checks [here](https://docs.docker.com/engine/reference/builder/#healthcheck).

#### Customizing cluster startup

you can customize behaviour of `up` method by supplying it with a `UpConfig` object.

`UpConfig` object has following properties:

| Property     | CLI equivalent | default value
| ------------ | -------------- | --------------
| `forceBuild` | `--build`      | `false`

Example usage:

```java
UpConfig config = UpConfig.defaults().withForceBuild(true);
compose.up(config);
```

#### Shutting compose cluster down gracefully

```java
compose.down();
```

Calling `down` method is equivalent to executing `docker-compose down -v` command.

#### Customizing cluster shutdown

You can customize behaviour of `down` method by supplying it with a `DownConfig` object.

`DownConfig` object has following properties:

| Property         | CLI equivalent     | default value
| ---------------- | ------------------ | -------------------------------------
| `removeImages`   | `--rmi`            | `NONE`
| `removeVolumes`  | `-v`               | `true`
| `removeOrphans`  | `--remove-orphans` | `false`
| `timeoutSeconds` | `-t`               | `10`

Example usage:

```java
DownConfig config = DownConfig.defaults().withRemoveVolumes(false);
compose.down(config);
```

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

#### Stopping and starting multiple services at once

You can stop and start multiple services at once by using varargs-flavoured `stop` & `start` methods. There are also `stopAll` & `startAll` methods
available that can stop and start all services defined in your docker-compose file.

```java
// Order of services matters. Rabbitmq service will be stopped before postgresql.
compose.stop("rabbitmq", "postgresql");
// Postgresql will start before rabbitmq
compose.start("postgresql", "rabbitmq");
```

```java
compose.stopAll()
compose.startAll()
```

#### Passing environment variables to docker-compose

```java
DockerCompose compose = DockerCompose.builder()
    .classpath()
    .env("MY_ENV_VAR", "my value")
    .env("ANOTHER_VAR", "another value")
    .build();
```

#### Assigning project name to your compose cluster

By default, Johann uses implicitly generated project name and passes it to `docker-compose` command via `-p` switch.
You can override this behaviour by passing your own project name to the builder:

```java
DockerCompose compose = DockerCompose.builder()
    .classpath()
    .projectName("customProjectName")
    .build();
```

#### Implicit project name generation

When running `docker-compose up` without `-p` switch, your compose cluster will be assigned a project name equal to the name of directory containing your
`docker-compose.yml` file. This may lead to problems when running multiple clusters at once. Johann tries to handle this case by always passing
a project name to `docker-compose` CLI. This project name can be given explicitly via builder pattern or can be generated implicitly.

The sources of implicit project name are given below, ordered by priority:

1. `System.getProperty("maven.dockerCompose.project")` (will be used if returned value is not blank)
2. `System.getenv("COMPOSE_PROJECT_NAME")` (will be used if returned value is not blank)
3. Random ASCII string generator

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

You can easily retrieve container's IP address for rabbitmq service:

```java
String ip = compose.containerIp("rabbitmq");
```

If a container is bound to multiple networks, you can pass the network name as a second argument to the `ip` method:

```java
String ip = compose.containerIp("rabbitmq", "my_custom_network");
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

You can retrieve a host port bound to container's 5672 port by invoking Johann's `port` method:

```java
ContainerPort containerPort = compose.port("rabbitmq", 5672);
int port = containerPort.getPort();
```

You can even use `ContainerPort::format` method to create proper URL address with one-liner:

```java
String url = compose.port("rabbitmq", 5672).format("tcp://$HOST:$PORT");
```

#### Redirecting logs from containers to standard output and standard error

```java
compose.up();
compose.followLogs();
```

Logs from containers will be passed to `System.out` and `System.err` of currently running JVM until cluster is shut down or currently running JVM
exits.

Note that cluster should be up before calling `followLogs` method. It may return prematurely otherwise and won't capture any logs.

#### Redirecting logs from containers to custom `PrintStream` objects

```
PrintStream out = new PrintStream(...);
PrintStream err = new PrintStream(...);

compose.up();
compose.followLogs(out, err);
```

### Remote docker engine

Johann can connect to a remote Docker Engine if `DOCKER_HOST` environment variable is passed to the Java process that runs Johann.
`DOCKER_TLS_VERIFY` and `DOCKER_CERT_PATH` variables are also supported.

You can read more about these variables [here](https://docs.docker.com/compose/production/#running-compose-on-a-single-server).

## Running test suite

Running tests located in this repository requires you to install docker and docker-compose on your local machine. Also, assuming you are running Linux distro,
user running the tests must be added to the `docker` group. Version requirements are posted at the top of this README.
