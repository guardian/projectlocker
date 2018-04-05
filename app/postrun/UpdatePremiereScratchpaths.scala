package postrun

import helpers.PostrunDataCache
import models.{PlutoCommission, PlutoWorkingGroup, ProjectEntry, ProjectType}

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, Text}

class UpdatePremiereScratchpaths extends PojoPostrun with AdobeXml {
  class UpdateAudioVideoLocations(newLocation:String,transferLocation:String) extends RewriteRule {
    override def transform(n: Node): Seq[Node] = n match {
      case Elem(prefix,"CapturedVideoLocation0",attributes,scope,_*)=>
        //Seq(Elem.apply(prefix,"CapturedVideoLocation0",attributes,scope,false,Text(newLocation)))
        <CapturedVideoLocation0>{newLocation}</CapturedVideoLocation0>
      case Elem(prefix,"CapturedAudioLocation0",attributes,scope,_*)=>
        Seq(Elem.apply(prefix,"CapturedAudioLocation0",attributes,scope,false,Text(newLocation)))
      case Elem(prefix,"TransferMediaLocation0",attributes,scope,child@_*)=>
        Seq(Elem.apply(prefix,"TransferMediaLocation0",attributes,scope,false,Text(transferLocation)))
      case other=>other
    }
  }

  def postrun(projectFileName:String,projectEntry:ProjectEntry,projectType:ProjectType,dataCache:PostrunDataCache,
              workingGroupMaybe: Option[PlutoWorkingGroup], commissionMaybe: Option[PlutoCommission]):Future[Try[PostrunDataCache]] = {
    val maybeNewPath = dataCache.get("created_asset_folder")
    if(maybeNewPath.isEmpty) Future(Failure(new RuntimeException("no value for created_asset_folder")))

    getXmlFromGzippedFile(projectFileName).map({
      case Failure(err)=>Failure(err)
      case Success(xmlData)=>
        val updatedXml = new RuleTransformer(new UpdateAudioVideoLocations(maybeNewPath.get, maybeNewPath.get)).transform(xmlData).head
        putXmlToGzippedFile(projectFileName,Elem.apply(updatedXml.prefix, updatedXml.label, updatedXml.attributes, updatedXml.scope, false, updatedXml.child :_*))
        Success(dataCache)
    })
  }
}
