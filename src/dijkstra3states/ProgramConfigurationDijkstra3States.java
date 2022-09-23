package dijkstra3states;

import datastructure.NodePerturbationResults;
import template.ProgramConfigurationTemplate;
import datastructure.NodeActionEvaluationResults;
import template.SuccessorInfo;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static dijkstra3states.NodeStateDijkstra3States.*;


/**
 * State/configuration for Dijkstra 3-state program
 */
public class ProgramConfigurationDijkstra3States extends ProgramConfigurationTemplate {

    public ProgramConfigurationDijkstra3States(
            int numberOfNodes,
            TreeMap<Integer, NodeStateDijkstra3States> nodeStateMap,
            int cvf,
            int probeLimit){

//        super(); // empty content
//
//        TreeMap<Integer, NodeStateTemplate> templateTreeMap = new TreeMap();
//
//        for(int nodeId = 0; nodeId < numberOfNodes; nodeId ++){
//            NodeStateTemplate nodeState = (NodeStateTemplate) nodeStateMap.get(nodeId);
//            templateTreeMap.put(nodeId, nodeState);
//        }
//
//        setNodeStateMap(templateTreeMap);
//        setNumberOfNodes(numberOfNodes);

        super(numberOfNodes, nodeStateMap, cvf, probeLimit);

    }

//    /**
//     * Create a program configuration similar to this configuration
//     * except that the node at specified position is replaced by new state
//     * @param nodeId  position of node we want to replace state
//     * @param newStateForNodeId new state for the node at specified position
//     * @return the newly created program configuration
//     */
//    public ProgramConfigurationDijkstra3States copyConfigurationAndReplaceState(
//            int nodeId,
//            NodeStateDijkstra3States newStateForNodeId){
//
//        ProgramConfigurationDijkstra3States copy = this.getDeepCopy();
//        copy.updateStateForANode(nodeId, newStateForNodeId);
//
//        return copy;
//    }

    /**
     * Deep copy: create a program configuration similar to this configuration
     * @return a deep copy of a program configuration
     */
    public ProgramConfigurationDijkstra3States getDeepCopy(){
        TreeMap<Integer, NodeStateDijkstra3States> currentMap = getNodeStateMap();
        TreeMap<Integer, NodeStateDijkstra3States> newNodeStateMap = new TreeMap<>();

        for(Map.Entry<Integer,NodeStateDijkstra3States> entry: currentMap.entrySet()){
            newNodeStateMap.put(entry.getKey(), entry.getValue().getDeepCopy());
        }

        return new ProgramConfigurationDijkstra3States(getNumberOfNodes(), newNodeStateMap, getCvf(), getProbeLimit());
    }

    /*
        Get values and positions of a node and its left and right neighbors
     */
    int getNodeValue(int nodePosition){
        return ((NodeStateDijkstra3States) getNodeStateMap().get(nodePosition)).getNodeStateValue();
    }
    int getRightHandNeighborPosition(int nodePosition){
        return (nodePosition+1) % getNumberOfNodes();
    }
    int getRightHandNeighborValue(int nodePosition){
        int rightNbrNodeId = getRightHandNeighborPosition(nodePosition);
        return ((NodeStateDijkstra3States) getNodeStateMap().get(rightNbrNodeId)).getNodeStateValue();
    }
    int getLeftHandNeighborPosition(int nodePosition){
        return (nodePosition - 1 + getNumberOfNodes()) % getNumberOfNodes();
    }
    int getLeftHandNeighborValue(int nodePosition){
        int leftNbrNodeId = getLeftHandNeighborPosition(nodePosition);
        return ((NodeStateDijkstra3States) getNodeStateMap().get(leftNbrNodeId)).getNodeStateValue();
    }

