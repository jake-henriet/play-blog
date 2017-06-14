package model

import java.sql.Timestamp

import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcProfile
import play.api.db.slick.DatabaseConfigProvider
import play.api.Play
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.{Env, Identity, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator

/* Case classes to represent the tables in the database */
//case class Profiles(id: Long, username: String, aboutMe: String, isAdmin: Boolean)
case class User(id: Long, username: String, aboutMe: String, isAdmin: Boolean) extends Identity
case class Blog(id: Long, userId: Long, title:String, description: String)
case class BlogPost(id: Long, userId: Long, blogId: Long, title: String, content: String, created: Timestamp, edited: Timestamp)

trait UserCookieEnv extends Env {
  type I = User
  type A = CookieAuthenticator
}

//val userCookieEnv = Environment[UserCookieEnv](...)

@Singleton
class ModelsService @Inject()(dbConfigProvider: DatabaseConfigProvider) extends IdentityService[User] {

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  /* Define Slick mappings for model classes to tables */
  class UserTableDef(tag: Tag) extends Table[User](tag, "USER") {

    def id = column[Long]("ID", O.PrimaryKey,O.AutoInc)
    def userName = column[String]("USER_NAME")
    def aboutMe = column[String]("ABOUT_ME")
    def isAdmin = column[Boolean]("IS_ADMIN")

    override def * =
      (id, userName, aboutMe, isAdmin) <> (User.tupled, User.unapply)

  }

  val users = TableQuery[UserTableDef]

  class BlogTableDef(tag: Tag) extends Table[Blog](tag, "BLOG") {

    def id = column[Long]("ID", O.PrimaryKey,O.AutoInc)
    def userId = column[Long]("USER_ID")
    def title = column[String]("TITLE")
    def description = column[String]("DESCRIPTION")

    override def * =
      (id, userId, title, description) <>(Blog.tupled, Blog.unapply)

    def user = foreignKey("B_USER_FK",userId, users)(_.id)

  }

  val blogs = TableQuery[BlogTableDef]

  class BlogPostTableDef(tag: Tag) extends Table[BlogPost](tag, "BLOG_POST") {

    def id = column[Long]("ID", O.PrimaryKey,O.AutoInc)
    def userId = column[Long]("USER_ID")
    def blogId = column[Long]("BLOG_ID")
    def title = column[String]("TITLE")
    def content = column[String]("CONTENT")
    def created = column[Timestamp]("CREATED")
    def edited = column[Timestamp]("EDITED")

    override def * =
      (id, userId, blogId, title, content, created,edited) <> (BlogPost.tupled, BlogPost.unapply)

    def user = foreignKey("BP_USER_FK",userId, users)(_.id)
    def blog = foreignKey("BP_BLOG_FK",blogId, blogs)(_.id)

  }

  val blogPosts = TableQuery[BlogPostTableDef]

  def getUser(id: Long): Future[Option[User]] = {
    dbConfig.db.run(users.filter(_.id === id).result.headOption)
  }

  def findUserByUsername(username: String): Future[Option[User]] = {
    dbConfig.db.run(users.filter(p => p.userName.toLowerCase === username.toLowerCase).result.headOption)
  }

  def listAllUsers: Future[Seq[User]] = {
    dbConfig.db.run(users.result)
  }

  def getAllBlogs: Future[Seq[Blog]] = {
    dbConfig.db.run(blogs.result)
  }

  def getAllBlogsWithUsers: Future[Seq[(Blog,User)]] = {
    val innerJoin = for {
      (b, u) <- blogs join users on (_.userId === _.id)
    } yield (b, u)
    dbConfig.db.run(innerJoin.result)
  }

  def getBlog(blogId: Long): Future[Option[Blog]] = {
    dbConfig.db.run(blogs.filter(_.id === blogId).result.headOption)
  }

  def getBlogPosts(blogId: Long): Future[Seq[BlogPost]] = {
    dbConfig.db.run(blogPosts.filter(_.blogId === blogId).sortBy(_.created desc).result)
  }

  /*Add a blog post and get the newly added id */
  def addBlogPost(userId: Long, blogId: Long, title: String, content: String): Future[Long] = {
    val today = new java.util.Date()
    val timestampDeprectad  = new java.sql.Timestamp(today.getTime())
    dbConfig.db.run((blogPosts returning blogPosts.map(_.id)) += BlogPost(-1,userId,blogId,title,content,timestampDeprectad,timestampDeprectad )).map(res => res).recover {
      case ex: Exception => -1
    }
  }

  def addUser(username: String, aboutMe: String): Future[Long] = {
    dbConfig.db.run((users returning users.map(_.id)) += User(-1,username,aboutMe,false)).map(res => res).recover {
      case ex: Exception => -1
    }
  }

/* Add a blog and get the newly added id */
  def addBlog(userId: Long, title: String, description: String): Future[Long] = {
    dbConfig.db.run((blogs returning blogs.map(_.id)) += Blog(-1,userId,title,description)).map(res => res).recover {
      case ex: Exception => -1
    }
  }

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    println("retrive called: " + loginInfo)
    dbConfig.db.run(users.filter(_.userName === loginInfo.providerKey).result.headOption)
  }
}