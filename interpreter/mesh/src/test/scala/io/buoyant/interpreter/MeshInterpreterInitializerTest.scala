package io.buoyant.interpreter

import com.twitter.finagle.util.LoadService
import com.twitter.finagle.{Path, Stack}
import io.buoyant.config.Parser
import io.buoyant.namer.{InterpreterConfig, InterpreterInitializer}
import org.scalatest.FunSuite

class MeshInterpreterInitializerTest extends FunSuite {
  test("sanity") {
    // ensure it doesn't totally blowup
    val _ = MeshInterpreterConfig(Some(Path.read("/whats/in/a")), Some(Path.read("/default")), None, None)
      .newInterpreter(Stack.Params.empty)
  }

  test("interpreter registration") {
    assert(LoadService[InterpreterInitializer]().exists(_.isInstanceOf[MeshInterpreterInitializer]))
  }

  test("parse config") {
    val yaml = s"""|kind: io.l5d.mesh
                   |experimental: true
                   |dst: /$$/inet/127.1/4321
                   |root: /default
                   |tls:
                   |  commonName: namerd
                   |  caCert: namerd/examples/certs/namerd-cacert.pem
                   |""".stripMargin

    val mapper = Parser.objectMapper(yaml, Iterable(Seq(MeshInterpreterInitializer)))
    val namerd = mapper.readValue[InterpreterConfig](yaml).asInstanceOf[MeshInterpreterConfig]
    mapper.writeValueAsString(namerd) // ensure serialization doesn't blow up
    assert(namerd.dst == Some(Path.read("/$/inet/127.1/4321")))
    assert(namerd.root == Some(Path.read("/default")))
    assert(!namerd.disabled)

    val tls = namerd.tls.get
    assert(tls.commonName == "namerd")
    assert(tls.caCert == Some("namerd/examples/certs/namerd-cacert.pem"))
  }

  test("without experimental") {
    val yaml = s"""|kind: io.l5d.mesh
                   |dst: /$$/inet/127.1/4321
                   |root: /default
                   |tls:
                   |  commonName: namerd
                   |  caCert: namerd/examples/certs/namerd-cacert.pem
                   |""".stripMargin

    val mapper = Parser.objectMapper(yaml, Iterable(Seq(MeshInterpreterInitializer)))
    val namerd = mapper.readValue[InterpreterConfig](yaml).asInstanceOf[MeshInterpreterConfig]
    mapper.writeValueAsString(namerd) // ensure serialization doesn't blow up
    assert(namerd.disabled)
  }
}
