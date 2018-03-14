import shutil
import os.path
import logging

logger = logging.getLogger("copy_project_to_assetfolder")


def postrun(projectFilename="",projectFileExtension="",dataCache={},**kwargs):
    if not 'created_asset_folder' in dataCache:
        raise RuntimeError("No created_asset_folder value in data cache. This action must depend on make_asset_folder.")

    destfile = os.path.join(dataCache['created_asset_folder'], os.path.basename(projectFilename))
    logger.info("Going to copy {0} to {1}".format(projectFilename, destfile))
    shutil.copy(projectFilename, destfile)