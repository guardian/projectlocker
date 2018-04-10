# --!Ups
CREATE TABLE "ProjectMetadata" (
  id INTEGER NOT NULL PRIMARY KEY,
  K_PROJECT_ENTRY INTEGER NOT NULL,
  S_KEY CHARACTER VARYING NOT NULL,
  S_VALUE CHARACTER VARYING
);

ALTER TABLE "ProjectMetadata" ADD CONSTRAINT "FK_PROJECT_ENTRY" FOREIGN KEY (K_PROJECT_ENTRY) REFERENCES "ProjectEntry"(id);
CREATE UNIQUE INDEX "IX_PROJECT_ENTRY_METAKEY" ON "ProjectMetadata"(K_PROJECT_ENTRY,S_KEY);
ALTER TABLE "ProjectEntry" DROP COLUMN S_ADOBE_UUID;

CREATE SEQUENCE "ProjectMetadata_id_seq"
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;
ALTER SEQUENCE "ProjectMetadata_id_seq" OWNED BY "ProjectMetadata".id;
ALTER TABLE ONLY "ProjectMetadata" ALTER COLUMN id SET DEFAULT nextval('"ProjectMetadata_id_seq"'::regclass);
ALTER TABLE public."ProjectMetadata_id_seq" OWNER TO projectlocker;

# --!Downs
DROP TABLE "ProjectMetadata";
ALTER TABLE "ProjectEntry" ADD COLUMN S_ADOBE_UUID CHARACTER VARYING NULL;