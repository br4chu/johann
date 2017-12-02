package io.brachu.johann

import java.util.concurrent.TimeUnit

import io.brachu.johann.exception.DockerComposeException
import spock.lang.Specification

class JohannAcceptanceSpec extends Specification {

    def dockerCompose = DockerCompose.builder()
            .classpath()
            .env('EXTERNAL_MANAGEMENT_PORT', '1337')
            .build()

    def "should run without errors"() {
        when:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)
        dockerCompose.down()

        then:
        noExceptionThrown()
    }

    def "ps command should return all entries defined in docker-compose file"() {
        given:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        when:
        def ids = dockerCompose.ps()

        then:
        ids.size() == 2
        ids.every {
            it.toString().matches('[0-9a-f]{64}')
        }

        cleanup:
        dockerCompose.down()
    }

    def "public port should be retrievable"() {
        given:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        when:
        def containerPort = dockerCompose.port("rabbitmq", 5672)

        then:
        containerPort.port != 5672
        containerPort.format('http://$HOST:$PORT') != 'http://$HOST:$PORT'

        cleanup:
        dockerCompose.down()
    }

    def "should pass environment variables to docker compose"() {
        given:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        when:
        def containerPort = dockerCompose.port("rabbitmq", 15672)

        then:
        containerPort.port == 1337

        cleanup:
        dockerCompose.down()
    }

    def "should show error from docker-compose cli"() {
        given:
        dockerCompose = DockerCompose.builder()
                .classpath()
                .build()

        when:
        dockerCompose.up()

        then:
        def ex = thrown DockerComposeException
        ex.message.startsWith('Non-zero (1) exit code returned from \'docker-compose -f')
    }

    def "should pass project name to docker-compose"() {
        given:
        dockerCompose = DockerCompose.builder()
                .classpath()
                .projectName('_johann-0')
                .env('EXTERNAL_MANAGEMENT_PORT', '1337')
                .build()

        when:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)
        def containerPort = dockerCompose.port("rabbitmq", 15672)

        then:
        containerPort.port == 1337
        dockerCompose.projectName == '_johann-0'

        cleanup:
        dockerCompose.down()
    }

    def "should reject invalid project name"() {
        when:
        DockerCompose.builder()
                .classpath()
                .projectName(";'[]")

        then:
        def ex = thrown IllegalArgumentException
        ex.getMessage() == 'Due to security reasons, projectName must match [a-zA-Z0-9_-]+ regex pattern'
    }

    def "should retrieve project name from docker-compose-maven-plugin system property when specified"() {
        given:
        def expectedProjectName = 'helloFromMaven'
        System.setProperty('maven.dockerCompose.project', expectedProjectName)
        dockerCompose = DockerCompose.builder()
                .classpath()
                .env('EXTERNAL_MANAGEMENT_PORT', '1337')
                .build()

        when:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        then:
        dockerCompose.projectName == expectedProjectName

        cleanup:
        dockerCompose.down()
    }

}
