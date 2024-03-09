package maxmatching;

import datastructure.NodeActionEvaluationResults;
import datastructure.NodePerturbationResults;
import template.ProgramConfigurationTemplate;
import template.SuccessorInfo;

import java.util.*;

import static main.AnalyzeCvfs.graphTopology;
import static main.Utility.getBooleanFromInt;
import static maxmatching.NodeStateMaxMatching.*;

/**
 * created on 11/15/20 by duongnn
 */

public class ProgramConfigurationMaxMatching extends ProgramConfigurationTemplate {
    public ProgramConfigurationMaxMatching(
            int numberOfNodes,
            TreeMap<Integer, NodeStateMaxMatching> nodeStateMap,
            int cvf,
            int probeLimit) {
        super(numberOfNodes, nodeStateMap, cvf, probeLimit);
    }

    /***********************************************************************
     ***  Implement abstract method of superclass
     ************************************************************************/

    public ProgramConfigurationTemplate getDeepCopy(){
        TreeMap<Integer, NodeStateMaxMatching> currentMap = getNodeStateMap();
        TreeMap<Integer, NodeStateMaxMatching> newNodeStateMap = new TreeMap<>();

        for(Map.Entry<Integer,NodeStateMaxMatching> entry: currentMap.entrySet()){
            newNodeStateMap.put(entry.getKey(), entry.getValue().getDeepCopy());
        }

        return new ProgramConfigurationMaxMatching(getNumberOfNodes(), newNodeStateMap, getCvf(), getProbeLimit());
    }


    public long getSizeOfStateSpace(){
        if(cvf == CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION){
            return getSizeOfStateSpaceWithTopology();
        }else {
            // for each node
            //    2 options for mvalue
            //    n options for pvalue (-1 or other node in graph except itself), where n is number of nodes
            int numberOfValuesForIndividualNode = 2 * getNumberOfNodes();
            return (long) Math.pow(numberOfValuesForIndividualNode, getNumberOfNodes());
        }
    }

    // estimate size of state space with consideration of topology
    // It is expected to be more efficient than getSizeOfStateSpace, i.e. it gives smaller size
    public long getSizeOfStateSpaceWithTopology(){
        // for each node
        //    2 options for mvalue
        //    k+1 options for pvalue where k is the number of neighbors
        //    (+1 since it include MAX_MATCHING_PVALUE_NULL)
        long count = 1;
        for(int nodeId = 0; nodeId < getNumberOfNodes(); nodeId++){
            count = count * 2 * (graphTopology.get(nodeId).size() + 1);
        }
        return count;
    }

    public int getNodePvalue(int nodePosition){
        NodeStateMaxMatching nodestate = (NodeStateMaxMatching) getNodeStateMap().get(nodePosition);
        return nodestate.getPvalue();
    }

    public boolean getNodeMvalue(int nodePosition){
        NodeStateMaxMatching nodestate = (NodeStateMaxMatching) getNodeStateMap().get(nodePosition);
        return nodestate.getMvalue();
    }

    /**
     * Perturb arbitrarily the state of a specified node.
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by arbitrarily perturbing a node
     *         Current configuration is excluded in the results.
     */
    public NodePerturbationResults perturbANodeArbitrarily(int nodeId){
        TreeSet<SuccessorInfo> perturbedSuccessorList = new TreeSet<>();

        int currentPvalue = getNodePvalue(nodeId);
        boolean currentMvalue = getNodeMvalue(nodeId);

        // the perturbed state is different from the current state of the node
        for(int pvalue = -1; pvalue < getNumberOfNodes(); pvalue ++){
            if(pvalue == currentPvalue){
                // perturb mvalue
                boolean newMvalue = !currentMvalue;
                perturbedSuccessorList.add(
                        new SuccessorInfo(nodeId,
                                copyConfigurationAndReplaceState(
                                    nodeId,
                                    new NodeStateMaxMatching(nodeId, newMvalue, pvalue)
                        )
                ));
            }else{
                // consider both cases for mvalue
                perturbedSuccessorList.add(
                        new SuccessorInfo(nodeId,
                                copyConfigurationAndReplaceState(
                                    nodeId,
                                    new NodeStateMaxMatching(nodeId, MAX_MATCHING_MARRIED, pvalue)
                        )
                ));
                perturbedSuccessorList.add(
                        new SuccessorInfo(nodeId,
                                copyConfigurationAndReplaceState(
                                    nodeId,
                                    new NodeStateMaxMatching(nodeId, MAX_MATCHING_UNMARRIED, pvalue)
                        )
                ));
            }
        }

        return new NodePerturbationResults(perturbedSuccessorList);
    }

