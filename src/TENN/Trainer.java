package TENN;

import org.apache.commons.math3.util.FastMath;
import java.io.*;
import java.util.ArrayList;


class Trainer
{
    private static final int networksPerGeneration = 100;

    private long totalFrames = 0; //counter that will limit how long the trainer has to find a solution

    private NeuralNetwork[] generation = new NeuralNetwork[networksPerGeneration]; //stores all nets in current gen
    //uses binary insertion to rank neural networks according to their fitness
    private ArrayList<NeuralNetwork> orderedNeuralNetworks = new ArrayList<>(networksPerGeneration);
    private int currentNeuralNetworkIndex;
    private NeuralNetwork currentNeuralNetwork;
    private int number; //the index of this simulation

    private int inputs, outputs; //number of input and output nodes for NN

    //constants used in computation
    private static final double dt = 0.02; //time step interval
    private static final double poleLength = 50;
    private static final double g = 9.81;
    private static final double tendt = 10 * dt;
    private static final double initialAngle = FastMath.PI * 11 / 20; //starting angle for simulation
    private static final double leftDeadAngle = FastMath.PI * 9 / 10;
    private static final double rightDeadAngle = FastMath.PI / 10;
    private static final double dtOverPoleLength = dt / poleLength;
    private static final double gdtOverPoleLength = g * dtOverPoleLength;

    private long t = 0; //measures frames that the current neural network has survived for

    //fills the first generation with random neural networks
    private void initializePrimaryGeneration()
    {
        for (int i = 0; i < networksPerGeneration; i++)
        {
            //randomly generates data for the neural network
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

    //uses the ranking of the neural networks to create a distribution for the next generation
    private void createNextGeneration()
    {
        //note: this distribution works much better than usual methods, which involve mutating the top 50% and adding
        //some amount of arbitrary noise. this has been tested with noise approximated by an appropriate sigmoid curve
        //also, cloning the top 5 gives better results, but somewhat stagnates the gene pool. this should be tested more
        //rigorously to be sure
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

    //number refers to which simulation this is, with respect to the order of launching in Main
    Trainer(int number, int inputs, int outputs)
    {
        double cartPosition = 0; //x value sent to NN, [-100, 100]
        double cartSpeed = -1; //v value sent to NN
        double poleAngle = Math.PI * 11 / 20; //theta sent to NN, [pi / 10, 9pi / 10]
        double poleSpeed = 0; //omega sent to NN

        this.inputs = inputs;
        this.outputs = outputs;
        this.number = number;

        initializePrimaryGeneration();
        currentNeuralNetwork = generation[0]; //sets current network to first net in the generation

        while (true)
        {
            //we were unable to find a standard time, so 5000000 was chosen, which is about 3 hours (at 500 fps)
            if (t > 5000000)
            {
                passLog();
                break; //break command will end this thread
            }

            if (currentNeuralNetwork.dead || poleAngle < rightDeadAngle || poleAngle > leftDeadAngle
                    || cartPosition < -100 || cartPosition > 100)
            {
                //we save the fitness so that it can be compared to later on
                currentNeuralNetwork.fitness = t;

                //insertNetwork does not handle the empty case, so we handle it here
                if (currentNeuralNetworkIndex == 0)
                    orderedNeuralNetworks.add(generation[0]);
                else
                    insertNetwork();

                totalFrames += t;

                if (currentNeuralNetworkIndex == 99)
                {
                    //somewhat arbitrary upper bound that catches many failing populations from spending too much time
                    if (totalFrames > 70000000)
                    {
                        failLog();
                        break; //break command will end this thread
                    }
                    currentNeuralNetworkIndex = 0;
                    createNextGeneration();
                    orderedNeuralNetworks.clear();
                }

                currentNeuralNetworkIndex++; //putting the increment after the break saves us one operation :')

                //reset conditions
                currentNeuralNetwork = generation[currentNeuralNetworkIndex];
                t = 0;
                cartPosition = 0;
                cartSpeed = -1;
                poleAngle = initialAngle;
                poleSpeed = 0;
                continue;
            }

            t++;

            //tanh, acos and cos are heavily tested - it is possible that cos can be optimized further, but for some
            //reason, Riven.cos does not work as expected. different implementations do not seem as effective, so this
            //is a low priority optimization
            //the biggest bottleneck is here: NeuralNetwork.execute. this is the highest priority optimization
            cartSpeed += tendt * tanh(currentNeuralNetwork.execute(cartPosition, cartSpeed, poleAngle, poleSpeed)[0]);
            cartPosition += cartSpeed * dt;
            double cosAngle = FastMath.cos(poleAngle); //value is used twice, so it is worth making a variable for
            poleSpeed -= cosAngle * gdtOverPoleLength; //change in angular speed due to gravity
            poleAngle = acos((cartSpeed * dtOverPoleLength) + cosAngle); //pole rotation due to motion of cart, derived assuming mass moves straight up
            poleAngle += poleSpeed * dt; //pole rotation due to angular speed
        }
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
                orderedNeuralNetworks.add(middle, currentNeuralNetwork);
                break;
            }
            else if (t > temp)
            {
                right = middle - 1;
                if (left > right)
                {
                    orderedNeuralNetworks.add(middle, currentNeuralNetwork);
                    break;
                }
            }
            else
            {
                left = middle + 1;
                if (left > right)
                {
                    orderedNeuralNetworks.add(middle + 1, currentNeuralNetwork);
                    break;
                }
            }
        }
    }

    private void passLog()
    {
        System.out.println("Simulation " + number + " stopped; current network fitness over 5000000");
        try
        {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("log/experiment10/success/" + totalFrames + ".txt"), "utf-8"));
            writer.write("Final network:" + System.getProperty("line.separator"));
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void failLog()
    {
        System.out.println("Simulation " + number + " stopped; generation 1000 reached");
        try
        {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("log/experiment10/fail/" + totalFrames + ".txt"), "utf-8"));
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //function has about 2% error, which is more or less good enough
    private static double tanh(double val)
    {
        return (exp(2 * val) - 1) / (exp(2 * val) + 1);
    }

    //function has about 1.5% error, which is more or less good enough
    private static double exp(double val)
    {
        if (val < -700)
            return 0;
        if (val > 700)
            val = 700;
        final long tmp = (long) (1512775 * val + (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);
    }

    //values that are used for acos method
    private static final double C0 = 1.57073, C1 = -0.212053, C2 = 0.0740935, C3 = -0.0186166;

    //function is accurate, but accuracy is unknown
    private static double acos(double val)
    {
        val = -val;
        return Math.PI - ((((C3 * val + C2) * val + C1) * val + C0) * Math.sqrt(1 - val));
    }
}