import java.sql.*;
import java.text.ParseException;
import java.util.Random;

public class insert {
	public static void main(String[] args) throws ParseException {
    	long start = Date.valueOf("2023-06-01").getTime(); // 開始日期
        long end = Date.valueOf("2023-06-30").getTime();   // 結束日期
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ce) {
            System.out.println("JDBC 驅動程式加載失敗: " + ce.getMessage());
            return;
        }
        Random random = new Random();
        try (Connection cn = DriverManager.getConnection(
                "jdbc:sqlserver://localhost;user=project;password=123456789;database=java;encrypt=true;trustServerCertificate=true")) {
            System.out.println("資料庫連線成功");
            
            // 插入 10 台車
            String insertCarSQL = "INSERT INTO car (carID, capacity, nowCapacity, isDriving) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = cn.prepareStatement(insertCarSQL)) {
                for (int i = 1; i <= 10; i++) {
                	boolean k = false; //random.nextBoolean();
                	int c = Math.abs(random.nextInt());
                	if(i < 10) {
                		ps.setString(1, "CAR00" + i);
                	}
                	else if(i >= 10 && i <= 99) {
                		ps.setString(1, "CAR0" + i);
                	}
                	int cc = (c % 4 + 1) * 1000;
                    ps.setInt(2, cc); // 設定容量
                    ps.setInt(3, cc);
                    ps.setBoolean(4, k);
                    ps.executeUpdate();
                }
                System.out.println("成功插入 10 台車");
            }

            // 插入 10 司機
            String insertDriverSQL = "INSERT INTO driver (carID, driverID,isDriving) VALUES (?, ?, ?)";
            try (PreparedStatement ps = cn.prepareStatement(insertDriverSQL)) {
                for (int i = 1; i <= 10; i++) {
                	boolean k = random.nextBoolean();
                    if(i < 10) {
                		ps.setString(1, "CAR00" + i);
                	}
                	else if(i >= 10 && i <= 99) {
                		ps.setString(1, "CAR0" + i);
                	}
                    ps.setInt(2, 100 + i); // 司機 ID
                    ps.setBoolean(3,k);
                    ps.executeUpdate();
                }
                System.out.println("成功插入 10 司機");
            }

            // 插入 10 種 goods
            String insertGoodSQL = "INSERT INTO good (goodID, goodfrozened, size) VALUES (?, ?, ?)";
            try (PreparedStatement ps = cn.prepareStatement(insertGoodSQL)) {
                for (int i = 1; i <= 10; i++) {
                    ps.setInt(1, i);
                    ps.setBoolean(2, i % 2 == 0); // 偶數冷凍
                    ps.setInt(3, i * 5); // 商品大小
                    ps.executeUpdate();
                }
                System.out.println("成功插入 10 種 goods");
            }

            // 插入 10 個 deliverOrder
//            String insertDeliverOrderSQL = "INSERT INTO deliverOrder (deliverOrderID, managerID, goodsID, goodsNumber, target,isDriving, deliverDate, driverId) VALUES (?, ?, ?, ?, ?,?, ?, ?)";
//            try (PreparedStatement ps = cn.prepareStatement(insertDeliverOrderSQL)) {
//            	
//            	 // 指定日期範圍
//                
//                for (int i = 1; i <= 10; i++) {
//                    ps.setInt(1, i);
//                    ps.setInt(2, 200 + (i % 2)); // managerID: 200, 201
//                    ps.setInt(3, i); // goodsID
//                    ps.setInt(4, i * 10); // 商品數量
//                    ps.setString(5, "Target" + i); // 目標地點
//                    boolean k = random.nextBoolean();
//                    ps.setBoolean(6,k);
//                 // 隨機生成日期
//                    long randomDate = start + (long) (random.nextDouble() * (end - start));
//                    ps.setDate(7, new Date(randomDate)); // deliverDate
//                    
//                    ps.setInt(8, 100 + i%5+1); // 司機 ID
//                    ps.executeUpdate();
//                }
//                System.out.println("成功插入 10 個 deliverOrder");
//            }

            // 插入 20 個 order
            String insertOrderSQL = "INSERT INTO [order] (target, orderID, goodID, goodsNumber, customerID, orderTime, shipToday, isDriving) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = cn.prepareStatement(insertOrderSQL)) {
                String loc[] = {"高雄","台中","台北"};         
            	for (int i = 1; i <= 20; i++) {
            		int k = random.nextInt(3); // 產生 0, 1, 2 的隨機值
                    ps.setString(1, "送往" + loc[k]); // 目的地
                    ps.setInt(2, i); // orderID
                    ps.setInt(3, (i % 10) + 1); // goodID
                    ps.setInt(4, i * 2); // 商品數量
                    ps.setInt(5, 300 + i); // customerID
                                     
                    // 設定隨機日期
                    long randomdate = start + (long) (random.nextDouble() * (end - start));
                    ps.setDate(6, new Date(randomdate));// 設定時間       
                    boolean j = random.nextBoolean();
                    ps.setBoolean(7, j); //shipToday
                    ps.setBoolean(8, false); //isDriving
                    ps.executeUpdate();
                }
                System.out.println("成功插入 20 個 order");
                }

            // 插入 2 位 manager
            String insertManagerSQL = "INSERT INTO manager (lastName, firstName, managerID) VALUES (?, ?, ?)";
            try (PreparedStatement ps = cn.prepareStatement(insertManagerSQL)) {
                ps.setString(1, "Lin");
                ps.setString(2, "Yuting");
                ps.setInt(3, 200); // managerID
                ps.executeUpdate();
                ps.setString(1, "Chen");
                ps.setString(2, "Weiting");
                ps.setInt(3, 201); // managerID
                ps.executeUpdate();
                System.out.println("成功插入 2 位 manager");
            }

            // 插入 10 個 picker
            String insertPickerSQL = "INSERT INTO picker (pickerID, lastName, firstName) VALUES (?, ?, ?)";
            try (PreparedStatement ps = cn.prepareStatement(insertPickerSQL)) {
                for (int i = 1; i <= 10; i++) {
                    ps.setInt(1, i);
                    ps.setString(2, "PickerLastName" + i);
                    ps.setString(3, "PickerFirstName" + i);
                    ps.executeUpdate();
                }
                System.out.println("成功插入 10 個 picker");
            }

            // 插入 5 張 pickingList
//            String insertPickingListSQL = "INSERT INTO pickingList (pickingListID, goodsID, goodsNumber, pickerID) VALUES (?, ?, ?, ?)";
//            try (PreparedStatement ps = cn.prepareStatement(insertPickingListSQL)) {
//                for (int i = 1; i <= 5; i++) {
//                    ps.setInt(1, i);
//                    ps.setInt(2, i); // goodsID
//                    ps.setInt(3, i * 5); // 商品數量
//                    ps.setInt(4, i); // pickerID
//                    ps.executeUpdate();
//                }
//                System.out.println("成功插入 5 張 pickingList");
//            }

        } catch (SQLException e) {
            System.out.println("資料庫連線或插入失敗: " + e.getMessage());
        }
    }
}
