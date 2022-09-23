package dijkstra3states;

import template.NodeStateTemplate;

import java.util.Objects;

/**
 * State of a node in Dijkstra 3-states ring program.
 * Which is an assignment of nodeId to one of the three values 0, 1, 2
 */
public class NodeStateDijkstra3States extends NodeStateTemplate {
    public static final int DIJKSTRA_3_STATES_NODE_STATE_0 = 0;
    public static final int DIJKSTRA_3_STATES_NODE_STATE_1 = 1;
    public static final int DIJKSTRA_3_STATES_NODE_STATE_2 = 2;
    public static final int DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES = 3;

    private int nodeId; // start from 0
    private int nodeStateValue;

    public NodeStateDijkstra3States(int nodeId, int nodeStateValue){
        this.nodeId = nodeId;
        this.nodeStateValue = nodeStateValue;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getNodeStateValue() {
        return nodeStateValue;
    }

    public void setNodeStateValue(int nodeStateValue) {
        this.nodeStateValue = nodeStateValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeStateDijkstra3States that = (NodeStateDijkstra3States) o;
        return nodeId == that.nodeId &&
                nodeStateValue == that.nodeStateValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, nodeStateValue);
    }

    public NodeStateDijkstra3States getDeepCopy(){
        return new NodeStateDijkstra3States(getNodeId(), getNodeStateValue());
    }

    // implement Comparable Interface
    public int compareTo(Object o) {
        NodeStateDijkstra3States otherState = (NodeStateDijkstra3States) o;

        // assume nodeId is the same
        // since it is used in compareTo() of ProgramConfigurationDijkstra3State
        // in which we compare content of 2 nodes with same Id from 2 configurations
        if(this.getNodeStateValue() > otherState.getNodeStateValue()) {
            return 1;
        }
        if(this.getNodeStateValue() < otherState.getNodeStateValue()) {
            return -1;
        }
        return 0;
    }


//    public String toString(){
//        StringBuilder s = new StringBuilder();
//        s.append(nodeId + " : " + nodeStateValue);
//        return s.toString();
//    }

    public String toString(){
        StringBuilder s = new StringBuilder();
        s.append(nodeStateValue);
        return s.toString();
    }

}
