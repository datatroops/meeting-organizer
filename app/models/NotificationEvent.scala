package models

import play.api.libs.json.{JsObject, Json}

sealed trait NotificationEvent {
  def eventType: String
  def user: User
  def meeting: Meeting
  def toJson: JsObject
  def formatMessage: String
}

case class MeetingCreatedEvent(organizer: User, meeting: Meeting) extends NotificationEvent {
  override def eventType: String = "meeting_created"
  override def user: User = organizer
  override def formatMessage: String =
    s"Hey ${organizer.name}, you have successfully created the meeting with details: " +
      s"Title: ${meeting.title}, Start: ${meeting.startTime}, End: ${meeting.endTime}"

  override def toJson: JsObject = Json.obj(
    "userId" -> organizer.id.get,
    "email" -> organizer.email,
    "type" -> eventType,
    "message" -> formatMessage,
    "meetingId" -> meeting.id.get,
    "meetingDetails" -> Json.obj(
      "title" -> meeting.title,
      "startTime" -> meeting.startTime.toString,
      "endTime" -> meeting.endTime.toString
    )
  )
}

case class MeetingInvitationEvent(participant: User, meeting: Meeting, organizer: User) extends NotificationEvent {
  override def eventType: String = "meeting_invitation"
  override def user: User = participant
  override def formatMessage: String =
    s"Hey ${participant.name}, a meeting has been created by ${organizer.name} and you " +
      s"have been invited. Here are the meeting details: Title: ${meeting.title}, " +
      s"Start: ${meeting.startTime}, End: ${meeting.endTime}"

  override def toJson: JsObject = Json.obj(
    "userId" -> participant.id.get,
    "email" -> participant.email,
    "type" -> eventType,
    "message" -> formatMessage,
    "meetingId" -> meeting.id.get,
    "organizerId" -> organizer.id.get,
    "organizerName" -> organizer.name,
    "meetingDetails" -> Json.obj(
      "title" -> meeting.title,
      "startTime" -> meeting.startTime.toString,
      "endTime" -> meeting.endTime.toString
    )
  )
}

case class MeetingUpdatedEvent(user: User, meeting: Meeting, isOrganizer: Boolean) extends NotificationEvent {
  override def eventType: String = "meeting_updated"
  override def formatMessage: String =
    if (isOrganizer) {
      s"Hey ${user.name}, you have successfully updated the meeting with details: " +
        s"Title: ${meeting.title}, Start: ${meeting.startTime}, End: ${meeting.endTime}"
    } else {
      s"Hey ${user.name}, a meeting you're invited to has been updated. " +
        s"Here are the new details: Title: ${meeting.title}, " +
        s"Start: ${meeting.startTime}, End: ${meeting.endTime}"
    }

  override def toJson: JsObject = Json.obj(
    "userId" -> user.id.get,
    "email" -> user.email,
    "type" -> eventType,
    "message" -> formatMessage,
    "meetingId" -> meeting.id.get,
    "isOrganizer" -> isOrganizer,
    "meetingDetails" -> Json.obj(
      "title" -> meeting.title,
      "startTime" -> meeting.startTime.toString,
      "endTime" -> meeting.endTime.toString
    )
  )
}

case class MeetingDeletedEvent(user: User, meeting: Meeting, isOrganizer: Boolean) extends NotificationEvent {
  override def eventType: String = "meeting_deleted"
  override def formatMessage: String =
    if (isOrganizer) {
      s"Hey ${user.name}, you have successfully deleted the meeting: ${meeting.title}"
    } else {
      s"Hey ${user.name}, a meeting you were invited to has been cancelled: ${meeting.title}"
    }

  override def toJson: JsObject = Json.obj(
    "userId" -> user.id.get,
    "email" -> user.email,
    "type" -> eventType,
    "message" -> formatMessage,
    "meetingId" -> meeting.id.get,
    "isOrganizer" -> isOrganizer,
    "meetingDetails" -> Json.obj(
      "title" -> meeting.title,
      "startTime" -> meeting.startTime.toString,
      "endTime" -> meeting.endTime.toString
    )
  )
}

