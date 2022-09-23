package template;

import datastructure.DebugInfo;
import datastructure.NodeActionEvaluationResults;
import datastructure.NodePerturbationResults;
import datastructure.RankEvaluationResults;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static datastructure.DebugInfo.*;
import static main.Utility.getRandomElementFromTreeSet;
import static main.Utility.getRandomElementFromSuccessorTreeSet;
import static main.Utility.updateRankCache;

/**
 * Abstract class (template) for a program state/configuration.
 * A distributed program consists of a set of nodes and communication channels between them.
 * A program state/configuration is a value assignment to each node.
 * The set of all program state/configuration is the state space.
 */
public abstract class ProgramConfigurationTemplate<NST extends NodeStateTemplate> implements Comparable {
    public static final int INFINITY_RANK = Integer.MAX_VALUE;
    public static final int CVF_AS_ARBITRARY_PERTURBATION = 1;
    public static final int CVF_AS_CONSTRAINED_PERTURBATION = 2;
    public static final int CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION = 4;

    public static final int PROBE_LIMIT_OPTION_ALL_PATHS = -1;
    public static final int PROBE_LIMIT_OPTION_ADAPTIVE = -2;

    // number of nodes in the distributed program
    private int numberOfNodes;
    // map node Id to state of that node
    private TreeMap<Integer, NST> nodeStateMap;
    // how cvf is defined, e.g. arbitrary or constrained perturbation
    protected int cvf;
    // at most how many paths to be probes in random evaluation of ranks
    private int probeLimit;

    public ProgramConfigurationTemplate(int numberOfNodes, TreeMap<Integer, NST> nodeStateMap, int cvf, int probeLimit){
        this.numberOfNodes = numberOfNodes;
        this.nodeStateMap = nodeStateMap;
        this.cvf = cvf;
        this.probeLimit = probeLimit;
    }

    /**
     * @return: get a deep copy of the current program configuration
     */
    abstract public ProgramConfigurationTemplate getDeepCopy();

