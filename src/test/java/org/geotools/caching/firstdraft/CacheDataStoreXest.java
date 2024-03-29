/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2003-2006, Geotools Project Managment Committee (PMC)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.caching.firstdraft;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import org.opengis.filter.Filter;
import org.geotools.caching.firstdraft.impl.InMemoryDataCache;
import org.geotools.data.DataStore;
import org.geotools.data.DataTestCase;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultQuery;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.DiffFeatureReader;
import org.geotools.data.EmptyFeatureReader;
import org.geotools.data.EmptyFeatureWriter;
import org.geotools.data.FeatureEvent;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureListenerManager;
import org.geotools.data.FeatureLock;
import org.geotools.data.FeatureLockFactory;
import org.geotools.data.FeatureLocking;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FilteringFeatureReader;
import org.geotools.data.FilteringFeatureWriter;
import org.geotools.data.InProcessLockingManager;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.TransactionStateDiff;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.feature.AttributeType;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SimpleFeature;
import org.geotools.filter.FidFilter;
import org.geotools.filter.FidFilterImpl;
import org.geotools.filter.FilterFactory;
import org.geotools.filter.FilterFactoryFinder;


/**
 * DOCUMENT ME!
 *
 * @author Jody Garnett, Refractions Research
 * @source $URL:
 *         http://svn.geotools.org/geotools/trunk/gt/modules/library/main/src/test/java/org/geotools/data/memory/MemoryDataStoreTest.java $
 */
public class CacheDataStoreXest extends DataTestCase {
    InMemoryDataCache data;

    /**
     * Constructor for MemoryDataStoreTest.
     *
     * @param arg0
     */
    public CacheDataStoreXest(String arg0) {
        super(arg0);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        MemoryDataStore ds = new MemoryDataStore();
        ds.addFeatures(roadFeatures);
        ds.addFeatures(riverFeatures);
        data = new InMemoryDataCache(ds);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        data = null;
        super.tearDown();
    }

    public void testFixture() throws Exception {
        FeatureType type = DataUtilities.createType("namespace.typename",
                "name:String,id:0,geom:MultiLineString");
        assertEquals("namespace", new URI("namespace"), type.getNamespace());
        assertEquals("typename", "typename", type.getTypeName());
        assertEquals("attributes", 3, type.getAttributeCount());

        AttributeType[] a = type.getAttributeTypes();
        assertEquals("a1", "name", a[0].getName());
        assertEquals("a1", String.class, a[0].getType());

        assertEquals("a2", "id", a[1].getName());
        assertEquals("a2", Integer.class, a[1].getType());

        assertEquals("a3", "geom", a[2].getName());
        assertEquals("a3", MultiLineString.class, a[2].getType());
    }

    public void testMemoryDataStore() throws Exception {
        DataStore store = new MemoryDataStore();
        InMemoryDataCache cache = new InMemoryDataCache(store);
    }

    /*
     * Test for void MemoryDataStore(FeatureCollection)
     */
    public void testMemoryDataStoreFeatureCollection() {
        DataStore store = new MemoryDataStore(DataUtilities.collection(roadFeatures));
    }

    /*
     * Test for void MemoryDataStore(FeatureReader)
     */
    public void testMemoryDataStoreFeatureArray() throws IOException {
        DataStore store = new MemoryDataStore(roadFeatures);
    }

    /*
     * Test for void MemoryDataStore(FeatureReader)
     */
    public void testMemoryDataStoreFeatureReader() throws IOException {
        FeatureReader reader = DataUtilities.reader(roadFeatures);
        DataStore store = new MemoryDataStore(reader);
    }

    public void testGetFeatureTypes() throws IOException {
        String[] names = data.getTypeNames();
        assertEquals(2, names.length);
        assertTrue(contains(names, "road"));
        assertTrue(contains(names, "river"));
    }

