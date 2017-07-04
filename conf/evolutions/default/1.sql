--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: FileEntry; Type: TABLE; Schema: public; Owner: projectlocker; Tablespace: 
--

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

--
-- Name: FileEntry_id_seq; Type: SEQUENCE; Schema: public; Owner: projectlocker
--

CREATE SEQUENCE "FileEntry_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."FileEntry_id_seq" OWNER TO projectlocker;

--
-- Name: FileEntry_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: projectlocker
--

ALTER SEQUENCE "FileEntry_id_seq" OWNED BY "FileEntry".id;


--
-- Name: ProjectEntry; Type: TABLE; Schema: public; Owner: projectlocker; Tablespace: 
--

CREATE TABLE "ProjectEntry" (
    id integer NOT NULL,
    "ProjectFileAssociation" integer NOT NULL,
    "ProjectType" integer NOT NULL,
    created timestamp without time zone NOT NULL,
    "user" character varying NOT NULL
);


ALTER TABLE public."ProjectEntry" OWNER TO projectlocker;

--
-- Name: ProjectEntry_id_seq; Type: SEQUENCE; Schema: public; Owner: projectlocker
--

CREATE SEQUENCE "ProjectEntry_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."ProjectEntry_id_seq" OWNER TO projectlocker;

--
-- Name: ProjectEntry_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: projectlocker
--

ALTER SEQUENCE "ProjectEntry_id_seq" OWNED BY "ProjectEntry".id;


--
-- Name: ProjectFileAssociation; Type: TABLE; Schema: public; Owner: projectlocker; Tablespace: 
--

CREATE TABLE "ProjectFileAssociation" (
    id integer NOT NULL,
    "ProjectEntry" integer NOT NULL,
    "FileEntry" integer NOT NULL
);


ALTER TABLE public."ProjectFileAssociation" OWNER TO projectlocker;

--
-- Name: ProjectFileAssociation_id_seq; Type: SEQUENCE; Schema: public; Owner: projectlocker
--

CREATE SEQUENCE "ProjectFileAssociation_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."ProjectFileAssociation_id_seq" OWNER TO projectlocker;

--
-- Name: ProjectFileAssociation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: projectlocker
--

ALTER SEQUENCE "ProjectFileAssociation_id_seq" OWNED BY "ProjectFileAssociation".id;


--
-- Name: ProjectTemplate; Type: TABLE; Schema: public; Owner: projectlocker; Tablespace: 
--

CREATE TABLE "ProjectTemplate" (
    id integer NOT NULL,
    name character varying NOT NULL,
    "ProjectType" integer NOT NULL,
    filepath character varying NOT NULL,
    storage integer NOT NULL
);


ALTER TABLE public."ProjectTemplate" OWNER TO projectlocker;

--
-- Name: ProjectTemplate_id_seq; Type: SEQUENCE; Schema: public; Owner: projectlocker
--

CREATE SEQUENCE "ProjectTemplate_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."ProjectTemplate_id_seq" OWNER TO projectlocker;

--
-- Name: ProjectTemplate_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: projectlocker
--

ALTER SEQUENCE "ProjectTemplate_id_seq" OWNED BY "ProjectTemplate".id;


--
-- Name: ProjectType; Type: TABLE; Schema: public; Owner: projectlocker; Tablespace: 
--

CREATE TABLE "ProjectType" (
    id integer NOT NULL,
    name character varying NOT NULL,
    "opensWith" character varying NOT NULL,
    "targetVersion" character varying NOT NULL
);


ALTER TABLE public."ProjectType" OWNER TO projectlocker;

--
-- Name: ProjectType_id_seq; Type: SEQUENCE; Schema: public; Owner: projectlocker
--

CREATE SEQUENCE "ProjectType_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."ProjectType_id_seq" OWNER TO projectlocker;

--
-- Name: ProjectType_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: projectlocker
--

ALTER SEQUENCE "ProjectType_id_seq" OWNED BY "ProjectType".id;


--
-- Name: StorageEntry; Type: TABLE; Schema: public; Owner: projectlocker; Tablespace: 
--

CREATE TABLE "StorageEntry" (
    id integer NOT NULL,
    rootpath character varying,
    "storageType" character varying NOT NULL,
    "user" character varying,
    password character varying,
    host character varying,
    port integer
);


ALTER TABLE public."StorageEntry" OWNER TO projectlocker;

--
-- Name: StorageEntry_id_seq; Type: SEQUENCE; Schema: public; Owner: projectlocker
--

