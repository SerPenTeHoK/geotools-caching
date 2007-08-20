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
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.geotools.caching.spatialindex.Region;
import org.geotools.caching.spatialindex.grid.GridData;


public class GridNodeTest extends TestCase {
    GridNode node;
    Region mbr;
    String data = "Sample data";
    String data2 = "Sample data 2";

    public static Test suite() {
        return new TestSuite(GridNodeTest.class);
    }

    public void setUp() {
        mbr = new Region(new double[] { 0, 1 }, new double[] { 2, 3 });
        node = new GridNode(new Grid(), mbr);
    }

    public void testConstructor() {
        assertEquals(mbr, node.mbr);
        assertEquals(new Region(mbr), node.mbr);

        GridNode child = new GridNode(new Grid(), mbr);
        assertEquals(0, child.getLevel());
        assertEquals(mbr, node.mbr);
        assertEquals(new Region(mbr), node.mbr);
    }

    void populate() {
        for (int i = 0; i < 10; i++) {
            node.insertData(new GridData(8, mbr, data));
            node.insertData(new GridData(16, mbr, data2));
        }
    }

    public void testInsert() {
        populate();
        assertEquals(20, node.num_data);
        assertEquals(8, node.data[0].id);
        assertEquals(data2, node.data[1].getData());
        assertEquals(8, node.data[18].id);
        assertEquals(data2, node.data[19].getData());
    }

    public void testDelete() {
        populate();
        node.deleteData(14);
        node.deleteData(15);
        assertEquals(18, node.num_data);
        assertEquals(8, node.data[0].id);
        assertEquals(data2, node.data[1].getData());
        assertEquals(16, node.data[14].id);
        assertEquals(data, node.data[15].getData());
        assertEquals(null, node.data[19]);

        try {
            node.deleteData(18);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }
}
