package main;

import datastructure.AnalysisResults;
import datastructure.ConfigurationPair;
import datastructure.ProgramConfigurationInformation;
import datastructure.RankEvaluationResults;
import template.ProgramConfigurationTemplate;

import java.text.DateFormat;
import java.util.*;

import static main.Utility.*;

/**
 * For random analysis, instead of selecting random configurations (vertices in state space)
 * and evaluate the effect of cvfs and program transitions on them,
 * we randomly generate transitions (edges connecting configurations in state space)
 * and evaluate the rank changes of those edges.
 *
 */

public class AnalyzeProgramBasedOnTransitions<PCT extends ProgramConfigurationTemplate> extends AnalyzeCvfs {
    static final int DEFAULT_RANK_EFFECT = Integer.MAX_VALUE;

    // for program transitions/cvf rank effect based on max rank
    TreeMap<ConfigurationPair<PCT>, Integer> transMaxRankEffectMap;
    TreeMap<ConfigurationPair<PCT>, Integer> cvfMaxRankEffectMap;

    // for rank effect based on average rank
    TreeMap<ConfigurationPair<PCT>, Integer> transAvgRankEffectMap;
    TreeMap<ConfigurationPair<PCT>, Integer> cvfAvgRankEffectMap;

    AnalysisResults<PCT> randMaxAnaResults; // select max from random paths
    AnalysisResults<PCT> randAvgAnaResults; // select average from random paths

    public AnalyzeProgramBasedOnTransitions(
            int runId,
            int numberOfNodes,
            int cvf,
            String programName,
            String outputFileName,
            long sampleSize,
            long probeLimit,
            PCT currentProgramConfig,
            PCT firstProgramConfig){

        super(runId,
                numberOfNodes,
                cvf,
                programName,
                outputFileName,
                sampleSize,
                probeLimit,
                currentProgramConfig,
                firstProgramConfig);

    }

