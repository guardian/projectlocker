import shutil
import os.path
import logging

logging.basicConfig(level=logging.ERROR)

logger = logging.getLogger("copy_project_to_assetfolder")


def postrun(projectFile="",projectFileExtension="",dataCache={},**kwargs):
    if not 'created_asset_folder' in dataCache:
        raise RuntimeError("No created_asset_folder value in data cache. This action must depend on make_asset_folder.")

    destfile = os.path.join(dataCache['created_asset_folder'], os.path.basename(projectFile))
    logger.info("Going to copy {0} to {1}".format(projectFile, destfile))
    shutil.copy(projectFile, destfile)