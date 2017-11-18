package TENN;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application
{
    private static final boolean showcaseMode = true; //use for "showing off" the program, like a demo mode
    static final String[] inputs = new String[]{"x", "v", "θ", "ω"}; //defines all input nodes for the NN
    static final String[] outputs = new String[]{"force"}; //defines all output nodes for the NN
    synchronized

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        primaryStage.setTitle("TENN");
        Group root = new Group();
        Canvas canvas = new Canvas(500, 250);
        root.getChildren().add(canvas);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK); //everything will be black, not worth alternating colors because of performance drop

        new ShowcaseTrainer(primaryStage, canvas, inputs.length, outputs.length);
    }

    public static void main(String[] args)
    {
        if (showcaseMode)
            launch(args);
        else for (int i = 0; i < 100; i++)
        {
            final int temp = i; //since it is accessed by a run method inside the lambda, this needs to be final
            new Thread(() -> new Trainer(temp, inputs.length, outputs.length)).start(); //lambda expression with hardcoded inputs and outputs
        }
    }
}