    // implement Comparable interface
    public int compareTo(Object o){
        ProgramConfigurationDijkstra3States other = (ProgramConfigurationDijkstra3States) o;

        if(this.getNumberOfNodes() > other.getNumberOfNodes())
            return 1;
        if(this.getNumberOfNodes() < other.getNumberOfNodes())
            return -1;

        // same number of nodes
        TreeMap<Integer, NodeStateDijkstra3States> myNodeStateMap = getNodeStateMap();
        TreeMap<Integer, NodeStateDijkstra3States> otherNodeStateMap = other.getNodeStateMap();
        for(int nodeId : myNodeStateMap.keySet()){
            int compareResult = myNodeStateMap.get(nodeId).compareTo(otherNodeStateMap.get(nodeId));
            if(compareResult < 0)
                return -1;
            if(compareResult > 0)
                return 1;
        }

        return 0;
    }

    public long getSizeOfStateSpace(){
        return (long) Math.pow(3, getNumberOfNodes());
    }

    /**
     * Implement super class abstract method
     */
    public NodeActionEvaluationResults evaluateANodeActions(int nodeId){
        TreeSet<SuccessorInfo> succListOfNodeId = new TreeSet();
        int privilegesCount = 0;

        // right hand neighbor
        int R = getRightHandNeighborValue(nodeId);
        // left hand neighbor
        int L = getLeftHandNeighborValue(nodeId);

        // current node state
        int S = getNodeValue(nodeId);
        int newValueForNodeId;

//        System.out.println("    getSuccesorListFromANodeAction():");
//        System.out.println("      nodeId = " + nodeId + " S = " + S + " R = " + R + " L = " + L);

        if(nodeId == 0){ // bottom node -- decreases if right nbr exceeds you 1
            //   if S+1 mod 3 == R then
            //      S = (S-1) mode 3
            //   fi
            if (((S + 1) % DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES) == R){
                newValueForNodeId = (S - 1 + DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES) % DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES;

                succListOfNodeId.add(
                        new SuccessorInfo(nodeId,
                            copyConfigurationAndReplaceState(
                                nodeId,
                                new NodeStateDijkstra3States(nodeId, newValueForNodeId))));
                privilegesCount ++;
            }else{
                // no action, no successor
            }

            return new NodeActionEvaluationResults(nodeId, privilegesCount, succListOfNodeId);
        }

        if(nodeId == getNumberOfNodes() - 1){
            // top node -- exceed left nbr if left nbr equals bottom
            // and you do not exceed left nbr by 1
            // if L = R and (L+1)mod 3 != S then
            //      S = (L+1)mod 3
            // fi
            if((L == R) &&
                    (((L+1) % DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES) != S)){
                newValueForNodeId = (L + 1) % DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES;

                succListOfNodeId.add(
                        new SuccessorInfo(nodeId,
                                copyConfigurationAndReplaceState(
                                        nodeId,
                                        new NodeStateDijkstra3States(nodeId, newValueForNodeId))));
                privilegesCount ++;

            }else{
                // no action, no successor

            }

            return new NodeActionEvaluationResults(nodeId, privilegesCount, succListOfNodeId);

        }

        // other nodes: copy from right or left nbr who is 1 more than you
        // if (S+1)mod 3 = L then
        //      S := L
        // fi
        if((S + 1) % DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES == L){
            newValueForNodeId = L;

            succListOfNodeId.add(
                    new SuccessorInfo(nodeId,
                        copyConfigurationAndReplaceState(
                            nodeId,
                            new NodeStateDijkstra3States(nodeId, newValueForNodeId))));
            privilegesCount ++;
        }

        // if (S+1)mod 3 = R then
        //      S := R
        // fi
        if((S + 1) % DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES == R){
            newValueForNodeId = R;

            succListOfNodeId.add(
                    new SuccessorInfo(nodeId,
                        copyConfigurationAndReplaceState(
                            nodeId,
                            new NodeStateDijkstra3States(nodeId, newValueForNodeId))));
            privilegesCount ++;
        }

        return new NodeActionEvaluationResults(nodeId, privilegesCount, succListOfNodeId);

    }

