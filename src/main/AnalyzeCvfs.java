package main;

import coloring.ProgramConfigurationColoring;
import datastructure.AnalysisResults;
import datastructure.DebugInfo;
import dijkstra3states.ProgramConfigurationDijkstra3States;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import maxmatching.ProgramConfigurationMaxMatching;
import template.ProgramConfigurationTemplate;

import java.io.*;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import static main.Utility.averageToStr;
import static main.Utility.readGraphTopology;
import static template.ProgramConfigurationTemplate.*;

/**
 * Main program for analyzing cvfs
 */

public class AnalyzeCvfs<PCT extends ProgramConfigurationTemplate> {
    public static final String PROGRAM_NAME_DIJKSTRA_3_STATES = "dijkstra-3-states-program";
    public static final String PROGRAM_NAME_MAX_MATCHING = "max-matching";
    public static final String PROGRAM_NAME_COLORING = "coloring";

    public static final String ANALYSIS_MODE_CONFIG_BASE = "config-base";
    public static final String ANALYSIS_MODE_TRANS_BASE = "trans-base";

    // If some tree map structure reaches this size, we should clear it
    // to avoid out of memory error.
    public static final int TREE_MAP_THRESHOLD_FOR_CLEARANCE = 2_000_000;

    protected int runId;
    protected int numberOfNodes;
    protected String programName;
    protected String outputFileNamePrefix;
    protected int cvf;

    // graph topology in form of adjacent matrix
    public static HashMap<Integer, Vector<Integer>> graphTopology;
    public static int maxDegree;

    // for statistical analysis (random sampling)
    protected long sampleSize;  // how many program configurations to be sampled
    protected long probeLimit;  // maximum how many paths from a sampled configuration to the invariant to be probed

    protected PCT currentProgramConfig;
    protected PCT firstProgramConfig;

