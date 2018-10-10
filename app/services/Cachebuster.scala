package services

trait Cachebuster {
  def checksumFor(key:String):Option[String]
}
