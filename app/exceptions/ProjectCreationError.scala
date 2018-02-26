package exceptions

class ProjectCreationError(message:String) extends RuntimeException {
  override def toString: String = super.toString + ": " + message
}
