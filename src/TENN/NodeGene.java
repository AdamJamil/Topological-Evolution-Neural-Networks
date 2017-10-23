package TENN;

import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.MersenneTwister;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;


class NodeGene
{
    Node node;
    short position; //position in boxes

    private static final double mutationMultiplier = 1.5;
    private static final double changeActivationType = 0.1 * mutationMultiplier;
    private static final double changeAC1 = 0.4 * mutationMultiplier;
    private static final double changeAC2 = 0.4 * mutationMultiplier;
    private static final GaussianRandomGenerator random = new GaussianRandomGenerator(new MersenneTwister());
    private static final XoRoShiRo128PlusRandom uniformRandom = new XoRoShiRo128PlusRandom();

    private static int nodeGenes = 0;

    private NodeGene()
    {
        node = new Node();
    }

    NodeGene mutate()
    {
        NodeGene tempNodeGene = actualCloneBecauseJavaIsWrittenByPajeets();

        if (uniformRandom.nextDoubleFast() < changeActivationType)
            tempNodeGene.node.activationType = (short) (uniformRandom.nextDoubleFast() * 8);

        if (uniformRandom.nextDoubleFast() < changeAC1)
        {
            double mutatedAC1 = (2.5 * random.nextNormalizedDouble()) + tempNodeGene.node.AC1;
            if (mutatedAC1 > 10)
                mutatedAC1 = 10;
            if (mutatedAC1 < -10)
                mutatedAC1 = -10;
            tempNodeGene.node.AC1 = mutatedAC1;
        }

        if (uniformRandom.nextDoubleFast() < changeAC2)
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


    static NodeGene randomNodeGene(int inputs, int outputs, int size)
    {
        NodeGene nodeGene = new NodeGene();

        nodeGene.node.activationType = (short) (uniformRandom.nextDoubleFast() * 8);
        nodeGene.node.AC1 = uniformRandom.nextDoubleFast() * 20 - 10;
        nodeGene.node.AC2 = uniformRandom.nextDoubleFast() * 4 - 2;
        nodeGene.node.name = nodeGenes;
        nodeGene.position = (short) (inputs + (uniformRandom.nextDoubleFast() * (size - inputs - outputs)));

        nodeGenes++;

        return nodeGene;
    }
}