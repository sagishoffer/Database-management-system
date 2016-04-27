// Sagi Shoffer
// Matan Shulman

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.FloatStringConverter;
import javafx.util.converter.IntegerStringConverter;

public class ProjectClient extends Application {
	// Connection components
	private Socket socket;
	private DataOutputStream toServer;
	private DataInputStream fromServer;
	private ObjectInputStream objFromServer;
	private ObjectOutputStream objToServer;
	// Events components
	private Button edit = new Button("Edit");
	private Button update = new Button("Update");
	private Button reset = new Button("Reset System");
	private Button submit = new Button("Submit Action");	
	private Button delRow = new Button("Delete Row");
	private ComboBox<String> addRow = new ComboBox<String>();
	private ComboBox<String> delCol = new ComboBox<String>();
	private ComboBox<String> addCol = new ComboBox<String>();
	private Button pin = new Button();
	private Button unPin = new Button();
	// Static arrays
	private String[] colS = {"ID","First_Name","Last_Name","City","Street","Zip_Code","Department","Gender","Transport","Picture"};
	private String[] colN = {"Start_Year","Credits","Average","Failures","Rank"};
	// Dynamic arrays
	private List<String> colNames = new ArrayList<String>();
	private List<String> colType = new ArrayList<String>();
	private List<String> removedCols = new ArrayList<String>();
	private List<String> addRowList = new ArrayList<String>();
	// Combo boxes
	private ComboBox<String>[] cbString = new ComboBox[colS.length]; 	// combo box for strings fields
	private ComboBox<String>[] cbNumeric = new ComboBox[colN.length]; 	// combo box for numeric fields
	private ComboBox<String>[] cbOp = new ComboBox[colN.length]; 		// combo box for operators
	private ComboBox<String>[] cbClone = new ComboBox[colN.length];		// second combo box for between operator
	// Date components
	private DatePicker date;
	private ComboBox<String> opDate;
	private DatePicker date2;
	// Table view 
	private TableView<Record> tableView = new TableView<Record>();		
	private List<Record> lst = new ArrayList<Record>();
	private Record record;
	// Utilities
	private boolean canRun = false;
	private boolean condition;
	private boolean first_time = true;
	private boolean flag = false;
	private boolean check = true;
	private String old_source;
	private boolean first = true;
	private boolean start = true;
	private List<Integer> tableIndex = new ArrayList<Integer>();
	private Label info = new Label();
	private Stage stage;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void start(Stage primaryStage) throws IOException { 
		stage = primaryStage;
		// Create string combo boxes
		for (int i = 0; i < cbString.length; i++) {
			cbString[i] = new ComboBox<String>();
			cbString[i].setPrefWidth(115);
			cbString[i].setPromptText(colS[i]);
		}
		
		// Create numeric combo boxes
		for (int i = 0; i < cbNumeric.length; i++) {
			cbNumeric[i] = new ComboBox<String>();
			cbNumeric[i].setPrefWidth(115);
			cbNumeric[i].setPromptText(colN[i]);
			cbOp[i] = new ComboBox<String>();
			cbOp[i].setPrefWidth(115);
			cbOp[i].setPromptText("Range");
			cbClone[i] = new ComboBox<String>();
			cbClone[i].setPrefWidth(115);
			cbClone[i].setPromptText(colN[i]);
		}
		
		// Create date combo boxes
		date = new DatePicker();
		opDate = new ComboBox<String>();
		date2 = new DatePicker();
		
		// Other combo boxes configuration
		delCol.setPromptText("Delete Column");
		addCol.setPromptText("Add Column");
		addRow.setPromptText("Add Row");
		date.setPromptText("Birth_Date");
		date2.setPromptText("Birth_Date");		
	
		// Grid for string combo boxes
		HBox stringBox = new HBox();
		GridPane stringGrid = new GridPane();
		int counter = 0;
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++) {
				stringGrid.add(cbString[counter], i, j);
				counter++;
			}
		stringBox.getChildren().add(stringGrid);
		stringBox.setStyle("-fx-border-color: black");
		
		// Grid for date
		GridPane dateGrid = new GridPane();
		date.setPrefWidth(115);
		dateGrid.add(date, 0, 0);
		opDate.setPrefWidth(115);
		opDate.setPromptText("Range");
		dateGrid.add(opDate, 0, 1);
		date2.setPrefWidth(115);
		dateGrid.add(date2, 0, 2);
		
		// Grid for numeric combo boxes
		GridPane numGrid = new GridPane();
		for (int i = 0; i < 5; i++) {
			numGrid.add(cbNumeric[i], i, 0);
			numGrid.add(cbOp[i], i, 1);
			numGrid.add(cbClone[i], i, 2);
		}
		HBox numBox = new HBox();
		numBox.getChildren().addAll(numGrid,dateGrid);
		numBox.setStyle("-fx-border-color: black");
		
		// Image on grid
		Image image = new Image("HR.gif");
		ImageView iv = new ImageView();
		iv.setImage(image);
		iv.setFitWidth(210);
		iv.setFitHeight(75);
		
		// Pin button
		Image pinned = new Image("pinOn.gif");
		ImageView ivPin = new ImageView();
		ivPin.setImage(pinned);
		ivPin.setFitWidth(15);
		ivPin.setFitHeight(15);
		pin.setGraphic(ivPin);
		pin.setPrefSize(20, 20);
		pin.setTooltip(new Tooltip("Unpin Menu"));
		
		// unPin button
		Image unPinned = new Image("pinOff.gif");
		ImageView ivUnPin = new ImageView();
		ivUnPin.setImage(unPinned);
		ivUnPin.setFitWidth(15);
		ivUnPin.setFitHeight(15);
		unPin.setGraphic(ivUnPin);
		unPin.setPrefSize(20, 20);
		unPin.setTooltip(new Tooltip("Pin Menu"));
				
		// Main grid to hold combo boxes
		GridPane comboGrid = new GridPane();
		comboGrid.add(stringBox, 0, 0);
		comboGrid.add(numBox, 1, 0);
		comboGrid.setPadding(new Insets(1, 1, 1, 1));
		comboGrid.setHgap(10);
		