    protected AnalyzeCvfs(
            int runId,
            int numberOfNodes,
            int cvf,
            String programName,
            String outputFileName,
            long sampleSize,
            long probeLimit,
            PCT currentProgramConfig,
            PCT firstProgramConfig
            ){

        this.runId = runId;
        this.numberOfNodes = numberOfNodes;
        this.cvf = cvf;
        this.programName = programName;
        this.outputFileNamePrefix = outputFileName;
        this.sampleSize = sampleSize;
        this.probeLimit = probeLimit;
        this.currentProgramConfig = currentProgramConfig;
        this.firstProgramConfig = firstProgramConfig;

    }
    // main program
    public static void main(String args[]) throws Exception {

        // parsing input arguments
        OptionParser mainParser = new OptionParser();

        OptionSpec analysisModeSpec = mainParser.accepts("analysis-mode")
                .withRequiredArg();
        OptionSpec configBaseAnalysisTaskSpec = mainParser.accepts("config-base-analysis-task")
                .withRequiredArg();

        OptionSpec programNameSpec = mainParser.accepts("program-name")
                .withRequiredArg();
        OptionSpec<Integer> numberOfNodesSpec = mainParser.accepts("number-of-nodes")
                .withRequiredArg()
                .ofType(Integer.class);
        OptionSpec<Integer> runIdSpec = mainParser.accepts("run-id")
                .withRequiredArg()
                .ofType(Integer.class);
        OptionSpec graphTopologyFileNameSpec = mainParser.accepts("graph-topology-filename")
                .withRequiredArg();
        OptionSpec outputFileNamePrefixSpec = mainParser.accepts("output-filename-prefix")
                .withRequiredArg();
        OptionSpec debugFileNamePrefixSpec = mainParser.accepts("debug-filename-prefix")
                .withRequiredArg();
        OptionSpec<String> cvfStrSpec = mainParser.accepts("cvf")
                .withRequiredArg();
        OptionSpec<Long> sampleSizeSpec = mainParser.accepts("sample-size")
                .withRequiredArg()
                .ofType(Long.class);
        OptionSpec<Integer> probeLimitSpec = mainParser.accepts("probe-limit")
                .withRequiredArg()
                .ofType(Integer.class);
        OptionSpec<Double> randomTransNbrProbabilitySpec = mainParser.accepts("random-trans-nbr-prob")
                .withOptionalArg()
                .ofType(Double.class);
        OptionSpec<Double> randomCvfNbrProbabilitySpec = mainParser.accepts("random-cvf-nbr-prob")
                .withOptionalArg()
                .ofType(Double.class);


        OptionSet mainOption = mainParser.parse(args);
        int numberOfNodes;
        int runId = mainOption.valueOf(runIdSpec);
        long sampleSize = mainOption.valueOf(sampleSizeSpec);
        int cvf;
        switch(mainOption.valueOf(cvfStrSpec)){
            case "arbitrary-perturb":
                cvf = CVF_AS_ARBITRARY_PERTURBATION;
                break;
            case "constrained-perturb":
                cvf = CVF_AS_CONSTRAINED_PERTURBATION;
                break;
            case "constrained-perturb-and-topology-restriction":
                cvf = CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION;
                break;
            default:
                throw new Exception("Unknown known cvf: " + mainOption.valueOf(cvfStrSpec));

        }
        String analysisMode = (String) mainOption.valueOf(analysisModeSpec);
        String configBaseAnalysisTask = (String) mainOption.valueOf(configBaseAnalysisTaskSpec);
        String programName = (String) mainOption.valueOf(programNameSpec);
        String graphTopologyFileName = (String) mainOption.valueOf(graphTopologyFileNameSpec);
        String outputFileNamePrefix = (String) mainOption.valueOf(outputFileNamePrefixSpec);
        String debugFileNamePrefix = (String) mainOption.valueOf(debugFileNamePrefixSpec);
        int probeLimit = mainOption.valueOf(probeLimitSpec);

        double randomTransNbrProb = (randomTransNbrProbabilitySpec == null?
                1.0 :
                mainOption.valueOf(randomTransNbrProbabilitySpec));
        double randomCvfNbrProb = (randomCvfNbrProbabilitySpec == null?
                1.0 :
                mainOption.valueOf(randomCvfNbrProbabilitySpec));



        if(graphTopologyFileName.trim().contains("implicit_topology.txt")){
            // graph topology is not explicitly given
            numberOfNodes = mainOption.valueOf(numberOfNodesSpec);
            graphTopology = null;
            // max degree may be not relevant or it will be inferred from program context
        }else{
            maxDegree = 0;
            if((graphTopology = readGraphTopology(graphTopologyFileName)) == null){
                throw new Exception("Error when reading graph topology");
            }

            numberOfNodes = graphTopology.size();
            for(Map.Entry<Integer,Vector<Integer>> entry : graphTopology.entrySet()){
                Vector<Integer> nbrList = entry.getValue();
                if(maxDegree < nbrList.size()){
                    maxDegree = nbrList.size();
                }
            }
        }

        if(numberOfNodes <= 1){
            throw new Exception("The number of nodes = " + numberOfNodes + ". Distributed system has at least 2 nodes");
        }


        // Done with parsing input arguments
        System.out.println("++ Program start time:      " + DateFormat.getDateTimeInstance().format(System.currentTimeMillis()));
        System.out.println("     analysis mode:         " + analysisMode);
        if(analysisMode.equals(ANALYSIS_MODE_CONFIG_BASE)){
            System.out.println("       config base analysis task:  " + configBaseAnalysisTask);
        }

        System.out.println("     program name:          " + programName);
        System.out.println("     run id:                " + runId);
        System.out.println("     graph topology file:   " + graphTopologyFileName);
        System.out.println("     number of nodes:       " + numberOfNodes);
        System.out.println("     cvf perturbation type: " + cvf);
        System.out.println("     (max) sampleSize:      " + sampleSize);
        System.out.println("     probeLimit:            " + probeLimit);
        System.out.println("     randomCvfNbrProb:      " + randomCvfNbrProb);
        System.out.println("     randomTransNbrProb:    " + randomTransNbrProb);
        System.out.println();



        if (probeLimit == PROBE_LIMIT_OPTION_ADAPTIVE) {
            probeLimit = 100 * numberOfNodes;
        }

        ProgramConfigurationTemplate currentConfig = null;
        ProgramConfigurationTemplate firstConfig = null;

        switch (programName) {
            case PROGRAM_NAME_MAX_MATCHING:
                currentConfig = new ProgramConfigurationMaxMatching(numberOfNodes, new TreeMap<>(), cvf, probeLimit);
                firstConfig = new ProgramConfigurationMaxMatching(numberOfNodes, new TreeMap<>(), cvf, probeLimit);
                break;

            case PROGRAM_NAME_DIJKSTRA_3_STATES:
                currentConfig = new ProgramConfigurationDijkstra3States(numberOfNodes, new TreeMap<>(), cvf, probeLimit);
                firstConfig = new ProgramConfigurationDijkstra3States(numberOfNodes, new TreeMap<>(), cvf, probeLimit);
                break;

            case PROGRAM_NAME_COLORING:
                currentConfig = new ProgramConfigurationColoring(numberOfNodes, new TreeMap<>(), cvf, probeLimit);
                firstConfig = new ProgramConfigurationColoring(numberOfNodes, new TreeMap<>(), cvf, probeLimit);
                break;

            default:
                currentConfig = null;
                firstConfig = null;
                throw new Exception("Unknown program name: " + programName);
        }

        if(sampleSize > currentConfig.getSizeOfStateSpace()){
            sampleSize = currentConfig.getSizeOfStateSpace();
            System.out.println("   update sampleSize to " + sampleSize);
        }


        AnalyzeCvfs ap;
        switch(analysisMode){
            case ANALYSIS_MODE_TRANS_BASE:
                ap = new AnalyzeProgramBasedOnTransitions(
                        runId,
                        numberOfNodes,
                        cvf,
                        programName,
                        outputFileNamePrefix,
                        sampleSize,
                        probeLimit,
                        currentConfig,
                        firstConfig);

                break;

            case ANALYSIS_MODE_CONFIG_BASE:
                ap = new AnalyzeProgramBasedOnConfigurations(
                        runId,
                        numberOfNodes,
                        cvf,
                        programName,
                        outputFileNamePrefix,
                        sampleSize,
                        probeLimit,
                        currentConfig,
                        firstConfig,
                        randomTransNbrProb,
                        randomCvfNbrProb,
                        configBaseAnalysisTask);

                break;
            default:
                throw new Exception("Unknown analysis mode: " + analysisMode);
        }


        DebugInfo.debugFile = new BufferedWriter(new PrintWriter(debugFileNamePrefix + "-debug.txt"));

        ap.runAnalyzeProgram();

        DebugInfo.debugFile.close();

    }


