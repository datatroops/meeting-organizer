package services

import javax.inject.{Inject, Singleton}
import models.User
import repositories.UserRepository
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserService @Inject()(userRepository: UserRepository)(implicit ec: ExecutionContext) {
  def create(user: User): Future[User] = userRepository.create(user)

  def list(): Future[Seq[User]] = userRepository.list()

  def getById(id: Long): Future[Option[User]] = userRepository.getById(id)

  def update(id: Long, user: User): Future[Boolean] = userRepository.update(id, user).map(_ > 0)

  def delete(id: Long): Future[Boolean] = userRepository.delete(id).map(_ > 0)

  def validateUsers(userIds: Seq[Long]): Future[Boolean] = {
    userRepository.existsAll(userIds)
  }
}