		// right grid
		GridPane rightGrid = new GridPane();
		rightGrid.setAlignment(Pos.TOP_RIGHT);
		rightGrid.add(pin, 0, 0);
		
		// Border pane for top pane
		BorderPane topPane = new BorderPane();
		topPane.setLeft(comboGrid);
		topPane.setCenter(iv);
		topPane.setRight(rightGrid);
		
		// Closed Grid
		GridPane mainGridClose = new GridPane();
		mainGridClose.setAlignment(Pos.TOP_RIGHT);
		mainGridClose.add(unPin, 0, 0);
						
		// Binding property
		for (int i = 0; i < cbOp.length; i++)
			cbClone[i].disableProperty().bind(cbOp[i].valueProperty().isEqualTo("between").not());
		date2.disableProperty().bind(opDate.valueProperty().isEqualTo("between").not());
		
		// Table View settings
		tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		tableView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
				@Override
				public void changed(ObservableValue observable,Object oldvalue, Object newValue) {
					Record rec2 = (Record) newValue;

					if (first_time == false) {
						try {
							if (old_source.equals(rec2.getID())) {
								flag = false;
								check = false;
							}
							if (check == true && !(old_source.equals(rec2.getID()))) {
								record = (Record) newValue;
								old_source = record.getID();
								flag = true;
							}
							check = true;
						} catch (Exception e) {
							//System.out.println("Please Select a Column ");
						}
					}
					flag = true;
					
					if (first_time) {					
						first_time = false;
						record = (Record) newValue;
						old_source = rec2.getID();
					}
				}
			});
				
		// Grid to handle events buttons
		GridPane buttons = new GridPane();
		buttons.setAlignment(Pos.CENTER_LEFT);
		reset.setPrefWidth(95);
		submit.setPrefWidth(95);
		buttons.add(submit, 0, 0);
		buttons.add(reset, 1, 0);
		buttons.add(delCol, 2, 0);
		buttons.add(addCol, 3, 0);
		buttons.add(delRow, 4, 0);
		buttons.add(addRow, 5, 0);
		buttons.add(edit, 6, 0);
		buttons.add(update, 7, 0);
		info.setText("System ready");
		update.setDisable(true);
		buttons.setPrefWidth(450);
		
		// Border pane for bottom pane
		BorderPane bottomPane = new BorderPane();
		bottomPane.setCenter(buttons);
		info.setPrefWidth(350);
		bottomPane.setLeft(info);
		
		// Border pane to handle all components
		BorderPane mainPane = new BorderPane();
		mainPane.setTop(mainGridClose);
		mainPane.setCenter(tableView);
		mainPane.setBottom(bottomPane);
		
		Scene scene = new Scene(mainPane);
		primaryStage.setTitle("Client"); 
		primaryStage.getIcons().add(new Image("clientIcon.png")); 
		primaryStage.setScene(scene); 
		primaryStage.show(); 
		primaryStage.setMaximized(true);
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>(){
	    	public void handle(WindowEvent event)
	    	{
	    		try {
					if(canRun)
						socket.close();
					ProjectMenu.closeClient(primaryStage);
				} catch (IOException e) {}
	    	}
	    });
		
//---------------------------------------------- Pin Click ---------------------------------------------------//			
		pin.setOnAction(e -> {
			Platform.runLater(() -> { 
				mainPane.setTop(mainGridClose);
			});			
		});	
		
		unPin.setOnAction(e -> {	
			Platform.runLater(() -> { 
				mainPane.setTop(topPane);
			});		
		});	
		
//---------------------------------------------- Reset System ---------------------------------------------------//		
		reset.setOnAction(e -> {			
			try { 
				toServer.writeUTF("reset");
				toServer.flush();
				
				// clear all content
				lst.clear();
				colNames.clear();
				colType.clear();
				removedCols.clear();
				addRowList.clear();
				tableView.getColumns().clear();
				
				// reset all combo boxes
				for (int j = 0; j < colS.length; j++) 				
					cbString[j].setDisable(false);
				
				for (int j = 0; j < colN.length; j++) {
					cbNumeric[j].setDisable(false);
					cbOp[j].setDisable(false);
				}
				
				date.setDisable(false);
				opDate.setDisable(false);
							
				InsertData();
				tableView.setItems(FXCollections.observableList(lst));
				tableView.getSelectionModel().clearSelection();
				info.setText("Reset completed");
				
				Platform.runLater(() -> { 
					delCol.getItems().clear();
					addCol.getItems().clear();
					delCol.getItems().addAll(colNames);  	// adding updated content to delete box
					System.out.println(delCol.getItems().indexOf("ID"));
					delCol.getItems().remove(delCol.getItems().indexOf("ID"));
					addCol.getItems().addAll(removedCols);	// adding updated content to add box
					//delCol.setValue(null);
					//addCol.setValue(null);
				});
				
			} catch (Exception ex) {
				if (ex instanceof SocketException)
					disableAll();
				else {
					System.err.println("Initial exception - client");
					System.out.println(ex);
					return;
				}
			}
		});	
		
