import unittest2
import shutil
import xml.etree.ElementTree as ET
from mock import patch
from os import unlink

class TestGetDoctype(unittest2.TestCase):
    def test_get_doctype_pr2017(self):
        """
        getDoctype should return the doctype annotation of a document
        :return:
        """
        from scripts.update_premiere_uuid import getDoctype
        result = getDoctype("data/blank_premiere_2017.prproj")

        self.assertEqual(result,None)


class TestLoadFile(unittest2.TestCase):
    def test_loadfile_normal(self):
        """
        loadFile should load a regular compressed pr2017 project
        :return:
        """
        from scripts.update_premiere_uuid import loadFile

        xmltree, isCompressed = loadFile("data/blank_premiere_2017.prproj")
        self.assertEqual(isCompressed, True)
        self.assertEqual(str(xmltree.__class__.__name__),"Element")
        self.assertEqual(xmltree.find('Project/RootProjectItem').attrib['ObjectURef'],"2cb1b6c5-c598-4f3b-b463-28e2f52f1d2f")

    def test_loadfile_notfound(self):
        """
        loadFile should raise an IOError if the file is not found
        :return:
        """
        from scripts.update_premiere_uuid import loadFile

        with self.assertRaises(IOError):
            xmltree, isCompressed = loadFile("someinvalidfilename")


class TestDoUpdate(unittest2.TestCase):
    def test_doupdate_normal(self):
        """
        doUpdate should update the provided xml tree attributes with the provided uuid
        :return:
        """

        xmltree = ET.Element("myroot")
        ET.SubElement(xmltree,"ProjectThingy",attrib={'Myuuid': "nothing"})

        with patch('uuid.uuid4', return_value="new_uuid"):
            from scripts.update_premiere_uuid import doUpdate
            result = doUpdate(xmltree,[{"xpath": "ProjectThingy", "attrib": "Myuuid"}])

        self.assertEqual(xmltree.find("ProjectThingy").attrib["Myuuid"], "new_uuid")


class TestDoPostrun(unittest2.TestCase):
    def test_integration(self):
        """
        Integration test to make sure that the postrun function works
        :return:
        """
        with patch("uuid.uuid4",return_value="fake_uuid") as mock_save:
            from scripts.update_premiere_uuid import loadFile, postrun
            shutil.copy("data/blank_premiere_2017.prproj","/tmp/testproject.prproj")

            result = postrun(projectFilename="/tmp/testproject.prproj")

            xmltree, is_compressed = loadFile("/tmp/testproject.prproj")
            self.assertEqual(xmltree.find('Project/RootProjectItem').attrib['ObjectURef'],"fake_uuid")
            self.assertEqual(xmltree.find('RootProjectItem').attrib['ObjectUID'],"fake_uuid")
            unlink("/tmp/testproject.prproj")