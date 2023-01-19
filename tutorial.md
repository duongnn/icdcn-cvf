In this tutorial, we will see how to add a program to the framework so that we can analyze the effect of `cvfs` and program transitions.


Tutorial outline:
1. [General steps for including a program into our framework](#general-steps)
2. [Demonstration with Dijkstra's three state program.](#demo)
    - [Create a package for program.](#demo-create-pkg)
    - [Add `NodeStateTemplate` subclass to the package](#demo-nodestate)
    - [Add `ProgramConfigurationTemplate` subclass to package](demo-program-config)

	
<div id="general-steps"></div>

## General steps for including a program into our framework

To include a program, says program `A`, into our framework, there are 3 steps:
+ Step 1: Create a package for program `A`
+ Step 2: Inside that package, we add a class for storing the state (variables and their values) of a node in program `A`, says `NodeStateA`. 
		`NodeStateA` is a subclass of the framework's abstract class `NodeStateTemplate` and should override the following abstract methods working with the state of a node.
    
      public int compareTo(Ojbect) // since NodeStateTemplate implements interface Comparable
      public boolean equals(Object)
      public int hashcode()
      public NodeStateA getDeepCopy()
        // Any change to the state of the copy should not affect the state of the current node and vice versa.
      public String toString()
      
    Class `NodeStateA` also needs basic methods such as constructors, getters, and setters.

+ Step 3: We also add to the package a class for storing the configuration (i.e. the combination of the states of all nodes) of program `A`, says `ProgramConfigurationA`. 
		`ProgramConfigurationA` is a subclass of the framework's abstract class `ProgramConfigurationTemplate` and should override methods working with a configuration of program `A`. The central method is `evaluateANodeActions` which simulates a program transition (execution of an action at a given node).
    
    First are methods for enumerating program `A`'s configurations.
    
			public int compareTo(Object)  // since ProgramConfigurationTemplate implements interface Comparable
			public ProgramConfigurationA moveToFirstProgramConfig()
			public ProgramConfigurationA moveToNextProgramConfig()
			public ProgramConfigurationA moveToLastProgramConfig()
			public ProgramConfigurationA moveToNthProgramConfig()
    
    We are free to choose the order in which we enumerate all program configurations. Once the enumeration scheme is defined, the methods `compareTo`, `moveToFirst/Next/Nth/LastProgramConfig` must be consistent with that scheme.

    Next are these methods:
    
			public boolean isInsideInvariant()
			public ProgramConfigurationA getDeepCopy()
			public long getSizeOfStateSpace()
			public NodeActionEvaluationResults evaluateANodeActions(int nodeId)
    
    Finally are methods for perturbing the state of a node:
    
			public NodePerturbationResults perturbANodeArbitrarily(int nodeId)
			public NodePerturbationResults perturbANodeWithConstraint(int nodeId)
			public NodePerturbationResults perturbANodeWithConstraintAndTopologyRestriction(int nodeId)

    The constructor for `ProgramConfigurationA` often just calls superclass constructor.


## Demonstration with Dijkstra's ring program <span id="demo"><span>

Description of Dijkstra's ring prorams can be found in Dijkstra's paper [Self-stabilizing systems in spite of Distributed Control](https://dl.acm.org/doi/10.1145/361179.361202). The program described below is the third program (3-state machines) in the paper.

The framework's source code is available [here.](https://github.com/duongnn/icdcn-cvf)

### 2.1. Create a package for Dijkstra program, says `newdijkstra` <span id="demo-create-pkg"><span>

We add a new directory `newdijkstra` under the directory `src`

### 2.2. Add class `NodeStateNewDijkstra` to the package <span id="demo-nodestate"><span>

We create a new file `NodeStateNewDijkstra.java` in the directory `src/newdijkstra/`. The complete source code for `NodeStateNewDijkstra.java` is available [here](https://github.com/duongnn/icdcn-cvf/blob/0d668a5a97e980746814682feb2b6f718590d3fd/src/newdijkstra/NodeStateNewDijkstra.java)

In Dijkstra program, state of a node is an integer in the set {0, 1, 2}.
So the class `NodeStateNewDijkstra` has a field says `int nodeValue` for storing this integer.
We also need the field `int nodeId` to differentiate the nodes.
We also declare some constants in this class
	
```
    public static final int NEW_DIJKSTRA_NODE_STATE_0 = 0;
    public static final int NEW_DIJKSTRA_NODE_STATE_1 = 1;
    public static final int NEW_DIJKSTRA_NODE_STATE_2 = 2;
    public static final int NEW_DIJKSTRA_NUMBER_OF_NODE_STATES = 3;

    private int nodeId; // id of a node in the ring. From 0 to n-1 where n is the number of nodes in the ring.
    private int nodeValue; // node value. From {0, 1, 2} according to Dijkstra 3 state program
	
```

The constructor for `NodeStateNewDijkstra` 

```
    public NodeStateNewDijkstra(int nodeId, int nodeValue){
        this.nodeId = nodeId;
        this.nodeValue = nodeValue;
    }
```

Methods for setting and getting the values of those fields.
	
```
    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getNodeValue() {
        return nodeValue;
    }

    public void setNodeValue(int nodeValue) {
        this.nodeValue = nodeValue;
    }
```

When implementing the `compareTo()` method of `Comparable` interface, we are free to define how to compare two objects of `NodeStateNewDijkstra`. 
For example, we use field `nodeValue` to compare states of two nodes.
If nodeValue are equal, we use nodeId to break the tie.

```
    public int compareTo(Object o){
        if (this == o)
            return 0;
        // assume o is not null
        NodeStateNewDijkstra other = (NodeStateNewDijkstra) o;

        // we can define whatever way of comparing two NodeStateNewDijkstra objects suitable for our purpose
        // for example, we can compare nodeId first
        //      if they differ then the comparison will be the results
        //      if they equal then we compare nodeValue
        if (this.getNodeId() < other.getNodeId())
            return -1;
        if (this.getNodeId() > other.getNodeId())
            return 1;
        // now nodeIds are equals, compare nodeValue
        if(this.getNodeValue() < other.getNodeValue())
            return -1;
        if(this.getNodeValue() > other.getNodeValue())
            return 1;
        // nodeIds and nodeValues are equals, so two objects' contents are equal
        return 0;
    }
```
	   		
Then abstract methods of `NodeStateTemplate`:

```
    @Override
    public boolean equals(Object otherNodeState){
        if (this == otherNodeState)
            return true;
        if(otherNodeState == null || otherNodeState.getClass() != this.getClass())
            return false;
        NodeStateNewDijkstra that = (NodeStateNewDijkstra) otherNodeState;
        return (this.getNodeValue() == that.getNodeValue() && this.getNodeId() == that.getNodeId());
    }
	
    @Override
    public int hashCode(){
        return Objects.hash(nodeId, nodeValue);
    }
    
    @Override
    // returned a deep copy of the current NodeStateNewDijkstra object
    // any change to the state of the returned copy should not affect the state of the current node and vice versa.
    public NodeStateNewDijkstra getDeepCopy(){
        return new NodeStateNewDijkstra(this.getNodeId(), this.getNodeValue());
    }

    @Override
    // return a string representation of NodeStateNewDijkstra object
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("nodeId = " + this.getNodeId() + " ");
        str.append("nodeValue = " + this.getNodeValue());
        return str.toString();
    }

```


	
	
### 2.3. Add class `ProgramConfigurationNewDijkstra` to package <span id="demo-program-config"><span>
	
We create a new file `ProgramConfigurationNewDijkstra.java` in the directory `src/newdijkstra/`.
The complete source code of `ProgramConfigurationNewDijkstra.java` is available [here](https://github.com/duongnn/icdcn-cvf/blob/be2eeeb22640a856268641072a4854331dd7a639/src/newdijkstra/ProgramConfigurationNewDijkstra.java).

For Dijkstra's program, we do not have any program-specific information to keep.
All necessary information are already declared in the superclass ProgramConfigurationTemplate.
So the we do not define new fields in ProgramConfigurationNewDijkstra, and its constructor simply calls the superclass' constructor.
	
```
    public ProgramConfigurationNewDijkstra(int numberOfNodes, TreeMap<Integer, NodeStateNewDijkstra> nodeStateMap, int cvf, int probeLimit){
        super(numberOfNodes, nodeStateMap, cvf, probeLimit);
    }
	
```
	
Next, we implement the `compareTo()` method of `Comparable` interface, and implement abstract methods of `ProgramConfigurationTemplate`.

To implement `compareTo()`, we first identify the order in which we enumerate all the program configurations in Dijkstra's program.
One way to enumerate the program configurations is using lexicographical order.
For example, with 3 nodes, the enumberation will be:
	
		0-0-0 (state of node 0, 1, 2 is 0, 0, and 0 respectively)
		0-0-1
		0-1-0
		0-1-1
		...
		1-1-0
		1-1-1

With the above scheme for enumerating program configurations, we can implement `compareTo()` as follows:
```
    public int compareTo(Object o){
        if (this == o)
            return 0;
        // assume o is not null
        ProgramConfigurationNewDijkstra other = (ProgramConfigurationNewDijkstra) o;

        // we can define whatever way of comparing two ProgramConfigurationNewDijkstra objects suitable for our purpose
        // for example, we can compare the x values of nodes in lexicological order
        for(int nodeId = 0; nodeId < getNumberOfNodes(); nodeId++){
            int myValue = getNodeValue(nodeId);
            int otherValue = other.getNodeValue(nodeId);
            if(myValue < otherValue)
                return -1;
            if(myValue > otherValue)
                return 1;
        }
        // all nodes have same x values
        return 0;
    }
    
    // getNodeValue and updateStateForANode are supporting functions.
    // getNodeValue returns the nodeValue of a given node
    public int getNodeValue(int nodeId){
        NodeStateNewDijkstra currentNode = (NodeStateNewDijkstra) getNodeStateMap().get(nodeId);
        return currentNode.getNodeValue();
    }

    // change state of a node
    // assuming the value of nodeId inside the second param is equal to the first param
    private void updateStateForANode(int nodeId, NodeStateNewDijkstra nodeState){
        TreeMap<Integer, NodeStateNewDijkstra> currentMap = this.getNodeStateMap();

        // this way is simple but not very efficient as it allocates new memory space
        // currentMap.put(nodeId, nodeState.getDeepCopy());

        // another way is to modify the value inside the state of the interested node
        NodeStateNewDijkstra ns = currentMap.get(nodeId);

        if(ns == null){
            currentMap.put(nodeId, nodeState.getDeepCopy());
        }else{
            ns.setNodeValue(nodeState.getNodeValue());
        }

    }

    private void setStateForAllNodes(NodeStateNewDijkstra nodeState){
        for(int nodeId = 0; nodeId < getNumberOfNodes(); nodeId ++){
            // since updateStateForANode assume the value of nodeId in nodeState is equal to the first param
            // we need to update nodeId in nodeState first
            nodeState.setNodeId(nodeId);
            updateStateForANode(nodeId, nodeState);
        }
    }


```

In the above enumeration scheme, the first program configuration is the one where states of all nodes are 0s, the last program configuration is where states of all nodes are 2s.
Thus we have
```
    @Override
    // first program configuration is the one where every node has the state DIJKSTRA_3_STATES_NODE_STATE_0
    public ProgramConfigurationNewDijkstra moveToFirstProgramConfig(){
        NodeStateNewDijkstra newState = new NodeStateNewDijkstra(0, NodeStateNewDijkstra.NEW_DIJKSTRA_NODE_STATE_0);
        setStateForAllNodes(newState);
        return this;
    }

    @Override
    // last program configuration is the one where every node has the state DIJKSTRA_3_STATES_NODE_STATE_2
    public ProgramConfigurationNewDijkstra moveToLastProgramConfig(){
        NodeStateNewDijkstra newState = new NodeStateNewDijkstra(0, NodeStateNewDijkstra.NEW_DIJKSTRA_NODE_STATE_2);
        setStateForAllNodes(newState);
        return this;
    }

    @Override
    // next program config is the one after the current configure in lexicological order
    // e.g. if the current one is 1-0-2-1
    //      the next one is       1-0-2-2
    //      then                  1-1-0-0
    // after 2-2-2-2 is 0-0-0-0
    public ProgramConfigurationNewDijkstra moveToNextProgramConfig(){
        // going from last node in the ring (with highest nodeId), find the first one whose state is not 2
        for(int nodeId = getNumberOfNodes() - 1; nodeId >= 0; nodeId --){
            NodeStateNewDijkstra currentNode = (NodeStateNewDijkstra) getNodeStateMap().get(nodeId);
            int currentNodeValue = currentNode.getNodeValue();
            if(currentNodeValue == NodeStateNewDijkstra.NEW_DIJKSTRA_NODE_STATE_2){
                // change it to 0
               currentNode.setNodeValue(NodeStateNewDijkstra.NEW_DIJKSTRA_NODE_STATE_0);
               continue;
            }else{
                // increase nodeValue
                currentNode.setNodeValue(currentNodeValue + 1);
                // and stop
                break;
            }
        }
        return this;
    }
```

Implementing function `moveToNthProgramConfig` is not required. 
If we would like to improve the performance, we can override the existing implementation.
	
```
    @Override
    // Move to the nth program configurations.
    // This is similar to convert number N from base 10 to base 3 by keep dividing the
    // dividend by 3 until the dividend is 0.
    // The remainders obtained through the division process will be the base-3 representation in reverse order.
    public ProgramConfigurationNewDijkstra moveToNthProgramConfig(long n) {
        int digitPosition = getNumberOfNodes() - 1;
        while (n > 0) {
            // divide n by 3
            int digitValue = (int) n % 3;
            n = n / 3;
            updateStateForANode(digitPosition, new NodeStateNewDijkstra(digitPosition, digitValue));
            digitPosition--;
        }
        // all other positions will have value 0
        while (digitPosition >= 0) {
            updateStateForANode(digitPosition, new NodeStateNewDijkstra(digitPosition, 0));
            digitPosition--;
        }
        return this;
    }

```

Now we implement other abstract methods of `ProgramConfigurationTemplate`.
	
For `getDeepCopy`, we should replicate the mapping of nodeId to node state so that any change to the copy should not affect the current configuration and vice versa.
In Dijkstra's 3-state program, the size of state space is $3^{N}$ where $N$ is the number of nodes.
By definition in Dijkstra's paper, a configuration is a legitimate (i.e. inside the invariant) when there is exactly one privilege present in the configuration.
	
```
    @Override
    public ProgramConfigurationNewDijkstra getDeepCopy(){
        // generate new node map
        TreeMap<Integer, NodeStateNewDijkstra> currentMap = this.getNodeStateMap();
        TreeMap<Integer, NodeStateNewDijkstra> newTreeMap = new TreeMap<>();
        for(Map.Entry<Integer, NodeStateNewDijkstra> entry : currentMap.entrySet()){
            NodeStateNewDijkstra nodeState = (NodeStateNewDijkstra) entry.getValue().getDeepCopy();
            Integer nodeId = new Integer(entry.getKey());
            newTreeMap.put(nodeId, nodeState);
        }
        return new ProgramConfigurationNewDijkstra(
                getNumberOfNodes(),
                newTreeMap,
                getCvf(),
                getProbeLimit()
        );
    }

    @Override
    public long getSizeOfStateSpace(){
        // there are three possible values (0, 1, 2) for each node.
        // the number of states will be 3^{numberOfNodes}
        return (long) Math.pow(3, getNumberOfNodes());
    }

    @Override
    public boolean isInsideInvariant() {
        return getNumberOfPrivileges() == 1;
    }

```

Next is the central method `evaluateANodeActions` which simulates the situation where a given node is chosen to execute actions.
The chosen node could have multiple actions enabled. The execution of an enabled action will cause the program change to another configuration called a successor. This function will return all possible successors that could be obtained when that node executes its actions.
It also return the number of enabled actions (privileges) at that node in the current configuration. 
This information is useful when we determine whether a configuration is inside the invariant or not.

The implementation of the method `evaluateANodeActions` is based on the program description.
The algorithm of Dijkstra 3-state program is as follows:
Notation:
	
    S: is x value of current node
    L: is x value of left neighbor
    R: is x value of right neighbor:

Algorithm:
	
    if current node is bottom node (nodeId == 0)
        if (S+1) mod 3 == R then
            S = (S-1) mod 3
    if current node is top node (nodeId == n-1)
        if L == R and (L+1) mod 3 != S then
            S = (L+1) mod 3
    other nodes
        if (S+1) mod 3 == L
            S = L
        if (S+1) mod 3 == R
            S = R

Based on the above algorithm, we have the following implementation for `evaluateANodeActions`:
```
    @Override
    /**
     * Count how many privileges (enabled actions) at a node in the current program config, and
     * list of successor configurations if those actions are executed
     * @param nodeId nodeId of the interested node.
     * @return evaluation result of a node, which contains:
     *           number of enabled actions (privileges), and
     *           list of successor states if the actions at specified nodes are executed.
     *              this list could be obtained by simulate execution of each privilege
     *              since two privileges could result in one successor, we should be careful to avoid duplicate successors
     *         If the node is not enabled, return empty list.
     */
    public NodeActionEvaluationResults evaluateANodeActions(int nodeId){
        // The result of this function includes a list of successor
        // To avoid duplicate, we use TreeSet
        TreeSet<SuccessorInfo> successlorList = new TreeSet<>();
        int numberOfPrivileges = 0;

        //first we get the nodeValue at the current node and its left and right neighbors
        int S = getNodeValue(nodeId);
        int newS;
        int rightNbrNodeId = (nodeId + 1) % getNumberOfNodes();
        int R = getNodeValue(rightNbrNodeId);
        int leftNbrNodeId = (nodeId - 1 + getNumberOfNodes()) % getNumberOfNodes();
        int L = getNodeValue(leftNbrNodeId);

        // then evaluate actions based on the described algorithm
        if(nodeId == 0){
            // bottom
            if((S + 1) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES == R){
                // this privilege is present
                numberOfPrivileges ++;
                // we add DIJKSTRA_3_STATES_NUMBER_OF_NODE_STATES to avoid negative value
                newS = (S - 1 + NEW_DIJKSTRA_NUMBER_OF_NODE_STATES) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES;
                // create a new configuration which is the successor of the current configuration
                // i.e. by executing this privilege, the program move from current configuration to the successor configuration
                ProgramConfigurationNewDijkstra successorConfiguration = this.getDeepCopy();
                successorConfiguration.updateStateForANode(nodeId, new NodeStateNewDijkstra(nodeId, newS));
                SuccessorInfo successor = new SuccessorInfo(nodeId, successorConfiguration);
                successlorList.add(successor);
            }

            return new NodeActionEvaluationResults(nodeId, numberOfPrivileges, successlorList);

        }else if (nodeId == getNumberOfNodes() - 1){
            // top
            if (L == R && ((L + 1) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES != S)){
                newS = (L + 1) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES;
                numberOfPrivileges ++;
                ProgramConfigurationNewDijkstra successorConfiguration = this.getDeepCopy();
                successorConfiguration.updateStateForANode(nodeId, new NodeStateNewDijkstra(nodeId, newS));
                SuccessorInfo successor = new SuccessorInfo(nodeId, successorConfiguration);
                successlorList.add(successor);
            }

            return new NodeActionEvaluationResults(nodeId, numberOfPrivileges, successlorList);

        }else{
            // other
            // note that we could have two privileges present in this case
            if( (S+1) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES == L){
                numberOfPrivileges ++;
                newS = L;
                ProgramConfigurationNewDijkstra successorConfiguration = this.getDeepCopy();
                successorConfiguration.updateStateForANode(nodeId, new NodeStateNewDijkstra(nodeId, newS));
                SuccessorInfo successor = new SuccessorInfo(nodeId, successorConfiguration);
                successlorList.add(successor);
            }
            if((S+1) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES == R){
                newS = R;
                numberOfPrivileges ++;
                ProgramConfigurationNewDijkstra successorConfiguration = this.getDeepCopy();
                successorConfiguration.updateStateForANode(nodeId, new NodeStateNewDijkstra(nodeId, newS));
                SuccessorInfo successor = new SuccessorInfo(nodeId, successorConfiguration);
                successlorList.add(successor);
            }

            return new NodeActionEvaluationResults(nodeId, numberOfPrivileges, successlorList);

        }

    }
	
```

Next we implement abstract methods for perturbing state of a node. These methods are used to simulate the effect of `consistency violating faults` (`cvfs`).
+ In the most general case, a `cvf` can be considered as a state perturbation at a given node (see the paper [Benefits of Stabilization versus Rollback in Self-Stabilizing Graph-Based Applications on Eventually Consistent Key-Value Stores](https://ieeexplore.ieee.org/document/9252036) for more detailed explanation). In this case, we use the method `perturbANodeArbitrarily` to simulate `cvfs`.
+ However, a `cvf` is a program transition using stale information. The effect of a `cvf` executed at a node is not necessarily an arbitrary perturbation in the node's state. Looking back at algorithm for Dijkstra's 3-state program transition, we observe that execution of an action (a transition) does not change a state arbitrarily. For example, the value of the bottom node only decreases. So if the current `nodeValue` is 1, the perturbation caused by a `cvf` can only change `nodeValue` to 0 but not 2. Perturbations of a node's state that also takes into account the specifics of program transition rules are simulated by the method `perturbANodeWithConstraint`. The results of such perturbations are closer to the actual effect of `cvfs`.
+ In some programs where the action of a node depends on the topology (e.g., the number of neighbors) like in graph coloring program, the effect of `cvf` could also depend on topology. This situation is simulated by the method `perturbANodeWithConstraintAndTopologyRestriction`. Note that in Dijkstra's token ring program, the topology is implicity (i.e. a ring). Thus, topology does not affect `cvf` in Dijkstra's program and `perturbANodeWithConstraintAndTopologyRestriction` is the same as `perturbANodeWithConstraint`

```
    @Override
    /**
     * Perturb arbitrarily the state of a specified node.
     * @param nodeId node whose state to be perturbed
     * @return list of states obtained by arbitrarily perturbing a node
     *         Current configuration is excluded in the results.
     */
    public NodePerturbationResults perturbANodeArbitrarily(int nodeId){
        TreeSet<SuccessorInfo> successorList = new TreeSet<>();

        for(int newNodeValue = 0; newNodeValue < NEW_DIJKSTRA_NUMBER_OF_NODE_STATES; newNodeValue ++){
            // new only consider new state for the node
            if(newNodeValue != getNodeValue(nodeId)){
                NodeStateNewDijkstra nodeNewState = new NodeStateNewDijkstra(nodeId, newNodeValue);
                ProgramConfigurationNewDijkstra newConfig = (ProgramConfigurationNewDijkstra) this.copyConfigurationAndReplaceState(nodeId, nodeNewState);
                SuccessorInfo successor = new SuccessorInfo(nodeId, newConfig);
                successorList.add(successor);
            }
        }
        return new NodePerturbationResults(successorList);
    }

    @Override
    public NodePerturbationResults perturbANodeWithConstraint(int nodeId){
        TreeSet<SuccessorInfo> successorList = new TreeSet<>();
        /*
        The algorithm of Dijkstra 3-state program is as follows:
            S: is x value of current node
            L: is x value of left neighbor
            R: is x value of right neighbor
            if current node is bottom node (nodeId == 0)
                if (S+1) mod 3 == R then
                    S = (S-1) mod 3
            if current node is top node (nodeId == n-1)
                if L == R and (L+1) mod 3 != S then
                    S = (L+1) mod 3
            other nodes
                if (S+1) mod 3 == L
                    S = L
                if (S+1) mod 3 == R
                    S = R
         */
        if(nodeId == 0){
            // bottom node, x value can only be decreased
            int newNodeValue = (getNodeValue(nodeId) - 1 + NEW_DIJKSTRA_NUMBER_OF_NODE_STATES) % NEW_DIJKSTRA_NUMBER_OF_NODE_STATES;
            NodeStateNewDijkstra nodeNewState = new NodeStateNewDijkstra(nodeId, newNodeValue);
            ProgramConfigurationNewDijkstra newConfig = (ProgramConfigurationNewDijkstra) this.copyConfigurationAndReplaceState(nodeId, nodeNewState);
            SuccessorInfo successor = new SuccessorInfo(nodeId, newConfig);
            successorList.add(successor);
        }else if(nodeId == getNumberOfNodes() - 1){
            // top node, x value can only be left neighbor plus one.
            // since left neighbor can be any value, perturbed x value can be any value except the current value
            for(int newNodeValue = 0; newNodeValue < NEW_DIJKSTRA_NUMBER_OF_NODE_STATES; newNodeValue ++){
                if(newNodeValue != getNodeValue(nodeId)){
                    NodeStateNewDijkstra nodeNewState = new NodeStateNewDijkstra(nodeId, newNodeValue);
                    ProgramConfigurationNewDijkstra newConfig = (ProgramConfigurationNewDijkstra) this.copyConfigurationAndReplaceState(nodeId, nodeNewState);
                    SuccessorInfo successor = new SuccessorInfo(nodeId, newConfig);
                    successorList.add(successor);
                }
            }
        }else{
            // other node: value can only increase
            int newNodeValue = (getNodeValue(nodeId) + 1)% NEW_DIJKSTRA_NUMBER_OF_NODE_STATES;
            NodeStateNewDijkstra nodeNewState = new NodeStateNewDijkstra(nodeId, newNodeValue);
            ProgramConfigurationNewDijkstra newConfig = (ProgramConfigurationNewDijkstra) this.copyConfigurationAndReplaceState(nodeId, nodeNewState);
            SuccessorInfo successor = new SuccessorInfo(nodeId, newConfig);
            successorList.add(successor);
        }

        return new NodePerturbationResults(successorList);
    }

    public NodePerturbationResults perturbANodeWithConstraintAndTopologyRestriction(int nodeId){
        // in Dijkstra' program, there is no difference between this function and perturbANodeWithConstraint
        return perturbANodeWithConstraint(nodeId);
    }
	
```


VM options:
-Xms1G -Xmx1G

Program arguments:
--analysis-mode=config-base --config-base-analysis-task=full --program-name=coloring --number-of-nodes=5 --output-filename-prefix=.\results\abc --debug-filename-prefix=.\results\debug\abc --cvf=constrained-perturb --sample-size=0 --probe-limit=0 --random-trans-nbr-prob=1 --random-cvf-nbr-prob=1 --graph-topology-filename=.\graphs\graph_random_regular_graph_n6_d3.txt --run-id=0
		    




