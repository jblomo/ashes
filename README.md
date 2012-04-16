# ashes

Sync PDF documents to kindle from a computer using Kindle's notion of "collections."

ashes is primarily built to help you manage reading PDFs on your kindle.  There
are several Kindle Collections and syncing managers out there but I wanted
something a little different:

- Open Source
- Convert 2 column PDFs to 1 column for optimized Kindle reading
- Workflow for reading: documents are in 1 of 'new', 'later', or 'read'
collections.
- If a paper is 'read' on any device, it will be moved to the 'read' collection
on every device.

This is an ALPHA release.

## Usage

     mount /dev/sdc1 /mnt/kindle
     lein2 run -k /mnt/kindle/ -c ~/Documents/cs-papers/
     umount /mnt/kindle && sudo eject /dev/sdc
     # restart (not just turn off!) kindle

ashes operates on filesystems, so the kindle device must be mounted for
everything to work.  If you `ls /mnt/kindle` you should see subdirectories
including `documents` and `systems`.

The computer directory to sync (`cs-papers` in the example above) should have 3
subdirectories: `new`, `later`, `read`.  Each subdirectory contains the PDFs or
other documents you are interested in syncing.  PDFs ending in `-2c.pdf` are
considered tagged as 2 column PDFs and will be split into 1 column.  Right now,
tagging is manual: ashes will not recognize 2 column PDFs on its own.

If everything goes well, you'll see INFO statements describing the files being
copied, and finally a "Kindle updated" message.  Now it is safe to unmount and
eject the device.

Kindle will not recognize the new collection settings unless you *restart* the
device.  This can be done by the options `Home -> Menu -> Settings -> Menu ->
Restart` or holding the on/off switch for 15 seconds.  After restarting, Kindle
will take a couple minutes to reload the collections.

Reading PDFs can be more easily read in Landscape mode or, for split PDFs, by
zooming in to "actual size."

After reading a paper on the Kindle, place it in the "read" collection and the
next time you sync, the original PDF will be placed in the "read" folder on your
computer.

## Plans

I'll be using GitHub issues to track ideas, but as high priority I'd like to

- play well with collections not controlled by ashes. ashes current completely
overwrites the collections.json file.
- automatically detect 2 column PDFs
- set page sizes for 2 column PDFs such that one can normally page through them

## License

Copyright (C) 2012 Jim Blomo

Distributed under the Eclipse Public License, the same as Clojure.
