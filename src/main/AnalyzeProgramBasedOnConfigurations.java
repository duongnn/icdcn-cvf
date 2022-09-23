package main;

import datastructure.AnalysisResults;
import datastructure.DebugInfo;
import datastructure.ProgramConfigurationInformation;
import datastructure.RankEvaluationResults;
import template.ProgramConfigurationTemplate;
import template.SuccessorInfo;

import java.io.*;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static datastructure.ProgramConfigurationInformation.DEFAULT_MIN_RANK;
import static template.ProgramConfigurationTemplate.*;
import static datastructure.RankEvaluationResults.cleanSanityCheckSummary;
import static datastructure.RankEvaluationResults.displaySanityCheckSummary;
import static main.Utility.*;

/**
 * Analyze the effect of program transitions and cvfs on the change in rank of a distributed program T
 * Rank of a program is typically the max rank = length of longest path the the invariant.
 * A program transition (or cvf) takes program from current configuration to the next configuration.
 * The effect of a program transitions (or cvs) is the rank difference between
 * the next configuration and the current configuration
 *
 */

public class AnalyzeProgramBasedOnConfigurations<PCT extends ProgramConfigurationTemplate> extends AnalyzeCvfs {

    // for full analysis
    AnalysisResults<PCT> fullMaxAnaResults; // for max rank
    AnalysisResults<PCT> fullAvgAnaResults; // for average rank

    // for random analysis
    AnalysisResults<PCT> randMaxAnaResults; // select max from random paths
    AnalysisResults<PCT> randAvgAnaResults; // select average from random paths

    private double randomTransNbrProb;  // probability to select a transition nbr to evaluate effect of transitions
    private double randomCvfNbrProb;    // probability to select a cvf nbr to evaluate effect of cvfs
    private String configBaseAnalysisTask; // perform full or statistical analysis or both

    public AnalyzeProgramBasedOnConfigurations(
            int runId,
            int numberOfNodes,
            int cvf,
            String programName,
            String outputFileName,
            long sampleSize,
            long probeLimit,
            PCT currentProgramConfig,
            PCT firstProgramConfig,
            double randomTransNbrProb,
            double randomCvfNbrProb,
            String configBaseAnalysisTask){

        super(runId,
                numberOfNodes,
                cvf,
                programName,
                outputFileName,
                sampleSize,
                probeLimit,
                currentProgramConfig,
                firstProgramConfig);

        this.randomTransNbrProb = randomTransNbrProb;
        this.randomCvfNbrProb = randomCvfNbrProb;
        this.configBaseAnalysisTask = configBaseAnalysisTask;
    }


    /**
     * Initialize the the mapping of each program config to:
     *   an initial rank.
     *     if a config is in the invariant, its initial rank is 0 and permanent
     *     if a config is outside the invariant, its initial rank is infinity and to be updated
     *   and list of its successors
     */
    void init() {
        fullMaxAnaResults = new AnalysisResults<>(new TreeMap<>(), new TreeMap<>());
        fullAvgAnaResults = new AnalysisResults<>(new TreeMap<>(), new TreeMap<>());

        TreeMap<Integer, Integer> max_fullProgConfigRankDistribution = fullMaxAnaResults.getProgConfigRankDistribution();
        TreeMap<PCT, ProgramConfigurationInformation<PCT>> max_fullProgConfigInfoMap = fullMaxAnaResults.getProgConfigInfoMap();
        TreeMap<Integer, Integer> avg_fullProgConfigRankDistribution = fullAvgAnaResults.getProgConfigRankDistribution();
//        TreeMap<PCT, ProgramConfigurationInformation<PCT>> avg_fullProgConfigInfoMap = fullAvgAnaResults.getProgConfigInfoMap();

        currentProgramConfig.moveToFirstProgramConfig();
        firstProgramConfig.moveToFirstProgramConfig();

        // iterate through the program configs
        int configCount = 0;
        do{
            PCT currentConfigCopy = (PCT) currentProgramConfig.getDeepCopy();

            // invariant: rank 0, otherwise: rank infinity
            int initRank;
            if(currentConfigCopy.isInsideInvariant()){
                initRank = 0;

                addValueToDistribution(max_fullProgConfigRankDistribution, initRank);
                addValueToDistribution(avg_fullProgConfigRankDistribution, initRank);

//                System.out.println("  " + currentConfigCopy.toString() + " => 0");
            }else{
                initRank = INFINITY_RANK;

//                System.out.println("  " + currentConfigCopy.toString() + " => infinity");
            }


            max_fullProgConfigInfoMap.put(currentConfigCopy,
                    new ProgramConfigurationInformation<PCT>(
                            initRank,
                            DEFAULT_MIN_RANK,
                            BigInteger.valueOf(initRank),
                            BigInteger.ONE,
                            currentConfigCopy.getProgramConfigTransSuccessorList(),
                            currentConfigCopy.getProgramConfigCvfSuccessorList()));

            // no need this code since info about average rank
            // can also be stored in max_fullProgConfigInfoMap
//            avg_fullProgConfigInfoMap.put(currentConfigCopy,
//                    new ProgramConfigurationInformation<PCT>(
//                            initRank,
//                            DEFAULT_MIN_RANK,
//                            BigInteger.valueOf(initRank),
//                            BigInteger.ONE,
//                            currentConfigCopy.getProgramConfigTransSuccessorList(),
//                            currentConfigCopy.getProgramConfigCvfSuccessorList()));

            // move to next state in some enumeration scheme
            currentProgramConfig.moveToNextProgramConfig();
            configCount ++;
        }while(! currentProgramConfig.equals(firstProgramConfig)); // until we finish a round

        System.out.println("\n     total " + configCount + " configs are initialized");
//        displayProgramConfigInfoMap(max_fullProgConfigInfoMap, 1, false);
//        for(Map.Entry<PCT, ProgramConfigurationInformation<PCT>> entry : max_fullProgConfigInfoMap.entrySet()){
//            System.out.println(entry.getKey().toString());
//            System.out.println(entry.getValue().toString(max_fullProgConfigInfoMap, 1, false));
//        }

        System.out.println();
    }

