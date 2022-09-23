#!/bin/bash

classpath=../lib/commons-codec-1.6.jar:../lib/guava-14.0.1.jar:../lib/jopt-simple-4.6.jar:../lib/commons-collections4-4.1.jar:.

outputdir=../classes

#safecheck=-Xlint
safecheck=

echo "Compiling ..."
mkdir -p ./classes
cd src

javac $safecheck -d $outputdir ./template/*.java -classpath $classpath && \
javac $safecheck -d $outputdir ./dijkstra3states/*.java -classpath $classpath && \
javac $safecheck -d $outputdir ./main/*.java -classpath $classpath 
javac $safecheck -d $outputdir ./simulation/*.java -classpath $classpath 
#javac $safecheck -d $outputdir ./*.java -classpath $classpath

if [ $? -ne 0 ]; then
  echo
  echo "Error with compling the code. Exit now"
  exit 1
fi


mkdir -p ../results
mkdir -p ../results/debug

cd ../classes

hostname=$(hostname)


JAVA_OPT="-Xms1G -Xmx1G"
if [[ $hostname == *"compute"* ]]; then
  # EGR compute server with more memory
  # Note that usually the heap size is limited by 1/4 of physical memory
  # Furthermore, there are overhead for the heap, e.g. JVM allocate bitmaps of 1/32 of requested heap size
  # So with 755G of memory, you cannot request roughly 755/4 = 188G
  # By trials, the max value for Xmx on EGR compute machine is 173G
  JAVA_OPT="-Xms170G -Xmx170G"  
fi


echo
echo
echo "---------------------------------------------------"
echo "Running Simulation for analyzing cvfs recovery cost"
echo


logfile="../results/screenLog.txt"
echo "" > $logfile


####### START OF CHANGE REGION - CHANGE THESE PARAMETERS IF NEEDED ########################

## name of distributed program to be analyzed
##   dijkstra-3-states-program: dijkstra ring program, the first self-stabilizing program in literature
##   max-matching: silently self-stabilizing maximal matching program
##   coloring: silently self-stabilizing coloring program
##   to-be-added: population protocol
program_name_list=("coloring") #("coloring" "max-matching" "dijkstra-3-states-program")


## input file specifying graph topology
##   empty file name "" means no graph topology
graph_topology_filename_list=(
  ###
  ### implict topology are the topology implicitly used in some problems
  ### for example, in Dijkstra, the topology is always a ring,
  ### and with specific assignment of which are the bottom and top nodes.
  # implicit_topology.txt
  ###
  ### rings
  graph_random_regular_graph_n3_d2.txt
  graph_random_regular_graph_n4_d2.txt
  graph_random_regular_graph_n5_d2.txt
  graph_random_regular_graph_n6_d2.txt
  graph_random_regular_graph_n7_d2.txt
  graph_random_regular_graph_n8_d2.txt
  graph_random_regular_graph_n9_d2.txt
  graph_random_regular_graph_n10_d2.txt
  graph_random_regular_graph_n11_d2.txt
  graph_random_regular_graph_n12_d2.txt
  graph_random_regular_graph_n13_d2.txt
  graph_random_regular_graph_n14_d2.txt
  graph_random_regular_graph_n15_d2.txt
  graph_random_regular_graph_n16_d2.txt
  graph_random_regular_graph_n17_d2.txt
  graph_random_regular_graph_n18_d2.txt
  graph_random_regular_graph_n19_d2.txt
  graph_random_regular_graph_n20_d2.txt
  graph_random_regular_graph_n21_d2.txt
  graph_random_regular_graph_n22_d2.txt
  graph_random_regular_graph_n23_d2.txt
  graph_random_regular_graph_n24_d2.txt
  graph_random_regular_graph_n25_d2.txt
  # graph_random_regular_graph_n26_d2.txt
  # graph_random_regular_graph_n27_d2.txt
  # graph_random_regular_graph_n28_d2.txt
  # graph_random_regular_graph_n29_d2.txt
  # graph_random_regular_graph_n30_d2.txt
  ###
  ### regular graphs but not ring
  # graph_random_regular_graph_n4_d3.txt
  # graph_random_regular_graph_n5_d4.txt
  # graph_random_regular_graph_n6_d3.txt
  # graph_random_regular_graph_n7_d4.txt
  # graph_random_regular_graph_n8_d4.txt
  # graph_random_regular_graph_n9_d4.txt
  # graph_random_regular_graph_n10_d4.txt
  # graph_random_regular_graph_n11_d4.txt
  # graph_random_regular_graph_n12_d4.txt
  # graph_random_regular_graph_n13_d4.txt
  # graph_random_regular_graph_n14_d4.txt
  # graph_random_regular_graph_n15_d4.txt
  ###
  ### power law graphs
  # graph_powerlaw_cluster_graph_n5.txt
  # graph_powerlaw_cluster_graph_n6.txt
  # graph_powerlaw_cluster_graph_n7.txt
  # graph_powerlaw_cluster_graph_n8.txt
  # graph_powerlaw_cluster_graph_n9.txt
  # graph_powerlaw_cluster_graph_n10.txt
  # graph_powerlaw_cluster_graph_n11.txt
  # graph_powerlaw_cluster_graph_n12.txt
  # graph_powerlaw_cluster_graph_n13.txt
  # graph_powerlaw_cluster_graph_n14.txt
  # graph_powerlaw_cluster_graph_n15.txt
  # graph_powerlaw_cluster_graph_n16.txt
  # graph_powerlaw_cluster_graph_n17.txt
  # graph_powerlaw_cluster_graph_n18.txt
  # graph_powerlaw_cluster_graph_n19.txt
  # graph_powerlaw_cluster_graph_n20.txt
)

## range of number of nodes in the graph
## This should be ignored if actual graph topology is provided
node_num_start=0
node_num_end=0
node_num_step=1

## number of simulation steps
simulation_limit_list=(10000)  # with 10K steps, node_num_end for Dijkstra should not be more than 22

# number of samples
sample_size_list=(100)

## number of trials, i.e. for a given program configuration, how many
## times we evaluate the convergence of that program (with and without cvf)
number_of_trials_list=(5)

## cvf interval, i.e. how many program transitions are in between cvf occurrences
cvf_interval_list=(1 2 4 8)


# # how cvf is defined
# #   arbitrary-perturb: arbitrary perturbation of a node state
# #   constrained-perturb: perturbation of a node state constrained by program transition
# #   constrained-perturb-and-topology-restriction: perturbation of a node state constrained by program transition rule 
# #                                                as well as graph topology (i.e. who are the neighbors of a node).
# #                                                For example, in max-matching, p-value can only be either null-value
# #                                                or one of the neighbors, not an arbitrary node.
# cvf="arbitrary-perturb"
cvf="constrained-perturb-and-topology-restriction"


num_of_runs=1



####### END OF CHANGE REGION ########################

for simulation_limit in "${simulation_limit_list[@]}"
do
  for program_name in "${program_name_list[@]}"
  do
    for graph_topology_filename in "${graph_topology_filename_list[@]}"
    do
      for (( node_num =$node_num_start; node_num<=$node_num_end; node_num=$node_num+$node_num_step ))
      do
        for sample_size in "${sample_size_list[@]}"
        do
          for number_of_trials in "${number_of_trials_list[@]}"
          do          
            for cvf_interval in  "${cvf_interval_list[@]}"
            do
              for ((run_id=0; run_id<$num_of_runs; run_id++ ))
              do

                    # output_filename_prefix=stdout
                    filename_prefix="$(printf '%s'  \
                                       ${program_name}-${node_num}-nodes\
                                       -${simulation_limit}-steps\
                                       -samples-${sample_size}\
                                       -${number_of_trials}-trials\
                                       -${cvf_interval}-cvfintv\
                                       -${cvf}-run-${run_id}\
                                       -${graph_topology_filename})"

                    output_filename_prefix="../results/${filename_prefix}"
                    debug_filename_prefix="../results/debug/${filename_prefix}"

                    java    $JAVA_OPT \
                            -classpath ../lib/jopt-simple-4.6.jar:. \
                            simulation.Simulation \
                            --program-name=$program_name \
                            --number-of-nodes=$node_num \
                            --simulation-limit=$simulation_limit \
                            --output-filename-prefix=$output_filename_prefix \
                            --debug-filename-prefix=$debug_filename_prefix \
                            --cvf=$cvf \
                            --sample-size=$sample_size \
                            --number-of-trials=$number_of_trials \
                            --cvf-interval=$cvf_interval \
                            --graph-topology-filename=../graphs/${graph_topology_filename} \
                            --run-id=$run_id | tee -a $logfile

                    echo; echo; echo  

              done # run_id
            done # cvf_interval
          done # number_of_trials
        done # sample_size
      done # node_num
    done # graph_topology_filename
  done # program_name
done # simulation_limit