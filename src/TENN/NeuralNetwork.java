package TENN;

import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

class NeuralNetwork
{
    static final double mutationMultiplier = 1.5;
    static final double addNode = 0.2 * mutationMultiplier;
    static final double addEdge = 0.3 * mutationMultiplier;
    static final double increaseSize = 0.20 * mutationMultiplier;
    static final double mutateEdge = 0.35 * mutationMultiplier;
    static final double mutateNode = 0.35 * mutationMultiplier;
    static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    int nodeCounter = 0;
    int edgeCounter = 0;

    long fitness; //number of frames survived for

    boolean dead; //if something goes wrong during "birth", the NN is dead
    boolean hitOutput; //keeps track if an output has been hit during traveral. if not, then NN is killed
    boolean selected = true;

    short outputs;

    ArrayList<Node> nodes = new ArrayList<>(); //stores every node from NodeGenes
    Node[] boxes; //sparse array of nodes. every edge will connect to an entry, and if that entry has a node, it connects to that node
    //this means edges might connect to nothing, but it preserves existing connections better across mutations

    String[] inputNames, outputNames;
    NodeGene[] nodeGenes;
    EdgeGene[] edgeGenes;
    short size; //size of boxes array. it should be small at first to ensure that at least some edges connect to something

    ArrayList<Executable> orderedExecutables = new ArrayList<>(); //contains nodes and edges, in the order that they should be executed in
    ArrayList<Edge> recurrentEdges = new ArrayList<>(); //contains all the edges that will link to the next execute call

    ArrayList<Node> stack = new ArrayList<>(); //mostly a temporary arraylist used in the traverse method

    Node[] inLayer;
    Node[] outLayer;

    double[] execute(double[] input)
    {
        //loads input values into the corresponding nodes
        for (int i = 0; i < input.length; i++)
            boxes[i].val = input[i];

        //executes all nodes and edges in order. this gives us the output
        for (Executable object : orderedExecutables)
            object.execute();

        //load output into double array
        double[] result = new double[outputs];
        for (int i = 0; i < outputs; i++)
            result[i] = boxes[size - i - 1].residue;

        //reset the residue so that it does not affect the next iteration
        for (Node node : nodes)
            node.residue = 0;

        //executes the recurrent edges, pushing the old values into new residues
        for (Edge recurrentEdge : recurrentEdges)
            recurrentEdge.execute();

        //clears values. this doesn't actually have to be done, as .execute resets val anyways
        for (Node node : nodes)
            node.val = 0;
        return result;
    }

    NeuralNetwork(String[] inputNames, String[] outputNames, NodeGene[] inputNodeGenes, EdgeGene[] inputEdgeGenes, short inputSize)
    {
        this.inputNames = inputNames;
        this.outputNames = outputNames;
        nodeGenes = inputNodeGenes;
        edgeGenes = inputEdgeGenes;
        size = inputSize;
        boxes = new Node[size];

        outputs = (short) outputNames.length;

        inLayer = new Node[inputNames.length];
        outLayer = new Node[outputNames.length];

        for (int i = 0; i < inLayer.length; i++)
            inLayer[i] = new Node(inputNames[i]);

        for (int i = 0; i < outLayer.length; i++)
        {
            outLayer[i] = new Node(outputNames[i]);
            outLayer[i].output = true;
        }

        //load all input and output nodes into arrayList
        Collections.addAll(nodes, inLayer);
        Collections.addAll(nodes, outLayer);

        if (boxes.length < inLayer.length)
        {
            dead = true;
            //System.out.println("dead 1");
            return;
        }
        else for (int i = 0; i < inLayer.length; i++)
        {
            boxes[i] = inLayer[i];
            boxes[i].input = true;
        }

        for (int i = size - 1; i >= size - outputs; i--)
        {
            if (boxes[i] != null)
            {
                dead = true;
                //System.out.println("dead 2");
                return;
            }
            boxes[i] = outLayer[size - 1 - i];
            boxes[i].output = true;
        }

        //load all expressed nodes into arrayList and boxes
        for (NodeGene inputNodeGene : inputNodeGenes)
        {
            if (boxes.length <= inputNodeGene.position)
                continue;
            /*if (boxes[inputNodeGene.position] != null)
            {
                dead = true;
                return;
            }*/
            boxes[inputNodeGene.position] = inputNodeGene.node;
            nodes.add(inputNodeGene.node);
        }

        for (EdgeGene edgeGene : inputEdgeGenes)
            if (edgeGene.outgoingNode < size && edgeGene.incomingNode < size)
                if (boxes[edgeGene.incomingNode] != null && boxes[edgeGene.outgoingNode] != null)
                    if (!boxes[edgeGene.incomingNode].output && !boxes[edgeGene.outgoingNode].input)
                        if (edgeGene.incomingNode != edgeGene.outgoingNode)
                        {
                            edgeGene.edge.incomingNode = boxes[edgeGene.incomingNode];
                            edgeGene.edge.outgoingNode = boxes[edgeGene.outgoingNode];
                            edgeGene.edge.incomingNode.outgoingEdges.add(edgeGene.edge);
                        }

        for (Node inputNode : inLayer)
            for (Edge inputEdge : inputNode.outgoingEdges)
            {
                stack.add(inputNode);
                traverse(inputEdge);
                stack.remove(inputNode);
            }

        if (!hitOutput)
        {
            dead = true;
            //System.out.println("dead 4");
            return;
        }

        orderedExecutables.removeAll(Arrays.asList(inLayer));
        orderedExecutables.removeAll(Arrays.asList(outLayer));
    }

