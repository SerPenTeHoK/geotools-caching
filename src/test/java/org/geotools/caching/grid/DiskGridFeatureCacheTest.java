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
import java.io.File;
import java.io.IOException;
import org.geotools.caching.AbstractFeatureCache;
import org.geotools.caching.FeatureCacheException;
import org.geotools.caching.spatialindex.store.DiskStorage;


public class DiskGridFeatureCacheTest extends GridFeatureCacheTest {
    public static Test suite() {
        return new TestSuite(DiskGridFeatureCacheTest.class);
    }

    @Override
    protected AbstractFeatureCache createInstance(int capacity)
        throws FeatureCacheException, IOException {
        DiskStorage storage = new DiskStorage(File.createTempFile("cache", ".tmp"), 1000);
        this.cache = new GridFeatureCache(ds.getFeatureSource(dataset.getSchema().getTypeName()),
                100, capacity, storage);
        storage.setParent(this.cache.tracker);

        return this.cache;
    }
}
