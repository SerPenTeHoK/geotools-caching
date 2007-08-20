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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opengis.feature.simple.SimpleFeature;
import org.geotools.caching.spatialindex.Data;
import org.geotools.caching.spatialindex.Shape;
import org.geotools.caching.util.SimpleFeatureMarshaller;
import org.geotools.feature.IllegalAttributeException;


/** Associates data with its shape and id, as to be stored in the index.
 *
 * @author Christophe Rousson, SoC 2007, CRG-ULAVAL
 *
 */
public class GridData implements Data, Externalizable {
    /**
     *
     */
    private static final long serialVersionUID = 2435341100521921266L;
    static SimpleFeatureMarshaller marshaller = new SimpleFeatureMarshaller();
    int id;
    Shape shape;
    Object data;

    public GridData() {
    }

    public GridData(int id, Shape shape, Object data) {
        this.id = id;
        this.shape = shape;
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public int getIdentifier() {
        return id;
    }

    public Shape getShape() {
        return shape;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readInt();
        this.shape = (Shape) in.readObject();

        if (in.readBoolean()) {
            try {
                this.data = marshaller.unmarshall(in);
            } catch (IllegalAttributeException e) {
                throw (IOException) new IOException().initCause(e);
            }
        } else {
            this.data = in.readObject();
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(id);
        out.writeObject(shape);

        if (data instanceof SimpleFeature) {
            out.writeBoolean(true);
            marshaller.marshall((SimpleFeature) data, out);
        } else {
            out.writeBoolean(false);
            out.writeObject(data);
        }
    }
}
