package datastructure;

import template.ProgramConfigurationTemplate;
import template.SuccessorInfo;

import java.util.TreeSet;

/**
 * This class contains the results of perturbing the state of a node.
 * Specifically, it contains the list of successors obtained by perturbing the state of that node
 *
 */

public class NodePerturbationResults<PCT extends ProgramConfigurationTemplate> {
    private TreeSet<SuccessorInfo> perturbedSuccessorList;

    public NodePerturbationResults(TreeSet<SuccessorInfo> perturbedSuccessorList) {
        this.perturbedSuccessorList = perturbedSuccessorList;
    }

    public TreeSet<SuccessorInfo> getPerturbedSuccessorList() {
        return perturbedSuccessorList;
    }

    public void setPerturbedSuccessorList(TreeSet<SuccessorInfo> perturbedSuccessorList) {
        this.perturbedSuccessorList = perturbedSuccessorList;
    }
}
