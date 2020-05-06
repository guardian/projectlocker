import unittest2
from mock import patch
import tempfile
import shutil
import logging
import os

logger = logging.getLogger("test_copy_project_to_assetfolder")


class TestCopyProjectToAssetFolder(unittest2.TestCase):
    def test_postrun_integration(self):
        """
        integration test to check that postrun correctly copies project file
        :return:
        """
        from scripts.copy_project_to_assetfolder import postrun

        destprojectfile = tempfile.mktemp(".prproj","testpremirescratch")
        logger.info("test data file is {0}".format(destprojectfile))

        shutil.copy("data/blank_premiere_2017.prproj", destprojectfile)

        assetfolder = "/tmp/fake_assets"
        if not os.path.exists(assetfolder):
            os.mkdir(assetfolder)
        copiedpath = os.path.join(assetfolder, os.path.basename(destprojectfile))
        self.assertFalse(os.path.exists(copiedpath))

        try:
            with self.assertRaises(OSError): #unfortunately have to expect a permission denied error when changing group in testing environment
                postrun(destprojectfile, projectFileExtension=".prproj", dataCache={
                    'created_asset_folder': '/tmp/fake_assets'
                })
            self.assertTrue(os.path.exists(copiedpath))
        finally:
            os.unlink(copiedpath)
            os.unlink(destprojectfile)

    def test_postrun_nofolder(self):
        """
        if no asset folder is set, should raise an error
        :return:
        """
        from scripts.copy_project_to_assetfolder import postrun
        with self.assertRaises(RuntimeError):
            postrun("noprojectfile",projectFileExtension="", dataCache={
                'dasjkadjhad': 'dsadsaad'
            })
