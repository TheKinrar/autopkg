package fr.thekinrar.autopkg
package docker

import cats.effect.*
import com.github.dockerjava.api.command.WaitContainerResultCallback

class Container(id: String, dockerClient: DockerClient) {
  def start(): IO[Unit] = IO {
    dockerClient.client.startContainerCmd(id).exec()
  }

  def waitFor(): IO[Int] = IO {
    val cb = WaitContainerResultCallback()
    dockerClient.client.waitContainerCmd(id).exec(cb)
    cb.awaitStatusCode()
  }
}
