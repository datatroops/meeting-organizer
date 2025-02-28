package services

import javax.inject.{Inject, Singleton}
import models.Meeting
import repositories.{MeetingRepository, UserRepository}
import utils.ValidationError
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MeetingService @Inject()(
                                meetingRepository: MeetingRepository,
                                userRepository: UserRepository,
                                notificationService: NotificationService
                              )(implicit ec: ExecutionContext) {

  def create(meeting: Meeting): Future[Either[ValidationError, Meeting]] = {
    validateMeeting(meeting).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(_) =>
        meetingRepository.create(meeting).flatMap { createdMeeting =>
          for {
            organizerOpt <- userRepository.getById(meeting.organizerId)
            participants <- userRepository.getByIds(meeting.participantIds)
            _ <- organizerOpt match {
              case Some(organizer) =>
                notificationService.notifyMeetingCreation(
                  organizer,
                  createdMeeting,
                  participants
                ).recover { case _ => Seq.empty[Boolean] }
              case None =>
                Future.successful(Seq.empty[Boolean])
            }
          } yield Right(createdMeeting)
        }
    }
  }

  def list(): Future[Seq[Meeting]] = meetingRepository.list()

  def getById(id: Long): Future[Option[Meeting]] = meetingRepository.getById(id)

  def update(id: Long, meeting: Meeting): Future[Either[ValidationError, Boolean]] = {
    // First check if the meeting exists
    meetingRepository.getById(id).flatMap {
      case None =>
        Future.successful(Left(ValidationError(s"Meeting with ID $id does not exist")))
      case Some(_) =>
        // Then proceed with validation
        validateMeeting(meeting).flatMap {
          case Left(error) =>
            Future.successful(Left(error))
          case Right(_) =>
            // Ensure we're passing the correct ID to the update
            val meetingWithId = meeting.copy(id = Some(id))
            meetingRepository.update(id, meetingWithId).flatMap { result =>
              if (result) {
                for {
                  organizerOpt <- userRepository.getById(meeting.organizerId)
                  participants <- userRepository.getByIds(meeting.participantIds)
                  // Get the updated meeting to send in notifications
                  updatedMeetingOpt <- meetingRepository.getById(id)
                  _ <- (organizerOpt, updatedMeetingOpt) match {
                    case (Some(organizer), Some(updatedMeeting)) =>
                      notificationService.notifyMeetingUpdate(
                        organizer,
                        updatedMeeting,
                        participants
                      ).recover { case _ => Seq.empty[Boolean] }
                    case _ =>
                      Future.successful(Seq.empty[Boolean])
                  }
                } yield Right(true)
              } else {
                Future.successful(Right(false))
              }
            }
        }
    }
  }

  def delete(id: Long): Future[Boolean] = {
    meetingRepository.getById(id).flatMap {
      case Some(meeting) =>
        for {
          result <- meetingRepository.delete(id)
          if result
          organizerOpt <- userRepository.getById(meeting.organizerId)
          participants <- userRepository.getByIds(meeting.participantIds)
          _ <- organizerOpt match {
            case Some(organizer) =>
              notificationService.notifyMeetingDeletion(
                organizer,
                meeting,
                participants
              ).recover { case _ => Seq.empty[Boolean] }
            case None =>
              Future.successful(Seq.empty[Boolean])
          }
        } yield result
      case None =>
        Future.successful(false)
    }
  }

  private def validateMeeting(meeting: Meeting): Future[Either[ValidationError, Unit]] = {
    if (meeting.startTime.isAfter(meeting.endTime)) {
      Future.successful(Left(ValidationError("Start time must be before end time")))
    } else if (meeting.startTime.isBefore(LocalDateTime.now())) {
      Future.successful(Left(ValidationError("Start time cannot be in the past")))
    } else {
      val organizerCheck = userRepository.getById(meeting.organizerId).map {
        case Some(_) => Right(())
        case None    => Left(ValidationError(s"Organizer with ID ${meeting.organizerId} does not exist"))
      }

      val participantsCheck = userRepository.existsAll(meeting.participantIds).map {
        case true  => Right(())
        case false => Left(ValidationError("One or more participants do not exist"))
      }

      for {
        organizerResult   <- organizerCheck
        participantsResult <- participantsCheck
      } yield {
        (organizerResult, participantsResult) match {
          case (Right(_), Right(_)) => Right(())
          case (Left(error), _)     => Left(error)
          case (_, Left(error))     => Left(error)
        }
      }
    }
  }
}