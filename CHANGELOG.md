# 0.4.0

New operation:

* `ip` - retrieves container IP address for a service within specified network. If network is left unspecified, an IP address from default network will be
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
