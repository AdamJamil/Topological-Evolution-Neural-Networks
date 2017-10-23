package TENN;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.MersenneTwister;

class EdgeGene
{
    Edge edge;
    short incomingNode;
    short outgoingNode;

    private static final double mutationMultiplier = 1.5;
    private static final double changeWeight = 0.4 * mutationMultiplier;
    private static final GaussianRandomGenerator random = new GaussianRandomGenerator(new MersenneTwister());

    private static int edgeGenes = 0;

    private static XoRoShiRo128PlusRandom uniformRandom = new XoRoShiRo128PlusRandom();

    private EdgeGene(short incomingNode, short outgoingNode)
    {
        edge = new Edge();

        this.incomingNode = incomingNode;
        this.outgoingNode = outgoingNode;
    }

    EdgeGene mutate()
    {
        EdgeGene tempEdgeGene = actualCloneBecauseJavaIsWrittenByPajeets();

        if (uniformRandom.nextDoubleFast() < changeWeight)
        {
            double mutatedWeight = (3.33 * random.nextNormalizedDouble()) + tempEdgeGene.edge.weight;
            if (mutatedWeight > 10)
                mutatedWeight = 10;
            else if (mutatedWeight < -10)
                mutatedWeight = -10;
            tempEdgeGene.edge.weight = mutatedWeight;
        }

        return tempEdgeGene;
    }

    EdgeGene actualCloneBecauseJavaIsWrittenByPajeets()
    {
        EdgeGene clonedEdgeGene = new EdgeGene(this.incomingNode, this.outgoingNode);
        clonedEdgeGene.edge.weight = edge.weight;

        return clonedEdgeGene;
    }

    static EdgeGene randomEdgeGene(int size)
    {
        EdgeGene edgeGene = new EdgeGene((short) (uniformRandom.nextDoubleFast() * size), (short) (uniformRandom.nextDoubleFast() * size));
        edgeGene.edge.weight = -10 + (20 * uniformRandom.nextDoubleFast());
        edgeGene.edge.name = edgeGenes;
        edgeGenes++;

        return edgeGene;
    }
}