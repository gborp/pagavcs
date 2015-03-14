# PagaVCS

PagaVCS is a Nautilus, Thunar and mugCommander integrated TortoiseSVN clone, Subversion GUI client for Linux desktop. Like Tortoise it can be used directly from the filemanager (Nautilus, Thunar, mugCommander) via context menu.

Pagavcs-17 is the new generation supporting Subversion 1.7+.
The old Pagavcs is in maintenance-only state now, the Pagavcs-17 is the preferred and developed version.

Contact: pagavcs@gmail.com


Supported Ubuntu versions:
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
(The 1.4.x series supports 9.10 Karmic, 10.04 Lucid, 10.10 Maverick, 11.04 Natty too. [PagaVCS14Series See more])

# Install
The Ubuntu Launchpad PPA repository has to be set, it provides automatic updates too.

{{{
sudo apt-key adv --recv-key --keyserver keyserver.ubuntu.com 9B17F2B406D92A67
sudo add-apt-repository ppa:gaborgabor/pagavcs
sudo apt-get update

echo install pagavcs-17 or pagavcs-17-dev
sudo apt-get install pagavcs-17
echo for Nautilus support:
sudo apt-get install pagavcs-17-nautilus
echo for Nemo support:
sudo apt-get install pagavcs-17-nemo
echo for Thunar support:
sudo apt-get install pagavcs-17-thunar

echo file manager or computer restart is required after install

}}}

If you are behind proxy you may need to change the first instruction to (substituting your own proxy and port):
{{{
sudo apt-key adv --recv-key --keyserver keyserver.ubuntu.com --keyserver-options http-proxy=http://[proxy]:[port] 9B17F2B406D92A67"
}}}

# 13.04 Nautilus workaround
It's a [https://bugs.launchpad.net/ubuntu/+source/nautilus-python/+bug/1170017 known Ubuntu bug], after installing PagaVCS execute in terminal:

64 bit:
{{{
sudo ln -sf /usr/lib/x86_64-linux-gnu/libpython2.7.so.1.0 /usr/lib/
sudo ln -sf /usr/lib/x86_64-linux-gnu/libpython2.7.so.1 /usr/lib/
sudo ldconfig
killall nautilus
}}}

32 bit:
{{{
sudo ln -sf /usr/lib/i386-linux-gnu/libpython2.7.so.1.0 /usr/lib/libpython2.7.so.1.0
sudo ln -sf /usr/lib/i386-linux-gnu/libpython2.7.so.1 /usr/lib/libpython2.7.so.1
sudo ldconfig
killall nautilus
}}}

# Features

  * Subversion: svn, http, https, file
  * compatible with subversion 1.8 server
  * Subversion: Update ([http://pagavcs.googlecode.com/svn/wiki/screenshots/update.png screenshot])
  * Subversion: Commit ([http://pagavcs.googlecode.com/svn/wiki/screenshots/commit.png screenshot])
  * Subversion: Show log ([http://pagavcs.googlecode.com/svn/wiki/screenshots/showlog.png screenshot])
  * Subversion: Add
  * Subversion: Delete
  * Subversion: Ignore
  * Subversion: Unignore
  * Subversion: Resolve conflict
  * Subversion: Cleanup
  * Subversion: Merge ([http://pagavcs.googlecode.com/svn/wiki/screenshots/other.png screenshot])
  * Subversion: Switch ([http://pagavcs.googlecode.com/svn/wiki/screenshots/other.png screenshot])
  * Subversion: Blame ([http://pagavcs.googlecode.com/svn/wiki/screenshots/other.png screenshot])
  * Subversion: Export ([http://pagavcs.googlecode.com/svn/wiki/screenshots/other.png screenshot])
  * Subversion: Repository Browser ([http://pagavcs.googlecode.com/svn/wiki/screenshots/repobrowser.png screenshot])
  * Subversion: Create patch, Apply patch
  * It doesn’t slow down the nautilus navigation at all, even very large working copies don't affect the performance
  * Marks versioned files
  * Recent messages for commit

Used libraries:
  * [http://svnkit.com/ SVNKIT]
  * [http://www.toedter.com/en/jcalendar/index.html JCalendar]
  * [http://www.jgoodies.com/freeware/forms/ JGoodies Forms]
  * [http://logging.apache.org/log4j/ Log4J]
  * [http://www.matthew.ath.cx/projects/java/ Unix Sockets Libray]
