package main;

import datastructure.ConfigurationPair;
import datastructure.DebugInfo;
import datastructure.RankEvaluationResults;
import template.ProgramConfigurationTemplate;
import template.SuccessorInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 */

public class Utility {
    // dipslay average in string
    public static String averageToStr(double total, int count){
        if(count == 0){
            return "N/A";
        }else{
            return String.format("%.2f", total/count);
        }
    }

    public static <PCT extends ProgramConfigurationTemplate> void updateRankCache(
            TreeMap<PCT, RankEvaluationResults> rankCache,
            PCT pc,
            RankEvaluationResults<PCT> eval){

        rankCache.put(pc, eval);

//        if(eval.getRankTotal() > DebugInfo.maxRankTotalRecorded){
//            DebugInfo.maxRankTotalRecorded = eval.getRankTotal();
//        }
//        if(eval.getPathCount() > DebugInfo.maxPathCountRecorded){
//            DebugInfo.maxPathCountRecorded = eval.getPathCount();
//        }
        if(eval.getRankTotal().compareTo(DebugInfo.maxRankTotalRecorded) > 0){
            DebugInfo.maxRankTotalRecorded = eval.getRankTotal();
        }
        if(eval.getPathCount().compareTo(DebugInfo.maxPathCountRecorded) > 0){
            DebugInfo.maxPathCountRecorded = eval.getPathCount();
        }


    }

    public static void addValueToDistribution(TreeMap<Integer, Integer> dist, int val){
        if(!dist.containsKey(val)){
            dist.put(val, 1);
        }else{
            int currentCount = dist.get(val);
            dist.put(val, currentCount + 1);
        }
    }

    public static <PCT extends ProgramConfigurationTemplate> PCT getRandomElementFromTreeSet(TreeSet<PCT> aSet){
        int numberOfSuccessors = aSet.size();
        long elementId = ThreadLocalRandom.current().nextInt(numberOfSuccessors);
        int idCount = 0;

        for(PCT element : aSet){
            if(idCount == elementId){
                return element;
            }
            idCount ++;
        }

        return null;
    }

    public static SuccessorInfo getRandomElementFromSuccessorTreeSet(TreeSet<SuccessorInfo> aSet){
        int numberOfSuccessors = aSet.size();
        long elementId = ThreadLocalRandom.current().nextInt(numberOfSuccessors);
        int idCount = 0;

        for(SuccessorInfo element : aSet){
            if(idCount == elementId){
                return element;
            }
            idCount ++;
        }

        return null;
    }


    public static <PCT extends ProgramConfigurationTemplate> TreeMap<Integer, Integer> configPairRankEffectMapToRankDistribution(
            TreeMap<ConfigurationPair<PCT>, Integer> configPairRankEffectMap){

        TreeMap<Integer, Integer> rankDistribution = new TreeMap<>();
        for(Map.Entry<ConfigurationPair<PCT>, Integer> entry : configPairRankEffectMap.entrySet()){
            int rankValue = entry.getValue();
            addValueToDistribution(rankDistribution, rankValue);
        }
        return rankDistribution;
    }

    public static int getIntFromBoolean(boolean booleanValue){
        if(booleanValue)
            return 1;
        else
            return 0;
    }

    public static boolean getBooleanFromInt(int intValue){
        //return (intValue != 0);
        if(intValue == 0){
            return false;
        }else{
            return true;
        }
    }

    public static HashMap<Integer, Vector<Integer>> readGraphTopology(String graphTopologyFileName){
        try(BufferedReader reader = new BufferedReader(new FileReader(graphTopologyFileName))){
            String line = null;

            // reading graph information
            while((line = reader.readLine()) != null){
                if(line.equals("#")){
                    break;
                }

                // make sure number of nodes matches what is specified in graphTopologyFile
//                String[] lineSplit = line.trim().split("=");
//                if(lineSplit[0].equals("numberOfNodes")){
//                    int valueInFile = Integer.valueOf(lineSplit[1]);
//                    if(numberOfNodes != valueInFile){
//                        System.out.println("number of nodes is not consistent");
//                        System.out.println("   in param: " + numberOfNodes);
//                        System.out.println("   in file : " + valueInFile);
//                        return null;
//                    }
//                }

            }

            if(!line.equals("#")){
                System.out.println("Something wrong in format of the file");
                return null;
            }

            HashMap<Integer, Vector<Integer>> graphTopology = new HashMap<>(100);

            // reading adjacent matrix (in networkx format)
            while((line = reader.readLine()) != null){
                String lineSplit[] = line.trim().split("\\s+");
                int nodeId = Integer.valueOf(lineSplit[0]);
                Vector<Integer> nbrList = new Vector<>();
                for(int nbrIdx = 1; nbrIdx < lineSplit.length; nbrIdx ++){
                    nbrList.addElement(Integer.valueOf(lineSplit[nbrIdx]));
                }
                graphTopology.put(nodeId, nbrList);

                // remove this code since maxDegree is no longer defined
                // calling code will have to read through nbrList to get maxDegree
//                if(maxDegree < nbrList.size()){
//                    maxDegree = nbrList.size();
//                }
            }

            return graphTopology;

        }catch(IOException ioe){
            System.out.println("IOException with opening/reading file " + graphTopologyFileName);
            System.out.println(ioe.getMessage());
            return null;
        }
    }

}
