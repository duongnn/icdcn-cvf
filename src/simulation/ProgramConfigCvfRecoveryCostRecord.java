package simulation;

import java.util.Vector;

/**
 */

public class ProgramConfigCvfRecoveryCostRecord {
    // each element is the number of program transitions it takes
    // to converge from the given program config, without cvf during execution
    Vector<Integer> programConvergence;
    double avgProgramConvergence; // average of the above vector
    // each element is the number of program transitions it takes
    // to converge from the given program config, with cvf occurrences
    Vector<Integer> cvfConvergence;
    double avgCvfConvergence; // average of the above vector

    double cvfRecoverCost; // the ratio of avgCvfConvergence/avgProgramConvergence

    ProgramConfigCvfRecoveryCostRecord(){
        programConvergence = new Vector<>();
        cvfConvergence = new Vector<>();
        avgProgramConvergence = 0;
        avgCvfConvergence = 0;
        cvfRecoverCost = Double.NaN; // negative values means invalid
    }

    public Vector<Integer> getProgramConvergence() {
        return programConvergence;
    }

    public double getAvgProgramConvergence() {
        return avgProgramConvergence;
    }

    public Vector<Integer> getCvfConvergence() {
        return cvfConvergence;
    }

    public double getAvgCvfConvergence() {
        return avgCvfConvergence;
    }

    public double getCvfRecoverCost() {
        return cvfRecoverCost;
    }
// disable the code adding program convergence or cvf convergence independently
    // to avoid the possibility of dividing by zero
//    protected void addProgConvergence(int progConv){
//        programConvergence.addElement(progConv);
//        avgProgramConvergence = programConvergence.stream()
//                .mapToInt(Integer::valueOf)
//                .average().orElse(Double.NaN);
//
//        cvfRecoverCost = avgCvfConvergence/avgProgramConvergence;
//    }
//    protected void addCvfConvergence(int cvfConv){
//        cvfConvergence.addElement(cvfConv);
//        avgCvfConvergence = cvfConvergence.stream()
//                .mapToInt(Integer::valueOf)
//                .average().orElse(Double.NaN);
//
//        cvfRecoverCost = avgCvfConvergence/avgProgramConvergence;
//    }

    protected void addData(int progConv, int cvfConv){
        programConvergence.addElement(progConv);
        avgProgramConvergence = programConvergence.stream()
                .mapToInt(Integer::valueOf)
                .average().orElse(Double.NaN);

        cvfConvergence.addElement(cvfConv);
        avgCvfConvergence = cvfConvergence.stream()
                .mapToInt(Integer::valueOf)
                .average().orElse(Double.NaN);

        cvfRecoverCost = avgCvfConvergence/avgProgramConvergence;

    }
}
