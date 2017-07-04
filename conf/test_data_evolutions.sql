--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: play_evolutions; Type: TABLE; Schema: public; Owner: projectlocker; Tablespace: 
--

CREATE TABLE play_evolutions (
    id integer NOT NULL,
    hash character varying(255) NOT NULL,
    applied_at timestamp without time zone NOT NULL,
    apply_script text,
    revert_script text,
    state character varying(255),
    last_problem text
);


ALTER TABLE public.play_evolutions OWNER TO projectlocker;

--
-- Data for Name: play_evolutions; Type: TABLE DATA; Schema: public; Owner: projectlocker
--

COPY play_evolutions (id, hash, applied_at, apply_script, revert_script, state, last_problem) FROM stdin;
1	0882869cdd213cbd8195c9f01ba229066f826d61	2017-07-04 13:09:32.938	SET statement_timeout = 0;\nSET client_encoding = 'UTF8';\nSET standard_conforming_strings = on;\nSET check_function_bodies = false;\nSET client_min_messages = warning;\n\n\nSET search_path = public, pg_catalog;\n\nSET default_tablespace = '';\n\nSET default_with_oids = false;\n\n\nCREATE TABLE "FileEntry" (\nid integer NOT NULL,\nfilepath character varying NOT NULL,\nstorage integer NOT NULL,\n"user" character varying NOT NULL,\nversion integer NOT NULL,\nctime timestamp without time zone NOT NULL,\nmtime timestamp without time zone NOT NULL,\natime timestamp without time zone NOT NULL\n);\n\n\nALTER TABLE public."FileEntry" OWNER TO projectlocker;\n\n\nCREATE SEQUENCE "FileEntry_id_seq"\nSTART WITH 1\nINCREMENT BY 1\nNO MINVALUE\nNO MAXVALUE\nCACHE 1;\n\n\nALTER TABLE public."FileEntry_id_seq" OWNER TO projectlocker;\n\nALTER SEQUENCE "FileEntry_id_seq" OWNED BY "FileEntry".id;\n\nCREATE TABLE "ProjectEntry" (\nid integer NOT NULL,\n"ProjectFileAssociation" integer NOT NULL,\n"ProjectType" integer NOT NULL,\ncreated timestamp without time zone NOT NULL,\n"user" character varying NOT NULL\n);\n\n\nALTER TABLE public."ProjectEntry" OWNER TO projectlocker;\n\nCREATE SEQUENCE "ProjectEntry_id_seq"\nSTART WITH 1\nINCREMENT BY 1\nNO MINVALUE\nNO MAXVALUE\nCACHE 1;\n\n\nALTER TABLE public."ProjectEntry_id_seq" OWNER TO projectlocker;\n\nALTER SEQUENCE "ProjectEntry_id_seq" OWNED BY "ProjectEntry".id;\n\nCREATE TABLE "ProjectFileAssociation" (\nid integer NOT NULL,\n"ProjectEntry" integer NOT NULL,\n"FileEntry" integer NOT NULL\n);\n\n\nALTER TABLE public."ProjectFileAssociation" OWNER TO projectlocker;\n\nCREATE SEQUENCE "ProjectFileAssociation_id_seq"\nSTART WITH 1\nINCREMENT BY 1\nNO MINVALUE\nNO MAXVALUE\nCACHE 1;\n\n\nALTER TABLE public."ProjectFileAssociation_id_seq" OWNER TO projectlocker;\n\nALTER SEQUENCE "ProjectFileAssociation_id_seq" OWNED BY "ProjectFileAssociation".id;\n\nCREATE TABLE "ProjectTemplate" (\nid integer NOT NULL,\nname character varying NOT NULL,\n"ProjectType" integer NOT NULL,\nfilepath character varying NOT NULL,\nstorage integer NOT NULL\n);\n\n\nALTER TABLE public."ProjectTemplate" OWNER TO projectlocker;\n\nCREATE SEQUENCE "ProjectTemplate_id_seq"\nSTART WITH 1\nINCREMENT BY 1\nNO MINVALUE\nNO MAXVALUE\nCACHE 1;\n\n\nALTER TABLE public."ProjectTemplate_id_seq" OWNER TO projectlocker;\n\nALTER SEQUENCE "ProjectTemplate_id_seq" OWNED BY "ProjectTemplate".id;\n\nCREATE TABLE "ProjectType" (\nid integer NOT NULL,\nname character varying NOT NULL,\n"opensWith" character varying NOT NULL,\n"targetVersion" character varying NOT NULL\n);\n\n\nALTER TABLE public."ProjectType" OWNER TO projectlocker;\n\nCREATE SEQUENCE "ProjectType_id_seq"\nSTART WITH 1\nINCREMENT BY 1\nNO MINVALUE\nNO MAXVALUE\nCACHE 1;\n\n\nALTER TABLE public."ProjectType_id_seq" OWNER TO projectlocker;\n\nALTER SEQUENCE "ProjectType_id_seq" OWNED BY "ProjectType".id;\n\nCREATE TABLE "StorageEntry" (\nid integer NOT NULL,\nrootpath character varying,\n"storageType" character varying NOT NULL,\n"user" character varying,\npassword character varying,\nhost character varying,\nport integer\n);\n\n\nALTER TABLE public."StorageEntry" OWNER TO projectlocker;\n\nCREATE SEQUENCE "StorageEntry_id_seq"\nSTART WITH 1\nINCREMENT BY 1\nNO MINVALUE\nNO MAXVALUE\nCACHE 1;\n\n\nALTER TABLE public."StorageEntry_id_seq" OWNER TO projectlocker;\n\nALTER SEQUENCE "StorageEntry_id_seq" OWNED BY "StorageEntry".id;\n\nALTER TABLE ONLY "FileEntry" ALTER COLUMN id SET DEFAULT nextval('"FileEntry_id_seq"'::regclass);\n\n\nALTER TABLE ONLY "ProjectEntry" ALTER COLUMN id SET DEFAULT nextval('"ProjectEntry_id_seq"'::regclass);\n\nALTER TABLE ONLY "ProjectFileAssociation" ALTER COLUMN id SET DEFAULT nextval('"ProjectFileAssociation_id_seq"'::regclass);\n\n\nALTER TABLE ONLY "ProjectTemplate" ALTER COLUMN id SET DEFAULT nextval('"ProjectTemplate_id_seq"'::regclass);\n\n\nALTER TABLE ONLY "ProjectType" ALTER COLUMN id SET DEFAULT nextval('"ProjectType_id_seq"'::regclass);\n\n\nALTER TABLE ONLY "StorageEntry" ALTER COLUMN id SET DEFAULT nextval('"StorageEntry_id_seq"'::regclass);\n\n\nALTER TABLE ONLY "FileEntry"\nADD CONSTRAINT "FileEntry_pkey" PRIMARY KEY (id);\n\n\nALTER TABLE ONLY "ProjectEntry"\nADD CONSTRAINT "ProjectEntry_pkey" PRIMARY KEY (id);\n\n\nALTER TABLE ONLY "ProjectFileAssociation"\nADD CONSTRAINT "ProjectFileAssociation_pkey" PRIMARY KEY (id);\n\n\nALTER TABLE ONLY "ProjectTemplate"\nADD CONSTRAINT "ProjectTemplate_pkey" PRIMARY KEY (id);\n\nALTER TABLE ONLY "ProjectType"\nADD CONSTRAINT "ProjectType_pkey" PRIMARY KEY (id);\n\n\nALTER TABLE ONLY "StorageEntry"\nADD CONSTRAINT "StorageEntry_pkey" PRIMARY KEY (id);\n\nALTER TABLE ONLY "ProjectEntry"\nADD CONSTRAINT "ProjectType" FOREIGN KEY ("ProjectType") REFERENCES "ProjectType"(id) DEFERRABLE INITIALLY IMMEDIATE;\n\nALTER TABLE ONLY "ProjectFileAssociation"\nADD CONSTRAINT "fk_FileEntry" FOREIGN KEY ("FileEntry") REFERENCES "FileEntry"(id) DEFERRABLE INITIALLY IMMEDIATE;\n\nALTER TABLE ONLY "ProjectFileAssociation"\nADD CONSTRAINT "fk_ProjectEntry" FOREIGN KEY ("ProjectEntry") REFERENCES "ProjectEntry"(id) DEFERRABLE INITIALLY IMMEDIATE;\n\nALTER TABLE ONLY "ProjectEntry"\nADD CONSTRAINT "fk_ProjectFileAssociation" FOREIGN KEY ("ProjectFileAssociation") REFERENCES "ProjectFileAssociation"(id) ON UPDATE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE;\n\nALTER TABLE ONLY "ProjectTemplate"\nADD CONSTRAINT "fk_ProjectType" FOREIGN KEY ("ProjectType") REFERENCES "ProjectType"(id) DEFERRABLE INITIALLY IMMEDIATE;\n\nALTER TABLE ONLY "ProjectTemplate"\nADD CONSTRAINT "fk_SourceDir" FOREIGN KEY (storage) REFERENCES "FileEntry"(id) DEFERRABLE INITIALLY IMMEDIATE;\n\nALTER TABLE ONLY "FileEntry"\nADD CONSTRAINT fk_storage FOREIGN KEY (storage) REFERENCES "StorageEntry"(id) DEFERRABLE INITIALLY IMMEDIATE;\n\nREVOKE ALL ON SCHEMA public FROM PUBLIC;\nREVOKE ALL ON SCHEMA public FROM postgres;\nGRANT ALL ON SCHEMA public TO postgres;\nGRANT ALL ON SCHEMA public TO PUBLIC;		applied	
\.


--
-- Name: play_evolutions_pkey; Type: CONSTRAINT; Schema: public; Owner: projectlocker; Tablespace: 
--

ALTER TABLE ONLY play_evolutions
    ADD CONSTRAINT play_evolutions_pkey PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

