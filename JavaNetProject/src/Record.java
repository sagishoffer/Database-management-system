// Sagi Shoffer
// Matan Shulman

import java.util.HashMap;

@SuppressWarnings("serial")
public class Record implements java.io.Serializable {
		private String ID;
		private String fName;
		private String lName;
		private String city;
		private String street;
		private String zip;
		private String bd;
		private int start;
		private String dept;
		private double credit;
		private float avg;
		private int fails;
		private int rank;
		private String gen;
		private String trans;
		private String pic;
		
		Record(HashMap<String,String> map) {
			this.ID = map.get("ID");
			this.fName = map.get("First_Name");
			this.lName = map.get("Last_Name");
			this.city = map.get("City");
			this.street = map.get("Street");
			this.zip = map.get("Zip_Code");
			this.bd = map.get("Birth_Date");
			if (map.get("Start_Year") != null)
				this.start = Integer.parseInt(map.get("Start_Year"));
			this.dept = map.get("Department");
			if (map.get("Credits") != null)
				this.credit = Double.parseDouble(map.get("Credits"));
			if (map.get("Average") != null)
				this.avg = Float.parseFloat(map.get("Average"));
			if (map.get("Failures") != null)
				this.fails = Integer.parseInt(map.get("Failures"));
			if (map.get("Rank") != null)
				this.rank = Integer.parseInt(map.get("Rank"));	
			// optional 
			this.gen = map.get("Gender");
			this.trans = map.get("Transport");
			this.pic = map.get("Picture");
		}

		public void setID(String iD) {
			this.ID = iD;
		}

		public void setfName(String fName) {
			this.fName = fName;
		}

		public void setlName(String lName) {
			this.lName = lName;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public void setZip(String zip) {
			this.zip = zip;
		}

		public void setBd(String bd) {
			this.bd = bd;
		}
		
		public void setStart(int start) {
			this.start = start;
		}

		public void setDept(String dept) {
			this.dept = dept;
		}

		public void setCredit(double credit) {
			this.credit = credit;
		}

		public void setAvg(float avg) {
			this.avg = avg;
		}

		public void setFails(int fails) {
			this.fails = fails;
		}

		public void setRank(int rank) {
			this.rank = rank;
		}

		public void setGen(String gen) {
			this.gen = gen;
		}

		public void setTrans(String trans) {
			this.trans = trans;
		}

		public void setPic(String pic) {
			this.pic = pic;
		}

		public String getID() {
			return ID;
		}

		public String getFirst_Name() {
			return fName;
		}

		public String getLast_Name() {
			return lName;
		}

		public String getCity() {
			return city;
		}

		public String getStreet() {
			return street;
		}
		public void setStreet(String street) {
			this.street = street;
		}

		public String getZip_Code() {
			return zip;
		}

		public String getBirth_Date() {
			return bd;
		}

		public int getStart_Year() {
			return start;
		}

		public String getDepartment() {
			return dept;
		}

		public double getCredits() {
			return credit;
		}

		public float getAverage() {
			return avg;
		}

		public int getFailures() {
			return fails;
		}

		public int getRank() {
			return rank;
		}
		
		public String getGender() {
			return gen;
		}
		
		public String getTransport() {
			return trans;
		}
		
		public String getPicture() {
			return pic;
		}		
}

	