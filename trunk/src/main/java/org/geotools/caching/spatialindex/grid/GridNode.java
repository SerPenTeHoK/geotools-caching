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

import java.io.Serializable;
import org.geotools.caching.spatialindex.Node;
import org.geotools.caching.spatialindex.NodeIdentifier;
import org.geotools.caching.spatialindex.Region;
import org.geotools.caching.spatialindex.RegionNodeIdentifier;
import org.geotools.caching.spatialindex.Shape;
import org.geotools.caching.spatialindex.SpatialIndex;
import org.geotools.caching.spatialindex.grid.GridData;


/** A node in the grid.
 * Data objects are stored in an array.
 * Extra data about the node may be stored in node_data, which is a HashMap.
 *
 * @author Christophe Rousson, SoC 2007, CRG-ULAVAL
 *
 */
public class GridNode implements Node, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 7786313461725794946L;
    Region mbr;

    //HashMap node_data;
    int num_data;

    //protected int[] data_ids;
    protected GridData[] data;
    transient protected RegionNodeIdentifier id = null;

    //transient boolean visited = false;
    transient protected Grid grid;

    /**No-arg constructor for serialization purpose.
     * Deserialized nodes must call init(Grid grid) before any other operation.
     *
     */
    protected GridNode() {
    }

    protected GridNode(Grid grid, Region mbr) {
        this.mbr = new Region(mbr);
        //this.parent = parent;
        //this.node_data = new HashMap();
        this.num_data = 0;
        this.data = new GridData[10];
        //this.data_ids = new int[10];
        this.grid = grid;
    }

    /**Post-deserialization initialization.
     *
     * @param grid
     */
    public void init(SpatialIndex grid) {
        this.grid = (Grid) grid;
    }

    public NodeIdentifier getChildIdentifier(int index)
        throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("GridNode have no children.");
    }

    public Shape getChildShape(int index) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("GridNode have no children.");
    }

    public int getChildrenCount() {
        return 0;
    }

    public int getLevel() {
        return 0;
    }

    public boolean isIndex() {
        return false;
    }

    public boolean isLeaf() {
        return true;
    }

    public NodeIdentifier getIdentifier() {
        if (id == null) {
            id = new RegionNodeIdentifier(this);

            if (grid.node_ids.containsKey(id)) {
                id = grid.node_ids.get(id);
            } else {
                grid.node_ids.put(id, id);
            }
        }

        return id;
    }

    public Shape getShape() {
        return mbr;
    }

    /** Insert new data in this node.
     *
     * @param id of data
     * @param data
     */
    protected boolean insertData(GridData data) {
        if (num_data == this.data.length) {
            //int[] n_data_ids = new int[this.data.length * 2];
            GridData[] n_data = new GridData[this.data.length * 2];
            //System.arraycopy(data_ids, 0, n_data_ids, 0, num_data);
            System.arraycopy(this.data, 0, n_data, 0, num_data);
            //data_ids = n_data_ids;
            this.data = n_data;
        }

        //data_ids[num_data] = data.id;
        this.data[num_data] = data;
        num_data++;

        return true;
    }

    /** Delete blindly data at the given index.
     * Index is not the id of the data, the search should be performed by the Grid class,
     * which determines the index of data to delete, and then call this method.
     *
     * @param index
     */
    protected void deleteData(int index) {
        if ((index < 0) || (index > (num_data - 1))) {
            throw new IndexOutOfBoundsException();
        }

        if (index < (num_data - 1)) {
            //data_ids[index] = data_ids[num_data - 1];
            data[index] = data[num_data - 1];
        }

        data[num_data - 1] = null;
        num_data--;
    }

    /** Erase all data referenced by this node.
     *
     */
    public void clear() {
        this.num_data = 0;
        this.data = new GridData[10];

        //this.data_ids = new int[10];
    }

    public int getDataCount() {
        return this.num_data;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("GridNode: MBR:" + mbr);

        return sb.toString();
    }
}
