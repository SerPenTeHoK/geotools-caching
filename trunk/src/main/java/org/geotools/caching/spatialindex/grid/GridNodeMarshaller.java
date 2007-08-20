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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.opengis.feature.simple.SimpleFeature;
import org.geotools.caching.spatialindex.Node;
import org.geotools.caching.spatialindex.store.AbstractNodeMarshaller;
import org.geotools.caching.util.SimpleFeatureMarshaller;
import org.geotools.feature.IllegalAttributeException;


public class GridNodeMarshaller extends AbstractNodeMarshaller {
    SimpleFeatureMarshaller marshaller = new SimpleFeatureMarshaller();

    @Override
    protected Node read(ObjectInputStream ois)
        throws IOException, ClassNotFoundException, IllegalAttributeException {
        boolean isRoot = ois.readBoolean();

        if (isRoot) {
            GridRootNode node = new GridRootNode();
        } else {
        }

        return null;
    }

    @Override
    protected void write(ObjectOutputStream oos, Node node)
        throws IOException {
        if (node instanceof GridRootNode) {
            write(oos, (GridRootNode) node);
        } else {
            write(oos, (GridNode) node);
        }
    }

    void write(ObjectOutputStream oos, GridNode node) throws IOException {
        oos.writeBoolean(false);
        oos.writeObject(node.getShape());
        oos.writeInt(node.num_data);

        for (int i = 0; i < node.num_data; i++) {
            GridData gd = node.data[i];
            oos.writeInt(gd.id);
            oos.writeObject(gd.shape);

            Object data = gd.data;

            if (data instanceof SimpleFeature) {
                marshaller.marshall((SimpleFeature) data, oos);
            } else {
                oos.writeObject(data);
            }
        }
    }

    void write(ObjectOutputStream oos, GridRootNode node)
        throws IOException {
        oos.writeBoolean(true);
        oos.writeObject(node.getShape());
        oos.writeInt(node.num_data);
        oos.writeInt(node.capacity);
        oos.writeDouble(node.tiles_size);
        oos.writeObject(node.tiles_number);

        for (int i = 0; i < node.num_data; i++) {
            GridData gd = node.data[i];
            oos.writeInt(gd.id);
            oos.writeObject(gd.shape);

            Object data = gd.data;

            if (data instanceof SimpleFeature) {
                marshaller.marshall((SimpleFeature) data, oos);
            } else {
                oos.writeObject(data);
            }
        }
    }
}
