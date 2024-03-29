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

import junit.framework.Test;
import junit.framework.TestSuite;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import com.vividsolutions.jts.geom.Coordinate;
import org.opengis.filter.Filter;
import org.geotools.caching.AbstractFeatureCache;
import org.geotools.caching.AbstractFeatureCacheTest;
import org.geotools.caching.CacheOversizedException;
import org.geotools.caching.FeatureCacheException;
import org.geotools.caching.FeatureCollectingVisitor;
import org.geotools.caching.spatialindex.store.MemoryStorage;
import org.geotools.caching.util.Generator;
import org.geotools.data.FeatureStore;


public class GridFeatureCacheTest extends AbstractFeatureCacheTest {
    static boolean testEviction_holistic = false;
    GridFeatureCache cache;

    public static Test suite() {
        return new TestSuite(GridFeatureCacheTest.class);
    }

    @Override
    protected AbstractFeatureCache createInstance(int capacity)
        throws FeatureCacheException, IOException {
        this.cache = new GridFeatureCache((FeatureStore) ds.getFeatureSource(
                    dataset.getSchema().getTypeName()), 100, capacity, new MemoryStorage(100));

        return this.cache;
    }

    @Override
    public void testEviction() throws IOException, FeatureCacheException {
        super.cache = createInstance(numdata / 2);

        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 11; j++) {
                Filter f = Generator.createBboxFilter(new Coordinate(i * 0.1, j * 0.1), 0.1, 0.1);
                Collection c = cache.getFeatures(f);
                Collection control = ds.getFeatureSource(dataset.getSchema().getTypeName())
                                       .getFeatures(f);
                assertEquals(control.size(), c.size());

                if (!testEviction_holistic && (cache.tracker.getEvictions() > 10)) { // wait to generate a fair amount of evictions,
                                                                                     // and see everything is still working

                    return;
                }
            }
        }

        System.out.println(cache.tracker.getStatistics());
        System.out.println(cache.sourceAccessStats());

        if (!testEviction_holistic) {
            fail("Did not got enough evictions : " + cache.tracker.getEvictions());
        }
    }

    @Override
    public void testPut() throws CacheOversizedException {
        cache.put(dataset);

        FeatureCollectingVisitor v = new FeatureCollectingVisitor(dataset.getFeatureType());
        cache.tracker.intersectionQuery(AbstractFeatureCache.convert(unitsquare), v);

        assertEquals(dataset.size(), v.getCollection().size());
    }
}
