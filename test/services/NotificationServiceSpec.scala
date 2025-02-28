package services

import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.JsObject
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class NotificationServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {


  implicit val ec: ExecutionContext = ExecutionContext.global

  // Mock dependencies
  val mockWsClient: WSClient = mock[WSClient]
  val mockConfig: Configuration = mock[Configuration]
  val mockWebSocketService: WebSocketService = mock[WebSocketService]
  val mockWsRequest: WSRequest = mock[WSRequest]
  val mockWsResponse: WSResponse = mock[WSResponse]

  // Test webhook URL
  val webhookUrl = "https://webhook.site/b157ec54-9269-4e2f-a649-947c3c9f6edc"

  // Sample data
  val testMeeting = Meeting(
    id = Some(1L),
    title = "Test Meeting",
    startTime = LocalDateTime.of(2025, 3, 1, 10, 0),
    endTime = LocalDateTime.of(2025, 3, 1, 11, 0),
    organizerId = 1L,
    participantIds = List(2L, 3L),
    createdAt = Some(LocalDateTime.now()),
    updatedAt = Some(LocalDateTime.now())
  )

  val organizer = User(
    id = Some(1L),
    name = "John Organizer",
    email = "john@example.com"
  )

  val participant1 = User(
    id = Some(2L),
    name = "Alice Participant",
    email = "alice@example.com"
  )

  val participant2 = User(
    id = Some(3L),
    name = "Bob Participant",
    email = "bob@example.com"
  )

  val participants = Seq(participant1, participant2)

  def resetMocks(): Unit = {
    reset(mockWsClient, mockWebSocketService, mockWsRequest, mockWsResponse)
    when(mockConfig.get[String]("webhook.endpoint")).thenReturn(webhookUrl)
    when(mockWsClient.url(anyString())).thenReturn(mockWsRequest)
    when(mockWsRequest.addHttpHeaders(any())).thenReturn(mockWsRequest)
    when(mockWsRequest.post(any[JsObject])(any())).thenReturn(Future.successful(mockWsResponse))
    when(mockWsResponse.status).thenReturn(200)
  }

  // Set up service for tests
  def setupService(): NotificationService = {
    when(mockConfig.get[String]("webhook.endpoint")).thenReturn(webhookUrl)
    when(mockWsClient.url(anyString())).thenReturn(mockWsRequest)
    when(mockWsRequest.addHttpHeaders(any())).thenReturn(mockWsRequest)
    when(mockWsRequest.post(any[JsObject])(any())).thenReturn(Future.successful(mockWsResponse))
    when(mockWsResponse.status).thenReturn(200)

    new NotificationService(mockWsClient, mockConfig, mockWebSocketService)
  }

  "NotificationService" should {

    "send notifications via webhook" when {
      "notifyEvent is called" in {
        val service = setupService()
        val event = MeetingCreatedEvent(organizer, testMeeting)

        val resultFuture = service.notifyEvent(event)

        whenReady(resultFuture) { result =>
          result shouldBe true
          verify(mockWsClient).url(webhookUrl)
          verify(mockWsRequest).addHttpHeaders("Content-Type" -> "application/json")

          val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
          verify(mockWsRequest).post(jsonCaptor.capture())(any())

          val json = jsonCaptor.getValue
          (json \ "userId").as[Long] shouldBe organizer.id.get
          (json \ "email").as[String] shouldBe organizer.email
          (json \ "type").as[String] shouldBe "meeting_created"
          (json \ "meetingId").as[Long] shouldBe testMeeting.id.get
          (json \ "meetingDetails" \ "title").as[String] shouldBe testMeeting.title
        }
      }

      "handle webhook failures gracefully" in {
        val service = setupService()
        when(mockWsRequest.post(any[JsObject])(any())).thenReturn(Future.failed(new Exception("Network error")))
        val event = MeetingCreatedEvent(organizer, testMeeting)

        val resultFuture = service.notifyEvent(event)

        whenReady(resultFuture) { result =>
          result shouldBe false
        }
      }

      "handle non-success HTTP responses" in {
        val service = setupService()
        when(mockWsResponse.status).thenReturn(500)
        val event = MeetingCreatedEvent(organizer, testMeeting)

        val resultFuture = service.notifyEvent(event)

        whenReady(resultFuture) { result =>
          result shouldBe false
        }
      }
    }

    "send notifications via both webhook and WebSocket" when {
      "notifyEventWithWebSocket is called" in {
        resetMocks()
        val service = setupService()
        val event = MeetingCreatedEvent(organizer, testMeeting)
        val userId = organizer.id.get

        val resultFuture = service.notifyEventWithWebSocket(event, userId)

        whenReady(resultFuture) { result =>
          result shouldBe true
          verify(mockWsClient).url(webhookUrl)
          verify(mockWebSocketService).sendNotification(userId, event)
        }
      }
    }

    "handle meeting event notifications" when {
      "notifyMeetingCreation is called" in {
        resetMocks()
        val service = setupService()

        val resultsFuture = service.notifyMeetingCreation(organizer, testMeeting, participants)

        whenReady(resultsFuture) { results =>
          results.size shouldBe 3  // Organizer + 2 participants
          results.forall(_ == true) shouldBe true

          verify(mockWebSocketService).sendNotification(
            org.mockito.ArgumentMatchers.eq(organizer.id.get),
            org.mockito.ArgumentMatchers.isA(classOf[MeetingCreatedEvent])
          )

          val organizerEventCaptor = ArgumentCaptor.forClass(classOf[MeetingCreatedEvent])
          verify(mockWebSocketService).sendNotification(
            org.mockito.ArgumentMatchers.eq(organizer.id.get),
            organizerEventCaptor.capture()
          )

          val organizerEvent = organizerEventCaptor.getValue
          organizerEvent.user shouldBe organizer
          organizerEvent.meeting shouldBe testMeeting

          verify(mockWsRequest, times(3)).post(any[JsObject])(any())

          verify(mockWebSocketService, times(1)).sendNotification(anyLong(), any[NotificationEvent])
        }
      }

      "notifyMeetingUpdate is called" in {
        resetMocks()
        val service = setupService()

        val resultsFuture = service.notifyMeetingUpdate(organizer, testMeeting, participants)

        whenReady(resultsFuture) { results =>
          results.size shouldBe 3  // Organizer + 2 participants
          results.forall(_ == true) shouldBe true

          val organizerEventCaptor = ArgumentCaptor.forClass(classOf[MeetingUpdatedEvent])
          verify(mockWebSocketService).sendNotification(
            org.mockito.ArgumentMatchers.eq(organizer.id.get),
            organizerEventCaptor.capture()
          )

          val organizerEvent = organizerEventCaptor.getValue
          organizerEvent.user shouldBe organizer
          organizerEvent.meeting shouldBe testMeeting
          organizerEvent.isOrganizer shouldBe true

          verify(mockWsRequest, times(3)).post(any[JsObject])(any())

          verify(mockWebSocketService, times(1)).sendNotification(anyLong(), any[NotificationEvent])
        }
      }

      "notifyMeetingDeletion is called" in {
        resetMocks()
        val service = setupService()

        val resultsFuture = service.notifyMeetingDeletion(organizer, testMeeting, participants)

        whenReady(resultsFuture) { results =>
          results.size shouldBe 3  // Organizer + 2 participants
          results.forall(_ == true) shouldBe true

          val organizerEventCaptor = ArgumentCaptor.forClass(classOf[MeetingDeletedEvent])
          verify(mockWebSocketService).sendNotification(
            org.mockito.ArgumentMatchers.eq(organizer.id.get),
            organizerEventCaptor.capture()
          )

          val organizerEvent = organizerEventCaptor.getValue
          organizerEvent.user shouldBe organizer
          organizerEvent.meeting shouldBe testMeeting
          organizerEvent.isOrganizer shouldBe true

          val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])
          verify(mockWsRequest, times(3)).post(jsonCaptor.capture())(any())

          val capturedJsons = jsonCaptor.getAllValues
          val participantJsons = capturedJsons.stream()
            .filter(json => (json \ "userId").as[Long] != organizer.id.get)
            .toArray

          participantJsons.length shouldBe 2

          verify(mockWebSocketService, times(1)).sendNotification(anyLong(), any[NotificationEvent])
        }
      }
    }
  }
}