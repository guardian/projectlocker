import xml.etree.ElementTree as ET
import re
import os
import gzip
import shutil
import logging

logger = logging.getLogger("PremiereProjectFile")

def setNodeAttrib(rootNode,nodePath,attribName,attribValue):
    global args

    if rootNode.__class__ == "<class 'xml.etree.ElementTree.ElementTree'>":
        rootNode=rootNode.getroot()

    if nodePath is None:
        sourceNode = rootNode
    else:
        sourceNode=rootNode.find(nodePath)
        if sourceNode is None:
            raise RuntimeError("Unable to find %s node" % nodePath)

    sourceNode.attrib[attribName] = attribValue


def getDoctype(filepath):
    check_expr=re.compile(r'!DOCTYPE')
    found = None

    try:
        f = gzip.open(filepath,"r")
        for line in f:
            line=line.rstrip('\n')
            #print line
            if check_expr.search(line) is not None:
                found = line
                f.close()
                break
    except IOError:
        f = open(filepath,"r")
        for line in f:
            line=line.rstrip('\n')
            if check_expr.search(line) is not None:
                found = line
                f.close()
                break
    finally:
        f.close()
        return found


def loadFile(project_file):
    """
    loads the provided premiere project file as an XML element tree, possibly de-compressing it in memory
    :param project_file:  path to .prproj file
    :return: tuple of (XML elementttree, wasCompressed) where wasCompressed is a boolean indicating whether it should be compressed
    again on output
    """
    print "Loading XML from %s..." % project_file

    #larger premiere projects tend to be gzipped
    isCompressed = True

    try:
        f = gzip.open(project_file,"rb")
        xmltree = ET.fromstring(f.read())
        f.close()
    except IOError: #if gzip doesn't want to read it, then try as a plain file...
        isCompressed = False
        xmltree = ET.parse(project_file)
        xmltree=xmltree.getroot()

    return xmltree, isCompressed


def writeFile(xmltree, projectFilename, is_compressed=False, backups_path="/tmp"):
    """
    Write the given xml tree back out to the file, optionally compressing it on the way. An existing file at that path is
    assumed and will be backed up while the write is in progress
    :param projectFilename:
    :param isCompressed:
    :param backups_path:
    :return:
    """
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
        logger.info("Restoring project from backup...")
        shutil.copy(os.path.join(backups_path,os.path.basename(projectFilename)),projectFilename)
        raise