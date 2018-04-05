package postrun

import helpers.PostrunDataCache

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, Text}

class UpdatePremiereScratchpaths extends AdobeXml {
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

//  def updateXmlData(elem: Elem, newLocation:String) = {
//    Elem(
//      elem.prefix,
//      elem.label,
//      elem.attributes,
//      elem.scope,
//      elem.child.map({
//        case Elem(prefix,"CapturedVideoLocation0",attributes,scope,child@_*)=>
//          Elem(prefix,"CapturedVideoLocation0",attributes,scope,Text(newLocation))
//        case Elem(prefix,"CapturedAudioLocation0",attributes,scope,child@_*)=>
//          Elem(prefix,"CapturedVideoLocation0",attributes,scope,Text(newLocation))
//        case Elem(prefix,label,attributes,scope,child)=>
//          Elem(prefix,label, attributes,scope,child)
//      })
//    )
//
//  }
  def postrun(projectFile:String, projectFileExtension:String, dataCache: PostrunDataCache):Future[Try[PostrunDataCache]] = {
    val maybeNewPath = dataCache.get("created_asset_folder")
    if(maybeNewPath.isEmpty) Future(Failure(new RuntimeException("no value for created_asset_folder")))

    getXmlFromGzippedFile(projectFile).map({
      case Failure(err)=>Failure(err)
      case Success(xmlData)=>
        val updatedXml = new RuleTransformer(new UpdateAudioVideoLocations(maybeNewPath.get, maybeNewPath.get)).transform(xmlData).head
        putXmlToGzippedFile(projectFile,Elem.apply(updatedXml.prefix, updatedXml.label, updatedXml.attributes, updatedXml.scope, false, updatedXml.child :_*))
        Success(dataCache)
    })
  }
}
