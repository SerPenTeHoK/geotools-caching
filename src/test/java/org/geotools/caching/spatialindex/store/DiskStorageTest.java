/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002-2006, GeoTools Project Managment Committee (PMC)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.caching.spatialindex.store;

import junit.framework.Test;
import junit.framework.TestSuite;
import java.io.File;
import java.io.IOException;
import org.geotools.caching.spatialindex.Storage;


public class DiskStorageTest extends AbstractStorageTest {
    public static Test suite() {
        return new TestSuite(DiskStorageTest.class);
    }

    @Override
    Storage createStorage() {
        try {
            DiskStorage storage = new DiskStorage(File.createTempFile("cache", ".tmp"), 1000);
            storage.setParent(this.grid);

            return storage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
