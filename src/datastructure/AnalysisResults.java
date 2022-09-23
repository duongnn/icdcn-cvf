package datastructure;

import template.ProgramConfigurationTemplate;

import java.util.TreeMap;
import java.util.Vector;

/**
 * Bundles of information for analysis rank effect of a program
 */

public class AnalysisResults<PCT extends ProgramConfigurationTemplate> {
    // mapping a program configuration to related information
    private TreeMap<PCT, ProgramConfigurationInformation<PCT>> progConfigInfoMap;

    // mapping a rank to the number of program configs having that rank
    private TreeMap<Integer, Integer> progConfigRankDistribution;

    // mapping a rank change/effect (should be <=0)
    // to the number of (normal) program transitions having that rank effect.
    // Note that, any program transition starting from a state in the invariant
    // (whose rank is 0) will lead to a state in the invariant (rank is 0 too). Thus
    // rank effect of such a transition is 0 - 0 = 0, and we do not include them.
    // We only map program transition outside the invariant.
    private TreeMap<Integer, Integer> progTransOutsideInvRankEffectDistribution;
    private int numberOfProgTransOutsideInv;
    private int progTransOutsideInvTotalRankEffect;
    private int numberOfProgTransInsideInv;

    // mapping a rank change/effect (could be negative, positive, 0)
    // to the number of cvfs having that rank effect.
    // We consider two kinds of cvf:
    //   ones occur at a state inside the invariant
    //   ones occur at a state outside the invariant.
    private TreeMap<Integer, Integer> cvfInsideInvRankEffectDistribution;
    private TreeMap<Integer, Integer> cvfOutsideInvRankEffectDistribution;
    private int numberOfCvfInsideInv;
    private int numberOfCvfOutsideInv;
    private int cvfInsideInvTotalRankEffect;
    private int cvfOutsideInvTotalRankEffect;
    // more detail of cvfOutsideInvRankEffectDistribution.
    // Specifically, the element at index nodeId of the vector shows
    // the rank effect of cvfs outside invariant occurring on node nodeId;
    private Vector<TreeMap<Integer, Integer>> perNodeCvfOutsideInvRankEffectDistribution;

    public AnalysisResults(TreeMap<PCT, ProgramConfigurationInformation<PCT>> progConfigInfoMap,
                           TreeMap<Integer, Integer> progConfigRankDistribution){
        this.progConfigInfoMap = progConfigInfoMap;
        this.progConfigRankDistribution = progConfigRankDistribution;
    }

    // full constructor
    public AnalysisResults(
            TreeMap<PCT, ProgramConfigurationInformation<PCT>> progConfigInfoMap,
            TreeMap<Integer, Integer> progConfigRankDistribution,
            TreeMap<Integer, Integer> progTransOutsideInvRankEffectDistribution,
            int numberOfProgTransOutsideInv,
            int progTransOutsideInvTotalRankEffect,
            int numberOfProgTransInsideInv,
            TreeMap<Integer, Integer> cvfInsideInvRankEffectDistribution,
            TreeMap<Integer, Integer> cvfOutsideInvRankEffectDistribution,
            int numberOfCvfInsideInv,
            int numberOfCvfOutsideInv,
            int cvfInsideInvTotalRankEffect,
            int cvfOutsideInvTotalRankEffect){

        this.progConfigInfoMap = progConfigInfoMap;
        this.progConfigRankDistribution = progConfigRankDistribution;
        this.progTransOutsideInvRankEffectDistribution = progTransOutsideInvRankEffectDistribution;
        this.numberOfProgTransOutsideInv = numberOfProgTransOutsideInv;
        this.progTransOutsideInvTotalRankEffect = progTransOutsideInvTotalRankEffect;
        this.numberOfProgTransInsideInv = numberOfProgTransInsideInv;
        this.cvfInsideInvRankEffectDistribution = cvfInsideInvRankEffectDistribution;
        this.cvfOutsideInvRankEffectDistribution = cvfOutsideInvRankEffectDistribution;
        this.numberOfCvfInsideInv = numberOfCvfInsideInv;
        this.numberOfCvfOutsideInv = numberOfCvfOutsideInv;
        this.cvfInsideInvTotalRankEffect = cvfInsideInvTotalRankEffect;
        this.cvfOutsideInvTotalRankEffect = cvfOutsideInvTotalRankEffect;
        // disable the code below because it is used only in transition-based
        //this.perNodeCvfOutsideInvRankEffectDistribution = perNodeCvfOutsideInvRankEffectDistribution;
    }

