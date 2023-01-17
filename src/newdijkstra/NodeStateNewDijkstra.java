package newdijkstra;

import template.NodeStateTemplate;

import java.util.Objects;

public class NodeStateNewDijkstra extends NodeStateTemplate{
    public static final int NEW_DIJKSTRA_NODE_STATE_0 = 0;
    public static final int NEW_DIJKSTRA_NODE_STATE_1 = 1;
    public static final int NEW_DIJKSTRA_NODE_STATE_2 = 2;
    public static final int NEW_DIJKSTRA_NUMBER_OF_NODE_STATES = 3;

    private int nodeId; // id of a node in the ring. From 0 to n-1 where n is the number of nodes in the ring.
    private int nodeValue; // node value. From {0, 1, 2} according to Dijkstra 3 state program

    public NodeStateNewDijkstra(int nodeId, int nodeValue){
        this.nodeId = nodeId;
        this.nodeValue = nodeValue;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getNodeValue() {
        return nodeValue;
    }

    public void setNodeValue(int nodeValue) {
        this.nodeValue = nodeValue;
    }

    // implement superclass abstract methods
    public boolean equals(Object otherNodeState){
        if (this == otherNodeState)
            return true;
        if(otherNodeState == null || otherNodeState.getClass() != this.getClass())
            return false;
        NodeStateNewDijkstra that = (NodeStateNewDijkstra) otherNodeState;
        return (this.getNodeValue() == that.getNodeValue() && this.getNodeId() == that.getNodeId());
    }

    public int hashCode(){
        return Objects.hash(nodeId, nodeValue);
    }

    public NodeStateNewDijkstra getDeepCopy(){
        return new NodeStateNewDijkstra(this.getNodeId(), this.getNodeValue());
    }

    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("nodeId = " + this.getNodeId() + " ");
        str.append("nodeValue = " + this.getNodeValue());
        return str.toString();
    }

    // implement Comparable interface
    public int compareTo(Object o){
        if (this == o)
            return 0;
        // assume o is not null
        NodeStateNewDijkstra other = (NodeStateNewDijkstra) o;

        // we can define whatever way of comparing two NodeStateNewDijkstra objects suitable for our purpose
        // for example, we can compare nodeId first
        //      if they differ then the comparison will be the results
        //      if they equal then we compare nodeValue
        if (this.getNodeId() < other.getNodeId())
            return -1;
        if (this.getNodeId() > other.getNodeId())
            return 1;
        // now nodeIds are equals, compare nodeValue
        if(this.getNodeValue() < other.getNodeValue())
            return -1;
        if(this.getNodeValue() > other.getNodeValue())
            return 1;
        // nodeIds and nodeValues are equals, so two objects' contents are equal
        return 0;
    }

}


