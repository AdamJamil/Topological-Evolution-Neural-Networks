package TENN;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.util.Duration;
import javazoom.jl.player.advanced.AdvancedPlayer;
import org.apache.commons.math3.util.FastMath;

import java.io.*;
import java.util.ArrayList;


class Trainer
{
    Timeline timeline;
    static final int networksPerGeneration = 100;

    NeuralNetwork[] generation = new NeuralNetwork[networksPerGeneration];
    ArrayList<NeuralNetwork> orderedNeuralNetworks = new ArrayList<>(networksPerGeneration);
    int currentNeuralNetwork, currentGeneration;

    String[] inputNames, outputNames;

    double width, height;

    double cartPosition = 0; //range of [-100, 100], scale to whatever makes sense for display
    double cartSpeed = -1; //no real upper or lower limit, but reset to 0 when a wall is hit
    double poleAngle = Math.PI * 11 / 20; //range of [0, Math.PI]
    double poleSpeed = 0; //again no real upper or lower limit

    static final double dt = 0.02;
    static final double tendt = 10 * dt;
    static final double initialAngle = FastMath.PI * 11 / 20;
    static final double leftDeadAngle = FastMath.PI * 9 / 10;
    static final double rightDeadAngle = FastMath.PI / 10;
    static final double poleLength = 50; //let's play with this a little before settling on a number
    static final double g = 9.81; //probably doesn't need explanation
    static final double dtOverPoleLength = dt / poleLength;
    static final double gdtOverPoleLength = g * dtOverPoleLength;
    static final double oneOverSixteen = 1 / 16;

    long allTimeBest = 0;

    long t = 0;

    void initializePrimaryGeneration()
    {
        for (int i = 0; i < networksPerGeneration; i++)
        {
            short size = (short) (8 + (Math.random() * 10));

            NodeGene[] inputNodeGenes = new NodeGene[3];
            EdgeGene[] inputEdgeGenes = new EdgeGene[9];

            for (int j = 0; j < 3; j++)
                inputNodeGenes[j] = NodeGene.randomNodeGene(j, inputNames.length, outputNames.length, size);

            for (int j = 0; j < 9; j++)
                inputEdgeGenes[j] = EdgeGene.randomEdgeGene(j, size);

            generation[i] = new NeuralNetwork(inputNames, outputNames, inputNodeGenes, inputEdgeGenes, size);
        }

    }

