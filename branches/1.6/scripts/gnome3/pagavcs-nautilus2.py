#!/usr/bin/python3
# -*- coding: UTF-8 -*-
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


from gi.repository import Nautilus, GObject

import os
os.environ["NAUTILUS_PYTHON_REQUIRE_GTK3"] = "1"
import re
import sys
import traceback
import socket
import threading
import urllib
import socket
from threading import Thread


EXECUTABLE = '/usr/bin/pagavcs'
SEPARATOR = '-'
server_creating = False


def sendRequest(request):
	global server_creating
	if (server_creating):
		#print ("DEBUG server is under creating")
		return ""

	try:
		#print ("DEBUG requesting client socket")
		clientsocket = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
		clientsocket.settimeout(60)
		clientsocket.connect(os.getenv("HOME")+"/.pagavcs/socket")
		#print ('sendrequest: '+request)
		clientsocket.sendall(request+'\n')
	except (socket.timeout, socket.error):
		server_creating = True
		StartPagaVCSServerThread().start()
		#print ("DEBUG comm error")
		#traceback.print_exc(file=sys.stdout)
		return ""

	data = ""
	try:
		data = clientsocket.recv(8192)
	except (socket.timeout, socket.error):
		#print ("DEBUG comm timeout or error")
		#traceback.print_exc(file=sys.stdout)
		return  ""
	clientsocket.close()
	return data


class StartPagaVCSServerThread (threading.Thread):

	def __init__(self):
		threading.Thread.__init__(self)
		pass

	def run ( self ):
		global server_creating
		executeCommand = EXECUTABLE+' ping'    
		os.system(executeCommand)
		server_creating = False


class EmblemExtensionSignature(GObject.GObject, Nautilus.InfoProvider):
	
	def __init__(self):
		server_creating = True
		StartPagaVCSServerThread().start()       
		pass

	def update_file_info (self, file):
		filename = urllib.unquote(file.get_uri()[7:])
		data = sendRequest('getfileinfo '+filename)
		if (data != ''):
			file.add_emblem (data)

class PagaVCS(GObject.GObject, Nautilus.MenuProvider):
	def __init__(self):
		#	#self.client = gconf.client_get_default()
		pass
	
	def _do_command(self, menu, strFiles, command):
		# -a for a(sync) operation
		arg = "-a "+command+' '+strFiles
		sendRequest(arg)
	
	def _get_all_items(self, toolbar, strFiles):
		lstItems = []
		if (not toolbar):
			folderitem = Nautilus.MenuItem(name='Nautilus::pagavcs', label='PagaVCS', tip='PagaVCS subversion client', icon='pagavcs-logo')
			submenu = Nautilus.Menu()
			folderitem.set_submenu(submenu)
			actionNamePostfix = ''
		else:
			actionNamePostfix = '-tb'
		
		
		menuItemsString = ''
		menuItemsString = sendRequest('getmenuitems '+strFiles)
		menuItems = menuItemsString.split('\n')
		
		i = 0
		size = len(menuItems) - 1
		while (i < size):
			
			if (menuItems[i] == '--end--'):
				i = i + 1
				continue
			
			if (toolbar):
				if (menuItems[i+4].find('t') == -1):
					i = i + 6
					continue
			#elif (menuItems[i+4].find('s') != -1):
			#	item = Nautilus.MenuItem('pagavcsseparator%d'%i, '––––––––––','')
			#	submenu.append_item(item)
			
			item = Nautilus.MenuItem(name=menuItems[i]+actionNamePostfix, label=menuItems[i+1], tip=menuItems[i+2], icon=menuItems[i+3])			
			item.connect('activate', self._do_command, strFiles, menuItems[i+5])
			if (not toolbar and menuItems[i+4].find('p') == -1):
				submenu.append_item(item)
			else:
				lstItems.append(item)
			i = i + 6
		
		if (not toolbar):
			lstItems.append(folderitem)
			
		return lstItems
	
	
	def get_file_items(self, window, files):
		
		if (len(files)<1):
			return
		
		strFilesList = []
		for file in files:
			if file.get_uri_scheme() != 'file':        
				return
			filename = urllib.unquote(file.get_uri()[7:])
			strFilesList.append('"')
			strFilesList.append(filename)
			strFilesList.append('" ')
		
		strFiles = ''.join(strFilesList)
		# debug
		# print ('get_file_items: '+strFiles)
		return self._get_all_items(False, strFiles)

	def get_background_items(self, window, file):
		
		if file.get_uri_scheme() != 'file':
			return
		filename = urllib.unquote(file.get_uri()[7:])
		
		return self._get_all_items(False, '"'+filename+'"')
	
#	def get_toolbar_items(self, window, file):
#		if file.get_uri_scheme() != 'file':
#			return
#		filename = urllib.unquote(file.get_uri()[7:])
#		
#		return self._get_all_items(True, '"'+filename+'"')
