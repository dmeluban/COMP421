import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.InputMismatchException;
import java.util.Scanner;

public class dbconnect {

	public static void main(String[] args) throws SQLException {

		Connection conn = null;
		Statement stmt = null;
		Scanner input = null;
		try {
			// connection semantics
			Class.forName("org.postgresql.Driver");

			conn = DriverManager.getConnection("jdbc:postgresql://comp421.cs.mcgill.ca:5432/cs421",
					"cs421g30", "dbsql2030");

			int userChoice = 0;
			input = new Scanner(System.in);
			stmt = conn.createStatement();

			// choice UI
			System.out.println("Welcome to MMTD Gyms Database \n\tTrack|Compare|Optimize\n");

			System.out.println("1: Check what classes a specific client is enrolled in.");
			System.out.println("2. Check if a specific food is in stock.");
			System.out.println("3. Enroll a client in a class (Inserting them into our DB)");
			System.out.println(
					"4. Aggregate all food nutritional values for a specific client purchases and how much money they have spent.");
			System.out.println("5. Update the address of a specific client. Then show all clients in the database.");
			System.out.println("6. QUIT");

			while (userChoice != 6) {
				System.out.println("Please pick one of the options:\n");
				try {
					userChoice = input.nextInt(); // TODO: add catch

				}
				catch (InputMismatchException e) {
					userChoice = 7;
				}
				if (userChoice > 6 || userChoice < 1) {
					System.out.println("Please pick a correct option from range 1-6 inclusive.");
					input.nextLine();
					continue;
				}
				input.nextLine();
				switch (userChoice) {
				case 1:
				{
					System.out.println("Please enter the email of the client.\n");
					System.out.println("Email of client: ");
					String emailCaseOne = input.nextLine().toLowerCase();

					PreparedStatement clientInClassPrep = null;
					try {
						clientInClassPrep = conn.prepareStatement("Select classname FROM enrolls WHERE email = ?");
						clientInClassPrep.setString(1, emailCaseOne);

						ResultSet rs = clientInClassPrep.executeQuery();
						int i=0;
						while(rs.next()) {
							i++;
							System.out.println("Email: " + emailCaseOne + " is enlisted in: " + rs.getString(1) );
						}
						if (i==0) {
							System.out.println(emailCaseOne + " is not enrolled in any classes.");
						}

					} catch (SQLException e) {
						System.err.println("msg:" + e.getMessage() + "\n code:" + e.getErrorCode() + "\n state: "
								+ e.getSQLState());
					} finally {
						clientInClassPrep.close();
					}

					break;
				}

				case 2:
				{
					System.out.println("Please input what food you would like to search for: ");
					String foodSelected = input.nextLine();
					PreparedStatement foodPrep = null;
					ResultSet rs = null;
					try {
						foodPrep = conn.prepareStatement("SELECT instock FROM Foodtype WHERE name= ?");
						foodPrep.setString(1, foodSelected);

						rs = foodPrep.executeQuery();
						int i=0;
						while (rs.next()) {
							System.out.println("Food " + foodSelected + " has " + rs.getInt(1) + " units in stock.");
							i++;
						}
						if (i==0) {
							System.out.println("There are no '" + foodSelected + "' in stock.");
						}
					} catch (SQLException e) {
						System.err.println("msg:" + e.getMessage() + "\n code:" + e.getErrorCode() + "\n state: "
								+ e.getSQLState());
					} finally {
						foodPrep.close();
						rs.close();
					}

					break;
				}
				case 3:
				{
					System.out.println(
							"Inserting a client into a database will require a e-mail, name, and the membership type.");
					System.out.println("Please enter the client's e-mail: ");
					String emailInput = input.nextLine().toLowerCase();
					System.out.println();

					System.out.println("Please enter the client's name: (case sensitive)");
					String nameInput = input.nextLine();
					System.out.println();
					String typeInput = "";
					while (true) {
						System.out.println("Please enter the membership type (regular or premium)");
						typeInput = input.nextLine().toLowerCase();
						if (typeInput.equals("regular") || typeInput.equals("premium")) {
							break;
						}
						else {
							System.out.println("That is not an option of membership type. Try again. \n");
						}
					}

					PreparedStatement clientUpdatePrep = null;
					try {
						DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
						LocalDateTime now = LocalDateTime.now();
						String d = (dtf.format(now)).toString();
						clientUpdatePrep = conn.prepareStatement(
								"INSERT INTO Client (email,name,subscriptiondate,membershipstatus) " + "VALUES (?, ? , '"
										+ d + "', ?)");

						clientUpdatePrep.setString(1, emailInput);
						clientUpdatePrep.setString(2, nameInput);
						clientUpdatePrep.setString(3, typeInput);

						clientUpdatePrep.executeUpdate();
						break;
					} catch (SQLException e) {
						System.err.println("msg:" + e.getMessage() + "\n code:" + e.getErrorCode() + "\n state: "
								+ e.getSQLState());
					} finally {
						clientUpdatePrep.close();
					}


					break;
				}
				case 4:
				{

					System.out.println("Please enter the e-mail of the client for which you want this information: ");
					String calorieEmail = input.nextLine().toLowerCase();

					PreparedStatement calorieInfoPrep = null;
					try {
						calorieInfoPrep = conn.prepareStatement("WITH agg AS(" + 
								"	SELECT CAST(AVG(calories) AS INT) AS avgCalories" + 
								"	, CAST(AVG(fat) AS INT) AS avgFat" + 
								"	, CAST(AVG(protein) AS INT) AS avgProtein" + 
								"	, CAST(AVG(carbohydrates) AS INT) AS avgCarbs" + 
								"	, email FROM purchases p" + 
								"	JOIN foodtype f ON p.name = f.name" + 
								"	GROUP BY email" + 
								")" + 
								", spent AS (" + 
								"	select SUM(price * quantity) AS totalSpent, email" + 
								"	FROM foodtype f" + 
								"	JOIN purchases p on f.name = p.name" + 
								"	GROUP BY p.email" + 
								")" + 
								"SELECT agg.email" + 
								", agg.avgcalories, agg.avgcalories/ totalSpent AS caloriesPerDollar" + 
								", agg.avgfat, agg.avgfat/ totalSpent AS fatPerDollar" + 
								", agg.avgprotein, agg.avgprotein/ totalSpent AS proteinPerDollar" + 
								", agg.avgcarbs, agg.avgcarbs/ totalSpent AS carbsPerDollar" + 
								", p.totalSpent " + 
								"FROM spent p " + 
								"JOIN agg ON p.email = agg.email WHERE agg.email = ?");
						calorieInfoPrep.setString(1,calorieEmail);
						ResultSet rs4 = calorieInfoPrep.executeQuery();

						while(rs4.next()) {
							System.out.println("Email: "+ calorieEmail);
							System.out.println("Average Calories: " + rs4.getInt(2));
							System.out.println("Calories Per Dollar: " + rs4.getInt(3));
							System.out.println("Avg Fat:" + rs4.getInt(4));
							System.out.println("Fat per Dollar: " + rs4.getInt(5));
							System.out.println("Avg Protein: " + rs4.getInt(6));
							System.out.println("Protein Per Dollar :" + rs4.getInt(7));
							System.out.println("Avg Carbs: " + rs4.getInt(8));
							System.out.println("Carbs Per Dollar " + rs4.getInt(9));
							System.out.println("Total Spent: " + rs4.getInt(10));


						}

					} catch (SQLException e) {
						System.err.println("msg:" + e.getMessage() + "\n code:" + e.getErrorCode() + "\n state: "
								+ e.getSQLState());
					}

					finally {
						calorieInfoPrep.close();

					}
					break;
				}

				case 5:
				{
					System.out.println("Updating the membership status of a client.\n");
					System.out.println("Please enter the client's email: ");
					String emailInputCaseFive = input.nextLine().toLowerCase();
					String typeInput = "";
					while (true) {
						System.out.println("Please enter the membership type (regular or premium)");
						typeInput = input.nextLine().toLowerCase();
						if (typeInput.equals("regular") || typeInput.equals("premium")) {
							break;
						}
						else {
							System.out.println("That is not an option of membership type. Try again. \n");
						}
					}
					System.out.println();
					PreparedStatement typeupdate = null;
					PreparedStatement showuser = null;
					try {
						typeupdate = conn.prepareStatement("UPDATE Client SET membershipstatus = ? WHERE email = ?");
						typeupdate.setString(1, typeInput);
						typeupdate.setString(2, emailInputCaseFive);
						typeupdate.executeUpdate();
						showuser = conn.prepareStatement("SELECT * FROM Client WHERE email = ?");
						showuser.setString(1, emailInputCaseFive);
						showuser.executeQuery();
						ResultSet rs5 = showuser.executeQuery();
						while (rs5.next()) {
							System.out.println(emailInputCaseFive + "'s new membershipstatus is " + typeInput + "!");
						}
						break;
					} catch (SQLException e) {
						System.err.println("msg:" + e.getMessage() + "\n code:" + e.getErrorCode() + "\n state: "
								+ e.getSQLState());
					}

					finally {
						showuser.close();
						typeupdate.close();
					}
					break;
				}

				case 6:
					break;
				}

			}
			System.out.println("Thank you for using our datbase. Consider signing up for premium membership "
					+ "if you have not already.");

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		finally {
			stmt.close();
			conn.close();
			input.close();
		}

	}

}