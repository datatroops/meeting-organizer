package models

import play.api.libs.json._

import java.time.LocalDateTime

case class Meeting(
                    id: Option[Long] = None,
                    title: String,
                    startTime: LocalDateTime,
                    endTime: LocalDateTime,
                    organizerId: Long,
                    participantIds: List[Long] = List.empty,
                    createdAt: Option[LocalDateTime] = None,
                    updatedAt: Option[LocalDateTime] = None
                  )

object Meeting {
  implicit val localDateTimeFormat: Format[LocalDateTime] = new Format[LocalDateTime] {
    override def reads(json: JsValue): JsResult[LocalDateTime] = json.validate[String].map(LocalDateTime.parse)
    override def writes(dt: LocalDateTime): JsValue = JsString(dt.toString)
  }

  implicit val format: Format[Meeting] = Json.format[Meeting]
}