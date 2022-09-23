package template;

/**
 * This class serves the purpose of tracing the cvf (and perhaps program transition) at different nodes
 */

public class SuccessorInfo implements Comparable {
    int idOfChangedNode; // In a configuration/state transition from some configuration to a successor,
                // the state of exactly one node is changed/perturbed.
                // This field indicates the ID of that node.
    ProgramConfigurationTemplate successorProgramConfig; // the configuration of this successor

    public SuccessorInfo(int idOfChangedNode, ProgramConfigurationTemplate spc){
        this.idOfChangedNode = idOfChangedNode;
        this.successorProgramConfig = spc;

    }
    public int getIdOfChangedNode() {
        return idOfChangedNode;
    }

    public void setIdOfChangedNode(int idOfChangedNode) {
        this.idOfChangedNode = idOfChangedNode;
    }

    public ProgramConfigurationTemplate getSuccessorProgramConfig() {
        return successorProgramConfig;
    }

    public void setSuccessorProgramConfig(ProgramConfigurationTemplate successorProgramConfig) {
        this.successorProgramConfig = successorProgramConfig;
    }

    // implement Comparable interface
    public int compareTo(Object o){
        SuccessorInfo other = (SuccessorInfo) o;

        if(this.getIdOfChangedNode() > other.getIdOfChangedNode())
            return 1;
        if(this.getIdOfChangedNode() < other.getIdOfChangedNode())
            return -1;

        return this.getSuccessorProgramConfig().compareTo(other.getSuccessorProgramConfig());

    }

    public String toString(){
        return successorProgramConfig.toString();
    }
}