CREATE SEQUENCE "StorageEntry_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public."StorageEntry_id_seq" OWNER TO projectlocker;

--
-- Name: StorageEntry_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: projectlocker
--

ALTER SEQUENCE "StorageEntry_id_seq" OWNED BY "StorageEntry".id;


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "FileEntry" ALTER COLUMN id SET DEFAULT nextval('"FileEntry_id_seq"'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "ProjectEntry" ALTER COLUMN id SET DEFAULT nextval('"ProjectEntry_id_seq"'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "ProjectFileAssociation" ALTER COLUMN id SET DEFAULT nextval('"ProjectFileAssociation_id_seq"'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "ProjectTemplate" ALTER COLUMN id SET DEFAULT nextval('"ProjectTemplate_id_seq"'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "ProjectType" ALTER COLUMN id SET DEFAULT nextval('"ProjectType_id_seq"'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "StorageEntry" ALTER COLUMN id SET DEFAULT nextval('"StorageEntry_id_seq"'::regclass);


--
-- Name: FileEntry_pkey; Type: CONSTRAINT; Schema: public; Owner: projectlocker; Tablespace: 
--

ALTER TABLE ONLY "FileEntry"
    ADD CONSTRAINT "FileEntry_pkey" PRIMARY KEY (id);


--
-- Name: ProjectEntry_pkey; Type: CONSTRAINT; Schema: public; Owner: projectlocker; Tablespace: 
--

ALTER TABLE ONLY "ProjectEntry"
    ADD CONSTRAINT "ProjectEntry_pkey" PRIMARY KEY (id);


--
-- Name: ProjectFileAssociation_pkey; Type: CONSTRAINT; Schema: public; Owner: projectlocker; Tablespace: 
--

ALTER TABLE ONLY "ProjectFileAssociation"
    ADD CONSTRAINT "ProjectFileAssociation_pkey" PRIMARY KEY (id);


--
-- Name: ProjectTemplate_pkey; Type: CONSTRAINT; Schema: public; Owner: projectlocker; Tablespace: 
--

ALTER TABLE ONLY "ProjectTemplate"
    ADD CONSTRAINT "ProjectTemplate_pkey" PRIMARY KEY (id);


--
-- Name: ProjectType_pkey; Type: CONSTRAINT; Schema: public; Owner: projectlocker; Tablespace: 
--

ALTER TABLE ONLY "ProjectType"
    ADD CONSTRAINT "ProjectType_pkey" PRIMARY KEY (id);


--
-- Name: StorageEntry_pkey; Type: CONSTRAINT; Schema: public; Owner: projectlocker; Tablespace: 
--

ALTER TABLE ONLY "StorageEntry"
    ADD CONSTRAINT "StorageEntry_pkey" PRIMARY KEY (id);


--
-- Name: ProjectType; Type: FK CONSTRAINT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "ProjectEntry"
    ADD CONSTRAINT "ProjectType" FOREIGN KEY ("ProjectType") REFERENCES "ProjectType"(id);


--
-- Name: fk_FileEntry; Type: FK CONSTRAINT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "ProjectFileAssociation"
    ADD CONSTRAINT "fk_FileEntry" FOREIGN KEY ("FileEntry") REFERENCES "FileEntry"(id);


--
-- Name: fk_ProjectEntry; Type: FK CONSTRAINT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "ProjectFileAssociation"
    ADD CONSTRAINT "fk_ProjectEntry" FOREIGN KEY ("ProjectEntry") REFERENCES "ProjectEntry"(id);


--
-- Name: fk_ProjectFileAssociation; Type: FK CONSTRAINT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "ProjectEntry"
    ADD CONSTRAINT "fk_ProjectFileAssociation" FOREIGN KEY ("ProjectFileAssociation") REFERENCES "ProjectFileAssociation"(id) ON UPDATE RESTRICT;


--
-- Name: fk_ProjectType; Type: FK CONSTRAINT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "ProjectTemplate"
    ADD CONSTRAINT "fk_ProjectType" FOREIGN KEY ("ProjectType") REFERENCES "ProjectType"(id);


--
-- Name: fk_SourceDir; Type: FK CONSTRAINT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "ProjectTemplate"
    ADD CONSTRAINT "fk_SourceDir" FOREIGN KEY (storage) REFERENCES "FileEntry"(id);


--
-- Name: fk_storage; Type: FK CONSTRAINT; Schema: public; Owner: projectlocker
--

ALTER TABLE ONLY "FileEntry"
    ADD CONSTRAINT fk_storage FOREIGN KEY (storage) REFERENCES "StorageEntry"(id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