    /**
     *  Implement abstract method of superclass
     */
    public NodePerturbationResults perturbANodeArbitrarily(int nodeId){
        TreeSet<SuccessorInfo> perturbedSuccessorListOfNodeId = new TreeSet<>();

        int currentValueForNodeId = getNodeValue(nodeId);

        // note: offset starts from 1 if we exclude the current program configuration
        //                     from 0 if we include it
        for(int offset = 1; offset < DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES; offset ++){
            int newValueForNodeId = (currentValueForNodeId + offset) % DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES;
            perturbedSuccessorListOfNodeId.add(
                    new SuccessorInfo(nodeId,
                            copyConfigurationAndReplaceState(
                                nodeId,
                                new NodeStateDijkstra3States(nodeId, newValueForNodeId))));
        }

        return new NodePerturbationResults(perturbedSuccessorListOfNodeId);
    }


    /**
     *  Implement abstract method of superclass
     */
    public NodePerturbationResults perturbANodeWithContraint(int nodeId) {
        // Old way to compute upperbound which may be too loose
        // return perturbANodeArbitrarily(nodeId);

        // More accurate way to compute tighter upper bound
        TreeSet<SuccessorInfo> constrainedPerturbedSuccessorListOfNodeId = new TreeSet<>();

        int currentValueForNodeId = getNodeValue(nodeId);
        int newValueForNodeId;

        // depending on the node position, the possible perturbation differ

        if (nodeId == 0) {
            // bottom node.
            // By program algorithm, its state can only decrease
            newValueForNodeId = (currentValueForNodeId - 1 + DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES)
                                    % DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES;

            constrainedPerturbedSuccessorListOfNodeId.add(
                    new SuccessorInfo(nodeId,
                            copyConfigurationAndReplaceState(
                                nodeId,
                                new NodeStateDijkstra3States(nodeId, newValueForNodeId))));

            return new NodePerturbationResults(constrainedPerturbedSuccessorListOfNodeId);
        }

        if(nodeId == getNumberOfNodes() - 1){
            // top node.
            // By program algorithm, cvf can perturb its state to any value. Original state is excluded.
            for(int offset = 1; offset < DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES; offset ++){
                newValueForNodeId = (currentValueForNodeId + offset) % DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES;
                constrainedPerturbedSuccessorListOfNodeId.add(
                        new SuccessorInfo(nodeId,
                                copyConfigurationAndReplaceState(
                                    nodeId,
                                    new NodeStateDijkstra3States(nodeId, newValueForNodeId))));
            }

            return new NodePerturbationResults(constrainedPerturbedSuccessorListOfNodeId);
        }

        // other nodes.
        // By program algorithm, its state can only increase
        newValueForNodeId = (currentValueForNodeId + 1) % DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES;
        constrainedPerturbedSuccessorListOfNodeId.add(
                new SuccessorInfo(nodeId,
                        copyConfigurationAndReplaceState(
                            nodeId,
                            new NodeStateDijkstra3States(nodeId, newValueForNodeId))));

        return new NodePerturbationResults(constrainedPerturbedSuccessorListOfNodeId);

    }

    // Topology specifics are only relevant to other problem but not Dijkstra
    // since the topology in Dijkstra is implicit.
    // Thus this function is not different from perturbANodeWithContraint
    public NodePerturbationResults perturbANodeWithContraintAndTopologyRestriction(int nodeId){
        return perturbANodeWithContraint(nodeId);
    }

    /**
     * Make all nodes have the same value
     * @param newValue same value to be associated with all nodes
     */
    public void setStateForAllNodes(int newValue){
        for(int nodeId = 0; nodeId < getNumberOfNodes(); nodeId ++){
            updateStateForANode(nodeId, newValue);
        }
    }

    /**
     * Associate or replace state of a node at specified position
     * @param nodeId  position of node of interest
     * @param newValue new value for that node
     */
    public void updateStateForANode(int nodeId, int newValue){
        TreeMap<Integer, NodeStateDijkstra3States> tm = getNodeStateMap();
        NodeStateDijkstra3States nodeState = tm.get(nodeId);

        if(nodeState == null){
            tm.put(nodeId, new NodeStateDijkstra3States(nodeId, newValue));
        }else{
            nodeState.setNodeStateValue(newValue);
        }
    }

//    This method has been moved to the superclass
//    /**
//     * Associate or replace state of a node at specified position
//     * @param nodeId  position of node of interest
//     * @param stateForNodeId new state for that node
//     */
//    public void updateStateForANode(int nodeId, NodeStateDijkstra3States stateForNodeId){
//        getNodeStateMap().put(nodeId, stateForNodeId);
//    }

