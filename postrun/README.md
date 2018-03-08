# Postrun Actions

A "Postrun Action" is a Python script that is called in response to a new project having been created,
but before the user is informed.

The idea is that operations such as creating asset folders,
modifying the project file, etc. can be done by these operations.

## Creating a postrun action

All postrun actions must reside in a location that is defined in application.conf.
By default, this is `postrun/scripts` in the app home directory.

Any script in here will be considered a viable postrun action.

In order to use the script, it must be associated with a Project Type
in the UI.  Any time a project is created of this Type, the script
will be run.

Additionally, dependencies between postrun actions can be declared in
the Postrun Actions section of the UI.

The script directory is scanned every 60s while the server is operational
and any new scripts present will be added to the system in order to be visible in
the UI.

### Calling convention

A postrun script should provide a function called `postrun`.
This will be called with the following keyword arguments:

- **projectFile**: path of the created project file
- **projectId**: numeric ID of the created project entry
- **vidispineProjectId**: vidispine ID of the project, or an empty string if none is set
- **projectTitle**: title as provided by the user when creating
- **projectCreated**: timestamp of project creation, as an ISO string
- **projectOwner**: name of the user who created the project
- **projectTypeId**: numeric ID of the project type used to create the project
- **projectTypeName**: name of the project type used to create the project
- **projectOpensWith**: app required client-side to open the project
- **projectTargetVersion**: version of the app which this is intended for
- **projectFileExtension**: recognised file extension of the project

If all of that seems like a lot of typing, as normal in Python you can get all kwargs as a dictionary
by using the double-splat `**` operator, e.g.:
                                         
```
def postrun(**kwargs):
 print "I was called with {0}".format(kwargs)
```

### Returning

Currently, the script exit code and return values are ignored.
Anything output to stdout and stderr is captured and will be shown
in the projectlocker log if the creation fails.

### Indicating an error

In order to indicate a failure, simply raise an exception that best describes
what went wrong and include a string description of the fault.
The project creation will be cancelled, and the exception along with any output from
stdout or stderr will be output to the projectlocker log to be examined

### Time limit

At present, a time limit of 30s for any postrun action is hardcoded.

## Execution environment

The scripts are run using Jython 2.7.1.  This means that you cannot rely
on any C-specific extensions; in practise there are very few of these.

If you require any external modules you should add them to `requirements.txt` in the `postrun/`
directory and they will be included in the build rpm.

The Jython libraries are contained in the `postrun/lib/python` directory.
