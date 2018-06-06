package helpers
import java.sql.Timestamp
import java.time.LocalDateTime

import models.PostrunAction
import org.specs2.mutable.Specification

class PostrunSorterSpec extends Specification {
  "PostrunSorter.doSort" should {
    "sort a list of postruns by dependency" in {
      val unsortedPostrunList = List(
        PostrunAction(Some(1),"somescript","Postrun 1",Some("deps on 2 and 4"),"test",1,Timestamp.valueOf(LocalDateTime.now)),
        PostrunAction(Some(2),"somescript","Postrun 2",Some("deps on none"),"test",1,Timestamp.valueOf(LocalDateTime.now)),
        PostrunAction(Some(3),"somescript","Postrun 3",Some("deps on 1 and 4"),"test",1,Timestamp.valueOf(LocalDateTime.now)),
        PostrunAction(Some(4),"somescript","Postrun 4",Some("deps on 2"),"test",1,Timestamp.valueOf(LocalDateTime.now))
      )

      val dependencies = Map(
        1 -> Seq(2,4),
        2 -> Seq(),
        3 -> Seq(1,4),
        4 -> Seq(2)
      )

      val sortedPostrunList = PostrunSorter.doSort(unsortedPostrunList, dependencies)
      println(sortedPostrunList.toString)
      sortedPostrunList.length mustEqual unsortedPostrunList.length
      sortedPostrunList.head.id must beSome(2)
      sortedPostrunList(1).id must beSome(4)
      sortedPostrunList(2).id must beSome(1)
      sortedPostrunList(3).id must beSome(3)
    }
  }
}
