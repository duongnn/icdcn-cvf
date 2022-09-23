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
echo "-----------------------------"
echo "Running AnalyzeCvfs"
echo


logfile="../results/screenLog.txt"
echo "" > $logfile


####### START OF CHANGE REGION - CHANGE THESE PARAMETERS IF NEEDED ########################

## analysis mode
##   we could analyze rank effect based on configurations or based on transitions
##   config-base: analyze rank effect based on configurations
##   trans-base: analyze rank effect based on transitions
analysis_mode_list=("config-base") #("trans-base")

## for config-base analysis mode, specify whether full analysis or statistical analysis
##    full: full analysis
#     statistical: statistical analysis
#     full_statistical: both full and statistical analysis
config_base_analysis_task="full"


## name of distributed program to be analyzed
##   dijkstra-3-states-program: dijkstra ring program, the first self-stabilizing program in literature
##   max-matching: silently self-stabilizing maximal matching program
##   coloring: silently self-stabilizing coloring program
##   to-be-added: population protocol
program_name_list=("dijkstra-3-states-program") #("coloring" "max-matching" "dijkstra-3-states-program")


## input file specifying graph topology
##   empty file name "" means no graph topology
graph_topology_filename_list=(
  ###
  ### implict topology are the topology implicitly used in some problems
  ### for example, in Dijkstra, the topology is always a ring,
  ### and with specific assignment of which are the bottom and top nodes.
  implicit_topology.txt
  ###
  ### rings
  # graph_random_regular_graph_n3_d2.txt
  # graph_random_regular_graph_n4_d2.txt
  # graph_random_regular_graph_n5_d2.txt
  # graph_random_regular_graph_n6_d2.txt
  # graph_random_regular_graph_n7_d2.txt
  # graph_random_regular_graph_n8_d2.txt
  # graph_random_regular_graph_n9_d2.txt
  # graph_random_regular_graph_n10_d2.txt
  # graph_random_regular_graph_n11_d2.txt
  # graph_random_regular_graph_n12_d2.txt
  # graph_random_regular_graph_n13_d2.txt
  # graph_random_regular_graph_n14_d2.txt
  # graph_random_regular_graph_n15_d2.txt
  # graph_random_regular_graph_n16_d2.txt
  # graph_random_regular_graph_n17_d2.txt
  # graph_random_regular_graph_n18_d2.txt
  # graph_random_regular_graph_n19_d2.txt
  # graph_random_regular_graph_n20_d2.txt
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
node_num_start=3
node_num_end=14
node_num_step=1


# number of samples
#   value 0 means the whole state space
sample_size_list=(0)


# To evaluate the rank of a configuration, we consider all the paths from that configuration
# to the invariant, and caculate the lengths of those paths. However, the number of paths 
# could be exponentially large.
# To reduce the time and memory, in statistical analysis, we limit the number of paths to be
# explored to a limit.
# professor Kulkarni suggests probe limit 10ish or 100ish only
# probe_limit > 0 means explore at most probe_limit paths
#             = 0 means explore all paths
#             = -1 also all paths
#             = -2 means adaptive -- not yet implemented
probe_limit_list=(0)


# In statistical analysis, to evaluate the effect of a program transition or 
# a cvf on a given configuration, we have to evaluate the ranks of the configurations
# and its transition neighbors, and then compute rank difference between the configuration 
# and its neighbors to determine the effect of program transitions.
# If we consider all the neighbors, the time could be long since the number of neighbors
# can be high. To speedup, we may consider a sample set of neighbors instead of all neighbors.

# probability to select a random transition neighbor to evaluate effect of program transitions
# transition neighbor is a configuration reachable from current config by a program transition.
# For example, if this probability is 0.2 then about 20% of the transition neighbors will be explored/selected
# to evaluate the effect of program transitions 
# current just use 1
random_trans_nbr_probability_list=(1)

# probability to select a random cvf neighbor to evaluate effect of cvfs
# cvf neighbor is a configuration reachable from current config by a cvf
# current just use 1
random_cvf_nbr_probability_list=(1)


# # how cvf is defined
# #   arbitrary-perturb: arbitrary perturbation of a node state
# #   constrained-perturb: perturbation of a node state constrained by program transition
# #   constrained-perturb-and-topology-restriction: perturbation of a node state constrained by program transition rule 
# #                                                as well as graph topology (i.e. who are the neighbors of a node).
# #                                                For example, in max-matching, p-value can only be either null-value
# #                                                or one of the neighbors, not an arbitrary node.
#cvf="arbitrary-perturb"
cvf="constrained-perturb"
#cvf="constrained-perturb-and-topology-restriction"


num_of_runs=1



####### END OF CHANGE REGION ########################


for analysis_mode in "${analysis_mode_list[@]}"
do
  for program_name in "${program_name_list[@]}"
  do
    for graph_topology_filename in "${graph_topology_filename_list[@]}"
    do
      for (( node_num =$node_num_start; node_num<=$node_num_end; node_num=$node_num+$node_num_step ))
      do
        for sample_size in "${sample_size_list[@]}"
        do
          for probe_limit in "${probe_limit_list[@]}"
          do
            for ((run_id=0; run_id<$num_of_runs; run_id++ ))
            do
              for random_trans_nbr_probability in "${random_trans_nbr_probability_list[@]}"
              do
                for random_cvf_nbr_probability in "${random_cvf_nbr_probability_list[@]}"
                do

                    # output_filename_prefix=stdout
                    filename_prefix="$(printf '%s'  \
                                       ${analysis_mode}-${program_name}-${node_num}-nodes\
                                       -samples-${sample_size}\
                                       -probes-${probe_limit}\
                                       -tnbr-${random_trans_nbr_probability}\
                                       -cnbr-${random_cvf_nbr_probability}\
                                       -${cvf}-run-${run_id}\
                                       -${graph_topology_filename})"

                    output_filename_prefix="../results/${filename_prefix}"
                    debug_filename_prefix="../results/debug/${filename_prefix}"

                    java    $JAVA_OPT \
                            -classpath ../lib/jopt-simple-4.6.jar:. \
                            main.AnalyzeCvfs \
                            --analysis-mode=$analysis_mode \
                            --config-base-analysis-task=$config_base_analysis_task \
                            --program-name=$program_name \
                            --number-of-nodes=$node_num \
                            --output-filename-prefix=$output_filename_prefix \
                            --debug-filename-prefix=$debug_filename_prefix \
                            --cvf=$cvf \
                            --sample-size=$sample_size \
                            --probe-limit=$probe_limit \
                            --random-trans-nbr-prob=$random_trans_nbr_probability \
                            --random-cvf-nbr-prob=$random_cvf_nbr_probability \
                            --graph-topology-filename=../graphs/${graph_topology_filename} \
                            --run-id=$run_id | tee -a $logfile

                    echo; echo; echo  

                done # random_trans_nbr_probability
              done # random_cvf_nbr_probability
            done # run_id
          done # probe_limit
        done # sample_size
      done # node_num
    done # graph_topology_filename
  done # program_name
done # analysis_mode
