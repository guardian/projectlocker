# --!Ups

ALTER TABLE "ProjectType" ADD COLUMN S_FILE_EXTENSION CHARACTER VARYING;

# --!Downs

ALTER TABLE "ProjectType" DROP COLUMN S_FILE_EXTENSION;