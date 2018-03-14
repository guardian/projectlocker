import unittest2
import xml.etree.ElementTree as ET
import shutil
import os
import tempfile
import logging


class TestPostrun(unittest2.TestCase):
    def __init__(self,*args,**kwargs):
        super(TestPostrun,self).__init__(*args,**kwargs)

        self.logger = logging.getLogger(self.__class__.__name__)

    def assertNodeContent(self, doc, xpath, content):
        nodeContent = doc.find(xpath)
        if nodeContent is None:
            raise AssertionError("document did not contain xpath {0}".format(xpath))
        self.assertEqual(nodeContent.text, content)

    def test_postrun_integrationtest(self):
        from scripts.update_premiere_scratchpaths import postrun
        from scripts.PremiereProjectFile import loadFile

        destprojectfile = tempfile.mktemp(".prproj","testpremirescratch")
        self.logger.info("test data file is {0}".format(destprojectfile))

        shutil.copy("data/blank_premiere_2017.prproj", destprojectfile)

        postrun(projectFile=destprojectfile, projectFileExtension=".prproj", dataCache={
            'created_asset_folder': '/tmp/testassets/folder'
        })

        xmltree, is_compressed = loadFile(destprojectfile)
        self.assertTrue(is_compressed)

        self.assertNodeContent(xmltree,"ScratchDiskSettings/CapturedVideoLocation0","/tmp/testassets/folder/")
        self.assertNodeContent(xmltree,"ScratchDiskSettings/CapturedAudioLocation0","/tmp/testassets/folder/")
        self.assertNodeContent(xmltree,"ScratchDiskSettings/TransferMediaLocation0","/tmp/testassets/folder/TransferMedia")

        os.unlink(destprojectfile)
        os.rmdir("/tmp/testassets/folder/TransferMedia")
        os.rmdir("/tmp/testassets/folder")