    /**
     * @return with the given number of nodes, how many possible program configurations are there?
     */
    abstract public long getSizeOfStateSpace();

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    public void setNumberOfNodes(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    public TreeMap<Integer, NST> getNodeStateMap() {
        return nodeStateMap;
    }

    public void setNodeStateMap(TreeMap<Integer, NST> nodeStateMap) {
        this.nodeStateMap = nodeStateMap;
    }

    public int getCvf() {
        return cvf;
    }

    public void setCvf(int cvf) {
        this.cvf = cvf;
    }

    public int getProbeLimit() {
        return probeLimit;
    }

    public void setProbeLimit(int probeLimit) {
        this.probeLimit = probeLimit;
    }

    /**
     * Associate or replace state of a node at specified position
     * @param nodeId  position of node of interest
     * @param stateForNodeId new state for that node
     */
    public void updateStateForANode(int nodeId, NST stateForNodeId){
        getNodeStateMap().put(nodeId, stateForNodeId);
    }

    /**
     * Create a program configuration similar to this configuration
     * except that the node at specified position is replaced by new state
     * @param nodeId  position of node we want to replace state
     * @param newStateForNodeId new state for the node at specified position
     * @return the newly created program configuration
     */
    public ProgramConfigurationTemplate copyConfigurationAndReplaceState(
            int nodeId,
            NST newStateForNodeId){

        ProgramConfigurationTemplate copy = this.getDeepCopy();
        copy.updateStateForANode(nodeId, newStateForNodeId);

        return copy;
    }

    /**
     * Move to another program configuration, i.e. change the current config to the new config
     * This method is assumed to be called in the context that we only need to change
     * the node state map. Other variables are supposed to be the same
     * @param newPc
     */
    public void moveToAnotherProgramConfig(ProgramConfigurationTemplate newPc){
        setNodeStateMap(newPc.getNodeStateMap());

        // these pieces of information is supposed to be the same
        // disable it since we may forget to set it properly in the newPc
//        setNumberOfNodes(newPc.getNumberOfNodes());
//        setCvf(newPc.getCvf());
//        setProbeLimit(newPc.getProbeLimit());

    }

    /**
     * Overriding equals method of Object class
     * @param otherState
     * @return true if the state assignment of this state is equal to otherState
     */
    @Override
    public boolean equals(Object otherState){
        if(otherState == null)
            return false;

        if(otherState == this)
            return true;

        if(!(otherState instanceof ProgramConfigurationTemplate))
            return false;

        ProgramConfigurationTemplate<NST> pct = (ProgramConfigurationTemplate<NST>) otherState;

        if(this.numberOfNodes != pct.getNumberOfNodes())
            return false;
        for(int nodeId = 0; nodeId < numberOfNodes; nodeId ++){
            if(!nodeStateMap.get(nodeId).equals(pct.getNodeStateMap().get(nodeId)))
                return false;
        }

        return true;
    }

    /**
     * When overriding equals(), we override hashCode().
     * @return the hash of program state
     */
    @Override
    public int hashCode(){
        Vector<Integer> nodeStateHashList = new Vector<>(numberOfNodes);
        for(int nodeId = 0; nodeId < numberOfNodes; nodeId++){
            nodeStateHashList.addElement(nodeStateMap.get(nodeId).hashCode());
        }
        return Objects.hash(numberOfNodes, nodeStateHashList);
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
    abstract public NodeActionEvaluationResults evaluateANodeActions(int nodeId);

    public int getNumberOfPrivileges(){
        int enabledPrivilegeCount = 0;

        for(int nodeId = 0; nodeId < getNumberOfNodes(); nodeId ++){
            enabledPrivilegeCount += evaluateANodeActions(nodeId).getPrivilegeCount();
        }

        return enabledPrivilegeCount;
    }

    /**
     * Perturb state of a specified node with cvf.
     * cvf is perturbation resulted from executing program transition based on reading (incorrect/stale) neighbor state.
     * Although cvf is a subset of function perturbANodeArbitrarily, obtaining the exact set of cvfs
     * is challenging because we don't know how the stale neighbor state would be.
     * This function provide several options for us to compute cvf such as
     *    cvf as perturbation of the given node state constrained by program transition
     *    cvf as arbitrary perturbation of the given node state
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by perturbing a node following cvf rules.
     *         Current configuration is excluded in results
     */
    public NodePerturbationResults perturbANodeByCvf(int nodeId){
        switch(cvf){
            case CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION:
                return perturbANodeWithContraintAndTopologyRestriction(nodeId);

            case CVF_AS_CONSTRAINED_PERTURBATION:
                return perturbANodeWithContraint(nodeId);

            case CVF_AS_ARBITRARY_PERTURBATION:
                return perturbANodeArbitrarily(nodeId);

            default:
                System.out.println("ERROR: unknown cvf type: " + cvf);
                return null;
        }
    }

    /**
     * Perturb arbitrarily the state of a specified node.
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by arbitrarily perturbing a node
     *         Current configuration is excluded in the results.
     */
    abstract public NodePerturbationResults perturbANodeArbitrarily(int nodeId);

    /**
     * Perturb the state of a specified node but the perturbation is constrained by program transition rules
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by arbitrarily perturbing a node
     *         Current configuration is excluded in the results.
     */
    abstract public NodePerturbationResults perturbANodeWithContraint(int nodeId);

    /**
     * Perturb the state of a specified node but the perturbation is constrained by program transition rules
     * Furthermore, we consider the topology restriction/specifics as well,
     * e.g. in maximal matching, p-value can only be one of the neighbors
     * This function differs and is expected to be more efficient than perturbANodeWithContraint
     * only in problem where topology is explicit.
     * For problems with implicit topology like Dijkstra-3-states, it does not differ.
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by arbitrarily perturbing a node
     *         Current configuration is excluded in the results.
     */
    abstract public NodePerturbationResults perturbANodeWithContraintAndTopologyRestriction(int nodeId);

    /**
     * @return list of all possible successor state of the current state.
     *         Those successors are obtained by normal program transition
     */
    public TreeSet<SuccessorInfo> getProgramConfigTransSuccessorList(){
        TreeSet<SuccessorInfo> successorOfAllNodesActions = new TreeSet<>();

        for(int nodeId : nodeStateMap.keySet()){
//            TreeSet<ProgramConfigurationTemplate> successorListOfCurrentNodeAction =
//                    evaluateANodeActions(nodeId).getSuccessorList();
//
//            for(ProgramConfigurationTemplate<T> aSuccessor : successorListOfCurrentNodeAction) {
//                successorOfAllNodesActions.add(aSuccessor);
//            }

            successorOfAllNodesActions.addAll(evaluateANodeActions(nodeId).getSuccessorList());
        }

        return successorOfAllNodesActions;
    }

    /**
     * @return list of all possible cvf successors of the current program configuration
     *          note that one perturbation is obtained by choosing a node (in the graph) and perturb its state.
     *         There are two ways to obtained cvf successor states
     *           (1) a cvf successor is obtained by arbitrarily perturbing the state
     *               of one node in the configuration.
     *               This is a upper bound
     *           (2) a cvf successor is obtained by perturbing the state of one node
     *               in the configuration, but the perturbation follows the program transition rules.
     *               This is a subset of (1).
     */
    public TreeSet<SuccessorInfo> getProgramConfigCvfSuccessorList(){
        TreeSet<SuccessorInfo> cvfSuccessorOfAllNodes = new TreeSet<SuccessorInfo>();

        for(int nodeId : nodeStateMap.keySet()){
//            TreeSet<ProgramConfigurationTemplate<T>> cvfSuccessorListOfCurrentNode =
//                    perturbANodeByCvf(nodeId).getPerturbedSuccessorList();
//
//            for(ProgramConfigurationTemplate<T> aCvfSuccessor : cvfSuccessorListOfCurrentNode) {
//                cvfSuccessorOfAllNodes.add(aCvfSuccessor);
//            }

            cvfSuccessorOfAllNodes.addAll(perturbANodeByCvf(nodeId).getPerturbedSuccessorList());
        }
        return cvfSuccessorOfAllNodes;
    }

//    /**
//     * @return list of some random (instead of all) cvf successors of the current program configuration
//     *          by choosing some random nodes and perturb it
//     */
//    public TreeSet<ProgramConfigurationTemplate<T>> getProgramConfigSomeCvfSuccessorList(){
//
//    }

    /**
     * @return true if the current state is inside the invariant (i.e. it is a legitimate state)
     *         false otherwise
     */
    abstract public boolean isInsideInvariant();

    /**
     * Change the current program config to the first program configuration in some enumeration scheme.
     * This scheme must be the same as the enumeration scheme used in getLastProgramConfig()
     * @return this object
     */
    abstract public ProgramConfigurationTemplate<NST> moveToFirstProgramConfig();

    /**
     * Change the current program config to the last program configuration in some enumeration scheme.
     * This scheme must be the same as the enumeration scheme used in getFirstProgramConfig()
     * @return this object
     */
    abstract public ProgramConfigurationTemplate<NST> moveToLastProgramConfig();

    /**
     * Change the current program config to the next program configuration in some enumeration scheme.
     * This scheme must be the same as the enumeration scheme used in getFirstProgramConfig(), getLastProgramConfig().
     * @return this object
     */
    abstract public ProgramConfigurationTemplate<NST> moveToNextProgramConfig();

    /**
     * Change the current program config to the nth program configuration
     * That is n positions after the first configuration, in some enumeration scheme.
     * This scheme must be the same as the enumeration scheme used in getFirstProgramConfig(), getLastProgramConfig().
     * @return this object
     */
    public ProgramConfigurationTemplate<NST> moveToNthProgramConfig(long n){
        // this is the base implementation
        // it is recommended to be overloaded by subclass for more efficient computation
        this.moveToFirstProgramConfig();
        for(long pos = 0; pos < n; pos ++){
            this.moveToNextProgramConfig();
        }

        return this;
    }


    /**
     * Change the current program configuration to a random program configuration in the state space
     * @return this object
     */
    public ProgramConfigurationTemplate<NST> getRandomProgramConfiguration(){
        long position = ThreadLocalRandom.current().nextLong(getSizeOfStateSpace());
        this.moveToNthProgramConfig(position);
        return this;
    }

//    /**
//     * Evaluate all distinct paths to the invariant for this program config, and compute their length as well.
//     * In other words, evaluate all possible ranks.
//     * Algo: BFS-based finding all paths
//     *      while allPaths not empty
//     *          pop out a path P at head of linked list allPaths
//     *          extract last element E of that path P
//     *          if E is the destination (in the invariant)
//     *              we found a path, report P
//     *          else
//     *              for each successor F of E
//     *                  create a new path by adding F to P
//     *                  i.e. P_F = P + F
//     *                  add P_F to linked list allPaths
//     * @return a vector of all the ranks (lengths)
//     */
//    @Deprecated
//    public Vector<Integer> evaluateAllPaths(){
//        Vector<Integer> rankList = new Vector<>();
//
//        LinkedList<ProgramConfigurationSequence> allPaths = new LinkedList<>();
//        Vector<ProgramConfigurationTemplate> initialPath = new Vector<>(200,200);
//
//        // init
//        initialPath.addElement(this);
//        allPaths.add(new ProgramConfigurationSequence(initialPath));
//
//        while(!allPaths.isEmpty()){
//            ProgramConfigurationSequence currentPath = allPaths.removeFirst();
//            ProgramConfigurationTemplate lastConfig = currentPath.getLastConfig();
//            if(lastConfig.isInsideInvariant()){
//                // report currentPath
//                rankList.addElement(currentPath.getPathLength());
//            }else{
//                // create new path by adding successors to currentPath
//                TreeSet<ProgramConfigurationTemplate> successorList = lastConfig.getProgramConfigTransSuccessorList();
//                for(ProgramConfigurationTemplate successor : successorList){
//                    ProgramConfigurationSequence newPath = currentPath.getDeepCopy();
//                    newPath.appendConfig(successor);
//                    allPaths.addLast(newPath);
//                }
//            }
//        }
//
//        return rankList;
//    }


//    /**
//     * Evaluate all distinct paths to the invariant for this program config, and compute their length as well.
//     * In other words, evaluate all possible ranks.
//     * Algo: BFS-based finding all paths
//     *      while allPaths not empty
//     *          pop out a path P at head of linked list allPaths
//     *          extract last element E of that path P
//     *          if E is the destination (in the invariant)
//     *              we found a path, report P
//     *          else
//     *              for each successor F of E
//     *                  create a new path by adding F to P
//     *                  i.e. P_F = P + F
//     *                  add P_F to linked list allPaths
//     * @return a vector of all the ranks (lengths)
//     */
//    public <PCT extends ProgramConfigurationTemplate>
//    RankEvaluationResults evaluateAllPaths(TreeMap<PCT, RankEvaluationResults> rankCache){
//
//        if(rankCache.containsKey((this))){
//            return rankCache.get(this);
//        }
//
//        LinkedList<ProgramConfigurationSequence> allPaths = new LinkedList<>();
//        Vector<ProgramConfigurationTemplate> initialPath = new Vector<>(200,200);
//
//        // init
//        initialPath.addElement(this);
//        allPaths.add(new ProgramConfigurationSequence(initialPath));
//
//        while(!allPaths.isEmpty()){
//            ProgramConfigurationSequence currentPath = allPaths.removeFirst();
//            ProgramConfigurationTemplate lastConfig = currentPath.getLastConfig();
//            if(lastConfig.isInsideInvariant()){
//                // report currentPath
//                rankList.addElement(currentPath.getPathLength());
//            }else{
//                // create new path by adding successors to currentPath
//                TreeSet<ProgramConfigurationTemplate> successorList = lastConfig.getProgramConfigTransSuccessorList();
//                for(ProgramConfigurationTemplate successor : successorList){
//                    ProgramConfigurationSequence newPath = currentPath.getDeepCopy();
//                    newPath.appendConfig(successor);
//                    allPaths.addLast(newPath);
//                }
//            }
//        }
//
//        return rankList;
//    }

    /**
     * evaluate the rank of current program configuration
     * @return the max, average, and min rank of current program configuration
     */
    public RankEvaluationResults evaluateRanks(TreeMap<ProgramConfigurationTemplate, RankEvaluationResults> rankCache){
        if(rankCache.containsKey(this)){
            return rankCache.get(this);
        }


        RankEvaluationResults rankEval = evaluateRanksBasedOnRandomPaths(rankCache);

        boolean sanityCheckResult = rankEval.sanityCheck();

//        if(!sanityCheckResult){
//            System.out.println("sanityCheck for pc " + this.toString() + " does not pass");
//            System.out.println("  " + rankEval.toString());
//        }

        return rankEval;
    }

//    /**
//     * evaluate the max rank of current program configuration
//     * @return the max rank of current program configuration, i.e. length of longest path to the invariant.
//     */
//    @Deprecated
//    int evaluateMaxRank(){
//        return Collections.max(evaluateAllPaths());
//    }

//    /**
//     * evaluate the max rank of current program configuration
//     * Using cache for speed
//     * @return the max rank of current program configuration, i.e. length of longest path to the invariant.
//     */
//    public RankEvaluationResults evaluateMaxRank(TreeMap<ProgramConfigurationTemplate, RankEvaluationResults> rankCache){
//        if(rankCache.containsKey(this)){
//            return rankCache.get(this);
//        }
//
//        RankEvaluationResults result;
//        result = evaluateMaxRankBasedOnRandomPaths(rankCache, probeLimit);
//
//        updateRankCache(rankCache, this, result);
//
//        return result;
//    }

    /**
     * evaluate the ranks of current program configuration
     * based on a set of probeLimit randomly probed paths to the invariant
     * @param rankCache cache containing the ranks of known program configurations
     * @return the max and average (round up) and min of the lengths of probed paths
     */
    RankEvaluationResults evaluateRanksBasedOnRandomPaths(
            TreeMap<ProgramConfigurationTemplate,
            RankEvaluationResults> rankCache){

        if(rankCache.containsKey(this))
            return rankCache.get(this);

        RankEvaluationResults result;

        // option 1: use cache
        //           faster but reduced randomness
        result = evaluateRanksBasedOnRandomPaths(rankCache, probeLimit);

//        // option 2: no cache
//        //           slower but more randomness
//        result = evaluateRanksBasedOnRandomPathsNoCache(probeLimit);

        //rankCache.put(this, result);
        // result.updateRankCache(rankCache, this);
        updateRankCache(rankCache, this, result);
        result.sanityCheck();

//        try {
//            debugFile.write(result.toString() + "\n");
//            debugFile.flush();
//        }catch (IOException e){
//            System.out.println(e.getMessage());
//        }

        return result;
    }


    /**
     * evaluate some random path to the invariant from the current program configuration.
     * we probe at most maxPathCount and calculate their lengths,
     * Those lengths are used to estimate the rank of the current program configuration.
     *
     * Note:
     * this version does not use cache to increase randomness
     * The trade-off is slower computation time
     *
     * @param maxPathCount maximum number of paths to be probed.
     *                     if maxPathCount = 0, it is all possible paths
     *                     if maxPathCount = -1, it call evaluateAllPaths()
     * @return the max, min and average lengths of at most maxPathCount random paths
     */
    public RankEvaluationResults evaluateRanksBasedOnRandomPathsNoCache(int maxPathCount) {
        if(maxPathCount < 0 || maxPathCount > 1000){
            System.out.println("ERROR: evaluateSomeRandomPaths with maxPathCount out of range [0 1000] " + maxPathCount);
        }

        // base case where current program configuration is in invariant
        if(this.isInsideInvariant()){
            return new RankEvaluationResults(0, 0, BigInteger.ZERO, BigInteger.ONE);
        }

        RankEvaluationResults result = new RankEvaluationResults();

        /*
        get list of successors
        if number of successors > maxPathCount
            select randomly maxPathCount successor
            call evaluateSomeRandomPaths(1) for each successor
            add one to each of returned pathLengthList
        if number of successors <= maxPathCount
            distribute maxPathCount among the successors, says a count each
            call evaluateSomeRandomPaths(a) for each successor
            add one to each of returned pathLengthList
         */

        TreeSet<SuccessorInfo> transSuccList = getProgramConfigTransSuccessorList();
        int numberOfTransSuccessors = transSuccList.size();

        if(maxPathCount == 0){
            DebugInfo.caseMaxPathCountZero ++;

            System.out.println("ATTENTION call with maxPathCount = 0");

            // probe as many as possible
            for(SuccessorInfo succ : transSuccList){
                addToResultsNewProbes(result,
                        succ.getSuccessorProgramConfig().evaluateRanksBasedOnRandomPathsNoCache(0));
            }
            return result;
        }

        // probe with limited number of paths
        if(numberOfTransSuccessors > maxPathCount){
            caseSuccessorIsMore ++;

            // randomly select maxPathCount successors
            TreeSet<Integer> positionSet = new TreeSet<>();
            while(positionSet.size() < maxPathCount){
                positionSet.add(ThreadLocalRandom.current().nextInt(numberOfTransSuccessors));
            }

            int position = 0;
            for(SuccessorInfo succ : transSuccList){
                if(position == positionSet.first()){
                    // remove smallest position
                    positionSet.pollFirst();

                    // for each selected successor, we probe only one path
                    addToResultsNewProbes(result,
                            succ.getSuccessorProgramConfig().evaluateRanksBasedOnRandomPathsNoCache(1));
                }

                position ++;

                if(positionSet.isEmpty()) {
                    break;
                }
            }
        }else{
            // more paths than successors, distribute the probes

            caseSuccessorIsLess ++;

//            int remainingProbeCount = maxPathCount;
//            for(ProgramConfigurationTemplate pc : transSuccList){
//                int probeAllowance = (remainingProbeCount + numberOfTransSuccessors - 1)/numberOfTransSuccessors;
//                RankEvaluationResults aNewProbe = pc.evaluateSomeRandomPathsNoCache(probeAllowance);
//                addToResultsNewProbes(result, aNewProbe);
//                remainingProbeCount -= aNewProbe.getPathCount();
//
//                // sometimes a probe return more path than the allowance
//                // and we should terminate early, otherwise, the number of paths could be huge
//                if(remainingProbeCount <= 0) {
//                    break;
//                }
//            }

            // more paths than successors, distribute the probes
            int probeCount = maxPathCount/numberOfTransSuccessors;
            int remainder = maxPathCount % numberOfTransSuccessors;

            for(SuccessorInfo succ : transSuccList){
                ProgramConfigurationTemplate pc = succ.getSuccessorProgramConfig();
                RankEvaluationResults aNewProbe;
                if(remainder > 0){
                    aNewProbe = pc.evaluateRanksBasedOnRandomPathsNoCache(probeCount + 1);
                    remainder --;
                }else{
                    aNewProbe = pc.evaluateRanksBasedOnRandomPathsNoCache(probeCount);
                }

                addToResultsNewProbes(result, aNewProbe);
            }

        }

        return result;

    }

    /**
     * evaluate some random path to the invariant from the current program configuration.
     * we probe at most maxPathCount and calculate their lengths,
     * Those lengths are used to estimate the max and average rank of the current program configuration.
     *
     * Note:
     * This version use cache to improve computation speed.
     * However, the cost is reduced randomness since we may be
     * stick to one a two successors which are already present
     * in the cache and their total path count is more than the
     * maximum number of paths to be probed
     *
     * @param rankCache cache containing rank of known/visited program configuration.
     *                  This helps to improve the speed by avoid probing same program configuration multiple times.
     *                  This should not affect the calculation of max, min rank, but average rank may be affected.
     * @param maxPathCount maximum number of paths to be probed.
     *                     if maxPathCount = 0, it is all possible paths
     *                     if maxPathCount = -1, it call evaluateAllPaths()
     * @return the max, min and average lengths of at most maxPathCount random paths
     */
    public RankEvaluationResults evaluateRanksBasedOnRandomPaths(
            TreeMap<ProgramConfigurationTemplate, RankEvaluationResults> rankCache,
            int maxPathCount){

//        if(maxPathCount < 0 || maxPathCount > 1000){
//            System.out.println("ERROR: evaluateSomeRandomPaths with maxPathCount out of range [0 1000] " + maxPathCount);
//        }

        // base case where current program configuration is in invariant
        if(this.isInsideInvariant()){
            numberOfProbedConfigurationsInsideInvariant ++;

//            return new RankEvaluationResults(0, 0, 0, 1);
            return new RankEvaluationResults(0, 0, BigInteger.ZERO, BigInteger.ONE);
        }

        RankEvaluationResults result = new RankEvaluationResults();

//        if(maxPathCount == PROBE_LIMIT_OPTION_ALL_PATHS)
//            return evaluateAllPaths();

        /*
        get list of successors
        if number of successors > maxPathCount
            select randomly maxPathCount successor
            call evaluateSomeRandomPaths(1) for each successor
            add one to each of returned pathLengthList
        if number of successors <= maxPathCount
            distribute maxPathCount among the successors, says a count each
            call evaluateSomeRandomPaths(a) for each successor
            add one to each of returned pathLengthList
         */

        TreeSet<SuccessorInfo> transSuccList = getProgramConfigTransSuccessorList();
        int numberOfTransSuccessors = transSuccList.size();

        if(maxPathCount == 0){
            DebugInfo.caseMaxPathCountZero ++;

//            System.out.println("ATTENTION call with maxPathCount = 0");

            // probe as many as possible
            for(SuccessorInfo succ : transSuccList){
                ProgramConfigurationTemplate pc = succ.getSuccessorProgramConfig();
                if(rankCache.containsKey(pc)){
                    addToResultsNewProbes(result, rankCache.get(pc));
                }else {
                    RankEvaluationResults aNewProbe = pc.evaluateRanksBasedOnRandomPaths(rankCache, 0);
                    addToResultsNewProbes(result, aNewProbe);
                    updateRankCache(rankCache, pc, aNewProbe);
                }
            }

            result.sanityCheck();

            return result;
        }

        // probe with limited number of paths
        if(numberOfTransSuccessors > maxPathCount){
            caseSuccessorIsMore ++;

            // randomly select maxPathCount successors
            TreeSet<Integer> positionSet = new TreeSet<>();
            while(positionSet.size() < maxPathCount){
                // since it is a set, duplicate is avoided
                // they are also ordered by value due to TreeSet
                positionSet.add(ThreadLocalRandom.current().nextInt(numberOfTransSuccessors));
            }

            int position = 0;
            for(SuccessorInfo succ : transSuccList){
                ProgramConfigurationTemplate pc = succ.getSuccessorProgramConfig();
                if(position == positionSet.first()){
                    // remove smallest position
                    positionSet.pollFirst();

                    // for each selected successor, we probe only one path
                    if(rankCache.containsKey(pc)){
                        addToResultsNewProbes(result, rankCache.get(pc));
                    }else {
                        RankEvaluationResults aNewProbe = pc.evaluateRanksBasedOnRandomPaths(rankCache, 1);
                        addToResultsNewProbes(result, aNewProbe);
                        updateRankCache(rankCache, pc, aNewProbe);
                    }
                }

                position ++;

                if(positionSet.isEmpty()) {
                    break;
                }
            }
        }else{
            // more paths than successors, distribute the probes

            caseSuccessorIsLess ++;

            // should make sure that the number of probes does not exceed the maxPathCount too much
            // otherwise, the number of probes could be huge and lead to overflow of long data type
            int remainingProbeCount = maxPathCount;
            for(SuccessorInfo succ : transSuccList){
                ProgramConfigurationTemplate pc = succ.getSuccessorProgramConfig();
                if(rankCache.containsKey(pc)){
                    addToResultsNewProbes(result, rankCache.get(pc));

                    //    enable: reduce number of actual probes
                    //    disable: probes more, but more time, and some count (maxPathCount) could overflow
                    // remainingProbeCount -= rankCache.get(pc).getPathCount();
//                    remainingProbeCount --;
                }else{
                    int probeAllowance = (remainingProbeCount + numberOfTransSuccessors - 1)/numberOfTransSuccessors;
                    RankEvaluationResults aNewProbe = pc.evaluateRanksBasedOnRandomPaths(rankCache, probeAllowance);
                    addToResultsNewProbes(result, aNewProbe);
                    updateRankCache(rankCache, pc, aNewProbe);
                    remainingProbeCount -= aNewProbe.getPathCount().intValue();
                }
                // sometimes a probe return more path than the allowance
                // and we should terminate early, otherwise, the number of paths could be huge
                if(remainingProbeCount <= 0) {
                    break;
                }
            }

//            // doing like below could lead to huge number of probes since we do not count
//            // the probes already in the cache
//            int remainingProbeCount = maxPathCount;
//            for(ProgramConfigurationTemplate pc : transSuccList){
//                if(rankCache.containsKey(pc)){
//                    // we can use the (brief) known results in cache to reduce time
//                    addToResultsNewProbes(result, rankCache.get(pc));
//                }else{
//                    // we have to probe
//                    int probeAllowance = (remainingProbeCount + numberOfTransSuccessors - 1)/numberOfTransSuccessors;
//                    remainingProbeCount -= probeAllowance;
//                    addToResultsNewProbes(result, pc.evaluateRanksBasedOnRandomPaths(rankCache, probeAllowance));
//                }
//            }

        }

        result.sanityCheck();

        return result;
    }



    /**
     * Add results from a probe to the overall results
     *   if applicable, we need to update
     *      max rank = max of new probe + 1
     *      min rank = min of new probe + 1
     *   update
     *      rank total += new probe rank total + new probe path count  // since each path of the new probe has length increased by 1
     *      path count += new probe path count
     * @param results
     * @param aNewProbe
     */
    private void addToResultsNewProbes(RankEvaluationResults results, RankEvaluationResults aNewProbe){
        // sanity check new probe
        aNewProbe.sanityCheck();

        if(results.getMaxRank() == -1){
            // results have no sub-path yet
            results.setMaxRank(aNewProbe.getMaxRank() + 1);
            results.setMinRank(aNewProbe.getMinRank() + 1);
            //results.setRankTotal(aNewProbe.getRankTotal() + aNewProbe.getPathCount());
            results.setRankTotal(aNewProbe.getRankTotal().add(aNewProbe.getPathCount()));
            results.setPathCount(aNewProbe.getPathCount());
        }else{
            // results already has sub-path before
            // update max, min
            if(results.getMaxRank() < aNewProbe.getMaxRank() + 1){
                results.setMaxRank(aNewProbe.getMaxRank() + 1);
            }
            if(results.getMinRank() > aNewProbe.getMinRank() + 1){
                results.setMinRank(aNewProbe.getMinRank() + 1);
            }

            // update totalRank and pathCount
            //long additionRank = aNewProbe.getRankTotal() + aNewProbe.getPathCount();
            //results.setRankTotal(results.getRankTotal() + additionRank);
            //results.setPathCount(results.getPathCount() + aNewProbe.getPathCount());
            BigInteger additionRank = aNewProbe.getRankTotal().add(aNewProbe.getPathCount());
            results.setRankTotal(results.getRankTotal().add(additionRank));
            results.setPathCount(results.getPathCount().add(aNewProbe.getPathCount()));
        }
    }



//    public String toString(){
//        StringBuilder s = new StringBuilder();
//        s.append("<");
//        s.append(getNodeStateMap().get(0).toString());
//        for(int nodeId = 1; nodeId < getNumberOfNodes(); nodeId ++){
//            s.append(", " + getNodeStateMap().get(nodeId).toString());
//        }
//        s.append(">");
//
//        return s.toString();
//    }

    public String toString(){
        StringBuilder s = new StringBuilder();
        s.append(getNodeStateMap().get(0).toString());
        for(int nodeId = 1; nodeId < getNumberOfNodes(); nodeId ++){
            s.append("-" + getNodeStateMap().get(nodeId).toString());
        }
        return s.toString();
    }



    /***********************************************************************
     ***  This code is for the simulation part
     ************************************************************************/

    /**
     * perform a computation to get the number of program transitions (steps) needed
     * to make the current program configuration to convergence to the invariant
     * @param cvfInterval number of program transitions before each cvf occurrence
     * @param simulationLimit number of simulation steps
     * @return number of program transitions needed for convergence
     */
    public int getNumberOfConvergenceSteps(int simulationLimit, int cvfInterval){
        int stepCount;
        for(stepCount = 0; stepCount < simulationLimit; stepCount ++){
            if(this.isInsideInvariant())
                break;

            // choose a random action and perform it
            // i.e. move to random successor
            SuccessorInfo progSuccessor =
                    getRandomElementFromSuccessorTreeSet(this.getProgramConfigTransSuccessorList());
            moveToAnotherProgramConfig(progSuccessor.getSuccessorProgramConfig());

            // insert a random cvf
            if((stepCount + 1) % cvfInterval == 0){
                SuccessorInfo cvfSuccessor =
                        getRandomElementFromSuccessorTreeSet(this.getProgramConfigCvfSuccessorList());
                moveToAnotherProgramConfig(cvfSuccessor.getSuccessorProgramConfig());
            }
        }

        return stepCount;

    }

}
