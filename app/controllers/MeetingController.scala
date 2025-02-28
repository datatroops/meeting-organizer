package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import models.Meeting
import services.MeetingService

@Singleton
class MeetingController @Inject()(
                                   cc: ControllerComponents,
                                   meetingService: MeetingService
                                 )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def list(): Action[AnyContent] = Action.async {
    meetingService.list().map(meetings => Ok(Json.toJson(meetings)))
  }

  def get(id: Long): Action[AnyContent] = Action.async {
    meetingService.getById(id).map {
      case Some(meeting) => Ok(Json.toJson(meeting))
      case None => NotFound(Json.obj("error" -> s"Meeting with id $id not found"))
    }
  }

  def create(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Meeting].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      meeting => meetingService.create(meeting).map {
        case Right(createdMeeting) => Created(Json.toJson(createdMeeting))
        case Left(error) => BadRequest(Json.obj("error" -> error.message))
      }
    )
  }

  def update(id: Long): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Meeting].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      meeting => meetingService.update(id, meeting).map {
        case Right(true) => Ok(Json.obj("success" -> true))
        case Right(false) => NotFound(Json.obj("error" -> s"Meeting with id $id not found"))
        case Left(error) => BadRequest(Json.obj("error" -> error.message))
      }
    )
  }

  def delete(id: Long): Action[AnyContent] = Action.async {
    meetingService.delete(id).map {
      case true => Ok(Json.obj("success" -> true))
      case false => NotFound(Json.obj("error" -> s"Meeting with id $id not found"))
    }
  }
}