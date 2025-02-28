package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import models.User
import scala.concurrent.{ExecutionContext, Future}
import services.UserService

@Singleton
class UserController @Inject()(
                                cc: ControllerComponents,
                                userService: UserService
                              )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def list(): Action[AnyContent] = Action.async {
    userService.list().map(users => Ok(Json.toJson(users)))
  }

  def get(id: Long): Action[AnyContent] = Action.async {
    userService.getById(id).map {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound(Json.obj("error" -> s"User with id $id not found"))
    }
  }

  def create(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[User].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      user => userService.create(user).map(createdUser => Created(Json.toJson(createdUser)))
    )
  }

  def update(id: Long): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[User].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      user => userService.update(id, user).map {
        case true => Ok(Json.obj("success" -> true))
        case false => NotFound(Json.obj("error" -> s"User with id $id not found"))
      }
    )
  }

  def delete(id: Long): Action[AnyContent] = Action.async {
    userService.delete(id).map {
      case true => Ok(Json.obj("success" -> true))
      case false => NotFound(Json.obj("error" -> s"User with id $id not found"))
    }
  }
}