    boolean contains(Object[] array, Object expected) {
        if ((array == null) || (array.length == 0)) {
            return false;
        }

        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(expected)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Like contain but based on match rather than equals
     *
     * @param array DOCUMENT ME!
     * @param expected DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    boolean containsLax(Feature[] array, Feature expected) {
        if ((array == null) || (array.length == 0)) {
            return false;
        }

        FeatureType type = expected.getFeatureType();

        for (int i = 0; i < array.length; i++) {
            if (match(array[i], expected)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Compare based on attributes not getID allows comparison of Diff contents
     *
     * @param expected DOCUMENT ME!
     * @param actual DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    boolean match(Feature expected, Feature actual) {
        FeatureType type = expected.getFeatureType();

        for (int i = 0; i < type.getAttributeCount(); i++) {
            Object av = actual.getAttribute(i);
            Object ev = expected.getAttribute(i);

            if ((av == null) && (ev != null)) {
                return false;
            } else if ((ev == null) && (av != null)) {
                return false;
            } else if (av instanceof Geometry && ev instanceof Geometry) {
                Geometry ag = (Geometry) av;
                Geometry eg = (Geometry) ev;

                if (!ag.equals(eg)) {
                    return false;
                }
            } else if (!av.equals(ev)) {
                return false;
            }
        }

        return true;
    }

    public void testGetSchema() throws IOException {
        assertSame(roadType, data.getSchema("road"));
        assertSame(riverType, data.getSchema("river"));
    }

    void assertCovers(String msg, FeatureCollection c1, FeatureCollection c2) {
        if (c1 == c2) {
            return;
        }

        assertNotNull(msg, c1);
        assertNotNull(msg, c2);
        assertEquals(msg + " size", c1.size(), c2.size());

        Feature f;

        for (FeatureIterator i = c1.features(); i.hasNext();) {
            f = i.next();
            assertTrue(msg + " " + f.getID(), c2.contains(f));
        }
    }

    /* public void testGetFeatureReader() throws IOException, IllegalAttributeException {
       FeatureReader reader = data.getFeatureReader("road");
       assertCovered(roadFeatures, reader);
       assertEquals(false, reader.hasNext());
       } */

    /* public void testGetFeatureReaderMutability() throws IOException, IllegalAttributeException {
       FeatureReader reader = data.getFeatureReader("road");
       Feature feature;
       while( reader.hasNext() ) {
           feature = (Feature) reader.next();
           feature.setAttribute("name", null);
       }
       reader.close();
       reader = data.getFeatureReader("road");
       while( reader.hasNext() ) {
           feature = (Feature) reader.next();
           assertNotNull(feature.getAttribute("name"));
       }
       reader.close();
       try {
           reader.next();
           fail("next should fail with an IOException");
       } catch (IOException expected) {
       }
       } */

    /*public void testGetFeatureReaderConcurancy() throws NoSuchElementException, IOException,
       IllegalAttributeException {
       FeatureReader reader1 = data.get;
       FeatureReader reader2 = data.getFeatureReader("road");
       FeatureReader reader3 = data.getFeatureReader("river");
       Feature feature1;
       Feature feature2;
       Feature feature3;
       while( reader1.hasNext() || reader2.hasNext() || reader3.hasNext() ) {
           assertTrue(contains(roadFeatures, reader1.next()));
           assertTrue(contains(roadFeatures, reader2.next()));
           if (reader3.hasNext()) {
               assertTrue(contains(riverFeatures, reader3.next()));
           }
       }
       try {
           reader1.next();
           fail("next should fail with an IOException");
       } catch (IOException expected) {
       }
       try {
           reader2.next();
           fail("next should fail with an IOException");
       } catch (IOException expected) {
       }
       try {
           reader3.next();
           fail("next should fail with an IOException");
       } catch (IOException expected) {
       }
       reader1.close();
       reader2.close();
       reader3.close();
       } */
    public void testGetFeatureReaderFilterAutoCommit()
        throws NoSuchElementException, IOException, IllegalAttributeException {
        FeatureType type = data.getSchema("road");
        FeatureReader reader;

        reader = data.getFeatureReader(new DefaultQuery("road"), Transaction.AUTO_COMMIT);
        assertFalse(reader instanceof FilteringFeatureReader);
        assertEquals(type, reader.getFeatureType());
        assertEquals(roadFeatures.length, count(reader));

        reader = data.getFeatureReader(new DefaultQuery("road", Filter.EXCLUDE),
                Transaction.AUTO_COMMIT);
        assertTrue(reader instanceof EmptyFeatureReader);

        assertEquals(type, reader.getFeatureType());
        assertEquals(0, count(reader));

        reader = data.getFeatureReader(new DefaultQuery("road", rd1Filter), Transaction.AUTO_COMMIT);
        assertTrue(reader instanceof FilteringFeatureReader);
        assertEquals(type, reader.getFeatureType());
        assertEquals(1, count(reader));
    }

    public void testGetFeatureReaderFilterTransaction()
        throws NoSuchElementException, IOException, IllegalAttributeException {
        Transaction t = new DefaultTransaction();
        FeatureType type = data.getSchema("road");
        FeatureReader reader;

        reader = data.getFeatureReader(new DefaultQuery("road", Filter.EXCLUDE), t);
        assertTrue(reader instanceof EmptyFeatureReader);
        assertEquals(type, reader.getFeatureType());
        assertEquals(0, count(reader));

        reader = data.getFeatureReader(new DefaultQuery("road"), t);
        assertTrue(reader instanceof DiffFeatureReader);
        assertEquals(type, reader.getFeatureType());
        assertEquals(roadFeatures.length, count(reader));

        reader = data.getFeatureReader(new DefaultQuery("road", rd1Filter), t);
        // assertTrue(reader instanceof DiffFeatureReader);//Currently wrapped by a filtering
        // feature reader
        assertEquals(type, reader.getFeatureType());
        assertEquals(1, count(reader));

        TransactionStateDiff state = (TransactionStateDiff) t.getState(data);
        FeatureWriter writer = state.writer("road", Filter.INCLUDE);
        Feature feature;

        while (writer.hasNext()) {
            feature = writer.next();

            if (feature.getID().equals("road.rd1")) {
                writer.remove();
            }
        }

        reader = data.getFeatureReader(new DefaultQuery("road", Filter.EXCLUDE), t);
        assertEquals(0, count(reader));

        reader = data.getFeatureReader(new DefaultQuery("road"), t);
        assertEquals(roadFeatures.length - 1, count(reader));

        reader = data.getFeatureReader(new DefaultQuery("road", rd1Filter), t);
        assertEquals(0, count(reader));

        t.rollback();
        reader = data.getFeatureReader(new DefaultQuery("road", Filter.EXCLUDE), t);
        assertEquals(0, count(reader));

        reader = data.getFeatureReader(new DefaultQuery("road"), t);
        assertEquals(roadFeatures.length, count(reader));

        reader = data.getFeatureReader(new DefaultQuery("road", rd1Filter), t);
        assertEquals(1, count(reader));
    }

    /**
     * When a data store is loaded with a reader, it would be nice if the memory
     * data store preserved feature order, so that features are always rendered
     * the same way (rendering is different if order changes and features do overlap)
     */
    public void testOrderPreservationRoad() throws Exception {
        assertOrderSame(roadFeatures);
    }

    public void testOrderPreservationRiver() throws Exception {
        assertOrderSame(riverFeatures);
    }

    public void testOrderPreservationMemFetures() throws Exception {
        SimpleFeature[] dynFeatures = new SimpleFeature[3];
        dynFeatures[0] = (SimpleFeature) roadType.create(new Object[] {
                    new Integer(1), line(new int[] { 1, 1, 2, 2, 4, 2, 5, 1 }), "r1",
                });
        dynFeatures[1] = (SimpleFeature) roadType.create(new Object[] {
                    new Integer(2), line(new int[] { 3, 0, 3, 2, 3, 3, 3, 4 }), "r2"
                });
        dynFeatures[2] = (SimpleFeature) roadType.create(new Object[] {
                    new Integer(3), line(new int[] { 3, 2, 4, 2, 5, 3 }), "r3"
                });
        assertOrderSame(dynFeatures);
    }

    void assertOrderSame(Feature[] features) throws Exception {
        // init using readers
        FeatureReader reader = DataUtilities.reader(features);
        DataStore store1 = new MemoryDataStore(reader);
        assertReaderOrderSame(features, store1);

        // init using array directly
        DataStore store2 = new MemoryDataStore(features);
        assertReaderOrderSame(features, store2);
    }

    private void assertReaderOrderSame(Feature[] features, DataStore store)
        throws IOException, IllegalAttributeException {
        FeatureReader r1 = store.getFeatureReader(new DefaultQuery(
                    features[0].getFeatureType().getTypeName()), Transaction.AUTO_COMMIT);
        FeatureReader r2 = DataUtilities.reader(features);

        while (r1.hasNext() && r2.hasNext()) {
            Feature f1 = r1.next();
            Feature f2 = r2.next();
            assertEquals(f1, f2);
        }

        assertEquals(r1.hasNext(), r2.hasNext());
        r1.close();
        r2.close();
    }

    void assertCovered(Feature[] features, FeatureReader reader)
        throws NoSuchElementException, IOException, IllegalAttributeException {
        int count = 0;

        try {
            while (reader.hasNext()) {
                assertTrue(contains(features, reader.next()));
                count++;
            }
        } finally {
            reader.close();
        }

        assertEquals(features.length, count);
    }

    /**
     * Ensure that FeatureReader reader contains extactly the contents of array.
     *
     * @param reader DOCUMENT ME!
     * @param array DOCUMENT ME!
     * @return DOCUMENT ME!
     * @throws NoSuchElementException DOCUMENT ME!
     * @throws IOException DOCUMENT ME!
     * @throws IllegalAttributeException DOCUMENT ME!
     */
    boolean covers(FeatureReader reader, Feature[] array)
        throws NoSuchElementException, IOException, IllegalAttributeException {
        Feature feature;
        int count = 0;

        try {
            while (reader.hasNext()) {
                feature = reader.next();

                if (!contains(array, feature)) {
                    return false;
                }

                count++;
            }
        } finally {
            reader.close();
        }

        return count == array.length;
    }

    boolean covers(FeatureIterator reader, Feature[] array)
        throws NoSuchElementException, IOException, IllegalAttributeException {
        Feature feature;
        int count = 0;

        try {
            while (reader.hasNext()) {
                feature = reader.next();

                if (!contains(array, feature)) {
                    return false;
                }

                count++;
            }
        } finally {
            reader.close();
        }

        return count == array.length;
    }

    boolean coversLax(FeatureReader reader, Feature[] array)
        throws NoSuchElementException, IOException, IllegalAttributeException {
        Feature feature;
        int count = 0;

        try {
            while (reader.hasNext()) {
                feature = reader.next();

                if (!containsLax(array, feature)) {
                    return false;
                }

                count++;
            }
        } finally {
            reader.close();
        }

        return count == array.length;
    }

    boolean coversLax(FeatureIterator reader, Feature[] array)
        throws NoSuchElementException, IOException, IllegalAttributeException {
        Feature feature;
        int count = 0;

        try {
            while (reader.hasNext()) {
                feature = reader.next();

                if (!containsLax(array, feature)) {
                    return false;
                }

                count++;
            }
        } finally {
            reader.close();
        }

        return count == array.length;
    }

    void dump(FeatureReader reader)
        throws NoSuchElementException, IOException, IllegalAttributeException {
        Feature feature;
        int count = 0;

        try {
            while (reader.hasNext()) {
                feature = reader.next();
                System.out.println(count + " feature:" + feature);
                count++;
            }
        } finally {
            reader.close();
        }
    }

    void dump(Object[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.println(i + " feature:" + array[i]);
        }
    }

    /*
     * Test for FeatureWriter getFeatureWriter(String, Filter, Transaction)
     */
    public void testGetFeatureWriter()
        throws NoSuchElementException, IOException, IllegalAttributeException {
        FeatureWriter writer = data.getFeatureWriter("road", Transaction.AUTO_COMMIT);
        assertEquals(roadFeatures.length, count(writer));

        try {
            writer.hasNext();
            fail("Should not be able to use a closed writer");
        } catch (IOException expected) {
        }

        try {
            writer.next();
            fail("Should not be able to use a closed writer");
        } catch (IOException expected) {
        }
    }

    public void testGetFeatureWriterRemove() throws IOException, IllegalAttributeException {
        FeatureWriter writer = data.getFeatureWriter("road", Transaction.AUTO_COMMIT);
        Feature feature;

        while (writer.hasNext()) {
            feature = writer.next();

            if (feature.getID().equals("road.rd1")) {
                writer.remove();
            }
        }

        assertEquals(roadFeatures.length - 1, data.getFeatureSource("road").getFeatures().size());
    }

    public void testGetFeaturesWriterAdd() throws IOException, IllegalAttributeException {
        FeatureWriter writer = data.getFeatureWriter("road", Transaction.AUTO_COMMIT);
        SimpleFeature feature;

        while (writer.hasNext()) {
            feature = (SimpleFeature) writer.next();
        }

        assertFalse(writer.hasNext());
        feature = (SimpleFeature) writer.next();
        feature.setAttributes(newRoad.getAttributes(null));
        writer.write();
        assertFalse(writer.hasNext());
        assertEquals(roadFeatures.length + 1, data.getFeatureSource("road").getFeatures().size());
    }

    public void testGetFeaturesWriterModify() throws IOException, IllegalAttributeException {
        FeatureWriter writer = data.getFeatureWriter("road", Transaction.AUTO_COMMIT);
        Feature feature;

        while (writer.hasNext()) {
            feature = writer.next();

            if (feature.getID().equals("road.rd1")) {
                feature.setAttribute("name", "changed");
                writer.write();
            }
        }

        Iterator it = data.getFeatureSource("road").getFeatures(ff.createFidFilter("road.rd1"))
                          .iterator();
        assertTrue(it.hasNext());
        feature = (Feature) it.next();
        assertEquals("changed", feature.getAttribute("name"));
    }

    public void testGetFeatureWriterTypeNameTransaction()
        throws NoSuchElementException, IOException, IllegalAttributeException {
        FeatureWriter writer;

        writer = data.getFeatureWriter("road", Transaction.AUTO_COMMIT);
        assertEquals(roadFeatures.length, count(writer));
        writer.close();
    }

    public void testGetFeatureWriterAppendTypeNameTransaction()
        throws Exception {
        FeatureWriter writer;

        writer = data.getFeatureWriterAppend("road", Transaction.AUTO_COMMIT);
        assertEquals(0, count(writer));
        writer.close();
    }

    /*
     * Test for FeatureWriter getFeatureWriter(String, boolean, Transaction)
     */
    public void testGetFeatureWriterFilter()
        throws NoSuchElementException, IOException, IllegalAttributeException {
        FeatureWriter writer;

        writer = data.getFeatureWriter("road", Filter.EXCLUDE, Transaction.AUTO_COMMIT);
        assertTrue(writer instanceof EmptyFeatureWriter);
        assertEquals(0, count(writer));

        writer = data.getFeatureWriter("road", Filter.INCLUDE, Transaction.AUTO_COMMIT);
        assertFalse(writer instanceof FilteringFeatureWriter);
        assertEquals(roadFeatures.length, count(writer));

        writer = data.getFeatureWriter("road", rd1Filter, Transaction.AUTO_COMMIT);
        assertTrue(writer instanceof FilteringFeatureWriter);
        assertEquals(1, count(writer));
    }

    /**
     * Test two transactions one removing feature, and one adding a feature.
     *
     * @throws Exception DOCUMENT ME!
     */
    public void testGetFeatureWriterTransaction() throws Exception {
        Transaction t1 = new DefaultTransaction();
        Transaction t2 = new DefaultTransaction();
        FeatureWriter writer1 = data.getFeatureWriter("road", rd1Filter, t1);
        FeatureWriter writer2 = data.getFeatureWriterAppend("road", t2);

        FeatureType road = data.getSchema("road");
        FeatureReader reader;
        SimpleFeature feature;
        SimpleFeature[] ORIGIONAL = roadFeatures;
        Feature[] REMOVE = new Feature[ORIGIONAL.length - 1];
        Feature[] ADD = new Feature[ORIGIONAL.length + 1];
        Feature[] FINAL = new Feature[ORIGIONAL.length];
        int i;
        int index;
        index = 0;

        for (i = 0; i < ORIGIONAL.length; i++) {
            feature = ORIGIONAL[i];

            if (!feature.getID().equals("road.rd1")) {
                REMOVE[index++] = feature;
            }
        }

        for (i = 0; i < ORIGIONAL.length; i++) {
            ADD[i] = ORIGIONAL[i];
        }

        ADD[i] = newRoad;

        for (i = 0; i < REMOVE.length; i++) {
            FINAL[i] = REMOVE[i];
        }

        FINAL[i] = newRoad;

        // start of with ORIGINAL
        reader = data.getFeatureReader(new DefaultQuery("road"), Transaction.AUTO_COMMIT);
        assertTrue(covers(reader, ORIGIONAL));

        // writer 1 removes road.rd1 on t1
        // -------------------------------
        // - tests transaction independence from DataStore
        while (writer1.hasNext()) {
            feature = (SimpleFeature) writer1.next();
            assertEquals("road.rd1", feature.getID());
            writer1.remove();
        }

        // still have ORIGIONAL and t1 has REMOVE
        reader = data.getFeatureReader(new DefaultQuery("road"), Transaction.AUTO_COMMIT);
        assertTrue(covers(reader, ORIGIONAL));
        reader = data.getFeatureReader(new DefaultQuery("road"), t1);
        assertTrue(covers(reader, REMOVE));

        // close writer1
        // --------------
        // ensure that modification is left up to transaction commmit
        writer1.close();

        // We still have ORIGIONAL and t1 has REMOVE
        reader = data.getFeatureReader(new DefaultQuery("road"), Transaction.AUTO_COMMIT);
        assertTrue(covers(reader, ORIGIONAL));
        reader = data.getFeatureReader(new DefaultQuery("road"), t1);
        assertTrue(covers(reader, REMOVE));

        // writer 2 adds road.rd4 on t2
        // ----------------------------
        // - tests transaction independence from each other
        feature = (SimpleFeature) writer2.next();
        feature.setAttributes(newRoad.getAttributes(null));
        writer2.write();

        // We still have ORIGIONAL and t2 has ADD
        reader = data.getFeatureReader(new DefaultQuery("road"), Transaction.AUTO_COMMIT);
        assertTrue(covers(reader, ORIGIONAL));
        reader = data.getFeatureReader(new DefaultQuery("road"), t2);
        assertTrue(coversLax(reader, ADD));

        // close writer2
        // -------------
        // ensure that modification is left up to transaction commmit
        writer2.close();

        // Still have ORIGIONAL and t2 has ADD
        reader = data.getFeatureReader(new DefaultQuery("road"), Transaction.AUTO_COMMIT);
        assertTrue(covers(reader, ORIGIONAL));
        reader = data.getFeatureReader(new DefaultQuery("road"), t2);
        assertTrue(coversLax(reader, ADD));

        // commit t1
        // ---------
        // -ensure that delayed writing of transactions takes place
        //
        t1.commit();

        // We now have REMOVE, as does t1 (which has not additional diffs)
        // t2 will have FINAL
        reader = data.getFeatureReader(new DefaultQuery("road"), Transaction.AUTO_COMMIT);
        assertTrue(covers(reader, REMOVE));
        reader = data.getFeatureReader(new DefaultQuery("road"), t1);
        assertTrue(covers(reader, REMOVE));
        reader = data.getFeatureReader(new DefaultQuery("road"), t2);
        assertTrue(coversLax(reader, FINAL));

        // commit t2
        // ---------
        // -ensure that everyone is FINAL at the end of the day
        t2.commit();

        // We now have Number( remove one and add one)
        reader = data.getFeatureReader(new DefaultQuery("road"), Transaction.AUTO_COMMIT);
        reader = data.getFeatureReader(new DefaultQuery("road"), Transaction.AUTO_COMMIT);
        assertTrue(coversLax(reader, FINAL));
        reader = data.getFeatureReader(new DefaultQuery("road"), t1);
        assertTrue(coversLax(reader, FINAL));
        reader = data.getFeatureReader(new DefaultQuery("road"), t2);
        assertTrue(coversLax(reader, FINAL));
    }

    /**
     * Test the transaction when multiple edits occur using a transaction and a fid filter.
     */
    public void testModifyInTransactionFidFilter() throws Exception {
        Transaction t1 = new DefaultTransaction();

        GeometryFactory fac = new GeometryFactory();

        FeatureWriter writer1 = data.getFeatureWriter("road", rd1Filter, t1);
        writer1.next()
               .setPrimaryGeometry(fac.createLineString(
                new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 1) }));
        writer1.write();

        writer1.close();

        FeatureReader reader = data.getFeatureReader(new DefaultQuery("road", rd1Filter), t1);
        Geometry geom1 = reader.next().getPrimaryGeometry();
        reader.close();
        assertEquals(new Coordinate(0, 0), geom1.getCoordinates()[0]);
        assertEquals(new Coordinate(0, 1), geom1.getCoordinates()[1]);

        writer1 = data.getFeatureWriter("road", rd1Filter, t1);
        writer1.next()
               .setPrimaryGeometry(fac.createLineString(
                new Coordinate[] { new Coordinate(10, 0), new Coordinate(10, 1) }));
        writer1.write();
        writer1.close();

        reader = data.getFeatureReader(new DefaultQuery("road", rd1Filter), t1);
        geom1 = reader.next().getPrimaryGeometry();
        reader.close();
        assertEquals(new Coordinate(10, 0), geom1.getCoordinates()[0]);
        assertEquals(new Coordinate(10, 1), geom1.getCoordinates()[1]);

        FeatureWriter writer = data.getFeatureWriterAppend("road", t1);
        Feature feature = writer.next();
        feature.setPrimaryGeometry(fac.createLineString(
                new Coordinate[] { new Coordinate(20, 0), new Coordinate(20, 1) }));
        writer.write();
        writer.close();

        FidFilter filter = FilterFactoryFinder.createFilterFactory().createFidFilter(feature.getID());

        reader = data.getFeatureReader(new DefaultQuery("road", filter), t1);
        geom1 = reader.next().getPrimaryGeometry();
        reader.close();
        assertEquals(new Coordinate(20, 0), geom1.getCoordinates()[0]);
        assertEquals(new Coordinate(20, 1), geom1.getCoordinates()[1]);

        writer1 = data.getFeatureWriter("road", filter, t1);
        writer1.next()
               .setPrimaryGeometry(fac.createLineString(
                new Coordinate[] { new Coordinate(30, 0), new Coordinate(30, 1) }));
        writer1.write();
        writer1.close();

        reader = data.getFeatureReader(new DefaultQuery("road", filter), t1);
        geom1 = reader.next().getPrimaryGeometry();
        reader.close();
        assertEquals(new Coordinate(30, 0), geom1.getCoordinates()[0]);
        assertEquals(new Coordinate(30, 1), geom1.getCoordinates()[1]);
    }

    // Feature Source Testing
    public void testGetFeatureSourceRoad() throws IOException {
        FeatureSource road = data.getFeatureSource("road");

        assertSame(roadType, road.getSchema());
        assertSame(data, road.getDataStore());
        assertEquals(3, road.getCount(Query.ALL));
        assertEquals(new Envelope(1, 5, 0, 4), road.getBounds(Query.ALL));

        FeatureCollection all = road.getFeatures();
        assertEquals(3, all.size());
        assertEquals(roadBounds, all.getBounds());

        FeatureCollection expected = DataUtilities.collection(roadFeatures);

        assertCovers("all", expected, all);
        assertEquals(roadBounds, all.getBounds());

        FeatureCollection some = road.getFeatures(rd12Filter);
        assertEquals(2, some.size());
        assertEquals(rd12Bounds, some.getBounds());
        assertEquals(some.getSchema(), road.getSchema());

        DefaultQuery query = new DefaultQuery("road", rd12Filter, new String[] { "name", });

        FeatureCollection half = road.getFeatures(query);
        assertEquals(2, half.size());
        assertEquals(1, half.getSchema().getAttributeCount());

        FeatureIterator reader = half.features();
        FeatureType type = half.getSchema();
        reader.close();

        FeatureType actual = half.getSchema();

        assertEquals(type.getTypeName(), actual.getTypeName());
        assertEquals(type.getNamespace(), actual.getNamespace());
        assertEquals(type.getAttributeCount(), actual.getAttributeCount());

        for (int i = 0; i < type.getAttributeCount(); i++) {
            assertEquals(type.getAttributeType(i), actual.getAttributeType(i));
        }

        assertNull(type.getPrimaryGeometry());
        assertEquals(type.getPrimaryGeometry(), actual.getPrimaryGeometry());
        assertEquals(type, actual);

        Envelope b = half.getBounds();
        assertEquals(new Envelope(1, 5, 0, 4), b);
    }

    public void testGetFeatureSourceRiver()
        throws NoSuchElementException, IOException, IllegalAttributeException {
        FeatureSource river = data.getFeatureSource("river");

        assertSame(riverType, river.getSchema());
        assertSame(data, river.getDataStore());

        FeatureCollection all = river.getFeatures();
        assertEquals(2, all.size());
        assertEquals(riverBounds, all.getBounds());
        assertTrue("rivers", covers(all.features(), riverFeatures));

        FeatureCollection expected = DataUtilities.collection(riverFeatures);
        assertCovers("all", expected, all);
        assertEquals(riverBounds, all.getBounds());
    }

    //
    // Feature Store Testing
    //
    public void testGetFeatureStoreModifyFeatures1() throws IOException {
        FeatureStore road = (FeatureStore) data.getFeatureSource("road");
        AttributeType name = roadType.getAttributeType("name");
        road.modifyFeatures(name, "changed", rd1Filter);

        FeatureCollection results = road.getFeatures(rd1Filter);
        assertEquals("changed", results.features().next().getAttribute("name"));
    }

    public void testGetFeatureStoreModifyFeatures2() throws IOException {
        FeatureStore road = (FeatureStore) data.getFeatureSource("road");
        AttributeType name = roadType.getAttributeType("name");
        road.modifyFeatures(new AttributeType[] { name, }, new Object[] { "changed", }, rd1Filter);

        FeatureCollection results = road.getFeatures(rd1Filter);
        assertEquals("changed", results.features().next().getAttribute("name"));
    }

    public void testGetFeatureStoreRemoveFeatures() throws IOException {
        FeatureStore road = (FeatureStore) data.getFeatureSource("road");

        road.removeFeatures(rd1Filter);
        assertEquals(0, road.getFeatures(rd1Filter).size());
        assertEquals(roadFeatures.length - 1, road.getFeatures().size());
    }

    public void testGetFeatureStoreAddFeatures() throws IOException {
        FeatureReader reader = DataUtilities.reader(new Feature[] { newRoad, });
        FeatureStore road = (FeatureStore) data.getFeatureSource("road");

        road.addFeatures(DataUtilities.collection(reader));
        assertEquals(roadFeatures.length + 1, road.getFeatures().size());
    }

    public void testGetFeatureStoreSetFeatures() throws IOException {
        FeatureReader reader = DataUtilities.reader(new Feature[] { newRoad, });
        FeatureStore road = (FeatureStore) data.getFeatureSource("road");

        road.setFeatures(reader);
        assertEquals(1, road.getFeatures().size());
    }

    public void testGetFeatureStoreTransactionSupport()
        throws Exception {
        Transaction t1 = new DefaultTransaction();
        Transaction t2 = new DefaultTransaction();

        FeatureStore road = (FeatureStore) data.getFeatureSource("road");
        FeatureStore road1 = (FeatureStore) data.getFeatureSource("road");
        FeatureStore road2 = (FeatureStore) data.getFeatureSource("road");

        road1.setTransaction(t1);
        road2.setTransaction(t2);

        Feature feature;
        Feature[] ORIGIONAL = roadFeatures;
        Feature[] REMOVE = new Feature[ORIGIONAL.length - 1];
        Feature[] ADD = new Feature[ORIGIONAL.length + 1];
        Feature[] FINAL = new Feature[ORIGIONAL.length];
        int i;
        int index;
        index = 0;

        for (i = 0; i < ORIGIONAL.length; i++) {
            feature = ORIGIONAL[i];

            if (!feature.getID().equals("road.rd1")) {
                REMOVE[index++] = feature;
            }
        }

        for (i = 0; i < ORIGIONAL.length; i++) {
            ADD[i] = ORIGIONAL[i];
        }

        ADD[i] = newRoad;

        for (i = 0; i < REMOVE.length; i++) {
            FINAL[i] = REMOVE[i];
        }

        FINAL[i] = newRoad;

        // start of with ORIGINAL
        assertTrue(covers(road.getFeatures().features(), ORIGIONAL));

        // road1 removes road.rd1 on t1
        // -------------------------------
        // - tests transaction independence from DataStore
        road1.removeFeatures(rd1Filter);

        // still have ORIGIONAL and t1 has REMOVE
        assertTrue(covers(road.getFeatures().features(), ORIGIONAL));
        assertTrue(covers(road1.getFeatures().features(), REMOVE));

        // road2 adds road.rd4 on t2
        // ----------------------------
        // - tests transaction independence from each other
        FeatureReader reader = DataUtilities.reader(new Feature[] { newRoad, });
        road2.addFeatures(DataUtilities.collection(reader));

        // We still have ORIGIONAL, t1 has REMOVE, and t2 has ADD
        assertTrue(covers(road.getFeatures().features(), ORIGIONAL));
        assertTrue(covers(road1.getFeatures().features(), REMOVE));
        assertTrue(coversLax(road2.getFeatures().features(), ADD));

        // commit t1
        // ---------
        // -ensure that delayed writing of transactions takes place
        //
        t1.commit();

        // We now have REMOVE, as does t1 (which has not additional diffs)
        // t2 will have FINAL
        assertTrue(covers(road.getFeatures().features(), REMOVE));
        assertTrue(covers(road1.getFeatures().features(), REMOVE));
        assertTrue(coversLax(road2.getFeatures().features(), FINAL));

        // commit t2
        // ---------
        // -ensure that everyone is FINAL at the end of the day
        t2.commit();

        // We now have Number( remove one and add one)
        assertTrue(coversLax(road.getFeatures().features(), FINAL));
        assertTrue(coversLax(road1.getFeatures().features(), FINAL));
        assertTrue(coversLax(road2.getFeatures().features(), FINAL));
    }

    boolean isLocked(String typeName, String fid) {
        InProcessLockingManager lockingManager = (InProcessLockingManager) data.getLockingManager();

        return lockingManager.isLocked(typeName, fid);
    }

    public void ztestFeatureEvents() throws Exception {
        FeatureStore store1 = (FeatureStore) data.getFeatureSource(roadFeatures[0].getFeatureType()
                                                                                  .getTypeName());
        FeatureStore store2 = (FeatureStore) data.getFeatureSource(roadFeatures[0].getFeatureType()
                                                                                  .getTypeName());
        store1.setTransaction(new DefaultTransaction());
        class Listener implements FeatureListener {
            String name;
            List events = new ArrayList();

            public Listener(String name) {
                this.name = name;
            }

            public void changed(FeatureEvent featureEvent) {
                this.events.add(featureEvent);
            }

            FeatureEvent getEvent(int i) {
                return (FeatureEvent) events.get(i);
            }
        }

        Listener listener1 = new Listener("one");
        Listener listener2 = new Listener("two");

        store1.addFeatureListener(listener1);
        store2.addFeatureListener(listener2);

        FilterFactory factory = FilterFactoryFinder.createFilterFactory();

        // test that only the listener listening with the current transaction gets the event.
        final Feature feature = roadFeatures[0];
        store1.removeFeatures(factory.createFidFilter(feature.getID()));
        assertEquals(1, listener1.events.size());
        assertEquals(0, listener2.events.size());

        FeatureEvent event = listener1.getEvent(0);
        assertEquals(feature.getBounds(), event.getBounds());
        assertEquals(FeatureEvent.FEATURES_REMOVED, event.getEventType());

        // test that commit only sends events to listener2.
        listener1.events.clear();
        listener2.events.clear();

        store1.getTransaction().commit();

        assertEquals(0, listener1.events.size());

        // changed 3 to 2
        assertEquals(2, listener2.events.size());
        event = listener2.getEvent(0);
        assertEquals(feature.getBounds(), event.getBounds());
        assertEquals(FeatureEvent.FEATURES_REMOVED, event.getEventType());

        // test add same as modify
        listener1.events.clear();
        listener2.events.clear();

        store1.addFeatures(DataUtilities.collection(feature));

        assertEquals(1, listener1.events.size());
        event = listener1.getEvent(0);
        assertEquals(feature.getBounds(), event.getBounds());
        assertEquals(FeatureEvent.FEATURES_ADDED, event.getEventType());
        assertEquals(0, listener2.events.size());

        // test that rollback only sends events to listener1.
        listener1.events.clear();
        listener2.events.clear();

        store1.getTransaction().rollback();

        assertEquals(1, listener1.events.size());
        event = listener1.getEvent(0);
        assertNull(event.getBounds());
        assertEquals(FeatureEvent.FEATURES_CHANGED, event.getEventType());

        assertEquals(0, listener2.events.size());

        // this is how Auto_commit is supposed to work
        listener1.events.clear();
        listener2.events.clear();
        store2.addFeatures(DataUtilities.collection(feature));

        // ???
        assertEquals(1, listener1.events.size());
        event = listener1.getEvent(0);
        assertEquals(feature.getBounds(), event.getBounds());
        assertEquals(FeatureEvent.FEATURES_ADDED, event.getEventType());
        assertEquals(0, listener2.events.size());
    }

    //
    // FeatureLocking Testing
    //
    /*
     * Test for void lockFeatures()
     */
    public void testLockFeatures() throws IOException {
        FeatureLock lock = FeatureLockFactory.generate("test", 3600);
        FeatureLocking road = (FeatureLocking) data.getFeatureSource("road");
        road.setFeatureLock(lock);

        assertFalse(isLocked("road", "road.rd1"));
        road.lockFeatures();
        assertTrue(isLocked("road", "road.rd1"));
    }

    public void testUnLockFeatures() throws IOException {
        FeatureLock lock = FeatureLockFactory.generate("test", 3600);
        FeatureLocking road = (FeatureLocking) data.getFeatureSource("road");
        road.setFeatureLock(lock);
        road.lockFeatures();

        try {
            road.unLockFeatures();
            fail("unlock should fail due on AUTO_COMMIT");
        } catch (IOException expected) {
        }

        Transaction t = new DefaultTransaction();
        road.setTransaction(t);

        try {
            road.unLockFeatures();
            fail("unlock should fail due lack of authorization");
        } catch (IOException expected) {
        }

        t.addAuthorization(lock.getAuthorization());
        road.unLockFeatures();
    }

    public void testLockFeatureInteraction() throws IOException {
        FeatureLock lockA = FeatureLockFactory.generate("LockA", 3600);
        FeatureLock lockB = FeatureLockFactory.generate("LockB", 3600);
        Transaction t1 = new DefaultTransaction();
        Transaction t2 = new DefaultTransaction();
        FeatureLocking road1 = (FeatureLocking) data.getFeatureSource("road");
        FeatureLocking road2 = (FeatureLocking) data.getFeatureSource("road");
        road1.setTransaction(t1);
        road2.setTransaction(t2);
        road1.setFeatureLock(lockA);
        road2.setFeatureLock(lockB);

        assertFalse(isLocked("road", "road.rd1"));
        assertFalse(isLocked("road", "road.rd2"));
        assertFalse(isLocked("road", "road.rd3"));

        road1.lockFeatures(rd1Filter);
        assertTrue(isLocked("road", "road.rd1"));
        assertFalse(isLocked("road", "road.rd2"));
        assertFalse(isLocked("road", "road.rd3"));

        road2.lockFeatures(rd2Filter);
        assertTrue(isLocked("road", "road.rd1"));
        assertTrue(isLocked("road", "road.rd2"));
        assertFalse(isLocked("road", "road.rd3"));

        try {
            road1.unLockFeatures(rd1Filter);
            fail("need authorization");
        } catch (IOException expected) {
        }

        t1.addAuthorization(lockA.getAuthorization());

        try {
            road1.unLockFeatures(rd2Filter);
            fail("need correct authorization");
        } catch (IOException expected) {
        }

        road1.unLockFeatures(rd1Filter);
        assertFalse(isLocked("road", "road.rd1"));
        assertTrue(isLocked("road", "road.rd2"));
        assertFalse(isLocked("road", "road.rd3"));

        t2.addAuthorization(lockB.getAuthorization());
        road2.unLockFeatures(rd2Filter);
        assertFalse(isLocked("road", "road.rd1"));
        assertFalse(isLocked("road", "road.rd2"));
        assertFalse(isLocked("road", "road.rd3"));
    }

    public void testGetFeatureLockingExpire() throws Exception {
        FeatureLock lock = FeatureLockFactory.generate("Timed", 1);
        FeatureLocking road = (FeatureLocking) data.getFeatureSource("road");
        road.setFeatureLock(lock);
        assertFalse(isLocked("road", "road.rd1"));
        road.lockFeatures(rd1Filter);
        assertTrue(isLocked("road", "road.rd1"));
        Thread.sleep(100);
        assertFalse(isLocked("road", "road.rd1"));
    }
}
