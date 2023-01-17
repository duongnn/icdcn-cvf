package newdijkstra;

import datastructure.NodeActionEvaluationResults;
import datastructure.NodePerturbationResults;
import template.ProgramConfigurationTemplate;
import template.SuccessorInfo;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static newdijkstra.NodeStateNewDijkstra.NEW_DIJKSTRA_NUMBER_OF_NODE_STATES;

public class ProgramConfigurationNewDijkstra extends ProgramConfigurationTemplate {
    public ProgramConfigurationNewDijkstra(int numberOfNodes, TreeMap<Integer, NodeStateNewDijkstra> nodeStateMap, int cvf, int probeLimit){
        super(numberOfNodes, nodeStateMap, cvf, probeLimit);
    }

    // super class abstract methods
    @Override
    public ProgramConfigurationNewDijkstra getDeepCopy(){
        // generate new nodProgramConfigurationNewDijkstrae map
        TreeMap<Integer, NodeStateNewDijkstra> currentMap = this.getNodeStateMap();
        TreeMap<Integer, NodeStateNewDijkstra> newTreeMap = new TreeMap<>();
        for(Map.Entry<Integer, NodeStateNewDijkstra> entry : currentMap.entrySet()){
            NodeStateNewDijkstra nodeState = (NodeStateNewDijkstra) entry.getValue().getDeepCopy();
            Integer nodeId = new Integer(entry.getKey());
            newTreeMap.put(nodeId, nodeState);
        }
        return new ProgramConfigurationNewDijkstra(
                getNumberOfNodes(),
                newTreeMap,
                getCvf(),
                getProbeLimit()
        );
    }

    @Override
    public long getSizeOfStateSpace(){
        // there are three possible values (0, 1, 2) for each node.
        // the number of states will be 3^{numberOfNodes}
        return (long) Math.pow(3, getNumberOfNodes());
    }

    // change state of a node
    // assuming the value of nodeId inside the second param is equal to the first param
    private void updateStateForANode(int nodeId, NodeStateNewDijkstra nodeState){
        TreeMap<Integer, NodeStateNewDijkstra> currentMap = this.getNodeStateMap();

        // this way is simple but not very efficient as it allocates new memory space
        // currentMap.put(nodeId, nodeState.getDeepCopy());

        // another way is to modify the value inside the state of the interested node
        NodeStateNewDijkstra ns = currentMap.get(nodeId);

        if(ns == null){
            currentMap.put(nodeId, nodeState.getDeepCopy());
        }else{
            ns.setNodeValue(nodeState.getNodeValue());
        }

    }

    private void setStateForAllNodes(NodeStateNewDijkstra nodeState){
        for(int nodeId = 0; nodeId < getNumberOfNodes(); nodeId ++){
            // since updateStateForANode assume the value of nodeId in nodeState is equal to the first param
            // we need to update nodeId in nodeState first
            nodeState.setNodeId(nodeId);
            updateStateForANode(nodeId, nodeState);
        }
    }

    @Override
    // first program configuration is the one where every node has the state DIJKSTRA_3_STATES_NODE_STATE_0
    public ProgramConfigurationNewDijkstra moveToFirstProgramConfig(){
        NodeStateNewDijkstra newState = new NodeStateNewDijkstra(0, NodeStateNewDijkstra.NEW_DIJKSTRA_NODE_STATE_0);
        setStateForAllNodes(newState);
        return this;
    }

    @Override
    // last program configuration is the one where every node has the state DIJKSTRA_3_STATES_NODE_STATE_2
    public ProgramConfigurationNewDijkstra moveToLastProgramConfig(){
        NodeStateNewDijkstra newState = new NodeStateNewDijkstra(0, NodeStateNewDijkstra.NEW_DIJKSTRA_NODE_STATE_2);
        setStateForAllNodes(newState);
        return this;
    }

    @Override
    // next program config is the one after the current configure in lexicological order
    // e.g. if the current one is 1-0-2-1
    //      the next one is       1-0-2-2
    //      then                  1-1-0-0
    // after 2-2-2-2 is 0-0-0-0
    public ProgramConfigurationNewDijkstra moveToNextProgramConfig(){
        // going from last node in the ring (with highest nodeId), find the first one whose state is not 2
        for(int nodeId = getNumberOfNodes() - 1; nodeId >= 0; nodeId --){
            NodeStateNewDijkstra currentNode = (NodeStateNewDijkstra) getNodeStateMap().get(nodeId);
            int currentNodeValue = currentNode.getNodeValue();
            if(currentNodeValue == NodeStateNewDijkstra.NEW_DIJKSTRA_NODE_STATE_2){
                // change it to 0
               currentNode.setNodeValue(NodeStateNewDijkstra.NEW_DIJKSTRA_NODE_STATE_0);
               continue;
            }else{
                // increase nodeValue
                currentNode.setNodeValue(currentNodeValue + 1);
                // and stop
                break;
            }
        }
        return this;
    }

