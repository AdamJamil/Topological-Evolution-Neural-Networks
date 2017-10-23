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

    private NeuralNetwork[] generation = new NeuralNetwork[networksPerGeneration];
    private ArrayList<NeuralNetwork> orderedNeuralNetworks = new ArrayList<>(networksPerGeneration);
    private int currentNeuralNetworkIndex, currentGeneration;
    private NeuralNetwork neuralNetwork;

    private int inputs, outputs;

    private double width, height;

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

    private void run()
    {
        System.out.println(finalNet);
        timeline.play();
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
            double output = finalNet.execute(cartPosition, cartSpeed, poleAngle, poleSpeed)[0];

            cartSpeed += tendt * tanh(output / 16);
            cartPosition += cartSpeed * dt;
            poleSpeed -= FastMath.cos(poleAngle) * gdtOverPoleLength; //change in angular speed due to gravity
            poleAngle = FastMath.acos((cartSpeed * dtOverPoleLength) + FastMath.cos(poleAngle)); //pole rotation due to motion of cart, derived assuming mass moves straight up
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
                finalNet = generation[currentNeuralNetworkIndex];
                finalNet.clean();
                t = 0;
                cartPosition = 0;
                cartSpeed = -1;
                poleAngle = initialAngle;
                poleSpeed = 0;

                hang();
                run();
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
                    currentNeuralNetworkIndex = 0;
                    currentGeneration++;
                    System.out.println("generation: " + currentGeneration + ",  best: " + orderedNeuralNetworks.get(0).fitness);
                    createNextGeneration();
                    orderedNeuralNetworks.clear();

                    if (currentGeneration == 200)
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
            double cosAngle = Riven.cos((float) poleAngle);
            poleSpeed -= cosAngle * gdtOverPoleLength; //change in angular speed due to gravity
            poleAngle = FastMath.acos((cartSpeed * dtOverPoleLength) + cosAngle); //pole rotation due to motion of cart, derived assuming mass moves straight up
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

    public static final class Riven
    {
        private static final int SIN_BITS, SIN_MASK, SIN_COUNT;
        private static final double radFull, radToIndex;
        private static final double degFull, degToIndex;
        private static final double[] cos;

        static {
            SIN_BITS = 12;
            SIN_MASK = ~(-1 << SIN_BITS);
            SIN_COUNT = SIN_MASK + 1;

            radFull = (Math.PI * 2.0);
            degFull = 360.0;
            radToIndex = SIN_COUNT / radFull;
            degToIndex = SIN_COUNT / degFull;

            cos = new double[SIN_COUNT];

            for (int i = 0; i < SIN_COUNT; i++)
                cos[i] = Math.cos((i + 0.5) / SIN_COUNT * radFull);

            // Four cardinal directions (credits: Nate)
            for (int i = 0; i < 360; i += 90)
                cos[(int) (i * degToIndex) & SIN_MASK] = Math.cos(i * Math.PI / 180.0);
        }

        static double cos(float rad)
        {
            return cos[(int) (rad * radToIndex) & SIN_MASK];
        }
    }
}