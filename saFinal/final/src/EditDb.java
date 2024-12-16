import java.sql.*;

public class EditDb {
	private static final String cnStr = "jdbc:sqlserver://localhost;user=project;password=123456789;database=java;encrypt=false;";
	
	// 驅動程式加載
    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ce) {
            System.err.println("JDBC沒有驅動：" + ce.getMessage());
        }
    }
	
    // 管理員註冊
	public static void managerRegistration(String lastName, String firstName, int managerID) {
        // 定義 SQL 語句
        String sqlStr = "INSERT INTO dbo.manager (lastName, firstName, managerID) VALUES (?, ?, ?)";
        
        // 資料庫連接與操作
        try (Connection cn = DriverManager.getConnection(cnStr);
            PreparedStatement ps = cn.prepareStatement(sqlStr)) {
            // 設定參數
        	ps.setString(1, lastName);
            ps.setString(2, firstName);
            ps.setInt(3, managerID);

            // 執行插入
            int count = ps.executeUpdate();
            if (count > 0) {
                System.out.println("管理員新增成功！");
            }
            else {
                System.out.println("管理員新增失敗！");
            }
        }
        catch (SQLException e) {
            System.out.println("資料庫操作失敗：" + e.getMessage());
        }
	}
	
	//接收訂單-使用者介面請接到這邊
	public static void inputOrder(int orderID, int goodID, int goodsNumber, int customerID, String target, Date orderTime, boolean shipToday) {
        // 定義 SQL 語句
        String sqlStr = "INSERT INTO [dbo].[order] (target, orderID, goodID, goodsNumber, customerID, orderTime, shipToday, isDriving) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        // 資料庫連接與操作
        try (Connection cn = DriverManager.getConnection(cnStr);
            PreparedStatement ps = cn.prepareStatement(sqlStr)/*執行SQL語句*/) {
            // 設定參數
        	ps.setString(1, target);
            ps.setInt(2, orderID);
            ps.setInt(3, goodID);
            ps.setInt(4, goodsNumber);
            ps.setInt(5, customerID);
            ps.setDate(6, orderTime);
            ps.setBoolean(7, shipToday);
            ps.setBoolean(8, false);

            // 執行插入
            int count = ps.executeUpdate();
            if (count > 0) {
                System.out.println("訂單新增成功！");
            }
            else {
                System.out.println("訂單新增失敗！");
            }
        }
        catch (SQLException e) {
            System.out.println("資料庫操作失敗：" + e.getMessage());
        }
    }

	public static void main(String[] args) {
		//測試
//        int orderID = 5;
//        int goodID = 110;
//        int goodsNumber = 1;
//        int customerID = 520;
//        String target = "台北市信義區";
//        boolean shipToday = false;
//        Date orderTime = new Date(System.currentTimeMillis());
//        boolean isDriving = false;
//        
//        inputOrder(orderID, goodID, goodsNumber, customerID, target, orderTime, shipToday);
	}

}