    /**
     * @return true if the current state is inside the invariant (i.e. it is a legitimate state)
     *         false otherwise
     */
    public boolean isInsideInvariant(){
//        // Note: the approach of using number of enabled nodes == 1 is wrong
//        // since one enabled nodes could corresponding to two privileges,
//        // e.g. 101 has second node enabled with two transitions. Although the successor of
//        // both transitions are the same (111), it is not in the invariant
//        return getNumberOfEnabledNodes() == 1;


        // we have to count the number of privileges whose guards are true
        return getNumberOfPrivileges() == 1;
    }


//    int getNumberOfEnabledNodes(){
//        int enabledNodeCount = 0;
//        for(int nodeId = 0; nodeId < getNumberOfNodes(); nodeId ++){
//            if(isThisNodeEnabled(nodeId)) {
//                enabledNodeCount ++;
//            }
//        }
//
//        return enabledNodeCount;
//    }

//    boolean isThisNodeEnabled(int nodeId){
//        HashSet<ProgramConfiguration> successors = evaluateANodeActions(nodeId).getSuccessorList();
//
//        if(successors.isEmpty()) {
//            return false;
//        }
//        else {
//            return true;
//        }
//    }

    /**
     * Change the current program config to the first program configuration in the lexicographically enumeration scheme.
     * i.e. all nodes are 0's
     * @return this object
     */
    public ProgramConfigurationDijkstra3States moveToFirstProgramConfig(){
        this.setStateForAllNodes(DIJKSTRA_3_STATES_NODE_STATE_0);

        return this;
    }

    /**
     * Change the current program config to the last program configuration in the lexicographically enumeration scheme.
     * i.e. all nodes are 2's
     * @return this object
     */
    public ProgramConfigurationDijkstra3States moveToLastProgramConfig(){
        this.setStateForAllNodes(DIJKSTRA_3_STATES_NODE_STATE_2);

        return this;
    }

    /**
     * Move to the next configuration which is lexicographically succeeding the current configuration
     * If current state is the last configuration (all 2's) then the next one is all 0's
     * @return this object
     */
    public ProgramConfigurationDijkstra3States moveToNextProgramConfig(){
        // start from last nodeId: find the first node whose state is not 2
        // if found
        //    increase that state
        // else current state is the last state, change to the first state (all 2's become 0's)

        int incrementPosition = getNumberOfNodes() - 1;
        int incrementPositionValue = 0;
        while(incrementPosition >= 0){
            incrementPositionValue = getNodeValue(incrementPosition);
            if(incrementPositionValue != DIJKSTRA_3_STATES_NODE_STATE_2)
                break;
            else
                incrementPosition --;
        }
        if(incrementPosition >= 0){
            // found a position which is not 2
            // increment this position
            updateStateForANode(incrementPosition, incrementPositionValue + 1);
            // mark all position after it 0
            for(int i = incrementPosition + 1; i < getNumberOfNodes(); i ++){
                updateStateForANode(i, DIJKSTRA_3_STATES_NODE_STATE_0);
            }
        }else{
            // not found, all are 2's
            setStateForAllNodes(DIJKSTRA_3_STATES_NODE_STATE_0);
        }

        return this;
    }

    @Override
    public ProgramConfigurationDijkstra3States moveToNthProgramConfig(long n){
        int states[] = {
                DIJKSTRA_3_STATES_NODE_STATE_0,
                DIJKSTRA_3_STATES_NODE_STATE_1,
                DIJKSTRA_3_STATES_NODE_STATE_2};

        this.moveToFirstProgramConfig();

        int nodePosition = getNumberOfNodes() - 1;
        while(n > 0){
            int digit = (int) (n % DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES);
            this.updateStateForANode(nodePosition, states[digit]);
            nodePosition --;
            n = n/DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES;
        }

        return this;
    }

}

