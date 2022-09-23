package maxmatching;

import template.NodeStateTemplate;

import java.util.Collections;
import java.util.Objects;
import java.util.Vector;

import static main.AnalyzeCvfs.graphTopology;
import static main.Utility.getIntFromBoolean;
import static template.ProgramConfigurationTemplate.CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION;

/**
 * State of a node in self-stabilizing max-matching program described in paper
 * F. Manne, M. Mjelde, L. Pilard, and S. Tixeuil,
 * “A new self-stabilizing maximal matching algorithm,”
 * Theoretical Computer Science, vol. 410, no. 14, pp. 1336 – 1345, 2009,
 * structural Information and Communication Complexity (SIROCCO 2007)
 */

public class NodeStateMaxMatching extends NodeStateTemplate {
    public static final boolean MAX_MATCHING_MARRIED = true;
    public static final boolean MAX_MATCHING_UNMARRIED = false;
    public static final int MAX_MATCHING_PVALUE_NULL = -1; // pointer to no partner

    private int nodeId; // start from 0 till (number of node - 1)
    private boolean mvalue;  // true if this node is married. false if unmarried
    private int pvalue; // pointer to partner. value range is -1 to (number of node - 1)
                        // If pvalue of i = j and pvalue of j = i, then nodes i and j are married (matched)

    public NodeStateMaxMatching(int nodeId, boolean mvalue, int pvalue){
        this.nodeId = nodeId;
        this.mvalue = mvalue;
        this.pvalue = pvalue;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public boolean getMvalue() {
        return mvalue;
    }

    public void setMvalue(boolean mvalue) {
        this.mvalue = mvalue;
    }

    public int getPvalue() {
        return pvalue;
    }

    public void setPvalue(int pvalue) {
        this.pvalue = pvalue;
    }

    private void incrementPvalue(){
        this.pvalue ++;
    }

    private void flipMvalue(){
        mvalue = !mvalue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeStateMaxMatching that = (NodeStateMaxMatching) o;
        // compare content
        return nodeId == that.nodeId
                && mvalue == that.getMvalue()
                && pvalue == that.getPvalue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, mvalue, pvalue);
    }

    public NodeStateMaxMatching getDeepCopy(){
        return new NodeStateMaxMatching(getNodeId(), getMvalue(), getPvalue());
    }

    // implement Comparable Interface
    public int compareTo(Object o) {
        NodeStateMaxMatching otherState = (NodeStateMaxMatching) o;
        int myMvalue = getIntFromBoolean(this.getMvalue());
        int myPvalue = this.getPvalue();
        int theirMvalue = getIntFromBoolean(otherState.getMvalue());
        int theirPValue = otherState.getPvalue();

        // assume nodeId is the same
        // since it is used in compareTo() of ProgramConfigurationMaxMatching
        // in which we compare content of 2 nodes with same Id from 2 configurations
        // so we compare mvalue first, then pvalue
        if(myMvalue > theirMvalue){
            return 1;
        }
        if(myMvalue < theirMvalue){
            return -1;
        }

        // m-value are the same, compare p-value
        if(myPvalue > theirPValue){
            return 1;
        }
        if(myPvalue < theirPValue){
            return -1;
        }

        // m-value and p-value are identical
        return 0;
    }

    // functions for ordering between node states
    public boolean isNodeStateMinimal(){
        return ((getMvalue() == false) &&  // false = 0
                (getPvalue() == MAX_MATCHING_PVALUE_NULL));
    }

    public boolean isNodeStateMaximal(int cvf){
        if(cvf == CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION){
            return isNodeStateMaximalWithTopologyRestriction();
        }else{
            return isNodeStateMaximal();
        }
    }

    private boolean isNodeStateMaximal(){
        return ((getMvalue() == true) &&   // true = 1
                (getPvalue() == graphTopology.size() - 1));
    }

    // differs from isNodeStateMaximal in that it consider topology restriction.
    // It is expected to be more efficient
    private boolean isNodeStateMaximalWithTopologyRestriction(){
        Vector<Integer> nbrList = graphTopology.get(nodeId);
        int maxIdOfNbr = Collections.max(nbrList);

        return ((getMvalue() == true) &&   // true = 1
                (getPvalue() >= maxIdOfNbr)); // we use >= instead of == maxIdOfNbr since that covers the case
                                              // where Pvalue is accidentally perturbed greater than maxIdOfNbr
    }

    // this function is not used since it create new memory space
//    public NodeStateMaxMatching getNextNodeState(){
//        int maximalPvalue = graphTopology.size() - 1;
//
//        if(getPvalue() < maximalPvalue){
//            return new NodeStateMaxMatching(getNodeId(), getMvalue(), getPvalue() + 1);
//        }else{
//            // we need to flip the m-value and make pvalue minimal
//            return new NodeStateMaxMatching(getNodeId(), !getMvalue(), MAX_MATCHING_PVALUE_NULL);
//        }
//    }


    public NodeStateMaxMatching moveToNextNodeState(int cvf){
        if(cvf == CVF_AS_CONSTRAINED_PERTURBATION_AND_TOPOLOGY_RESTRICTION){
            return moveToNextNodeStateWithTopologyRestriction();
        }else{
            return moveToNextNodeState();
        }
    }

    // this function does not allocate new memory space
    private NodeStateMaxMatching moveToNextNodeState(){
        int maximalPvalue = graphTopology.size() - 1;

        if(getPvalue() < maximalPvalue){
            this.incrementPvalue();
        }else{
            // we need to flip the m-value and make pvalue minimal
            this.flipMvalue();
            this.setPvalue(MAX_MATCHING_PVALUE_NULL);
        }

        return this;
    }

    // Differs from moveToNextNodeState in that it consider topology
    // It is expected to be more efficient
    private NodeStateMaxMatching moveToNextNodeStateWithTopologyRestriction(){
        Vector<Integer> nbrList = graphTopology.get(nodeId);
        Collections.sort(nbrList);
        int maximalPvalue = Collections.max(nbrList);

        if(getPvalue() < maximalPvalue){

            // change P-value to first value in nbrList that is greater than current p-value
            // note that we should not use indexOf(getPvalue()) + 1
            // since current P-value may not belong to nbrList due to perturbation
            int indexOfTheNbr = 0;
            while(nbrList.elementAt(indexOfTheNbr) <= getPvalue()){
                indexOfTheNbr ++;
            }
            this.setPvalue(nbrList.elementAt(indexOfTheNbr));

        }else{
            // we need to flip the m-value and make pvalue minimal
            this.flipMvalue();
            this.setPvalue(MAX_MATCHING_PVALUE_NULL);
        }

        return this;
    }

    public NodeStateMaxMatching moveToMinimalState(){
        this.setMvalue(false);
        this.setPvalue(MAX_MATCHING_PVALUE_NULL);
        return this;
    }

    public String toString(){
        StringBuilder s = new StringBuilder();
        s.append(mvalue).append(":").append(pvalue);
        return s.toString();
    }

}
