# -- !Ups

CREATE TABLE "PlutoWorkingGroup" (
  id INTEGER NOT NULL PRIMARY KEY,
  S_HIDE CHARACTER VARYING NULL,
  S_NAME CHARACTER VARYING NOT NULL,
  S_UUID CHARACTER VARYING NOT NULL UNIQUE
);

CREATE SEQUENCE "PlutoWorkingGroup_id_seq"
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;
ALTER SEQUENCE "PlutoWorkingGroup_id_seq" OWNED BY "PlutoWorkingGroup".id;

ALTER TABLE public."PlutoWorkingGroup" OWNER TO projectlocker;
ALTER TABLE "PlutoWorkingGroup_id_seq" OWNER TO projectlocker;

ALTER TABLE ONLY "PlutoWorkingGroup" ALTER COLUMN id SET DEFAULT nextval('"PlutoWorkingGroup_id_seq"'::regclass);

# -- !Downs
DROP TABLE "PlutoWorkingGroup"