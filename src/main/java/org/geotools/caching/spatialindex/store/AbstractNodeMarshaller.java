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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.geotools.caching.spatialindex.Node;
import org.geotools.feature.IllegalAttributeException;


public abstract class AbstractNodeMarshaller implements NodeMarshaller {
    public byte[] marshall(Node node) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        write(oos, node);
        oos.close();
        baos.close();

        return baos.toByteArray();
    }

    protected abstract void write(ObjectOutputStream oos, Node node)
        throws IOException;

    public Node unmarshall(byte[] data)
        throws IOException, ClassNotFoundException, IllegalAttributeException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Node r = read(ois);
        ois.close();
        bais.close();

        return r;
    }

    protected abstract Node read(ObjectInputStream ois)
        throws IOException, ClassNotFoundException, IllegalAttributeException;
}
