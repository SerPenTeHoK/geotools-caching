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
package org.geotools.caching.grid;

import org.geotools.caching.spatialindex.Region;
import org.geotools.caching.spatialindex.grid.GridData;
import org.geotools.caching.spatialindex.grid.GridNode;
import org.geotools.caching.spatialindex.grid.GridRootNode;


public class GridCacheRootNode extends GridRootNode {
    /**
     *
     */
    private static final long serialVersionUID = 6955051761313024458L;
    transient GridTracker grid;

    /** Create a not yet initialized root node.
     *
     * @param grid
     * @param mbr
     */
    GridCacheRootNode(GridTracker grid, Region mbr) {
        super(grid, mbr);
        this.grid = grid;
    }

    GridCacheRootNode(GridTracker grid, Region mbr, int capacity) {
        super(grid, mbr, capacity);
        this.grid = grid;
    }

    @Override
    protected void split() {
        super.split();
    }

    int getCapacity() {
        return super.capacity;
    }

    void setCapacity(int c) {
        super.capacity = c;
    }

    int[] getTilesNumber() {
        return super.tiles_number;
    }

    void setTilesNumber(int[] numb) {
        super.tiles_number = numb;
    }

    double getTileSize() {
        return super.tiles_size;
    }

    void setTileSize(double size) {
        super.tiles_size = size;
    }

    @Override
    protected GridNode createNode(Region reg) {
        return new GridCacheNode(grid, reg);
    }

    @Override
    protected boolean insertData(GridData data) {
        if (!getIdentifier().isValid()) { // FIXME: do not insert same data mutiple times

            return super.insertData(data);
        }

        return false;
    }

    protected GridData getData(int i) {
        return this.data[i];
    }
}
