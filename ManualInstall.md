# Download latest PagaVCS debian package #
From here: http://pagavcs.googlecode.com/svn/repo/pool/main/p/pagavcs/

# (Install the program "alien") #
On debian/ubuntu system:
  * command line:  sudo apt-get install alien
  * use synaptic or Software Center

# Unpack it #

alien --scripts -g pagavcs\_1.4.46\_all.deb
(change the filename if necessary)

# Copy to the system #

Now you see a new directory:
pagavcs-1.4.46
Inside you can see an "usr" directory, you need to copy its content to your root filesystem.

# Make PagaVCS run #
  * A) reboot computer
  * B) in command line: nautilus -q