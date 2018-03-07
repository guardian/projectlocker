# -- !Ups

CREATE TABLE "PostrunDependency" (
  id INTEGER NOT NULL PRIMARY KEY,
  K_SOURCE INTEGER NOT NULL,
  K_DEPENDSON INTEGER NOT NULL,
  UNIQUE (K_SOURCE, K_DEPENDSON)
);

ALTER TABLE "PostrunDependency" ADD CONSTRAINT "FK_SOURCE" FOREIGN KEY (K_SOURCE) REFERENCES "PostrunAction"(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "PostrunDependency" ADD CONSTRAINT "FK_DEPENDS_ON" FOREIGN KEY (K_DEPENDSON) REFERENCES "PostrunAction"(id) DEFERRABLE INITIALLY DEFERRED;

CREATE SEQUENCE "PostrunDependency_id_seq"
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;
ALTER SEQUENCE "PostrunDependency_id_seq" OWNED BY "PostrunDependency".id;

ALTER TABLE public."PostrunDependency" OWNER TO projectlocker;
ALTER TABLE "PostrunDependency_id_seq" OWNER TO projectlocker;

ALTER TABLE ONLY "PostrunDependency" ALTER COLUMN id SET DEFAULT nextval('"PostrunDependency_id_seq"'::regclass);

# -- !Downs
DROP TABLE "PostrunDependency"