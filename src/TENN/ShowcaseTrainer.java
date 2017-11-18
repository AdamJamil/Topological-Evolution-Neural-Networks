package TENN;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Stage;
import javafx.util.Duration;
import javazoom.jl.player.advanced.AdvancedPlayer;
import org.apache.commons.math3.util.FastMath;
import java.io.*;
import java.util.ArrayList;

class ShowcaseTrainer
{
    private Timeline timeline;
    private static final int networksPerGeneration = 100;

    private long totalFrames = 0;

    private NeuralNetwork[] generation = new NeuralNetwork[networksPerGeneration];
    private ArrayList<NeuralNetwork> orderedNeuralNetworks = new ArrayList<>(networksPerGeneration);
    private int currentNeuralNetworkIndex, currentGeneration;
    private NeuralNetwork neuralNetwork;

    private int inputs, outputs;

    private double width, height;

    private static double PI = FastMath.PI;

    private double cartPosition = 0; //range of [-100, 100], scale to whatever makes sense for display
    private double cartSpeed = -1; //no real upper or lower limit, but reset to 0 when a wall is hit
    private double poleAngle = Math.PI * 11 / 20; //range of [0, Math.PI]
    private double poleSpeed = 0; //again no real upper or lower limit

    private static final double dt = 0.02;
    private static final double tendt = 10 * dt;
    private static final double initialAngle = FastMath.PI * 11 / 20;
    private static final double leftDeadAngle = FastMath.PI * 9 / 10;
    private static final double rightDeadAngle = FastMath.PI / 10;
    private static final double poleLength = 50; //let's play with this a little before settling on a number
    private static final double g = 9.81; //probably doesn't need explanation
    private static final double dtOverPoleLength = dt / poleLength;
    private static final double gdtOverPoleLength = g * dtOverPoleLength;

    private NeuralNetwork finalNet;

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
        neuralNetwork = generation[0];
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

    ShowcaseTrainer(Stage stage, Canvas canvas, int inputs, int outputs)
    {
        this.inputs = inputs;
        this.outputs = outputs;

        initializePrimaryGeneration();

        width = canvas.getWidth();
        height = canvas.getHeight();

        GraphicsContext gc = canvas.getGraphicsContext2D();

        //boilerplate code
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(false);

        //simulation physics in this loop
        KeyFrame frame = new KeyFrame(Duration.millis(2f), (event) ->
        {
            gc.clearRect(0, 0, width, height);

            if (finalNet.dead || poleAngle < rightDeadAngle || poleAngle > leftDeadAngle || cartPosition < -100 || cartPosition > 100)
            {
                timeline.stop();
                System.exit(0);
            }
            t++;

            //kinematics
            cartSpeed += tendt * tanh(finalNet.execute(cartPosition, cartSpeed, poleAngle, poleSpeed)[0]);
            cartPosition += cartSpeed * dt;
            double cosAngle = FastMath.cos(poleAngle);
            poleSpeed -= cosAngle * gdtOverPoleLength; //change in angular speed due to gravity
            poleAngle = acos((cartSpeed * dtOverPoleLength) + cosAngle); //pole rotation due to motion of cart, derived assuming mass moves straight up
            poleAngle += poleSpeed * dt; //pole rotation due to angular speed

            //setting f(x) = ax + b and forcing f(-100) = 50, f(100) = width - 50 yields
            //f(x) = (width / 20 - 5) x + (width / 2)
            //for aesthetics we have the cart represented as 50 px wide and 10 px tall
            double center = ((100 - width) / 200) * cartPosition + (width / 2);
            gc.fillRect(center - 25, 100, 50, 10);
            gc.strokeLine(center, 100, center + poleLength * Math.cos(poleAngle), 100 - poleLength * Math.sin(poleAngle));
            gc.fillOval(center + poleLength * Math.cos(poleAngle) - 5, 100 - poleLength * Math.sin(poleAngle) - 5, 10, 10);
        });

        timeline.getKeyFrames().add(frame);

        //music thread
        Thread thread = new Thread(() ->
        {
            try
            {
                FileInputStream fileInputStream = new FileInputStream("song.mp3");
                AdvancedPlayer player = new AdvancedPlayer(fileInputStream);
                player.play();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });

        while (true)
        {
            if (t > 5000000)
            {
                //System.out.println(generation[currentNeuralNetworkIndex]);
                thread.start();
                finalNet = neuralNetwork;
                finalNet.clean();
                t = 0;
                cartPosition = 0;
                cartSpeed = -1;
                poleAngle = initialAngle;
                poleSpeed = 0;

                hang();
                System.out.println(finalNet);
                timeline.play();
                break;
            }

            if (neuralNetwork.dead || poleAngle < rightDeadAngle || poleAngle > leftDeadAngle || cartPosition < -100 || cartPosition > 100)
            {
                neuralNetwork.fitness = t;
                if (currentNeuralNetworkIndex == 0)
                    orderedNeuralNetworks.add(generation[0]);
                else
                    insertNetwork();

                totalFrames += t;

                currentNeuralNetworkIndex++;
                if (currentNeuralNetworkIndex == 100)
                {
                    currentNeuralNetworkIndex = 0;
                    currentGeneration++;
                    System.out.println("generation: " + currentGeneration + ",  best: " + orderedNeuralNetworks.get(0).fitness);
                    createNextGeneration();
                    orderedNeuralNetworks.clear();

                    if (totalFrames > 70000000)
                    {
                        try
                        {
                            new Main().start(stage);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        break;
                    }
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
            cartSpeed += tendt * tanh(neuralNetwork.execute(cartPosition, cartSpeed, poleAngle, poleSpeed)[0]);
            cartPosition += cartSpeed * dt;
            double cosAngle = FastMath.cos(poleAngle);
            poleSpeed -= cosAngle * gdtOverPoleLength; //change in angular speed due to gravity
            poleAngle = acos((cartSpeed * dtOverPoleLength) + cosAngle); //pole rotation due to motion of cart,
                                                                         // derived assuming mass moves straight up
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
                orderedNeuralNetworks.add(middle, generation[currentNeuralNetworkIndex]);
                break;
            }
            else if (t > temp)
            {
                right = middle - 1;
                if (left > right)
                {
                    orderedNeuralNetworks.add(middle, generation[currentNeuralNetworkIndex]);
                    break;
                }
            }
            else
            {
                left = middle + 1;
                if (left > right)
                {
                    orderedNeuralNetworks.add(middle + 1, generation[currentNeuralNetworkIndex]);
                    break;
                }
            }
        }
    }

    private void hang()
    {
        try
        {
            System.in.read();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static double tanh(double val)
    {
        return (exp(2 * val) - 1) / (exp(2 * val) + 1);
    }

    private static double exp(double val)
    {
        if (val < -700)
            return 0;
        if (val > 700)
            val = 700;
        final long tmp = (long) (1512775 * val + (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);
    }

    private static final double C0 = 1.57073, C1 = -0.212053, C2 = 0.0740935, C3 = -0.0186166;

    private static double acos(double val)
    {
        val = -val;
        return PI - ((((C3 * val + C2) * val + C1) * val + C0) * Math.sqrt(1 - val));
    }
}