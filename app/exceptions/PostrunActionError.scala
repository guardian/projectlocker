package exceptions

class PostrunActionError(message:String) extends RuntimeException {
  override def toString: String = super.toString + ": " + message
}
