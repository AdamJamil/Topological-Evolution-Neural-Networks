package TENN;

import org.apache.commons.math3.util.FastMath;
import java.io.*;
import java.util.ArrayList;


class Trainer
{
    private static final int networksPerGeneration = 100;

    private long[] bests = new long[1000];

    private NeuralNetwork[] generation = new NeuralNetwork[networksPerGeneration];
    private ArrayList<NeuralNetwork> orderedNeuralNetworks = new ArrayList<>(networksPerGeneration);
    private int currentNeuralNetworkIndex, currentGeneration;
    private NeuralNetwork neuralNetwork;

    private double cartPosition = 0; //range of [-100, 100], scale to whatever makes sense for display
    private double cartSpeed = -1; //no real upper or lower limit, but reset to 0 when a wall is hit
    private double poleAngle = Math.PI * 11 / 20; //range of [0, Math.PI]
    private double poleSpeed = 0; //again no real upper or lower limit

    private int inputs, outputs;

    private static final double dt = 0.02;
    private static final double tendt = 10 * dt;
    private static final double initialAngle = FastMath.PI * 11 / 20;
    private static final double leftDeadAngle = FastMath.PI * 9 / 10;
    private static final double rightDeadAngle = FastMath.PI / 10;
    private static final double poleLength = 50; //let's play with this a little before settling on a number
    private static final double g = 9.81; //probably doesn't need explanation
    private static final double dtOverPoleLength = dt / poleLength;
    private static final double gdtOverPoleLength = g * dtOverPoleLength;

    private long allTimeBest = 0;

    private long t = 0;

    private void initializePrimaryGeneration()
    {
        for (int i = 0; i < networksPerGeneration; i++)
        {
            short size = (short) (8 + (Math.random() * 10));

            NodeGene[] inputNodeGenes = new NodeGene[3];
            EdgeGene[] inputEdgeGenes = new EdgeGene[9];

            for (int j = 0; j < 3; j++)
                inputNodeGenes[j] = NodeGene.randomNodeGene(inputs, outputs, size);

            for (int j = 0; j < 9; j++)
                inputEdgeGenes[j] = EdgeGene.randomEdgeGene(size);

            generation[i] = new NeuralNetwork(inputs, outputs, inputNodeGenes, inputEdgeGenes, size);
        }
    }

    private void createNextGeneration()
    {
        //top 1 - 5: 1 clone, 2 mutations : 15
        //top 6 - 20: 3 mutations         : 45
        //top 21 - 30: 2 mutations        : 20
        //top 31 - 50: 1 mutation         : 20
        for (int i = 0; i < 5; i++)
        {
            orderedNeuralNetworks.get(i).clean();
            generation[i * 3] = orderedNeuralNetworks.get(i);
            generation[i * 3 + 1] = orderedNeuralNetworks.get(i).mutate();
            generation[i * 3 + 2] = orderedNeuralNetworks.get(i).mutate();
        }

        for (int i = 5; i < 20; i++)
        {
            generation[i * 3] = orderedNeuralNetworks.get(i).mutate();
            generation[i * 3 + 1] = orderedNeuralNetworks.get(i).mutate();
            generation[i * 3 + 2] = orderedNeuralNetworks.get(i).mutate();
        }

        for (int i = 20; i < 30; i++)
        {
            generation[i * 2 + 20] = orderedNeuralNetworks.get(i).mutate();
            generation[i * 2 + 21] = orderedNeuralNetworks.get(i).mutate();
        }

        for (int i = 30; i < 50; i++)
            generation[i + 50] = orderedNeuralNetworks.get(i).mutate();
    }

    Trainer(int number, int inputs, int outputs)
    {
        this.inputs = inputs;
        this.outputs = outputs;

        initializePrimaryGeneration();

        while (true)
        {
            if (t > 5000000)
            {
                System.out.println("Simulation " + number + " stopped; current network fitness over 5000000");

                try
                {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("log/experiment7/success/" + currentGeneration + ".txt"), "utf-8"));
                    for (int i = 0; i < currentGeneration - 1; i++)
                        writer.write(bests[i] + "");
                    writer.write("Final network:" + System.getProperty("line.separator"));
                    writer.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            }

            if (neuralNetwork.dead || poleAngle < rightDeadAngle || poleAngle > leftDeadAngle || cartPosition < -100 || cartPosition > 100)
            {
                neuralNetwork.fitness = t;
                if (currentNeuralNetworkIndex == 0)
                    orderedNeuralNetworks.add(generation[0]);
                else
                    insertNetwork();

                currentNeuralNetworkIndex++;
                if (currentNeuralNetworkIndex == 100)
                {
                    allTimeBest = (allTimeBest < orderedNeuralNetworks.get(0).fitness) ? orderedNeuralNetworks.get(0).fitness : allTimeBest;
                    bests[currentGeneration - 1] = orderedNeuralNetworks.get(0).fitness;
                    if (currentGeneration == 1000)
                    {
                        System.out.println("Simulation " + number + " stopped; generation 1000 reached");
                        try
                        {
                            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("log/experiment7/fail/" + number + " " + allTimeBest + ".txt"), "utf-8"));
                            for (int i = 0; i < 1000; i++)
                                writer.write(bests[i] + "\n");
                            writer.close();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        break;

                    }
                    currentNeuralNetworkIndex = 0;
                    currentGeneration++;
                    createNextGeneration();
                    orderedNeuralNetworks.clear();
                }

                neuralNetwork = generation[currentNeuralNetworkIndex];
                t = 0;
                cartPosition = 0;
                cartSpeed = -1;
                poleAngle = initialAngle;
                poleSpeed = 0;
                continue;
            }

            t++;

            //kinematics
            double output = neuralNetwork.execute(new double[]{cartPosition, cartSpeed, poleAngle, poleSpeed})[0];

            cartSpeed += tendt * tanh(output / 16);
            cartPosition += cartSpeed * dt;
            poleSpeed -= FastMath.cos(poleAngle) * gdtOverPoleLength; //change in angular speed due to gravity
            poleAngle = FastMath.acos((cartSpeed * dtOverPoleLength) + FastMath.cos(poleAngle)); //pole rotation due to motion of cart, derived assuming mass moves straight up
            poleAngle += poleSpeed * dt; //pole rotation due to angular speed
        }

        //runGeneration();
    }

    //binary search function
    private void insertNetwork()
    {
        int left = 0, right = currentNeuralNetworkIndex - 1, middle;

        while (true)
        {
            middle = (left + right) / 2;
            long temp = orderedNeuralNetworks.get(middle).fitness;
            if (t == temp)
            {
                orderedNeuralNetworks.add(middle, neuralNetwork);
                break;
            }
            else if (t > temp)
            {
                right = middle - 1;
                if (left > right)
                {
                    orderedNeuralNetworks.add(middle, neuralNetwork);
                    break;
                }
            }
            else
            {
                left = middle + 1;
                if (left > right)
                {
                    orderedNeuralNetworks.add(middle + 1, neuralNetwork);
                    break;
                }
            }
        }
    }

    private static double tanh(double val)
    {
        return (exp(2 * val) - 1) / (exp(2 * val) + 1);
    }

    private static double exp(double val)
    {
        final long tmp = (long) (1512775 * val + (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);
    }
}