    public void initAnalysisResults(){
        // program transitions related
        setNumberOfProgTransInsideInv(0);

        setProgTransOutsideInvRankEffectDistribution(new TreeMap<>());
        setNumberOfProgTransOutsideInv(0);
        setProgTransOutsideInvTotalRankEffect(0);

        // cvf related
        setCvfInsideInvRankEffectDistribution(new TreeMap<>());
        setNumberOfCvfInsideInv(0);
        setCvfInsideInvTotalRankEffect(0);

        setCvfOutsideInvRankEffectDistribution(new TreeMap<>());
        setNumberOfCvfOutsideInv(0);
        setCvfOutsideInvTotalRankEffect(0);

        setPerNodeCvfOutsideInvRankEffectDistribution(new Vector<>());
    }

    public TreeMap<PCT, ProgramConfigurationInformation<PCT>> getProgConfigInfoMap() {
        return progConfigInfoMap;
    }

    public void setProgConfigInfoMap(TreeMap<PCT, ProgramConfigurationInformation<PCT>> progConfigInfoMap) {
        this.progConfigInfoMap = progConfigInfoMap;
    }

    public TreeMap<Integer, Integer> getProgConfigRankDistribution() {
        return progConfigRankDistribution;
    }

    public void setProgConfigRankDistribution(TreeMap<Integer, Integer> progConfigRankDistribution) {
        this.progConfigRankDistribution = progConfigRankDistribution;
    }

    public TreeMap<Integer, Integer> getProgTransOutsideInvRankEffectDistribution() {
        return progTransOutsideInvRankEffectDistribution;
    }

    public void setProgTransOutsideInvRankEffectDistribution(TreeMap<Integer, Integer> progTransOutsideInvRankEffectDistribution) {
        this.progTransOutsideInvRankEffectDistribution = progTransOutsideInvRankEffectDistribution;
    }

    public int getNumberOfProgTransOutsideInv() {
        return numberOfProgTransOutsideInv;
    }

    public void setNumberOfProgTransOutsideInv(int numberOfProgTransOutsideInv) {
        this.numberOfProgTransOutsideInv = numberOfProgTransOutsideInv;
    }

    public void incrementNumberOfProgTransOutsideInv(){
        this.numberOfProgTransOutsideInv ++;
    }

    public int getProgTransOutsideInvTotalRankEffect() {
        return progTransOutsideInvTotalRankEffect;
    }

    public void setProgTransOutsideInvTotalRankEffect(int progTransOutsideInvTotalRankEffect) {
        this.progTransOutsideInvTotalRankEffect = progTransOutsideInvTotalRankEffect;
    }

    public void increaseProgTransOutsideInvTotalRankEffect(int inc){
        this.progTransOutsideInvTotalRankEffect += inc;
    }

    public double getProgTransOutsideInvAverageRankEffect(){
        return 1.0*progTransOutsideInvTotalRankEffect/numberOfProgTransOutsideInv;
    }

    public int getNumberOfProgTransInsideInv() {
        return numberOfProgTransInsideInv;
    }

    public void setNumberOfProgTransInsideInv(int numberOfProgTransInsideInv) {
        this.numberOfProgTransInsideInv = numberOfProgTransInsideInv;
    }

    public void incrementNumberOfProgTransInsideInv(){
        this.numberOfProgTransInsideInv ++;
    }

    public TreeMap<Integer, Integer> getCvfInsideInvRankEffectDistribution() {
        return cvfInsideInvRankEffectDistribution;
    }

    public void setCvfInsideInvRankEffectDistribution(TreeMap<Integer, Integer> cvfInsideInvRankEffectDistribution) {
        this.cvfInsideInvRankEffectDistribution = cvfInsideInvRankEffectDistribution;
    }

    public TreeMap<Integer, Integer> getCvfOutsideInvRankEffectDistribution() {
        return cvfOutsideInvRankEffectDistribution;
    }

    public void setCvfOutsideInvRankEffectDistribution(TreeMap<Integer, Integer> cvfOutsideInvRankEffectDistribution) {
        this.cvfOutsideInvRankEffectDistribution = cvfOutsideInvRankEffectDistribution;
    }

