# first steps #

first checkout the sourecode to a new folder (url:
http://pagavcs.googlecode.com/svn/trunk/ )
than use your favourite IDE. I use eclipse, so there's an eclipse workspace
committed. Unpack it and load that workspace. You need to have 1.6 java (sun or open)
jdk installed
If you have done some fixes, then make a patch from it, and send it me. I'll review
your changes, and if it's good, I'll commit them (and you'll be mentioned as
contributor of course)

# How to test your changes #

1, stop the running pagavcs by issueing the command (practically in the alt+f2 run popup in gnome):
pagavcs stop
2, start in debug mode the launch target PagaVCS in eclipse
3, in the eclipse console it displays upon successfull start: "PagaVCS started.". if you see there "Port
is not free, maybe PagaVCS is already running?" then repeat the steps from the first step.
4, I test in the gui what I wanna to test, sometimes placing breakpoints, watching variable value etc.

so if the nautilus-backend interface is not changed then you have to only stop the installed PagaVCS and start your developing version. that's all.

# Structure #

- backend (communicating with the SVNKit subversion library etc..)
- swing GUI (communicates with the backend only)
- nautilus integration (communicates with the backend only)
- mugCommander intergration (communicates with the backend only)

The backend and the swing GUI are both packaged into the pagavcs.jar


The nautilus integration part automatically wakes up the installed PagaVCS if no PagaVCS backend is running. So after you finished the testing just close the eclipse and do a refresh in the nautilus, it'll start the installed PagaVCS.