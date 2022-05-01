package io.brachu.johann

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

import io.brachu.johann.exception.DockerComposeException
import io.brachu.johann.exception.JohannTimeoutException
import spock.lang.Specification

class JohannAcceptanceSpec extends Specification {

    def dockerCompose = DockerCompose.builder()
            .classpath()
            .env('EXTERNAL_MANAGEMENT_PORT', '1337')
            .build()

    def cleanup() {
        dockerCompose.close()
    }

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
            it.toString().matches('[\\da-f]{64}')
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
                .env('EXTERNAL_MANAGEMENT_PORT', 'nonnumber')
                .build()

        when:
        dockerCompose.up()

        then:
        def ex = thrown DockerComposeException
        (ex.message =~ /Non-zero \(\d+\) exit code returned from 'docker-compose/).size() == 1
    }

    def "should pass project name to docker-compose"() {
        given:
        dockerCompose = DockerCompose.builder()
                .classpath()
                .projectName('johann-0')
                .env('EXTERNAL_MANAGEMENT_PORT', '1337')
                .build()

        when:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)
        def containerPort = dockerCompose.port("rabbitmq", 15672)

        then:
        containerPort.port == 1337
        dockerCompose.projectName == 'johann-0'

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
        def expectedProjectName = 'hellofrommaven'
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
        System.clearProperty('maven.dockerCompose.project')
        dockerCompose.down()
    }

    def "should return meaningful error message when a private port is not bound to any of the host's ports"() {
        when:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)
        dockerCompose.port("postgresql", 5433)

        then:
        def ex = thrown DockerComposeException
        ex.getMessage() == "No host port is bound to 'postgresql' container's 5433 tcp port."

        cleanup:
        dockerCompose.down()
    }

    def "timeout should result in a meaningful exception"() {
        when:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.SECONDS)

        then:
        thrown JohannTimeoutException

        cleanup:
        if (dockerCompose.isUp()) {
            dockerCompose.down()
        }
    }

    def "should stop a single service"() {
        given:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        when:
        dockerCompose.stop('rabbitmq')

        and:
        dockerCompose.port('rabbitmq', 5672)

        then:
        thrown DockerComposeException

        cleanup:
        dockerCompose.down()
    }

    def "should stop multiple services at once"() {
        given:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        when:
        dockerCompose.stop('rabbitmq', 'postgresql')

        and:
        dockerCompose.port('rabbitmq', 5672)

        then:
        thrown DockerComposeException

        cleanup:
        dockerCompose.down()
    }

    def "should stop all services"() {
        given:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        when:
        dockerCompose.stopAll()

        and:
        dockerCompose.port('rabbitmq', 5432)

        then:
        thrown DockerComposeException

        cleanup:
        dockerCompose.down()
    }

    def "should stop and start a single service"() {
        given:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        when:
        dockerCompose.stop('rabbitmq')
        dockerCompose.start('rabbitmq')
        dockerCompose.waitForService('rabbitmq', 1, TimeUnit.MINUTES)

        and:
        def containerPort = dockerCompose.port('rabbitmq', 15672)

        then:
        containerPort.port == 1337

        cleanup:
        dockerCompose.down()
    }

    def "should stop and start multiple services at once"() {
        given:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        when:
        dockerCompose.stop('rabbitmq', 'postgresql')
        dockerCompose.start('rabbitmq', 'postgresql')
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        and:
        def containerPort = dockerCompose.port('rabbitmq', 15672)

        then:
        containerPort.port == 1337

        cleanup:
        dockerCompose.down()
    }

    def "should stop and start all services"() {
        given:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        when:
        dockerCompose.stopAll()
        dockerCompose.startAll()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        and:
        def containerPort = dockerCompose.port('rabbitmq', 15672)

        then:
        containerPort.port == 1337

        cleanup:
        dockerCompose.down()
    }

    def "cluster should be up even if all services are stopped"() {
        given:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        when:
        dockerCompose.stopAll()

        then:
        dockerCompose.isUp()

        cleanup:
        dockerCompose.down()
    }

    def "should be able to retrieve container ip"() {
        given:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        expect:
        dockerCompose.containerIp('rabbitmq') =~ /\d+\.\d+\.\d+\.\d+/
        dockerCompose.containerIp('postgresql') =~ /\d+\.\d+\.\d+\.\d+/

        cleanup:
        dockerCompose.down()
    }

    def "should be able to change working directory of a docker-compose process"() {
        given:
        dockerCompose = DockerCompose.builder()
                .classpath("/docker-compose.short.yml")
                .workDir(new File("./src/test/resources"))
                .build()

        when:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        then:
        dockerCompose.isUp()

        cleanup:
        dockerCompose.down()
    }

    def "should fail merging compose file without providing correct working directory of a docker-compose process"() {
        given:
        dockerCompose = DockerCompose.builder()
                .classpath("/docker-compose.short.yml")
                .build()

        when:
        dockerCompose.up()

        then:
        def ex = thrown DockerComposeException
        ex.message.toLowerCase().contains("no such file or directory")
    }

    def "should dump logs to buffer after following them"() {
        given:
        def baos = new ByteArrayOutputStream()
        def printStream = new PrintStream(baos)

        when:
        dockerCompose.up()
        dockerCompose.followLogs(printStream, printStream)
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        then:
        def logs = baos.toString(StandardCharsets.UTF_8.name())
        logs.contains("/var/lib/postgresql/data")

        cleanup:
        dockerCompose.down()
    }

    def "should build an image if build is forced"() {
        given:
        dockerCompose = DockerCompose.builder()
                .classpath("/docker-compose.build.yml")
                .workDir("src/test/resources")
                .build()

        and:
        def upConfig = UpConfig.defaults().withForceBuild(true)

        when:
        dockerCompose.up(upConfig)
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES)

        then:
        dockerCompose.isUp()

        cleanup:
        dockerCompose.down()
    }

}
