/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2010 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.file.impl.local;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.FileURL;
import com.mucommander.file.ProtocolProvider;

import java.io.IOException;

/**
 * This class is the provider for the local filesystem implemented by {@link com.mucommander.file.impl.local.LocalFile}.
 *
 * @author Maxence Bernard
 * @see com.mucommander.file.impl.local.LocalFile
 */
public class LocalProtocolProvider implements ProtocolProvider {

    public AbstractFile getFile(FileURL url, Object... instantiationParams) throws IOException {
        return instantiationParams.length==0
            ?new LocalFile(url)
            :new LocalFile(url, (java.io.File)instantiationParams[0]);
    }
}