    @Override
    // Move to the nth program configurations.
    // This is similar to convert number N from base 10 to base 3 by keep dividing the
    // dividend by 3 until the dividend is 0.
    // The remainders obtained through the division process will be the base-3 representation in reverse order.
    public ProgramConfigurationNewDijkstra moveToNthProgramConfig(long n) {
        int digitPosition = getNumberOfNodes() - 1;
        while (n > 0) {
            // divide n by 3
            int digitValue = (int) n % 3;
            n = n / 3;
            updateStateForANode(digitPosition, new NodeStateNewDijkstra(digitPosition, digitValue));
            digitPosition--;
        }
        // all other positions will have value 0
        while (digitPosition >= 0) {
            updateStateForANode(digitPosition, new NodeStateNewDijkstra(digitPosition, 0));
            digitPosition--;
        }
        return this;
    }

    public int getNodeValue(int nodeId){
        NodeStateNewDijkstra currentNode = (NodeStateNewDijkstra) getNodeStateMap().get(nodeId);
        return currentNode.getNodeValue();
    }

    @Override
    /**
     * In Dijkstra 3 states program, a configuration is legitimate (inside the invariant) when the number of privileges is exactly 1
     */
    public boolean isInsideInvariant() {
        return getNumberOfPrivileges() == 1;
    }

    public int compareTo(Object o){
        if (this == o)
            return 0;
        // assume o is not null
        ProgramConfigurationNewDijkstra other = (ProgramConfigurationNewDijkstra) o;

        // we can define whatever way of comparing two ProgramConfigurationNewDijkstra objects suitable for our purpose
        // for example, we can compare the x values of nodes in lexicological order
        for(int nodeId = 0; nodeId < getNumberOfNodes(); nodeId++){
            int myValue = getNodeValue(nodeId);
            int otherValue = other.getNodeValue(nodeId);
            if(myValue < otherValue)
                return -1;
            if(myValue > otherValue)
                return 1;
        }
        // all nodes have same x values
        return 0;
    }

    @Override
    /**
     * Count how many privileges (enabled actions) at a node in the current program config, and
     * list of successor configurations if those actions are executed
     * @param nodeId nodeId of the interested node.
     * @return evaluation result of a node, which contains:
     *           number of enabled actions (privileges), and
     *           list of successor states if the actions at specified nodes are executed.
     *              this list could be obtained by simulate execution of each privilege
     *              since two privileges could result in one successor, we should be careful to avoid duplicate successors
     *         If the node is not enabled, return empty list.
     */
    public NodeActionEvaluationResults evaluateANodeActions(int nodeId){
        // The result of this function includes a list of successor
        // To avoid duplicate, we use TreeSet
        TreeSet<SuccessorInfo> successlorList = new TreeSet<>();
        int numberOfPrivileges = 0;
        /*
        The algorithm of Dijkstra 3-state program is as follows:
            S: is x value of current node
            L: is x value of left neighbor
            R: is x value of right neighbor

            if current node is bottom node (nodeId == 0)
                if (S+1) mod 3 == R then
                    S = (S-1) mod 3
            if current node is top node (nodeId == n-1)
                if L == R and (L+1) mod 3 != S then
                    S = (L+1) mod 3
            other nodes
                if (S+1) mod 3 == L
                    S = L
                if (S+1) mod 3 == R
                    S = R
         */
        //first we get the nodeValue at the current node and its left and right neighbors
        int S = getNodeValue(nodeId);
        int newS;
        int rightNbrNodeId = (nodeId + 1) % getNumberOfNodes();
        int R = getNodeValue(rightNbrNodeId);
        int leftNbrNodeId = (nodeId - 1 + getNumberOfNodes()) % getNumberOfNodes();
        int L = getNodeValue(leftNbrNodeId);

        // then evaluate actions based on the described algorithm
        if(nodeId == 0){
            // bottom
            if((S + 1) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES == R){
                // this privilege is present
                numberOfPrivileges ++;
                // we add DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES to avoid negative value
                newS = (S - 1 + NEW_DIJKSTRA_NUMBER_OF_NODE_STATES) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES;
                // create a new configuration which is the successor of the current configuration
                // i.e. by executing this privilege, the program move from current configuration to the successor configuration
                ProgramConfigurationNewDijkstra successorConfiguration = this.getDeepCopy();
                successorConfiguration.updateStateForANode(nodeId, new NodeStateNewDijkstra(nodeId, newS));
                SuccessorInfo successor = new SuccessorInfo(nodeId, successorConfiguration);
                successlorList.add(successor);
            }

            return new NodeActionEvaluationResults(nodeId, numberOfPrivileges, successlorList);

        }else if (nodeId == getNumberOfNodes() - 1){
            // top
            if (L == R && ((L + 1) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES != S)){
                newS = (L + 1) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES;
                numberOfPrivileges ++;
                ProgramConfigurationNewDijkstra successorConfiguration = this.getDeepCopy();
                successorConfiguration.updateStateForANode(nodeId, new NodeStateNewDijkstra(nodeId, newS));
                SuccessorInfo successor = new SuccessorInfo(nodeId, successorConfiguration);
                successlorList.add(successor);
            }

            return new NodeActionEvaluationResults(nodeId, numberOfPrivileges, successlorList);

        }else{
            // other
            // note that we could have two privileges present in this case
            if( (S+1) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES == L){
                numberOfPrivileges ++;
                newS = L;
                ProgramConfigurationNewDijkstra successorConfiguration = this.getDeepCopy();
                successorConfiguration.updateStateForANode(nodeId, new NodeStateNewDijkstra(nodeId, newS));
                SuccessorInfo successor = new SuccessorInfo(nodeId, successorConfiguration);
                successlorList.add(successor);
            }
            if((S+1) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES == R){
                newS = R;
                numberOfPrivileges ++;
                ProgramConfigurationNewDijkstra successorConfiguration = this.getDeepCopy();
                successorConfiguration.updateStateForANode(nodeId, new NodeStateNewDijkstra(nodeId, newS));
                SuccessorInfo successor = new SuccessorInfo(nodeId, successorConfiguration);
                successlorList.add(successor);
            }

            return new NodeActionEvaluationResults(nodeId, numberOfPrivileges, successlorList);

        }

    }