    void runAnalyzeProgram() throws Exception {
        System.out.println("WARNING: This method should be overriden by subclass");
    }

    // common methods used by subclasses
    /**
     * Display the rank distribution of program configurations,
     * i.e. content of progConfigRankDistribution
     * @param anaResults: results of an analysis
     * @param resultType: more information about the above analysis results,
     *                  i.e. whether it is random max, or random average, or full
     */
    public void displayProgConfigRankDistribution(AnalysisResults<PCT> anaResults, String resultType){
        TreeMap<Integer, Integer> progConfigRankDistribution = anaResults.getProgConfigRankDistribution();
        //TreeMap<T, ProgramConfigurationInformation<T>> progConfigInfoMap = anaResults.getProgConfigInfoMap();
        int numberOfPcs = 0;
        for(Map.Entry<Integer,Integer> entry : progConfigRankDistribution.entrySet()){
            numberOfPcs += entry.getValue();
        }

        BufferedWriter bufferedWriter;

        try{
            if (outputFileNamePrefix.equals("stdout"))
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));
            else
                bufferedWriter = new BufferedWriter(new PrintWriter(outputFileNamePrefix + "-rank-dist-prog-config-" + resultType + ".txt"));

            int maxRank = progConfigRankDistribution.lastKey();

            bufferedWriter.write("#   program name:                     " + programName + "\n");
            bufferedWriter.write("#   number of nodes:                  " + numberOfNodes + "\n");
            bufferedWriter.write("#   number of program configurations: " + numberOfPcs + "\n");
            bufferedWriter.write("#   max rank:                         " + maxRank + "\n");
            bufferedWriter.write("#         rank       count\n");
            bufferedWriter.write("#   ----------  ----------\n");
            for(Map.Entry<Integer, Integer> entry : progConfigRankDistribution.entrySet()){
                int progConfigRank = entry.getKey();

                bufferedWriter.write(String.format("    %10d  %10d\n",
                        progConfigRank,
                        entry.getValue()));

            }

