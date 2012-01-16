#!/usr/bin/python
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
import urllib

import gtk
import nautilus

import gobject
import gnomevfs
import os
import re
import sys
import traceback
import socket
import threading

EXECUTABLE = '/usr/bin/pagavcs'
SEPARATOR = unicode(u'\u2015'*10)
server_creating = False
DEFAULT_PORT=12905
PORT=-1


def sendRequest(request):
	global server_creating,PORT,DEFAULT_PORT
	if (server_creating):
		#print ("DEBUG server is under creating")
		return ""
	if (PORT<0):
		try:
			stream = open(os.path.expanduser('~/.pagavcs/config.properties'))
			otherchar = re.compile(r"(?<!\\)(\s*\=)|(?<!\\)(\s*\:)")
			bspacere = re.compile(r"\\(?!\s$)")
			otherchar2 = re.compile(r"(\s*\=)|(\s*\:)")
			lines = stream.readlines()
			i = iter(lines)
			for line in i:
				line = line.strip()
				if not line: continue
				if line[0] == '#': continue
				escaped=False
				sepidx = -1
				flag = 0
				m = otherchar.search(line)
				if m:
					first, last = m.span()
					start, end = 0, first
					flag = 1
					wspacere = re.compile(r'(?<![\\\=\:])(\s)')        
				else:
					if otherchar2.search(line):
						wspacere = re.compile(r'(?<![\\])(\s)')        
					start, end = 0, len(line)
				first, last = m.span()
				sepidx = last - 1
				
				while line[-1] == '\\':
					nextline = i.next()
					nextline = nextline.strip()
					line = line[:-1] + nextline
				
				if sepidx != -1:
					key, value = line[:sepidx], line[sepidx+1:]
				else:
						key,value = line,''
				
				if (key == "port"):
					PORT=int(value)
		except (IOError,ValueError):
			print()

	if (PORT<0):
		PORT=DEFAULT_PORT
	
	try:
		clientsocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		clientsocket.settimeout(None)
		clientsocket.connect(("localhost", PORT))
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


class EmblemExtensionSignature(nautilus.InfoProvider):
	def __init__(self):
		server_creating = True
		StartPagaVCSServerThread().start()       
		pass
	
	def update_file_info (self, file):
		filename = urllib.unquote(file.get_uri()[7:])
		data = sendRequest('getfileinfo '+filename)
		if (data != ''):
			file.add_emblem (data)


class PagaVCS(nautilus.MenuProvider):
	def __init__(self):
		#self.client = gconf.client_get_default()
		pass
	
	def _do_command(self, menu, strFiles, command):
		sendRequest(command+' '+strFiles)
	
	def _get_all_items(self, toolbar, strFiles):
		lstItems = []
		if (not toolbar):
			folderitem = nautilus.MenuItem('Nautilus::pagavcs','PagaVCS','PagaVCS subversion client')
			folderitem.set_property('icon', 'pagavcs-logo')
			submenu = nautilus.Menu()
			folderitem.set_submenu(submenu)
			actionNamePostfix = ''
		else:
			actionNamePostfix = '-tb'
		
		
		menuItemsString = ''
		menuItemsString = sendRequest('getmenuitems '+strFiles)
		menuItems = menuItemsString.split('\n')
		
		i = 0
		while (i < (len(menuItems) - 1)):
			
			if (menuItems[i] == '--end--'):
				i = i + 1
				continue
			
			if (toolbar):
				if (menuItems[i+4].find('t') == -1):
					i = i + 6
					continue
			#elif (menuItems[i+4].find('s') != -1):
			#	item = nautilus.MenuItem('pagavcsseparator%d'%i, '––––––––––','')
			#	submenu.append_item(item)
			
			item = nautilus.MenuItem(menuItems[i]+actionNamePostfix, menuItems[i+1], menuItems[i+2])
			item.set_property('icon', menuItems[i+3])
			
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
		#print ('get_file_items: '+strFiles)
		return self._get_all_items(False, strFiles)

	def get_background_items(self, window, file):
		
		if file.get_uri_scheme() != 'file':
			return
		filename = urllib.unquote(file.get_uri()[7:])
		
		return self._get_all_items(False, '"'+filename+'"')
	
	def get_toolbar_items(self, window, file):
		if file.get_uri_scheme() != 'file':
			return
		filename = urllib.unquote(file.get_uri()[7:])
		
		return self._get_all_items(True, '"'+filename+'"')
