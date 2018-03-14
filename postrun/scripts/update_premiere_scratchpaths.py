#!/usr/bin/python
__author__ = 'Andy Gallagher <andy.gallagher@theguardian.com>'

import logging
from PremiereProjectFile import *
import postrun_settings

logging.basicConfig(level=logging.ERROR)

logger = logging.getLogger("update_premiere_scratchpaths")


def setNodeTo(scratchNode,nodeName,newText):
    """
    updates or creates a node within a parent xmltree node
    :param scratchNode: parent node to search within
    :param nodeName: node to update
    :param newText: new text for the node
    :return: None
    """
    node = None
    targetNode = nodeName
    #in later versions of Premiere, there can be multiple locations with numbers after them...
    n=0
    while node is None:
        node = scratchNode.find(targetNode)
        targetNode = "%s%d" % (nodeName,n)
        n += 1
        if(n>100):
            raise StandardError("Unable to find node for %s" % nodeName)

    node.text = newText


def load_premiere_project(project_file):
    """
    loads the given premiere file path into memory as an xml element tree
    :param project_file: path to load
    :return: tuple of xmltree, is_compressed (boolean). Raises if the path can't be found
    """
    #larger premiere projects tend to be gzipped
    isCompressed = True
    try:
        f = gzip.open(project_file,"rb")
        xmltree = ET.fromstring(f.read())
        f.close()
    except IOError: #if gzip doesn't want to read it, then try as a plain file...
        isCompressed = False
        xmltree = ET.parse(project_file)

    return xmltree, isCompressed


def postrun(projectFile="", projectFileExtension="", dataCache={}, **kwargs):
    if not 'created_asset_folder' in dataCache:
        raise RuntimeError("No created_asset_folder value in datacache. This postrun must depend on make_asset_folder.")

    logger.info("Loading XML from %s..." % projectFile)
    xmltree, is_compressed = load_premiere_project(projectFile)

    backups_path = getattr(postrun_settings,'BACKUPS_PATH', '/tmp')
    logger.info("Project backup path is {0}".format(backups_path))

    #now we've got the project, find where the scratch disk settings are
    scratchNode = xmltree.find("ScratchDiskSettings") #remember, this starts from <PremiereData>, the root node
    if scratchNode is None:
        raise RuntimeError("ERROR: Unable to find scratch disk settings in XML (PremiereData/ScratchDiskSettings node)")

    #set AV capture to the Assets folder
    targetNodeList = ['CapturedVideoLocation',
                      'CapturedAudioLocation',
                      ]
    for nodeName in targetNodeList:
        try:
            setNodeTo(scratchNode,nodeName,dataCache['created_asset_folder'] + "/")
        except StandardError as e:
            logger.warning("WARNING: %s" % e.message)

    transfer_folder=os.path.join(dataCache['created_asset_folder'], "TransferMedia")
    try:
        os.makedirs(transfer_folder)
    except OSError as e:
        if e.errno!=17: #errno 17=> path already exists
            print "WARNING: System returned error code %d (%s) when trying to create directory %s" % (e.errno,e.strerror,transfer_folder)

    setNodeTo(scratchNode,'TransferMediaLocation',transfer_folder)
    print ET.dump(scratchNode)

    writeFile(xmltree, projectFile, is_compressed=is_compressed,backups_path=backups_path)
