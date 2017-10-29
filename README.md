# Introduction

Johann is a simple Java library giving you the ability to communicate with docker-compose CLI from within JVM.

# Getting started

Once Johann is uploaded to Maven Central, you will be able to add it as a maven/gradle dependency and start using it.

Maven dependency:
```xml
<dependency>
    <groupId>co.brachu</groupId>
    <artifactId>johann</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Gradle dependency:
```groovy
compile 'co.brachu:johann:0.1.0-SNAPSHOT'
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

### Remote docker engine

Johann can connect to a remote Docker Engine if `DOCKER_HOST` is passed to the Java process that runs Johann.
`DOCKER_TLS_VERIFY` and `DOCKER_CERT_PATH` variables are also supported.

You can read more about these variables [here](https://docs.docker.com/compose/production/#running-compose-on-a-single-server).