    @Override
    void runAnalyzeProgram() throws Exception {
        System.out.println(" Analyzing program based on transitions ");

        long startMs = System.currentTimeMillis();
        long currrentMs = startMs;

        transMaxRankEffectMap = new TreeMap<>();
        cvfMaxRankEffectMap = new TreeMap<>();

        transAvgRankEffectMap = new TreeMap<>();
        cvfAvgRankEffectMap = new TreeMap<>();

        // auxiliary map for storing ranks of program configurations
        TreeMap<PCT, RankEvaluationResults> rankCache = new TreeMap<>();

        // randomly generate some number of transitions and some number of cvfs
        // evaluate the rank of endpoints
        // compute rank difference

        /* how to generate transitions (or cvfs)
            1. randomly generate a config outside the invariant
            2. pick randomly one successors from transition (or cvf) successor list
            3. form the edge
                 if edge is already in the hashTable transEffect (or cvfEffect)
                   skip
                 else
                   put the edge in the hashTable transEffect (or cvfEffect)
           how to evaluate
            for each trans in transEffect (or cvfEffect)
                get first point
                get next point
                evaluate rank of firstpoint
                evaluate rank of next point
        */

        System.out.println("    Generating " + sampleSize + " random program transitions ... ");
        int transCount = 0;
        while(transCount < sampleSize){
            PCT currentPc = (PCT) currentProgramConfig.getRandomProgramConfiguration().getDeepCopy();

            if(currentPc.isInsideInvariant()){
                // at this moment, and for future work with silently stabilizing program
                // we only care configs outside the invariant
                continue;
            }else {

                TreeSet<PCT> transSuccList = currentPc.getProgramConfigTransSuccessorList();
                PCT transSucc = getRandomElementFromTreeSet(transSuccList);

                ConfigurationPair<PCT> trans = new ConfigurationPair<PCT>(currentPc, (PCT) transSucc.getDeepCopy());
                if (transMaxRankEffectMap.containsKey(trans)) {
                    // this transition is already generated
                    continue;
                } else {
                    transMaxRankEffectMap.put(trans, DEFAULT_RANK_EFFECT);
                    transAvgRankEffectMap.put(trans, DEFAULT_RANK_EFFECT);

                    transCount ++;

                }
            }
        }


        System.out.println("      done in " + ((System.currentTimeMillis() - currrentMs)/1000) + " seconds");
        currrentMs = System.currentTimeMillis();

        System.out.println("    Generating " + sampleSize + " random cvfs ... ");
        int cvfCount = 0;
        while(cvfCount < sampleSize){
            PCT currentPc = (PCT) currentProgramConfig.getRandomProgramConfiguration().getDeepCopy();
            if(currentPc.isInsideInvariant()){
                // we are interested only configs outside invariant
                continue;
            }else{
                TreeSet<PCT> cvfSuccList = currentPc.getProgramConfigCvfSuccessorList();
                TreeSet<PCT> transSuccList = currentPc.getProgramConfigTransSuccessorList();
                cvfSuccList.removeAll(transSuccList);
                if(cvfSuccList.isEmpty()){
                    // for this program config, all of its cvf successors are also transition successors
                    continue;
                }

                PCT cvfSucc = getRandomElementFromTreeSet(cvfSuccList);

                ConfigurationPair<PCT> cvf = new ConfigurationPair<PCT>(currentPc, (PCT) cvfSucc.getDeepCopy());
                if(cvfMaxRankEffectMap.containsKey(cvf)){
                    // this cvf is already generated
                    continue;
                }else{
                    cvfMaxRankEffectMap.put(cvf, DEFAULT_RANK_EFFECT);
                    cvfAvgRankEffectMap.put(cvf, DEFAULT_RANK_EFFECT);

                    cvfCount++;
                }
            }
        }

        System.out.println("      done in " + ((System.currentTimeMillis() - currrentMs)/1000) + " seconds");
        currrentMs = System.currentTimeMillis();

        // evaluate rank effect of transitions
        System.out.println(" Evaluating rank effect of transitions");
        transCount = 0;
        for(Map.Entry<ConfigurationPair<PCT>, Integer> entry : transMaxRankEffectMap.entrySet()){
//            updateConfigurationPairMaxRankEffect(entry, rankCache);

            ConfigurationPair<PCT> configPair = entry.getKey();

            PCT currentPc = configPair.getCurrentConfig();
            PCT nextPc = configPair.getNextConfig();

            RankEvaluationResults currentPcRankEval = currentPc.evaluateRanks(rankCache);
            RankEvaluationResults nextPcRankEval = nextPc.evaluateRanks(rankCache);
            int maxRankDifference = nextPcRankEval.getMaxRank() - currentPcRankEval.getMaxRank();
            int avgRankDifference = nextPcRankEval.getAvgRank() - currentPcRankEval.getAvgRank();

            entry.setValue(maxRankDifference);

            // update for transAvgRankEffectMap
            transAvgRankEffectMap.put(configPair, avgRankDifference);

            // show progress
            transCount++;
//            System.out.print(String.format("\r%s",
//                    String.format("         [ %d/%d    %5.1f %%    %5.1f microsec/pc ]     ",
//                            transCount,
//                            sampleSize,
//                            ((100*transCount)/(double) sampleSize),
//                            (System.currentTimeMillis() - startMs)/((double) transCount)
//                    )));

        }
        System.out.println("      done in " + ((System.currentTimeMillis() - currrentMs)/1000) + " seconds");
        currrentMs = System.currentTimeMillis();

        System.out.println(" Evaluating rank effect of cvfs");
        for(Map.Entry<ConfigurationPair<PCT>, Integer> entry: cvfMaxRankEffectMap.entrySet()){
//            updateConfigurationPairMaxRankEffect(entry, rankCache);

            ConfigurationPair<PCT> configPair = entry.getKey();

            PCT currentPc = configPair.getCurrentConfig();
            PCT nextPc = configPair.getNextConfig();

            RankEvaluationResults currentPcRankEval = currentPc.evaluateRanks(rankCache);
            RankEvaluationResults nextPcRankEval = nextPc.evaluateRanks(rankCache);
            int maxRankDifference = nextPcRankEval.getMaxRank() - currentPcRankEval.getMaxRank();
            int avgRankDifference = nextPcRankEval.getAvgRank() - currentPcRankEval.getAvgRank();
            entry.setValue(maxRankDifference);

            // update
            cvfAvgRankEffectMap.put(configPair, avgRankDifference);

            // show progress
            cvfCount++;
//            System.out.print(String.format("\r%s",
//                    String.format("         [ %d/%d    %5.1f %%    %5.1f microsec/pc ]     ",
//                            cvfCount,
//                            sampleSize,
//                            ((100*cvfCount)/(double) sampleSize),
//                            (System.currentTimeMillis() - startMs)/((double) cvfCount)
//                    )));

        }
        System.out.println("      done in " + ((System.currentTimeMillis() - currrentMs)/1000) + " seconds");
        currrentMs = System.currentTimeMillis();

        randMaxAnaResults = encapsulateIntoAnalysisResults(transMaxRankEffectMap, cvfMaxRankEffectMap);
        randAvgAnaResults = encapsulateIntoAnalysisResults(transAvgRankEffectMap, cvfAvgRankEffectMap);

        System.out.println(" Displaying results for rand-max");
        displayRankEffectOfProgramTransitionsAndCvfs(randMaxAnaResults, "rand-max");

        System.out.println(" Displaying results for rand-avg");
        displayRankEffectOfProgramTransitionsAndCvfs(randAvgAnaResults, "rand-avg");

        long endMs = System.currentTimeMillis();
        System.out.println("\n  + Program end time " + DateFormat.getDateTimeInstance().format(startMs));
        System.out.println(String.format("    duration: %.2f seconds", (endMs - startMs)/1000.0));

    }

