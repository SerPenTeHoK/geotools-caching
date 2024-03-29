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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import com.vividsolutions.jts.geom.Envelope;
import org.opengis.filter.Filter;
import org.geotools.caching.AbstractFeatureCache;
import org.geotools.caching.CacheOversizedException;
import org.geotools.caching.FeatureCacheException;
import org.geotools.caching.FeatureCollectingVisitor;
import org.geotools.caching.spatialindex.Region;
import org.geotools.caching.spatialindex.Storage;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.spatial.BBOXImpl;


public class GridFeatureCache extends AbstractFeatureCache {
    GridTracker tracker;
    int max_tiles = 10;
    int capacity;

    //int evictions = 0 ;
    //int puts = 0 ;

    /**
     * @param FeatureStore from which to cache features
     * @param indexcapacity = number of tiles in index
     * @param capacity = max number of features to cache
     * @throws FeatureCacheException
     */
    public GridFeatureCache(FeatureSource fs, int indexcapacity, int capacity, Storage store)
        throws FeatureCacheException {
        super(fs);

        Envelope universe;

        try {
            universe = fs.getBounds();
        } catch (IOException e) {
            throw new FeatureCacheException(e);
        }

        this.tracker = new GridTracker(convert(universe), indexcapacity, store);
        this.capacity = capacity;

        //this.tracker.addWriteNodeCommand(new EvictOnWriteCommand()) ;
    }

    protected Filter match(BBOXImpl sr) {
        Region search = convert(extractEnvelope(sr));
        Stack missing = tracker.searchMissingTiles(search);
        FilterFactoryImpl ff = new FilterFactoryImpl();

        // TODO: group regions
        if (missing.size() > max_tiles) {
            return sr;
        } else if (missing.size() > 1) {
            ArrayList<Filter> filters = new ArrayList<Filter>(missing.size());

            while (!missing.isEmpty()) {
                Region rg = (Region) missing.pop();
                Filter bbf = ff.bbox(sr.getPropertyName(), rg.getLow(0), rg.getLow(1),
                        rg.getHigh(0), rg.getHigh(1), sr.getSRS());
                filters.add(bbf);
            }

            return ff.or(filters);
        } else if (missing.size() == 1) {
            Region rg = (Region) missing.pop();

            return ff.bbox(sr.getPropertyName(), rg.getLow(0), rg.getLow(1), rg.getHigh(0),
                rg.getHigh(1), sr.getSRS());
        } else {
            return Filter.EXCLUDE;
        }
    }

    /**
     * @param e
     * @return list of envelopes not in cache
     */
    protected List match(Envelope e) {
        Region search = convert(e);
        ArrayList<Envelope> missing = new ArrayList<Envelope>();

        if (!this.tracker.getRoot().getShape().contains(search)) { // query is outside of root mbr
                                                                   // we limit our search to the inside of the root mbr

            Envelope r = convert((Region) this.tracker.getRoot().getShape());
            r = r.intersection(e);
            search = convert(r);
        }

        List missing_tiles = tracker.searchMissingTiles(search);

        // TODO: groug regions
        if (missing_tiles.size() > max_tiles) {
            missing.add(e);
        } else {
            for (Iterator it = missing_tiles.iterator(); it.hasNext();) {
                Region next = (Region) it.next();
                missing.add(convert(next));
            }
        }

        return missing;
    }

    public void clear() {
        tracker.flush();
    }

    public FeatureCollection peek(Envelope e) {
        FeatureCollectingVisitor v = new FeatureCollectingVisitor(this.getSchema());
        this.tracker.intersectionQuery(convert(e), v);

        return v.getCollection();
    }

    public void put(FeatureCollection fc, Envelope e) throws CacheOversizedException {
        put(fc);
        register(e);
    }

    public void remove(Envelope e) {
        InvalidatingVisitor v = new InvalidatingVisitor();
        this.tracker.intersectionQuery(convert(e), v);
    }

    public Envelope getBounds() throws IOException {
        return convert((Region) this.tracker.getRoot().getShape());
    }

    public Envelope getBounds(Query query) throws IOException {
        return this.fs.getBounds(query);
    }

    public int getCount(Query query) throws IOException {
        return this.fs.getCount(query);
    }

    public void put(FeatureCollection fc) throws CacheOversizedException {
        int size = fc.size();

        if (size > this.capacity) {
            throw new CacheOversizedException("Cannot cache collection of size " + fc.size()
                + " (capacity = " + capacity + " )");
        }

        //puts++ ;
        while (tracker.getStatistics().getNumberOfData() > (capacity - size)) { // was capacity - fc.size()
            tracker.policy.evict();

            //evictions++ ;
            //System.out.println("Put #" + puts + " > number of evictions = " + evictions) ;
        }

        FeatureIterator it = fc.features();

        while (it.hasNext()) {
            Feature f = it.next();
            this.tracker.insertData(f, convert(f.getBounds()), f.hashCode());
        }

        fc.close(it);
    }

    protected void register(BBOXImpl f) {
        register(extractEnvelope(f));
    }

    protected void register(Envelope e) {
        Region r = convert(e);
        ValidatingVisitor v = new ValidatingVisitor(r);
        this.tracker.containmentQuery(r, v);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("GridFeatureCache [");
        sb.append(" Source = " + this.fs);
        sb.append(" Capacity = " + this.capacity);
        sb.append(" Nodes = " + this.tracker.stats.getNumberOfNodes());
        sb.append(" ]");

        return sb.toString();
    }

    public Set getSupportedHints() {
        return new HashSet();
    }
}
