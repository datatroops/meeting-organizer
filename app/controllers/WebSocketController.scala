package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json.JsValue
import services.WebSocketService
import scala.concurrent.ExecutionContext

@Singleton
class WebSocketController @Inject()(
                                     cc: ControllerComponents,
                                     webSocketService: WebSocketService
                                   )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // WebSocket endpoint for user notifications with userId path parameter
  def socket(userId: Long) = Action { implicit request =>
    Ok(views.html.websocket(userId))
  }

  // Actual WebSocket handler
  def ws(userId: Long) = webSocketService.socket(userId)
}