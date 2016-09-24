JAVAC=/home/hendersk/bin/javac
.SUFFIXES: .java .class
.java.class:
	$(JAVAC) $*.java
SRCS = \
	AttributedGraph.java \
	EgonetGenerator.java \
	RankedRoleFinder.java \
	AttributedLink.java \
	Egonet.java \
	RightEgonet.java \
	AttributedNode.java \
	GenerateFeatures.java \
	TimeUtils.java \
	CalculateFeatures.java \
	HuffmanComparator.java \
	Counter.java \
	LeftEgonet.java

default: classes

classes: $(SRCS:.java=.class)

clean:
	$(RM) *.class

