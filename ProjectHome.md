![http://pagavcs.googlecode.com/svn/wiki/screenshots/contextmenu.png](http://pagavcs.googlecode.com/svn/wiki/screenshots/contextmenu.png)
[![http://pagavcs.googlecode.com/svn/wiki/screenshots/commit.png](http://pagavcs.googlecode.com/svn/wiki/screenshots/commit.png)

## PagaVCS ##

PagaVCS is a Nautilus, Thunar and mugCommander integrated TortoiseSVN clone, Subversion GUI client for Linux desktop. Like Tortoise it can be used directly from the filemanager (Nautilus, Thunar, mugCommander) via context menu.

Pagavcs-17 is the new generation supporting Subversion 1.7.
For old Pagavcs is now in maintenance-only state, the Pagavcs-17 is the preferred and developed version.

[Version information](Version.md)

### Supported Ubuntu versions ###
Currently:
  * Ubuntu 15.04 Vivid
  * Ubuntu 14.10 Utopic
  * Ubuntu 14.04 Trusty
Deprecated:
  * Ubuntu 13.10 Saucy
  * Ubuntu 13.04 Raring (workaround needed, see below)
  * Ubuntu 12.10 Quantal
  * Ubuntu 12.04 Precise

(The 1.6 series supports 11.10 Oneiric too.)
(The 1.4.x series supports 9.10 Karmic, 10.04 Lucid, 10.10 Maverick, 11.04 Natty too. [See more](PagaVCS14Series.md))

## 13.04 Nautilus workaround ##
It's a [known Ubuntu bug](https://bugs.launchpad.net/ubuntu/+source/nautilus-python/+bug/1170017), after installing PagaVCS execute in terminal:

64 bit:
```
sudo ln -sf /usr/lib/x86_64-linux-gnu/libpython2.7.so.1.0 /usr/lib/
sudo ln -sf /usr/lib/x86_64-linux-gnu/libpython2.7.so.1 /usr/lib/
sudo ldconfig
killall nautilus
```

32 bit:
```
sudo ln -sf /usr/lib/i386-linux-gnu/libpython2.7.so.1.0 /usr/lib/libpython2.7.so.1.0
sudo ln -sf /usr/lib/i386-linux-gnu/libpython2.7.so.1 /usr/lib/libpython2.7.so.1
sudo ldconfig
killall nautilus
```


## Install ##
The Ubuntu Launchpad PPA repository has to be set, it provides automatic updates too.

```
sudo apt-key adv --recv-key --keyserver keyserver.ubuntu.com 9B17F2B406D92A67
sudo add-apt-repository ppa:gaborgabor/pagavcs
sudo apt-get update

sudo apt-get install pagavcs-17
# for Nautilus support
sudo apt-get install pagavcs-17-nautilus
# for Nemo suppoert
sudo apt-get install pagavcs-17-nemo
# for Thunar suppoert
sudo apt-get install pagavcs-17-thunar

# file manager or computer restart is required after install

```

If you are behind proxy you may need to change the first instruction to (substituting your own proxy and port):
```
sudo apt-key adv --recv-key --keyserver keyserver.ubuntu.com --keyserver-options http-proxy=http://[proxy]:[port] 9B17F2B406D92A67"
```


## Alternative Install ##
The Ubuntu Launchpad PPA repository has to be set, it provides automatic updates too.

```
sudo apt-key adv --recv-key --keyserver keyserver.ubuntu.com 9B17F2B406D92A67
sudo add-apt-repository ppa:gaborgabor/pagavcs
sudo apt-get update

# you have 3 choices: (you can choose only one)

# 1, most cutting edge version: PagaVCS-17-dev
sudo apt-get install pagavcs-17-dev

# 2, stable PagaVCS-17:
sudo apt-get install pagavcs-17

# 3, for older the "plain" PagaVCS
sudo apt-get install pagavcs

# and install the requested filemanager plugin; eg. pagavcs-17-nautilus

# file manager or computer restart is required after install
```

If you are behind proxy you may need to change the first instruction to (substituting your own proxy and port):
```
sudo apt-key adv --recv-key --keyserver keyserver.ubuntu.com --keyserver-options http-proxy=http://[proxy]:[port] 9B17F2B406D92A67"
```

## Features ##

  * Subversion: svn, http, https, file
  * compatible with subversion 1.8 server
  * Subversion: Update ([screenshot](http://pagavcs.googlecode.com/svn/wiki/screenshots/update.png))
  * Subversion: Commit ([screenshot](http://pagavcs.googlecode.com/svn/wiki/screenshots/commit.png))
  * Subversion: Show log ([screenshot](http://pagavcs.googlecode.com/svn/wiki/screenshots/showlog.png))
  * Subversion: Add
  * Subversion: Delete
  * Subversion: Ignore
  * Subversion: Unignore
  * Subversion: Resolve conflict
  * Subversion: Cleanup
  * Subversion: Merge ([screenshot](http://pagavcs.googlecode.com/svn/wiki/screenshots/other.png))
  * Subversion: Switch ([screenshot](http://pagavcs.googlecode.com/svn/wiki/screenshots/other.png))
  * Subversion: Blame ([screenshot](http://pagavcs.googlecode.com/svn/wiki/screenshots/other.png))
  * Subversion: Export ([screenshot](http://pagavcs.googlecode.com/svn/wiki/screenshots/other.png))
  * Subversion: Repository Browser ([screenshot](http://pagavcs.googlecode.com/svn/wiki/screenshots/repobrowser.png))
  * Subversion: Create patch, Apply patch
  * It doesn’t slow down the nautilus navigation at all, even very large working copies don't affect the performance
  * Marks versioned files
  * Recent messages for commit

Used libraries:
  * [SVNKIT](http://svnkit.com/)
  * [JCalendar](http://www.toedter.com/en/jcalendar/index.html)
  * [JGoodies Forms](http://www.jgoodies.com/freeware/forms/)
  * [Log4J](http://logging.apache.org/log4j/)
  * [Unix Sockets Libray](http://www.matthew.ath.cx/projects/java/)

---
Contact: [pagavcs@gmail.com](mailto:pagavcs@gmail.com)

[![](https://www.paypal.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=W2TK5AAMSPTQS&lc=HU&currency_code=HUF&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted)