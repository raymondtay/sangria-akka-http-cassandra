import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server._
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.model.{HttpRequest, HttpEntity}

object Routes {

  def generateRowsInCassandra(implicit AS: ActorSystem, AM: ActorMaterializer) = new CassandraWriter

  implicit val dataCsvUnmarshaller : FromRequestUnmarshaller[List[Data]] = Unmarshaller.withMaterializer {
    implicit ex => implicit mat => req : HttpRequest => 
      req.entity.dataBytes.via(Framing.delimiter(ByteString("\n"), Int.MaxValue, true)).map(_.utf8String).runFold(List.empty[Data]){
        (acc, x) => 
          val xs = x.split(",")
          Data(xs(0), xs(1), xs(2)) :: acc
      }
  }

  val csvRoute = 
    (post & path("upload_csv")) {
      entity[List[Data]](dataCsvUnmarshaller) { 
        csvData => 
          println(csvData)
          complete(OK)
      }
    }

  def initializeCassandraWithDataRoute(implicit AS: ActorSystem, AM: ActorMaterializer) = 
    (post & path("gendata")) { // run this to generate 1-million rows into Cassandra on your localhost
      generateRowsInCassandra.run()
      complete(OK)
    }

}