    public int getNumberOfCvfInsideInv() {
        return numberOfCvfInsideInv;
    }

    public void setNumberOfCvfInsideInv(int numberOfCvfInsideInv) {
        this.numberOfCvfInsideInv = numberOfCvfInsideInv;
    }

    public void incrementNumberOfCvfInsideInv(){
        this.numberOfCvfInsideInv ++;
    }

    public int getNumberOfCvfOutsideInv() {
        return numberOfCvfOutsideInv;
    }

    public void setNumberOfCvfOutsideInv(int numberOfCvfOutsideInv) {
        this.numberOfCvfOutsideInv = numberOfCvfOutsideInv;
    }

    public void incrementNumberOfCvfOutsideInv(){
        this.numberOfCvfOutsideInv ++;
    }

    public int getNumberOfCvf(){
        return numberOfCvfInsideInv + numberOfCvfOutsideInv;
    }

    public int getCvfInsideInvTotalRankEffect() {
        return cvfInsideInvTotalRankEffect;
    }

    public void setCvfInsideInvTotalRankEffect(int cvfInsideInvTotalRankEffect) {
        this.cvfInsideInvTotalRankEffect = cvfInsideInvTotalRankEffect;
    }

    public void increaseCvfInsideInvTotalRankEffect(int inc){
        this.cvfInsideInvTotalRankEffect += inc;
    }

    public double getCvfInsideInvAverageRankEffect(){
        return 1.0*cvfInsideInvTotalRankEffect/numberOfCvfInsideInv;
    }

    public int getCvfOutsideInvTotalRankEffect() {
        return cvfOutsideInvTotalRankEffect;
    }

    public void setCvfOutsideInvTotalRankEffect(int cvfOutsideInvTotalRankEffect) {
        this.cvfOutsideInvTotalRankEffect = cvfOutsideInvTotalRankEffect;
    }

    public void increaseCvfOutsideInvTotalRankEffect(int inc){
        this.cvfOutsideInvTotalRankEffect += inc;
    }

    public double getCvfOutsideInvAverageRankEffect(){
        return 1.0*cvfOutsideInvTotalRankEffect/numberOfCvfOutsideInv;
    }

    public int getCvfTotalRankEffect(){
        return cvfInsideInvTotalRankEffect + cvfOutsideInvTotalRankEffect;
    }

    public Vector<TreeMap<Integer, Integer>> getPerNodeCvfOutsideInvRankEffectDistribution() {
        return perNodeCvfOutsideInvRankEffectDistribution;
    }

    public void setPerNodeCvfOutsideInvRankEffectDistribution(Vector<TreeMap<Integer, Integer>> perNodeCvfOutsideInvRankEffectDistribution) {
        this.perNodeCvfOutsideInvRankEffectDistribution = perNodeCvfOutsideInvRankEffectDistribution;
    }

    // comparing if the progConfigInfoMap are the same
    public boolean isProgConfigInfoMapEqual(AnalysisResults otherResults){
        TreeMap<PCT, ProgramConfigurationInformation<PCT>> other = otherResults.getProgConfigInfoMap();

        for(PCT pc : getProgConfigInfoMap().keySet()){
            if(!other.containsKey(pc))
                return false;

            if(! getProgConfigInfoMap().get(pc).isEqualToOther(other.get(pc)))
                return false;
        }

        return true;
    }

    // compare if the progConfigRankDistribution are the same
    public boolean isProgConfigRankDistributionEqual(AnalysisResults otherResults){
        TreeMap<Integer, Integer> other = otherResults.getProgConfigRankDistribution();

        if(getProgConfigRankDistribution().size() != other.size()) {
            System.out.println("size differs: " + getProgConfigRankDistribution().size() + " vs. " + other.size());

            return false;
        }

        for(int rankOffset : getProgConfigRankDistribution().keySet()){
            if(getProgConfigRankDistribution().get(rankOffset).intValue() != other.get(rankOffset).intValue()) {
                System.out.println(" count for rankOffset " + rankOffset + " differs: "
                                    + getProgConfigRankDistribution().get(rankOffset) + " vs. " +  other.get(rankOffset));

                return false;
            }
        }

        return true;
    }

