package datastructure;

import template.ProgramConfigurationTemplate;

import java.math.BigInteger;

/**
 * This class encapsulates the results of rank evaluation of a program configuration
 */

public class RankEvaluationResults<T extends ProgramConfigurationTemplate> {
    // summary of anomalies in rank calulcation
    static long negativeRankAnomalyCount; // there is negative rank
    static long avgMaxRankAnomalyCount; // average rank > max rank
    static long avgMinRankAnomalyCount; // average rank < min rank


    private int maxRank;
    private int minRank;

//    // this way of keeping average rank is most accurate but suffers from overflow, i.e
//    // the values of rankTotal, or even pathCount easily exceed the maximum value of Long
//    // for large graph size
//    // for average rank
//    private long rankTotal; // total of all ranks along all paths
//    private long pathCount; // number of paths

    private BigInteger rankTotal;
    private BigInteger pathCount;


    public RankEvaluationResults(){
        this.maxRank = -1; // for marking this instance is kind of null
    }

    public RankEvaluationResults(int maxRank, int minRank, BigInteger rankTotal, BigInteger pathCount){
        this.maxRank = maxRank;
        this.minRank = minRank;
        this.rankTotal = rankTotal;
        this.pathCount = pathCount;
    }


    public RankEvaluationResults(int rank){
        this.maxRank = rank;
        this.minRank = rank;
//        this.rankTotal = rank;
//        this.pathCount = 1;
        this.rankTotal = BigInteger.valueOf(rank);
        this.pathCount = BigInteger.valueOf(1);

    }

//    // to be remove
//    // rankList should have at least 3 elements
//    public RankEvaluationResults(Vector<Integer> rankList){
//        this.maxRank = rankList.elementAt(0);
//        this.avgRank = rankList.elementAt(1);
//        this.minRank = rankList.elementAt(2);
//    }

//    // to be remove
//    public Vector<Integer> toVector(){
//        Vector<Integer> v = new Vector<>(3,0);
//        v.addElement(maxRank);
//        v.addElement(avgRank);
//        v.addElement(minRank);
//
//        return v;
//    }

    public int getMaxRank() {
        return maxRank;
    }

    public void setMaxRank(int maxRank) {
        this.maxRank = maxRank;
    }

    public int getMinRank() {
        return minRank;
    }

    public void setMinRank(int minRank) {
        this.minRank = minRank;
    }

//    public long getRankTotal() {
//        return rankTotal;
//    }

//    public void setRankTotal(long rankTotal) {
//        this.rankTotal = rankTotal;
//    }

//    public long getPathCount() {
//        return pathCount;
//    }
//
//    public void setPathCount(long pathCount) {
//        this.pathCount = pathCount;
//    }


    public BigInteger getRankTotal() {
        return rankTotal;
    }

    public void setRankTotal(BigInteger rankTotal) {
        this.rankTotal = rankTotal;
    }

    public BigInteger getPathCount() {
        return pathCount;
    }

    public void setPathCount(BigInteger pathCount) {
        this.pathCount = pathCount;
    }

    public int getAvgRank(){
//        return (int) ((rankTotal + pathCount - 1)/pathCount);
        return rankTotal.add(pathCount).subtract(BigInteger.ONE).divide(pathCount).intValue();
    }

    public String toString(){
        return (new StringBuilder())
//                .append(String.format("%5d %5d %10d %10d", maxRank, minRank, rankTotal, pathCount))
                .append(String.format("%5d %5d %10s %10s",
                        maxRank, minRank, rankTotal.toString(), pathCount.toString()))
                .toString();
    }

    public boolean sanityCheck(){
        boolean isOK = true;
        if(getMaxRank() < 0 || getMinRank() < 0 || getAvgRank() < 0){
//            System.out.println("ERROR: RankEvaluationResult.sanityCheck: some rank < 0");
            negativeRankAnomalyCount ++;
            isOK = false;
        }

        if(getAvgRank() > getMaxRank()){
            System.out.println("ERROR: RankEvaluationResult.sanityCheck: avgRank " + getAvgRank()
                    + " > maxRank " + getMaxRank());
            avgMaxRankAnomalyCount ++;
            isOK = false;
        }
        if(getAvgRank() < getMinRank()){
//            System.out.println("ERROR: RankEvaluationResult.sanityCheck: avgRank < minRank");
            avgMinRankAnomalyCount ++;
            isOK = false;
        }

//        if(!isOK) {
//            System.out.println("       " + toString());
//        }

        return isOK;
    }

    public static void cleanSanityCheckSummary(){
        negativeRankAnomalyCount = 0;
        avgMaxRankAnomalyCount = 0;
        avgMinRankAnomalyCount = 0;
    }
    public static void displaySanityCheckSummary(){
        long totalAnomalies = negativeRankAnomalyCount + avgMaxRankAnomalyCount + avgMinRankAnomalyCount;
        if(totalAnomalies == 0){
            System.out.println("SanityCheck: OK!");
        }else{
            System.out.println("SanityCheck: ERROR: " + totalAnomalies + " anomalies in rank evaluation found!");
            System.out.println("  negativeRankAnomalyCount = " + negativeRankAnomalyCount);
            System.out.println("  avgMaxRankAnomalyCount =   " + avgMaxRankAnomalyCount);
            System.out.println("  avgMinRankAnomalyCount =   " + avgMinRankAnomalyCount);
        }
    }

//    public void updateRankCache(TreeMap<T, RankEvaluationResults> rankCache, T pc){
//        rankCache.put(pc, this);
//
//        if(this.getRankTotal() > DebugInfo.maxRankTotalRecorded){
//            DebugInfo.maxRankTotalRecorded = this.getRankTotal();
//        }
//        if(this.getPathCount() > DebugInfo.maxPathCountRecorded){
//            DebugInfo.maxPathCountRecorded = this.getPathCount();
//        }
//    }

}
