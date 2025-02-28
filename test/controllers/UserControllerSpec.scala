package controllers

import models.User
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
import services.UserService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UserControllerSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar with GuiceOneAppPerSuite {

  val mockUserService: UserService = mock[UserService]
  val cc: ControllerComponents = stubControllerComponents()
  val userController = new UserController(cc, mockUserService)

  val testUser1 = User(Some(1L), "John Doe", "john.doe@example.com")
  val testUser2 = User(Some(2L), "Jane Smith", "jane.smith@example.com")
  val testUserList = Seq(testUser1, testUser2)
  val newUser = User(None, "New User", "new.user@example.com")
  val createdUser = User(Some(3L), "New User", "new.user@example.com")
  val updatedUser = User(Some(1L), "John Updated", "john.updated@example.com")

  "UserController" should {
    "list all users" in {
      when(mockUserService.list()).thenReturn(Future.successful(testUserList))

      val result = userController.list().apply(FakeRequest(GET, "/users"))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      contentAsJson(result) shouldBe Json.toJson(testUserList)
      verify(mockUserService).list()
    }

    "get a user by id" in {
      when(mockUserService.getById(1L)).thenReturn(Future.successful(Some(testUser1)))

      val result = userController.get(1L).apply(FakeRequest(GET, "/users/1"))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      contentAsJson(result) shouldBe Json.toJson(testUser1)
      verify(mockUserService).getById(1L)
    }

    "return NotFound when getting a non-existent user" in {
      when(mockUserService.getById(999L)).thenReturn(Future.successful(None))

      val result = userController.get(999L).apply(FakeRequest(GET, "/users/999"))

      status(result) shouldBe NOT_FOUND
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "error").as[String] shouldBe "User with id 999 not found"
      verify(mockUserService).getById(999L)
    }

    "create a new user" in {
      when(mockUserService.create(any[User])).thenReturn(Future.successful(createdUser))

      val request = FakeRequest(POST, "/users")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.toJson(newUser))

      val result = userController.create().apply(request)

      status(result) shouldBe CREATED
      contentType(result) shouldBe Some("application/json")
      contentAsJson(result) shouldBe Json.toJson(createdUser)
      verify(mockUserService).create(any[User])
    }

    "return BadRequest when creating a user with invalid data" in {
      val invalidJson = Json.obj(
        "invalid_field" -> "This is not a valid user field",
        "another_invalid" -> 123
      )

      val request = FakeRequest(POST, "/users")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(invalidJson)

      val result = userController.create().apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("application/json")

    }

    "update an existing user" in {
      when(mockUserService.update(anyLong(), any[User])).thenReturn(Future.successful(true))

      val request = FakeRequest(PUT, "/users/1")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.toJson(updatedUser))

      val result = userController.update(1L).apply(request)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "success").as[Boolean] shouldBe true
      verify(mockUserService).update(anyLong(), any[User])
    }

    "return NotFound when updating a non-existent user" in {
      when(mockUserService.update(anyLong(), any[User])).thenReturn(Future.successful(false))

      val nonExistentId = 999L
      val request = FakeRequest(PUT, s"/users/$nonExistentId")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.toJson(updatedUser.copy(id = Some(nonExistentId))))

      val result = userController.update(nonExistentId).apply(request)

      status(result) shouldBe NOT_FOUND
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "error").as[String] shouldBe s"User with id $nonExistentId not found"
      verify(mockUserService).update(org.mockito.ArgumentMatchers.eq(nonExistentId), any[User])
    }

    "return BadRequest when updating a user with invalid data" in {
      val invalidJson = Json.obj("name" -> "Invalid User")

      val request = FakeRequest(PUT, "/users/1")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(invalidJson)

      val result = userController.update(1L).apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "error").toOption.isDefined shouldBe true
    }

    "delete an existing user" in {
      when(mockUserService.delete(1L)).thenReturn(Future.successful(true))

      val result = userController.delete(1L).apply(FakeRequest(DELETE, "/users/1"))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "success").as[Boolean] shouldBe true
      verify(mockUserService).delete(1L)
    }

    "return NotFound when deleting a non-existent user" in {
      when(mockUserService.delete(999L)).thenReturn(Future.successful(false))

      val result = userController.delete(999L).apply(FakeRequest(DELETE, "/users/999"))

      status(result) shouldBe NOT_FOUND
      contentType(result) shouldBe Some("application/json")
      (contentAsJson(result) \ "error").as[String] shouldBe "User with id 999 not found"
      verify(mockUserService).delete(999L)
    }
  }
}