    // compare if progTransOutsideInvRankEffectDistribution are the same
    public boolean isProgTransOutsideInvRankEffectDistributionEqual(AnalysisResults otherResults){
        TreeMap<Integer, Integer> other = otherResults.getProgTransOutsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> mine =getProgTransOutsideInvRankEffectDistribution();

        if(getNumberOfProgTransOutsideInv() != otherResults.getNumberOfProgTransOutsideInv()) {
//            System.out.println(" numberOfProgTransOutsideInv differs");
            return false;
        }
        if(getProgTransOutsideInvTotalRankEffect() != otherResults.getProgTransOutsideInvTotalRankEffect()) {
//            System.out.println(" progTransOutsideInvTotalRankEffect differs");
            return false;
        }
        if(getNumberOfProgTransOutsideInv() != otherResults.getNumberOfProgTransOutsideInv()) {
//            System.out.println(" numberOfProgTransOutsideInv differs");
            return false;
        }

        for(int re : mine.keySet()){
            if(mine.get(re).intValue() != other.get(re).intValue()) {
//                System.out.println(" count for re = " + re + " differs: " + mine.get(re) + " != " + other.get(re));

                return false;
            }
        }

        return true;
    }

    // compare if cvfInsideInvRankEffectDistribution are the same
    public boolean isCvfInsideInvRankEffectDistributionEqual(AnalysisResults otherResult){
        TreeMap<Integer, Integer> other = otherResult.getCvfInsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> mine = getCvfInsideInvRankEffectDistribution();

        if(getNumberOfCvfInsideInv() != otherResult.getNumberOfCvfInsideInv()) {
//            System.out.println(" numberOfCvfInsideInv differs + " + getNumberOfCvfInsideInv()
//                    + " != " + otherResult.getNumberOfCvfInsideInv());
            return false;
        }
        if(getCvfInsideInvTotalRankEffect() != otherResult.getCvfInsideInvTotalRankEffect()) {
//            System.out.println(" cvfInsideInvTotalRankEffect differs + " + getCvfInsideInvTotalRankEffect()
//                    + " != " + otherResult.getCvfInsideInvTotalRankEffect());
            return false;
        }
        for(int re : mine.keySet()){
            if(mine.get(re).intValue() != other.get(re).intValue()) {
//                System.out.println(" count for re = " + re + " differs " + mine.get(re) + " != " + other.get(re));
                return false;
            }
        }

        return true;
    }

    // compare if cvfOutsideInvRankEffectDistribution are the same
    public boolean isCvfOutsideInvRankEffectDistributionEqual(AnalysisResults otherResult){
        TreeMap<Integer, Integer> other = otherResult.getCvfOutsideInvRankEffectDistribution();
        TreeMap<Integer, Integer> mine = getCvfOutsideInvRankEffectDistribution();

        if(getNumberOfCvfOutsideInv() != otherResult.getNumberOfCvfOutsideInv()) {
            return false;
        }
        if(getCvfOutsideInvTotalRankEffect() != otherResult.getCvfOutsideInvTotalRankEffect()) {
            return false;
        }
        for(int re : mine.keySet()){
            if(mine.get(re).intValue() != other.get(re).intValue()) {
//                System.out.println(" count for re = " + re + " differs " + mine.get(re) + " != " + other.get(re));
                return false;
            }
        }

        return true;
    }

    public boolean isEqualToOther(AnalysisResults otherResult){
        boolean isEqual = true;

        if(!isProgConfigInfoMapEqual(otherResult)){
            System.out.println("progConfigInfoMap DIFFERS");
            isEqual = false;
        }

        if(!isProgConfigRankDistributionEqual(otherResult)){
            System.out.println("progConfigRankDistribution DIFFERS");
            isEqual = false;
        }

        if(!isProgTransOutsideInvRankEffectDistributionEqual(otherResult)){
            System.out.println("progTransOutsideInvRankEffectDistribution DIFFERS");
            isEqual = false;
        }

        if(!isCvfInsideInvRankEffectDistributionEqual(otherResult)){
            System.out.println("cvfInsideInvRankEffectDistribution DIFFERS");
            isEqual = false;
        }

        if(!isCvfOutsideInvRankEffectDistributionEqual(otherResult)){
            System.out.println("cvfOutsideInvRankEffectDistribution DIFFERS");
            isEqual = false;
        }

        return isEqual;
    }

}
