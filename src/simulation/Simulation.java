package simulation;

import coloring.ProgramConfigurationColoring;
import datastructure.AnalysisResults;
import datastructure.ProgramConfigurationInformation;
import dijkstra3states.ProgramConfigurationDijkstra3States;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import main.AnalyzeCvfs;
import main.AnalyzeProgramBasedOnConfigurations;
import main.AnalyzeProgramBasedOnTransitions;
import maxmatching.ProgramConfigurationMaxMatching;
import template.ProgramConfigurationTemplate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.*;

import static datastructure.ProgramConfigurationInformation.DEFAULT_MIN_RANK;
import static main.AnalyzeCvfs.*;
import static main.Utility.readGraphTopology;
import static template.ProgramConfigurationTemplate.*;
import static template.ProgramConfigurationTemplate.PROBE_LIMIT_OPTION_ADAPTIVE;

/**
 */

public class Simulation <PCT extends ProgramConfigurationTemplate>{
    public static int runId;
    public static String programName;
    public static String outputFileNamePrefix;
    public static int cvf;

    // graph topology in form of adjacent matrix
    protected static int numberOfNodes;

    protected static int sampleSize;  // number of initial configruations outside the invariant from which we evaluate the convergence
    protected static int simulationLimit;  // maximal number of simulation steps
    protected static int numberOfTrials;
    protected static int cvfInterval; // if cvfInterval = 5, that means a cvf occur every 5 program transitions
                               // if cvfInterval = simulationLimit + 1, that means no cvf occurs in the simulation

//    protected Simulation (
//            int runId,
//            int numberOfNodes,
//            int cvf,
//            String programName,
//            String outputFileName
//    ){
//        this.runId = runId;
//        this.numberOfNodes = numberOfNodes;
//        this.cvf = cvf;
//        this.programName = programName;
//        this.outputFileNamePrefix = outputFileName;
//    }

    public static void main(String args[]) throws Exception {
        long startMs = System.currentTimeMillis();

        // parsing input arguments
        OptionParser mainParser = new OptionParser();


        OptionSpec programNameSpec = mainParser.accepts("program-name")
                .withRequiredArg();
        OptionSpec<Integer> simulationLimitSpec = mainParser.accepts("simulation-limit")
                .withRequiredArg()
                .ofType(Integer.class);
        OptionSpec<Integer> sampleSizeSpec = mainParser.accepts("sample-size")
                .withRequiredArg()
                .ofType(Integer.class);
        OptionSpec<Integer> numberOfTrialsSpec = mainParser.accepts("number-of-trials")
                .withRequiredArg()
                .ofType(Integer.class);
        OptionSpec<Integer> cvfIntervalSpec = mainParser.accepts("cvf-interval")
                .withRequiredArg()
                .ofType(Integer.class);
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

        OptionSet mainOption = mainParser.parse(args);
        runId = mainOption.valueOf(runIdSpec);

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
        programName = (String) mainOption.valueOf(programNameSpec);
        String graphTopologyFileName = (String) mainOption.valueOf(graphTopologyFileNameSpec);
        outputFileNamePrefix = (String) mainOption.valueOf(outputFileNamePrefixSpec);
        String debugFileNamePrefix = (String) mainOption.valueOf(debugFileNamePrefixSpec);
        simulationLimit = mainOption.valueOf(simulationLimitSpec);
        sampleSize = mainOption.valueOf(sampleSizeSpec);
        numberOfTrials = mainOption.valueOf(numberOfTrialsSpec);
        cvfInterval = mainOption.valueOf(cvfIntervalSpec);

        if((simulationLimit <= 0) ||
                (sampleSize <= 0) ||
                (numberOfTrials <= 0) ||
                (cvfInterval <= 0)){
            throw new Exception("All parameters below must be > 0 \n" +
                                "   simulationLimit = " + simulationLimit + "\n" +
                                "   sampleSize = " + sampleSize + "\n" +
                                "   numberOfTrials = " + numberOfTrials + "\n" +
                                "   cvfInterval = " + cvfInterval + "\n");
        }

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
        System.out.println("++ Program start time:      " + DateFormat.getDateTimeInstance().format(startMs));
        System.out.println("     simulationLimit:       " + simulationLimit);
        System.out.println("     program name:          " + programName);
        System.out.println("     sampleSize:            " + sampleSize);
        System.out.println("     numberOfTrials:        " + numberOfTrials);
        System.out.println("     cvfInterval:           " + cvfInterval);
        System.out.println("     run id:                " + runId);
        System.out.println("     graph topology file:   " + graphTopologyFileName);
        System.out.println("     number of nodes:       " + numberOfNodes);
        System.out.println("     cvf perturbation type: " + cvf);
        System.out.println();


        // generate  sampleSize random program configurations and add to programConfigurationSamples
        HashMap<ProgramConfigurationTemplate, ProgramConfigCvfRecoveryCostRecord>
                cvfRecoveryCostMap = new HashMap<>();
        ProgramConfigurationTemplate currentConfig = null;
        int sampleCount = 0;
        int irrelevantProbeLimit = 0; // irrelevantProbeLimit is not relevant in this program
        switch (programName) {
            case PROGRAM_NAME_MAX_MATCHING:
                currentConfig = new ProgramConfigurationMaxMatching(numberOfNodes, new TreeMap<>(), cvf, irrelevantProbeLimit);
                break;
            case PROGRAM_NAME_DIJKSTRA_3_STATES:
                currentConfig = new ProgramConfigurationDijkstra3States(numberOfNodes, new TreeMap<>(), cvf, irrelevantProbeLimit);
                break;
            case PROGRAM_NAME_COLORING:
                currentConfig = new ProgramConfigurationColoring(numberOfNodes, new TreeMap<>(), cvf, irrelevantProbeLimit);
                break;
            default:
                throw new Exception("Unknown program name: " + programName);
        }

        System.out.println(" Generating random samples outside invariant ... ");
        if(sampleSize >= (currentConfig.getSizeOfStateSpace()/2)) {
            System.out.println("The state space is too few, " + currentConfig.getSizeOfStateSpace() +
                    " not enough for " + sampleSize + " samples outside invariant");
            System.exit(0);
        }

        while(sampleCount < sampleSize){
            ProgramConfigurationTemplate pc = currentConfig.getRandomProgramConfiguration().getDeepCopy();
            if(cvfRecoveryCostMap.containsKey(pc)){
                //if pc is already sampled
                continue;
            }else {
                if(pc.isInsideInvariant()){
                    // just being interested in program config outside invariant
                    continue;
                }else{
                    cvfRecoveryCostMap.put(pc, new ProgramConfigCvfRecoveryCostRecord());
                    sampleCount ++;
                }
            }
        }

        System.out.println(" Done with sample generation in " + (System.currentTimeMillis() - startMs)/1000 + " secs");

        for(Map.Entry<ProgramConfigurationTemplate, ProgramConfigCvfRecoveryCostRecord> entry
                : cvfRecoveryCostMap.entrySet()){
            ProgramConfigurationTemplate aPC = entry.getKey();


            for(int trialCount = 0; trialCount < numberOfTrials; trialCount ++) {
                int programConvergence = aPC.getDeepCopy().getNumberOfConvergenceSteps(simulationLimit, simulationLimit + 1);
                int cvfConvergence = aPC.getDeepCopy().getNumberOfConvergenceSteps(simulationLimit, cvfInterval);
                entry.getValue().addData(programConvergence, cvfConvergence);
            }
        }

        // write results to output file
        displayCvfRecoverCost(cvfRecoveryCostMap);

        long endMs = System.currentTimeMillis();
        System.out.println("\n  + Program end time " + DateFormat.getDateTimeInstance().format(startMs));
        System.out.println(String.format("    duration: %.2f seconds", (endMs - startMs)/1000.0));

    }