    @Override
    /**
     * Perturb arbitrarily the state of a specified node.
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by arbitrarily perturbing a node
     *         Current configuration is excluded in the results.
     */
    public NodePerturbationResults perturbANodeArbitrarily(int nodeId){
        TreeSet<SuccessorInfo> successorList = new TreeSet<>();

        for(int newNodeValue = 0; newNodeValue < NEW_DIJKSTRA_NUMBER_OF_NODE_STATES; newNodeValue ++){
            // new only consider new state for the node
            if(newNodeValue != getNodeValue(nodeId)){
                NodeStateNewDijkstra nodeNewState = new NodeStateNewDijkstra(nodeId, newNodeValue);
                ProgramConfigurationNewDijkstra newConfig = (ProgramConfigurationNewDijkstra) this.copyConfigurationAndReplaceState(nodeId, nodeNewState);
                SuccessorInfo successor = new SuccessorInfo(nodeId, newConfig);
                successorList.add(successor);
            }
        }
        return new NodePerturbationResults(successorList);
    }

    @Override
    public NodePerturbationResults perturbANodeWithConstraint(int nodeId){
        TreeSet<SuccessorInfo> successorList = new TreeSet<>();
        /*
        The algorithm of Dijkstra 3-state program is as follows:
            S: is x value of current node
            L: is x value of left neighbor
            R: is x value of right neighbor
            if current node is bottom node (nodeId == 0)
                if (S+1) mod 3 == R then
                    S = (S-1) mod 3
            if current node is top node (nodeId == n-1)
                if L == R and (L+1) mod 3 != S then
                    S = (L+1) mod 3
            other nodes
                if (S+1) mod 3 == L
                    S = L
                if (S+1) mod 3 == R
                    S = R
         */
        if(nodeId == 0){
            // bottom node, x value can only be decreased
            int newNodeValue = (getNodeValue(nodeId) - 1 + NEW_DIJKSTRA_NUMBER_OF_NODE_STATES) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES;
            NodeStateNewDijkstra nodeNewState = new NodeStateNewDijkstra(nodeId, newNodeValue);
            ProgramConfigurationNewDijkstra newConfig = (ProgramConfigurationNewDijkstra) this.copyConfigurationAndReplaceState(nodeId, nodeNewState);
            SuccessorInfo successor = new SuccessorInfo(nodeId, newConfig);
            successorList.add(successor);
        }else if(nodeId == getNumberOfNodes() - 1){
            // top node, x value can only be left neighbor plus one.
            // since left neighbor can be any value, perturbed x value can be any value except the current value
            for(int newNodeValue = 0; newNodeValue < NEW_DIJKSTRA_NUMBER_OF_NODE_STATES; newNodeValue ++){
                if(newNodeValue != getNodeValue(nodeId)){
                    NodeStateNewDijkstra nodeNewState = new NodeStateNewDijkstra(nodeId, newNodeValue);
                    ProgramConfigurationNewDijkstra newConfig = (ProgramConfigurationNewDijkstra) this.copyConfigurationAndReplaceState(nodeId, nodeNewState);
                    SuccessorInfo successor = new SuccessorInfo(nodeId, newConfig);
                    successorList.add(successor);
                }
            }
        }else{
            // other node: value can only increase
            int newNodeValue = (getNodeValue(nodeId) + 1)% NEW_DIJKSTRA_NUMBER_OF_NODE_STATES;
            NodeStateNewDijkstra nodeNewState = new NodeStateNewDijkstra(nodeId, newNodeValue);
            ProgramConfigurationNewDijkstra newConfig = (ProgramConfigurationNewDijkstra) this.copyConfigurationAndReplaceState(nodeId, nodeNewState);
            SuccessorInfo successor = new SuccessorInfo(nodeId, newConfig);
            successorList.add(successor);
        }

        return new NodePerturbationResults(successorList);
    }

    public NodePerturbationResults perturbANodeWithConstraintAndTopologyRestriction(int nodeId){
        // in Dijkstra' program, there is no difference between this function and perturbANodeWithConstraint
        return perturbANodeWithConstraint(nodeId);
    }
}
