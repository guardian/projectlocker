package helpers

import models.PostrunAction
import play.api.Logger

object PostrunSorter {
  /**
    * Implements a Khan algorithm topological sort: https://en.wikipedia.org/wiki/Topological_sorting
    */

  private val logger = Logger(getClass)
  def setWithNoEdges(incomingList: List[PostrunAction], dependencies: Map[Int,Seq[Int]]): List[PostrunAction] =
    incomingList
        .filter(_.id.isDefined)
        .filter(action=> !dependencies.contains(action.id.get) || dependencies(action.id.get).isEmpty)

  def setThatDependsOn(requiredDependency: PostrunAction, incomingList: List[PostrunAction], dependencies: Map[Int, Seq[Int]]): List[PostrunAction] = {
    val matchingIds = dependencies.filter(entry=>entry._2.contains(requiredDependency.id.get))
    logger.debug(s"setThatDependsOn: matchingIds=$matchingIds")
    logger.debug(s"setThatDependsOn: incomingList=$incomingList")
    matchingIds
      .map(entry=>incomingList.find(action=>action.id.contains(entry._1)))
      .collect({
        case Some(postrunAction)=>postrunAction
      }).toList
  }

  def removeFromDependencies(toRemove: PostrunAction, dependencies: Map[Int, Seq[Int]]): Map[Int, Seq[Int]] =
    dependencies.map(entry=>(entry._1, entry._2.filterNot(_==toRemove.id.get)))

  def removeIrrelevantDependencies(incomingList:List[PostrunAction], dependencies: Map[Int, Seq[Int]]):Map[Int, Seq[Int]] = {
    val relevantIds = incomingList.map(_.id).collect({case Some(id)=>id})
    dependencies
      .filter(entry=>relevantIds.contains(entry._1))
      .map(entry=>(entry._1, entry._2.filter(relevantIds.contains(_))))
  }

  def onlyOneDependency(incomingList:List[PostrunAction], dependencies: Map[Int, Seq[Int]]) =
    incomingList.filter(action=>dependencies.get(action.id.get).isDefined && dependencies(action.id.get).length==1)

  def iterateSort(incomingList: List[PostrunAction], resultList: List[PostrunAction], noEdgesList: List[PostrunAction], dependencies: Map[Int, Seq[Int]]) : List[PostrunAction] = {
    logger.debug(s"iterateSort: noEdgesList: ${noEdgesList}")
    logger.debug(s"remaining dependencies: $dependencies")
    if(noEdgesList.isEmpty) return resultList
    val nodeToProcess = noEdgesList.head
    val updatedResultList = resultList ++ List(nodeToProcess)
    val updatedIncomingList = incomingList.filter(_.id!=nodeToProcess.id)

    logger.debug(s"setThatDependsOn(${nodeToProcess.id.get}): ${setThatDependsOn(nodeToProcess, incomingList,dependencies)}")

    val updatedNoEdgesList = noEdgesList.tail ++ onlyOneDependency(setThatDependsOn(nodeToProcess, incomingList,dependencies), dependencies)

    logger.debug(s"iterateSort: updated result list: $updatedResultList")
    iterateSort(updatedIncomingList, updatedResultList, updatedNoEdgesList, removeFromDependencies(nodeToProcess, dependencies))
  }

  def doSort(incomingList: List[PostrunAction], dependencies: Map[Int, Seq[Int]]): List[PostrunAction] = {
    logger.debug(s"doSort: starting from $incomingList")
    logger.debug(s"doSort: initial dependencies list: $dependencies")
    val releventDependencies = removeIrrelevantDependencies(incomingList, dependencies)
    logger.debug(s"doSort: relevant dependencies list: $releventDependencies")
    val startSet = setWithNoEdges(incomingList, releventDependencies)

    iterateSort(incomingList, List(), startSet , releventDependencies)
  }
}