    /**
     * (full analysis)
     * compute maximum rank (length of the longest path to the invariant) for every program configuration:
     *   repeat until no change
     *     for each configuration whose rank is not determined (i.e. rank == INFINITY)
     *       if all of its successors have finite rank
     *          its rank = max rank of all successors + 1
     * @return number of rank updates
     */
    int fullComputeMaxRankForConfigs(){
        int totalRankUpdateCount = 0;
        int currentRoundRankUpdateCount = 0;
        int roundCount = 0;

        TreeMap<Integer, Integer> fullProgConfigRankDistribution = fullMaxAnaResults.getProgConfigRankDistribution();
        TreeMap<PCT, ProgramConfigurationInformation<PCT>> fullProgConfigInfoMap = fullMaxAnaResults.getProgConfigInfoMap();

        // repeat until no rank update
        do{
            currentRoundRankUpdateCount = 0;
            roundCount ++;

//            System.out.println("roundCount = " + roundCount);

            //System.out.println(" there are " + fullProgConfigInfoMap.entrySet().size() + " entries");

            // iterate through the program configs
            for(Map.Entry<PCT, ProgramConfigurationInformation<PCT>> entry : fullProgConfigInfoMap.entrySet()){

                if(entry.getValue().getMaxRank() == INFINITY_RANK){
//                    System.out.println("    rank of config " + entry.getKey().toString() + " is infinity");

                    TreeSet<SuccessorInfo> successorList = entry.getValue().getProgSuccessorList();

                    int maxRankOfSuccessors = -1;
                    ProgramConfigurationTemplate maxSuccessor = null;


                    for(SuccessorInfo successor : successorList){
                        int successorRank = fullProgConfigInfoMap.get(successor).getMaxRank();

//                        System.out.println("successor " + successor);
//                        ProgramConfigurationInformation<PCT> successorInfo = fullProgConfigInfoMap.get(successor);
//                        //System.out.println(successorInfo == null);
//                        int successorRank = successorInfo.getMaxRank();

                        if(maxRankOfSuccessors < successorRank) {
                            maxRankOfSuccessors = successorRank;
                            maxSuccessor = successor.getSuccessorProgramConfig();
                        }

                    }

                    if(maxRankOfSuccessors != INFINITY_RANK){
                        // all successors have finite ranks, i.e. been computed
                        int nodeNewRank = maxRankOfSuccessors + 1;
                        entry.getValue().setMaxRank(nodeNewRank);

                        if(!fullProgConfigRankDistribution.containsKey(nodeNewRank)){
                            fullProgConfigRankDistribution.put(nodeNewRank, 1);
                        }else{
                            fullProgConfigRankDistribution.put(
                                    nodeNewRank,
                                    fullProgConfigRankDistribution.get(nodeNewRank) + 1);
                        }

                        currentRoundRankUpdateCount ++;

//                        System.out.println("      " + entry.getKey().toString() + " => infinity -> " + (maxRankOfSuccessors + 1));
                    }else{
//                        System.out.println("      rank is unchanged");
                    }
                }else{
                    // rank of node has been computed
                }

            }

            totalRankUpdateCount += currentRoundRankUpdateCount;

//            System.out.println("  " + currentRoundRankUpdateCount + " rank updates in this round");

        }while(currentRoundRankUpdateCount != 0);

        return totalRankUpdateCount;
    }


    /**
     * (full analysis)
     * compute maximum rank (length of the longest path to the invariant) and
     * average rank (average distance to the invariant) for every program configuration:
     *   repeat until no change
     *     for each configuration whose rank is not determined (i.e. rank == INFINITY)
     *       if all of its successors have finite rank
     *          // for max rank
     *          its rank = max rank of all successors + 1
     *          // for average rank
     *          totalPathLength = sum of total path length of all successors + number of successors
     *          totalPathCount = sum of total path count of all successors
     *          its average rank can be derived as totalPathLength/totalPathCount
     * @return number of rank updates
     */
    int fullComputeMaxAndAverageRankForConfigs(){
        int totalRankUpdateCount = 0;
        int currentRoundRankUpdateCount = 0;
        int roundCount = 0;

        TreeMap<Integer, Integer> fullMaxProgConfigRankDistribution = fullMaxAnaResults.getProgConfigRankDistribution();
        TreeMap<PCT, ProgramConfigurationInformation<PCT>> fullMaxProgConfigInfoMap = fullMaxAnaResults.getProgConfigInfoMap();

        TreeMap<Integer, Integer> fullAvgProgConfigRankDistribution = fullAvgAnaResults.getProgConfigRankDistribution();
//        TreeMap<PCT, ProgramConfigurationInformation<PCT>> fullAvgProgConfigInfoMap = fullAvgAnaResults.getProgConfigInfoMap();

        // repeat until no rank update
        do{
            currentRoundRankUpdateCount = 0;
            roundCount ++;

//            System.out.println("roundCount = " + roundCount);

            //System.out.println(" there are " + fullMaxProgConfigInfoMap.entrySet().size() + " entries");

            // iterate through the program configs
            for(Map.Entry<PCT, ProgramConfigurationInformation<PCT>> entry : fullMaxProgConfigInfoMap.entrySet()){

                if(entry.getValue().getMaxRank() == INFINITY_RANK){
//                    System.out.println("    rank of config " + entry.getKey().toString() + " is infinity");

                    TreeSet<SuccessorInfo> successorList = entry.getValue().getProgSuccessorList();

                    int maxRankOfSuccessors = -1;
                    ProgramConfigurationTemplate maxSuccessor = null;
                    BigInteger allSuccTotalPathLength = BigInteger.ZERO;
                    BigInteger allSuccTotalNumberOfPaths = BigInteger.ZERO;

                    for(SuccessorInfo successor : successorList){
                        ProgramConfigurationTemplate successorPC = successor.getSuccessorProgramConfig();
                        int successorRank = fullMaxProgConfigInfoMap.get(successorPC).getMaxRank();

//                        System.out.println("successor " + successor);
//                        ProgramConfigurationInformation<PCT> successorInfo = fullMaxProgConfigInfoMap.get(successor);
//                        //System.out.println(successorInfo == null);
//                        int successorRank = successorInfo.getMaxRank();

                        if(maxRankOfSuccessors < successorRank) {
                            maxRankOfSuccessors = successorRank;
                            maxSuccessor = successorPC;
                        }

                        if(successorRank != INFINITY_RANK) {
                            // only do big integer calculation with successors with finite rank
                            BigInteger successorTotalPathLength = fullMaxProgConfigInfoMap.get(successorPC).getTotalPathLength();
                            BigInteger successorNumberOfPaths = fullMaxProgConfigInfoMap.get(successorPC).getNumberOfPaths();
                            allSuccTotalPathLength = allSuccTotalPathLength.add(successorTotalPathLength);
                            allSuccTotalNumberOfPaths = allSuccTotalNumberOfPaths.add(successorNumberOfPaths);
                        }
                    }

                    if(maxRankOfSuccessors != INFINITY_RANK){
                        // all successors have finite ranks, i.e. been computed
                        int nodeNewMaxRank = maxRankOfSuccessors + 1;
                        entry.getValue().setMaxRank(nodeNewMaxRank);

                        if(!fullMaxProgConfigRankDistribution.containsKey(nodeNewMaxRank)){
                            fullMaxProgConfigRankDistribution.put(nodeNewMaxRank, 1);
                        }else{
                            fullMaxProgConfigRankDistribution.put(
                                    nodeNewMaxRank,
                                    fullMaxProgConfigRankDistribution.get(nodeNewMaxRank) + 1);
                        }

                        currentRoundRankUpdateCount ++;

                        // update average rank
                        BigInteger nodeNewTotalPathLength = allSuccTotalPathLength.add(allSuccTotalNumberOfPaths);
                        BigInteger nodeNewNumberOfPaths = allSuccTotalNumberOfPaths;
                        entry.getValue().setTotalPathLength(nodeNewTotalPathLength);
                        entry.getValue().setNumberOfPaths(nodeNewNumberOfPaths);
                        int nodeNewAvgRank = entry.getValue().getAvgRank();
                        if(!fullAvgProgConfigRankDistribution.containsKey(nodeNewAvgRank)){
                            fullAvgProgConfigRankDistribution.put(nodeNewAvgRank, 1);
                        }else{
                            fullAvgProgConfigRankDistribution.put(
                                    nodeNewAvgRank,
                                    fullAvgProgConfigRankDistribution.get(nodeNewAvgRank) + 1);
                        }

//                        System.out.println("node: " + entry.getKey().toString() + "\n" +
//                                " tpl = " + nodeNewTotalPathLength +
//                                " np = " + nodeNewNumberOfPaths +
//                                " avg-rank = " + nodeNewAvgRank);
//                        for(PCT successor : successorList) {
//                            System.out.println("   succ: " + successor.toString() +
//                                    " tpl = " + fullMaxProgConfigInfoMap.get(successor).getTotalPathLength() +
//                                    " np = " + fullMaxProgConfigInfoMap.get(successor).getNumberOfPaths() +
//                                    " avg-rank = " + fullMaxProgConfigInfoMap.get(successor).getAvgRank());
//                        }

//                        System.out.println("      " + entry.getKey().toString() + " => infinity -> " + (maxRankOfSuccessors + 1));
                    }else{
//                        System.out.println("      rank is unchanged");
                    }
                }else{
                    // rank of node has been computed
                }

            }

            totalRankUpdateCount += currentRoundRankUpdateCount;

//            System.out.println("  " + currentRoundRankUpdateCount + " rank updates in this round");
//            System.out.println("  " + totalRankUpdateCount + " configs have rank updated");

        }while(currentRoundRankUpdateCount != 0);

        return totalRankUpdateCount;
    }

