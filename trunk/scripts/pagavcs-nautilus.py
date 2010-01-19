# PagaVCS is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
# 
# PagaVCS is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with PagaVCS;  If not, see <http://www.gnu.org/licenses/>.
#
# Thanks for Martin Enlund for his example code!
import urllib

import gtk
import nautilus
import gconf

import gobject
import gnomevfs
import os
import sys
import socket

EXECUTABLE = '/usr/bin/pagavcs'
SEPARATOR = unicode(u'\u2015'*10)



class EmblemExtensionSignature(nautilus.InfoProvider):
    def __init__(self):
        pass
    def update_file_info (self, file):
        filename = urllib.unquote(file.get_uri()[7:])
        if os.path.isdir(filename):
            svnpath = filename+'/.svn';
        else:
            (filepath, filename) = os.path.split(filename)
            svnpath = filepath+'/.svn';
            
        if (os.path.exists(svnpath)) and (os.path.isdir(svnpath)):
            clientsocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            clientsocket.settimeout(1) 
            clientsocket.connect(("localhost", 12905))
            try:
                clientsocket.sendall("getfileinfo "+filename+"\n")
            except socket.timeout:
                return

            data = ""
            try:
                data = clientsocket.recv(512)
            except socket.timeout:
                return  
            clientsocket.close()
            if (data == "svned"):
                file.add_emblem ('pagavcs-svn')

