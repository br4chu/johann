# 1.2.0

Added `workDir` method to `DockerCompose`'s instance builder. New method allows setting a working directory for docker-compose process. All relative
paths in compose file will then be resolved against this directory. By default `workDir` is set to `null` which means that working directory
of docker-compose process will be the same as working directory of Java process that starts it.

Added new methods to the API:
* no-args `followLogs` method starts `docker-compose logs -f` process and passes all the standard & error output of that process to `System.out` and
`System.err` respectively. The compose cluster should be up before this method is called, otherwise docker-compose process may exit prematurely and
no logs will be captured.
* overloaded two-args `followLogs` method does the same but allows you to specify `PrintStream`s to which logs should be redirected.

# 1.1.0

`up()` method no longer throws an exception when a docker-compose cluster is already up. Only INFO level log is printed in such case.

`down()` method and its overloaded variants no longer throw an exception when docker-compose cluster is already down. Only INFO level log is printed in such
case.

`DockerCompose` interface now extends `Closeable`. Users are advised to call `close()` method on their `DockerCompose` instance when they are done with it.
This will release external resources (e.g. opened TCP connections) back to the operating system.

Added new methods to the API:
* varargs-flavoured `stop` method can stop multiple services at once
* varargs-flavoured `start` method can start multiple services at once
* `stopAll` method stops all services defined in a docker-compose file
* `startAll` method starts all services defined in a docker-compose file

Stopping an already stopped service has no effect. The same goes for starting already started service.

# 1.0.1

`waitForCluster` and `waitForService` methods will now throw `JohannTimeoutException` instead of `DockerComposeException` when a timeout occurs. This
should allow library users to better handle such error. Previous exception was too generic and didn't help in distinguishing timeout error from some other,
timeout-unrelated CLI error.

# 1.0.0

Updated dependencies and maven plugins to latest stable releases.

Removed javax.activation dependency as it is now provided in the Spotify's docker-client.

Marked jsr305 dependency as provided.

No new features. Latest (0.8.0) version has been battle tested in multiple projects with great success and will therefore become the first stable release.

All 1.x versions will have no breaking changes.

# 0.8.0

Certain docker-compose operations like up, down, starting and stopping of a single service will be more verbose to tell end-user that something is
actually happening behind scenes.

Added "Automatic-Module-Name" entry to MANIFEST.MF for a better compatibility with JDK9+ downstream projects.

Added javax.activation dependency to work around exceptions in docker-client dependency.

# 0.7.0

Removed kill-before-down option. Users should call `kill()` method manually before calling `down()` if they want to achieve the same result.

# 0.6.0

Increased timeout on all CLI operations from 1 minute to 5 minutes.

Added kill-before-down option to DownConfig.

Updated dependencies to newest stable versions, including docker-client.

Before generating random project name, Johann will try to read value of COMPOSE_PROJECT_NAME environment variable.  
The priority of implicit project name creation is now:
1. `maven.dockerCompose.project` system property
2. `COMPOSE_PROJECT_NAME` environment variable
3. Random ASCII string

# 0.5.1

Explicitly excluded shaded jersey dependencies from docker-client dependency. Dependency clash was causing problems in downstream projects.

# 0.5.0

Renamed `ip` operation to `containerIp` because of ambiguity.

Added overloaded `down` method which accepts a `DownConfig` object.

Used shaded version of Spotify's Docker Client. May reduce dependency problems in downstream projects.

# 0.4.0

New operation:

* `ip` - retrieves container IP address for`` a service within specified network. If network is left unspecified, an IP address from default network will be
retrieved.

# 0.3.0

New operations:

* `stop` - stops a single service within a cluster.
* `start` - starts previously stopped service.
* `waitForService` - waits for a single service to be healthy or running.

# 0.2.0

* More meaningful error messages.

* All runtime exception classes now inherit from a single ancestor class named `JohannException`. This allows for simplier try/catch blocks.

* Added `DockerCompose::getProjectName` method.

* Removed "alreadyStarted" step from the builder. Johann will automatically find out if docker-compose cluster is up or not.

* Added integration with [docker-compose-maven-plugin](https://github.com/br4chu/docker-compose-maven-plugin). If you don't specify project's name via builder, Johann will first try to retrieve project's name
from `maven.dockerCompose.project` system property. If such property doesn't exist or has a blank value, only then random project name will be used. 

# 0.1.0

First feature-complete release.