    /**
     * Fully compute the rank effect of program transitions and cvf
     */
    public void fullComputeRankEffectOfProgramTransitionsAndCvfs() {
        fullMaxAnaResults.initAnalysisResults();
        fullAvgAnaResults.initAnalysisResults();

        TreeMap<PCT, ProgramConfigurationInformation<PCT>> progConfigInfoMap = fullMaxAnaResults.getProgConfigInfoMap();

        TreeMap<Integer, Integer> max_progTransOutsideInvRankEffectDistribution = fullMaxAnaResults.getProgTransOutsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> max_cvfInsideInvRankEffectDistribution = fullMaxAnaResults.getCvfInsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> max_cvfOutsideInvRankEffectDistribution = fullMaxAnaResults.getCvfOutsideInvRankEffectDistribution();
        Vector<TreeMap<Integer, Integer>> max_perNodeCvfOutsideInvRankEffectDistribution = fullMaxAnaResults.getPerNodeCvfOutsideInvRankEffectDistribution();

        TreeMap<Integer, Integer> avg_progTransOutsideInvRankEffectDistribution = fullAvgAnaResults.getProgTransOutsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> avg_cvfInsideInvRankEffectDistribution = fullAvgAnaResults.getCvfInsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> avg_cvfOutsideInvRankEffectDistribution = fullAvgAnaResults.getCvfOutsideInvRankEffectDistribution();
        Vector<TreeMap<Integer, Integer>> avg_perNodeCvfOutsideInvRankEffectDistribution = fullAvgAnaResults.getPerNodeCvfOutsideInvRankEffectDistribution();

        for(int nodeId = 0; nodeId < numberOfNodes; nodeId ++){
            // one element per node
            max_perNodeCvfOutsideInvRankEffectDistribution.addElement(new TreeMap<>());
            avg_perNodeCvfOutsideInvRankEffectDistribution.addElement(new TreeMap<>());
        }

        // compute the effect of program transitions and cvfs on each program configuration
        for (Map.Entry<PCT, ProgramConfigurationInformation<PCT>> entry : progConfigInfoMap.entrySet()) {
            PCT progConfig = entry.getKey();
            ProgramConfigurationInformation progConfigInfo = entry.getValue();
            int progConfigMaxRank = progConfigInfo.getMaxRank();
            int progConfigAvgRank = progConfigInfo.getAvgRank();

            TreeSet<SuccessorInfo> successorList = progConfigInfo.getProgSuccessorList();
            TreeSet<SuccessorInfo> cvfSuccessorList = progConfigInfo.getCvfSuccessorList();

            // program transitions
            if (progConfig.isInsideInvariant()) {
                // invariant state, count
                fullMaxAnaResults.incrementNumberOfProgTransInsideInv();
                fullAvgAnaResults.incrementNumberOfProgTransInsideInv();
            } else {
                for (SuccessorInfo successor : successorList) {
                    int successorMaxRank = progConfigInfoMap.get(successor.getSuccessorProgramConfig()).getMaxRank();
                    int progTransitionMaxRankEffect = successorMaxRank - progConfigMaxRank;

                    // increment count for progTransitionMaxRankEffect
                    addValueToDistribution(max_progTransOutsideInvRankEffectDistribution, progTransitionMaxRankEffect);
                    fullMaxAnaResults.incrementNumberOfProgTransOutsideInv();
                    fullMaxAnaResults.increaseProgTransOutsideInvTotalRankEffect(progTransitionMaxRankEffect);


                    // for average rank
                    int successorAvgRank = progConfigInfoMap.get(successor.getSuccessorProgramConfig()).getAvgRank();
                    int progTransitionAvgRankEffect = successorAvgRank - progConfigAvgRank;
                    addValueToDistribution(avg_progTransOutsideInvRankEffectDistribution, progTransitionAvgRankEffect);
                    fullAvgAnaResults.incrementNumberOfProgTransOutsideInv();
                    fullAvgAnaResults.increaseProgTransOutsideInvTotalRankEffect(progTransitionAvgRankEffect);
                }
            }

            // cvf transitions
            if (progConfig.isInsideInvariant()) {
                // invariant state
                for (SuccessorInfo cvfSuccessor : cvfSuccessorList) {
                    int cvfSuccessorMaxRank = progConfigInfoMap.get(cvfSuccessor.getSuccessorProgramConfig()).getMaxRank();
                    int cvfMaxRankEffect = cvfSuccessorMaxRank - progConfigMaxRank;

                    // increment count of cvfMaxRankEffect
                    addValueToDistribution(max_cvfInsideInvRankEffectDistribution, cvfMaxRankEffect);
                    fullMaxAnaResults.incrementNumberOfCvfInsideInv();
                    fullMaxAnaResults.increaseCvfInsideInvTotalRankEffect(cvfMaxRankEffect);


                    // for average rank
                    int cvfSuccessorAvgRank = progConfigInfoMap.get(cvfSuccessor.getSuccessorProgramConfig()).getAvgRank();
                    int cvfAvgRankEffect = cvfSuccessorAvgRank - progConfigAvgRank;
                    addValueToDistribution(avg_cvfInsideInvRankEffectDistribution, cvfAvgRankEffect);
                    fullAvgAnaResults.incrementNumberOfCvfInsideInv();
                    fullAvgAnaResults.increaseCvfInsideInvTotalRankEffect(cvfAvgRankEffect);
                }
            } else {
                // outside invariant state
                for (SuccessorInfo cvfSuccessor : cvfSuccessorList) {
                    if(!progConfigInfoMap.containsKey(cvfSuccessor.getSuccessorProgramConfig())){
                        System.out.println("  ERROR: no info for cvfSuccessor " + cvfSuccessor.toString());
                        System.out.println("progConfigInfoMap: (" + progConfigInfoMap.size() + " entries)");
                        for(Map.Entry<PCT, ProgramConfigurationInformation<PCT>> e : progConfigInfoMap.entrySet()){
                            System.out.println("   " + e.getKey().toString() + " ==> " + e.getValue().toString());
                        }
                        System.exit(1);
                    }

                    int cvfSuccessorMaxRank = progConfigInfoMap.get(cvfSuccessor.getSuccessorProgramConfig()).getMaxRank();
                    int cvfMaxRankEffect = cvfSuccessorMaxRank - progConfigMaxRank;
                    int idOfChangedNode = cvfSuccessor.getIdOfChangedNode();

                    // increment count of cvfMaxRankEffect
                    addValueToDistribution(max_cvfOutsideInvRankEffectDistribution, cvfMaxRankEffect);
                    fullMaxAnaResults.incrementNumberOfCvfOutsideInv();
                    fullMaxAnaResults.increaseCvfOutsideInvTotalRankEffect(cvfMaxRankEffect);
                    addValueToDistribution(max_perNodeCvfOutsideInvRankEffectDistribution.elementAt(idOfChangedNode), cvfMaxRankEffect);

                    // for average rank
                    int cvfSuccessorAvgRank = progConfigInfoMap.get(cvfSuccessor.getSuccessorProgramConfig()).getAvgRank();
                    int cvfAvgRankEffect = cvfSuccessorAvgRank - progConfigAvgRank;
                    addValueToDistribution(avg_cvfOutsideInvRankEffectDistribution, cvfAvgRankEffect);
                    fullAvgAnaResults.incrementNumberOfCvfOutsideInv();
                    fullAvgAnaResults.increaseCvfOutsideInvTotalRankEffect(cvfAvgRankEffect);
                    addValueToDistribution(avg_perNodeCvfOutsideInvRankEffectDistribution.elementAt(idOfChangedNode), cvfAvgRankEffect);
                }
            }
        }
    }