    void updateConfigurationPairMaxRankEffect(
            Map.Entry<ConfigurationPair<PCT>, Integer> entry,
            TreeMap<PCT, RankEvaluationResults> rankCache){

        ConfigurationPair<PCT> configPair = entry.getKey();

        PCT currentPc = configPair.getCurrentConfig();
        PCT nextPc = configPair.getNextConfig();

        RankEvaluationResults currentPcRankEval = currentPc.evaluateRanks(rankCache);
        RankEvaluationResults nextPcRankEval = nextPc.evaluateRanks(rankCache);
        int maxRankDifference =  nextPcRankEval.getMaxRank() - currentPcRankEval.getMaxRank();
        entry.setValue(maxRankDifference);
    }

    /**
     * encapsulate results in an AnalysisResults object
     * @param transRankEffectMap rank effect of transitions
     * @param cvfRankEffectMap rank effect of cvfs
     * @return an AnalysisResults object containing those rank effect information
     */
    private AnalysisResults encapsulateIntoAnalysisResults(
            TreeMap<ConfigurationPair<PCT>, Integer> transRankEffectMap,
            TreeMap<ConfigurationPair<PCT>, Integer> cvfRankEffectMap){
        // encapsulate results in an AnalysisResults object
        TreeMap<PCT, ProgramConfigurationInformation<PCT>> progConfigInfoMap = new TreeMap<>();
        TreeMap<Integer, Integer> progConfigRankDistribution = new TreeMap<>();
        TreeMap<Integer, Integer> progTransOutsideInvRankEffectDistribution = configPairRankEffectMapToRankDistribution(transRankEffectMap);
        int numberOfProgTransOutsideInv = transRankEffectMap.size();
        int progTransOutsideInvTotalRankEffect = transRankEffectMap.values().stream().reduce(0, Integer::sum);
        int numberOfProgTransInsideInv = 0;
        TreeMap<Integer, Integer> cvfInsideInvRankEffectDistribution = new TreeMap<>();
        TreeMap<Integer, Integer> cvfOutsideInvRankEffectDistribution = configPairRankEffectMapToRankDistribution(cvfRankEffectMap);
        int numberOfCvfInsideInv = 0;
        int numberOfCvfOutsideInv = cvfRankEffectMap.size();
        int cvfInsideInvTotalRankEffect = 0;
        int cvfOutsideInvTotalRankEffect = cvfRankEffectMap.values().stream().reduce(0, Integer::sum);

        return new AnalysisResults(
                progConfigInfoMap,
                progConfigRankDistribution,
                progTransOutsideInvRankEffectDistribution,
                numberOfProgTransOutsideInv,
                progTransOutsideInvTotalRankEffect,
                numberOfProgTransInsideInv,
                cvfInsideInvRankEffectDistribution,
                cvfOutsideInvRankEffectDistribution,
                numberOfCvfInsideInv,
                numberOfCvfOutsideInv,
                cvfInsideInvTotalRankEffect,
                cvfOutsideInvTotalRankEffect);

    }

}
