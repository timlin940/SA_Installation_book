import java.sql.*;

public class CreatDB {
	public static void main(String[] args) {
        try
        {
     	   Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        }
        catch(ClassNotFoundException ce)
        {
     	   System.out.println("JDBC沒有驅動程式" + ce.getMessage());
     	   return ;
        }
        try
        {
     	   Connection cn=DriverManager.getConnection ("jdbc:sqlserver://localhost;user=project;password=123456789;database=java;encrypt=true;trustServerCertificate=true");
     	   System.out.println("資料庫連接成功");
           cn.close();
           System.out.println("釋放與資料庫的連線");
        }
        catch(SQLException e)
        {
     	   System.out.println("資料庫連接失敗\n" + e.getMessage());
        }
        try (Connection cn = DriverManager.getConnection(
                "jdbc:sqlserver://localhost;user=project;password=123456789;database=java;encrypt=true;trustServerCertificate=true")) {
            System.out.println("資料庫連接成功");

            // 建立 manager 資料表
            try (Statement stmt = cn.createStatement()) {
                String createTableSQL = "CREATE TABLE manager (" +
                        "lastName NVARCHAR(50), " +
                        "firstName NVARCHAR(50), " +
                        "managerID INT PRIMARY KEY)";
                stmt.executeUpdate(createTableSQL);
                System.out.println("資料表 manager 已成功建立");
            } catch (SQLException e) {
                System.out.println("建立資料表時發生錯誤: " + e.getMessage());
            }
            
	        // 建立 order 資料表
	        try (Statement stmt = cn.createStatement()) {
	            String createTableSQL = "CREATE TABLE [order] (" +
                        "target NVARCHAR(255) NOT NULL, " + // 儲存目的地
	                    "orderID INT NOT NULL, " +
	                    "goodID INT NOT NULL, " +
	                    "goodsNumber INT NOT NULL, " +
	                    "customerID INT NOT NULL, " +
	                    "orderTime DATE NOT NULL, " +
                        "shipToday BIT NOT NULL, " + // 是否當日出貨 (布林值)
                        "isDriving BIT NOT NULL," +
	                    "PRIMARY KEY (orderID, goodID))";
	
	            stmt.executeUpdate(createTableSQL);
	            System.out.println("資料表 order 已成功建立");
	        } catch (SQLException e) {
	            System.out.println("建立資料表時發生錯誤: " + e.getMessage());
            }
            
	        // 建立 good 資料表
            try (Statement stmt = cn.createStatement()) {
                String createTableSQL = "CREATE TABLE good (" +
                        "goodID INT NOT NULL PRIMARY KEY, " + // 商品 ID (主鍵)
                        "goodfrozened BIT NOT NULL, " + // 商品是否冷凍 (布林值)
                        "size INT NOT NULL, " + // 商品大小
                        ")";

                stmt.executeUpdate(createTableSQL);
                System.out.println("資料表 good 已成功建立");
            } catch (SQLException e) {
                System.out.println("建立資料表時發生錯誤: " + e.getMessage());
            }
            
            // 建立 deliverOrder 資料表
            try (Statement stmt = cn.createStatement()) {
                String createDeliverOrderSQL = "CREATE TABLE deliverOrder (" +
                        "deliverOrderID INT NOT NULL PRIMARY KEY, " +
                        "managerID INT NOT NULL, " +
                        "goodsID INT NOT NULL, " +
                        "goodsNumber INT NOT NULL, " +
                        "target NVARCHAR(255) NOT NULL," +
                        "hasDriver BIT NOT NULL," +
	                    "deliverDate DATE NOT NULL, " +
                        "carID NVARCHAR(50) NOT NULL" +
                        ")";
                stmt.executeUpdate(createDeliverOrderSQL);
                System.out.println("資料表 deliverOrder 已成功建立");
            }catch (SQLException e) {
                System.out.println("建立資料表時發生錯誤: " + e.getMessage());
            }

            // 建立 driver 資料表
            try (Statement stmt = cn.createStatement()) {
                String createDriverSQL = "CREATE TABLE driver (" +
                        "carID NVARCHAR(50) NOT NULL, " +
                        "driverID INT NOT NULL PRIMARY KEY," +
                        "isDriving BIT NOT NULL" +
                        ")";
                stmt.executeUpdate(createDriverSQL);
                System.out.println("資料表 driver 已成功建立");
            }catch (SQLException e) {
                System.out.println("建立資料表時發生錯誤: " + e.getMessage());
            }

            // 建立 car 資料表
            try (Statement stmt = cn.createStatement()) {
                String createCarSQL = "CREATE TABLE car (" +
                        "carID NVARCHAR(50) NOT NULL PRIMARY KEY, " +
                        "capacity INT NOT NULL," +
                        "nowCapacity INT NOT NULL," +
                        "isDriving BIT NOT NULL" +
                        ")";
                stmt.executeUpdate(createCarSQL);
                System.out.println("資料表 car 已成功建立");
            }catch (SQLException e) {
                System.out.println("建立資料表時發生錯誤: " + e.getMessage());
            }
            
            // 建立 picker 資料表
            try (Statement stmt = cn.createStatement()) {
                String createPickerSQL = "CREATE TABLE picker (" +
                        "pickerID INT NOT NULL PRIMARY KEY, " +
                        "lastName NVARCHAR(50) NOT NULL, " +
                        "firstName NVARCHAR(50) NOT NULL" +
                        ")";
                stmt.executeUpdate(createPickerSQL);
                System.out.println("資料表 picker 已成功建立");
            }catch (SQLException e) {
                System.out.println("建立資料表時發生錯誤: " + e.getMessage());
            }
            
            // 建立 pickingList 資料表
            try (Statement stmt = cn.createStatement()) {
                String createPickingListSQL = "CREATE TABLE pickingList (" +
                        "pickingListID INT NOT NULL PRIMARY KEY, " +
                        "goodsID INT NOT NULL, " +
                        "goodsNumber INT NOT NULL, " +
                        "pickerID INT NOT NULL, " +
                        "FOREIGN KEY (pickerID) REFERENCES picker(pickerID)" +
                        ")";
                stmt.executeUpdate(createPickingListSQL);
                System.out.println("資料表 pickingList 已成功建立");
            }catch (SQLException e) {
                System.out.println("建立資料表時發生錯誤: " + e.getMessage());
            }
            

            // 關閉連線
            System.out.println("釋放與資料庫的連線");
        } catch(SQLException e){
	       	System.out.println("資料庫連接失敗\n" + e.getMessage());
	    }
	}

}