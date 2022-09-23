package datastructure;

import template.ProgramConfigurationTemplate;
import template.SuccessorInfo;

import java.math.BigInteger;
import java.util.TreeMap;
import java.util.TreeSet;

import static template.ProgramConfigurationTemplate.INFINITY_RANK;

/**
 * This class encapsulates information about a program configuration
 * such as its max rank and list of successors
 *
 */

public class ProgramConfigurationInformation<PCT extends ProgramConfigurationTemplate> {
    public static final int DEFAULT_MIN_RANK = -1;

    private int maxRank; // maximum length to invariant
    private int minRank; // minimum length to invariant
    // for average rank
    private BigInteger totalPathLength;
    private BigInteger numberOfPaths;

    // list of successors obtained by program transitions
    private TreeSet<SuccessorInfo> progSuccessorList;

    // list of successors obtained by a cvf, i.e. a perturbation of a node state
    private TreeSet<SuccessorInfo> cvfSuccessorList;
    private int totalCvfMaxRankOffset;

    public ProgramConfigurationInformation(
            int maxRank,
            int minRank,
            BigInteger totalPathLength,
            BigInteger numberOfPaths,
            TreeSet<SuccessorInfo> successorList,
            TreeSet<SuccessorInfo> cvfSuccessorList){
        this.maxRank = maxRank;
        this.minRank = minRank;
        this.totalPathLength = totalPathLength;
        this.numberOfPaths = numberOfPaths;

        this.progSuccessorList = successorList;
        this.cvfSuccessorList = cvfSuccessorList;
        this.totalCvfMaxRankOffset = 0;
    }

    public boolean isEqualToOther(ProgramConfigurationInformation<PCT> other){
        if(this.getMaxRank() != other.getMaxRank())
            return false;

        if(this.getProgSuccessorList().size() != other.getProgSuccessorList().size())
            return false;
        for(SuccessorInfo succ : this.getProgSuccessorList()){
            if(!other.getProgSuccessorList().contains(succ))
                return false;
        }

        if(this.getCvfSuccessorList().size() != other.getCvfSuccessorList().size())
            return false;
        for(SuccessorInfo succ : this.getCvfSuccessorList()){
            if(!other.getCvfSuccessorList().contains(succ))
                return false;
        }

        // skip totalCvfMaxRankOffset since it may be not computed

        return true;
    }

    public int getMaxRank() {
        return maxRank;
    }

    public void setMaxRank(int maxRank) {
        this.maxRank = maxRank;
    }

    public TreeSet<SuccessorInfo> getProgSuccessorList() {
        return progSuccessorList;
    }

    public void setProgSuccessorList(TreeSet<SuccessorInfo> progSuccessorList) {
        this.progSuccessorList = progSuccessorList;
    }

    public TreeSet<SuccessorInfo> getCvfSuccessorList() {
        return cvfSuccessorList;
    }

    public void setCvfSuccessorList(TreeSet<SuccessorInfo> cvfSuccessorList) {
        this.cvfSuccessorList = cvfSuccessorList;
    }

    public int getTotalCvfMaxRankOffset() {
        return totalCvfMaxRankOffset;
    }

    public void setTotalCvfMaxRankOffset(int totalCvfMaxRankOffset) {
        this.totalCvfMaxRankOffset = totalCvfMaxRankOffset;
    }

    public int getMinRank() {
        return minRank;
    }

    public void setMinRank(int minRank) {
        this.minRank = minRank;
    }

    public BigInteger getTotalPathLength() {
        return totalPathLength;
    }

    public void setTotalPathLength(BigInteger totalPathLength) {
        this.totalPathLength = totalPathLength;
    }

    public BigInteger getNumberOfPaths() {
        return numberOfPaths;
    }

    public void setNumberOfPaths(BigInteger numberOfPaths) {
        this.numberOfPaths = numberOfPaths;
    }

    public int getAvgRank(){
        //return (int) ((totalPathLength + numberOfPaths - 1)/numberOfPaths);
        return totalPathLength.add(numberOfPaths).subtract(BigInteger.ONE).divide(numberOfPaths).intValue();
    }

    public String toString(TreeMap<SuccessorInfo, ProgramConfigurationInformation<PCT>> progConfigInfoMap, int oneRankIndent, boolean displayCvfInfo){
        StringBuilder result = new StringBuilder();
        StringBuilder oneRankIndentStr = new StringBuilder();

        if(displayCvfInfo) {
            // cvf effect info string
            int totalCvfOffset = 0;
            for(SuccessorInfo cvfSuccessor : getCvfSuccessorList()){
                int cvfSuccessorMaxRank = progConfigInfoMap.get(cvfSuccessor).getMaxRank();
                if(cvfSuccessorMaxRank == INFINITY_RANK){
                    return "ERROR ProgramConfigurationInformation.toString() compute cvf effect while rank computation has not converged";
                }
                totalCvfOffset += (cvfSuccessorMaxRank - getMaxRank());
            }
            setTotalCvfMaxRankOffset(totalCvfOffset);

            result.append(String.format(" cvf effect: %5d, %5d, %7.2f ",
                    totalCvfOffset,
                    getCvfSuccessorList().size(),
                    (1.0*totalCvfOffset/getCvfSuccessorList().size())));
        }

        result.append(" => rank: ");

        // indentation
        for(int i = 0; i < oneRankIndent; i ++)
            oneRankIndentStr.append(" ");
        int adjustedMaxRank = getMaxRank();
        if(adjustedMaxRank == INFINITY_RANK){
            adjustedMaxRank = 0;
        }
        for(int i = 0; i < adjustedMaxRank; i++){
            result.append(oneRankIndentStr);
        }

        if(getMaxRank() == INFINITY_RANK){
            result.append(" infinity");
            return result.toString();
        }


        // node (max) rank
        result.append(String.format(" %8d", getMaxRank()));

        // successor info string
        int totalSuccessorOffset = 0;
        StringBuilder successorInfo = new StringBuilder();
        successorInfo.append(" (");
        for(SuccessorInfo successor : getProgSuccessorList()){
            int successorMaxRank = progConfigInfoMap.get(successor).getMaxRank();
            totalSuccessorOffset += (successorMaxRank - getMaxRank());
            successorInfo.append(successor.toString() + " [" + successorMaxRank + "], ");
        }
        //successorInfo.append(totalSuccessorOffset + ")");
        successorInfo.append(String.format(" rank change: %d, %d, %.2f )",
                totalSuccessorOffset,
                getProgSuccessorList().size(),
                (1.0*totalSuccessorOffset/getProgSuccessorList().size())));
        if(getMaxRank() == 0){
            result.append(" (invariant)");
        }else{
            result.append(successorInfo);
        }

        return result.toString();
    }
}