    /**
     * Generate random configurations in the configuration space
     * @param sampleSize total number of random configuration to be generated
     * @param minNumberOfSampleInsideInv the amount of configuration inside the invariant to be generated
     *                                   we need this for large size problem since in that case most
     *                                   configurations are outside the invariant
     */
    void generateSampleConfigs(long sampleSize, long minNumberOfSampleInsideInv){
        long sampleCountInsideInv = 0;
        long sampleCountOutsideInv = 0;
        long startMs = System.currentTimeMillis();

        System.out.println("      Generating random samples ... ");

        // make sure to generate some configurations inside invariant
        while(sampleCountInsideInv < minNumberOfSampleInsideInv){
            PCT pc = (PCT) currentProgramConfig.getRandomProgramConfiguration().getDeepCopy();
            if(randMaxAnaResults.getProgConfigInfoMap().containsKey(pc)){
                //if pc is already sampled
                continue;
            }else {
                if(pc.isInsideInvariant()){
                    int initRank = 0;

                    randMaxAnaResults.getProgConfigInfoMap().put(
                            pc,
                            new ProgramConfigurationInformation<PCT>(
                                    initRank,
                                    initRank,
                                    BigInteger.valueOf(initRank),
                                    BigInteger.ONE,
                                    pc.getProgramConfigTransSuccessorList(),
                                    pc.getProgramConfigCvfSuccessorList()));

                    sampleCountInsideInv ++;
                }else{
                    // ignore
                    continue;
                }
            }
        }

//        while(sampleCountInsideInv < sampleSize && sampleCountOutsideInv < sampleSize){
        while(sampleCountInsideInv + sampleCountOutsideInv < sampleSize){
            PCT pc = (PCT) currentProgramConfig.getRandomProgramConfiguration().getDeepCopy();
            if(randMaxAnaResults.getProgConfigInfoMap().containsKey(pc)){
                //if pc is already sampled
                continue;
            }else {
                if(pc.isInsideInvariant()){
                    int initRank = 0;

                    randMaxAnaResults.getProgConfigInfoMap().put(
                            pc,
                            new ProgramConfigurationInformation<PCT>(
                                    initRank,
                                    initRank,
                                    BigInteger.valueOf(initRank),
                                    BigInteger.ONE,
                                    pc.getProgramConfigTransSuccessorList(),
                                    pc.getProgramConfigCvfSuccessorList()));

                    sampleCountInsideInv ++;
                }else{
                    int initRank = INFINITY_RANK;

                    randMaxAnaResults.getProgConfigInfoMap().put(
                            pc,
                            new ProgramConfigurationInformation<PCT>(
                                    initRank,
                                    DEFAULT_MIN_RANK,
                                    BigInteger.ZERO,
                                    BigInteger.ZERO, // no known path yet
                                    pc.getProgramConfigTransSuccessorList(),
                                    pc.getProgramConfigCvfSuccessorList()));

                    sampleCountOutsideInv ++;
                }
            }
        }

        System.out.println("       Done with sample generation in " + (System.currentTimeMillis() - startMs)/1000 + " secs");

    }

