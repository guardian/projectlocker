import os.path


def postrun(projectFilename="", projectFileExtension="", dataCache={}, **kwargs):
    if not 'created_asset_folder' in dataCache:
        raise RuntimeError("No created_asset_folder in data cache. This postrun must depend on make_asset_folder")

    ptrFileName = projectFilename.replace(projectFileExtension,".ptr")
    with open(ptrFileName,"w") as f:
        f.write(dataCache['created_asset_folder'])