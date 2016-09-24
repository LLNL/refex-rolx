# ReFeX / RolX

ReFeX Source Files:
AttributedGraph.java
AttributedLink.java
AttributedNode.java
CalculateFeatures.java
Counter.java
EgonetGenerator.java
Egonet.java
GenerateFeatures.java
LeftEgonet.java
RankedRoleFinder.java
RightEgonet.java
TimeUtils.java

RolX Source Files:
HuffmanComparator.java
HuffmanCost.m
MaxLloyd.m
NMF_LS_FixedF.m
NMF_LS_new.m
NMF_MDL_Quantized.m

Shared/Utility Files:
Makefile
run_fixed.sh
run_refex_rolx.sh
sample-data
README.txt

(1) run_refex_rolx.sh calls the GenerateFeatures class to generate
features when the set of features is unknown.  If the set of features
is known, then you need to use CalculateFeatures class (run_fixed.sh
demonstrates this).

(2) There are sample input data in sample-data/ directory

(3) The output are:

out-featureNames.csv: feature names extracted by ReFex
out-featureValues.csv: feature values extracted by ReFex
out-nodeRoles.txt: node-by-role matrix
out-roleFeatures.txt: role-by-feature matrix
out-ids.txt: each line is the nodeID for the corresponding row in out-nodeRoles


## Notes

For descriptions of the feature naming convention, see Egonet.java.

## Release

ReFeX / RolX is released under an LGPL license.  For more details see the
LICENSE file.

ReFeX: ``LLNL-CODE-665875``
RolX: ``LLNL-CODE-665876``
