package models

import play.api.libs.json.{Format, Json}

case class User(
                 id: Option[Long] = None,
                 name: String,
                 email: String
               )

object User {
  implicit val format: Format[User] = Json.format[User]
}