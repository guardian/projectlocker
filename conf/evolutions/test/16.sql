# --!Ups
ALTER TABLE "FileEntry" ADD COLUMN K_MIRROR_PARENT BIGINT NULL;
ALTER TABLE "FileEntry" ADD CONSTRAINT FK_MIRROR_PARENT FOREIGN KEY (K_MIRROR_PARENT) REFERENCES "FileEntry"(id) DEFERRABLE INITIALLY DEFERRED;

# --!Downs
ALTER TABLE "FileEntry" DROP CONSTRAINT FK_MIRROR_PARENT;
ALTER TABLE "FileEntry" DROP COLUMN K_MIRROR_PARENT;