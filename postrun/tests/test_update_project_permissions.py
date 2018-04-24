import unittest2
import shutil
import xml.etree.ElementTree as ET
from mock import patch
from os import unlink


class TestPostrun(unittest2.TestCase):
    def test_init(self):
        from scripts.update_project_permissions import postrun