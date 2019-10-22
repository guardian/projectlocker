package exceptions

class AlreadyExistsException(message:String, nextAvailableVersion:Int) extends RuntimeException {
  def getNextAvailableVersion = nextAvailableVersion
  override def toString: String = super.toString + ": " + message
}
