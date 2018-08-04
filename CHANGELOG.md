# 0.8.0

Certain docker-compose operations like up, down, starting and stopping of a single service will be more verbose to tell end-user that something is
actually happening behind scenes.

Added "Automatic-Module-Name" entry to MANIFEST.MF for a better compatiblity with JDK9+ downstream projects.

Added javax.activation dependency to work around exception in JDK9+ downstream projects.

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
