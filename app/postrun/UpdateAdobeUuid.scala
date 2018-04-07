package postrun

import java.util.UUID

import helpers.PostrunDataCache
import models.{PlutoCommission, PlutoWorkingGroup, ProjectEntry, ProjectType}
import play.api.Logger

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml._

class UpdateAdobeUuid extends PojoPostrun with AdobeXml {
  private val logger = Logger(getClass)

  def canFindAttribute(attribs: MetaData, key: String): Boolean ={
    if(attribs.key==key){
      true
    } else {
      if(attribs.next!=null){
        canFindAttribute(attribs.next, key)
      } else {
        false
      }
    }
  }

  class UpdateUuidLocations(newUuid:UUID) extends RewriteRule {
    override def transform(n: Node): Seq[Node] = n match {
      case Elem(prefix,"RootProjectItem",attributes,scope,children@_*)=>
        logger.debug(s"prefix: $prefix, RootProjectItem, $attributes, scope: $scope")
        val maybeUpdatedAttribs:Option[MetaData] =
          if(canFindAttribute(attributes, "ObjectUID")){
            Some(new UnprefixedAttribute("ObjectUID",newUuid.toString,attributes.remove("ObjectUID")))
          } else if(canFindAttribute(attributes, "ObjectURef")){
            Some(new UnprefixedAttribute("ObjectURef", newUuid.toString, attributes.remove("ObjectURef")))
          } else {
            None
          }
        maybeUpdatedAttribs match {
          case Some(updatedAttribs)=>
            Seq(Elem.apply(prefix,"RootProjectItem",updatedAttribs,scope,true,children:_*))
          case None=>
            Elem.apply(prefix,"RootProjectItem",attributes, scope, true,children:_*)
        }

      case other=>other
    }
  }

  def postrun(projectFileName:String,projectEntry:ProjectEntry,projectType:ProjectType,dataCache:PostrunDataCache,
              workingGroupMaybe: Option[PlutoWorkingGroup], commissionMaybe: Option[PlutoCommission]):Future[Try[PostrunDataCache]] = {

    getXmlFromGzippedFile(projectFileName).map({
      case Failure(err)=>
        logger.error("Could not read adobe xml: ", err)
        Failure(err)
      case Success(xmlData)=>
        val newUuid = UUID.randomUUID()
        logger.info(s"New adobe UUID is $newUuid")
        val updatedXml = new RuleTransformer(new UpdateUuidLocations(newUuid)).transform(xmlData).head
        putXmlToGzippedFile(projectFileName,Elem.apply(updatedXml.prefix, updatedXml.label, updatedXml.attributes, updatedXml.scope, false, updatedXml.child :_*))
        Success(dataCache)
    })
  }
}
