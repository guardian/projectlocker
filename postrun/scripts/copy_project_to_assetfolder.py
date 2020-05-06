import shutil
from os import chown, chmod
import stat
import os.path
import logging
from sys import stderr

logging.basicConfig(level=logging.ERROR)

logger = logging.getLogger("copy_project_to_assetfolder")
logger.level=logging.INFO


def postrun(projectFile="",projectFileExtension="",dataCache={},**kwargs):
    import postrun_settings as settings

    if not 'created_asset_folder' in dataCache:
        raise RuntimeError("No created_asset_folder value in data cache. This action must depend on make_asset_folder.")

    destfile = os.path.join(dataCache['created_asset_folder'], os.path.basename(projectFile))
    logger.info("Going to copy {0} to {1}".format(projectFile, destfile))
    shutil.copy(projectFile, destfile)

    statinfo = os.stat(destfile)
    if statinfo is None:
        stderr.write("Projectfile {0} does not exist".format(destfile))
        raise RuntimeError("Projectfile {0} does not exist".format(destfile))

    try:
        chown(destfile, statinfo.st_uid, int(settings.PROJECT_GROUP))
        chmod(destfile, stat.S_IRUSR|stat.S_IWUSR|stat.S_IRGRP|stat.S_IWGRP|stat.S_IROTH)
    except Exception as e:
        stderr.write(str(e))
        raise
