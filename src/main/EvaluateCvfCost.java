//package template;
//
//import dijkstra3states.ProgramConfigurationDijkstra3States;
//import joptsimple.OptionParser;
//import joptsimple.OptionSet;
//import joptsimple.OptionSpec;
//
//import java.util.TreeMap;
//
//import static template.ComputeMaxRank.PROGRAM_NAME_DIJKSTRA_3_STATES;
//
///**
// * Evaluate the cost of cvf for a given distributed program
// */
//
//public class EvaluateCvfCost {
//
//    static void evaluateCvfEffect(ComputeMaxRank cmr){
//
//    }
//
//    // main program
//    public static void main(String args[]) throws Exception {
//        OptionParser mainParser = new OptionParser();
//
//        OptionSpec programNameSpec = mainParser.accepts("program-name")
//                .withRequiredArg();
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
//        String programName = (String) mainOption.valueOf(programNameSpec);
//
//        if(numberOfNodes <= 1){
//            throw new Exception("The number of nodes = " + numberOfNodes + ". Distributed system has at least 2 nodes");
//        }
//
//        ComputeMaxRank cmr;
//
//        switch(programName){
//            case PROGRAM_NAME_DIJKSTRA_3_STATES:
//            default:
//                ProgramConfigurationDijkstra3States currentConfig = new ProgramConfigurationDijkstra3States(numberOfNodes, new TreeMap<>());
//                ProgramConfigurationDijkstra3States firstConfig = new ProgramConfigurationDijkstra3States(numberOfNodes, new TreeMap<>());
//                cmr = new ComputeMaxRank(runId, numberOfNodes, currentConfig, firstConfig);
//
//                break;
//        }
//
//        System.out.println("++init: ");
//        cmr.init();
//
//        System.out.println("\n++compute max ranks for the program " + programName);
//        cmr.computeMaxRankForConfigs();
//
//        System.out.println("\n++results of max rank computation: ");
//        cmr.displayProgramConfigInfoMap(cmr.getProgConfigInfoMap(), 5, true);
//
//    }
//
//}
