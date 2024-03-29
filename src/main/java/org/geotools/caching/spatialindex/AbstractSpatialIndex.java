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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;


/** This is a base class for implementing spatial indexes.
 * It provides common routines useful for every type of indexes.
 *
 * @author Christophe Rousson, SoC 2007, CRG-ULAVAL
 *
 */
public abstract class AbstractSpatialIndex implements SpatialIndex {
    //public static final int RtreeVariantQuadratic = 1;
    //public static final int RtreeVariantLinear = 2;
    //public static final int RtreeVariantRstar = 3;
    //public static final int PersistentIndex = 1;
    //public static final int PersistentLeaf = 2;
    public static final int ContainmentQuery = 1;
    public static final int IntersectionQuery = 2;

    /**
     * The node at the root of index.
     * All others nodes should be direct or indirect children of this one.
     */
    protected NodeIdentifier root;
    protected Node rootNode = null;
    protected Storage store;

    /**
     * Indexes can be n-dimensional, but queries and data should be consistent with regards to dimensions.
     * This is the dimension of data shapes in the index, and should be considered final.
     * (It is not because it makes things easier to initialize index from a serialized form).
     */
    protected int dimension;
    protected Region infiniteRegion;
    protected ArrayList writeNodeCommands = new ArrayList();
    protected ArrayList readNodeCommands = new ArrayList();
    protected ArrayList deleteNodeCommands = new ArrayList();
    protected ThisStatistics stats = new ThisStatistics();

    public void addDeleteNodeCommand(NodeCommand nc) {
        deleteNodeCommands.add(nc);
    }

    public void addReadNodeCommand(NodeCommand nc) {
        readNodeCommands.add(nc);
    }

    public void addWriteNodeCommand(NodeCommand nc) {
        writeNodeCommands.add(nc);
    }

    public void intersectionQuery(Shape query, Visitor v) {
        if (query.getDimension() != dimension) {
            throw new IllegalArgumentException(
                "intersectionQuery: Shape has the wrong number of dimensions.");
        }

        rangeQuery(IntersectionQuery, query, v);
    }

    public void containmentQuery(Shape query, Visitor v) {
        if (query.getDimension() != dimension) {
            throw new IllegalArgumentException(
                "containmentQuery: Shape has the wrong number of dimensions.");
        }

        rangeQuery(ContainmentQuery, query, v);
    }

    public void pointLocationQuery(Point query, Visitor v) {
        if (query.getDimension() != dimension) {
            throw new IllegalArgumentException(
                "pointLocationQuery: Shape has the wrong number of dimensions.");
        }

        /*Region r = null;
           if (query instanceof Point) {
               r = new Region((Point) query, (Point) query);
           } else if (query instanceof Region) {
               r = (Region) query;
           } else {
               throw new IllegalArgumentException(
                   "pointLocationQuery: Shape can be Point or Region only.");
           }*/
        Region r = new Region(query, query);

        rangeQuery(IntersectionQuery, r, v);
    }

    /** Common algorithm used by both intersection and containment queries.
     *
     * @param type
     * @param query
     * @param v
     *
     * TODO: remember child index where to search from on next passage
     */
    protected void rangeQuery(int type, Shape query, Visitor v) {
        NodeIdentifier current = this.root;
        Node currentNode = null;
        current.setVisited(false);

        Stack<Node> nodes = new Stack<Node>();

        if (query.intersects(current.getShape())) {
            currentNode = readNode(current);
            nodes.push(currentNode);
        }

        while (!nodes.isEmpty()) {
            currentNode = (Node) nodes.pop();
            current = currentNode.getIdentifier();

            if (!current.isVisited()) {
                v.visitNode(currentNode);

                for (int i = 0; i < currentNode.getChildrenCount(); i++) {
                    currentNode.getChildIdentifier(i).setVisited(false);
                }

                if (v.isDataVisitor()) { // skip if visitor does nothing with data
                                         // visitData check for actual containement or intersection
                    visitData(currentNode, v, query, type);
                }

                current.setVisited(true);
            }

            // TODO: start from last child + 1 rather than from 0
            for (int i = 0; i < currentNode.getChildrenCount(); i++) {
                NodeIdentifier child = currentNode.getChildIdentifier(i);

                if (!child.isVisited()) {
                    if (query.intersects(child.getShape())) {
                        // we will go back to this one later to examine other children
                        nodes.push(currentNode);
                        // meanwhile, we put one child at a time into stack, so we do not waste space
                        nodes.push(readNode(child));

                        break;
                    } else {
                        // we won't have to compute intersection again and again
                        child.setVisited(true);
                    }
                }
            }
        }
    }

