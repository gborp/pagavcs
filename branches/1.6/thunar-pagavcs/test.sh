#!/bin/sh

make
sudo cp src/.libs/thunar_pagavcs.so /usr/lib/thunarx-2 && sudo  ldconfig -l /usr/lib/thunarx-2/thunar_pagavcs.so && thunar
