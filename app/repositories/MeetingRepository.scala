package repositories

import javax.inject.{Inject, Singleton}
import models.{Meeting, MeetingParticipant, User}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile

import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime

case class MeetingWithOrganizer(
                                 meeting: Meeting,
                                 organizer: User
                               )

@Singleton
class MeetingRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  class MeetingTable(tag: Tag) extends Table[Meeting](tag, "meetings") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def startTime = column[LocalDateTime]("start_time")
    def endTime = column[LocalDateTime]("end_time")
    def organizerId = column[Long]("organizer_id")
    def createdAt = column[LocalDateTime]("created_at")
    def updatedAt = column[LocalDateTime]("updated_at")

    def * = (id.?, title, startTime, endTime, organizerId, createdAt.?, updatedAt.?) <> (
      { tuple =>
        val (id, title, startTime, endTime, organizerId, createdAt, updatedAt) = tuple
        Meeting(id, title, startTime, endTime, organizerId, List.empty, createdAt, updatedAt)
      },
      { meeting: Meeting =>
        Some((meeting.id, meeting.title, meeting.startTime, meeting.endTime, meeting.organizerId,
          meeting.createdAt, meeting.updatedAt))
      }
    )
  }

  class MeetingParticipantTable(tag: Tag) extends Table[MeetingParticipant](tag, "meeting_participants") {
    def meetingId = column[Long]("meeting_id")
    def userId = column[Long]("user_id")

    def pk = primaryKey("pk_meeting_participants", (meetingId, userId))
    def * = (meetingId, userId) <> ((MeetingParticipant.apply _).tupled, MeetingParticipant.unapply)
  }

  private val meetings = TableQuery[MeetingTable]
  private val meetingParticipants = TableQuery[MeetingParticipantTable]

  def create(meeting: Meeting): Future[Meeting] = {
    val now = LocalDateTime.now()
    val meetingWithTimestamps = meeting.copy(
      createdAt = Some(now),
      updatedAt = Some(now)
    )

    val insertAction = for {
      meetingId <- (meetings returning meetings.map(_.id)) += meetingWithTimestamps.copy(participantIds = List.empty)
      _ <- meetingParticipants ++= meeting.participantIds.map(userId => MeetingParticipant(meetingId, userId))
    } yield meetingId

    db.run(insertAction.transactionally).map { id =>
      meetingWithTimestamps.copy(id = Some(id))
    }
  }

  def list(): Future[Seq[Meeting]] = {
    db.run(meetings.result).flatMap { meetingsList =>
      Future.sequence(meetingsList.map { meeting =>
        meeting.id match {
          case Some(id) =>
            db.run(meetingParticipants.filter(_.meetingId === id).map(_.userId).result)
              .map(participants => meeting.copy(participantIds = participants.toList))
          case None =>
            Future.successful(meeting)
        }
      })
    }
  }

  def getById(id: Long): Future[Option[Meeting]] = {
    db.run(meetings.filter(_.id === id).result.headOption).flatMap {
      case Some(meeting) =>
        db.run(meetingParticipants.filter(_.meetingId === id).map(_.userId).result)
          .map(participants => Some(meeting.copy(participantIds = participants.toList)))
      case None => Future.successful(None)
    }
  }

  def update(id: Long, meeting: Meeting): Future[Boolean] = {
    val now = LocalDateTime.now()
    val updateAction = for {
      updateCount <- meetings.filter(_.id === id)
        .map(m => (m.title, m.startTime, m.endTime, m.organizerId, m.updatedAt))
        .update((meeting.title, meeting.startTime, meeting.endTime, meeting.organizerId, now))
      _ <- meetingParticipants.filter(_.meetingId === id).delete
      _ <- meetingParticipants ++= meeting.participantIds.map(userId => MeetingParticipant(id, userId))
    } yield updateCount

    db.run(updateAction.transactionally).map(_ > 0)
  }

  def delete(id: Long): Future[Boolean] = {
    val deleteAction = for {
      _ <- meetingParticipants.filter(_.meetingId === id).delete
      deleteCount <- meetings.filter(_.id === id).delete
    } yield deleteCount

    db.run(deleteAction.transactionally).map(_ > 0)
  }
}