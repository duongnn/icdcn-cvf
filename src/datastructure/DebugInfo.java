package datastructure;

import java.io.BufferedWriter;
import java.math.BigInteger;

/**
 */

public class DebugInfo {
    // file to record debugging information
    public static BufferedWriter debugFile;

     public static long numberOfProbedConfigurationsInsideInvariant = 0;

    // invocation cases in evaluateSomeRandomPaths
    public static int caseMaxPathCountZero = 0;
    public static int caseSuccessorIsMore = 0;
    public static int caseSuccessorIsLess = 0;

    // info about RankEvaluationResults recorded
    // use BigInteger since RankEvaluationResults change datatype
//    public static long maxRankTotalRecorded = 0;
//    public static long maxPathCountRecorded = 0;
    public static BigInteger maxRankTotalRecorded = BigInteger.ZERO;
    public static BigInteger maxPathCountRecorded = BigInteger.ZERO;


    public static String getDebugInfoStr(){
        StringBuilder debugInfo = new StringBuilder();

        return debugInfo
                .append("numberOfProbedConfigurationsInsideInvariant = " + numberOfProbedConfigurationsInsideInvariant + "\n")
                .append("caseMaxPathCount = " + caseMaxPathCountZero + "\n")
                .append("caseSuccessorIsMore = " + caseSuccessorIsMore + "\n")
                .append("caseSuccessorIsLess = " + caseSuccessorIsLess + "\n")
                .append("maxRankTotalRecorded = " + maxRankTotalRecorded + "\n")
                .append("maxPathCountRecorded = " + maxPathCountRecorded + "\n")
                .toString();
    }
}
