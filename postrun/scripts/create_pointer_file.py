import os.path
import logging

logging.basicConfig(level=logging.ERROR)

logger = logging.getLogger("create_pointer_file")


def postrun(projectFile="", projectFileExtension="", dataCache={}, **kwargs):
    if not 'created_asset_folder' in dataCache:
        raise RuntimeError("No created_asset_folder in data cache. This postrun must depend on make_asset_folder")

    ptrFileName = projectFile.replace(projectFileExtension,".ptr")

    logger.info("Creating pointer file at {0}".format(ptrFileName))

    with open(ptrFileName,"w") as f:
        f.write(dataCache['created_asset_folder'])