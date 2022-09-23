package template;

/**
 * Template of the state of a node,
 * which is an assignment of nodeId to a value
 */
public abstract class NodeStateTemplate implements Comparable{
    /**
     * Overriding equals method of Object class
     * @param otherNodeState
     * @return true if the state of this node is equal to the state of other node
     */
    @Override
    abstract public boolean equals(Object otherNodeState);

    /**
     * When overriding equals(), we override hashCode().
     * @return the hash of this node state
     */
    @Override
    abstract public int hashCode();

    abstract public NodeStateTemplate getDeepCopy();

    abstract public String toString();
}
