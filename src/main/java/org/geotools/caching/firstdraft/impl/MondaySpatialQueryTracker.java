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
package org.geotools.caching.firstdraft.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.geotools.caching.firstdraft.CacheEntry;
import org.geotools.caching.firstdraft.QueryTracker;
import org.geotools.data.DefaultQuery;
import org.geotools.data.Query;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.spatial.BBOXImpl;
import org.geotools.index.Data;
import org.geotools.index.DataDefinition;
import org.geotools.index.LockTimeoutException;
import org.geotools.index.TreeException;
import org.geotools.index.quadtree.QuadTree;
import org.geotools.index.quadtree.StoreException;
import org.geotools.index.rtree.*;
import org.geotools.index.rtree.memory.MemoryPageStore;


/** First implementation of QueryTracker to handler BBox queries.
 * Stores the extent of queries in a R-tree,
 * so this tracker can tell what areas are already covered by previous queries.
 * Can compute a rough approximation of the complementary area needed to cover a new query.
 * Uses spatial index from org.geotools.index.rtree, and keeps tree in memory.
 *
 * Currently, does handle only queries made of a BBoxFilter.
 *
 * @task handle queries made a up of a _spatial filter_ and an _attribute filter_
 * Use FilterVisitor ?
 * @task should tree have a size limit ? for the time being, we rely a the cache eviction strategy,
 * and hope for the best. We should easily be able to store thousands query envelopes.
 *
 * @author Christophe Rousson, SoC 2007, CRG-ULAVAL
 *
 */
public class MondaySpatialQueryTracker implements QueryTracker {
    /**
     * The R-tree to keep track of queries bounds.
     */
    private QuadTree tree;
    private Envelope universe;

    /**
     *  A map to store queries bounds.
     *  As these are stored in the R-tree, why do we have to store these in another place ?
     *  Well, when we search the tree, we get data, not the envelope of data.
     *  Other R-tree implementation might do a better job.
     *
     */
    private final HashMap map = new HashMap();

    /**
     * We will use this instance of FilterFactory to build new queries.
     */
    private final FilterFactory filterFactory = new FilterFactoryImpl();

    /* (non-Javadoc)
     * @see org.geotools.caching.QueryTracker#clear()
     */
    public void clear() {
    }

    public Query match(Query q) {
        return new DefaultQuery(q.getTypeName(), match(q.getFilter()));
    }

    /* (non-Javadoc)
     * @see org.geotools.caching.QueryTracker#match(org.geotools.data.Query)
     */
    public Filter match(Filter f) {
        if (!accepts(f)) {
            return f;
        }

        BBOXImpl bb = (BBOXImpl) f;

        try {
            Envelope env = new Envelope(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY());
            Geometry searchArea = getRectangle(env);

            // find matches in R-tree
            Collection results = tree.search(env);

            // seems we know nothing about the requested area ... we have to process the whole query.
            if (results.size() == 0) {
                return f;
            }

            // at least part of the requeted area falls within the "known world"
            for (Iterator i = results.iterator(); i.hasNext();) {
                Data d = (Data) i.next();
                Envelope e = (Envelope) map.get(d.getValue(0));
                Polygon rect = getRectangle(e);

                // searchArea within the "known world".
                // We actually don't need any other data.
                if (rect.contains(searchArea)) {
                    return Filter.EXCLUDE;
                }

                // remove known area from search area ...
                searchArea = searchArea.difference(rect);
            }

            // searchArea may be some really complex geometry, with holes and patches.
            // get back to the envelope, to build a new query.
            Envelope se = searchArea.getEnvelopeInternal();
            Filter newbb = filterFactory.bbox(bb.getPropertyName(), se.getMinX(), se.getMinY(),
                    se.getMaxX(), se.getMaxY(), bb.getSRS());

            return newbb;
        } catch (StoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return f;
    }

    public void register(Query q) {
        register(q.getFilter());
    }

    /* (non-Javadoc)
     * @see org.geotools.caching.QueryTracker#register(org.geotools.data.Query)
     */
    public void register(Filter f) {
        if (accepts(f)) {
            BBOXImpl bb = (BBOXImpl) f;

            try {
                Envelope env = new Envelope(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY());
                Integer key = new Integer(env.hashCode());
            } finally {
            }
        }
    }

    public void unregister(Query q) {
        unregister(q.getFilter());
    }

    /* (non-Javadoc)
     * @see org.geotools.caching.QueryTracker#unregister(org.geotools.data.Query)
     */
    public void unregister(Filter f) {
        if (accepts(f)) {
            BBOXImpl bb = (BBOXImpl) f;
            Envelope env = new Envelope(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY());
            unregister(env);
        }
    }

    public void unregister(Envelope env) {
        try {
            Collection results = tree.search(env);

            for (Iterator i = results.iterator(); i.hasNext();) {
                Data d = (Data) i.next();
                Envelope e = (Envelope) map.get(d.getValue(0));
                map.remove(d.getValue(0));
            }
        } catch (StoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @param q
     * @return
     */
    private boolean accepts(Filter f) {
        if (f instanceof BBOXImpl) {
            return true;
        } else {
            return false;
        }
    }

    /** Envelope -> Polygon convenience function.
     *
     * @param e an envelope
     * @return a Rectangle that has the same shape as e
     */
    private static Polygon getRectangle(Envelope e) {
        Coordinate[] coords = new Coordinate[] {
                new Coordinate(e.getMinX(), e.getMinY()), new Coordinate(e.getMaxX(), e.getMinY()),
                new Coordinate(e.getMaxX(), e.getMaxY()), new Coordinate(e.getMinX(), e.getMaxY()),
                new Coordinate(e.getMinX(), e.getMinY())
            };
        CoordinateArraySequence seq = new CoordinateArraySequence(coords);
        LinearRing ls = new LinearRing(seq, new GeometryFactory());
        Polygon ret = new Polygon(ls, null, new GeometryFactory());

        return ret;
    }

    class Tile implements CacheEntry {
        private long creationTime = System.currentTimeMillis();
        private long lastAccessTime = System.currentTimeMillis();
        private int hits = 0;
        private boolean isValid = true;

        public long getCost() {
            // TODO Auto-generated method stub
            return -1;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public long getExpirationTime() {
            // TODO Auto-generated method stub
            return -1;
        }

        public int getHits() {
            return hits;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public long getLastUpdateTime() {
            // TODO Auto-generated method stub
            return -1;
        }

        public long getVersion() {
            // TODO Auto-generated method stub
            return -1;
        }

        public boolean isValid() {
            // TODO Auto-generated method stub
            return isValid;
        }

        public Object getKey() {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getValue() {
            // TODO Auto-generated method stub
            return null;
        }

        public Object setValue(Object arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public void hit() {
            hits++;
            lastAccessTime = System.currentTimeMillis();
        }
    }
}