            bufferedWriter.flush();

            // you probably do not want to close stdout
            // since all latter invocation of System.out.println() will go nowhere
            if(!outputFileNamePrefix.equals("stdout"))
                bufferedWriter.close();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Display content of programTransitionRankEffectDistribution and cvfRankEffectDistribution
     * @param anaResults: results of an analysis
     * @param resultType: more information the above analysis results,
     *                  i.e. whether it is full max, full average, random max, or random average, etc.
     */
    public void displayRankEffectOfProgramTransitionsAndCvfs(AnalysisResults<PCT> anaResults, String resultType) {
        TreeMap<Integer, Integer> progTransOutsideInvRankEffectDistribution = anaResults.getProgTransOutsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> cvfInsideInvRankEffectDistribution = anaResults.getCvfInsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> cvfOutsideInvRankEffectDistribution = anaResults.getCvfOutsideInvRankEffectDistribution();

        if(progTransOutsideInvRankEffectDistribution == null){
            System.out.println("Rank effect of program transitions has not been computed");
            return;
        }


        if(cvfInsideInvRankEffectDistribution == null || cvfOutsideInvRankEffectDistribution == null){
            System.out.println("Rank effect of cvfs has not been computed");
            return;
        }


        BufferedWriter bufferedWriter;

        try{
            if (outputFileNamePrefix.equals("stdout")) {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));
            }else {
                bufferedWriter = new BufferedWriter(new PrintWriter(outputFileNamePrefix +
                        "-rank-effect-dist-prog-transition-and-cvf-" + resultType + ".txt"));
            }

            Integer minProgTransOutsideInvRankEffect,
                    maxProgTransOutsideInvRankEffect,
                    minCvfInsideInvRankEffect,
                    maxCvfInsideInvRankEffect,
                    minCvfOutsideInvRankEffect,
                    maxCvfOutsideInvRankEffect;


            if(progTransOutsideInvRankEffectDistribution.isEmpty()){
                minProgTransOutsideInvRankEffect = Integer.MAX_VALUE;
                maxProgTransOutsideInvRankEffect = Integer.MIN_VALUE;
            }else{
                minProgTransOutsideInvRankEffect = progTransOutsideInvRankEffectDistribution.firstKey();
                maxProgTransOutsideInvRankEffect = progTransOutsideInvRankEffectDistribution.lastKey();
            }
            if(cvfInsideInvRankEffectDistribution.isEmpty()){
                minCvfInsideInvRankEffect = Integer.MAX_VALUE;
                maxCvfInsideInvRankEffect = Integer.MIN_VALUE;
            }else{
                minCvfInsideInvRankEffect = cvfInsideInvRankEffectDistribution.firstKey();
                maxCvfInsideInvRankEffect = cvfInsideInvRankEffectDistribution.lastKey();
            }
            if(cvfOutsideInvRankEffectDistribution.isEmpty()){
                minCvfOutsideInvRankEffect = Integer.MAX_VALUE;
                maxCvfOutsideInvRankEffect = Integer.MIN_VALUE;
            }else {
                minCvfOutsideInvRankEffect = cvfOutsideInvRankEffectDistribution.firstKey();
                maxCvfOutsideInvRankEffect = cvfOutsideInvRankEffectDistribution.lastKey();
            }

            int minCvfRankEffect = Math.min(minCvfInsideInvRankEffect, minCvfOutsideInvRankEffect);
            int minRankEffect = Math.min(minCvfRankEffect, minProgTransOutsideInvRankEffect);
            int maxCvfRankEffect = Math.max(maxCvfInsideInvRankEffect, maxCvfOutsideInvRankEffect);
            int maxRankEffect = Math.max(maxCvfRankEffect, maxProgTransOutsideInvRankEffect);
            int numberOfCvfs = anaResults.getNumberOfCvf();
            int cvfTotalRankEffect = anaResults.getCvfTotalRankEffect();

