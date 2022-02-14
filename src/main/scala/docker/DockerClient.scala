package fr.thekinrar.autopkg
package docker

import cats.effect.*
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.{Bind, HostConfig, Volume}
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientImpl}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import io.circe.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.client.*
import org.http4s.syntax.literals.uri

class DockerClient {
  private val clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
  private val httpClient = ApacheDockerHttpClient.Builder()
    .dockerHost(clientConfig.getDockerHost)
    .sslConfig(clientConfig.getSSLConfig)
    .build()
  val client = DockerClientImpl.getInstance(clientConfig, httpClient)

  def createContainer(path: String): IO[Container] = IO {
    Container(
      client.createContainerCmd("registry.kinrar.io/autopkg-builder")
        .withHostConfig(HostConfig().withBinds(Bind(path, Volume("/var/lib/builder"))))
        .exec()
        .getId,
      this
    )
  }
}
