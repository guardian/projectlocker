# -- !Ups

CREATE TABLE "FileEntry" (
    id integer NOT NULL,
    filepath character varying NOT NULL,
    storage integer NOT NULL,
    "user" character varying NOT NULL,
    version integer NOT NULL,
    ctime timestamp without time zone NOT NULL,
    mtime timestamp without time zone NOT NULL,
    atime timestamp without time zone NOT NULL
);


ALTER TABLE public."FileEntry" OWNER TO projectlocker;


CREATE SEQUENCE "FileEntry_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public."FileEntry_id_seq" OWNER TO projectlocker;

ALTER SEQUENCE "FileEntry_id_seq" OWNED BY "FileEntry".id;

CREATE TABLE "ProjectEntry" (
    id integer NOT NULL,
    "ProjectFileAssociation" integer NOT NULL,
    "ProjectType" integer NOT NULL,
    created timestamp without time zone NOT NULL,
    "user" character varying NOT NULL
);


ALTER TABLE public."ProjectEntry" OWNER TO projectlocker;

CREATE SEQUENCE "ProjectEntry_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."ProjectEntry_id_seq" OWNER TO projectlocker;


ALTER SEQUENCE "ProjectEntry_id_seq" OWNED BY "ProjectEntry".id;


CREATE TABLE "ProjectFileAssociation" (
    id integer NOT NULL,
    "ProjectEntry" integer NOT NULL,
    "FileEntry" integer NOT NULL
);


ALTER TABLE public."ProjectFileAssociation" OWNER TO projectlocker;

CREATE SEQUENCE "ProjectFileAssociation_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."ProjectFileAssociation_id_seq" OWNER TO projectlocker;

ALTER SEQUENCE "ProjectFileAssociation_id_seq" OWNED BY "ProjectFileAssociation".id;

CREATE TABLE "ProjectTemplate" (
    id integer NOT NULL,
    name character varying NOT NULL,
    "ProjectType" integer NOT NULL,
    filepath character varying NOT NULL,
    storage integer NOT NULL
);


ALTER TABLE public."ProjectTemplate" OWNER TO projectlocker;

CREATE SEQUENCE "ProjectTemplate_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."ProjectTemplate_id_seq" OWNER TO projectlocker;

ALTER SEQUENCE "ProjectTemplate_id_seq" OWNED BY "ProjectTemplate".id;

CREATE TABLE "ProjectType" (
    id integer NOT NULL,
    name character varying NOT NULL,
    "opensWith" character varying NOT NULL,
    "targetVersion" character varying NOT NULL
);


ALTER TABLE public."ProjectType" OWNER TO projectlocker;

CREATE SEQUENCE "ProjectType_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."ProjectType_id_seq" OWNER TO projectlocker;


ALTER SEQUENCE "ProjectType_id_seq" OWNED BY "ProjectType".id;

CREATE TABLE "StorageEntry" (
    id integer NOT NULL,
    rootpath character varying,
    "storageType" character varying NOT NULL,
    "user" character varying,
    password character varying,
    host character varying,
    port integer,
    "default" boolean
);


ALTER TABLE public."StorageEntry" OWNER TO projectlocker;

CREATE SEQUENCE "StorageEntry_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."StorageEntry_id_seq" OWNER TO projectlocker;


ALTER SEQUENCE "StorageEntry_id_seq" OWNED BY "StorageEntry".id;

ALTER TABLE ONLY "FileEntry" ALTER COLUMN id SET DEFAULT nextval('"FileEntry_id_seq"'::regclass);

ALTER TABLE ONLY "ProjectEntry" ALTER COLUMN id SET DEFAULT nextval('"ProjectEntry_id_seq"'::regclass);

ALTER TABLE ONLY "ProjectFileAssociation" ALTER COLUMN id SET DEFAULT nextval('"ProjectFileAssociation_id_seq"'::regclass);

ALTER TABLE ONLY "ProjectTemplate" ALTER COLUMN id SET DEFAULT nextval('"ProjectTemplate_id_seq"'::regclass);

ALTER TABLE ONLY "ProjectType" ALTER COLUMN id SET DEFAULT nextval('"ProjectType_id_seq"'::regclass);

ALTER TABLE ONLY "StorageEntry" ALTER COLUMN id SET DEFAULT nextval('"StorageEntry_id_seq"'::regclass);

ALTER TABLE ONLY "FileEntry"
    ADD CONSTRAINT "FileEntry_pkey" PRIMARY KEY (id);

ALTER TABLE ONLY "ProjectEntry"
    ADD CONSTRAINT "ProjectEntry_pkey" PRIMARY KEY (id);

ALTER TABLE ONLY "ProjectFileAssociation"
    ADD CONSTRAINT "ProjectFileAssociation_pkey" PRIMARY KEY (id);

ALTER TABLE ONLY "ProjectTemplate"
    ADD CONSTRAINT "ProjectTemplate_pkey" PRIMARY KEY (id);

ALTER TABLE ONLY "ProjectType"
    ADD CONSTRAINT "ProjectType_pkey" PRIMARY KEY (id);

ALTER TABLE ONLY "StorageEntry"
    ADD CONSTRAINT "StorageEntry_pkey" PRIMARY KEY (id);

ALTER TABLE ONLY "ProjectEntry"
    ADD CONSTRAINT "ProjectType" FOREIGN KEY ("ProjectType") REFERENCES "ProjectType"(id);

ALTER TABLE ONLY "ProjectFileAssociation"
    ADD CONSTRAINT "fk_FileEntry" FOREIGN KEY ("FileEntry") REFERENCES "FileEntry"(id);

ALTER TABLE ONLY "ProjectFileAssociation"
    ADD CONSTRAINT "fk_ProjectEntry" FOREIGN KEY ("ProjectEntry") REFERENCES "ProjectEntry"(id);

ALTER TABLE ONLY "ProjectEntry"
    ADD CONSTRAINT "fk_ProjectFileAssociation" FOREIGN KEY ("ProjectFileAssociation") REFERENCES "ProjectFileAssociation"(id) ON UPDATE RESTRICT;

ALTER TABLE ONLY "ProjectTemplate"
    ADD CONSTRAINT "fk_ProjectType" FOREIGN KEY ("ProjectType") REFERENCES "ProjectType"(id);

ALTER TABLE ONLY "ProjectTemplate"
    ADD CONSTRAINT "fk_SourceDir" FOREIGN KEY (storage) REFERENCES "FileEntry"(id);

ALTER TABLE ONLY "FileEntry"
    ADD CONSTRAINT fk_storage FOREIGN KEY (storage) REFERENCES "StorageEntry"(id);

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;

-- # -- !Downs
-- DROP TABLE "FileEntry" CASCADE;
-- DROP TABLE "ProjectTemplate" CASCADE;
-- DROP TABLE "ProjectEntry" CASCADE;
-- DROP TABLE "ProjectFileAssociation" CASCADE;
-- DROP TABLE "StorageEntry" CASCADE;
-- DROP TABLE "ProjectType" CASCADE;