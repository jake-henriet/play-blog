package controllers

import javax.inject.Inject
import javax.inject.Singleton
import com.mohiva.play.silhouette.api.{Authorization, LoginInfo, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
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
class MonoController @Inject()(modelService: ModelsService, val messagesApi: MessagesApi, silhouette: Silhouette[UserCookieEnv]) extends Controller with I18nSupport {

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
  def index = silhouette.UserAwareAction.async {
    implicit request =>
      modelService.getAllBlogsWithUsers.map { blogsUsers =>
        Ok(views.html.blogs(request.identity,blogsUsers))
      }
  }

  /* Get a blog for displaying a users blog */
  def blog(blogId: Long) = silhouette.UserAwareAction.async { implicit request =>
    modelService.getBlog(blogId) flatMap {
      case Some(blog) =>
        modelService.getUser(blog.userId) flatMap {
          case Some(blogUser) => {
            modelService.getBlogPosts(blogId) map { blogPosts => Ok(views.html.blog(request.identity, blogPosts, blog, blogUser)) }
          }
          case None =>
            Future.successful(InternalServerError(views.html.msg("Blog with no user found")))
        }
      case None =>
        Future.successful(NotFound(views.html.msg("Blog not found")))
    }
  }

  def login = silhouette.UnsecuredAction { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def logout = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect(routes.MonoController.index())
    //silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)

  }

  /* Attempt to login the user from the POST request */
  def loginPost = silhouette.UnsecuredAction.async { implicit request =>
    val form = loginForm.bindFromRequest
    form.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.login(formWithErrors)))
      },
      loginData => {
        val loginInfo = LoginInfo("oink",loginData)
        modelService.retrieve(loginInfo) flatMap{
          case Some(user) =>
            for {
              authenticator <- silhouette.env.authenticatorService.create(loginInfo)
              value <- silhouette.env.authenticatorService.init(authenticator)
              result <- silhouette.env.authenticatorService.embed(value, Redirect(routes.MonoController.index()))
            } yield result
          case None =>
            Future.successful(BadRequest(views.html.login(form.withError("username","User not found"))))
        }

      }
    )
  }

  def registerUser = silhouette.UnsecuredAction { implicit request =>
    Ok(views.html.register(userBlogForm))
  }

  /* Create a new User entry and Blog entry in tables and login the user via the register POST request */
  def registerUserPost = silhouette.UnsecuredAction.async { implicit request =>
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
                  blogIdIHope =>
                    for {
                      authenticator <- silhouette.env.authenticatorService.create(LoginInfo("oink",userData.userUsername))
                      value <- silhouette.env.authenticatorService.init(authenticator)
                      result <- silhouette.env.authenticatorService.embed(value, Redirect(routes.MonoController.blog(blogIdIHope)))
                    } yield result
                }
            }
        }
      }
    )
  }

  case class AsBlogOwner(blogId: Long) extends Authorization[User, CookieAuthenticator] {

    def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(
      implicit request: Request[B]) = {
      modelService.getBlog(blogId) flatMap {
        case Some(blog) =>
          Future.successful(user.id== blog.userId)
        case None =>
          Future.successful(false)
      }
    }
  }

  def newBlogPost(blogId: Long) = silhouette.SecuredAction/*(AsBlogOwner(blogId))*/ { implicit request =>
    Ok(views.html.newPost(Some(request.identity),blogPostForm,blogId))
  }

  /* Make a new blog post for a user.  Gives error is user tries to make a blog under another user. */
  def newBlogPostPost(blogId: Long) = silhouette.SecuredAction(AsBlogOwner(blogId)).async { implicit request =>
    val form = blogPostForm.bindFromRequest
    form.fold(
      formWithErrors => {
        Future.successful( BadRequest(views.html.newPost(Some(request.identity),formWithErrors,blogId)) )
      },
      blogPostData => {
        modelService.getBlog(blogId) flatMap {
          case Some(blog) =>
            modelService.addBlogPost(blog.userId, blog.id, blogPostData.title, blogPostData.content)
            Future.successful(Redirect(routes.MonoController.blog(blogId)))
          case None =>
            Future.successful(NotFound(views.html.msg("Unexpected error")))
        }
      }
    )
  }

}
