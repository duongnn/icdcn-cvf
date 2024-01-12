package coloring;

import datastructure.NodeActionEvaluationResults;
import datastructure.NodePerturbationResults;
import template.ProgramConfigurationTemplate;
import template.SuccessorInfo;

import java.util.*;

import static main.AnalyzeCvfs.graphTopology;
import static main.AnalyzeCvfs.maxDegree;

/**
 */

public class ProgramConfigurationColoring  extends ProgramConfigurationTemplate {
    public ProgramConfigurationColoring(
            int numberOfNodes,
            TreeMap<Integer, NodeStateColoring> nodeStateMap,
            int cvf,
            int probeLimit) {
        super(numberOfNodes, nodeStateMap, cvf, probeLimit);
    }

    void updateStateForANode(int nodeId, int color){
        TreeMap<Integer, NodeStateColoring> tm = getNodeStateMap();
        NodeStateColoring nodeInfo = tm.get(nodeId);

        if(nodeInfo == null){
            tm.put(nodeId, new NodeStateColoring(nodeId, color));
        }else{
            nodeInfo.setNodeColor(color);
        }
    }

    void setStateForAllNodes(int color){
        for(int i = 0; i < getNumberOfNodes(); i ++){
            updateStateForANode(i, color);
        }
    }

    /***********************************************************************
     ***  Implement abstract method of superclass
     ************************************************************************/

    public ProgramConfigurationTemplate getDeepCopy(){
        TreeMap<Integer, NodeStateColoring> currentMap = getNodeStateMap();
        TreeMap<Integer, NodeStateColoring> newNodeStateMap = new TreeMap<>();

        for(Map.Entry<Integer,NodeStateColoring> entry: currentMap.entrySet()){
            newNodeStateMap.put(entry.getKey(), entry.getValue().getDeepCopy());
        }

        return new ProgramConfigurationColoring(getNumberOfNodes(), newNodeStateMap, getCvf(), getProbeLimit());
    }

    public long getSizeOfStateSpace(){
        if(cvf == CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION){
            return getSizeOfStateSpaceWithTopology();
        }else {
            // for each node
            //    the color could have maxDegree + 1 options, from 0 to maxDegree
            return (long) Math.pow(maxDegree + 1, getNumberOfNodes());
        }
    }

    // estimate size of state space with consideration of topology
    // It is expected to be more efficient than getSizeOfStateSpace, i.e. it gives smaller size
    public long getSizeOfStateSpaceWithTopology(){
        // for each node
        //    k+1 options for color where k is the degree of that node
        //    (+1 since it include color 0)
        long count = 1;
        for(int nodeId = 0; nodeId < getNumberOfNodes(); nodeId++){
            count = count * (graphTopology.get(nodeId).size() + 1);
        }
        return count;
    }

    public int getNodeColor(int nodePosition){
        NodeStateColoring nodeState = (NodeStateColoring) getNodeStateMap().get(nodePosition);
        return nodeState.getNodeColor();
    }


    /**
     * Perturb arbitrarily the state of a specified node.
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by arbitrarily perturbing a node
     *         Current configuration is excluded in the results.
     */
    public NodePerturbationResults perturbANodeArbitrarily(int nodeId){
        TreeSet<SuccessorInfo> perturbedSuccessorList = new TreeSet<>();

        int currentColor = getNodeColor(nodeId);

        // the perturbed state is different from the current state of the node
        for(int color = 0; color <= maxDegree; color ++){
            if(color != currentColor) {
                perturbedSuccessorList.add(
                        new SuccessorInfo(nodeId,
                                copyConfigurationAndReplaceState(
                                        nodeId,
                                        new NodeStateColoring(nodeId, color)
                                )
                ));
            }
        }

        return new NodePerturbationResults(perturbedSuccessorList);
    }