    public static void displayCvfRecoverCost(
            HashMap<ProgramConfigurationTemplate, ProgramConfigCvfRecoveryCostRecord> cvfRecoveryCostMap){

        BufferedWriter bufferedWriter;

        double averageCvfRecoveryCost = 0.0;
        for(Map.Entry<ProgramConfigurationTemplate, ProgramConfigCvfRecoveryCostRecord> entry
                : cvfRecoveryCostMap.entrySet()){
            averageCvfRecoveryCost += entry.getValue().cvfRecoverCost;
        }
        averageCvfRecoveryCost = averageCvfRecoveryCost/sampleSize;


        try{
            if (outputFileNamePrefix.equals("stdout"))
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));
            else
                bufferedWriter = new BufferedWriter(new PrintWriter(outputFileNamePrefix + "-cvf-recovery-cost.txt"));

            bufferedWriter.write("#   program name:           " + programName + "\n");
            bufferedWriter.write("#   number of nodes:        " + numberOfNodes + "\n");
            bufferedWriter.write("#   sampleSize:             " + sampleSize + "\n");
            bufferedWriter.write("#   simulationLimit:        " + simulationLimit + "\n");
            bufferedWriter.write("#   numberOfTrials:         " + numberOfTrials + "\n");
            bufferedWriter.write("#   cvfInterval:            " + cvfInterval + "\n");
            bufferedWriter.write("#   runId:                  " + runId + "\n");
            bufferedWriter.write("#   cvf:                    " + cvf + "\n");
            bufferedWriter.write("#   averageCvfRecoveryCost: " + averageCvfRecoveryCost + "\n");

            bufferedWriter.write("##    progConv     cvfCov \n");
            bufferedWriter.write("#   ----------  ----------\n");
            for(Map.Entry<ProgramConfigurationTemplate, ProgramConfigCvfRecoveryCostRecord> entry
                    : cvfRecoveryCostMap.entrySet()){
                ProgramConfigCvfRecoveryCostRecord record = entry.getValue();
                for(int trial = 0; trial < numberOfTrials; trial ++){
                    bufferedWriter.write(String.format("    %10d  %10d\n",
                            record.getProgramConvergence().elementAt(trial),
                            record.getCvfConvergence().elementAt(trial)));
                }
            }

            bufferedWriter.write("## avgProgConv  avgCvfCov    avgCost \n");
            bufferedWriter.write("#   ----------  ---------- ----------\n");
            for(Map.Entry<ProgramConfigurationTemplate, ProgramConfigCvfRecoveryCostRecord> entry
                    : cvfRecoveryCostMap.entrySet()){
                ProgramConfigCvfRecoveryCostRecord record = entry.getValue();
                bufferedWriter.write(String.format("    %10.2f %10.2f %10.2f\n",
                        record.getAvgProgramConvergence(),
                        record.getAvgCvfConvergence(),
                        record.getCvfRecoverCost()));
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