    /**
     * Perturb the state of a specified node but the perturbation is constrained by program transition rules
     * Note that if we consider graph topology restriction, the implementation would still the same.
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by arbitrarily perturbing a node
     *         Current configuration is excluded in the results.
     */
    public NodePerturbationResults perturbANodeWithConstraint(int nodeId){
        NodeStateMaxMatching nodeInfo = (NodeStateMaxMatching) getNodeStateMap().get(nodeId);
        int nodePvalue = nodeInfo.getPvalue();
        boolean nodeMvalue = nodeInfo.getMvalue();
        // neighbors of this node
        Vector<Integer> nodeNbr = graphTopology.get(nodeId);

        // List of possible successors obtained by program transition caused by reading stale neighbor information
        // More accurate way to compute tighter upper bound
        TreeSet<SuccessorInfo> constrainedPerturbedSuccessorList = new TreeSet<>();
        Vector<Boolean> perturbedPRMarried = evaluatePerturbedPRMarried(nodeId);

        for (Boolean aPRMarriedValue : perturbedPRMarried) {
            if (nodeMvalue != aPRMarriedValue) {
                // m-value and predicate PRMarried does not match
                // the only action/successor is updating m-value
                constrainedPerturbedSuccessorList.add(
                        new SuccessorInfo(nodeId,
                                copyConfigurationAndReplaceState(
                                    nodeId,
                                    new NodeStateMaxMatching(nodeId, aPRMarriedValue, nodePvalue))));
            } else {// m-value and PRMarried match
                if (nodePvalue == MAX_MATCHING_PVALUE_NULL) { // this implies m-value is false
                    // It is possible that, due to stale info, any of the neighbor points back to this node
                    for (int nbr : nodeNbr) {
                        constrainedPerturbedSuccessorList.add(
                                new SuccessorInfo(nodeId,
                                        copyConfigurationAndReplaceState(
                                            nodeId,
                                            new NodeStateMaxMatching(nodeId, aPRMarriedValue, nbr))));
                    }
                    // It is also possible that, due to stale info, none of the neighbor points back to this node
                    // then this node may seduce one of its neighbor
                    // However, the configurations resulted from the seduction action is surely a subset of
                    // the list of successor already added previously. Thus, we do not need to add them again.
                    // Of course, if we add, it also does no harm
                } else {
                    // when m-value and PRMarried match and p-value is not null
                    // the only possible action/successor is abandonment
                    // Abandonment is possible since the pointed neighbor does not point back and
                    //   p-value < nodeId
                    //   the stale value of the pointed neighbor could be
                    //      p = null, m = true, or
                    //      p = k != nodeId, m = true
                    constrainedPerturbedSuccessorList.add(
                            new SuccessorInfo(nodeId,
                                    copyConfigurationAndReplaceState(
                                        nodeId,
                                        new NodeStateMaxMatching(nodeId, aPRMarriedValue, MAX_MATCHING_PVALUE_NULL))));
                }
            }
        }

        return new NodePerturbationResults(constrainedPerturbedSuccessorList);
    }


