import unittest2

class TestLoadFile(unittest2.TestCase):
    def test_loadfile_normal(self):
        """
        loadFile should load a regular compressed pr2017 project
        :return:
        """
        from scripts.update_adobe_uuid import loadFile

        xmltree, isCompressed = loadFile("data/blank_premiere_2017.prproj")
        self.assertEqual(isCompressed, True)
        self.assertEqual(str(xmltree.__class__.__name__),"Element")
        self.assertEqual(xmltree.find('Project/RootProjectItem').attrib['ObjectURef'],"2cb1b6c5-c598-4f3b-b463-28e2f52f1d2f")

    def test_loadfile_notfound(self):
        """
        loadFile should raise an IOError if the file is not found
        :return:
        """
        from scripts.update_adobe_uuid import loadFile

        with self.assertRaises(IOError):
            xmltree, isCompressed = loadFile("someinvalidfilename")