            // compute effect of adverse cvf
            double adverseCvfInsideInvTotalRankEffect = 0;
            int adverseCvfInsideInvCount = 0;
            double adverseCvfOutsideInvTotalRankEffect = 0;
            int adverseCvfOutsideInvCount = 0;
            int adverseCvfsCount = 0;
            double adverseCvfTotalRankEffect = 0;
            for(Map.Entry<Integer, Integer> entry : cvfInsideInvRankEffectDistribution.entrySet()){
                int rankEffect = entry.getKey();
                if(rankEffect > 0){
                    adverseCvfInsideInvCount += entry.getValue();
                    adverseCvfInsideInvTotalRankEffect += rankEffect * entry.getValue();
                }
            }

            for(Map.Entry<Integer, Integer> entry: cvfOutsideInvRankEffectDistribution.entrySet()){
                int rankEffect = entry.getKey();
                if(rankEffect > 0){
                    adverseCvfOutsideInvCount += entry.getValue();
                    adverseCvfOutsideInvTotalRankEffect += rankEffect * entry.getValue();
                }
            }
            adverseCvfsCount = adverseCvfInsideInvCount + adverseCvfOutsideInvCount;
            adverseCvfTotalRankEffect = adverseCvfInsideInvTotalRankEffect + adverseCvfOutsideInvTotalRankEffect;


            bufferedWriter.write("#   program name:             " + programName + "\n");
            bufferedWriter.write("#     number of nodes:        " + numberOfNodes + "\n");
            bufferedWriter.write("#     sample size:            " + sampleSize + "\n");
            bufferedWriter.write("#     probe limit:            " + probeLimit + "\n");
            bufferedWriter.write("#   prog trans inside Inv count:  " + anaResults.getNumberOfProgTransInsideInv() + "\n");
            bufferedWriter.write("#   prog trans outside Inv count: " + anaResults.getNumberOfProgTransOutsideInv() + "\n");
            bufferedWriter.write("#     min trans rank effect:  " + minProgTransOutsideInvRankEffect + "\n");
            bufferedWriter.write("#     max trans rank effect:  " + maxProgTransOutsideInvRankEffect + "\n");
            bufferedWriter.write("#     avg trans rank effect:  " +
                    averageToStr(anaResults.getProgTransOutsideInvTotalRankEffect(),
                            anaResults.getNumberOfProgTransOutsideInv()) + "\n");
            bufferedWriter.write("#   cvfs count:               " + numberOfCvfs + "\n");
            bufferedWriter.write("#     min cvf rank effect:    " + minCvfRankEffect + "\n");
            bufferedWriter.write("#     max cvf rank effect:    " + maxCvfRankEffect + "\n");
            bufferedWriter.write("#     avg cvf rank effect:    " +
                    averageToStr(1.0*cvfTotalRankEffect, numberOfCvfs) + "\n");
            bufferedWriter.write("#     avg adverse cvf rank effect:    " +
                    averageToStr(adverseCvfTotalRankEffect, adverseCvfsCount) + "\n");
            bufferedWriter.write("#   cvfs inside Inv count:    " + anaResults.getNumberOfCvfInsideInv() + "\n");
            bufferedWriter.write("#     min cvf inside Inv rank effect: " +
                    (minCvfInsideInvRankEffect == Integer.MAX_VALUE? "N/A" : minCvfInsideInvRankEffect) + "\n");
            bufferedWriter.write("#     max cvf inside Inv rank effect: " +
                    (maxCvfInsideInvRankEffect == Integer.MIN_VALUE? "N/A" : maxCvfInsideInvRankEffect) + "\n");
            bufferedWriter.write("#     avg cvf inside Inv rank effect: " +
                    averageToStr(anaResults.getCvfInsideInvTotalRankEffect(),
                            anaResults.getNumberOfCvfInsideInv()) + "\n");
            bufferedWriter.write("#     avg adverse cvf inside Inv rank effect: " +
                    averageToStr(1.0*adverseCvfInsideInvTotalRankEffect, adverseCvfInsideInvCount) + "\n");
            bufferedWriter.write("#   cvfs outside Inv count:   " + anaResults.getNumberOfCvfOutsideInv() + "\n");
            bufferedWriter.write("#     min cvf outside Inv rank effect: " + minCvfOutsideInvRankEffect + "\n");
            bufferedWriter.write("#     max cvf outside Inv rank effect: " + maxCvfOutsideInvRankEffect + "\n");
            bufferedWriter.write("#     avg cvf outside Inv rank effect: " +
                    averageToStr(anaResults.getCvfOutsideInvTotalRankEffect(),
                            anaResults.getNumberOfCvfOutsideInv()) + "\n");
            bufferedWriter.write("#     avg adverse cvf outside Inv rank effect: " +
                    averageToStr(1.0*adverseCvfOutsideInvTotalRankEffect, adverseCvfOutsideInvCount) + "\n");
            bufferedWriter.write("#   Overall max rank effect:          " + maxRankEffect + "\n");
            bufferedWriter.write("#   Overall min rank effect:          " + minRankEffect + "\n");
            bufferedWriter.write("#\n");
            bufferedWriter.write("#    rank effect        trans_count    cvfs_in_I_count   cvfs_out_I_count\n");
            bufferedWriter.write("#   ------------  -----------------  -----------------  -----------------\n");
            bufferedWriter.flush();