    /**
     * this function is basically the sample as generateSampleConfigs
     * except that, instead of choosing some random sample configs, it generates all configs
     * in the state space
     */
    void generateAllConfigs(){
        long startMs = System.currentTimeMillis();

        System.out.println("      Generating all configs in state space ... ");

        currentProgramConfig.moveToFirstProgramConfig();
        firstProgramConfig.moveToFirstProgramConfig();

        long configCount = 0;
        // iterate through the program configs
        do{
            // make deep copy
            PCT pc = (PCT) currentProgramConfig.getDeepCopy();

            if(pc.isInsideInvariant()){
                int initRank = 0;

                randMaxAnaResults.getProgConfigInfoMap().put(
                        pc,
                        new ProgramConfigurationInformation<PCT>(
                                initRank,
                                initRank,
                                BigInteger.valueOf(initRank),
                                BigInteger.ONE,
                                pc.getProgramConfigTransSuccessorList(),
                                pc.getProgramConfigCvfSuccessorList()));
            }else{
                int initRank = INFINITY_RANK;

                randMaxAnaResults.getProgConfigInfoMap().put(
                        pc,
                        new ProgramConfigurationInformation<PCT>(
                                initRank,
                                DEFAULT_MIN_RANK,
                                BigInteger.ZERO,
                                BigInteger.ZERO, // no known path yet
                                pc.getProgramConfigTransSuccessorList(),
                                pc.getProgramConfigCvfSuccessorList()));
            }

            // move to next state in some enumeration scheme
            currentProgramConfig.moveToNextProgramConfig();
            configCount ++;
        }while(! currentProgramConfig.equals(firstProgramConfig)); // until we finish a round

        System.out.println("       Done with generation of all " + configCount + " configs in " + (System.currentTimeMillis() - startMs)/1000 + " secs");

    }