//---------------------------------------------- Submit Query ---------------------------------------------------//		
		submit.setOnAction(e -> {	
			try {
				toServer.writeUTF("submit");
				toServer.flush();
				HashMap<String,List<String>> map = new HashMap<String,List<String>>();
				
				// insert chosen string values into map
				for (int i = 0; i < colS.length; i++) {
					List<String> queryLst = new ArrayList<String>();
					queryLst.add(0, cbString[i].getValue());
					queryLst.add(1, " = ");
					queryLst.add(2, null);
					map.put(colS[i], queryLst);
				}
				
				// insert chosen numeric values into map
				for (int i = 0; i < colN.length; i++) {
					List<String> queryLst = new ArrayList<String>();
					queryLst.add(0, cbNumeric[i].getValue());
					if (cbOp[i].getValue() == null)
						queryLst.add(1, "=");
					else
						queryLst.add(1, cbOp[i].getValue());
					queryLst.add(2, cbClone[i].getValue());
					map.put(colN[i], queryLst);
				}
				
				// insert chosen date values into map
				List<String> queryLst = new ArrayList<String>();
				LocalDate dateVal = date.getValue();
				if (dateVal != null) { 
					queryLst.add(0, date.getValue().toString());
					queryLst.add(1, opDate.getValue());
					if (date2.isDisabled())
						queryLst.add(2, null);
					else
						queryLst.add(2, date.getValue().toString()); 
				} else {
					queryLst.add(0, null);
					queryLst.add(1, null);
					queryLst.add(2, null);
				}
				map.put("Birth_Date", queryLst);
				
				// send query map to server
				objToServer.writeObject(map);
				objToServer.flush();
				
				refreshData();
				
				condition= fromServer.readBoolean();
				if(condition) {
					// get all relevant info from server
					lst = new ArrayList<Record>();
					int rows = fromServer.readInt();
					for (int i = 0; i < rows; i++) {
						lst.add((Record) objFromServer.readObject());					
					}
					tableView.setItems(FXCollections.observableList(lst));
					info.setText("OK");
				}
				else {		
					info.setText("Error with query - Data refreshed");	
					refreshData();				
				}			
			} catch (Exception ex) {
				if (ex instanceof SocketException)
					disableAll();
				else {
					System.err.println("Submit exception");
					System.out.println(ex);
					return;
				}
			} finally {
				// set all combo boxes to default prompt text
				for (int i = 0; i < cbNumeric.length; i++) {
					cbNumeric[i].setValue(null);
					cbOp[i].setValue(null);
					cbClone[i].setValue(null);
				}
				for (int i = 0; i < cbString.length; i++) {
					cbString[i].setValue(null);
				}
				date.setValue(null);
				opDate.setValue(null);
				date2.setValue(null);
			}
		});	
		
//---------------------------------------------- Delete Column ---------------------------------------------------//		
		delCol.setOnAction(e -> {	
			try {
				if (delCol.getValue() != null) {
					String colRemove = delCol.getValue();
					toServer.writeUTF("delCol");
					toServer.flush();
					toServer.writeUTF(colRemove);
					
					condition = fromServer.readBoolean();
					
					if (condition) {
						int index = colNames.indexOf(colRemove);
						removedCols.clear();
						colNames.clear();
						colType.clear();
						tableView.getColumns().remove(index);
	
						// get new removed list
						int countRemove = fromServer.readInt();
						for (int i = 0; i < countRemove; i++) {
							removedCols.add(fromServer.readUTF());
						}
	
						// get new table list
						int countTable = fromServer.readInt();
						for (int i = 0; i < countTable; i++) {
							colNames.add(fromServer.readUTF());
							colType.add(fromServer.readUTF());
						}
						
						// search the removed column and disable column and combo box
						for (int i = 0; i < colS.length; i++) {
							if (colRemove.equals(colS[i])) {
								cbString[i].setDisable(true);
								cbString[i].getItems().clear();
							}
						}
						for (int i = 0; i < colN.length; i++) {
							if (colRemove.equals(colN[i])) {
								cbNumeric[i].setDisable(true);
								cbOp[i].setDisable(true);
								cbNumeric[i].getItems().clear();
							}
						}
						if (colRemove.equals("Birth_Date")) {
							date.setDisable(true);
							opDate.setDisable(true);
						}
						
						info.setText(colRemove + " column was removed successfully");
						Platform.runLater(() -> { 
							delCol.getItems().clear();
							addCol.getItems().clear();
							delCol.getItems().addAll(colNames);  	// adding updated content to delete box
							delCol.getItems().remove(delCol.getItems().indexOf("ID"));
							addCol.getItems().addAll(removedCols);	// adding updated content to add box
							delCol.setValue(null);
						});
					}
					
					else {
						info.setText(colRemove + " column has already been removed - Data is now UpToDate");
						refreshData();
					}
				}
			} catch (Exception ex) {
				if (ex instanceof SocketException)
					disableAll();
				else {
					System.err.println("Delete column exception - Client");
					System.out.println(ex);
					return;
				}
			}
		});	
		
//---------------------------------------------- Add Column ---------------------------------------------------//		
		addCol.setOnAction(e -> {
			try {
				if (addCol.getValue() != null) {
					String colAdd = addCol.getValue();
					toServer.writeUTF("addCol");
					toServer.flush();
					toServer.writeUTF(colAdd);
					toServer.flush();
					
					condition = fromServer.readBoolean();
					
					if (condition) {
						removedCols.clear();
						colNames.clear();
						colType.clear();
						
						// get new removed list
						int countRemove = fromServer.readInt();
						for (int i = 0; i < countRemove; i++) {
							removedCols.add(fromServer.readUTF());
						}
	
						// get new table list
						int countTable = fromServer.readInt();
						for (int i = 0; i < countTable; i++) {
							colNames.add(fromServer.readUTF());
							colType.add(fromServer.readUTF());
						}
					
						// search the added column and enable combo box
						for (int i = 0; i < colS.length; i++) {
							if (colAdd.equals(colS[i]))
								cbString[i].setDisable(false);
						}
						for (int i = 0; i < colN.length; i++) {
							if (colAdd.equals(colN[i])) {
								cbNumeric[i].setDisable(false);
								cbOp[i].setDisable(false);
							}
						}
						if (colAdd.equals("Birth_Date")) {
							date.setDisable(false);
							opDate.setDisable(false);
						}
						
						tableView.getColumns().clear();
						resetCol(colAdd);
						orginaizeTable();
						
						info.setText(colAdd + " column was added successfully");					
						Platform.runLater(() -> { 
							delCol.getItems().clear();
							addCol.getItems().clear();
							delCol.getItems().addAll(colNames);  	// adding updated content to delete box
							delCol.getItems().remove(delCol.getItems().indexOf("ID"));
							addCol.getItems().addAll(removedCols);	// adding updated content to add box
							addCol.setValue(null);
						});
					}
					else {
						info.setText(colAdd + " column has already been added - Data is now UpToDate");
						refreshData();
					}
				}
			} catch (Exception ex) {
				if (ex instanceof SocketException)
					disableAll();
				else {
					System.err.println("Add column exception - Client");
					System.out.println(ex);
					return;
				}
			}
		});
		