    boolean traverse(Edge edge)
    {
        edge.name = Integer.toString(edgeCounter);
        edgeCounter++;

        if (edge.outgoingNode.output)
        {
            hitOutput = true;
            edge.incomingNode.reachesOutput = true;
        }

        if (stack.contains(edge.outgoingNode))
        {
            recurrentEdges.add(edge);
            return true;
        }

        if (edge.outgoingNode.visited)
        {
            orderedExecutables.add(0, edge);
            return true;
        }

        if (!edge.outgoingNode.output)
        {
            edge.outgoingNode.name = "" + alphabet.charAt(nodeCounter);
            nodeCounter++;
        }
        edge.outgoingNode.visited = true;
        stack.add(edge.outgoingNode);

        for (Edge edge2 : edge.outgoingNode.outgoingEdges)
            edge.outgoingNode.reachesOutput |= traverse(edge2);

        if (edge.outgoingNode.reachesOutput)
        {
            orderedExecutables.remove(edge.outgoingNode);
            orderedExecutables.add(0, edge.outgoingNode);
            orderedExecutables.add(0, edge);
        }

        stack.remove(edge.outgoingNode);

        return edge.outgoingNode.reachesOutput;

    }

    NeuralNetwork mutate()
    {
        int numberOfNodeGenes = nodeGenes.length;
        int numberOfEdgeGenes = edgeGenes.length;
        int mutatedSize = size;

        while (FastMath.random() < addNode)
            numberOfNodeGenes++;
        while (FastMath.random() < addEdge)
            numberOfEdgeGenes++;
        if (FastMath.random() < increaseSize)
            mutatedSize = (int) (10 * FastMath.random()) + size;

        NodeGene[] mutatedNodeGenes = new NodeGene[numberOfNodeGenes];
        EdgeGene[] mutatedEdgeGenes = new EdgeGene[numberOfEdgeGenes];

        for (int i = 0; i < nodeGenes.length; i++)
            if (FastMath.random() < mutateNode)
                mutatedNodeGenes[i] = nodeGenes[i].mutate();
            else
                mutatedNodeGenes[i] = nodeGenes[i].actualCloneBecauseJavaIsWrittenByPajeets();

        for (int i = nodeGenes.length; i < numberOfNodeGenes; i++)
            mutatedNodeGenes[i] = NodeGene.randomNodeGene(i, inputNames.length, outputNames.length, mutatedSize);

        for (int i = 0; i < edgeGenes.length; i++)
            if (FastMath.random() < mutateEdge)
                mutatedEdgeGenes[i] = edgeGenes[i].mutate();
            else
                mutatedEdgeGenes[i] = edgeGenes[i].actualCloneBecauseJavaIsWrittenByPajeets();

        for (int i = edgeGenes.length; i < numberOfEdgeGenes; i++)
            mutatedEdgeGenes[i] = EdgeGene.randomEdgeGene(i, mutatedSize);

        NeuralNetwork tempNeuralNetwork = new NeuralNetwork(inputNames, outputNames, mutatedNodeGenes, mutatedEdgeGenes, (short) mutatedSize);
        tempNeuralNetwork.dead |= mutatedSize < inputNames.length + outputNames.length;

        return tempNeuralNetwork;
    }

    @Override
    public String toString()
    {
        if (orderedExecutables.isEmpty())
            return "";

        String newline = System.getProperty("line.separator");
        String output = "{" + newline;
        for (Executable temp : orderedExecutables)
            if (temp instanceof Edge)
                output += "\t" + temp + ": (" + Math.floor(((Edge) temp).weight * 100) / 100 + ((Edge) temp).incomingNode + ") → " + ((Edge) temp).outgoingNode + newline;
            else if (temp instanceof Node)
            {
                output += "\t" + temp + ": f(x) = ";
                Node tempNode = (Node) temp;
                double tempDouble = FastMath.floor(tempNode.AC1 * 100) / 100;
                double tempDouble2 = FastMath.floor(tempNode.AC2 * 100) / 100;
                switch (tempNode.activationType % 9)
                {
                    case 0:
                        output += FastMath.floor(tempNode.AC1 * tempNode.AC2 * 100) / 100 + "x" + newline;
                        break;

                    case 1:
                        output += tempDouble + "max{-1, min{1, " + tempDouble2 + "x}}" + newline;
                        break;

                    case 2:
                        output += tempDouble + " * (1 " + ((FastMath.signum(tempNode.AC2) == -1) ? "- sgn(x)" : "+ sgn(x)") + ") / 2" + newline;
                        break;

                    case 3:
                        output += ((FastMath.signum(tempNode.AC2) == -1) ? "-)" : "") + tempDouble + "sgn(x)" + newline;
                        break;

                    case 4:
                        output += tempDouble + " / (1 + exp(" + (-FastMath.abs(tempDouble2)) + "x))" + newline;
                        break;

                    case 5:
                        output += tempDouble + "tanh(" + tempDouble2 + "x)" + newline;
                        break;

                    case 6:
                        output += tempDouble + "max{0, " + tempDouble2 + " * x}" + newline;
                        break;

                    case 7:
                        output += tempDouble + "exp(" + (-tempDouble2) + "x^2)" + newline;
                        break;
                }
            }
        output += "}";
        return output;
    }
}