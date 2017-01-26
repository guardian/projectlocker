# -- !Ups
SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;


INSERT INTO "StorageEntry" (id, rootpath, "storageType", "user", password, host, port) VALUES (1, NULL, 'filesystem', 'me', NULL, NULL, NULL);
INSERT INTO "StorageEntry" (id, rootpath, "storageType", "user", password, host, port) VALUES (2, '/backups/projectfiles', 'ftp', 'me', '123456abcde', 'ftp.mysite.com', 21);
INSERT INTO "StorageEntry" (id, rootpath, "storageType", "user", password, host, port) VALUES (3, '/backups/projectfiles', 'ftp', 'me', '123456abcde', 'ftp.mysite.com', 21);



INSERT INTO "FileEntry" (id, filepath, storage, "user", version, ctime, mtime, atime) VALUES (1, '/path/to/a/video.mxf', 2, 'me', 1, '2017-01-17 16:55:00.123', '2017-01-17 16:55:00.123', '2017-01-17 16:55:00.123');



SELECT pg_catalog.setval('"FileEntry_id_seq"', 1, true);

INSERT INTO "ProjectType" (id, name, "opensWith", "targetVersion") VALUES (1, 'Premiere 2014 test', 'AdobePremierePro.app', '14.0');
INSERT INTO "ProjectType" (id, name, "opensWith", "targetVersion") VALUES (2, 'Prelude 2014 test', 'AdobePrelude.app', '14.0');
INSERT INTO "ProjectType" (id, name, "opensWith", "targetVersion") VALUES (3, 'Cubase test', 'Cubase.app', '6.0');






SELECT pg_catalog.setval('"ProjectEntry_id_seq"', 1, false);



SELECT pg_catalog.setval('"ProjectFileAssociation_id_seq"', 1, false);



INSERT INTO "ProjectTemplate" (id, name, "ProjectType", filepath, storage) VALUES (3, 'Premiere test template 1', 1, '/srv/projectfiles/ProjectTemplatesDev/Premiere/premiere_template_2014.prproj', 1);



SELECT pg_catalog.setval('"ProjectTemplate_id_seq"', 3, true);



SELECT pg_catalog.setval('"ProjectType_id_seq"', 3, true);



SELECT pg_catalog.setval('"StorageEntry_id_seq"', 3, true);

