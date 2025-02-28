package controllers

import models.{Meeting, User}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import services.{MeetingService, NotificationService}
import utils.ValidationError

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.time.LocalDateTime

class MeetingControllerSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar with GuiceOneAppPerSuite {

  val mockMeetingService: MeetingService = mock[MeetingService]
  val mockNotificationService: NotificationService = mock[NotificationService]
  val cc: ControllerComponents = stubControllerComponents()
  val meetingController = new MeetingController(cc, mockMeetingService)

  // Test users
  val organizer = User(Some(1L), "John Doe", "john.doe@example.com")
  val participant1 = User(Some(2L), "Jane Smith", "jane.smith@example.com")
  val participant2 = User(Some(3L), "Bob Johnson", "bob.johnson@example.com")

  // Test meetings
  val now = LocalDateTime.now()
  val tomorrow = now.plusDays(1)
  val dayAfterTomorrow = now.plusDays(2)

  val testMeeting1 = Meeting(
    Some(1L),
    "Team Meeting",
    tomorrow,
    tomorrow.plusHours(1),
    organizer.id.get,
    List(participant1.id.get, participant2.id.get),
    Some(now),
    Some(now)
  )

  val testMeeting2 = Meeting(
    Some(2L),
    "Project Review",
    dayAfterTomorrow,
    dayAfterTomorrow.plusHours(2),
    organizer.id.get,
    List(participant1.id.get),
    Some(now),
    Some(now)
  )

  val testMeetingList = Seq(testMeeting1, testMeeting2)

  val newMeeting = Meeting(
    None,
    "New Meeting",
    tomorrow.plusDays(1),
    tomorrow.plusDays(1).plusHours(1),
    organizer.id.get,
    List(participant1.id.get)
  )

  val createdMeeting = newMeeting.copy(id = Some(3L))

  val updatedMeeting = testMeeting1.copy(
    title = "Updated Team Meeting",
    startTime = tomorrow.plusHours(2),
    endTime = tomorrow.plusHours(3)
  )

  "MeetingController" should {
    "list all meetings" in {
      when(mockMeetingService.list()).thenReturn(Future.successful(testMeetingList))

      val result = meetingController.list().apply(FakeRequest(GET, "/meetings"))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      contentAsJson(result) shouldBe Json.toJson(testMeetingList)
      verify(mockMeetingService).list()
    }

    "get a meeting by id" in {
      when(mockMeetingService.getById(1L)).thenReturn(Future.successful(Some(testMeeting1)))

      val result = meetingController.get(1L).apply(FakeRequest(GET, "/meetings/1"))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      contentAsJson(result) shouldBe Json.toJson(testMeeting1)
      verify(mockMeetingService).getById(1L)
    }

    "return NotFound when getting a non-existent meeting" in {
      when(mockMeetingService.getById(999L)).thenReturn(Future.successful(None))

      val result = meetingController.get(999L).apply(FakeRequest(GET, "/meetings/999"))

      status(result) shouldBe NOT_FOUND
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "error").as[String] shouldBe "Meeting with id 999 not found"
      verify(mockMeetingService).getById(999L)
    }

    "create a new meeting" in {
      when(mockMeetingService.create(any[Meeting])).thenReturn(Future.successful(Right(createdMeeting)))

      val request = FakeRequest(POST, "/meetings")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.toJson(newMeeting))

      val result = meetingController.create().apply(request)

      status(result) shouldBe CREATED
      contentType(result) shouldBe Some("application/json")
      contentAsJson(result) shouldBe Json.toJson(createdMeeting)
      verify(mockMeetingService).create(any[Meeting])
    }

    "return BadRequest when creating a meeting with validation errors" in {
      val errorMessage = "Start time must be before end time"
      when(mockMeetingService.create(any[Meeting])).thenReturn(
        Future.successful(Left(ValidationError(errorMessage)))
      )

      val request = FakeRequest(POST, "/meetings")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.toJson(newMeeting))

      val result = meetingController.create().apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "error").as[String] shouldBe errorMessage
    }

    "return BadRequest when creating a meeting with invalid data" in {
      val invalidJson = Json.obj(
        "title" -> "Invalid Meeting",
        "organizerId" -> 1
        // Missing required fields
      )

      val request = FakeRequest(POST, "/meetings")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(invalidJson)

      val result = meetingController.create().apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "error").toOption.isDefined shouldBe true
    }

    "update an existing meeting" in {
      when(mockMeetingService.update(anyLong(), any[Meeting])).thenReturn(Future.successful(Right(true)))

      val request = FakeRequest(PUT, "/meetings/1")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.toJson(updatedMeeting))

      val result = meetingController.update(1L).apply(request)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "success").as[Boolean] shouldBe true
      verify(mockMeetingService).update(anyLong(), any[Meeting])
    }

    "return NotFound when updating a non-existent meeting" in {
      when(mockMeetingService.update(anyLong(), any[Meeting])).thenReturn(Future.successful(Right(false)))

      val nonExistentId = 999L
      val request = FakeRequest(PUT, s"/meetings/$nonExistentId")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.toJson(updatedMeeting.copy(id = Some(nonExistentId))))

      val result = meetingController.update(nonExistentId).apply(request)

      status(result) shouldBe NOT_FOUND
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "error").as[String] shouldBe s"Meeting with id $nonExistentId not found"
      verify(mockMeetingService).update(org.mockito.ArgumentMatchers.eq(nonExistentId), any[Meeting])
    }

    "return BadRequest when updating a meeting with validation errors" in {
      val errorMessage = "Start time must be before end time"
      when(mockMeetingService.update(anyLong(), any[Meeting])).thenReturn(
        Future.successful(Left(ValidationError(errorMessage)))
      )

      val request = FakeRequest(PUT, "/meetings/1")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.toJson(updatedMeeting))

      val result = meetingController.update(1L).apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "error").as[String] shouldBe errorMessage
    }

    "return BadRequest when updating a meeting with invalid data" in {
      val invalidJson = Json.obj("title" -> "Invalid Meeting")

      val request = FakeRequest(PUT, "/meetings/1")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(invalidJson)

      val result = meetingController.update(1L).apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "error").toOption.isDefined shouldBe true
    }

    "delete an existing meeting" in {
      when(mockMeetingService.delete(1L)).thenReturn(Future.successful(true))

      val result = meetingController.delete(1L).apply(FakeRequest(DELETE, "/meetings/1"))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "success").as[Boolean] shouldBe true
      verify(mockMeetingService).delete(1L)
    }

    "return NotFound when deleting a non-existent meeting" in {
      when(mockMeetingService.delete(999L)).thenReturn(Future.successful(false))

      val result = meetingController.delete(999L).apply(FakeRequest(DELETE, "/meetings/999"))

      status(result) shouldBe NOT_FOUND
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "error").as[String] shouldBe "Meeting with id 999 not found"
      verify(mockMeetingService).delete(999L)
    }
  }
}