    /**
     * Perturb the state of a specified node but the perturbation is constrained by program transition rules
     * Furthermore, we consider the topology restriction as well, e.g. p-value can only be one of the neighbors
     * Although it is expected to be more efficient than perturbANodeWithContraint,
     * for maximal matching, the implementation is the same
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by arbitrarily perturbing a node
     *         Current configuration is excluded in the results.
     */
    public NodePerturbationResults perturbANodeWithConstraintAndTopologyRestriction(int nodeId){
        return perturbANodeWithConstraint(nodeId);
    }

    /**
     * @return true if the current state is inside the invariant (i.e. it is a legitimate state)
     *         false otherwise
     */
    public boolean isInsideInvariant(){
        // In max-matching, a configuration is in the invariant if no node is enabled
        return (getNumberOfPrivileges() == 0);
    }

    /**
     * Change the current program config to the first program configuration in some enumeration scheme.
     * This scheme must be the same as the enumeration scheme used in getLastProgramConfig()
     * @return this object
     */
    public ProgramConfigurationMaxMatching moveToFirstProgramConfig(){
        // in max matching, the first program configuration is when for all nodes
        // the values of m and p is minimal, i.e.
        // m = false (0) and p = null (-1)
        setStateForAllNodes(0, MAX_MATCHING_PVALUE_NULL);
        return this;
    }

    /**
     * Change the current program config to the last program configuration in some enumeration scheme.
     * This scheme must be the same as the enumeration scheme used in getFirstProgramConfig()
     * @return this object
     */
    public ProgramConfigurationMaxMatching moveToLastProgramConfig(){
        // in max matching, the last program configuration is when for all nodes
        // the values of m and p is maximal
        if(cvf == CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION){
            // with topology restriction, a node state is maximal when
            // m = true (1) and p = max of neighbor's node id
            for(int nodeId = 0; nodeId < getNumberOfNodes(); nodeId ++){
                updateStateForANode(nodeId, 1, Collections.max(graphTopology.get(nodeId)));
            }
            return this;
        }else {
            // without topology restriction, a node state is maximal when
            // m = true (1) and p = number of node - 1
            setStateForAllNodes(1, getNumberOfNodes() - 1);
            return this;
        }
    }


