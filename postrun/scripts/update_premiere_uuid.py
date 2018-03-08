#!/usr/bin/python
__author__ = 'Andy Gallagher <andy.gallagher@theguardian.com>'

#This script should overwrite the project's uuid with a new one to allow Portal to find it

import os
import re
import gzip
import xml.etree.ElementTree as ET
import uuid
import shutil
import traceback

backups_path = "/tmp/.backup"


def setNodeAttrib(rootNode,nodePath,attribName,attribValue):
    global args

    print rootNode.__class__

    if rootNode.__class__ == "<class 'xml.etree.ElementTree.ElementTree'>":
        print "info: re-checking root node"
        rootNode=rootNode.getroot()

    if nodePath is None:
        sourceNode = rootNode
    else:
        sourceNode=rootNode.find(nodePath)
        if sourceNode is None:
            raise Exception("Unable to find %s node" % nodePath)

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
            #print line
            #print check_expr.search(line)
            if check_expr.search(line) is not None:
                #print "Found it!"
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

def doUpdate(xmltree,attrib_spec,new_uuid=None):
    """
    Updates the element tree with the new UUID, in every place identified in attrib_spec
    :param xmltree: xml element tree to update
    :param attrib_spec: list of dictionaries, containing 'xpath' and 'attrib' elements
    :param new_uuid: uuid to replace with, or None. Defaults to None, in which case a new uuid is generated here
    :return: the updated xmltree
    """
    if new_uuid is None:
        new_uuid = str(uuid.uuid4())

    print "New UUID is %s" % new_uuid

    for ref in attrib_spec:
        setNodeAttrib(xmltree,ref['xpath'],ref['attrib'],new_uuid)

    return xmltree


def save_updated_xml(xmltree, project_file, doctype, isCompressed=False):
    """
    Writes the modified xml tree out to the project file again, recompressing if isCompressed is true.
      Makes a backup copy first in case the output goes wrong.
    :param xmltree: xml tree to output
    :param project_file: path to project file
    :param doctype: doctypes are not retained by ElementTree, so if a doctype was present in the original doc
    specify it here and it will be prepended to the document.
    :param isCompressed: if true, the project file is compressed with gzip as it's saved
    :return:
    """

    try:
        if not os.path.exists(backups_path):
            os.makedirs(backups_path)

        shutil.copy(project_file,os.path.join(backups_path,os.path.basename(project_file)))

        if isCompressed:
            print "INFO: writing gzip compressed file"
            f = gzip.open(project_file,"wb")
        else:
            print "INFO: writing plain, uncompressed file"
            f = open(project_file,"wb")

        if(doctype):
            f.write(doctype+"\n")

        f.write(ET.tostring(xmltree))
        f.close()

    except Exception as e:
        print "ERROR: Unable to back up or replace original project file with modified version: %s" % e.message
        print traceback.print_exc()
        print "Restoring project from backup..."
        shutil.copy(os.path.join(backups_path,os.path.basename(project_file)),project_file)
        raise


def postrun(projectFilename=None,**kwargs):
    """
    Main postrun function that is called by projectlocker
    :param projectFilename: project filename to update
    :param kwargs: other arguments
    :return: None
    """
    print "Updating project {0}".format(projectFilename)
    premiereAttribSpec = [ {'xpath': 'Project/RootProjectItem', 'attrib': 'ObjectURef'},
                        {'xpath': 'RootProjectItem', 'attrib': 'ObjectUID' }]
    (xmltree, is_compressed) = loadFile(projectFilename)

    doctype = None
    try:
        doctype = getDoctype(projectFilename)
        print "got doctype %s" % doctype
    except Exception as e:
        print "WARNING: Unable to get doctype: %s" % e.message

    doUpdate(xmltree,premiereAttribSpec)

    save_updated_xml(xmltree,projectFilename, doctype, is_compressed)

    preludeAttribSpec = [ {'xpath': None, 'attrib': 'ClassID'} ]
    #doRewrite("%s/%s.plproj" % (args.destpath,args.vsid), preludeAttribSpec)
