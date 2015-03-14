## PagaVCS ##

PagaVCS is a Nautilus and mugCommander integrated free TortoiseSVN clone, Subversion GUI client for Linux desktop. Like Tortoise it can be used directly from the filemanager (Nautilus, mugCommander) via context menu and the Update, Commit and Log operations are accessible from the toolbar too.

### Supported Ubuntu versions ###
  * Ubuntu  9.10 Karmic
  * Ubuntu 10.04 Lucid
  * Ubuntu 10.10 Maverick
  * Ubuntu 11.04 Natty
  * Ubuntu 11.10 Oneiric

## Changelog ##

[pagavcs changelog](http://code.google.com/p/pagavcs/source/browse/trunk/debbuild-pagavcs-template/debian/changelog)
| [pagavcs-core changelog](http://code.google.com/p/pagavcs/source/browse/trunk/debbuild-pagavcs-core-template/debian/changelog)
| [pagavcs-nautilus changelog](http://code.google.com/p/pagavcs/source/browse/trunk/debbuild-pagavcs-nautilus-template/debian/changelog)

**1.4.70**
  * (log-gui) fix memory leak

**1.4.69**
  * (log-gui) better recognize the entry type on show-diff (query it again if it's "unknown", preventing trying to show file-diff for a directory)

**1.4.68**
  * Fix upgrade problem introduced by 1.4.61 (package-dependency problem)

**1.4.67**
  * Artwork refinement

**1.4.66**
  * facelift for the cli: up=update; -wait and -autoclose switches; support relative paths
  * Intelligent Merge-url History: place on the top the urls which was already used with that merge destination

**1.4.65**
  * New artwork

**1.4.64**
  * [Issue 147](https://code.google.com/p/pagavcs/issues/detail?id=147): (log) show differences on double-clicking on the upper table too
  * [Issue 148](https://code.google.com/p/pagavcs/issues/detail?id=148): (log) exception on "show changes" on a newly created directory
  * [Issue 136](https://code.google.com/p/pagavcs/issues/detail?id=136): (log) "show log" on a file in the bottom detail log table too

**1.4.62**
  * Fix 1.4.61

**1.4.61** - wrong release
  * Separate package for the core and the nautilus part. You may need to execute "**sudo apt-get install -f**" in terminal.

**1.4.60**
  * [Issue 146](https://code.google.com/p/pagavcs/issues/detail?id=146):	(properties gui) new feature: add property

**1.4.57**
  * [Issue 119](https://code.google.com/p/pagavcs/issues/detail?id=119): Window sizes are not correct (small) --- merge-gui, resolve-conflict-gui, fix saving window bounds when closing by ESC
  * [Issue 140](https://code.google.com/p/pagavcs/issues/detail?id=140): Use less memory on 64bit OS (using java's compressed pointers) --- reverting

**1.4.56**
  * [Issue 142](https://code.google.com/p/pagavcs/issues/detail?id=142): Nautilus extension doesn't work on Ubuntu 11.10

**1.4.55**
  * [Issue 133](https://code.google.com/p/pagavcs/issues/detail?id=133): Login dialog everytime if you don't need to login (empty username or password can be valid too)

**1.4.54**
  * [Issue 132](https://code.google.com/p/pagavcs/issues/detail?id=132): Support Gnome 3 (and so Ubuntu 11.10)

**1.4.53**
  * [Issue 131](https://code.google.com/p/pagavcs/issues/detail?id=131): Support file:// protocol

## Repository for automatic updates -- PPA (recommended) ##

### Method A ###
```
sudo add-apt-repository ppa:gaborgabor/pagavcs
sudo apt-get update
sudo apt-get install pagavcs
```

### Method B ###
Add the GPG key to your keyring by executing this command in a terminal:
```
sudo apt-key adv --recv-key --keyserver keyserver.ubuntu.com 9B17F2B406D92A67
```

Add to your software sources:
```
ppa:gaborgabor/pagavcs
```
And **install** from either Software-Center, Synaptic or with command line apt-get tool the package **pagavcs**.

## Repository for automatic updates -- google-code repo ##

add this line to the software sources:
```
deb http://pagavcs.googlecode.com/svn/repo karmic main
```

Execute this line from Terminal to get rid of "W: GPG error ... NO\_PUBKEY" warning.

```
wget -q -O - http://pagavcs.googlecode.com/svn/repo/pagavcs-archive.key | sudo apt-key add -
```

And install from either Synaptic or with command line apt-get tool the package **pagavcs**. You can also install the **pagavcs-bigmem** package to increase the memory size of PagaVCS, but it's usually not needed.

## Downloads ##

  * [PagaVCS](http://pagavcs.googlecode.com/svn/repo/pool/main/p/pagavcs/)
  * [mugCommander](http://pagavcs.googlecode.com/svn/repo/pool/main/m/mugcommander/)
  * http://pagavcs.googlecode.com/svn[b/repo/pool/main/m/mugcommander-pagavcs-extensions/ mugCommander PagaVCS extension]
  * [mugCommander Gnome extensions](http://pagavcs.googlecode.com/svn/repo/pool/main/m/mugcommander-gnome-extensions/)
  * [mugCommander Bonjour extension](http://pagavcs.googlecode.com/svn/repo/pool/main/m/mugcommander-bonjour-extensions/)

## Install ##

[Package for Ubuntu is avaible for download. (It might work on Debian too)](http://code.google.com/p/pagavcs/downloads/list)
Nautilus restart ("nautilus -q" command) or computer restart is required after first time install! Otherwise it's not necessery because the nautilus integration part is changing rarely.

![http://pagavcs.googlecode.com/svn/wiki/screenshots/contextmenu.png](http://pagavcs.googlecode.com/svn/wiki/screenshots/contextmenu.png)

## Features ##

  * Subversion: svn, http, https
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
  * Recenet messages for commit

Currently it supports only Gnome, and it's tested only under Ubuntu.

Used libraries:
  * [SVNKIT](http://svnkit.com/)
  * [JCalendar](http://www.toedter.com/en/jcalendar/index.html)
  * [JGoodies Forms](http://www.jgoodies.com/freeware/forms/)

## mugCommander ##

[Changelog](http://code.google.com/p/pagavcs/source/browse/trunk-mugcommander/debbuild/debian/changelog)

mugCommander is a fork of the muCommander. Special thanks to muCommander team for making this amazing file manager ( http://www.mucommander.com ). MugCommander is currently only for Linux, Gnome and it also needs PagaVCS.
Added features to muCommander:
  * **Extension support** (eg.: PagaVCS, Bonjour extension)
  * **PagaVCS support** (context menu, file emblems) as separete plugin.
  * **Open Terminal** function as separate plugin (currently it's in the PagaVCS extension bundle)
  * **Find file function** (name pattern, find in archive, find text, case sensitive/insensitive mode, settable character encoding). The result list is displayed as it was a archive file's content, but the real files can be manipulated there.
  * **Text viewer/editor**: faster scrolling by mouse wheel; cut/copy/paste function in conext menu; fix editor/viewer windows sometimes displays empty (until a resize event); remember editor/viewer window's positions and open in that size on next displaying
  * make default keyboard shortcuts similar as in Total Commander (eg.: ctrl+r=refresh panel, alt+f5=pack)
  * use the standard classloader instead of custom one

![http://pagavcs.googlecode.com/svn/wiki/screenshots/mgc-findfile.png](http://pagavcs.googlecode.com/svn/wiki/screenshots/mgc-findfile.png)

## mugCommander PagaVCS Extension ##
[Changelog](http://code.google.com/p/pagavcs/source/browse/trunk-mugcommander-pagavcs-extension/debbuild/debian/changelog)

## mugCommander Bonjour Extension ##
[Changelog](http://code.google.com/p/pagavcs/source/browse/trunk-mugcommander-bonjour-extension/debbuild/debian/changelog)