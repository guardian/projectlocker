import org.specs2.Specification
import org.specs2.specification.core._

class AMasterSpec extends Specification with org.specs2.matcher.ThrownExpectations {
  override def is=s2"""
    Test the controllers...
    ${"ApplicationController" ~ new ApplicationSpec}
    ${"DefaultsController" ~ new DefaultsControllerSpec}
    ${"FileController" ~ new FileControllerSpec}
    ${"PostrunController" ~ new PostrunControllerSpec}
    ${"ProjectEntryController" ~ new ProjectEntryControllerSpec}
    ${"ProjectTemplateController" ~ new ProjectTemplateControllerSpec}
    ${"HmacAuth" ~ new HmacAuth}
  """

}
