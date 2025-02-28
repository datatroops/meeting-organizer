package repositories

import javax.inject.{Inject, Singleton}
import models.User
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def email = column[String]("email")

    def * = (id.?, name, email) <> ((User.apply _).tupled, User.unapply)
  }

  private val users = TableQuery[UserTable]

  def create(user: User): Future[User] = {
    val insertQuery = (users returning users.map(_.id) into ((user, id) => user.copy(id = Some(id))))
    db.run(insertQuery += user)
  }

  def list(): Future[Seq[User]] = db.run(users.result)

  def getById(id: Long): Future[Option[User]] = db.run(users.filter(_.id === id).result.headOption)

  def getByIds(ids: Seq[Long]): Future[Seq[User]] = db.run(users.filter(_.id inSet ids).result)

  def update(id: Long, user: User): Future[Int] = {
    db.run(users.filter(_.id === id).map(u => (u.name, u.email)).update((user.name, user.email)))
  }

  def delete(id: Long): Future[Int] = db.run(users.filter(_.id === id).delete)

  def existsAll(ids: Seq[Long]): Future[Boolean] = {
    db.run(users.filter(_.id inSet ids).map(_.id).result).map(_.toSet == ids.toSet)
  }
}