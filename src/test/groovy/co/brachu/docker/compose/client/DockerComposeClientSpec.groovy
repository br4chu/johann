package co.brachu.docker.compose.client

import java.util.concurrent.TimeUnit

import co.brachu.docker.compose.client.exception.DockerComposeException
import spock.lang.Specification

class DockerComposeClientSpec extends Specification {

    def dockerCompose = DockerCompose.cli()
            .file().classpath()
            .env('EXTERNAL_MANAGEMENT_PORT', '1337')
            .build()

    def "should run without errors"() {
        when:
        dockerCompose.up()
        dockerCompose.waitForCluster(1, TimeUnit.MINUTES);
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
        dockerCompose = DockerCompose.cli()
                .file().classpath()
                .build()

        when:
        dockerCompose.up()

        then:
        def ex = thrown DockerComposeException
        ex.message.startsWith('Non-zero (1) exit code returned from \'docker-compose -f')
    }

}