    /**
     * Perturb the state of a specified node but the perturbation is constrained by program transition rules
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by arbitrarily perturbing a node
     *         Current configuration is excluded in the results.
     */
    public NodePerturbationResults perturbANodeWithContraint(int nodeId){
        NodeStateColoring nodeInfo = (NodeStateColoring) getNodeStateMap().get(nodeId);
        int currentColor = nodeInfo.getNodeColor();
        int nodeDegree = graphTopology.get(nodeId).size();

        // List of possible successors obtained by program transition
        // caused by reading stale neighbor information (i.e. neighbor state could be perturbed).
        // More accurate way to compute tighter upper bound
        // For coloring, no matter what is the color of neighbors,
        // the color should not exceed the degree of the node if the algorithm chooses smallest color available
        TreeSet<SuccessorInfo> constrainedPerturbedSuccessorList = new TreeSet<>();

        // the perturbed state is different from the current state of the node
        for(int color = 0; color <= nodeDegree; color ++){
            if(color != currentColor) {
                constrainedPerturbedSuccessorList.add(
                        new SuccessorInfo(nodeId,
                                copyConfigurationAndReplaceState(
                                    nodeId,
                                    new NodeStateColoring(nodeId, color)
                        )
                ));
            }
        }

        return new NodePerturbationResults(constrainedPerturbedSuccessorList);
    }

    /**
     * Perturb the state of a specified node but the perturbation is constrained by program transition rules
     * Furthermore, we consider the topology restriction as well, e.g. color cannot exceed node degree
     * Although it is expected to be more efficient than perturbANodeWithContraint,
     * in fact, there is no difference from perturbANodeWithContraint
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by arbitrarily perturbing a node
     *         Current configuration is excluded in the results.
     */
    public NodePerturbationResults perturbANodeWithContraintAndTopologyRestriction(int nodeId) {
        return perturbANodeWithContraint(nodeId);
    }

    /**
     * @return true if the current state is inside the invariant (i.e. it is a legitimate state)
     *         false otherwise
     */
    public boolean isInsideInvariant(){
        // In silently stabilizing program like coloring,
        // a configuration is in the invariant if no node is enabled
        return (getNumberOfPrivileges() == 0);
    }

    /**
     * Change the current program config to the first program configuration in some enumeration scheme.
     * This scheme must be the same as the enumeration scheme used in getLastProgramConfig()
     * @return this object
     */
    public ProgramConfigurationColoring moveToFirstProgramConfig(){
        // in coloring, the first program configuration is when for all nodes
        // the color value is minimal, i.e. it is equal to 0
        setStateForAllNodes(0);
        return this;
    }

    /**
     * Change the current program config to the last program configuration in some enumeration scheme.
     * This scheme must be the same as the enumeration scheme used in getFirstProgramConfig()
     * @return this object
     */
    public ProgramConfigurationColoring moveToLastProgramConfig(){
        // the last program configuration is when for all nodes
        // the color value is maximal
        if(cvf == CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION){
            // with topology restriction, a node state is maximal when
            // color is equal to node degree
            for(int nodeId = 0; nodeId < getNumberOfNodes(); nodeId ++){
                updateStateForANode(nodeId, graphTopology.get(nodeId).size());
            }
            return this;
        }else {
            // without topology restriction, a node state is maximal when
            // color is equal to the maxDegree
            setStateForAllNodes(maxDegree);
            return this;
        }
    }


