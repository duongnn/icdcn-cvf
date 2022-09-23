package coloring;

import template.NodeStateTemplate;

import java.util.Objects;

import static main.AnalyzeCvfs.graphTopology;
import static main.AnalyzeCvfs.maxDegree;
import static template.ProgramConfigurationTemplate.CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION;

/**
 */

public class NodeStateColoring extends NodeStateTemplate {
    private int nodeId;
    private int nodeColor;

    public NodeStateColoring(int nodeId, int nodeColor){
        this.nodeId = nodeId;
        this.nodeColor = nodeColor;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getNodeColor() {
        return nodeColor;
    }

    public void setNodeColor(int nodeColor) {
        this.nodeColor = nodeColor;
    }

    private void incrementNodeColor(){
        this.nodeColor ++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeStateColoring that = (NodeStateColoring) o;
        // compare content
        return nodeId == that.nodeId
                && nodeColor == that.getNodeColor();
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, nodeColor);
    }

    public NodeStateColoring getDeepCopy(){
        return new NodeStateColoring(nodeId, nodeColor);
    }

    // implement Comparable Interface
    public int compareTo(Object o) {
        NodeStateColoring otherState = (NodeStateColoring) o;
        int theirNodeColor = otherState.getNodeColor();

        // assume nodeId is equal
        // since it is used in compareTo() of ProgramConfigurationColoring
        // in which we compare content of 2 nodes with same Id from 2 configurations
        if(nodeColor > theirNodeColor) {
            return 1;
        }
        if(nodeColor < theirNodeColor) {
            return -1;
        }

        return 0;
    }


    // functions for ordering between node states
    public boolean isNodeStateMinimal(){
        return getNodeColor() == 0;
    }

    public boolean isNodeStateMaximal(int cvf){
        if(cvf == CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION){
            return isNodeStateMaximalWithTopologyRestriction();
        }else{
            return isNodeStateMaximal();
        }
    }

    private boolean isNodeStateMaximal(){
        return getNodeColor() == maxDegree;
    }

    // more efficient implementation of isNodeStateMaximal
    private boolean isNodeStateMaximalWithTopologyRestriction(){
        return getNodeColor() >= graphTopology.get(nodeId).size();
    }

    public NodeStateColoring moveToNextNodeState(int cvf){
        if(cvf == CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION){
            return moveToNextNodeStateWithTopologyRestriction();
        }else{
            return moveToNextNodeState();
        }
    }

    // this function does not allocate new memory space
    // this function does not consider the specific connectivity of each node
    private NodeStateColoring moveToNextNodeState(){
        int maximalColor = maxDegree;

        if(getNodeColor() < maximalColor){
            this.incrementNodeColor();
        }else{
            this.setNodeColor(0);
        }

        return this;
    }

    // Differs from moveToNextNodeState in that it consider topology
    // It is expected to be more efficient
    private NodeStateColoring moveToNextNodeStateWithTopologyRestriction(){
        int maximalColorValue = graphTopology.get(nodeId).size();

        if(getNodeColor() < maximalColorValue){
            this.incrementNodeColor();
        }else{
            this.setNodeColor(0);
        }

        return this;
    }

    public NodeStateColoring moveToMinimalState(){
        this.setNodeColor(0);
        return this;
    }

    public String toString(){
        StringBuilder s = new StringBuilder();
        s.append(nodeColor);
        return s.toString();
    }

}
