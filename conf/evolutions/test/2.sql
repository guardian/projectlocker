--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

--
-- Data for Name: StorageEntry; Type: TABLE DATA; Schema: public; Owner: projectlocker
--

COPY "StorageEntry" (id, rootpath, "storageType", "user", password, host, port) FROM stdin;
1	\N	filesystem	me	\N	\N	\N
2	/backups/projectfiles	ftp	me	123456abcde	ftp.mysite.com	21
3	/backups/projectfiles	ftp	me	123456abcde	ftp.mysite.com	21
\.


--
-- Data for Name: FileEntry; Type: TABLE DATA; Schema: public; Owner: projectlocker
--

COPY "FileEntry" (id, filepath, storage, "user", version, ctime, mtime, atime) FROM stdin;
1	/path/to/a/video.mxf	2	me	1	2017-01-17 16:55:00.123	2017-01-17 16:55:00.123	2017-01-17 16:55:00.123
\.


--
-- Name: FileEntry_id_seq; Type: SEQUENCE SET; Schema: public; Owner: projectlocker
--

SELECT pg_catalog.setval('"FileEntry_id_seq"', 1, true);


--
-- Data for Name: ProjectFileAssociation; Type: TABLE DATA; Schema: public; Owner: projectlocker
--

COPY "ProjectFileAssociation" (id, "ProjectEntry", "FileEntry") FROM stdin;
\.


--
-- Data for Name: ProjectType; Type: TABLE DATA; Schema: public; Owner: projectlocker
--

COPY "ProjectType" (id, name, "opensWith", "targetVersion") FROM stdin;
1	Premiere 2014 test	AdobePremierePro.app	14.0
2	Prelude 2014 test	AdobePrelude.app	14.0
3	Cubase test	Cubase.app	6.0
\.


--
-- Data for Name: ProjectEntry; Type: TABLE DATA; Schema: public; Owner: projectlocker
--

COPY "ProjectEntry" (id, "ProjectFileAssociation", "ProjectType", created, "user") FROM stdin;
\.


--
-- Name: ProjectEntry_id_seq; Type: SEQUENCE SET; Schema: public; Owner: projectlocker
--

SELECT pg_catalog.setval('"ProjectEntry_id_seq"', 1, false);


--
-- Name: ProjectFileAssociation_id_seq; Type: SEQUENCE SET; Schema: public; Owner: projectlocker
--

SELECT pg_catalog.setval('"ProjectFileAssociation_id_seq"', 1, false);


--
-- Data for Name: ProjectTemplate; Type: TABLE DATA; Schema: public; Owner: projectlocker
--

COPY "ProjectTemplate" (id, name, "ProjectType", filepath, storage) FROM stdin;
3	Premiere test template 1	1	/srv/projectfiles/ProjectTemplatesDev/Premiere/premiere_template_2014.prproj	1
\.


--
-- Name: ProjectTemplate_id_seq; Type: SEQUENCE SET; Schema: public; Owner: projectlocker
--

SELECT pg_catalog.setval('"ProjectTemplate_id_seq"', 3, true);


--
-- Name: ProjectType_id_seq; Type: SEQUENCE SET; Schema: public; Owner: projectlocker
--

SELECT pg_catalog.setval('"ProjectType_id_seq"', 3, true);


--
-- Name: StorageEntry_id_seq; Type: SEQUENCE SET; Schema: public; Owner: projectlocker
--

SELECT pg_catalog.setval('"StorageEntry_id_seq"', 3, true);


--
-- PostgreSQL database dump complete
--

