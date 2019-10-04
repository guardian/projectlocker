package drivers.objectmatrix
import java.io.File

import com.om.mxs.client.japi.UserInfo
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.util.{Failure, Success, Try}

case class UserInfoBuilder(cluster:Option[String],user:Option[String],
                           password:Option[String],vault:Option[String],addresses:Option[String],vaultName:Option[String],
                           protocol:Option[String],clusterPassword:Option[String],descriptiveLabel:Option[String]) {
  val whitespace = "\\s+".r

  def withCluster(clusterName:String) = this.copy(cluster=Some(clusterName))
  def withUser(value:String) = this.copy(user=Some(value))
  def withPassword(value:String) = this.copy(password=Some(value))
  def withVault(value:String) = this.copy(vault=Some(value))
  def withAddresses(value:String) = this.copy(addresses=Some(whitespace.replaceAllIn(value,"")))
  def withVaultName(value:String) = this.copy(vaultName=Some(value))
  def withProtocol(value:String) = this.copy(protocol=Some(value))
  def withClusterPassword(value:String) = this.copy(clusterPassword=Some(value))
  def withDescriptiveLabel(value:String) = this.copy(descriptiveLabel=Some(value))

  def getUserInfo:Try[UserInfo] = Try { new UserInfo(addresses.get, cluster.get, vault.get, user.get, password.get) }

}

object UserInfoBuilder {
  private val logger = LoggerFactory.getLogger(getClass)

  def apply(cluster: Option[String], user: Option[String],
            password: Option[String], vault: Option[String], addresses: Option[String], vaultName: Option[String],
            protocol: Option[String], cluster_password: Option[String],descriptiveLabel:Option[String]): UserInfoBuilder = new UserInfoBuilder(cluster, user, password, vault, addresses, vaultName, protocol, cluster_password, descriptiveLabel)
  def apply() = new UserInfoBuilder(None,None,None,None,None,None,None,None,None)

  private val commentRE = "^#".r
  private val splitterRE = "^([^=]+)=(.*)$".r

  def fromPath(filePath:String) = fromFile(new File(filePath))

  def builderFromFile(file:File) = {
    val src = Source.fromFile(file.getAbsolutePath)
    var builder:UserInfoBuilder = UserInfoBuilder()

    logger.info(s"Loading ${file.getAbsolutePath}")
    try {
      for (line <- src.getLines()) {
        line match {
          case commentRE(_) =>
          case splitterRE(key, value) =>
            logger.debug(s"Got key '$key' with value '$value'")
            key match {
              case "cluster" => builder = builder.withCluster(value)
              case "user" => builder = builder.withUser(value)
              case "password" => builder = builder.withPassword(value)
              case "vault" => builder = builder.withVault(value)
              case "addresses" => builder = builder.withAddresses(value)
              case "vaultName" => builder = builder.withVaultName(value)
              case "protocol" => builder = builder.withProtocol(value)
              case "cluster password" => builder = builder.withClusterPassword(value)
            }
          case _=>
            logger.warn(s"Couldn't interpret line $line")
        }
      }
      builder = builder.withDescriptiveLabel(file.getName)
      logger.info(s"Loaded ${builder.cluster} for ${builder.addresses} from ${file.getAbsolutePath}")
      Success(builder)
    } catch {
      case err:Throwable=>Failure(err)
    } finally {
      src.close()
    }
  }

  def fromFile(file: File) = builderFromFile(file).flatMap(_.getUserInfo)
}
