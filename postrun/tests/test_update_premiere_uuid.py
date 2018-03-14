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
        from scripts.update_adobe_uuid import getDoctype
        result = getDoctype("data/blank_premiere_2017.prproj")

        self.assertEqual(result,None)


class TestDoUpdate(unittest2.TestCase):
    def test_doupdate_normal(self):
        """
        doUpdate should update the provided xml tree attributes with the provided uuid
        :return:
        """

        xmltree = ET.Element("myroot")
        ET.SubElement(xmltree,"ProjectThingy",attrib={'Myuuid': "nothing"})

        with patch('uuid.uuid4', return_value="new_uuid"):
            from scripts.update_adobe_uuid import doUpdate
            result = doUpdate(xmltree,[{"xpath": "ProjectThingy", "attrib": "Myuuid"}])

        self.assertEqual(xmltree.find("ProjectThingy").attrib["Myuuid"], "new_uuid")


class TestDoPostrun(unittest2.TestCase):
    def test_integration(self):
        """
        Integration test to make sure that the postrun function works
        :return:
        """
        with patch("uuid.uuid4",return_value="fake_uuid") as mock_save:
            from scripts.update_adobe_uuid import loadFile, postrun
            shutil.copy("data/blank_premiere_2017.prproj","/tmp/testproject.prproj")

            result = postrun(projectFile="/tmp/testproject.prproj", projectFileExtension=".prproj")

            xmltree, is_compressed = loadFile("/tmp/testproject.prproj")
            self.assertEqual(xmltree.find('Project/RootProjectItem').attrib['ObjectURef'],"fake_uuid")
            self.assertEqual(xmltree.find('RootProjectItem').attrib['ObjectUID'],"fake_uuid")
            unlink("/tmp/testproject.prproj")

    def test_invalid_extension(self):
        """
        postrun should raise a ValueError if the project file extension is not recognised
        :return:
        """
        from scripts.update_adobe_uuid import loadFile, postrun
        with self.assertRaises(ValueError):
            shutil.copy("data/blank_premiere_2017.prproj","/tmp/testproject.prproj")
            postrun(projectFile="/tmp/testproject.prproj", projectFileExtension=".wibble")