# icdcn-cvf
This repo contains the source code and experiment data for ICDCN'23 paper (submission 685).

We have developed this program as a generic toolset so that by only coding the specific algorithm, one can obtain the effect of cvf on that program. 
Specifically, our analysis of `mrank` and `arank` uses program transitions as a parameter. Thus, by simply specifying the new program transitions, one can analyze the effect of cvfs for the given program.

Please see [the tutorial](tutorial.md) on how to add an algorithm/program to the framework.