    /** Visit data associated with a node using given visitor.
     * At this stage, we only know that node's MBR intersects query.
     * This method is reponsible for iterating over node's data, if any,
     * and for checking if data is actually part of the query result.
     * Then it uses the visitor's visit() method on the selected data.
     *
     * @param node to visit
     * @param visitor for callback
     * @param query
     * @param type of query, either containement or intersection (@see AbstractSpatialIndex)
     */
    protected abstract void visitData(Node n, Visitor v, Shape query, int type);

    public void nearestNeighborQuery(int k, Shape query, Visitor v, NearestNeighborComparator nnc) {
        // TODO Auto-generated method stub
    }

    public void nearestNeighborQuery(int k, Shape query, Visitor v) {
        // TODO Auto-generated method stub
    }

    public void queryStrategy(QueryStrategy qs) {
        NodeIdentifier current = this.root;
        Node currentNode = null;

        while (true) {
            boolean[] hasNext = new boolean[] { false };
            currentNode = readNode(current);
            current = qs.getNextNode(currentNode, hasNext);

            if (hasNext[0] == false) {
                break;
            }
        }
    }

    public boolean deleteData(Shape shape, int id) {
        if (shape.getDimension() != dimension) {
            throw new IllegalArgumentException(
                "deleteData: Shape has the wrong number of dimensions.");
        }

        if (this.root.getShape().intersects(shape)) {
            return deleteData(this.root, shape, id);
        } else {
            return false;
        }
    }

    /** Try to delete data from the specified node,
     * or its children.
     *
     * @param node
     * @param shape of data to delete
     * @param id of data to delete
     * @return true if some data has been found and deleted.
     */
    protected abstract boolean deleteData(NodeIdentifier n, Shape shape, int id);

    public void insertData(Object data, Shape shape, int id) {
        if (shape.getDimension() != dimension) {
            throw new IllegalArgumentException(
                "insertData: Shape has the wrong number of dimensions.");
        }

        if (this.root.getShape().contains(shape)) {
            insertData(this.root, data, shape, id);
        } else {
            insertDataOutOfBounds(data, shape, id);
        }
    }

    /** Insert new data into target node. Node may delegate to child nodes, if required.
     * Implementation note : it is assumed arguments verify :
     * <code>node.getShape().contains(shape)</code>
     * So this must be checked before calling this method.
     *
     * @param node where to insert data
     * @param data
     * @param shape of data
     * @param id of data
     */
    protected abstract void insertData(NodeIdentifier n, Object data, Shape shape, int id);

    /** Insert new data with shape not contained in the current index.
     * Some indexes may require to recreate the root or the index,
     * depending on the type of index ...
     *
     * @param data
     * @param shape
     * @param id
     */
    protected abstract void insertDataOutOfBounds(Object data, Shape shape, int id);

    public Statistics getStatistics() {
        return stats;
    }

    protected Node readNode(NodeIdentifier id) {
        Node ret;

        if (id.equals(this.root)) {
            return rootNode;
        }

        ret = store.get(id);
        stats.stats_reads++;

        for (Iterator it = readNodeCommands.iterator(); it.hasNext();) {
            NodeCommand next = (NodeCommand) it.next();
            next.execute(ret);
        }

        return ret;
    }

    protected void writeNode(Node node) {
        if (node.getIdentifier().equals(this.root)) {
            this.rootNode = node;

            return;
        }

        store.put(node);
        stats.stats_writes++;

        for (Iterator it = writeNodeCommands.iterator(); it.hasNext();) {
            NodeCommand next = (NodeCommand) it.next();
            next.execute(node);
        }
    }

    protected void deleteNode(NodeIdentifier id) {
        store.remove(id);
    }

    /** Data structure to store statistics about the index.
     *
     * @author Christophe Rousson, SoC 2007, CRG-ULAVAL
     *
     */
    public class ThisStatistics implements Statistics {
        int stats_reads = 0;
        int stats_writes = 0;
        int stats_nodes = 0;
        int stats_data = 0;

        public long getNumberOfData() {
            return stats_data;
        }

        public long getNumberOfNodes() {
            return stats_nodes;
        }

        public long getReads() {
            return stats_reads;
        }

        public long getWrites() {
            return stats_writes;
        }

        public void addToReadsCounter(int count) {
            stats_reads += count;
        }

        public void addToWritesCounter(int count) {
            stats_writes += count;
        }

        public void addToNodesCounter(int count) {
            stats_nodes += count;
        }

        public void addToDataCounter(int count) {
            stats_data += count;
        }

        public void reset() {
            stats_reads = 0;
            stats_writes = 0;
            stats_nodes = 0;
            stats_data = 0;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Reads = " + stats_reads);
            sb.append(" ; Writes = " + stats_writes);
            sb.append(" ; Nodes = " + stats_nodes);
            sb.append(" ; Data = " + stats_data);

            return sb.toString();
        }
    }
}
