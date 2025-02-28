package services

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.libs.json.JsObject
import models._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationService @Inject()(
                                     ws: WSClient,
                                     config: Configuration,
                                     webSocketService: WebSocketService
                                   )(implicit ec: ExecutionContext) {

  private val webhookEndpoint = config.get[String]("webhook.endpoint")

  def notifyEvent(event: NotificationEvent): Future[Boolean] = {
    // Send notification via webhook
    sendNotification(event.toJson)
  }

  def notifyEventWithWebSocket(event: NotificationEvent, userId: Long): Future[Boolean] = {
    // Send notification via webhook
    val webhookFuture = sendNotification(event.toJson)

    // Send notification via WebSocket
    webSocketService.sendNotification(userId, event)

    // Return the result of the webhook notification
    webhookFuture
  }

  def notifyMeetingCreation(organizer: User, meeting: Meeting, participants: Seq[User]): Future[Seq[Boolean]] = {
    val organizerEvent = MeetingCreatedEvent(organizer, meeting)
    val participantEvents = participants.map(participant =>
      MeetingInvitationEvent(participant, meeting, organizer))

    // Notify organizer via both webhook and WebSocket
    for {
      organizerNotified <- notifyEventWithWebSocket(organizerEvent, organizer.id.get)
      // Notify participants via webhook only
      participantsNotified <- Future.sequence(participantEvents.map(notifyEvent))
    } yield organizerNotified +: participantsNotified
  }

  def notifyMeetingUpdate(organizer: User, meeting: Meeting, participants: Seq[User]): Future[Seq[Boolean]] = {
    val organizerEvent = MeetingUpdatedEvent(organizer, meeting, isOrganizer = true)
    val participantEvents = participants.map(participant =>
      MeetingUpdatedEvent(participant, meeting, isOrganizer = false))

    // Notify organizer via both webhook and WebSocket
    for {
      organizerNotified <- notifyEventWithWebSocket(organizerEvent, organizer.id.get)
      // Notify participants via webhook only
      participantsNotified <- Future.sequence(participantEvents.map(notifyEvent))
    } yield organizerNotified +: participantsNotified
  }

  def notifyMeetingDeletion(organizer: User, meeting: Meeting, participants: Seq[User]): Future[Seq[Boolean]] = {
    val organizerEvent = MeetingDeletedEvent(organizer, meeting, isOrganizer = true)
    val participantEvents = participants.map(participant =>
      MeetingDeletedEvent(participant, meeting, isOrganizer = false))

    // Notify organizer via both webhook and WebSocket
    for {
      organizerNotified <- notifyEventWithWebSocket(organizerEvent, organizer.id.get)
      // Notify participants via webhook only
      participantsNotified <- Future.sequence(participantEvents.map(notifyEvent))
    } yield organizerNotified +: participantsNotified
  }

  private def sendNotification(data: JsObject): Future[Boolean] = {
    ws.url(webhookEndpoint)
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(data)
      .map(response => response.status >= 200 && response.status < 300)
      .recover { case _ => false }
  }
}