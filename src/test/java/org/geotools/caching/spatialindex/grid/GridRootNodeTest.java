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
import java.util.Iterator;
import org.geotools.caching.spatialindex.NodeIdentifier;
import org.geotools.caching.spatialindex.Region;
import org.geotools.caching.spatialindex.store.MemoryStorage;


public class GridRootNodeTest extends GridNodeTest {
    GridRootNode node;
    GridRootNode node3D;
    Region mbr3D;
    int size = 100;
    int size3D = 1000;

    public static Test suite() {
        return new TestSuite(GridRootNodeTest.class);
    }

    public void setUp() {
        mbr = new Region(new double[] { 0, 20 }, new double[] { 10, 30 });
        mbr3D = new Region(new double[] { 0, 20, 40 }, new double[] { 10, 30, 50 });

        Grid index = new Grid(mbr, 100, new MemoryStorage(100));
        node = new GridRootNode(index, mbr, size);
        node3D = new GridRootNode(index, mbr3D, size3D);
        super.node = node;
    }

    public void testContructor() {
        assertTrue(node.capacity >= size);
    }

    public void testConstructor3D() {
        assertTrue(node3D.capacity >= size3D);
    }

    public void testIncrement() {
        int dims = node.tiles_number.length;
        double[] pos = new double[dims];
        double[] nextpos = new double[dims];
        double[] posback = new double[dims];
        double[] nextposback = new double[dims];

        for (int i = 0; i < dims; i++) {
            pos[i] = node.mbr.getLow(i);
            posback[i] = pos[i];
            nextpos[i] = pos[i] + node.tiles_size;
            nextposback[i] = nextpos[i];
        }

        node.increment(pos, nextpos);
        assertEquals(nextposback[0], pos[0], 0);

        for (int i = 1; i < dims; i++) {
            assertEquals(posback[i], pos[i], 0);
        }

        int count = 1;

        do {
            count++;
        } while (node.increment(pos, nextpos));

        assertEquals(node.capacity, count);

        for (int i = 0; i < dims; i++) {
            assertEquals(posback[i], pos[i], 0);
        }
    }

    public void testGridIndexToNodeId() {
        int[] index = new int[] { 1, 2, 3 };
        int id = node3D.gridIndexToNodeId(index);
        int of = ((int) Math.pow(size3D, 1d / 3)) + 1;
        assertEquals((of * of * 3) + (of * 2) + 1, id);
    }

    public void testSplit() {
        node.split();
        assertEquals(node.capacity, node.children.size());

        double size = node.getShape().getArea() / node.capacity;

        for (Iterator it = node.children.iterator(); it.hasNext();) {
            NodeIdentifier next = (NodeIdentifier) it.next();
            assertTrue(node.getShape().intersects(next.getShape()));
            assertEquals(size, next.getShape().getArea(), 1e-2);
        }
    }

    public void testSplit3D() {
        node3D.split();
        assertEquals(node3D.capacity, node3D.children.size());

        double size = node3D.getShape().getArea() / node3D.capacity;

        for (Iterator it = node3D.children.iterator(); it.hasNext();) {
            NodeIdentifier next = (NodeIdentifier) it.next();
            assertTrue(node3D.getShape().intersects(next.getShape()));
            assertEquals(size, next.getShape().getArea(), 1e-2);
        }
    }
}
