package TENN;

import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;

class Node extends Executable
{
    ArrayList<Edge> outgoingEdges = new ArrayList<>();

    boolean visited;
    boolean output;
    boolean input;
    boolean cleaned;
    boolean reachesOutput = false;

    double AC1; //activation constant 1; range bounded to [0, 10], used as a multiplicative constant

    double AC2; //activation constant 2; range bounded to [0, 2]. idea is to have
    //equal chance of squashing (x \in [-1, 1]$ and stretching (x \not \in [-1, 1])

    //the reason for using independent variables for the sign of the constants is
    //1) omit Math.abs() calls
    //2) more control during mutation

    short activationType; //range [0, 8] \cap \nat

    double residue; //sum of all inputs
    double val; //result of activation function

    String name;

    void execute()
    {

        if (residue > 2000000000)
            residue = 2000000000;
        if (residue < -2000000000)
            residue = -2000000000;

        //switches activation type to figure out which function to use
        switch (activationType % 9) //there are 9 activation functions i picked
        {
            //identity (linear) activation function
            case 0:
                val = AC1 * AC2 * residue;
                break;

            //piecewise linear, referred to as hardtanh
            case 1:
                val = AC1 * FastMath.max(-1, FastMath.min(1, FastMath.abs(AC2) * residue));
                break;

            //step function
            case 2:
                val = AC1 * (1 + FastMath.signum(AC2 * residue)) / 2;
                break;

            //bipolar (step function with -1 instead of 0 as output)
            case 3:
                val = AC1 * FastMath.signum(AC2 * residue);
                break;

            //logistic function
            case 4:
                val = AC1 / (1 + FastMath.exp(-FastMath.abs(AC2) * residue));
                break;

            //tanh
            case 5:
                val = AC1 * FastMath.tanh(AC2 * residue);
                break;

            //ReLU/rectifier
            case 6:
                val = AC1 * FastMath.max(0, AC2 * residue);
                break;

            //gaussian
            case 7:
                val = AC1 * FastMath.exp(-FastMath.abs(AC2) * residue * residue);
                break;
        }
    }

    Node()
    {
        AC2 = 1;
        AC1 = 1;
        activationType = 0;
    }

    Node(String name)
    {
        this();
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}