            for(int rankEffect = minRankEffect; rankEffect <= maxCvfRankEffect; rankEffect ++){
                int progTransCount = 0;
                int cvfInInvCount = 0;
                int cvfOutInvCount = 0;
                if(progTransOutsideInvRankEffectDistribution.containsKey(rankEffect)){
                    progTransCount = progTransOutsideInvRankEffectDistribution.get(rankEffect);
                }
                if(cvfInsideInvRankEffectDistribution.containsKey(rankEffect)){
                    cvfInInvCount = cvfInsideInvRankEffectDistribution.get(rankEffect);
                }
                if(cvfOutsideInvRankEffectDistribution.containsKey(rankEffect)){
                    cvfOutInvCount = cvfOutsideInvRankEffectDistribution.get(rankEffect);
                }

                if(progTransCount == 0 && cvfInInvCount == 0 && cvfOutInvCount ==0) {
                    // all maps have no entry for this key, skip
                    continue;
                }else{
                    bufferedWriter.write(String.format("    %12d  %16d  %16d  %16d\n",
                            rankEffect, progTransCount, cvfInInvCount, cvfOutInvCount));
                }
            }


            bufferedWriter.flush();

            // you probably do not want to close stdout
            // since all latter invocation of System.out.println() will go nowhere
            if(!outputFileNamePrefix.equals("stdout"))
                bufferedWriter.close();



