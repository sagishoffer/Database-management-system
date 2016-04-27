// Sagi Shoffer
// Matan Shulman


import java.util.ArrayList;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ProjectMenu extends Application {
	private Button server;
	private Button client;
	private static ArrayList<Stage> listOfClients = new ArrayList<Stage>();
	private static Stage serverRuns = new Stage();
	private static SimpleBooleanProperty canRun = new SimpleBooleanProperty(false);

	@Override
	public void start(Stage primeryStage) throws Exception {		
		Image imageServer = new Image("serverIcon2.png");
		ImageView ivServer = new ImageView();
		ivServer.setImage(imageServer);
		ivServer.setFitWidth(150);
		ivServer.setFitHeight(130);
				
		Image imageClient = new Image("clientIcon.png");
		ImageView ivClient = new ImageView();
		ivClient.setImage(imageClient);
		ivClient.setFitWidth(150);
		ivClient.setFitHeight(120);
		
		server = new Button("Load Server",ivServer);
		client = new Button("Open Client",ivClient);
		server.setPrefSize(300, 150);
		client.setPrefSize(300, 150);
		
		VBox vb = new VBox(20);
		vb.setAlignment(Pos.CENTER);
		vb.getChildren().addAll(server, client);
		Scene scene = new Scene(vb, 400, 400);

		server.setOnAction(e -> {
			ProjectServer server = new ProjectServer();
			try {
				Stage serverStage = new Stage();
				server.start(serverStage);
				serverRuns = serverStage;
				canRun.set(true);
			} catch (Exception ex) {
				ex.printStackTrace();
			}		
		});
		
		client.setOnAction(e -> {
			ProjectClient client = new ProjectClient();
			try {
				Stage clientStage = new Stage();
				listOfClients.add(clientStage);
				client.start(clientStage);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
		
		server.disableProperty().bind(canRun);
		client.disableProperty().bind(canRun.not());

		primeryStage.setTitle("Menu"); 
		primeryStage.getIcons().add(new Image("menuIcon.png")); 
		primeryStage.setScene(scene); 				   
		primeryStage.setResizable(false);
		primeryStage.show(); 
		
		primeryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent arg0) {
				Platform.exit();
				System.exit(0);
			}
		});
	}
	
	public static void closeServer(Stage primaryStage) {
		serverRuns.close();
	}
	
	public static void closeClient(Stage clientStage) {
		listOfClients.remove(clientStage);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
