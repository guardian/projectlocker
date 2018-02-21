package models

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

case class LoginRequest(username:String, password:String)

trait LoginRequestSerializer {
  implicit val loginRequestReads:Reads[LoginRequest] = (
    (JsPath \ "username").read[String] and
      (JsPath \ "password").read[String]
    )(LoginRequest.apply _)
}
