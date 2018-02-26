package exceptions

class AlreadyExistsException(message:String) extends RuntimeException {
  override def toString: String = super.toString + ": " + message
}
