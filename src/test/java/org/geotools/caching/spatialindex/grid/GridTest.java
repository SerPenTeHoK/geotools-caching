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
package org.geotools.caching.spatialindex.grid;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.geotools.caching.spatialindex.AbstractSpatialIndex;
import org.geotools.caching.spatialindex.AbstractSpatialIndexTest;
import org.geotools.caching.spatialindex.Region;
import org.geotools.caching.spatialindex.store.MemoryStorage;


public class GridTest extends AbstractSpatialIndexTest {
    Grid index;

    public static Test suite() {
        return new TestSuite(GridTest.class);
    }

    protected AbstractSpatialIndex createIndex() {
        index = new Grid(new Region(universe), 100, new MemoryStorage(100));

        return index;
    }

    public void testInsertion() {
        super.testInsertion();
        System.out.println("Root insertions = " + index.root_insertions);
    }
}
