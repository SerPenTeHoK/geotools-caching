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

import junit.framework.TestCase;
import org.geotools.caching.spatialindex.Node;
import org.geotools.caching.spatialindex.Region;
import org.geotools.caching.spatialindex.RegionNodeIdentifier;
import org.geotools.caching.spatialindex.Storage;
import org.geotools.caching.spatialindex.grid.Grid;
import org.geotools.caching.spatialindex.grid.GridNode;


public abstract class AbstractStorageTest extends TestCase {
    Storage store;
    TestNode n;
    RegionNodeIdentifier id;
    Grid grid;

    protected void setUp() {
        grid = new Grid(new Region(new double[] { 0, 0 }, new double[] { 1, 1 }), 10,
                new MemoryStorage(100));
        n = new TestNode(grid, new Region(new double[] { 0, 0 }, new double[] { 1, 1 }));
        id = new RegionNodeIdentifier(n);
        store = createStorage();
    }

    abstract Storage createStorage();

    public void testPut() {
        store.put(n);
        store.put(n);
    }

    public void testGet() {
        store.put(n);

        Node g = store.get(id);
        assertEquals(n.getIdentifier(), g.getIdentifier());
    }

    public void testRemove() {
        store.put(n);
        store.get(id);
        store.remove(id);
        store.get(id);
        store.put(n);
        store.get(id);
    }
}
