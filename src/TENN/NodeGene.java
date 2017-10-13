package TENN;

import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.FastMath;

class NodeGene
{
    Node node;
    short position; //position in boxes

    static final double mutationMultiplier = 1.5;
    static final double changeActivationType = 0.1 * mutationMultiplier;
    static final double changeAC1 = 0.4 * mutationMultiplier;
    static final double changeAC2 = 0.4 * mutationMultiplier;
    static final GaussianRandomGenerator random = new GaussianRandomGenerator(new MersenneTwister());

    static int nodeGenes = 0;

    NodeGene()
    {
        node = new Node();
    }

    NodeGene mutate()
    {
        NodeGene tempNodeGene = actualCloneBecauseJavaIsWrittenByPajeets();

        if (FastMath.random() < changeActivationType)
            tempNodeGene.node.activationType = (short) (FastMath.random() * 8);

        if (FastMath.random() < changeAC1)
        {
            double mutatedAC1 = (2.5 * random.nextNormalizedDouble()) + tempNodeGene.node.AC1;
            if (mutatedAC1 > 10)
                mutatedAC1 = 10;
            if (mutatedAC1 < -10)
                mutatedAC1 = -10;
            tempNodeGene.node.AC1 = mutatedAC1;
        }

        if (FastMath.random() < changeAC2)
        {
            double mutatedAC2 = (0.5 * random.nextNormalizedDouble()) + tempNodeGene.node.AC2;
            if (mutatedAC2 > 2)
                mutatedAC2 = 2;
            if (mutatedAC2 < -2)
                mutatedAC2 = -2;
            tempNodeGene.node.AC2 = mutatedAC2;
        }

        return tempNodeGene;
    }

    NodeGene actualCloneBecauseJavaIsWrittenByPajeets()
    {
        Node clonedNode = new Node();
        clonedNode.name = node.name;
        clonedNode.AC1 = node.AC1;
        clonedNode.AC2 = node.AC2;
        clonedNode.activationType = node.activationType;
        clonedNode.output = node.output;
        clonedNode.input = node.input;

        NodeGene clonedNodeGene = new NodeGene();
        clonedNodeGene.node = clonedNode;
        clonedNodeGene.position = position;

        return clonedNodeGene;
    }


    static NodeGene randomNodeGene(int number, int inputs, int outputs, int size)
    {
        NodeGene nodeGene = new NodeGene();

        nodeGene.node.activationType = (short) (FastMath.random() * 8);
        nodeGene.node.AC1 = FastMath.random() * 20 - 10;
        nodeGene.node.AC2 = FastMath.random() * 4 - 2;
        nodeGene.node.name = "node " + nodeGenes;
        nodeGene.position = (short) (inputs + (FastMath.random() * (size - inputs - outputs)));

        nodeGenes++;

        return nodeGene;
    }
}