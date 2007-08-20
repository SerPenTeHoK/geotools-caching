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
package org.geotools.caching.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.opengis.feature.simple.SimpleFeature;
import org.geotools.feature.IllegalAttributeException;


public class MarshallerTest extends TestCase {
    public static Test suite() {
        return new TestSuite(MarshallerTest.class);
    }

    /** Marshall and unmarshall a DefaultFeature, and test for equality with the result.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws IllegalAttributeException
     */
    public void testMarshall()
        throws IOException, ClassNotFoundException, IllegalAttributeException {
        Generator gen = new Generator(1000, 1000);
        SimpleFeature f = (SimpleFeature) gen.createFeature(0);
        SimpleFeatureMarshaller m = new SimpleFeatureMarshaller();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        m.marshall(f, oos);

        byte[] ba = baos.toByteArray();
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        ObjectInputStream ois = new ObjectInputStream(bais);
        SimpleFeature newf = m.unmarshall(ois);
        bais.close();
        //System.out.println(f) ;
        //System.out.println(newf);
        assertTrue(f.equals(newf));
    }

    /** Marshall and unmarshall same DefaultFeature many times.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws IllegalAttributeException
     */
    public void testMultipleMarshall()
        throws IOException, ClassNotFoundException, IllegalAttributeException {
        Generator gen = new Generator(1000, 1000);
        int iterations = 10;
        SimpleFeature f = (SimpleFeature) gen.createFeature(0);
        SimpleFeature original = f;
        SimpleFeatureMarshaller m = new SimpleFeatureMarshaller();

        for (int i = 0; i < iterations; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            m.marshall(f, oos);
            baos.close();

            byte[] ba = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(ba);
            ObjectInputStream ois = new ObjectInputStream(bais);
            SimpleFeature newf = m.unmarshall(ois);
            bais.close();
            assertTrue(f.equals(newf));
            f = newf;
        }

        assertEquals(original, f);
    }

    /** Disabled test to mesure time to marshall/unmarshall features.
     *  Test results on my PC :
     *   <ul><li>0.8 ms per feature for a marshall/unmarshall cycle
     *       <li>0.2 ms per feature for marshalling only
     *   </ul>
     */
    public void ztestMarshallTime() {
        Generator gen = new Generator(1000, 1000);
        List<SimpleFeature> features = new ArrayList<SimpleFeature>();

        for (int i = 0; i < 10000; i++) {
            SimpleFeature f = (SimpleFeature) gen.createFeature(i);
            features.add(f);
        }

        long start = System.currentTimeMillis();

        for (int i = 0; i < 10000; i++) {
            SimpleFeature f = features.get(i);
            SimpleFeatureMarshaller marsh = new SimpleFeatureMarshaller();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                marsh.marshall(f, oos);

                byte[] ba = baos.toByteArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(ba);
                ObjectInputStream ois = new ObjectInputStream(bais);
                SimpleFeature newf = marsh.unmarshall(ois);

                if (!f.equals(newf)) {
                    throw new RuntimeException("Error at unmarshall");
                }

                if (i == (1000 * (i / 1000))) {
                    System.out.println(i);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                //	TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAttributeException e) {
                //	 TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        long stop = System.currentTimeMillis();
        System.out.println("Elapsed time for 10000 features : " + (stop - start) + " ms.");
    }
}
