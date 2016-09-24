#!/bin/sh

#
# Version: 1.0
# Author: Keith Henderson
# Contact: keith@llnl.gov
#

JAVA=${HOME}/bin/java
MATLAB=${HOME}/bin/matlab

#jvm memory allocation
MEM_IN_MEGS=500


# ReFeX parameters
INFILE=sample-data/netsci-undirected.csv # src,dst,wgt edges
CORR_THRESH=0 #usually 0 -- this is the initial lattice error threshold
BIN_SIZE=0.5 #usually 0.5 -- this is the fraction in each bin

# Feature files are prefixed with this
FEATFILE=out

# run ReFeX
echo ${JAVA} -Xmx${MEM_IN_MEGS}M GenerateFeatures ${INFILE} ${CORR_THRESH} ${BIN_SIZE} ${FEATFILE}
${JAVA} -Xmx${MEM_IN_MEGS}M GenerateFeatures ${INFILE} ${CORR_THRESH} ${BIN_SIZE} ${FEATFILE}

# RolX output files
NODEFILE=out-nodeRoles.txt #which node belongs to which role
ROLEFILE=out-roleFeatures.txt #which features influenced each role
IDFILE=out-ids.txt 

# run RolX
echo ${MATLAB} -nodisplay -r "javaaddpath('.'); W=load('${FEATFILE}-featureValues.csv'); IDs=W(:,1); save('${IDFILE}', 'IDs', '-ASCII'); [n,m] = size(W); V=W(1:n,2:m); [F,G,dlen]=NMF_MDL_Quantized(V); save('${NODEFILE}', 'G', '-ASCII'); save('${ROLEFILE}', 'F', '-ASCII'); quit;"
${MATLAB} -nodisplay -r "javaaddpath('.'); W=load('${FEATFILE}-featureValues.csv'); IDs=W(:,1); save('${IDFILE}', 'IDs', '-ASCII'); [n,m] = size(W); V=W(1:n,2:m); [F,G,dlen]=NMF_MDL_Quantized(V); save('${NODEFILE}', 'G', '-ASCII'); save('${ROLEFILE}', 'F', '-ASCII'); quit;"