//---------------------------------------------- Add Row ---------------------------------------------------//		
		addRow.setOnAction(e -> {	
			try {
				if (addRow.getValue() != null) {
					String id = addRow.getValue();
					toServer.writeUTF("addRow");
					toServer.flush();
					toServer.writeUTF(id);
					toServer.flush();
					
					condition = fromServer.readBoolean();
					
					if (condition) {
						Record rec = (Record)objFromServer.readObject();		// get new record from server
						lst.add(rec);
						tableView.setItems(FXCollections.observableList(lst));	// insert new record to table view
						addRowList.remove(id);
						getCBContent();	
						
						info.setText("Student: "+id+" was added successfully");
						Platform.runLater(() -> { 
							addRow.getItems().clear();
							addRow.getItems().addAll(addRowList);						
						});
					}
					else {
						info.setText("Student: "+id+" has already been added - Data is now UpToDate");
						refreshData();
					}
				}
			} catch (Exception ex) {
				if (ex instanceof SocketException)
					disableAll();
				else {
					System.err.println("Add row exception - Client");
					System.out.println(ex);
					return;
				}
			} finally {
				addRow.setValue(null);
			}
		});
		
//---------------------------------------------- Delete Row ---------------------------------------------------//		
		delRow.setOnAction(e -> { 
			try {
				if (flag != false) { 
					String recordId = record.getID();
					toServer.writeUTF("delRow");
					toServer.flush();
					toServer.writeUTF(recordId);
					toServer.flush();
					
					flag = false;
					condition = fromServer.readBoolean();
					
					if (condition) {
						for (int i = 0; i < lst.size(); i++) 
							if (record.getID() == lst.get(i).getID())
								lst.remove(record);
						
						tableView.setItems(FXCollections.observableList(lst));
						tableView.getSelectionModel().clearSelection();
						addRowList.add(recordId);
						getCBContent();	
						
						info.setText("Student: "+recordId+" was removed successfully");
						Platform.runLater(() -> { 
							addRow.getItems().clear();
							addRow.getItems().addAll(addRowList);	
						});
					}				
					else {
						info.setText("Student: "+recordId+" has already been removed - Data is now UpToDate");
						refreshData();
					}		
				}
				
				else {	// no row selected
					info.setText("Please press a row");	
				}			
			} catch (Exception ex) {
				if (ex instanceof SocketException)
					disableAll();
				else {
					System.err.println("Delete row exception - Client");
					System.out.println(ex);
					return;
				}
			}	
		});
		
//---------------------------------------------- Update Row ---------------------------------------------------//				
		update.setOnAction(e -> {	
			try {
				List<Integer> indexes = new ArrayList<Integer>();
				toServer.writeUTF("updateRow");
				toServer.flush();
				
				// send indexes of changed rows with matching records
				toServer.writeInt(tableIndex.size());	
				for (int i = 0; i < tableIndex.size(); i++) {
					objToServer.writeObject(lst.get(tableIndex.get(i)));
					condition = fromServer.readBoolean();
					if (condition == false)
						indexes.add(tableIndex.get(i)+1);
				}	
				
				List<Record> tempLst = new ArrayList<Record>(lst);
				
				String massage;
				if (indexes.size() > 0) {
					if (indexes.size() > 1)
						massage = "Rows ";
					else
						massage = "Row ";
					
					info.setText(massage + "number: " + indexes + " does not exist");
				}
				else {
					info.setText("Update was successful");
				}
				refreshData();
				tableView.setItems(FXCollections.observableList(tempLst));
				
			} catch (Exception ex) {
				if (ex instanceof SocketException)
					disableAll();
				else {
					System.err.println("Update row exception - Client");
					System.out.println(ex);
					return;
				}
			}
			
			tableIndex.clear();
			// enable all buttons after update	
			edit.setDisable(false);
			reset.setDisable(false);
			addCol.setDisable(false);
			delCol.setDisable(false);
			addRow.setDisable(false);
			delRow.setDisable(false);
			submit.setDisable(false);
			update.setDisable(true);
			
			tableView.setEditable(false);		
		});	
		
//---------------------------------------------- Edit Mode ---------------------------------------------------//
		edit.setOnAction(e -> {		
			tableView.setEditable(true);
			
			// disable all buttons on edit mode
			update.setDisable(false);
			edit.setDisable(true);
			reset.setDisable(true);
			addCol.setDisable(true);
			delCol.setDisable(true);
			addRow.setDisable(true);
			delRow.setDisable(true);
			submit.setDisable(true);		
		});
					
//--------------------------------- Create a socket and connect to server ---------------------------------------//		
		try {
			socket = new Socket("localhost", 8000);
			canRun = true;
			toServer = new DataOutputStream(socket.getOutputStream());
			fromServer = new DataInputStream(socket.getInputStream());
			objToServer = new ObjectOutputStream(socket.getOutputStream());
			objFromServer = new ObjectInputStream(socket.getInputStream());
			InsertData(); // Insert initial data into combo boxes
		} catch (IOException ex) {
			disableAll();
			canRun = false;
		}
	}

