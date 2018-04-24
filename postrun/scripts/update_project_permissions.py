import logging
import postrun_settings as settings
from pprint import pformat, pprint
from os import stat, chown, chmod

logging.basicConfig(level=logging.INFO)

logger = logging.getLogger("update_project_permissions")
logger.level = logging.DEBUG


def postrun(**kwargs):
    if not 'projectFile' in kwargs:
        logger.error("No projectFile has been passed")
        raise RuntimeError("No projectFile has been passed")

    if not hasattr(settings, 'PROJECT_GROUP'):
        logger.error("Postrun settings has no key PROJECT_GROUP")
        raise RuntimeError("Postrun settings has no key PROJECT_GROUP")

    statinfo = stat(kwargs['projectFile'])
    if statinfo is None:
        logger.error("Projectfile {0} does not exist".format(kwargs['projectFile']))
        raise RuntimeError("Projectfile {0} does not exist".format(kwargs['projectFile']))

    try:
        chown(kwargs['projectFile'], statinfo.st_uid, int(settings.PROJECT_GROUP))
    except Exception as e:
        logger.error(str(e))
        raise

    return {}