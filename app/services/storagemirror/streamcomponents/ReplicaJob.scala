package services.storagemirror.streamcomponents

import models.FileEntry

case class ReplicaJob(sourceEntry:FileEntry, destEntry:FileEntry, bytesCopied:Option[Long])
