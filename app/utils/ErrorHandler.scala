package utils

import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent._
import javax.inject.Singleton
import play.api.libs.json.Json

case class ValidationError(message: String)

@Singleton
class ErrorHandler extends HttpErrorHandler {
  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      Status(statusCode)(Json.obj(
        "error" -> message,
        "path" -> request.path
      ))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(
      InternalServerError(Json.obj(
        "error" -> "A server error occurred",
        "path" -> request.path
      ))
    )
  }
}