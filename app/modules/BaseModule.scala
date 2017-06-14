package modules

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.actions.{SecuredErrorHandler, UnsecuredErrorHandler}
import com.mohiva.play.silhouette.api.crypto.{CookieSigner, Crypter, CrypterAuthenticatorEncoder}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Environment, EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.crypto.{JcaCookieSigner, JcaCookieSignerSettings, JcaCrypter, JcaCrypterSettings}
import com.mohiva.play.silhouette.impl.authenticators.{CookieAuthenticator, CookieAuthenticatorService, CookieAuthenticatorSettings}
import com.mohiva.play.silhouette.impl.providers.oauth1.TwitterProvider
import com.mohiva.play.silhouette.impl.providers.{CredentialsProvider, SocialProviderRegistry}
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, PlayCacheLayer, SecureRandomIDGenerator}
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import model.{ModelsService, User, UserCookieEnv}
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

/**
  * The base Guice module.
  */
class BaseModule extends AbstractModule with ScalaModule {

  /**
    * Configures the module.
    */
  def configure(): Unit = {
    bind[Silhouette[UserCookieEnv]].to[SilhouetteProvider[UserCookieEnv]]
    //bind[UnsecuredErrorHandler].to[CustomUnsecuredErrorHandler]
    //bind[SecuredErrorHandler].to[CustomSecuredErrorHandler]
    //bind[UserService].to[UserServiceImpl]
    //bind[UserDAO].to[UserDAOImpl]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[PasswordHasher].toInstance(new BCryptPasswordHasher)
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
  }

  @Provides
  def provideEnvironment(
                          //identityService: IdentityService[User],
                          identityService: ModelsService,
                          authenticatorService: AuthenticatorService[CookieAuthenticator],
                          eventBus: EventBus): Environment[UserCookieEnv] = {

    Environment[UserCookieEnv](
      identityService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  @Provides
  def provideAuthenticatorService(
                                   @Named("authenticator-cookie-signer") cookieSigner: CookieSigner,
                                   @Named("authenticator-crypter") crypter: Crypter,
                                   fingerprintGenerator: FingerprintGenerator,
                                   idGenerator: IDGenerator,
                                   configuration: Configuration,
                                   clock: Clock): AuthenticatorService[CookieAuthenticator] = {

    //Looks like we are making a  CookieAuthenticatorSettings object from the application.conf
    val config = configuration.underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")

//    import scala.concurrent.duration._
//    val config= CookieAuthenticatorSettings(
//                                            cookieName = "authenticator",
//                                            cookiePath = "/",
//                                            cookieDomain = None,
//                                            secureCookie = false,
//                                            httpOnlyCookie = true,
//                                            useFingerprinting = true,
//                                            cookieMaxAge = None,
//                                            authenticatorIdleTimeout = None,
//                                            authenticatorExpiry = 12 hours)

    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new CookieAuthenticatorService(config, None, cookieSigner, encoder, fingerprintGenerator, idGenerator, clock)
  }
  /**
    * Provides the cookie signer for the authenticator.
    *
    * @param configuration The Play configuration.
    * @return The cookie signer for the authenticator.
    */
  @Provides @Named("authenticator-cookie-signer")
  def provideAuthenticatorCookieSigner(configuration: Configuration): CookieSigner = {
    //val config = configuration.underlying.as[JcaCookieSignerSettings]("silhouette.authenticator.cookie.signer")

    //case class JcaCookieSignerSettings(key: String, pepper: String = "-mohiva-silhouette-cookie-signer-")

    new JcaCookieSigner(JcaCookieSignerSettings(key = "asdfjkl234890",pepper = "-mohiva-silhouette-cookie-signer-"))
  }

  /**
    * Provides the crypter for the authenticator.
    *
    * @param configuration The Play configuration.
    * @return The crypter for the authenticator.
    */
  @Provides @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    //val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")

    new JcaCrypter(JcaCrypterSettings(key="asdfjkl234890"))
  }


//  @Provides
//  def provideAuthenticatorService(
//                                   fingerprintGenerator: FingerprintGenerator,
//                                   idGenerator: IDGenerator,
//                                   configuration: Configuration,
//                                   clock: Clock): AuthenticatorService[CookieAuthenticator] = {
//
//    val config = configuration.underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")
//    new CookieAuthenticatorService(config, None, fingerprintGenerator, idGenerator, clock)
//  }

/*
  @Provides
  def provideCredentialsProvider(
                                  authInfoRepository: AuthInfoRepository,
                                  passwordHasher: PasswordHasher): CredentialsProvider = {

    new CredentialsProvider(authInfoRepository, passwordHasher, Seq(passwordHasher))
  }
  */
}
