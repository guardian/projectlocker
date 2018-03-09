def postrun(**kwargs):
    print "I was provided with {0}".format(kwargs)
    return {'answer': 'Hello world!'}