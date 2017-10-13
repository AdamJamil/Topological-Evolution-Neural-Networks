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

        for (int i = 0; i < 100; i++)
        {
            final int temp = i;
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    Trainer trainer = new Trainer(temp, canvas, new String[]{"x", "v", "theta", "ω"}, new String[]{"force"});
                }
            }).start();
        }
    }


    public static void main(String[] args)
    {
        launch(args);
    }
}