            displayPerNodeRankEffectOfProgramTransitionsAndCvfs(anaResults, resultType);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void displayPerNodeRankEffectOfProgramTransitionsAndCvfs(AnalysisResults<PCT> anaResults, String resultType){
        Vector<TreeMap<Integer, Integer>> perNodeCvfOutsideInvRankEffectDistribution = anaResults.getPerNodeCvfOutsideInvRankEffectDistribution();

        if(perNodeCvfOutsideInvRankEffectDistribution == null){
            System.out.println("Rank effect of cvfs per node has not been computed");
            return;
        }

        Vector<BufferedWriter> perNodeBufferedWriter = new Vector<>();
        for(int nodeId = 0; nodeId < numberOfNodes; nodeId ++) {
            try {
                if (outputFileNamePrefix.equals("stdout")) {
                    perNodeBufferedWriter.addElement(new BufferedWriter(new OutputStreamWriter(System.out)));

                } else {
                     perNodeBufferedWriter.addElement(new BufferedWriter(new PrintWriter(outputFileNamePrefix +
                                "-rank-effect-dist-prog-transition-and-cvf-" + resultType + "-per-node-" + nodeId + ".txt")));
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        for(int nodeId = 0; nodeId < numberOfNodes; nodeId ++){
            TreeMap<Integer, Integer> cvfOutsideInvRankEffectDistribution = perNodeCvfOutsideInvRankEffectDistribution.elementAt(nodeId);
            BufferedWriter bufferedWriter = perNodeBufferedWriter.elementAt(nodeId);

            try{
                Integer minCvfOutsideInvRankEffect,
                        maxCvfOutsideInvRankEffect;

                if(cvfOutsideInvRankEffectDistribution.isEmpty()){
                    minCvfOutsideInvRankEffect = Integer.MAX_VALUE;
                    maxCvfOutsideInvRankEffect = Integer.MIN_VALUE;
                }else {
                    minCvfOutsideInvRankEffect = cvfOutsideInvRankEffectDistribution.firstKey();
                    maxCvfOutsideInvRankEffect = cvfOutsideInvRankEffectDistribution.lastKey();
                }

                int minCvfRankEffect = minCvfOutsideInvRankEffect;
                int maxCvfRankEffect = maxCvfOutsideInvRankEffect;
                int minRankEffect = minCvfRankEffect;
                int maxRankEffect = maxCvfRankEffect;

                int numberOfCvfOutsideInv = 0;
                int cvfOutsideInvTotalRankEffect = 0;
                int adverseCvfOutsideInvCount = 0;
                double adverseCvfOutsideInvTotalRankEffect = 0;
                for(Map.Entry<Integer, Integer> entry : cvfOutsideInvRankEffectDistribution.entrySet()){
                    int rankEffect = entry.getKey();
                    int count = entry.getValue();
                    numberOfCvfOutsideInv += count;
                    cvfOutsideInvTotalRankEffect += rankEffect * count;
                    if(rankEffect > 0){
                        adverseCvfOutsideInvCount += count;
                        adverseCvfOutsideInvTotalRankEffect += rankEffect * count;
                    }
                }

                int numberOfCvfs = numberOfCvfOutsideInv;
                int cvfTotalRankEffect = cvfOutsideInvTotalRankEffect;
                int adverseCvfCount = adverseCvfOutsideInvCount;
                double adverseCvfTotalRankEffect = adverseCvfOutsideInvTotalRankEffect;

                bufferedWriter.write("#   program name:             " + programName + "\n");
                bufferedWriter.write("#     number of nodes:        " + numberOfNodes + "\n");
                bufferedWriter.write("#     sample size:            " + sampleSize + "\n");
                bufferedWriter.write("#     probe limit:            " + probeLimit + "\n");
                bufferedWriter.write("#   cvfs count:               " + numberOfCvfs + "\n");
                bufferedWriter.write("#     min cvf rank effect:    " + minCvfRankEffect + "\n");
                bufferedWriter.write("#     max cvf rank effect:    " + maxCvfRankEffect + "\n");
                bufferedWriter.write("#     avg cvf rank effect:    " +
                        averageToStr(1.0*cvfTotalRankEffect, numberOfCvfs) + "\n");
                bufferedWriter.write("#     avg adverse cvf rank effect:    " +
                        averageToStr(adverseCvfTotalRankEffect, adverseCvfCount) + "\n");
                bufferedWriter.write("#   cvfs outside Inv count:   " + numberOfCvfOutsideInv + "\n");
                bufferedWriter.write("#     min cvf outside Inv rank effect: " + minCvfOutsideInvRankEffect + "\n");
                bufferedWriter.write("#     max cvf outside Inv rank effect: " + maxCvfOutsideInvRankEffect + "\n");
                bufferedWriter.write("#     avg cvf outside Inv rank effect: " +
                        averageToStr(cvfOutsideInvTotalRankEffect,
                                numberOfCvfOutsideInv) + "\n");
                bufferedWriter.write("#     avg adverse cvf outside Inv rank effect: " +
                        averageToStr(1.0*adverseCvfOutsideInvTotalRankEffect, adverseCvfOutsideInvCount) + "\n");
                bufferedWriter.write("#\n");
                bufferedWriter.write("#    rank effect    cvfs_out_I_count\n");
                bufferedWriter.write("#   ------------   -----------------\n");
                bufferedWriter.flush();


                for(int rankEffect = minRankEffect; rankEffect <= maxCvfRankEffect; rankEffect ++){
                    int cvfOutInvCount = 0;
                    if(cvfOutsideInvRankEffectDistribution.containsKey(rankEffect)){
                        cvfOutInvCount = cvfOutsideInvRankEffectDistribution.get(rankEffect);
                    }

                    if(cvfOutInvCount == 0) {
                        // all maps have no entry for this key, skip
                        continue;
                    }else{
                        bufferedWriter.write(String.format("    %12d  %16d\n",
                                rankEffect, cvfOutInvCount));
                    }
                }

                bufferedWriter.flush();

                // you probably do not want to close stdout
                // since all latter invocation of System.out.println() will go nowhere
                if(!outputFileNamePrefix.equals("stdout"))
                    bufferedWriter.close();

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        }

    }

}
