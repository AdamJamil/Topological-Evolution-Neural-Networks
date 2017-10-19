package TENN;

import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.FastMath;

class EdgeGene
{
    Edge edge;
    short incomingNode;
    short outgoingNode;

    private static final double mutationMultiplier = 1.5;
    private static final double changeWeight = 0.4 * mutationMultiplier;
    private static final GaussianRandomGenerator random = new GaussianRandomGenerator(new MersenneTwister());

    private static int edgeGenes = 0;

    private EdgeGene(short incomingNode, short outgoingNode)
    {
        edge = new Edge();

        this.incomingNode = incomingNode;
        this.outgoingNode = outgoingNode;
    }

    EdgeGene mutate()
    {
        EdgeGene tempEdgeGene = actualCloneBecauseJavaIsWrittenByPajeets();

        if (FastMath.random() < changeWeight)
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
        EdgeGene edgeGene = new EdgeGene((short) (FastMath.random() * size), (short) (FastMath.random() * size));
        edgeGene.edge.weight = -10 + (20 * FastMath.random());
        edgeGene.edge.name = edgeGenes;
        edgeGenes++;

        return edgeGene;
    }
}