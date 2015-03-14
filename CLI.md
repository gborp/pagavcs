# Command LIne Interface #

usage: pagavcs (command) (argument(s))
  * update        Update working copy.
  * commit        Show commit dialog for working copy.
  * og           Show log for working copy.
  * gnore        Add file or directory to ignore list.
  * unignore      Remove file or directory from ignore list.
  * delete        Delete file or directory.
  * revert        Revert file or directory.
  * checkout      Checkout a working copy.
  * cleanup       Cleanup working copy.
  * resolve       Resolve conflict.
  * other         Display other window.
  * settings      Display settings window.
  * stop          Stop PagaVCS server.
  * getmenuitems  Get menuitems for file or directory. It is used by the nautilus-plugin.
  * -h, --help    Display this help.

Example:
> pagavcs update "/home/johnny/big-project" "/home/johnny/mega-project"

The qoutation marks are mandatory!