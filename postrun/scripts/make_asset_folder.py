"""
This script is acutually a kind of sudo reverse-trapdoor, which calls the real (suidperl) script with root privileges.
"""
import logging
import postrun_settings as settings
from pprint import pformat
logger = logging.getLogger("make_asset_folder")


class FolderCreationFailed(StandardError):
    pass


def _run_helper_script(asset_folder_location,raven_client,fixmode=False):
    """
    Run the setuid helper, to actually create/fix asset folders
    :param asset_folder_location: path on-disk to create or fix
    :param raven_client: Raven client instance for reporting errors
    :param fixmode: boolean value, passed to the suid script. If True, it will not error out if e.g. folder already exists.
    :return: None.  Raises CalledProcessError or FolderCreationFailed if the script fails.
    """
    import os
    from subprocess import Popen,PIPE,CalledProcessError

    af_owner = "0"
    if hasattr(settings, 'ASSET_FOLDER_OWNER'):
        af_owner = settings.ASSET_FOLDER_OWNER
    af_group = "0"
    if hasattr(settings, 'ASSET_FOLDER_GROUP'):
        af_group = settings.ASSET_FOLDER_GROUP

    if raven_client is not None:
        raven_client.extra_context({
            'owner': af_owner,
            'group': af_group
        })

    mypath = os.path.dirname(os.path.realpath(__file__))
    logger.debug("My path is {0}".format(mypath))

    args = "sudo {p} \"{f}\" {o} {g}".format(p=os.path.join(mypath, 'scripts/mkdir_on_behalf_of.pl'),
                                             f=asset_folder_location,
                                             o=str(af_owner), g=str(af_group))
    if fixmode:
        args += " --fixmode"

    if raven_client is not None:
        raven_client.extra_context({'create_script': os.path.join(mypath, 'scripts/mkdir_on_behalf_of.pl')})
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
                'asset_folder_location': asset_folder_location,
                'create_script'        : os.path.join(mypath, 'scripts/mkdir_on_behalf_of.pl'),
                'owner'                : af_owner,
                'group'                : af_group,
            })
            raven_client.captureException()

        logger.error("Error calling {0}: {1}".format(args, str(e)))
        logger.error(stderr_text)
        logger.error(stdout_text)
        raise FolderCreationFailed(args)
    logger.debug("Script output {0}\n{1}".format(stdout_text, stderr_text))


def postrun()