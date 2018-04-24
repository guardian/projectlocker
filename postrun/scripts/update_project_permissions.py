import logging
import postrun_settings as settings
from pprint import pformat, pprint
import stat
import os
from os import chown, chmod
from sys import stderr

logging.basicConfig(level=logging.INFO)

logger = logging.getLogger("update_project_permissions")
logger.level = logging.DEBUG


def postrun(**kwargs):
    if not 'projectFile' in kwargs:
        stderr.write("No projectFile has been passed")
        raise RuntimeError("No projectFile has been passed")

    if not hasattr(settings, 'PROJECT_GROUP'):
        stderr.write("Postrun settings has no key PROJECT_GROUP")
        raise RuntimeError("Postrun settings has no key PROJECT_GROUP")

    statinfo = os.stat(kwargs['projectFile'])
    if statinfo is None:
        stderr.write("Projectfile {0} does not exist".format(kwargs['projectFile']))
        raise RuntimeError("Projectfile {0} does not exist".format(kwargs['projectFile']))

    try:
        chown(kwargs['projectFile'], statinfo.st_uid, int(settings.PROJECT_GROUP))
        chmod(kwargs['projectFile'], stat.S_IRUSR|stat.S_IWUSR|stat.S_IRGRP|stat.S_IWGRP|stat.S_IROTH)
    except Exception as e:
        stderr.write(str(e))
        raise
