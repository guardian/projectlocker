#!/usr/bin/python

import requests
import hashlib
import copy
import hmac
from email.utils import formatdate
import base64
import json

def sign_request(original_headers, method, path, content_body, shared_secret):
    """
    returns a dictionary including a suitable authorization header
    :param original_headers: original content headers
    :param content_body: data that is being sent
    :return: new headers dictionary
    """
    new_headers = copy.deepcopy(original_headers)

    content_hasher = hashlib.sha384()
    content_hasher.update(content_body)

    nowdate = formatdate(usegmt=True)
    new_headers['X-Sha384-Checksum'] = base64.b64encode(content_hasher.digest())
    new_headers['Content-Length'] = str(len(content_body))
    new_headers['Date'] = nowdate

    string_to_sign = """{date}\n{contentlength}\n{checksum}\n{method}\n{path}""".format(
        date=nowdate,contentlength=new_headers['Content-Length'],checksum=new_headers['X-Sha384-Checksum'],
        method=method,path=path
    )

    print "debug: string to sign: {0}".format(string_to_sign)

    hmaccer = hmac.new(shared_secret, string_to_sign, hashlib.sha384)
    result = base64.b64encode(hmaccer.digest())
    print "debug: final digest is {0}".format(result)
    new_headers['Authorization'] = "testscript:{0}".format(result)
    return new_headers

#START MAIN
shared_secret = "rubbish"

print "GET request"
print "---------------"
signed_headers = sign_request({}, "GET", "/api/storage", "", shared_secret)

result = requests.get("http://localhost:9000/api/storage", headers=signed_headers)
print "Server returned {0}: {1}".format(result.status_code, result.content)

print "\n----------------"
print "PUT request"
print "\n----------------"

body = json.dumps({'somekey': 'somevalue'})

signed_headers = sign_request({'Content-Type': 'application/json'}, "PUT", "/api/storage", body, shared_secret)
result = requests.put("http://localhost:9000/api/storage", data=body, headers=signed_headers)
print "Server returned {0}: {1}".format(result.status_code, result.content)