//----------------------------------------- Referenced Methods----------------------------------------------------//	
	
	/** Get data form server to combo boxes*/
	public void InsertData() throws IOException {				
		// save table columns from server
		int count = fromServer.readInt();
		for (int i = 0; i < count; i++) {
			colNames.add(fromServer.readUTF());
			colType.add(fromServer.readUTF());
		}
		
		// save removed columns from server
		int removed = fromServer.readInt();
		for (int i = 0; i < removed; i++) {
			removedCols.add(fromServer.readUTF());
		}
		
		getCBContent(); 			
		
		// add content to addRow combo box clear to insert update data
		addRow.getItems().clear();				
		int count2 = fromServer.readInt();		
		for (int i = 0; i < count2; i++) 
			addRowList.add(fromServer.readUTF());
		addRow.getItems().addAll(addRowList);
		
		String temp = colNames.get(0);
		colNames.remove(0);
		delCol.getItems().addAll(colNames);		// columns that can be delete
		colNames.add(0, temp);
		addCol.getItems().addAll(removedCols);	// columns that can be add
		
		// set disable on removed items ///
		for (int i = 0; i < removedCols.size(); i++) {
			for (int j = 0; j < colS.length; j++) {
				if (removedCols.get(i).equals(colS[j]))
					cbString[j].setDisable(true);
			}
			for (int j = 0; j < colN.length; j++) {
				if (removedCols.get(i).equals(colN[j]))
					cbNumeric[j].setDisable(true);
			}
		}	
		
		orginaizeTable();
		
		if (start) {
			String[] op = {">","<","=","between"};
			ObservableList<String> operators = FXCollections.observableArrayList(op);
			// insert operators to opBC
			for (int i = 0; i < cbOp.length; i++) {
				cbOp[i].getItems().addAll(operators);
			}				
			opDate.getItems().addAll(operators); 	// manual insert to date operators				
			start = false;
		}
	}
	
	/** Insert updated content to combo boxes */
	public void getCBContent() {
		ObservableList<String> dataInsert = FXCollections.observableArrayList();
				
		try {
			int count = fromServer.readInt();
			// add content to combo boxes by type
			for (int i = 0; i < count; i++) {
				String cName = fromServer.readUTF();
				if (!cName.equals("Picture")) { 			// no need to get images to combo box
					String type = fromServer.readUTF();
					int numOfRec = fromServer.readInt();

					for (int j = 0; j < numOfRec; j++) {
						String s = fromServer.readUTF();
						if (!s.equals("e"))
							dataInsert.add(s);
						else
							dataInsert.add("");
					}

					if (type.equals("FLOAT") || type.equals("DOUBLE") || type.equals("INT")) {
						int index = getConstIndex(cName, "n");
						cbNumeric[index].getItems().clear();
						cbClone[index].getItems().clear();
						cbNumeric[index].getItems().addAll(dataInsert);
						cbClone[index].getItems().addAll(dataInsert);
					} else if (!type.equals("DATE")) { 		// no need to get dates to combo box
						int index = getConstIndex(cName, "s");
						cbString[index].getItems().clear();
						cbString[index].getItems().addAll(dataInsert);
					}
					dataInsert.clear();
				}
			}
		} catch (IOException ex) {
			System.out.println(ex);
		}		
	}
	
	/** Attach value property to column */
	public void orginaizeTable() {		
		for (int i = 0; i < colNames.size(); i++) {
			String fieldName = colNames.get(i);

			switch (fieldName) {							
				case "ID": {
					TableColumn<Record, String> col = new TableColumn<Record, String>("ID");
					col.setCellValueFactory(new PropertyValueFactory<Record, String>("ID"));
					col.setPrefWidth(100);
					tableView.getColumns().add(col);
				}
					break;
			
				case "First_Name": {
					TableColumn<Record, String> col = new TableColumn<Record, String>("First_Name");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.forTableColumn());
					col.setCellValueFactory(new PropertyValueFactory<Record, String>("First_Name"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, String>>() {
						@Override
						public void handle(CellEditEvent<Record, String> t) {
							String input = t.getNewValue();
							String regex = "[a-zA-Z]+";
							Pattern pattern = Pattern.compile(regex);
							Matcher matcher = pattern.matcher(input);
							boolean isMatched = matcher.matches();
		
							if (isMatched) {
								info.setText("");
								((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setfName(t.getNewValue());
								if (!tableIndex.contains(t.getTablePosition().getRow()))
									tableIndex.add(t.getTablePosition().getRow());
							} else {
								info.setText("Input is invalid - please try again!");
								tableView.getItems().set(t.getTablePosition().getRow(),lst.get(t.getTablePosition().getRow()));
							}						
						}
					});
					tableView.getColumns().add(col);	
				}
					break;
				
				case "Last_Name": {
					TableColumn<Record, String> col = new TableColumn<Record, String>("Last_Name");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.forTableColumn());
					col.setCellValueFactory(new PropertyValueFactory<Record, String>("Last_Name"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, String>>() {
						@Override
						public void handle(CellEditEvent<Record, String> t) {
							String input = t.getNewValue();
							String regex = "[a-zA-Z]+";
							Pattern pattern = Pattern.compile(regex);
							Matcher matcher = pattern.matcher(input);
							boolean isMatched = matcher.matches();

							if (isMatched) {
								info.setText("");
								((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setlName(t.getNewValue());
								if (!tableIndex.contains(t.getTablePosition().getRow()))
									tableIndex.add(t.getTablePosition().getRow());
							} else {
								info.setText("Input is invalid - please try again!");
								tableView.getItems().set(t.getTablePosition().getRow(),lst.get(t.getTablePosition().getRow()));
							}
						}
					});
					tableView.getColumns().add(col);
				}
					break;
					
				case "City": {
					TableColumn<Record, String> col = new TableColumn<Record, String>("City");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.forTableColumn());
					col.setCellValueFactory(new PropertyValueFactory<Record, String>("City"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, String>>() {
						@Override
						public void handle(CellEditEvent<Record, String> t) {
							String input = t.getNewValue();
							String regex = "[a-zA-Z-]+";
							Pattern pattern = Pattern.compile(regex);
							Matcher matcher = pattern.matcher(input);
							boolean isMatched = matcher.matches();
	
							if (isMatched) {
								info.setText("");
								((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setCity(t.getNewValue());
								if (!tableIndex.contains(t.getTablePosition().getRow()))
									tableIndex.add(t.getTablePosition().getRow());
							} else {
								info.setText("Input is invalid - please try again!");
								tableView.getItems().set(t.getTablePosition().getRow(),lst.get(t.getTablePosition().getRow()));
							}
						}
					});
					tableView.getColumns().add(col);
				}
					break;
				
				case "Street": {
					TableColumn<Record, String> col = new TableColumn<Record, String>("Street");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.forTableColumn());
					col.setCellValueFactory(new PropertyValueFactory<Record, String>("Street"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, String>>() {
						@Override
						public void handle(CellEditEvent<Record, String> t) {
							((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setStreet(t.getNewValue());
							if (!tableIndex.contains(t.getTablePosition().getRow()))
								tableIndex.add(t.getTablePosition().getRow());						
						}
					});
					tableView.getColumns().add(col);
				}
					break;
						
				case "Zip_Code": {
					TableColumn<Record, String> col = new TableColumn<Record, String>("Zip_Code");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.forTableColumn());
					col.setCellValueFactory(new PropertyValueFactory<Record, String>("Zip_Code"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, String>>() {
						@Override
						public void handle(CellEditEvent<Record, String> t) {
							((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setZip(t.getNewValue());
							if (!tableIndex.contains(t.getTablePosition().getRow()))
								tableIndex.add(t.getTablePosition().getRow());
						}
					});
					tableView.getColumns().add(col);
				}
					break;
					
				case "Birth_Date": {
					TableColumn<Record, String> col = new TableColumn<Record, String>("Birth_Date");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.forTableColumn());
					col.setCellValueFactory(new PropertyValueFactory<Record, String>("Birth_Date"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, String>>() {
						@Override
						public void handle(CellEditEvent<Record, String> t) {
						String input = t.getNewValue();
						if (input.matches("\\d{4}-[01]\\d-[0-3]\\d")) {
							info.setText("");
							((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setBd(t.getNewValue());
							if (!tableIndex.contains(t.getTablePosition().getRow()))
								tableIndex.add(t.getTablePosition().getRow());
						} else {
							info.setText("Input is invalid, pattern is: YYYY-MM-DD");
							tableView.getItems().set(t.getTablePosition().getRow(),lst.get(t.getTablePosition().getRow()));
						}
					}
				});
					tableView.getColumns().add(col);
				}
					break;
					
				case "Start_Year": {
					TableColumn<Record, Integer> col = new TableColumn<Record, Integer>("Start_Year");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
					Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {						
						info.setText("Inccorect format number - please try again");
					});
					col.setCellValueFactory(new PropertyValueFactory<Record, Integer>("Start_Year"));	
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, Integer>>() {
						@Override
						public void handle(CellEditEvent<Record, Integer> t) {
							String input = t.getNewValue().toString();
							String regex = "^\\d{4}";
							Pattern pattern = Pattern.compile(regex);
							Matcher matcher = pattern.matcher(input);
							boolean isMatched = matcher.matches();
								
							if (isMatched) {
								info.setText("");
								((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setStart(t.getNewValue());
								if (!tableIndex.contains(t.getTablePosition().getRow()))
									tableIndex.add(t.getTablePosition().getRow());
							} else {
								info.setText("Input is invalid, pattern is: YYYY");
								tableView.getItems().set(t.getTablePosition().getRow(),lst.get(t.getTablePosition().getRow()));
							}						
						}
					});
					tableView.getColumns().add(col);
				}
					break;
					
				case "Department": {
					TableColumn<Record, String> col = new TableColumn<Record, String>("Department");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.forTableColumn());
					col.setCellValueFactory(new PropertyValueFactory<Record, String>("Department"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, String>>() {
						@Override
						public void handle(CellEditEvent<Record, String> t) {
							String input = t.getNewValue();
							String regex = "[a-zA-Z]+";
							Pattern pattern = Pattern.compile(regex);
							Matcher matcher = pattern.matcher(input);
							boolean isMatched = matcher.matches();
	
							if (isMatched) {
								info.setText("");
								((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setDept(t.getNewValue());
								if (!tableIndex.contains(t.getTablePosition().getRow()))
									tableIndex.add(t.getTablePosition().getRow());
							} else {
								info.setText("Input is invalid - please try again!");
								tableView.getItems().set(t.getTablePosition().getRow(),lst.get(t.getTablePosition().getRow()));
							}
						}
					});
					tableView.getColumns().add(col);
				}
					break;
					
				case "Credits": {
					TableColumn<Record, Double> col = new TableColumn<Record, Double>("Credits");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.<Record, Double> forTableColumn(new DoubleStringConverter()));
					Thread.currentThread().setUncaughtExceptionHandler((thread, NumberFormatException) -> {						
						info.setText("Inccorect format number - please try again");
					});
					col.setCellValueFactory(new PropertyValueFactory<Record, Double>("Credits"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, Double>>() {
						@Override
						public void handle(CellEditEvent<Record, Double> t) {
							String input = t.getNewValue().toString();
							String regex = "\\d{1,3}\\.[05]";
							Pattern pattern = Pattern.compile(regex);
							Matcher matcher = pattern.matcher(input);
							boolean isMatched = matcher.matches();
							
							if (isMatched) {
								info.setText("");
								((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setCredit(t.getNewValue());
								if (!tableIndex.contains(t.getTablePosition().getRow()))
									tableIndex.add(t.getTablePosition().getRow());
							} else {
								info.setText("Input is invalid - pattern is: XXX.0 or XXX.5");
								tableView.getItems().set(t.getTablePosition().getRow(),lst.get(t.getTablePosition().getRow()));
							}	
						}
					});
					tableView.getColumns().add(col);
				}
					break;
					
				case "Average": {
					TableColumn<Record, Float> col = new TableColumn<Record, Float>("Average");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.<Record, Float> forTableColumn(new FloatStringConverter()));
					Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {						
						info.setText("Inccorect format number - please try again");
					});
					col.setCellValueFactory(new PropertyValueFactory<Record, Float>("Average"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, Float>>() {
						@Override
						public void handle(CellEditEvent<Record, Float> t) {
							String input = t.getNewValue().toString();
							String regex = "(\\d{1,2}(?!\\d)|100)\\.[0-9]";
							Pattern pattern = Pattern.compile(regex);
							Matcher matcher = pattern.matcher(input);
							boolean isMatched = matcher.matches();
							
							if (isMatched) {
								info.setText("");
								((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setAvg(t.getNewValue());
								if (!tableIndex.contains(t.getTablePosition().getRow()))
									tableIndex.add(t.getTablePosition().getRow());
							} else {
								info.setText("Input is invalid - pattern is: XX.X");
								tableView.getItems().set(t.getTablePosition().getRow(),lst.get(t.getTablePosition().getRow()));
							}
						}
					});
					tableView.getColumns().add(col);
				}
					break;
					
				case "Failures": {
					TableColumn<Record, Integer> col = new TableColumn<Record, Integer>("Failures");
					col.setPrefWidth(80);
					col.setCellFactory(TextFieldTableCell.<Record, Integer> forTableColumn(new IntegerStringConverter()));
					Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {						
						info.setText("Inccorect format number - please try again");
					});
					col.setCellValueFactory(new PropertyValueFactory<Record, Integer>("Failures"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, Integer>>() {
						@Override
						public void handle(CellEditEvent<Record, Integer> t) {
							String input = t.getNewValue().toString();
							String regex = "\\d{1,2}";
							Pattern pattern = Pattern.compile(regex);
							Matcher matcher = pattern.matcher(input);
							boolean isMatched = matcher.matches();
							
							if (isMatched) {
								info.setText("");
								((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setFails(t.getNewValue());
								if (!tableIndex.contains(t.getTablePosition().getRow()))
									tableIndex.add(t.getTablePosition().getRow());
							} else {
								info.setText("Input is invalid - please try again!");
								tableView.getItems().set(t.getTablePosition().getRow(),lst.get(t.getTablePosition().getRow()));
							}				
						}
					});
					tableView.getColumns().add(col);
				}
					break;
					
				case "Rank": {
					TableColumn<Record, Integer> col = new TableColumn<Record, Integer>("Rank");
					col.setPrefWidth(80);
					col.setCellValueFactory(new PropertyValueFactory<Record, Integer>("Rank"));
					col.setCellFactory(new Callback<TableColumn<Record, Integer>, TableCell<Record, Integer>>() {
						public TableCell<Record, Integer> call(TableColumn<Record, Integer> p) {
							TableCell<Record, Integer> cell = new TableCell<Record, Integer>() {
								@Override
								public void updateItem(Integer item, boolean empty) {
									super.updateItem(item, empty);
									if (!empty) {
										if (item != 0)
											setText(""+item);
										else {
											setText("Not Rated");
										}
									}
								}
							};
							return cell;
						}
					});
					tableView.getColumns().add(col);
				}
					break;
					
				case "Picture": {			
					TableColumn<Record, String> col = new TableColumn<Record, String>("Picture");
					col.setPrefWidth(100);
					col.setCellValueFactory(new PropertyValueFactory<Record, String>("Picture"));
					col.setSortable(false);
					col.setOnEditStart(new EventHandler<CellEditEvent<Record, String>>() {
						@Override
						public void handle(CellEditEvent<Record, String> t) {						
							String workingDir = System.getProperty("user.dir");
							workingDir += "\\src\\images";
							File file = new File(workingDir);
							FileChooser fc = new FileChooser();
							fc.setInitialDirectory(file);
							File f = fc.showOpenDialog(stage);
							if (f != null) {
								String fileType;
								int lastIndexOf = f.getName().lastIndexOf(".");
								if (lastIndexOf == -1) 
									fileType = "";
								else
									fileType = f.getName().substring(lastIndexOf).toLowerCase();
													
								if(!fileType.equals(".jpg") && !fileType.equals(".png") && !fileType.equals(".gif")) {
									info.setText("File is not a picture");
									tableView.getSelectionModel().clearSelection();
								}
								else {
									String str = f.getAbsolutePath();
									str = str.replace("\\", "/");
									System.out.println("String converted: = " + str);
									System.out.println("f.getAPath = " + f.getAbsolutePath());
									String imageUrl = "file:///"+str;
									((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setPic(imageUrl);
									if (!tableIndex.contains(t.getTablePosition().getRow()))
										tableIndex.add(t.getTablePosition().getRow());
									final List items = tableView.getItems();
									if(items == null || items.size() == 0) 
										return;
									Record item = tableView.getItems().get(t.getTablePosition().getRow());
									items.remove(t.getTablePosition().getRow());
									tableView.getSelectionModel().clearSelection();
									info.setText("Picture accepted");
									Platform.runLater(new Runnable() { 
										@Override
										public void run() {
											items.add(t.getTablePosition().getRow(), item);
										}
									});
								}
							}
							else {
								info.setText("No picture selected");
							}
						}
					});					
					col.setCellFactory(new Callback<TableColumn<Record, String>, TableCell<Record, String>>() {
						public TableCell<Record, String> call(TableColumn<Record, String> parametr) {
							TableCell<Record, String> cell = new TableCell<Record, String>() {
								public void updateItem(String item, boolean empty) {
									super.updateItem(item, empty);
									ImageView imageView = null;
									if (item != null) {
										Image img = new Image(item);
										imageView = new ImageView(img);
										imageView.setFitHeight(70);
										imageView.setFitWidth(80);
									}
									setGraphic(imageView);
								}
							};
							update.addEventHandler(ActionEvent.ACTION, e-> cell.cancelEdit());
							return cell;
						}
					});
					tableView.getColumns().add(col);
				}
					break;
					
				case "Gender": {
					TableColumn<Record, String> col = new TableColumn<Record, String>("Gender");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.forTableColumn());
					col.setCellValueFactory(new PropertyValueFactory<Record, String>("Gender"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, String>>() {
						@Override
						public void handle(CellEditEvent<Record, String> t) {
							String input = t.getNewValue();
							String regex = "[MF]";
							Pattern pattern = Pattern.compile(regex);
							Matcher matcher = pattern.matcher(input);
							boolean isMatched = matcher.matches();
	
							if (isMatched) {
								info.setText("");
								((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setGen(t.getNewValue());
								if (!tableIndex.contains(t.getTablePosition().getRow()))
									tableIndex.add(t.getTablePosition().getRow());
							} else {
								info.setText("Input is invalid - Input should be: M/F");
								tableView.getItems().set(t.getTablePosition().getRow(),lst.get(t.getTablePosition().getRow()));
							}
						}
					});
					tableView.getColumns().add(col);
				}
					break;
					
				case "Transport": {
					TableColumn<Record, String> col = new TableColumn<Record, String>("Transport");
					col.setPrefWidth(100);
					col.setCellFactory(TextFieldTableCell.forTableColumn());
					col.setCellValueFactory(new PropertyValueFactory<Record, String>("Transport"));
					col.setOnEditCommit(new EventHandler<CellEditEvent<Record, String>>() {
						@Override
						public void handle(CellEditEvent<Record, String> t) {
							String input = t.getNewValue();
							String regex = "[YN]";
							Pattern pattern = Pattern.compile(regex);
							Matcher matcher = pattern.matcher(input);
							boolean isMatched = matcher.matches();
	
							if (isMatched) {
								info.setText("");
								((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setTrans(t.getNewValue());
								if (!tableIndex.contains(t.getTablePosition().getRow()))
									tableIndex.add(t.getTablePosition().getRow());
							} else {
								info.setText("Input is invalid - Input should be: Y/N");
								tableView.getItems().set(t.getTablePosition().getRow(),lst.get(t.getTablePosition().getRow()));
							}
						}
					});
					tableView.getColumns().add(col);
				}
					break;											
			}			
		}
	}				
	
	/** Find index of constant array by type */
	public int getConstIndex(String cName, String mode) {
		int index = 0;
		
		if (mode.equals("s")) {
			for (int i = 0; i < colS.length; i++) {
				if (cName.equals(colS[i])) {
					index = i;
					break;
				}
			}	
		}
		else {
			for (int i = 0; i < colN.length; i++) {
				if (cName.equals(colN[i])) {
					index = i;
					break;
				}
			}			
		}
		
		return index;
	}
		
	/** Refresh Data throws IOException throws ClassNotFoundException */
	public void refreshData() throws IOException, ClassNotFoundException {
		// Retrieve columns
		colNames.clear();
		colType.clear();
		int cols = fromServer.readInt();
		for (int i = 0; i < cols; i++) {
			colNames.add(fromServer.readUTF());
			colType.add(fromServer.readUTF());
		}
		
		// Retrieve deleted columns
		removedCols.clear();
		int remC = fromServer.readInt();
		for (int i = 0; i < remC; i++) {
			removedCols.add(fromServer.readUTF());
		}	
	
		// insert new data to combo box panel
		getCBContent();		
		
		// get all records from DB
		lst.clear();
		int rec = fromServer.readInt();
		for (int i = 0; i < rec; i++){
			lst.add((Record)objFromServer.readObject());
		}
			
		// get all id from NewStudent
		addRowList.clear();			
		int idNum = fromServer.readInt();
		for (int i = 0; i < idNum; i++){
			addRowList.add(fromServer.readUTF());
		}
						
		// get new data for add column & delete column
		Platform.runLater(() -> { 
			addRow.getItems().clear();
			addRow.getItems().addAll(addRowList);
			addCol.getItems().clear();
			delCol.getItems().clear();		
			addCol.getItems().addAll(removedCols);
			delCol.getItems().addAll(colNames);	
			delCol.getItems().remove(delCol.getItems().indexOf("ID"));
			addCol.setValue(null);
			delCol.setValue(null);
		});
				
		// arrange table view
		tableView.getColumns().clear();	
		orginaizeTable();	
		tableView.setItems(FXCollections.observableList(lst));	
		
		// reset all combo boxes
		for (int j = 0; j < colS.length; j++) 				
			cbString[j].setDisable(false);
		
		for (int j = 0; j < colN.length; j++) {
			cbNumeric[j].setDisable(false);
			cbOp[j].setDisable(false);
		}		
		date.setDisable(false);
		opDate.setDisable(false);
		
		// set disable on removed items 
		for (int i = 0; i < removedCols.size(); i++) {
			for (int j = 0; j < colS.length; j++) {
				if (removedCols.get(i).equals(colS[j]))
					cbString[j].setDisable(true);
			}
			for (int j = 0; j < colN.length; j++) {
				if (removedCols.get(i).equals(colN[j])) {
					cbNumeric[j].setDisable(true);
					cbOp[j].setDisable(true);
				}
			}
			if (removedCols.get(i).equals("Birth_Date")) {
				date.setDisable(true);
				opDate.setDisable(true);
			}
		}	
	}

	/** Reset the view in the Table*/
	public void resetCol(String colAdd) {
		if (lst != null) {
			for (int i = 0; i < lst.size(); i++) {
				if (colAdd.equals("First_Name"))
					((Record) lst.get(i)).setfName(null);

				if (colAdd.equals("Birth_Date"))
					((Record) lst.get(i)).setBd("yyyy-mm-dd");

				if (colAdd.equals("Last_Name"))
					((Record) lst.get(i)).setlName(null);

				if (colAdd.equals("Street"))
					((Record) lst.get(i)).setStreet(null);

				if (colAdd.equals("City"))
					((Record) lst.get(i)).setCity(null);

				if (colAdd.equals("Zip_Code"))
					((Record) lst.get(i)).setZip(null);

				if (colAdd.equals("Department"))
					((Record) lst.get(i)).setDept(null);

				if (colAdd.equals("Gender"))
					((Record) lst.get(i)).setGen(null);

				if (colAdd.equals("Transport"))
					((Record) lst.get(i)).setTrans(null);

				if (colAdd.equals("Picture"))
					((Record) lst.get(i)).setPic(null);

				if (colAdd.equals("Start_Year"))
					((Record) lst.get(i)).setStart(0);

				if (colAdd.equals("Credits"))
					((Record) lst.get(i)).setCredit(0);

				if (colAdd.equals("Average"))
					((Record) lst.get(i)).setAvg(0);

				if (colAdd.equals("Failures"))
					((Record) lst.get(i)).setFails(0);

				if (colAdd.equals("Rank"))
					((Record) lst.get(i)).setRank(0);

				if (colAdd.equals("Rank"))
					((Record) lst.get(i)).setRank(0);
			}
		}
	}
	
	/** Disable program */
	public void disableAll() {
		info.setText("No Server!");
		update.setDisable(true);
		edit.setDisable(true);
		reset.setDisable(true);
		addCol.setDisable(true);
		delCol.setDisable(true);
		addRow.setDisable(true);
		delRow.setDisable(true);
		submit.setDisable(true);	
		
		for (int i = 0; i < cbString.length; i++) {
			cbString[i].setDisable(true);
		}
		for (int i = 0; i < cbNumeric.length; i++) {
			cbNumeric[i].setDisable(true);
			cbOp[i].setDisable(true);
		}
		date.setDisable(true);
		opDate.setDisable(true);
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
