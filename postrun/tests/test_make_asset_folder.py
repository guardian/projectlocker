# -*- coding: utf-8 -*-

import unittest2
from mock import MagicMock, patch
import os.path


class TestRunHelperScript(unittest2.TestCase):
    class MockedProcess(object):
        def __init__(self, return_code):
            self.returncode = return_code

        def communicate(self):
            return "fake stdout","fake stderr"

    def test_run_helper_script_normal(self):
        """
        _run_helper_script should run sudo helper and return successfully if it does
        :return:
        """
        with patch('subprocess.Popen',return_value=self.MockedProcess(return_code=0)) as mocked_popen:
            from scripts.make_asset_folder import _run_helper_script
            from subprocess import PIPE

            mypath = os.path.realpath(os.path.dirname(os.path.realpath(__file__)) + "/..")
            scriptpath = os.path.join(mypath, 'scripts','mkdir_on_behalf_of.pl')

            expected_run_args = "/usr/bin/sudo -n {0} \"/path/to/my/asset_folder\" 500 500".format(scriptpath)

            _run_helper_script("/path/to/my/asset_folder",None,False)

            mocked_popen.assert_called_once_with(expected_run_args, shell=True, stdout=PIPE, stderr=PIPE)

    def test_run_helper_script_error(self):
        """
        _run_helper_script should raise a FolderCreationFailed if the script returns non-zero
        :return:
        """
        with patch('subprocess.Popen',return_value=self.MockedProcess(return_code=1)) as mocked_popen:
            from scripts.make_asset_folder import _run_helper_script, FolderCreationFailed
            from subprocess import PIPE

            mypath = os.path.realpath(os.path.dirname(os.path.realpath(__file__)) + "/..")
            scriptpath = os.path.join(mypath, 'scripts','mkdir_on_behalf_of.pl')

            expected_run_args = "/usr/bin/sudo -n {0} \"/path/to/my/asset_folder\" 500 500".format(scriptpath)

            with self.assertRaises(FolderCreationFailed) as raised_excep:
                _run_helper_script("/path/to/my/asset_folder",None,False)

            mocked_popen.assert_called_once_with(expected_run_args, shell=True, stdout=PIPE, stderr=PIPE)
            self.assertEqual(str(raised_excep.exception), expected_run_args)


class TestMakeSafeString(unittest2.TestCase):
    def test_make_safe_string(self):
        """
        make_safe_string should replace any non-alphanumeric character with a _
        :return:
        """
        from scripts.make_asset_folder import make_safe_string

        self.assertEqual(make_safe_string(u"my fünny project   title!"),"my_f_nny_project_title")


class TestPostrun(unittest2.TestCase):
    def test_postrun_normal(self):
        """
        postrun() launch function should create an asset folder path from args and call the helper script
        :return:
        """
        with patch('scripts.make_asset_folder._run_helper_script') as mock_run:
            from scripts.make_asset_folder import postrun
            result = postrun(workingGroupName="Hard Work Group",commissionTitle="Serious Investigations", projectOwner="ken_smith", projectTitle="Where did my money go?")

            mock_run.assert_called_once_with("/tmp/hard_work_group/serious_investigations/ken_smith_where_did_my_money_go",raven_client=None, fixmode=False)
            self.assertDictEqual(result,{'created_asset_folder': "/tmp/hard_work_group/serious_investigations/ken_smith_where_did_my_money_go"})

    def test_postrun_missing_args(self):
        """
        postrun() should report missing args
        :return:
        """
        with patch('scripts.make_asset_folder._run_helper_script') as mock_run:
            from scripts.make_asset_folder import postrun
            with self.assertRaises(RuntimeError) as raised_excep:
                postrun(commissionTitle="Serious Investigations", projectOwner="ken_smith", projectTitle="Where did my money go?")
            self.assertEqual(str(raised_excep.exception),"Could not create asset folder, missing working group and/or commission? see log for details")
            mock_run.assert_not_called()