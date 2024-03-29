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
package org.geotools.caching;

import java.util.Iterator;
import java.util.LinkedHashMap;
import org.geotools.caching.spatialindex.NodeIdentifier;


public class LRUEvictionPolicy implements EvictionPolicy {
    LinkedHashMap<NodeIdentifier, Object> queue;
    EvictableTree tree;

    public LRUEvictionPolicy(EvictableTree tree) {
        this.queue = new LinkedHashMap<NodeIdentifier, Object>(100, .75f, true);
        this.tree = tree;
    }

    public void evict() {
        Iterator<NodeIdentifier> it = queue.keySet().iterator();

        if (it.hasNext()) {
            NodeIdentifier node = it.next();
            tree.evict(node);
            queue.remove(node);
        }
    }

    public void access(NodeIdentifier node) {
        if (queue.containsKey(node)) {
            queue.get(node);
        } else {
            queue.put(node, null);
        }
    }
}
