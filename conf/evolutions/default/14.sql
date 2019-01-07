# --!Ups
ALTER TABLE "ProjectTemplate" ADD COLUMN B_DEPRECATED boolean null;

# --!Downs
ALTER TABLE "ProjectTemplate" DROP COLUMN B_DEPRECATED;
