package TENN;

class Edge extends Executable
{
    Node incomingNode;
    Node outgoingNode;
    double weight;

    int name;

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
        return Integer.toString(name);
    }
}