    void createNextGeneration()
    {
        //top 1 - 5: 1 clone, 2 mutations : 15
        //top 6 - 20: 3 mutations         : 45
        //top 21 - 30: 2 mutations        : 20
        //top 31 - 50: 1 mutation         : 20
        for (int i = 0; i < 5; i++)
        {
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

    void runGeneration()
    {
        timeline.play();
    }

    Trainer(int number, Canvas canvas, String[] inputNames, String[] outputNames)
    {
        this.inputNames = inputNames;
        this.outputNames = outputNames;

        initializePrimaryGeneration();

        width = canvas.getWidth();
        height = canvas.getHeight();

        /*GraphicsContext gc = canvas.getGraphicsContext2D();

        //boilerplate code
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(false);

        //simulation physics in this loop
        KeyFrame frame = new KeyFrame(Duration.millis(2f), (event) ->
        {
            gc.clearRect(0, 0, width, height);

            if (generation[0].dead || poleAngle < rightDeadAngle || poleAngle > leftDeadAngle || cartPosition < -100 || cartPosition > 100)
            {
                timeline.stop();
                System.exit(0);
            }
            t++;

            //kinematics
            double output = generation[currentNeuralNetwork].execute(new double[]{cartPosition, cartSpeed, poleAngle, poleSpeed})[0];

            cartSpeed += 10 * dt * Math.tanh(output / 16);
            cartPosition += cartSpeed * dt;
            poleSpeed -= (g * Math.cos(poleAngle) / poleLength) * dt; //change in angular speed due to gravity
            poleAngle = Math.acos((cartSpeed * dt / poleLength) + Math.cos(poleAngle)); //pole rotation due to motion of cart, derived assuming mass moves straight up
            poleAngle += poleSpeed * dt; //pole rotation due to angular speed

            //setting f(x) = ax + b and forcing f(-100) = 50, f(100) = width - 50 yields
            //f(x) = (width / 20 - 5) x + (width / 2)
            //for aesthetics we have the cart represented as 50 px wide and 10 px tall
            double center = ((100 - width) / 200) * cartPosition + (width / 2);
            gc.fillRect(center - 25, 100, 50, 10);
            gc.strokeLine(center, 100, center + poleLength * Math.cos(poleAngle), 100 - poleLength * Math.sin(poleAngle));
            gc.fillOval(center + poleLength * Math.cos(poleAngle) - 5, 100 - poleLength * Math.sin(poleAngle) - 5, 10, 10);
        });

        //boilerplate code
        timeline.getKeyFrames().add(frame);
        //timeline.play();

        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    FileInputStream fileInputStream = new FileInputStream("song.mp3");
                    AdvancedPlayer player = new AdvancedPlayer(fileInputStream);
                    System.out.println("what");
                    player.play();
                    System.out.println("what");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });*/

        while (true)
        {
            if (t > 5000000)
            {
                System.out.println("Simulation " + number + " stopped; current network fitness over 5000000");
                //System.out.println(generation[currentNeuralNetwork]);
                /*thread.start();
                hang();
                generation[0] = generation[currentNeuralNetwork];
                generation[0].clean();
                t = 0;
                cartPosition = 0;
                cartSpeed = -1;
                poleAngle = initialAngle;
                poleSpeed = 0;
                break;*/

                try
                {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("log/experiment2/success/" + currentGeneration + ".txt"), "utf-8"));
                    writer.write("Final network:" + System.getProperty("line.separator"));
                    writer.write(generation[currentNeuralNetwork].toString());
                    writer.close();
                }
                catch (Exception e)
                {

                }
                break;
            }

            if (generation[currentNeuralNetwork].dead || poleAngle < rightDeadAngle || poleAngle > leftDeadAngle || cartPosition < -100 || cartPosition > 100)
            {
                generation[currentNeuralNetwork].fitness = t;
                if (currentNeuralNetwork == 0)
                    orderedNeuralNetworks.add(generation[0]);
                else
                    insertNetwork();

                currentNeuralNetwork++;
                //System.out.println(t + "     exec path: " + generation[currentNeuralNetwork - 1]);
                if (currentNeuralNetwork == 100)
                {
                    allTimeBest = (allTimeBest < orderedNeuralNetworks.get(0).fitness) ? orderedNeuralNetworks.get(0).fitness : allTimeBest;
                    if (currentGeneration == 2500)
                    {
                        System.out.println("Simulation " + number + " stopped; generation 2500 reached");
                        try
                        {
                            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("log/experiment2/fail/" + allTimeBest + ".txt"), "utf-8"));
                            writer.write("Failed after 2500 generations" + System.getProperty("line.separator"));
                            writer.write("Best: " + allTimeBest);
                            writer.close();
                        }
                        catch (Exception e)
                        {

                        }
                        break;

                    }
                    currentNeuralNetwork = 0;
                    currentGeneration++;
                    //System.out.println("generation: " + currentGeneration + ",  best: " + orderedNeuralNetworks.get(0).fitness);
                    createNextGeneration();
                    orderedNeuralNetworks.clear();
                }
                //else
                //    System.out.print(currentNeuralNetwork + ": ");

                t = 0;
                cartPosition = 0;
                cartSpeed = -1;
                poleAngle = initialAngle;
                poleSpeed = 0;
                continue;
            }

            t++;

            //kinematics
            double output = generation[currentNeuralNetwork].execute(new double[]{cartPosition, cartSpeed, poleAngle, poleSpeed})[0];

            cartSpeed += tendt * FastMath.tanh(output * oneOverSixteen);
            cartPosition += cartSpeed * dt;
            poleSpeed -= FastMath.cos(poleAngle) * gdtOverPoleLength; //change in angular speed due to gravity
            poleAngle = FastMath.acos((cartSpeed * dtOverPoleLength) + FastMath.cos(poleAngle)); //pole rotation due to motion of cart, derived assuming mass moves straight up
            poleAngle += poleSpeed * dt; //pole rotation due to angular speed
        }

        //runGeneration();
    }

    //binary search function
    void insertNetwork()
    {
        int left = 0, right = currentNeuralNetwork - 1, middle;

        while (true)
        {
            middle = (left + right) / 2;
            long temp = orderedNeuralNetworks.get(middle).fitness;
            if (t == temp)
            {
                orderedNeuralNetworks.add(middle, generation[currentNeuralNetwork]);
                break;
            }
            else if (t > temp)
            {
                right = middle - 1;
                if (left > right)
                {
                    orderedNeuralNetworks.add(middle, generation[currentNeuralNetwork]);
                    break;
                }
            }
            else
            {
                left = middle + 1;
                if (left > right)
                {
                    orderedNeuralNetworks.add(middle + 1, generation[currentNeuralNetwork]);
                    break;
                }
            }
        }
    }

    void hang()
    {
        try
        {
            System.in.read();
        }
        catch (Exception e)
        {

        }
    }
}
