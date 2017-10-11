package TENN;

class Edge extends Executable
{
    Node incomingNode;
    Node outgoingNode;
    double weight;

    String name;

    public void execute()
    {
        outgoingNode.residue += incomingNode.val * weight;
    }

    Edge()
    {

    }

    @Override
    public String toString()
    {
        return name;
    }
}
