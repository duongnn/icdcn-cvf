package datastructure;

import dijkstra3states.NodeStateDijkstra3States;
import dijkstra3states.ProgramConfigurationDijkstra3States;
import template.ProgramConfigurationTemplate;

import java.util.Objects;
import java.util.TreeMap;
import java.util.Vector;

/**
 * This class wraps a pair of configuration
 * A pair of configuration can be used to represent a program transition or a cvf
 *
 */

public class ConfigurationPair<PCT extends ProgramConfigurationTemplate> implements Comparable{
    PCT currentConfig;
    PCT nextConfig;

    public ConfigurationPair(PCT current, PCT next){
        currentConfig = current;
        nextConfig = next;
    }

    public PCT getCurrentConfig() {
        return currentConfig;
    }

    public void setCurrentConfig(PCT currentConfig) {
        this.currentConfig = currentConfig;
    }

    public PCT getNextConfig() {
        return nextConfig;
    }

    public void setNextConfig(PCT nextConfig) {
        this.nextConfig = nextConfig;
    }

    /**
     * Overriding equals method of Object class
     * So that we can accurately check if an object is already in the map
     *
     * @param anotherConfigPair
     * @return true if the two configs in this pair are equal to those of anotherConfigPair
     */
    @Override
    public boolean equals(Object anotherConfigPair){
        if(anotherConfigPair == null)
            return false;
        if(anotherConfigPair == this)
            return true;

        if(!(anotherConfigPair instanceof ConfigurationPair))
            return false;

        ConfigurationPair<PCT> cp = (ConfigurationPair<PCT>) anotherConfigPair;

        if(!this.currentConfig.equals(cp.getCurrentConfig())){
            return false;
        }

        if(!this.nextConfig.equals(cp.getNextConfig())){
            return false;
        }

        return true;
    }

    /**
     * When overriding equals(), we override hashCode().
     * @return the hash of program state
     */
    @Override
    public int hashCode(){
        return Objects.hash(currentConfig, nextConfig);
    }


    @Override
    public int compareTo(Object o){
        ConfigurationPair<PCT> other = (ConfigurationPair<PCT>) o;

        int firstCompare =this.currentConfig.compareTo(other.getCurrentConfig());
        int secondComapre = this.nextConfig.compareTo(other.getNextConfig());

        if(firstCompare == 0){
            if(secondComapre == 0){
                return 0;
            }else{
                return secondComapre;
            }
        }else{
            return firstCompare;
        }
    }

    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append(this.getCurrentConfig().toString())
                .append(" : ")
                .append(this.getNextConfig().toString());
        return str.toString();
    }

}
