package services

import javax.inject.{Inject, Singleton}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import play.api.libs.json.{JsValue, Json}
import models.NotificationEvent
import scala.concurrent.ExecutionContext
import play.api.libs.streams.ActorFlow
import org.apache.pekko.actor.{Actor, ActorRef, Props}
import play.api.mvc.WebSocket

@Singleton
class WebSocketService @Inject()(implicit
                                 system: ActorSystem,
                                 materializer: Materializer,
                                 ec: ExecutionContext) {

  private var connections = Map.empty[Long, Set[ActorRef]]

  def addConnection(userId: Long, actorRef: ActorRef): Unit = {
    synchronized {
      val userConnections = connections.getOrElse(userId, Set.empty)
      connections = connections + (userId -> (userConnections + actorRef))
    }
  }

  def removeConnection(userId: Long, actorRef: ActorRef): Unit = {
    synchronized {
      val userConnections = connections.getOrElse(userId, Set.empty)
      val updatedConnections = userConnections - actorRef
      if (updatedConnections.isEmpty) {
        connections = connections - userId
      } else {
        connections = connections + (userId -> updatedConnections)
      }
    }
  }

  def sendNotification(userId: Long, event: NotificationEvent): Unit = {
    val userConnections = connections.getOrElse(userId, Set.empty)
    val message = Json.toJson(event.toJson)
    userConnections.foreach(_ ! message)
  }

  def socket(userId: Long): WebSocket = {
    WebSocket.accept[JsValue, JsValue] { _ =>
      ActorFlow.actorRef { out =>
        WebSocketActor.props(out, this, userId)
      }
    }
  }

  object WebSocketActor {
    def props(out: ActorRef, service: WebSocketService, userId: Long): Props =
      Props(new WebSocketActor(out, service, userId))
  }

  class WebSocketActor(out: ActorRef, service: WebSocketService, userId: Long) extends Actor {

    override def preStart(): Unit = {
      service.addConnection(userId, out)
    }

    override def postStop(): Unit = {
      service.removeConnection(userId, out)
    }

    def receive: Receive = {
      case msg: JsValue =>
        out ! msg
    }
  }
}