    /**
     * Count how many privileges (enabled actions) at a node in the current program config, and
     * list of successor configurations if those actions are executed
     * @param nodeId nodeId of the interested node.
     * @return evaluation result of a node, which contains:
     *           number of enabled actions (privileges), and
     *           list of successor states if the actions at specified nodes are executed.
     *         If the node is not enabled, return empty list.
     *         Since action depends on specific program, this method should be implemented in subclass
     */
    public NodeActionEvaluationResults evaluateANodeActions(int nodeId){
        TreeSet<SuccessorInfo> listOfSuccessors = new TreeSet();
        int privilegesCount = 0;

        // local information of specified node
        NodeStateMaxMatching nodeInfo = (NodeStateMaxMatching) getNodeStateMap().get(nodeId);
        // list of its neighbor
        Vector<Integer> nodeNbr = graphTopology.get(nodeId);
        int nodePvalue = nodeInfo.getPvalue();
        boolean nodeMvalue = nodeInfo.getMvalue();
        boolean nodePRMarried = evaluatePRMarried(nodeId);

        // unlike Dijkstra 3 states program where there is top, bottom, and other nodes,
        // in max-matching, there is no difference between nodes.
        // Although max-matching program has hierarchical collateral composition,
        // it still has some non-determinism in action 2, i.e. when there are multiple proposals,
        // a node could choose any of them

        // check if m-value and PRMarried is consistent
        if(nodeMvalue != nodePRMarried){
            // action 1: update m-value
            listOfSuccessors.add(
                    new SuccessorInfo(nodeId,
                        copyConfigurationAndReplaceState(
                            nodeId,
                            new NodeStateMaxMatching(nodeId, nodePRMarried, nodePvalue))));
            privilegesCount ++;
        }else{ // m-value is consistent with PRMarried
            if(nodePvalue == MAX_MATCHING_PVALUE_NULL){
                int marriageActionCount = 0;
                int seductionCandidate = MAX_MATCHING_PVALUE_NULL;

                for(int nbr : nodeNbr){
                    NodeStateMaxMatching nbrInfo = (NodeStateMaxMatching) getNodeStateMap().get(nbr);
                    if(nbrInfo.getPvalue() == nodeId){
                        // action 2: marriage
                        listOfSuccessors.add(
                                new SuccessorInfo(nodeId,
                                        copyConfigurationAndReplaceState(
                                            nodeId,
                                            new NodeStateMaxMatching(nodeId, nodePRMarried, nbr))));
                        privilegesCount ++;

                        marriageActionCount ++;

                        // do not break the for loop now
                        // because we are calculating all possible successors/transitions

                    }else{
                        // check if could be a candidate for seduction
                        if((nbrInfo.getPvalue() == MAX_MATCHING_PVALUE_NULL) &&
                                //(nbr > nodeId) &&  // this is old code.
                                (nbr < nodeId) &&    // new code that is consistent with paper description
                                (!nbrInfo.getMvalue())){

                            // update seduction candidate if it is smaller
                            if(seductionCandidate < nbr){
                                seductionCandidate = nbr;
                            }
                        }
                    }
                }

                if(marriageActionCount != 0){
                    // marriage action is already been done
                    // nothing else is needed
                }else{
                    // no marriage action, then we may need to do seduction
                    if(seductionCandidate != MAX_MATCHING_PVALUE_NULL){
                        // action 3: seduction
                        listOfSuccessors.add(
                                new SuccessorInfo(nodeId,
                                        copyConfigurationAndReplaceState(
                                            nodeId,
                                            new NodeStateMaxMatching(nodeId, nodePRMarried, seductionCandidate))));
                        privilegesCount ++;
                    }else{
                        // no seduction, no marriage, just wait
                    }
                }
            }else{ // p-value is not null
                // check if partner is pointing back to you
                NodeStateMaxMatching partnerInfo = (NodeStateMaxMatching) getNodeStateMap().get(nodePvalue);
                if((partnerInfo.getPvalue() != nodeId) &&   // partner is not pointing back
                        //(partnerInfo.getMvalue() || (nodePvalue < nodeId))){  // old code
                        (partnerInfo.getMvalue() || (nodePvalue > nodeId))){   // new code that is consistent with paper description
                    // action 4: abandonment
                    listOfSuccessors.add(
                            new SuccessorInfo(nodeId,
                                    copyConfigurationAndReplaceState(
                                        nodeId,
                                        new NodeStateMaxMatching(nodeId, nodePRMarried, MAX_MATCHING_PVALUE_NULL))));
                    privilegesCount ++;
                }
            }
        }

        return new NodeActionEvaluationResults(nodeId, privilegesCount, listOfSuccessors);
    }


    /***********************************************************************
     *** Overriding methods
     ************************************************************************/

    // implement Comparable interface
    public int compareTo(Object o){
        ProgramConfigurationMaxMatching other = (ProgramConfigurationMaxMatching) o;

        if(this.getNumberOfNodes() > other.getNumberOfNodes())
            return 1;
        if(this.getNumberOfNodes() < other.getNumberOfNodes())
            return -1;

        // same number of nodes
        TreeMap<Integer, NodeStateMaxMatching> myNodeStateMap = getNodeStateMap();
        TreeMap<Integer, NodeStateMaxMatching> otherNodeStateMap = other.getNodeStateMap();
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
    public ProgramConfigurationMaxMatching moveToNextProgramConfig(){
        // find the first node from right to left (i.e. from nodeId = n-1 to nodeId = 0)
        // where the node state is not maximal
        int incrementPosition = getNumberOfNodes() - 1;
        NodeStateMaxMatching nodeInfo = null;
        while(incrementPosition >= 0){
            nodeInfo = (NodeStateMaxMatching) getNodeStateMap().get(incrementPosition);
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
                NodeStateMaxMatching prevNodeInfo = (NodeStateMaxMatching) getNodeStateMap().get(prevPosition);
                updateStateForANode(prevPosition, prevNodeInfo.moveToMinimalState());
            }
        }else{
            // all positions/nodes have maximal state
            // rewind all
            moveToFirstProgramConfig();
        }

        return this;
    }

