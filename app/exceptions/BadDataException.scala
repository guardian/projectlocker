package exceptions

class BadDataException(message:String) extends RuntimeException {
  override def toString: String = super.toString + ": " + message
}