    /**
     * Compute the rank effect of program transitions and cvfs by
     * randomly sample program transitions and cvfs
     * This function compute both max rank and average rank
     * Algo:
     *      // we may need a tree map to save rank of a state to avoid redo a computation
     *      // we may need to play with the value of SAMPLE_SIZE to see when the results
     *      // agree with full analysis
     *      // we need 2 primitives
     *      //    getRandomProgramConfig(): return a random program configuration in the state space
     *      //    getRank(): compute the (max) rank of a given program configuration
     *
     *      sample_count = 0;
     *      while sample_count < SAMPLE_SIZE
     *          pc = getRandomProgramConfig()
     *          if pc is already sampled
     *              continue
     *          else
     *             if pc is inside invariant
     *                  // only compute rank effect of cvf on pc
     *                  r_pc = getRank()
     *                  cvfs = get pc successor by cvf
     *                  for each cs in cvfs
     *                      r_cs = getRank()
     *                      r_diff_cvf = r_cs - r_pc
     *                  then compute the average of rank change r_diff_cvf
     *             else // pc is outside invariant
     *                  // compute rank effect of both program transition on pc and cvf
     *                  r_pc = getRank()
     *                  trans = get pc successors by program transitions
     *                  cvfs = get pc successors by cvf
     *                  for each ts in trans
     *                      r_ts = getRank()
     *                      r_diff_trans = r_ts - r_pc
     *                  then compute average of rank change r_diff (should be negative)
     *                  for each cs in cvfs
     *                      r_cs = getRank()
     *                      r_diff_cvf = r_cs - r_pc
     *                  then compute the average of rank change r_diff_cvf
     *
     */
    void randomComputeMaxAndAverageRankEffectOfProgramTransitionsAndCvfs(){
        randMaxAnaResults = new AnalysisResults<>(new TreeMap<>(), new TreeMap<>());
        randMaxAnaResults.initAnalysisResults();

        randAvgAnaResults = new AnalysisResults<>(new TreeMap<>(), new TreeMap<>());
        randAvgAnaResults.initAnalysisResults();

        int minNumberOfSampleInsideInv = 0;
        if(sampleSize > currentProgramConfig.getSizeOfStateSpace()) {
            sampleSize = currentProgramConfig.getSizeOfStateSpace()/1;
            minNumberOfSampleInsideInv = 0;
        }else{
            if(sampleSize >= 5000) {
                // requiring some minimal number of samples inside invariant could
                // take a lot of time to generate
                //minNumberOfSampleInsideInv = 50;
                minNumberOfSampleInsideInv = 0;
            }
        }

        System.out.println("      sampleSize = " + sampleSize);
        System.out.println("      minNumberOfSampleInsideInv = " + minNumberOfSampleInsideInv);

        // obtain sample_count random program configurations for both inside and outside invariant
        if(sampleSize > 0) {
            generateSampleConfigs(sampleSize, minNumberOfSampleInsideInv);
        }else{
            // sample size is the whole state space
            generateAllConfigs();
        }

        // auxiliary map for storing ranks of program configurations
        TreeMap<PCT, RankEvaluationResults> rankCache = new TreeMap<>();

        TreeMap<Integer, Integer> max_progTransOutsideInvRankEffectDistribution = randMaxAnaResults.getProgTransOutsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> max_cvfInsideInvRankEffectDistribution = randMaxAnaResults.getCvfInsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> max_cvfOutsideInvRankEffectDistribution = randMaxAnaResults.getCvfOutsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> max_progConfigRankDistribution = randMaxAnaResults.getProgConfigRankDistribution();
        Vector<TreeMap<Integer, Integer>> max_perNodeCvfOutsideInvRankEffectDistribution = randMaxAnaResults.getPerNodeCvfOutsideInvRankEffectDistribution();

        TreeMap<Integer, Integer> avg_progTransOutsideInvRankEffectDistribution = randAvgAnaResults.getProgTransOutsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> avg_cvfInsideInvRankEffectDistribution = randAvgAnaResults.getCvfInsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> avg_cvfOutsideInvRankEffectDistribution = randAvgAnaResults.getCvfOutsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> avg_progConfigRankDistribution = randAvgAnaResults.getProgConfigRankDistribution();
        Vector<TreeMap<Integer, Integer>> avg_perNodeCvfOutsideInvRankEffectDistribution = randAvgAnaResults.getPerNodeCvfOutsideInvRankEffectDistribution();

        // for progress display
        int numberOfPcs = randMaxAnaResults.getProgConfigInfoMap().keySet().size();
        int pcCount = 0;
        long startMs = System.currentTimeMillis();

        for(int nodeId = 0; nodeId < numberOfNodes; nodeId ++){
            // one element per node
            max_perNodeCvfOutsideInvRankEffectDistribution.addElement(new TreeMap<>());
            avg_perNodeCvfOutsideInvRankEffectDistribution.addElement(new TreeMap<>());
        }

//        int cvfContainsTransCount = 0;
//        int cvfNotContainsTransCount = 0;

        // computing the rank effect of program transitions and cvfs
        // on the sampled program configurations
        System.out.println("      Computing rank effect on sampled configurations");
        for(PCT pc : randMaxAnaResults.getProgConfigInfoMap().keySet()){
            int pcMaxRank;
            int pcAvgRank;
            ProgramConfigurationInformation pcInfo = randMaxAnaResults.getProgConfigInfoMap().get(pc);

            if(pc.isInsideInvariant()){
                pcMaxRank = pcInfo.getMaxRank(); // should be 0
                pcAvgRank = pcMaxRank;

                if(pcMaxRank != 0){
                    System.out.println("ERROR: pcMaxRank of configuration inside invariant is not 0 but " + pcMaxRank);
                    System.out.println("   pc = " + pc.toString());
                }

                updateRankCache(rankCache, pc, new RankEvaluationResults(pcMaxRank));

                // program transition doesn't change rank of pc inside invariant
                // thus we only compute rank effect of cvf on pc
                TreeSet<SuccessorInfo> cvfs = pcInfo.getCvfSuccessorList();
                int totalCvfMaxRankOffset = 0;

                for(SuccessorInfo cvfSuccessor : cvfs){
                    ProgramConfigurationTemplate cs = cvfSuccessor.getSuccessorProgramConfig();
                    if(ThreadLocalRandom.current().nextDouble() >= randomCvfNbrProb){
                        continue; // skip this cs
                    }

                    RankEvaluationResults rankEval = cs.evaluateRanks(rankCache);
                    int csMaxRank = rankEval.getMaxRank();
                    int csAvgRank = rankEval.getAvgRank();
                    int maxRankOffset = csMaxRank - pcMaxRank;
                    int avgRankOffset = csAvgRank - pcMaxRank;

                    addValueToDistribution(max_cvfInsideInvRankEffectDistribution, maxRankOffset);
                    randMaxAnaResults.incrementNumberOfCvfInsideInv();
                    randMaxAnaResults.increaseCvfInsideInvTotalRankEffect(maxRankOffset);

                    addValueToDistribution(avg_cvfInsideInvRankEffectDistribution, avgRankOffset);
                    randAvgAnaResults.incrementNumberOfCvfInsideInv();
                    randAvgAnaResults.increaseCvfInsideInvTotalRankEffect(avgRankOffset);

                    totalCvfMaxRankOffset += maxRankOffset;
                }
                pcInfo.setTotalCvfMaxRankOffset(totalCvfMaxRankOffset);

            }else{
                // pc is outside invariant
                // we compute the rank effect of both program transitions and cvfs
                RankEvaluationResults pcRankEval = pc.evaluateRanks(rankCache);
                pcMaxRank = pcRankEval.getMaxRank();
                pcAvgRank = pcRankEval.getAvgRank();
                pcInfo.setMaxRank(pcMaxRank);

//                boolean pcSanityCheck = pcRankEval.sanityCheck();
//                if(!pcSanityCheck){
//                    System.out.println("   pcCount = " + pcCount + "  pc = " + pc.toString());
//                }

                TreeSet<SuccessorInfo> trans = pcInfo.getProgSuccessorList();
                TreeSet<SuccessorInfo> cvfs = pcInfo.getCvfSuccessorList();

//                if(cvfs.containsAll(trans)){
//                    cvfContainsTransCount ++;
//                }else{
//                    cvfNotContainsTransCount ++;
//                }

                for(SuccessorInfo tSuccessor : trans){
                    ProgramConfigurationTemplate ts = tSuccessor.getSuccessorProgramConfig();
                    if(ThreadLocalRandom.current().nextDouble() >= randomTransNbrProb){
                        continue; // skip this cs
                    }

                    // obtain rank of transition successor
                    RankEvaluationResults tsRankEval = ts.evaluateRanks(rankCache);
                    int tsMaxRank = tsRankEval.getMaxRank();
                    int tsAvgRank = tsRankEval.getAvgRank();
                    int maxRankOffset = tsMaxRank - pcMaxRank;
                    int avgRankOffset = tsAvgRank - pcAvgRank;

                    addValueToDistribution(max_progTransOutsideInvRankEffectDistribution, maxRankOffset);
                    randMaxAnaResults.incrementNumberOfProgTransOutsideInv();
                    randMaxAnaResults.increaseProgTransOutsideInvTotalRankEffect(maxRankOffset);

                    addValueToDistribution(avg_progTransOutsideInvRankEffectDistribution, avgRankOffset);
                    randAvgAnaResults.incrementNumberOfProgTransOutsideInv();
                    randAvgAnaResults.increaseProgTransOutsideInvTotalRankEffect(avgRankOffset);
                }

                for(SuccessorInfo cSuccessor : cvfs){
                    ProgramConfigurationTemplate cs = cSuccessor.getSuccessorProgramConfig();
                    if(ThreadLocalRandom.current().nextDouble() >= randomCvfNbrProb){
                        continue; // skip this cs
                    }

                    // obtain rank of cvf successor
                    RankEvaluationResults csRankEval = cs.evaluateRanks(rankCache);
                    int csMaxRank = csRankEval.getMaxRank();
                    int csAvgRank = csRankEval.getAvgRank();
                    int maxRankOffset = csMaxRank - pcMaxRank;
                    int avgRankOffset = csAvgRank - pcAvgRank;
                    int idOfChangedNode = cSuccessor.getIdOfChangedNode();

                    addValueToDistribution(max_cvfOutsideInvRankEffectDistribution, maxRankOffset);
                    randMaxAnaResults.incrementNumberOfCvfOutsideInv();
                    randMaxAnaResults.increaseCvfOutsideInvTotalRankEffect(maxRankOffset);
                    addValueToDistribution(max_perNodeCvfOutsideInvRankEffectDistribution.elementAt(idOfChangedNode), maxRankOffset);

                    addValueToDistribution(avg_cvfOutsideInvRankEffectDistribution, avgRankOffset);
                    randAvgAnaResults.incrementNumberOfCvfOutsideInv();
                    randAvgAnaResults.increaseCvfOutsideInvTotalRankEffect(avgRankOffset);
                    addValueToDistribution(avg_perNodeCvfOutsideInvRankEffectDistribution.elementAt(idOfChangedNode), avgRankOffset);
                }
            }

            // update rank distribution
            addValueToDistribution(max_progConfigRankDistribution, pcMaxRank);
            addValueToDistribution(avg_progConfigRankDistribution, pcAvgRank);

            pcCount ++;

            long rankCacheSize = rankCache.size();

            // show progress
            System.out.print(String.format("\r%s",
                    String.format("         [ %d/%d    %5.1f %%    %5.1f microsec/pc %d ]     ",
                            pcCount,
                            numberOfPcs,
                            ((100*pcCount)/(double) numberOfPcs),
                            (System.currentTimeMillis() - startMs)/((double) pcCount),
                            rankCacheSize
                    )));

            // clear rankCache to avoid out of memory error
            if(rankCacheSize > TREE_MAP_THRESHOLD_FOR_CLEARANCE){
                rankCache.clear();
            }

        }

        System.out.println();

        try {
            DebugInfo.debugFile.write("rankCache.size() = " + rankCache.size() + "\n");
//            DebugInfo.debugFile.write("cvfContainsTransCount = " + cvfContainsTransCount + "\n");
//            DebugInfo.debugFile.write("cvfNotContainsTransCount = " + cvfNotContainsTransCount + "\n");
            DebugInfo.debugFile.write(DebugInfo.getDebugInfoStr());
            DebugInfo.debugFile.flush();
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
    }

//    /**
//     * Compute the rank effect of program transitions and cvfs by
//     * randomly sample program transitions and cvfs
//     * This function is similar to randomComputeMaxRankEffectOfProgramTransitionsAndCvfs
//     * except that it only computes max rank, for reason of speed.
//     */
//    void randomComputeMaxRankEffectOfProgramTransitionsAndCvfs(){
//        randMaxAnaResults = new AnalysisResults<>(new TreeMap<>(), new TreeMap<>());
//        randMaxAnaResults.initAnalysisResults();
//
//        int minNumberOfSampleInsideInv = 0;
//        if(sampleSize > currentProgramConfig.getSizeOfStateSpace()) {
//            sampleSize = currentProgramConfig.getSizeOfStateSpace() / 3;
//            minNumberOfSampleInsideInv = 0;
//        }else{
//            if(sampleSize >= 5000) {
//                minNumberOfSampleInsideInv = 50;
//            }
//        }
//
//        System.out.println("      sampleSize = " + sampleSize);
//        System.out.println("      minNumberOfSampleInsideInv = " + minNumberOfSampleInsideInv);
//
//        // obtain sample_count random program configurations for both inside and outside invariant
//        generateSampleConfigs(sampleSize, minNumberOfSampleInsideInv);
//
//        // auxiliary map for storing ranks of program configurations
//        TreeMap<PCT, RankEvaluationResults> rankCache = new TreeMap<>();
//
//        TreeMap<Integer, Integer> max_progTransOutsideInvRankEffectDistribution = randMaxAnaResults.getProgTransOutsideInvRankEffectDistribution();
//        TreeMap<Integer, Integer> max_cvfInsideInvRankEffectDistribution = randMaxAnaResults.getCvfInsideInvRankEffectDistribution();
//        TreeMap<Integer, Integer> max_cvfOutsideInvRankEffectDistribution = randMaxAnaResults.getCvfOutsideInvRankEffectDistribution();
//        TreeMap<Integer, Integer> max_progConfigRankDistribution = randMaxAnaResults.getProgConfigRankDistribution();
//
//        // for progress display
//        int numberOfPcs = randMaxAnaResults.getProgConfigInfoMap().keySet().size();
//        int pcCount = 0;
//        long startMs = System.currentTimeMillis();
//
//        // computing the rank effect of program transitions and cvfs
//        // on the sampled program configurations
//        System.out.println("      Computing rank effect on sampled configurations");
//        for(PCT pc : randMaxAnaResults.getProgConfigInfoMap().keySet()){
//            int pcMaxRank;
//            int pcAvgRank;
//            ProgramConfigurationInformation pcInfo = randMaxAnaResults.getProgConfigInfoMap().get(pc);
//
//            if(pc.isInsideInvariant()){
//                pcMaxRank = pcInfo.getMaxRank(); // should be 0
//                pcAvgRank = pcMaxRank;
//
//                if(pcMaxRank != 0){
//                    System.out.println("ERROR: pcMaxRank of configuration inside invariant is not 0 but " + pcMaxRank);
//                    System.out.println("   pc = " + pc.toString());
//                }
//
//                //rankCache.put(pc, new RankEvaluationResults(pcMaxRank));
//                //new RankEvaluationResults(pcMaxRank).updateRankCache(rankCache, pc);
//                updateRankCache(rankCache, pc, new RankEvaluationResults(pcMaxRank));
//
//                // program transition doesn't change rank of pc inside invariant
//                // thus we only compute rank effect of cvf on pc
//                TreeSet<PCT> cvfs = pcInfo.getCvfSuccessorList();
//                int totalCvfMaxRankOffset = 0;
//
//                for(PCT cs : cvfs){
//                    RankEvaluationResults rankEval = cs.evaluateMaxRank(rankCache);
//                    int csMaxRank = rankEval.getMaxRank();
//                    int maxRankOffset = csMaxRank - pcMaxRank;
//
//                    addValueToDistribution(max_cvfInsideInvRankEffectDistribution, maxRankOffset);
//                    randMaxAnaResults.incrementNumberOfCvfInsideInv();
//                    randMaxAnaResults.increaseCvfInsideInvTotalRankEffect(maxRankOffset);
//
//                    totalCvfMaxRankOffset += maxRankOffset;
//                }
//                pcInfo.setTotalCvfMaxRankOffset(totalCvfMaxRankOffset);
//            }else{
//                // pc is outside invariant
//                // we compute the rank effect of both program transitions and cvfs
//                RankEvaluationResults pcRankEval = pc.evaluateMaxRank(rankCache);
//                pcMaxRank = pcRankEval.getMaxRank();
//                pcInfo.setMaxRank(pcMaxRank);
//
//                if(!pcRankEval.sanityCheck()){
//                    System.out.println("   pcCount = " + pcCount + "  pc = " + pc.toString());
//                }
//
//
//                TreeSet<ProgramConfigurationTemplate> trans = pcInfo.getProgSuccessorList();
//                TreeSet<ProgramConfigurationTemplate> cvfs = pcInfo.getCvfSuccessorList();
//
//                for(ProgramConfigurationTemplate ts : trans){
//                    // obtain rank of transition successor
//                    RankEvaluationResults tsRankEval = ts.evaluateMaxRank(rankCache);
//                    int tsMaxRank = tsRankEval.getMaxRank();
//                    int maxRankOffset = tsMaxRank - pcMaxRank;
//
//                    addValueToDistribution(max_progTransOutsideInvRankEffectDistribution, maxRankOffset);
//                    randMaxAnaResults.incrementNumberOfProgTransOutsideInv();
//                    randMaxAnaResults.increaseProgTransOutsideInvTotalRankEffect(maxRankOffset);
//                }
//
//                for(ProgramConfigurationTemplate cs : cvfs){
//                    // obtain rank of cvf successor
//                    RankEvaluationResults csRankEval = cs.evaluateMaxRank(rankCache);
//                    int csMaxRank = csRankEval.getMaxRank();
//                    int maxRankOffset = csMaxRank - pcMaxRank;
//
//                    addValueToDistribution(max_cvfOutsideInvRankEffectDistribution, maxRankOffset);
//                    randMaxAnaResults.incrementNumberOfCvfOutsideInv();
//                    randMaxAnaResults.increaseCvfOutsideInvTotalRankEffect(maxRankOffset);
//
//                }
//            }
//
//            // update rank distribution
//            addValueToDistribution(max_progConfigRankDistribution, pcMaxRank);
//
//            pcCount ++;
//
//            // show progress
//            System.out.print(String.format("\r%s",
//                    String.format("         [ %d/%d    %5.1f %%    %5.1f microsec/pc ]     ",
//                            pcCount,
//                            numberOfPcs,
//                            ((100*pcCount)/(double) numberOfPcs),
//                            (System.currentTimeMillis() - startMs)/((double) pcCount)
//                    )));
//
//        }
//
//        System.out.println();
//
//        try {
//            DebugInfo.debugFile.write("rankCache.size() = " + rankCache.size() + "\n");
//            DebugInfo.debugFile.write(DebugInfo.getDebugInfoStr());
//            DebugInfo.debugFile.flush();
//        }catch(IOException e){
//            System.out.println(e.getMessage());
//        }
//    }

    @Override
    // overriding superclass method
    void runAnalyzeProgram() throws Exception {
        System.out.println(" Analyzing program based on configurations ");

        long startMs = System.currentTimeMillis();
        long currrentMs = startMs;


        if(configBaseAnalysisTask.contains("full")){
            // Full analysis, max-rank based is requested
            System.out.println("  + Full Analysis");
            System.out.println("    Initializing ... ");
            init();
            System.out.println("    init is done in " + ((System.currentTimeMillis() - currrentMs)/1000) + " seconds");

//            currrentMs = System.currentTimeMillis();
//            System.out.println("\n    Fully compute max ranks for program configurations... ");
//            fullComputeMaxRankForConfigs();
//            System.out.println("    compute max ranks is done " + ((System.currentTimeMillis() - currrentMs)/1000) + " seconds");

            currrentMs = System.currentTimeMillis();
            System.out.println("\n    Fully compute max rank and average rank for program configurations... ");
            fullComputeMaxAndAverageRankForConfigs();
            System.out.println("    compute max rank and average rank is done " + ((System.currentTimeMillis() - currrentMs)/1000) + " seconds");

            currrentMs = System.currentTimeMillis();
            System.out.println("\n    Getting full max rank distribution of program configurations ");
            displayProgConfigRankDistribution(fullMaxAnaResults, "full-max");

            currrentMs = System.currentTimeMillis();
            System.out.println("\n    Getting full average rank distribution of program configurations ");
            displayProgConfigRankDistribution(fullAvgAnaResults, "full-avg");

            fullComputeRankEffectOfProgramTransitionsAndCvfs();

            System.out.println("\n    Getting full effect on max rank change of program transitions and cvfs ");
            displayRankEffectOfProgramTransitionsAndCvfs(fullMaxAnaResults, "full-max");

            System.out.println("\n    Getting full effect on average rank change of program transitions and cvfs ");
            displayRankEffectOfProgramTransitionsAndCvfs(fullAvgAnaResults, "full-avg");

            System.out.println("    Full analysis is done " + ((System.currentTimeMillis() - currrentMs)/1000) + " seconds");
            currrentMs = System.currentTimeMillis();
        }

        if(configBaseAnalysisTask.contains("statistical")){
            // Statistical analysis
            System.out.println("\n  + Statistical Analysis");
            System.out.println("\n    Randomly compute rank effects for program configurations and cvfs... ");
            cleanSanityCheckSummary();
            randomComputeMaxAndAverageRankEffectOfProgramTransitionsAndCvfs();

            System.out.println("      Random compute ranks is done " + ((System.currentTimeMillis() - currrentMs)/1000) + " seconds");
            currrentMs = System.currentTimeMillis();

            // max-rank based from random paths
            System.out.println("\n    Getting statistical max rank distribution of program configurations ");
            displayProgConfigRankDistribution(randMaxAnaResults, "rand-max");
            System.out.println("     max rank = " + randMaxAnaResults.getProgConfigRankDistribution().lastKey());

            System.out.println("\n    Getting statistical effect on max-rank change of program transitions and cvfs ");
            displayRankEffectOfProgramTransitionsAndCvfs(randMaxAnaResults, "rand-max");
            System.out.println("     trans outside invariant = " + randMaxAnaResults.getNumberOfProgTransOutsideInv());
            System.out.println("     cvfs = " + randMaxAnaResults.getNumberOfCvf());
            System.out.println("       inside invariant = " + randMaxAnaResults.getNumberOfCvfInsideInv());
            System.out.println("       outside invariant = " + randMaxAnaResults.getNumberOfCvfOutsideInv());

            displaySanityCheckSummary();

            // verifying if full analysis and statistical analysis matches for max rank
            if(configBaseAnalysisTask.contains("full")) {
                if (fullMaxAnaResults.isEqualToOther(randMaxAnaResults)) {
                    System.out.println("\n    *** Full analysis MATCHES statistical analysis for max-rank");
                } else {
                    System.out.println("\n    *** Full analysis DOES NOT MATCH statistical analysis for max-rank");
                }
            }

            // average-rank based from random paths
            System.out.println("\n    Getting statistical avg rank distribution of program configurations ");
            displayProgConfigRankDistribution(randAvgAnaResults, "rand-avg");
            System.out.println("     max rank = " + randAvgAnaResults.getProgConfigRankDistribution().lastKey());

            System.out.println("\n    Getting statistical effect on avg-rank change of program transitions and cvfs ");
            displayRankEffectOfProgramTransitionsAndCvfs(randAvgAnaResults, "rand-avg");
            System.out.println("     trans outside invariant = " + randAvgAnaResults.getNumberOfProgTransOutsideInv());
            System.out.println("     cvfs = " + randAvgAnaResults.getNumberOfCvf());

            // verification of avg-rank is not needed since full analysis is for max-rank
            // verifying if full analysis and statistical analysis matches for average rank
            if(configBaseAnalysisTask.contains("full")){
                if(fullAvgAnaResults.isEqualToOther(randAvgAnaResults)){
                    System.out.println("\n    *** Full analysis MATCHES statistical analysis for average-rank");
                }else{
                    System.out.println("\n    *** Full analysis DOES NOT MATCH statistical analysis for average-rank");
                }
            }

            long endMs = System.currentTimeMillis();
            System.out.println("\n  + Program end time " + DateFormat.getDateTimeInstance().format(endMs));
            System.out.println(String.format("    duration: %.2f seconds", (endMs - startMs)/1000.0));

//        System.out.println("\n++results: ");
//        ap.displayProgramConfigInfoMap(ap.progConfigInfoMap, 5, true);

        }

    }
}
