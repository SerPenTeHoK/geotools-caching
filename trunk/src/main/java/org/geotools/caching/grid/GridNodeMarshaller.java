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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.geotools.caching.spatialindex.Node;
import org.geotools.caching.spatialindex.Region;
import org.geotools.caching.spatialindex.grid.Grid;
import org.geotools.caching.spatialindex.grid.GridData;
import org.geotools.caching.util.SimpleFeatureMarshaller;
import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;


public class GridNodeMarshaller {
    GridTracker grid;
    SimpleFeatureMarshaller marshaller = new SimpleFeatureMarshaller();

    public byte[] marshall(Node node) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(node.getShape());
        oos.writeInt(node.getDataCount());

        if (node instanceof GridCacheNode) {
            oos.writeBoolean(false);
            write((GridCacheNode) node, oos);
        } else if (node instanceof GridCacheRootNode) {
            oos.writeBoolean(true);
            write((GridCacheRootNode) node, oos);
        }

        oos.close();
        baos.close();

        return baos.toByteArray();
    }

    void write(GridCacheNode n, ObjectOutputStream oos)
        throws IOException {
        for (int i = 0; i < n.getDataCount(); i++) {
            GridData gd = n.getData(i);
            oos.writeInt(gd.getIdentifier());
            oos.writeObject(gd.getShape());
            marshaller.marshall((Feature) gd.getData(), oos);
        }
    }

    void write(GridCacheRootNode n, ObjectOutputStream oos)
        throws IOException {
        oos.writeInt(n.getCapacity());
        oos.writeDouble(n.getTileSize());
        oos.writeObject(n.getTilesNumber());

        for (int i = 0; i < n.getDataCount(); i++) {
            GridData gd = n.getData(i);
            oos.writeInt(gd.getIdentifier());
            oos.writeObject(gd.getShape());
            marshaller.marshall((Feature) gd.getData(), oos);
        }
    }

    public Node unmarshall(byte[] data)
        throws IOException, ClassNotFoundException, IllegalAttributeException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Region mbr = (Region) ois.readObject();
        int numdata = ois.readInt();
        boolean isRoot = ois.readBoolean();
        Node r = null;

        if (isRoot) {
            GridCacheRootNode ret = new GridCacheRootNode(grid, mbr);
            ret.setCapacity(ois.readInt());
            ret.setTileSize(ois.readDouble());
            ret.setTilesNumber((int[]) ois.readObject());
            ret.split();

            for (int i = 0; i < numdata; i++) {
                int id = ois.readInt();
                Region datambr = (Region) ois.readObject();
                Feature f = (Feature) marshaller.unmarshall(ois);
                GridData gd = new GridData(id, datambr, f);
                ret.insertData(gd);
            }

            r = ret;
        } else {
            GridCacheNode ret = new GridCacheNode(grid, mbr);

            for (int i = 0; i < numdata; i++) {
                int id = ois.readInt();
                Region datambr = (Region) ois.readObject();
                Feature f = (Feature) marshaller.unmarshall(ois);
                GridData gd = new GridData(id, datambr, f);
                ret.insertData(gd);
            }

            r = ret;
        }

        ois.close();
        bais.close();

        return r;
    }
}
