package datastructure;

import template.ProgramConfigurationTemplate;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

/**
 * This class encapsulates information about all the paths
 * from specific configuration to the destination (usually the destination is the invariant)
 */
public class ProgramConfigurationPathInformation {
    ProgramConfigurationTemplate source;

    // if destination is the invariant, this variable is null
    ProgramConfigurationTemplate destination;

    // list of paths
    // each entry maps a successor and the distance of all the paths
    // from that successor to the destination.
    // in case sourceNodeId == destinationNodeId, this path is empty (but not null)
    TreeMap<ProgramConfigurationTemplate, Vector<Integer>> pathList;

    public boolean isSourceTheDestination(){
        if(destination == null){
            // check if source is inside invariant
            return source.isInsideInvariant();
        }else{
            return source.equals(destination);
        }
    }

    public Vector<Integer> getPathDistanceList(){
        Vector<Integer> pdl = new Vector<>();

        if(isSourceTheDestination()){
            pdl.addElement(0);
        }else{
            for(Map.Entry<ProgramConfigurationTemplate, Vector<Integer>> entry : pathList.entrySet()){
                // prefer not to use addAll since we may get a shallow copy of data
                // pdl.addAll(entry.getValue());

                // using int will force autoboxing
                for(int dist : entry.getValue()){
                    pdl.addElement(dist);
                }
            }
        }

        return pdl;
    }
}