class PagaVCS(nautilus.MenuProvider):
    def __init__(self):
        self.client = gconf.client_get_default()
        
    def _is_versioned(self, file):
        filename = urllib.unquote(file.get_uri()[7:])
        return self._is_versioned_filename(filename)
        
    def _is_versioned_filename(self, filename):
        if os.path.isdir(filename):
            svnpath = filename+'/.svn';
        else:
            (filepath, filename) = os.path.split(filename)
            svnpath = filepath+'/.svn';
            
        return (os.path.exists(svnpath) and os.path.isdir(svnpath))
        
    def _is_parentdir_versioned(self, file):
        dirname = urllib.unquote(file.get_uri()[7:])
        if not os.path.isdir(dirname):
            (filepath, filename) = os.path.split(filename)
            dirname=filepath
        parentdir = os.path.normpath(os.path.join(dirname, '..'))
        return parentdir != dirname and self._is_versioned_filename(parentdir)

    def _do_command(self, file, command):
        filename = urllib.unquote(file.get_uri()[7:])
        executeCommand = EXECUTABLE+' '+command+' "'+filename+'" &'    
        os.system(executeCommand)
        
    def update_menu_activate_cb(self, menu, file):
        self._do_command(file, 'update')

    def commit_menu_activate_cb(self, menu, file):
        self._do_command(file, 'commit')
        
    def log_menu_activate_cb(self, menu, file):
        self._do_command(file, 'log')

    def ignore_menu_activate_cb(self, menu, file):
        self._do_command(file, 'ignore')
    
    def unignore_menu_activate_cb(self, menu, file):
        self._do_command(file, 'unignore')
    
    def delete_menu_activate_cb(self, menu, file):
        self._do_command(file, 'delete')
        
    def revert_menu_activate_cb(self, menu, file):
        self._do_command(file, 'revert')
    
    def checkout_menu_activate_cb(self, menu, file):
        self._do_command(file, 'checkout')    
    
    def cleanup_menu_activate_cb(self, menu, file):
        self._do_command(file, 'cleanup')
    
    def other_menu_activate_cb(self, menu, file):
        self._do_command(file, 'other')
    
    def settings_menu_activate_cb(self, menu, file):
        self._do_command(file, 'settings')
        
    def _get_all_items(self, file):
        folderitem = nautilus.MenuItem('Nautilus::pagavcs','PagaVCS','PagaVCS subversion client')
        folderitem.set_property('icon', 'pagavcs-logo')
        submenu = nautilus.Menu()
        folderitem.set_submenu(submenu)    
            
        item = nautilus.MenuItem('NautilusPython::update_file_item',
                                 'Update' ,
                                 'Update %s' % file.get_name())
        item.set_property('icon', 'pagavcs-update')
        item.connect('activate', self.update_menu_activate_cb, file)
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::commit_file_item',
                                 'Commit' ,
                                 'Commit %s' % file.get_name())
        item.set_property('icon', 'pagavcs-commit') 
        item.connect('activate', self.commit_menu_activate_cb, file)
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::log_file_item',
                                 'Log' ,
                                 'Log %s' % file.get_name())
        item.connect('activate', self.log_menu_activate_cb, file)
        item.set_property('icon', 'pagavcs-log')
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::ignore_file_item',
                                 'Ignore' ,
                                 'Ignore %s' % file.get_name())
        item.set_property('icon', 'pagavcs-ignore')
        item.connect('activate', self.ignore_menu_activate_cb, file)
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::unignore_file_item',
                                 'Unignore' ,
                                 'Unignore %s' % file.get_name())
        item.set_property('icon', 'pagavcs-unignore')
        item.connect('activate', self.unignore_menu_activate_cb, file)
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::delete_file_item',
                                 'Delete' ,
                                 'Delete %s' % file.get_name())
        item.set_property('icon', 'pagavcs-delete')
        item.connect('activate', self.delete_menu_activate_cb, file)
        submenu.append_item(item)
                
        item = nautilus.MenuItem('NautilusPython::revert_file_item',
                                 'Revert' ,
                                 'Revert In %s' % file.get_name())
        item.set_property('icon', 'pagavcs-revert')
        item.connect('activate', self.revert_menu_activate_cb, file)
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::checkout_file_item',
                                 'Checkout' ,
                                 'Checkout %s' % file.get_name())
        item.set_property('icon', 'pagavcs-checkout')
        item.connect('activate', self.checkout_menu_activate_cb, file)
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::cleanup_file_item',
                                 'Cleanup' ,
                                 'Cleanup %s' % file.get_name())
        item.set_property('icon', 'pagavcs-cleanup')
        item.connect('activate', self.cleanup_menu_activate_cb, file)        
        submenu.append_item(item)

        item = nautilus.MenuItem('NautilusPython::other_file_item',
                                 'Other' ,
                                 'Other %s' % file.get_name())
        item.set_property('icon', 'pagavcs-other')
        item.connect('activate', self.other_menu_activate_cb, file)
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::settings_file_item',
                                 'Settings' ,
                                 'Settings')
        item.set_property('icon', 'pagavcs-settings')
        item.connect('activate', self.settings_menu_activate_cb, file)
        submenu.append_item(item)
        
        return folderitem,    
    
    def _get_unversioned_but_parent_is_versioned_items(self, file):
    	folderitem = nautilus.MenuItem('Nautilus::pagavcs','PagaVCS','PagaVCS subversion client')
    	folderitem.set_property('icon', 'pagavcs-logo')
        submenu = nautilus.Menu()
        folderitem.set_submenu(submenu)    
            
        item = nautilus.MenuItem('NautilusPython::ignore_file_item',
                                 'Ignore' ,
                                 'Ignore %s' % file.get_name())
        item.set_property('icon', 'pagavcs-ignore')
        item.connect('activate', self.ignore_menu_activate_cb, file)
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::unignore_file_item',
                                 'Unignore' ,
                                 'Unignore %s' % file.get_name())
        item.set_property('icon', 'pagavcs-unignore')
        item.connect('activate', self.unignore_menu_activate_cb, file)
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::delete_file_item',
                                 'Delete' ,
                                 'Delete %s' % file.get_name())
        item.set_property('icon', 'pagavcs-delete')
        item.connect('activate', self.unignore_menu_activate_cb, file)
        submenu.append_item(item)
                
        item = nautilus.MenuItem('NautilusPython::revert_file_item',
                                 'Revert' ,
                                 'Revert %s' % file.get_name())
        item.set_property('icon', 'pagavcs-revert')
        item.connect('activate', self.revert_menu_activate_cb, file)
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::settings_file_item',
                                 'Settings' ,
                                 'Settings')
        item.set_property('icon', 'pagavcs-settings')
        item.connect('activate', self.settings_menu_activate_cb, file)
        submenu.append_item(item)
        
        return folderitem, 
    
    def _get_full_unversioned_items(self, file):
        folderitem = nautilus.MenuItem('Nautilus::pagavcs','PagaVCS','PagaVCS subversion client')
        folderitem.set_property('icon', 'pagavcs-logo')
        submenu = nautilus.Menu()
        folderitem.set_submenu(submenu)    
            
        item = nautilus.MenuItem('NautilusPython::checkout_file_item',
                                 'Checkout' ,
                                 'Checkout %s' % file.get_name())
        item.set_property('icon', 'pagavcs-checkout')
        item.connect('activate', self.checkout_menu_activate_cb, file)
        submenu.append_item(item)
        
        item = nautilus.MenuItem('NautilusPython::settings_file_item',
                                 'Settings' ,
                                 'Settings')
        item.set_property('icon', 'pagavcs-settings')
        item.connect('activate', self.settings_menu_activate_cb, file)

        submenu.append_item(item)
        
        return folderitem, 
    
    def get_file_items(self, window, files):
        if len(files) != 1:
            return
        
        file = files[0]
        # not file.is_directory() or 
        if file.get_uri_scheme() != 'file':        
            return
            
        if not self._is_versioned(file):
            if self._is_parentdir_versioned(file):
                 return self._get_unversioned_but_parent_is_versioned_items(file)
            else:
                 return self._get_full_unversioned_items(file)
        else:
            return self._get_all_items(file)

    def get_background_items(self, window, file):
    
        if file.get_uri_scheme() != 'file':        
            return
        if not self._is_versioned(file):
            if self._is_parentdir_versioned(file):
                 return self._get_unversioned_but_parent_is_versioned_items(file)
            else:
                 return self._get_full_unversioned_items(file)
        else:
            return self._get_all_items(file)
        
    def get_toolbar_items(self, window, file):
        if not self._is_versioned(file):
             return
                     
        item1 = nautilus.MenuItem('NautilusPython::tb_update_file_item',
                                 'Update' ,
                                 'Update %s' % file.get_name())
        item1.set_property('icon', 'pagavcs-update')                                 
        item1.connect('activate', self.update_menu_activate_cb, file)
        
        item2 = nautilus.MenuItem('NautilusPython::tb_commit_file_item',
                                 'Commit' ,
                                 'Commit %s' % file.get_name())
        item2.set_property('icon', 'pagavcs-commit') 
        item2.connect('activate', self.commit_menu_activate_cb, file)      
        
        item3 = nautilus.MenuItem('NautilusPython::tb_log_file_item',
                                 'Log' ,
                                 'Log %s' % file.get_name())
        item3.set_property('icon', 'pagavcs-log')                                 
        item3.connect('activate', self.log_menu_activate_cb, file)
                             
        return item1, item2, item3
