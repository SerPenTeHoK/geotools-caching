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
package org.geotools.caching.spatialindex;


/** Identify nodes by the region they represent.
 *
 * @author crousson
 *
 */
public final class RegionNodeIdentifier extends NodeIdentifier {
    /**
     *
     */
    private static final long serialVersionUID = 6630434291791608926L;
    private Region shape;

    /** Used for serialization only.
     * So kept package private.
     *
     */
    RegionNodeIdentifier() {
    }

    /** Identify a new node.
     *
     * @param node
     */
    public RegionNodeIdentifier(Node n) {
        if (n.getShape() instanceof Region) {
            this.shape = new Region((Region) n.getShape());
        } else {
            throw new IllegalArgumentException(
                "DefaultNodeIdentifier can only identify nodes representing a Region.");
        }
    }

    public Shape getShape() {
        return new Region(shape);
    }

    public int hashCode() {
        return shape.hashCode();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof RegionNodeIdentifier) {
            RegionNodeIdentifier ni = (RegionNodeIdentifier) o;

            return shape.equals(ni.getShape());
        } else {
            return false;
        }
    }
}
