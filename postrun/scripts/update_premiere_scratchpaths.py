#!/usr/bin/python
__author__ = 'Andy Gallagher <andy.gallagher@theguardian.com>'

import os
import xml.etree.ElementTree as ET
import gzip
import shutil
import traceback
import logging

logger = logging.getLogger("update_premiere_scratchpaths")


#This needs to be the path to the assets folder ON THE CLIENT MAC
assets_root = "/Volumes/Multimedia2/Media Production/Assets"
#default_video_preview = "/Volumes/Multimedia2/Media Production/Previews/Video/"
encoding_root = "/Volumes/Multimedia2/Media Production/Encoding"
#This needs to be the path to the projects store ON THE LINUX SERVER
projects_root = "/srv/projectfiles/tmp"
backups_path = "/srv/projectfiles/.backup"

def setNodeTo(scratchNode,nodeName,newText):
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


def postrun(projectFilename="", projectFileExtension="", dataCache={}, **kwargs):
    if not 'created_asset_folder' in dataCache:
        raise RuntimeError("No created_asset_folder value in datacache. This postrun must depend on make_asset_folder.")

    logger.info("Loading XML from %s..." % projectFilename)
    xmltree, is_compressed = load_premiere_project(projectFilename)

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

    dvd_encoding_folder = os.path.join(encoding_root, "DVD")

    try:
        os.makedirs(dvd_encoding_folder)
    except OSError as e:
        if e.errno!=17: #errno 17=> path already exists
            print "WARNING: System returned error code %d (%s) when trying to create directory %s" % (e.errno,e.strerror,dvd_encoding_folder)

    setNodeTo(scratchNode,'DVDEncodingLocation',dvd_encoding_folder)

    transfer_folder=os.path.join(dataCache['created_asset_folder'], "TransferMedia")

    try:
        os.makedirs(transfer_folder)
    except OSError as e:
        if e.errno!=17: #errno 17=> path already exists
            print "WARNING: System returned error code %d (%s) when trying to create directory %s" % (e.errno,e.strerror,transfer_folder)

    setNodeTo(scratchNode,'TransferMediaLocation',transfer_folder)
    print ET.dump(scratchNode)

    #Now we've updated the elementtree, write it back out to the original file
    try:
        if not os.path.exists(backups_path):
            os.makedirs(backups_path)

        shutil.move(projectFilename,os.path.join(backups_path,os.path.basename(projectFilename)))

        if is_compressed:
            logger.info("writing gzip compressed file")
            f = gzip.open(projectFilename,"wb")
        else:
            logger.info("writing plain, uncompressed file")
            f = open(projectFilename,"wb")

        tree = ET.ElementTree(xmltree)
        tree.write(f)
        f.close()

    except Exception as e:
        logger.error("Unable to back up or replace original project file with modified version: %s" % e.message)
        print traceback.print_exc()
        logger.info("Restoring project from backup...")
        shutil.copy(os.path.join(backups_path,os.path.basename(projectFilename)),projectFilename)
        raise
