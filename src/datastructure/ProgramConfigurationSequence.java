package datastructure;

import template.ProgramConfigurationTemplate;

import java.util.Vector;

/**
 * A sequence of program configuration without cycle such that
 * the next program configuration can be obtained from the current one by execution
 * of some enabled action of the current one.
 */

public class ProgramConfigurationSequence {
    Vector<ProgramConfigurationTemplate> configSequence;

    public ProgramConfigurationSequence(Vector<ProgramConfigurationTemplate> configSequence){
        this.configSequence = configSequence;
    }

    public Vector<ProgramConfigurationTemplate> getConfigSequence() {
        return configSequence;
    }

    public void setConfigSequence(Vector<ProgramConfigurationTemplate> configSequence) {
        this.configSequence = configSequence;
    }

    public int getPathLength(){
        return configSequence.size() - 1;
    }

    public ProgramConfigurationSequence getDeepCopy(){
        Vector<ProgramConfigurationTemplate> configSequenceCopy = new Vector<>();
        for(ProgramConfigurationTemplate pct : configSequence){
            configSequenceCopy.addElement(pct.getDeepCopy());
        }

        return new ProgramConfigurationSequence(configSequenceCopy);
    }

    public ProgramConfigurationTemplate getFirstConfig(){
        return configSequence.firstElement();
    }

    public ProgramConfigurationTemplate getLastConfig(){
        return configSequence.lastElement();
    }

    public void appendConfig(ProgramConfigurationTemplate pc){
        configSequence.addElement(pc);
    }
}