    /**
     * Count how many privileges (enabled actions) at a node in the current program config, and
     * list of successor configurations if those actions are executed
     * @param nodeId nodeId of the interested node.
     * @return evaluation result of a node, which contains:
     *           number of enabled actions (privileges), and
     *           list of successor states if the actions at specified node are executed.
     *         If the node is not enabled, return empty list.
     *         Since action depends on specific program, this method should be implemented in subclass
     */
    public NodeActionEvaluationResults evaluateANodeActions(int nodeId){
        TreeSet<SuccessorInfo> listOfSuccessors = new TreeSet();
        int privilegesCount = 0;

        // local information of specified node
        NodeStateColoring nodeInfo = (NodeStateColoring) getNodeStateMap().get(nodeId);
        int nodeColor = nodeInfo.getNodeColor();
        Vector<Integer> nodeNbr = graphTopology.get(nodeId);

        int nodeDegree;
        if(cvf == CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION){
            nodeDegree = nodeNbr.size();
        }else{
            nodeDegree = maxDegree;
        }

        // unlike Dijkstra 3 states program where there is top, bottom, and other nodes,
        // in coloring, there is no difference between nodes.
        // If the algorithm chooses the smallest color available, then there is no non-determinism
        TreeSet<Integer> acceptableColors = new TreeSet<>();
        for(int color = 0; color <= nodeDegree; color ++){
            acceptableColors.add(color);
        }

        for(int nbr : nodeNbr){
            NodeStateColoring nbrInfo = (NodeStateColoring) getNodeStateMap().get(nbr);
            int nbrColor = nbrInfo.getNodeColor();
            acceptableColors.remove(nbrColor);
        }

        if(acceptableColors.contains(nodeColor)){
            // no action is needed
            // privilegesCount is 0
            // listOfSuccessors is empty
        }else {
            // have color conflict, we have to change color
            int chosenColor = acceptableColors.first();

            listOfSuccessors.add(
                    new SuccessorInfo(
                        nodeId,
                        copyConfigurationAndReplaceState(nodeId, new NodeStateColoring(nodeId, chosenColor))));
            privilegesCount++;
        }
        return new NodeActionEvaluationResults(nodeId, privilegesCount, listOfSuccessors);
    }

    /***********************************************************************
     *** Overriding methods
     ************************************************************************/

    // implement Comparable interface
    public int compareTo(Object o){
        ProgramConfigurationColoring other = (ProgramConfigurationColoring) o;

        if(this.getNumberOfNodes() > other.getNumberOfNodes())
            return 1;
        if(this.getNumberOfNodes() < other.getNumberOfNodes())
            return -1;

        // same number of nodes
        TreeMap<Integer, NodeStateColoring> myNodeStateMap = getNodeStateMap();
        TreeMap<Integer, NodeStateColoring> otherNodeStateMap = other.getNodeStateMap();
        for(int nodeId : myNodeStateMap.keySet()){
            int compareResult = myNodeStateMap.get(nodeId).compareTo(otherNodeStateMap.get(nodeId));
            if(compareResult < 0)
                return -1;
            if(compareResult > 0)
                return 1;
        }

        return 0;
    }

    /**
     * Change the current program config to the next program configuration in some enumeration scheme.
     * This scheme must be the same as the enumeration scheme used in getFirstProgramConfig(), getLastProgramConfig().
     * @return this object
     */
    @Override
    public ProgramConfigurationColoring moveToNextProgramConfig(){
        // find the first node from right to left (i.e. from nodeId = n-1 to nodeId = 0)
        // where the node state is not maximal
        int incrementPosition = getNumberOfNodes() - 1;
        NodeStateColoring nodeInfo = null;
        while(incrementPosition >= 0){
            nodeInfo = (NodeStateColoring) getNodeStateMap().get(incrementPosition);
            if(!nodeInfo.isNodeStateMaximal(cvf)){
                // current incrementPosition is not maximal yet
                break;
            }else{
                // current incrementPosition is maximal, move the next one on the left
                incrementPosition --;
            }
        }

        if(incrementPosition >= 0){
            // found a position not yet maximal
            // increment it
            updateStateForANode(incrementPosition, nodeInfo.moveToNextNodeState(cvf));
            // for all position/node before it, rewind
            for(int prevPosition = getNumberOfNodes() - 1; prevPosition > incrementPosition; prevPosition --){
                NodeStateColoring prevNodeInfo = (NodeStateColoring) getNodeStateMap().get(prevPosition);
                updateStateForANode(prevPosition, prevNodeInfo.moveToMinimalState());
            }
        }else{
            // all positions/nodes have maximal state
            // rewind all
            moveToFirstProgramConfig();
        }

        return this;
    }


}
