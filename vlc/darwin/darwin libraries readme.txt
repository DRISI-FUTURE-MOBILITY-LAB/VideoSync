VLC.app has been modified in the following way to remove symlinks/aliases:

* Content/MacOS/lib/libvlc.dylib has been deleted and replaced with a copy of libvlc.5.dylib

* Content/MacOS/lib/libvlccore.dylib has been deleted and replaced with a copy of libvlccore.7.dylib

This is required because the aliases do not properly extract from the jar files. 