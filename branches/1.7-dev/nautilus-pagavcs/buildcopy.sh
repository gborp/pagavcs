#!/bin/sh

nautilus -q
make && cp /home/mhp/projects/pagavcs/branches/1.7-dev/nautilus-pagavcs/src/.libs/libnautilus-pagavcs.so /usr/lib/nautilus/extensions-3.0/
