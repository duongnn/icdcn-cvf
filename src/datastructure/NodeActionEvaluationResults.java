package datastructure;

import template.ProgramConfigurationTemplate;
import template.SuccessorInfo;

import java.util.TreeSet;

/**
 * This class contains the results of evaluating actions of a node.
 * Specifically, it contains how many privileges (enabled actions) available at a node in a given program configuration.
 * It also contains the list of successors obtained by executing all those privileges.
 * Note that the size of successor list and the number of privileges is not always equal
 * because two privileges could result in the same successor.
 */

public class NodeActionEvaluationResults<PCT extends ProgramConfigurationTemplate> {
    int nodeId;  // the node on which the action is performed (i.e. the state of this node is changed)
    int privilegeCount;
    TreeSet<SuccessorInfo> successorList;

    public NodeActionEvaluationResults(int nodeId, int privilegeCount, TreeSet<SuccessorInfo> successorList){
        this.privilegeCount = privilegeCount;
        this.successorList = successorList;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getPrivilegeCount() {
        return privilegeCount;
    }

    public void setPrivilegeCount(int privilegeCount) {
        this.privilegeCount = privilegeCount;
    }

    public TreeSet<SuccessorInfo> getSuccessorList() {
        return successorList;
    }

    public void setSuccessorList(TreeSet<SuccessorInfo> successorList) {
        this.successorList = successorList;
    }
}
