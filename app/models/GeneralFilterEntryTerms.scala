package models

import slick.lifted.Query

trait GeneralFilterEntryTerms[Row, Entry] {
  val wildcard: FilterTypeWildcard.Value

  protected def makeWildcard(termString:String):String = wildcard match {
    case FilterTypeWildcard.W_STARTSWITH=>termString + "%"
    case FilterTypeWildcard.W_EXACT=>termString
    case FilterTypeWildcard.W_ENDSWITH=>"%" + termString
    case FilterTypeWildcard.W_CONTAINS=>"%" + termString + "%"
  }

  def addFilterTerms(f: =>Query[Row, Entry, Seq]):Query[Row, Entry, Seq]
}
