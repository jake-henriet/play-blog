package controllers

import javax.inject.Inject
import javax.inject.Singleton

import model._
import play.api.mvc._

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n._

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

/* Case classes for interacting with forms in the views */
case class UserFormData(username: String, aboutMe: String)
case class UserBlogFormData(userUsername: String, userAboutMe: String, blogTitle: String, blogDescription: String)
case class BlogPostFormData(title: String, content: String)

/* A monolithic controller to handle all the apps controllers*/
@Singleton
class MonoController @Inject()(modelService: ModelsService, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /* Forms for the controller to interact with the views */
  val userBlogForm: Form[UserBlogFormData] = Form.apply {
    mapping(
      "username" -> nonEmptyText,
      "aboutMe" -> nonEmptyText,
      "blogTitle" -> nonEmptyText,
      "blogDescription" -> nonEmptyText
    )(UserBlogFormData.apply)(UserBlogFormData.unapply)
  }

  val loginForm = Form(
    single(
      "username" -> nonEmptyText
    )
  )

  val blogPostForm: Form[BlogPostFormData] = Form.apply {
    mapping(
      "title" -> nonEmptyText,
      "content" -> nonEmptyText
    )(BlogPostFormData.apply)(BlogPostFormData.unapply)
  }

  /* Get blogs for display on main page */
  def blogs = Action.async {
    implicit request =>
      modelService.getAllBlogsWithUsers.map { blogsUsers =>
        Ok(views.html.blogs(blogsUsers))
      }
  }

  /* Get a blog for displaying a users blog */
  def blog(blogId: Long) = Action.async { implicit request =>
    modelService.getBlog(blogId) flatMap {
      case Some(blog) =>
        modelService.getUser(blog.userId) flatMap {
          case Some(user) => {
            modelService.getBlogPosts(blogId) map { blogPosts => Ok(views.html.blog(blogPosts, blog, user)) }
          }
          case None =>
            Future.successful(InternalServerError(views.html.msg("Blog with no user found")))
        }
      case None =>
        Future.successful(NotFound(views.html.msg("Blog not found")))
    }
  }

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  /* Attempt to login the user from the POST request */
  def loginPost = Action.async { implicit request =>
    val form = loginForm.bindFromRequest
    form.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.login(formWithErrors)))
      },
      loginData => {
        modelService.findUserByUsername(loginData) flatMap {
          case Some(user) =>
            Future.successful(Redirect(routes.MonoController.blogs()).withSession("userid" -> user.id.toString, "username" -> user.username))
          case None =>
            Future.successful(BadRequest(views.html.login(form.withError("username","User not found"))))
        }
      }
    )
  }

  def logout = Action { implicit request =>
    (Redirect(routes.MonoController.blogs()).withNewSession)
  }

  def registerUser = Action { implicit request =>
    Ok(views.html.register(userBlogForm))
  }

  /* Create a new User entry and Blog entry in tables and login the user via the register POST request */
  def registerUserPost = Action.async { implicit request =>
    val form = userBlogForm.bindFromRequest
    form.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.register(formWithErrors)))
      },
      userData => {
        modelService.findUserByUsername(userData.userUsername) flatMap {
          case Some(foundUser) => Future.successful(BadRequest(views.html.register(form.withError("username","This username is already registered"))))
          case None =>
            modelService.addUser(userData.userUsername, userData.userAboutMe) flatMap {
              userIdIThink =>
                modelService.addBlog(userIdIThink, userData.blogTitle, userData.blogDescription) flatMap {
                  blogIdIHope => Future.successful(Redirect(routes.MonoController.blog(blogIdIHope)).withSession("userid" -> userIdIThink.toString, "username" -> userData.userUsername))
                }
            }
        }
      }
    )

  }

  def newBlogPost(blogId: Long) = Action { implicit request =>
    Ok(views.html.newPost(blogPostForm,blogId))
  }

  /* Make a new blog post for a user.  Gives error is user tries to make a blog under another user. */
  def newBlogPostPost(blogId: Long) = Action.async { implicit request =>
    val form = blogPostForm.bindFromRequest
        form.fold(
          formWithErrors => {
            Future.successful( BadRequest(views.html.newPost(formWithErrors,blogId)) )
          },
          blogPostData => {
            modelService.getBlog(blogId) flatMap {
              case Some(blog) =>
                request.session.get("userid").map { loggedInUserId =>
                  if (loggedInUserId == blog.userId.toString) {
                    modelService.addBlogPost(blog.userId, blog.id, blogPostData.title, blogPostData.content)
                    Future.successful(Redirect(routes.MonoController.blog(blogId)))
                  } else {
                    Future.successful(Unauthorized("You are not authorized for this action on this user."))
                  }
                }.getOrElse {
                  Future.successful(Unauthorized("You are not authenticated for this action."))
                }
              case None =>
                Future.successful(NotFound(views.html.msg("Unexpected error")))
            }
          }
        )
  }

  implicit def loggedInUser(implicit session: Session): Option[(String, String)] = {
    for (userid <- session.get("userid"); username <- session.get("username")) yield (userid, username)
  }

}