    /***********************************************************************
     *** Additional functions that are specific to the max matching program
    ************************************************************************/

    /** evaluate the value of predicate PRMarried
     * @param nodeId id of node of interest
     * @return value of predicate PRMarried (defined in the paper)
     */
    boolean evaluatePRMarried(int nodeId){
        // local information of specified node
        NodeStateMaxMatching nodeInfo = (NodeStateMaxMatching) getNodeStateMap().get(nodeId);
        // list of its neighbor
        Vector<Integer> nodeNbr = graphTopology.get(nodeId);
        int nodePvalue = nodeInfo.getPvalue();

        // if not pointing to some other node, it is not married
        if(nodePvalue == MAX_MATCHING_PVALUE_NULL)
            return false;

        // if pointing to a non-neighbor node, that marriage is not valid
        if(!nodeNbr.contains(nodePvalue)) {
            return false;
        }else { // pointing to a neighbor
            int myFianceeId = nodePvalue;
            NodeStateMaxMatching fianceeInfo = (NodeStateMaxMatching) getNodeStateMap().get(myFianceeId);

            // if that neighbor (fiancee) is not pointing back to you, not valid marriage
            if(fianceeInfo.getPvalue() != nodeId){
                return false;
            }else{
                // both nodes pointing to each other
                return true;
            }
        }
    }

    /**
     * Evaluate the value of predicate PRMarried under the condition that the state
     * of the neighbors could be perturbed
     * This function seems not depend on whether topology is considered or not
     * @param nodeId id of node of interest
     * @return list of possible values of predicate PRMarried
     */
    Vector<Boolean> evaluatePerturbedPRMarried(int nodeId){
        // local information of specified node
        NodeStateMaxMatching nodeInfo = (NodeStateMaxMatching) getNodeStateMap().get(nodeId);
        // list of its neighbor
        Vector<Integer> nodeNbr = graphTopology.get(nodeId);
        int nodePvalue = nodeInfo.getPvalue();

        Vector<Boolean> results = new Vector<>(2);

        // if not pointing to some other node, it is not married
        if(nodePvalue == MAX_MATCHING_PVALUE_NULL) {
            results.addElement(Boolean.FALSE);
            return results;
        }

        // if pointing to a non-neighbor node, that marriage is not valid
        if(!nodeNbr.contains(nodePvalue)) {
            results.addElement(Boolean.FALSE);
            return results;
        }else { // pointing to a neighbor
            // the value of PRMarried now depends on the p-value of the pointed neighbor
            // Since this p-value could be a stale value, we assume it could be this node or some other node
            // Thus the value of PRMarried could be true or false
            results.addElement(Boolean.FALSE);
            results.addElement(Boolean.TRUE);

            return results;
        }
    }

    void updateStateForANode(int nodeId, boolean mValue, int pValue){
        TreeMap<Integer, NodeStateMaxMatching> tm = getNodeStateMap();
        NodeStateMaxMatching nodeInfo = tm.get(nodeId);

        if(nodeInfo == null){
            tm.put(nodeId, new NodeStateMaxMatching(nodeId, mValue, pValue));
        }else{
            nodeInfo.setMvalue(mValue);
            nodeInfo.setPvalue(pValue);
        }
    }

    void updateStateForANode(int nodeId, int mValue, int pValue){
        updateStateForANode(nodeId, getBooleanFromInt(mValue), pValue);
    }

    void setStateForAllNodes(int mValue, int pValue){
        for(int i = 0; i < getNumberOfNodes(); i ++){
            updateStateForANode(i, mValue, pValue);
        }
    }

}
