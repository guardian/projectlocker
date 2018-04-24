import logging
import postrun_settings as settings
from pprint import pformat, pprint
from os import stat, chown, chmod
from sys import stderr

logging.basicConfig(level=logging.INFO)

logger = logging.getLogger("update_project_permissions")
logger.level = logging.DEBUG


def _run_helper_script(project_file_location,raven_client,fixmode=False):
    """
    Run the setuid helper, to actually create/fix asset folders
    :param asset_folder_location: path on-disk to create or fix
    :param raven_client: Raven client instance for reporting errors
    :param fixmode: boolean value, passed to the suid script. If True, it will not error out if e.g. folder already exists.
    :return: None.  Raises CalledProcessError or FolderCreationFailed if the script fails.
    """
    import os
    from subprocess import Popen,PIPE,CalledProcessError

    mypath = os.path.dirname(os.path.realpath(__file__))
    logger.debug("My path is {0}".format(mypath))

    args = "/usr/bin/sudo -n {p} \"{f}\"".format(p=os.path.join(mypath, 'fix_project_permission.pl'),
                                                         f=project_file_location)
    if fixmode:
        args += " --fixmode"

    if raven_client is not None:
        raven_client.extra_context({'create_script': os.path.join(mypath, 'fix_project_permission.pl')})
    logger.debug(pformat(args))
    try:
        proc = Popen(args, shell=True, stdout=PIPE, stderr=PIPE)
        (stdout_text, stderr_text) = proc.communicate()
        if raven_client is not None:
            raven_client.extra_context({
                'stdout_text': stdout_text,
                'stderr_text': stderr_text,
            })
        if proc.returncode != 0:
            raise CalledProcessError(proc.returncode, args)
    except CalledProcessError as e:
        if raven_client is not None:
            raven_client.extra_context({
                'create_script'        : os.path.join(mypath, 'scripts/fix_project_permission.pl'),
            })
            raven_client.captureException()

        logger.error("Error calling {0}: {1}".format(args, str(e)))
        logger.error(stderr_text)
        logger.error(stdout_text)
        raise RuntimeError("Error calling external script")
    logger.debug("Script output {0}\n{1}".format(stdout_text, stderr_text))


def postrun(**kwargs):
    if not 'projectFile' in kwargs:
        stderr.write("No projectFile has been passed")
        raise RuntimeError("No projectFile has been passed")

    if not hasattr(settings, 'PROJECT_GROUP'):
        stderr.write("Postrun settings has no key PROJECT_GROUP")
        raise RuntimeError("Postrun settings has no key PROJECT_GROUP")

    _run_helper_script(kwargs['PROJECT_GROUP'], raven_client=None)
    # statinfo = stat(kwargs['projectFile'])
    # if statinfo is None:
    #     stderr.write("Projectfile {0} does not exist".format(kwargs['projectFile']))
    #     raise RuntimeError("Projectfile {0} does not exist".format(kwargs['projectFile']))
    #
    # try:
    #     chown(kwargs['projectFile'], statinfo.st_uid, int(settings.PROJECT_GROUP))
    # except Exception as e:
    #     stderr.write(str(e))
    #     raise
