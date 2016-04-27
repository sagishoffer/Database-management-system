// Sagi Shoffer
// Matan Shulman

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ProjectServer extends Application {
	private TextArea ta = new TextArea();
	private ServerSocket serverSocket;
	private Socket socket;
	private int clientNo = 0;
	private Statement statement;
	private List<String> colNames = new ArrayList<String>();
	private List<String> colType = new ArrayList<String>();
	private List<String> removedCols = new ArrayList<String>();
	private Connection connection;
	private HashMap<String,String> typeMap = new HashMap<String,String>();
	// all possible columns in data base
	private String[] namesForMap = {"ID","First_Name","Last_Name","City","Street","Zip_Code","Birth_Date","Start_Year","Department","Credits","Average","Failures","Rank","Gender","Transport","Picture"};
	private String[] typeForMap = {"CHAR(9)","VARCHAR(25)","VARCHAR(25)","VARCHAR(15)","VARCHAR(25)","CHAR(5)","DATE","INT","VARCHAR(25)","DOUBLE(3,1)","FLOAT","INT(2)","INT(2)","VARCHAR(25)","VARCHAR(25)","VARCHAR(255)"};
	// Utilities
	private boolean rankZero = false;
	private boolean condition;
	
	public ProjectServer() {
		connectToDB();
		createStatMap();
	}
	
	 @Override
	public void start(Stage primaryStage) { 
		Scene scene = new Scene(new ScrollPane(ta), 480, 185);
		primaryStage.setTitle("Server");
		primaryStage.getIcons().add(new Image("serverIcon.png")); 
		primaryStage.setScene(scene); 
		primaryStage.show(); 
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent event) {
				try {
					serverSocket.close();
					Platform.exit();
					System.exit(0);
					ProjectMenu.closeServer(primaryStage);
				} catch (Exception ex){}
			}
		});
	
		new Thread(() -> {
			try {
				serverSocket = new ServerSocket(8000);
				Platform.runLater(() -> {
					ta.appendText("Server started at " + new Date() + '\n');
					ta.appendText("Server is connected to DataBase" + '\n');
				});
				
				// Client communications management
				while (true) {
					socket = serverSocket.accept();
					clientNo++;
					Platform.runLater(() -> { 
						// Display the client number
						ta.appendText("Starting thread for client " + clientNo + " at " + new Date() + '\n');
						// Find the client's host name, and IP address
						InetAddress inetAddress = socket.getInetAddress();
						ta.appendText("Client " + clientNo + "'s host name is "+ inetAddress.getHostName() + "\n");
						ta.appendText("Client " + clientNo + "'s IP Address is " + inetAddress.getHostAddress() + "\n");
					});
					// Create and start a new thread for the connection
				new Thread(new HandleAClient(socket)).start();
			}
		} catch (IOException ex) {
			//System.err.println(ex);
		}
	}).start();
	}

	// Class to handle new connection
	class HandleAClient implements Runnable  {
		//private Socket socket; 
		private DataInputStream fromClient;
		private DataOutputStream toClient;
		private ObjectInputStream objFromClient;
		private ObjectOutputStream objToClient;

		public HandleAClient(Socket socket) throws IOException {
			//this.socket = socket;
			this.fromClient = new DataInputStream(socket.getInputStream());
			this.toClient = new DataOutputStream(socket.getOutputStream());
			this.objFromClient = new ObjectInputStream(socket.getInputStream());
			this.objToClient = new ObjectOutputStream(socket.getOutputStream());			
		}

		/** Run a thread */
		public void run() {
			try { 			
				retrieveData(); 	// Retrieve data to client 	
				
				// Continuously serve the client
				while (true) { 
					String s = fromClient.readUTF();
					switch (s) {
						case "reset": 
							resetSystem();
							break;
						case "submit":
							submit();
							break;
						case "delCol":
							delCol();
							break;
						case "addCol":						
							addCol();
							break;
						case "delRow":
							delRow();
							break;
						case "addRow":
							addRow();
							break;
						case "updateRow":
							updateRow();
							break;
						default: 
			                System.out.println("defult");
			                break;
					}
					
				}
			} catch (IOException | SQLException | ClassNotFoundException ex) {
			}
		}	
		
		/** Retrieve all data at new connection */
		public void retrieveData() throws SQLException, IOException {
			List<String> lst = new ArrayList<String>();
			String query = "select * from Student";
			ResultSet resultSet = statement.executeQuery(query);
			ResultSetMetaData rsMetaData = resultSet.getMetaData();
			
			int count = rsMetaData.getColumnCount();
			toClient.writeInt(count);
			// need to clear for every new connection
			colNames.clear();
			colType.clear();
			
			// send properties and types
			for (int i = 1; i <= count; i++) {
				colNames.add(rsMetaData.getColumnName(i));
				colType.add(rsMetaData.getColumnTypeName(i));
				toClient.writeUTF(rsMetaData.getColumnName(i));
				toClient.writeUTF(rsMetaData.getColumnTypeName(i));
			}
			
			// send removed columns
			toClient.writeInt(removedCols.size());
			for (int i = 0; i < removedCols.size(); i++) {
				toClient.writeUTF(removedCols.get(i));
			}
			
			sendCBData(resultSet);
	
			String query2 = "select * from NewStudent";
			ResultSet resultSet2 = statement.executeQuery(query2);
			while (resultSet2.next()) 
				lst.add(resultSet2.getString("ID"));		
			toClient.writeInt(lst.size());
			for (int i = 0; i < lst.size(); i++)
				toClient.writeUTF(lst.get(i));
			
			setRank();
		}
		
		/** send content throws SQLException, IOException */
		public void sendCBData(ResultSet resultSet) throws SQLException, IOException {
			List<String> lst = new ArrayList<String>();
			ResultSetMetaData rsMetaData = resultSet.getMetaData();
			int count = rsMetaData.getColumnCount();
			
			toClient.writeInt(count);
			for (int i = 1; i <= count; i++) { 
				String cName = rsMetaData.getColumnName(i);
				toClient.writeUTF(cName);
				if (!cName.equals("Picture")) {
					toClient.writeUTF(rsMetaData.getColumnTypeName(i));
					while (resultSet.next()) {
						if (!lst.contains(resultSet.getString(i)))
							lst.add(resultSet.getString(i));
					}
					toClient.writeInt(lst.size());
					for (int j = 0; j < lst.size(); j++) {
						if (lst.get(j) == null)
							toClient.writeUTF("e");
						else
							toClient.writeUTF(lst.get(j));
					}
					lst.clear();
					resultSet.beforeFirst();
				}
			}			
		}
		
		/**  Restore data to reset mode */
		public void resetSystem() {
			rankZero = false;
			try {
				FileInputStream fInput = new FileInputStream("Source.txt");		
				// Get the object of DataInputStream
				DataInputStream dInput = new DataInputStream(fInput);
				@SuppressWarnings("resource")
				BufferedReader br = new BufferedReader(new InputStreamReader(dInput));
				String strLine;
				// Read File Line By Line
				while ((strLine = br.readLine()) != null) {
					if (strLine != null && !strLine.equals(""))
						statement.execute(strLine);
				}
				
				// clear all content
				colNames.clear();
				colType.clear();
				removedCols.clear();
				removedCols.add("Gender");
				removedCols.add("Transport");
				
				// send all data to combo boxes
				retrieveData();
							
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		/** Submit query */
		public void submit() throws ClassNotFoundException, IOException, SQLException {
			try {
				setRank();
				@SuppressWarnings({ "unchecked", "rawtypes" })
				HashMap<String,List<String>> map = ((HashMap)objFromClient.readObject());
				List<Record> list = new ArrayList<Record>();
				
				refreshData();
				
				String query = "select * from Student where ";
				int count = 0;
				for (String key : map.keySet()) {
					if (map.get(key).get(0) != null)
						count++;
				}
					
				if (count != 0) {
					for (String key : map.keySet()) {
						if (map.get(key).get(0) != null) {
							String val = map.get(key).get(0);
							String op = map.get(key).get(1);
							String val2 = map.get(key).get(2);
							
							if (op.equals("between")) {
								if (count == 1)
									query += key +" " + op +"'"+val+"' AND " +"'"+val2+ "'";
								else
									query += key +" " + op +"'"+val+"' AND " +"'"+val2+"' and ";
								count--;
							}
							else {
								if (count == 1)
									query += key + op +"'"+val+"'";
								else
									query += key + op +"'"+val+"'" + " and ";
								count--;
							}
						}
					}
				}
				
				else {
					query = "select * from Student";
				}
					
				System.out.println(query+"\n");
				ResultSet rset = statement.executeQuery(query);	
				toClient.writeBoolean(true);
				ResultSetMetaData rsMetaData = rset.getMetaData();
				HashMap<String,String> mapOut = new HashMap<String,String>();
	
				while (rset.next()) {
					for (int i = 0; i < rsMetaData.getColumnCount(); i++) {
						mapOut.put(colNames.get(i), rset.getString(colNames.get(i)));
					}
					Record rec = new Record(mapOut); 
					list.add(rec);
					mapOut.clear();
				}
	
				toClient.writeInt(list.size());
				for (int i = 0; i < list.size(); i++){
					objToClient.writeObject(list.get(i));
				}
				
			} catch (SQLException e) {
				toClient.writeBoolean(false);
				System.out.println("error in server");
				refreshData();
			}
		}
		
		/** Delete column from data base throws IOException SQLException */
		public void delCol() throws IOException, SQLException {
			String col = fromClient.readUTF();
			condition = checkAction("delCol", col);
			toClient.writeBoolean(condition);
			
			if (condition) {
				if(col.equals("Average") || col.equals("Department") || col.equals("Credits") ){	//this is make the rank col to 0 
					rankZero = true;
					setRank();
				}
				
				// not sure we need this
//				if(col.equals("Average") || col.equals("Credits") ){	
//					updateDB(col);
//				}
				
				removedCols.add(col); 		// keep in removed columns
				colType.remove(colNames.indexOf(col));
				colNames.remove(col); 		// remove the column from table columns
				
				String query = "alter table Student drop column " + col;
				String queryCopy = "alter table NewStudent drop column " + col;  
				try {
					statement.execute(query);
					statement.execute(queryCopy); 
					// sends new list of removed columns
					toClient.writeInt(removedCols.size());
					for (int i = 0; i < removedCols.size(); i++) {
						toClient.writeUTF(removedCols.get(i));
					}
					// sends new list of table columns
					toClient.writeInt(colNames.size());
					for (int i = 0; i < colNames.size(); i++) {
						toClient.writeUTF(colNames.get(i));
						toClient.writeUTF(colType.get(i));
					}
					
				} catch (SQLException | IOException e) {
					System.out.println("Del Column - server");
				}	
			}
			
			else {
				refreshData();
			}
		}
		
		/** Add selected column throws IOException SQLException */
		public void addCol() throws IOException, SQLException {
			String col = fromClient.readUTF();
			condition = checkAction("addCol", col);
			System.out.println("cond = " + condition);
			toClient.writeBoolean(condition);
			
			if (condition) {
				removedCols.remove(col);			// remove column from removed list
				colNames.add(col);					// add the column to table list
				String[] parameters = typeMap.get(col).split(Pattern.quote("("));
				colType.add(parameters[0]);			// add column type to type list						
				
				String query = "alter table Student add " + col +" "+ typeMap.get(col);
				String queryCopy = "alter table NewStudent add " + col +" "+ typeMap.get(col);
				try {
					statement.execute(query);
					statement.execute(queryCopy); 
					
					// sends new list of removed columns
					toClient.writeInt(removedCols.size());
					for (int i = 0; i < removedCols.size(); i++) {
						toClient.writeUTF(removedCols.get(i));
					}
					// sends new list of table columns
					toClient.writeInt(colNames.size());
					for (int i = 0; i < colNames.size(); i++) {
						toClient.writeUTF(colNames.get(i));
						toClient.writeUTF(colType.get(i));
					}
					
					// this is make the rank column to be calculate
					ArrayList<String> copy_colNames = new ArrayList<String>();
					query = "select * from Student";
					ResultSet rset = statement.executeQuery(query);
					ResultSetMetaData rsMetaData = rset.getMetaData();
					for (int i = 1; i < rsMetaData.getColumnCount() + 1; i++) {
						copy_colNames.add(rsMetaData.getColumnName(i));
					}
					
					if(copy_colNames.contains("Department") && copy_colNames.contains("Average") && copy_colNames.contains("Credits")){
						// need to check all records if there is one null department - don't change flag
						rankZero = false;			
					}
								
				} catch (SQLException | IOException e) {
					System.out.println("Add Column - server");
				}	
			}
			
			else {
				refreshData();
			}
		}
		
		/** Delete selected row */
		public void delRow() throws IOException, SQLException {				
			String  id = fromClient.readUTF();
			condition = checkAction("delRow", id);
			toClient.writeBoolean(condition);
			
			if (condition) {
				// update NewStudent table
				String query = "insert into NewStudent select * from Student where id ='"+ id +"'";
				System.out.println( "update query : " + query);
				statement.execute(query); 
			   
				// delete row from Student table
			    query = "delete from Student  where id = '"+ id+"'" ;
				System.out.println( "delete row  query : " + query);
			    statement.execute(query);
			    
			    query = "select * from Student";
				ResultSet newRset = statement.executeQuery(query);	
				sendCBData(newRset);
			}
			else {
				refreshData();
			}
		}
		
		/** Add row by Id */
		public void addRow() throws IOException, SQLException {	
			// update Student table
			String  id = fromClient.readUTF();
			condition = checkAction("addRow", id);
			toClient.writeBoolean(condition);
			
			if (condition) {
				String query = "insert into Student select * from NewStudent where id ='"+ id +"'";
				System.out.println("update query : " + query);
				statement.execute(query); 		
			    
				// delete row from  NewStudent table
			    query = "delete from NewStudent where id = '"+ id+"'" ;
				System.out.println("delete row  query : " + query);
			    statement.execute(query); 		
			    
			    query = "select * from Student where id = '"+ id+"'" ;
			    ResultSet rset = statement.executeQuery(query);	
			    ResultSetMetaData rsMetaData = rset.getMetaData();
				HashMap<String,String> mapOut = new HashMap<String,String>();
				while (rset.next()) {
					for (int i = 0; i < rsMetaData.getColumnCount(); i++) {
						mapOut.put(colNames.get(i), rset.getString(colNames.get(i)));
					}
					Record rec = new Record(mapOut); 
					objToClient.writeObject(rec);
					mapOut.clear();
				}
				
				query = "select * from Student";
				ResultSet newRset = statement.executeQuery(query);	
				sendCBData(newRset);
			}
			else {
				refreshData();
			}
		}
		
		/** Update selected rows from client */
		public void updateRow() throws ClassNotFoundException {				
			try {
				int rowsNum = fromClient.readInt();
				
				for (int i = 0; i < rowsNum; i++) {
					Record rec = (Record)objFromClient.readObject();
					String id = rec.getID();
					
					condition = checkAction("update", id);
					toClient.writeBoolean(condition);
					
					if (condition) {
						String query = "update Student set ";
						HashMap<String,String> map = createMapToUpdate(rec);
						
						List<String> keys = new ArrayList<String>();
						Set<String> keySet = map.keySet();
						for (String key: keySet)
							keys.add(key);
					
						int count = 0;
						for (int j = 0; j < keys.size(); j++) {
							if (!removedCols.contains(keys.get(j))) {
								count++;
							}
						}					
						for (int j = 0; j < keys.size(); j++) {
							if (!removedCols.contains(keys.get(j))) {
								if (count == 1)
									query += keys.get(j) + " = '" + map.get(keys.get(j))+"'";
								else
									query += keys.get(j) + " = '" + map.get(keys.get(j))+"', ";
								count--;
							}
						}
									
						query += " where ID = '" + map.get("ID") + "'";
						//System.out.println(query+"\n");
						statement.execute(query);
					}
				}
				
				refreshData();
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		/** Update binded columns // not sure we need this */
		public void updateDB(String col) {
			String query = "update Student set ";
			
			if (col.equals("Average"))
				query += "Credits = 0";
			else
				query += "Average = 0";
			
			query += ", Failures = 0";
			
			try {
				System.out.println(query);
				statement.execute(query);		//////////////////////////////////
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		
		/** Check action throws SQLException */
		public boolean checkAction(String action, String value) throws SQLException {
				String query = "select * from Student";
				ResultSet rset = statement.executeQuery(query);
				ResultSetMetaData rsMetaData = rset.getMetaData();
				int cols = rsMetaData.getColumnCount();

				switch (action) {
					case "delCol": {
						for (int i = 1; i <= cols; i++)
							if (rsMetaData.getColumnName(i).equals(value))
								return true;
						return false;
					}
					case "addCol": {
						for (int i = 1; i <= cols; i++) {
							if (rsMetaData.getColumnName(i).equals(value))
								return false;
						}
						return true;
					}
					case "delRow": {
						while (rset.next()) {
							if (rset.getString("ID").equals(value))
								return true;
						}
						return false;
					}
					case "addRow": {						
						while (rset.next()) {
							if (rset.getString("ID").equals(value))
								return false;
						}
						return true;
					}
					case "update": {
						while (rset.next()) {
							if (rset.getString("ID").equals(value))
								return true;
						}
						return false;
					}
				}
				return false;
		}
		
		/** throws SQLException throws IOException */
		public void refreshData() throws SQLException, IOException {

			// clear all data 
			colNames.clear();
			colType.clear();
			
			// query for updated data
			String query = "select * from Student";
			ResultSet rset = statement.executeQuery(query);	
			ResultSetMetaData rsMetaData = rset.getMetaData();
			int cols = rsMetaData.getColumnCount();
			
			// insert new data and send to client
			toClient.writeInt(cols);
			for (int i = 1; i <= cols; i++) {
				colNames.add(rsMetaData.getColumnName(i));
				colType.add(rsMetaData.getColumnTypeName(i));
				toClient.writeUTF(rsMetaData.getColumnName(i));
				toClient.writeUTF(rsMetaData.getColumnTypeName(i));
			}
			
			toClient.writeInt(removedCols.size());
			for (int i = 0; i <removedCols.size(); i++) {
				toClient.writeUTF(removedCols.get(i));
			}
			
			/* ********************************** handle CB *********************************************/
			
			sendCBData(rset);
		
			
			/* *************************** handle table content *******************************************/
			
			List<Record> list = new ArrayList<Record>();
			HashMap<String,String> map = new HashMap<String,String>();
			
			while (rset.next()) {
				for (int i = 0; i < rsMetaData.getColumnCount(); i++) {
					map.put(colNames.get(i), rset.getString(colNames.get(i)));
				}
				Record rec = new Record(map); 
				list.add(rec);
				map.clear();
			}

			toClient.writeInt(list.size());
			for (int i = 0; i < list.size(); i++){
				objToClient.writeObject(list.get(i));
			}
			
			/* *************************** handle table content *******************************************/
			
			List<String> list2 = new ArrayList<String>();
			String query2 = "select * from NewStudent";
			ResultSet resultSet2 = statement.executeQuery(query2);
			while (resultSet2.next()) 
				list2.add(resultSet2.getString("ID"));		
			toClient.writeInt(list2.size());
			for (int i = 0; i < list2.size(); i++)
				toClient.writeUTF(list2.get(i));
			
			/* *************************** handle table content *******************************************/
			
			
			
		}
	}
	
	//----------------------------------- Referenced Methods ---------------------------------------------------//
	
	/** creating map to hold all possible columns */
	private void createStatMap() {
		removedCols.add("Gender");
		removedCols.add("Transport");
		
		for (int i = 0; i < namesForMap.length; i++) 
			typeMap.put(namesForMap[i], typeForMap[i]);
	}
	
	/** Connect to Data Base */
	@SuppressWarnings("resource")
	private void connectToDB() {
		String url = "jdbc:mysql://localhost/";
		String db = "";
		String driver = "com.mysql.jdbc.Driver";
		try {
			Class.forName(driver);
			System.out.println("Driver Loaded");
			//Connection connection = DriverManager.getConnection(url+db, "scott", "tiger");
			connection = DriverManager.getConnection(url+db, "scott", "tiger");
			System.out.println("Connection Established");
			statement = connection.createStatement();
			FileInputStream fInput = new FileInputStream("Source.txt");
			// Get the object of DataInputStream
			DataInputStream dInput = new DataInputStream(fInput);
			BufferedReader br = new BufferedReader(new InputStreamReader(dInput));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				//System.out.println(strLine);
				if (strLine != null && !strLine.equals(""))
					statement.execute(strLine);
			}
			setRank();
			
		} catch (Exception e) {
			System.out.println("connectToDB");
			System.out.println(e);
		} finally {
		}
	}
	
	/** set rank by department */
	private void setRank() throws SQLException {
		String query = "select * from Student";
		ResultSet rset = statement.executeQuery(query);
		ResultSetMetaData rsMetaData = rset.getMetaData();
		HashMap<String, String> mapOut = new HashMap<String, String>();
		ArrayList<Record> lst = new ArrayList<Record>();
		ArrayList<String> copy_colNames = new ArrayList<String>();

		for (int i = 1; i < rsMetaData.getColumnCount() + 1; i++) {
			copy_colNames.add(rsMetaData.getColumnName(i));
		}
		
		if(copy_colNames.contains("Rank")){
			if (rankZero == false) {
				rset = statement.executeQuery(query);
	
				while (rset.next()) {
					for (int i = 0; i < rsMetaData.getColumnCount(); i++) {
						mapOut.put(copy_colNames.get(i),
								rset.getString(copy_colNames.get(i)));
					}
					Record rec = new Record(mapOut);
					lst.add(rec);
				}
	
				double half;
				ArrayList<Record> rankRec = new ArrayList<Record>();
				ArrayList<Double> temp = new ArrayList<Double>();
				ArrayList<String> counterDep = new ArrayList<String>();
				ArrayList<Double> Median = new ArrayList<Double>();
				HashMap<String, List<Double>> map = new HashMap<String, List<Double>>();
	
				for (int i = 0; i < lst.size(); i++) { // count how much Department we have
	
					if (!(counterDep.contains(lst.get(i).getDepartment()))) {
						counterDep.add(lst.get(i).getDepartment());
					}
				}
				
				if(copy_colNames.contains("Department") && copy_colNames.contains("Average") && copy_colNames.contains("Credits")){
	
				for (int i = 0; i < counterDep.size(); i++) { // initial map with Department name and counter 0
					Median = new ArrayList<Double>();
					map.put((String) counterDep.get(i), Median);
				}
	
				for (int i = 0; i < lst.size(); i++) { // add to each Department the record to find Median
					((ArrayList<Double>) map.get(lst.get(i).getDepartment())).add(lst.get(i).getCredits());
				}
	
				for (int i = 0; i < counterDep.size(); i++) {
					Median = new ArrayList<Double>();
					rankRec = new ArrayList<Record>();
					temp = ((ArrayList<Double>) map.get(counterDep.get(i)));
					Collections.sort(temp);
					half = temp.get((int) temp.size() / 2);
					for (int j = 0; j < lst.size(); j++) {
						//System.out.println(lst.get(j).getAverage());
						try {
							if (counterDep.get(i).equals(lst.get(j).getDepartment()) && lst.get(j).getCredits() >= half) {					
								rankRec.add(lst.get(j));
							}
							else if (counterDep.get(i).equals(lst.get(j).getDepartment())) {
								lst.get(j).setRank(0);
								query = "UPDATE student s  SET rank = '" + 0 + "' where s.id = '" + lst.get(j).getID() + "'";
								statement.execute(query);
							}
						} catch (Exception e) {
							
						}
					}
					for (int z = 0; z < rankRec.size() + 1; z++) {
						for (int x = 0; x < rankRec.size() - 1; x++) {
							if (rankRec.get(x).getAverage() <= rankRec.get(x + 1).getAverage()) {
								Record rec = rankRec.get(x);
								rankRec.set(x, rankRec.get(x + 1));
								rankRec.set(x + 1, rec);	
							}	
						}
					}
					for (int z = 0; z < rankRec.size(); z++) {
						query = "UPDATE student s  SET rank = '" + (z + 1) + "' where s.id = '" + rankRec.get(z).getID() + "'";
						statement.execute(query);
					}
	
				}
				}
				copy_colNames.clear();
				
			}
	
			else {
			    query = "UPDATE student s SET rank = '" + 0 + "'";
				statement.execute(query);
			    }
		}

	}
	
	/** utility map to update record */
	public HashMap<String,String> createMapToUpdate(Record rec) {
		HashMap<String,String> map = new HashMap<String, String>();
		
		map.put(namesForMap[0], rec.getID());
		map.put(namesForMap[1], rec.getFirst_Name());
		map.put(namesForMap[2], rec.getLast_Name());
		map.put(namesForMap[3], rec.getCity());
		map.put(namesForMap[4], rec.getStreet());
		map.put(namesForMap[5], rec.getZip_Code());
		map.put(namesForMap[6], rec.getBirth_Date());
		map.put(namesForMap[7], Integer.toString(rec.getStart_Year()));
		map.put(namesForMap[8], rec.getDepartment());
		map.put(namesForMap[9], Double.toString(rec.getCredits()));
		map.put(namesForMap[10], Float.toString(rec.getAverage()));
		map.put(namesForMap[11], Integer.toString(rec.getFailures()));
		map.put(namesForMap[12], Integer.toString(rec.getRank()));
		map.put(namesForMap[13], rec.getGender());
		map.put(namesForMap[14], rec.getTransport());
		map.put(namesForMap[15], rec.getPicture());
		//System.out.println("pic record = " + rec.getPicture());
		
		return map;
	}

 	public static void main(String[] args) {
		launch(args);
	}

}
