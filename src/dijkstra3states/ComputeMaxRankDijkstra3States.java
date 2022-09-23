//package dijkstra3states;
//
//import joptsimple.OptionParser;
//import joptsimple.OptionSet;
//import joptsimple.OptionSpec;
//
//import java.util.*;
//
//import static template.ProgramConfigurationTemplate.INFINITY_RANK;
//
///**
// * This is a pilot class to compute max rank for Dijkstra 3 state program.
// * It is deprecated and replaced by ComputeMaxRank class.
// */
//public class ComputeMaxRankDijkstra3States {
//    private int runId;
//    private int numberOfNodes;
//
//    // mapping a program configuration/state to its rank
//    private TreeMap<ProgramConfigurationDijkstra3States, Integer> programConfigMaxRankMap;
//
//    // who are successors of a config
//    private TreeMap<ProgramConfigurationDijkstra3States, TreeSet<ProgramConfigurationDijkstra3States>>
//            programConfigSuccessorMap;
//
//    // who is successor of a config on the longest path
//    private TreeMap<ProgramConfigurationDijkstra3States, ProgramConfigurationDijkstra3States>
//            successorLongestPathMap;
//
//
//
//    public ComputeMaxRankDijkstra3States(int runId, int numberOfNodes){
//        this.runId = runId;
//        this.numberOfNodes = numberOfNodes;
//    }
//
//
//
//    /**
//     * Initialize the the mapping of each program config to a rank.
//     * If a config is in the invariant, its rank is 0.
//     * Other configs outside the invariant have initial rank of infinity
//     * Also initialize a map mapping each configuration to its successors
//     * @throws Exception
//     */
//    void init() throws Exception {
//        int hashMapInitialCapacity = 15000000; //1500000000; // 1.5 billion states
//        programConfigMaxRankMap = new TreeMap<>(); // new HashMap<>(hashMapInitialCapacity, 0.9f);
//        programConfigSuccessorMap = new TreeMap<>(); // new HashMap<>(hashMapInitialCapacity, 0.9f);
//        successorLongestPathMap = new TreeMap<>(); // new HashMap<>(hashMapInitialCapacity, 0.9f);
//
//        ProgramConfigurationDijkstra3States currentProgramConfig =
//                new ProgramConfigurationDijkstra3States(numberOfNodes, new TreeMap<>())
//                .moveToFirstProgramConfig();
//        ProgramConfigurationDijkstra3States firstProgramConfig =
//                new ProgramConfigurationDijkstra3States(numberOfNodes, new TreeMap<>())
//                .moveToFirstProgramConfig();
//
//        // iterate through the program configs
//        int configCount = 0;
//        do{
//            ProgramConfigurationDijkstra3States currentConfigCopy = currentProgramConfig.getDeepCopy();
//
//            // if current config is in invariant, set rank 0
//            // otherwise, set rank infinity
//            if(currentConfigCopy.isInsideInvariant()){
//
//                programConfigMaxRankMap.put(currentConfigCopy, 0);
//
//                System.out.println("  " + currentConfigCopy.toString() + " => 0");
//
//            }else{
//                programConfigMaxRankMap.put(currentConfigCopy, INFINITY_RANK);
//
//                System.out.println("  " + currentConfigCopy.toString() + " => infinity");
//            }
//
//            // also associate a config with its successors
//            programConfigSuccessorMap.put(
//                    currentConfigCopy,
//                    currentConfigCopy.getProgramConfigSuccessorList());
//
//            // move to next state in some enumeration scheme
//            currentProgramConfig.moveToNextProgramConfig();
//            configCount ++;
//        }while(! currentProgramConfig.equals(firstProgramConfig)); // until we finish a round
//
//        System.out.println("\n total " + configCount + " configs are initialized");
//        displayProgramConfigMaxRankMap();
//        System.out.println();
//    }
//
//
//    /**
//     * Compute all ranks (length of every distinct path to the invariant) for all program configs.
//     * Algo:
//     *      Init:
//     *         for each node i in the invariant
//     *            associate i with a vector with 1 element 0 (rank = 0)
//     *         repeat
//     *              for each node j not in the invariant
//     *                  for each successor s of j
//     *                      for each path p from s to destination
//     *                          add path p + 1 for node j to destination
//     *         until no changes
//     * @return a vector of all the ranks (lengths)
//     */
////    TreeMap<ProgramConfigurationDijkstra3States, ConfigurationPathInformation> computeAllRanksForAllConfigs(){
////
////
////    }
//
//    /**
//     * compute maximum ranks (length of longest path to the invariant) for all program configurations:
//     *   repeat until no change
//     *     for each configuration whose rank is not determined (i.e. rank == INFINITY)
//     *       if all of its successors have finite rank
//     *          its rank = max rank of all successors + 1*
//     * @return number of rank updates
//     */
//    int computeMaxRankForConfigs(){
//        int totalRankUpdateCount = 0;
//        int currentRoundRankUpdateCount = 0;
//        int roundCount = 0;
//
//        // repeat until no rank update
//        do{
//            currentRoundRankUpdateCount = 0;
//            roundCount ++;
//
//            System.out.println("roundCount = " + roundCount);
//
//            System.out.println(" there are " + programConfigMaxRankMap.entrySet().size() + " entries");
//
//            // iterate through the program configs
//            for(Map.Entry<ProgramConfigurationDijkstra3States, Integer> entry : programConfigMaxRankMap.entrySet()){
//
//                if(entry.getValue() == INFINITY_RANK){
//                    System.out.println("    rank of config " + entry.getKey().toString() + " is infinity");
//
//                    TreeSet<ProgramConfigurationDijkstra3States> successorList =
//                            programConfigSuccessorMap.get(entry.getKey());
//
//                    int maxSuccessorRank = -1;
//                    ProgramConfigurationDijkstra3States maxSuccessor = null;
//
//                    for(ProgramConfigurationDijkstra3States successor : successorList){
//                        int successorRank = programConfigMaxRankMap.get(successor);
//                        if(maxSuccessorRank < successorRank) {
//                            maxSuccessorRank = successorRank;
//                            maxSuccessor = successor;
//                        }
//                    }
//
//                    if(maxSuccessorRank != INFINITY_RANK){
//                        // all successors have finite ranks
//                        entry.setValue(maxSuccessorRank + 1);
//                        successorLongestPathMap.put(entry.getKey(), maxSuccessor);
//
//                        currentRoundRankUpdateCount ++;
//
//                        System.out.println("      " + entry.getKey().toString() + " => infinity -> " + (maxSuccessorRank + 1));
//                    }else{
//                        System.out.println("      rank is unchanged");
//                    }
//                }else{
//                    // rank of node has been computed
//                }
//
//            }
//
//            totalRankUpdateCount += currentRoundRankUpdateCount;
//
//            System.out.println("  " + currentRoundRankUpdateCount + " rank updates in this round");
//
//        }while(currentRoundRankUpdateCount != 0);
//
//        return totalRankUpdateCount;
//    }
//
//    /**
//     * iterate through all program configs and display their ranks
//     */
//    void displayConfigMaxRank(){
//        ProgramConfigurationDijkstra3States currentProgramConfig =
//                new ProgramConfigurationDijkstra3States(numberOfNodes, new TreeMap<>())
//                        .moveToFirstProgramConfig();
//        ProgramConfigurationDijkstra3States firstProgramConfig =
//                new ProgramConfigurationDijkstra3States(numberOfNodes, new TreeMap<>())
//                        .moveToFirstProgramConfig();
//
//        // iterate through the program configs
//        int configCounter = 0;
//        do {
//            int rank = programConfigMaxRankMap.get(currentProgramConfig);
//
//            StringBuilder rankIndent = new StringBuilder();
//            for(int i = 0; i < rank; i++){
//                rankIndent.append("    ");
//            }
//
//            System.out.println(
//                    String.format("%10d: %s => %s %3d (%s)",
//                            configCounter,
//                            currentProgramConfig.toString(),
//                            rankIndent.toString(),
//                            rank,
//                            successorLongestPathMap.get(currentProgramConfig))
//            );
//            // move to next state in some enumeration scheme
//            currentProgramConfig.moveToNextProgramConfig();
//            configCounter ++;
//        }while(!currentProgramConfig.equals(firstProgramConfig));
//    }
//
//    void displayProgramConfigMaxRankMap(){
//        for(Map.Entry<ProgramConfigurationDijkstra3States, Integer> entry : programConfigMaxRankMap.entrySet()){
//            System.out.println("  " + entry.getKey().toString() + " => " + entry.getValue());
//        }
//    }
//
//
//
//    // main program
//    public static void main(String args[]) throws Exception {
//        OptionParser mainParser = new OptionParser();
//
//        OptionSpec<Integer> numberOfNodesSpec = mainParser.accepts("number-of-nodes")
//                .withRequiredArg()
//                .ofType(Integer.class);
//        OptionSpec<Integer> runIdSpec = mainParser.accepts("run-id")
//                .withRequiredArg()
//                .ofType(Integer.class);
//
//
//        OptionSet mainOption = mainParser.parse(args);
//        int numberOfNodes = mainOption.valueOf(numberOfNodesSpec);
//        int runId = mainOption.valueOf(runIdSpec);
//
//        if(numberOfNodes <= 1){
//            throw new Exception("The number of nodes " + numberOfNodes + " is too small. Should be > 1");
//        }
//        if(numberOfNodes > 19){
//            throw new Exception("The number of nodes " + numberOfNodes + " is too large." +
//                    " Should be <= 19.");
//        }
//
//        ComputeMaxRankDijkstra3States crd3 = new ComputeMaxRankDijkstra3States(runId, numberOfNodes);
//
//        System.out.println("++init: ");
//        crd3.init();
//
//        System.out.println("\n++compute ranks: ");
//        crd3.computeMaxRankForConfigs();
//
//        System.out.println("\n++results: ");
//        crd3.displayConfigMaxRank();
//
//    }
//}