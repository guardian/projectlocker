import unittest2
import os

class TestCreatePointerFile(unittest2.TestCase):
    def test_postrun_normal(self):
        """
        should create a pointer file for the asset folder location
        :return:
        """
        from scripts.create_pointer_file import postrun

        postrun(projectFilename="/tmp/myproject.prproj",projectFileExtension=".prproj",
                dataCache={'created_asset_folder':"/path/to/my/assets"})

        self.assertTrue(os.path.exists("/tmp/myproject.ptr"))
        with open("/tmp/myproject.ptr") as f:
            content = f.read()
            self.assertEqual(content,"/path/to/my/assets")

        os.unlink("/tmp/myproject.ptr")

    def test_postrun_nodata(self):
        """
        should raise an exception if the asset folder path is not given
        :return:
        """

        from scripts.create_pointer_file import postrun

        with self.assertRaises(RuntimeError) as raised_excep:
            postrun(projectFilename="/tmp/myproject.prproj",projectFileExtension=".prproj",
                dataCache={'invalidkey':"/path/to/my/assets"})
        self.assertEqual(str(raised_excep.exception),"No created_asset_folder in data cache. This postrun must depend on make_asset_folder")