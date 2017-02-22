#-- !Ups

ALTER TABLE ONLY "ProjectTemplate"
    DROP CONSTRAINT "fk_SourceDir";

ALTER TABLE ONLY "ProjectTemplate"
    ADD CONSTRAINT "fk_Storage" FOREIGN KEY (storage) REFERENCES "StorageEntry"(id);

#-- !Downs
ALTER TABLE ONLY "ProjectTemplate"
  DROP CONSTRAINT "fk_Storage";

ALTER TABLE ONLY "ProjectTemplate"
  ADD CONSTRAINT "fk_SourceDir" FOREIGN KEY (storage) REFERENCES "FileEntry"(id);