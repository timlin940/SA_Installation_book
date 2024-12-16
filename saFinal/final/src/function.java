import java.sql.*;
import java.sql.Date;
import java.util.*;

public class function {

    private static final String cnStr = "jdbc:sqlserver://localhost;user=project;password=123456789;database=java;encrypt=false;";

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            System.out.println("✓ JDBC驅動程式載入成功");
        } catch (ClassNotFoundException ce) {
            System.err.println("✗ JDBC驅動程式載入失敗：" + ce.getMessage());
        }
    }

    public static String[] makePlan() {
        String[] results = new String[2];
        System.out.println("\n=== 開始處理配送訂單 ===");
        try (Connection cn = DriverManager.getConnection(cnStr)) {
            System.out.println("✓ 成功連接到資料庫");
            
            Map<String, List<Order>> categorizedOrders = new HashMap<>();
            String sql = "SELECT * FROM [order] WHERE isDriving = 0";
            
            try (Statement stmt = cn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    String target = rs.getString("target");
                    boolean shipToday = rs.getBoolean("shipToday");
                    String category = target + "_" + (shipToday ? "today" : "later");
                    
                    Order order = new Order(
                        rs.getInt("orderID"),
                        rs.getInt("goodID"),
                        rs.getInt("goodsNumber"),
                        target,
                        shipToday
                    );
                    
                    categorizedOrders.computeIfAbsent(category, k -> new ArrayList<>())
                                   .add(order);
                }
            }

            System.out.println("\n為各區域分配車輛...");
            for (Map.Entry<String, List<Order>> entry : categorizedOrders.entrySet()) {
                assignCarToOrders(cn, entry.getValue());
            }
            
            for (Map.Entry<String, List<Order>> entry : categorizedOrders.entrySet()) {
                assignCarToOrders(cn, entry.getValue());
            }
            
            // 獲取配送單和揀貨單結果
            results[0] = displayDeliverOrder(cn);
            results[1] = displayPickingList(cn);
            
            // 顯示結果
            System.out.println(results[0]);
            System.out.println(results[1]);
            
            System.out.println("\n=== 配送訂單處理完成 ===");
            
        } catch (SQLException e) {
            System.out.println("✗ 處理訂單錯誤: " + e.getMessage());
        }
        return results;
    }

    private static void assignCarToOrders(Connection cn, List<Order> orders) throws SQLException {
        if (orders.isEmpty()) return;
        
        // 獲取該批次訂單的地區和發貨時間信息
        String target = orders.get(0).target;
        boolean shipToday = orders.get(0).shipToday;
        
        // 先查找是否有已分配給該地區和發貨時間的車輛
        String findAssignedCarSQL = 
            "SELECT DISTINCT d.carID, c.nowCapacity, c.capacity " +
            "FROM deliverOrder d " +
            "JOIN car c ON d.carID = c.carID " +
            "WHERE d.target = ? AND CAST(d.deliverDate AS DATE) = ? " +
            "AND c.isDriving = 0";
        
        // 如果沒有已分配的車輛，則尋找新的可用車輛
        String findNewCarSQL = 
            "SELECT carID, nowCapacity, capacity " +
            "FROM car " +
            "WHERE isDriving = 0 " +
            "AND carID NOT IN (SELECT DISTINCT carID FROM deliverOrder) " +
            "ORDER BY capacity DESC";
        
        String currentCarID = null;
        int currentCarCapacity = 0;
        
        //System.out.println("\n處理地區: " + target + (shipToday ? " (今日發貨)" : " (非今日發貨)"));
        
        // 找到已分配給該地區的車輛
        try (PreparedStatement pstmt = cn.prepareStatement(findAssignedCarSQL)) {
            pstmt.setString(1, target);
            pstmt.setDate(2, shipToday ? 
                new java.sql.Date(System.currentTimeMillis()) : 
                new java.sql.Date(System.currentTimeMillis() + 86400000));
                
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    currentCarID = rs.getString("carID");
                    currentCarCapacity = rs.getInt("nowCapacity");
                    //System.out.println("✓ 該地區已分配的車輛: " + currentCarID);
                }
            }
        }
        
        // 如果沒有已分配的車輛，找新的車輛
        if (currentCarID == null) {
            try (Statement stmt = cn.createStatement();
                 ResultSet rs = stmt.executeQuery(findNewCarSQL)) {
                if (rs.next()) {
                    currentCarID = rs.getString("carID");
                    currentCarCapacity = rs.getInt("capacity");
                    //System.out.println("✓ 分配新車輛: " + currentCarID);
                }
            }
        }
        
        if (currentCarID == null) {
            //System.out.println("✗ 無可用車輛");
            return;
        }
        
        // 處理該批次所有訂單
        for (Order order : orders) {
            int goodSize = getGoodSize(cn, order.goodID);
            int requiredSpace = goodSize * order.goodsNumber;
            
            //System.out.println("\n正在處理訂單 ID: " + order.orderID + ", 商品 ID: " + order.goodID + ", 數量: " + order.goodsNumber + ", 所需空間: " + requiredSpace);
            
            // 檢查當前車輛空間是否足夠
            if (currentCarCapacity >= requiredSpace) {
                createDeliverOrder(cn, order, currentCarID);
                currentCarCapacity -= requiredSpace;
                updateCarCapacity(cn, currentCarID, currentCarCapacity);
                //System.out.println("✓ 分配車輛: " + currentCarID + ", 剩餘容量: " + currentCarCapacity);
            }
            else {
                // 找新車輛
                try (Statement stmt = cn.createStatement();
                     ResultSet rs = stmt.executeQuery(findNewCarSQL)) {
                    if (rs.next()) {
                        currentCarID = rs.getString("carID");
                        currentCarCapacity = rs.getInt("capacity");
                        if (currentCarCapacity >= requiredSpace) {
                            createDeliverOrder(cn, order, currentCarID);
                            currentCarCapacity -= requiredSpace;
                            updateCarCapacity(cn, currentCarID, currentCarCapacity);
                            //System.out.println("✓ 分配新車輛: " + currentCarID + ", 剩餘容量: " + currentCarCapacity);
                        }
                        else {
                            System.out.println("✗ 找不到容量足夠的車輛");
                        }
                    }
                    else {
                        System.out.println("✗ 找不到可用車輛");
                    }
                }
            }
        }
        // 更新已使用車輛狀態
        updateCarStatus(cn, currentCarID);
    }

    private static int getGoodSize(Connection cn, int goodID) throws SQLException {
        String sql = "SELECT size FROM good WHERE goodID = ?";
        try (PreparedStatement pstmt = cn.prepareStatement(sql)) {
            pstmt.setInt(1, goodID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int size = rs.getInt("size");
                    //System.out.println("商品 ID: " + goodID + " 的大小為: " + size);
                    return size;
                }
                throw new SQLException("找不到商品: " + goodID);
            }
        }
    }

    private static void createDeliverOrder(Connection cn, Order order, String carID) throws SQLException {
        int deliverOrderID = 0;
        String sql1 = "SELECT MAX(deliverOrderID) as maxID FROM deliverOrder";
        try (Statement stmt = cn.createStatement();
             ResultSet rs = stmt.executeQuery(sql1)) {
            if (rs.next()) {
            	deliverOrderID =  rs.getInt("maxID") + 1;
            }
            else {deliverOrderID =  1;}
        }
        
        int managerID = getAvailableManagerID(cn);
        
        //System.out.println("創建配送訂單 - 訂單ID: " + deliverOrderID + ", 管理員ID: " + managerID + ", 車輛ID: " + carID);
        
        String sql = "INSERT INTO deliverOrder (deliverOrderID, managerID, goodsID, goodsNumber, " +
                    "target, hasDriver, deliverDate, carID) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = cn.prepareStatement(sql)) {
            pstmt.setInt(1, deliverOrderID);
            pstmt.setInt(2, managerID);
            pstmt.setInt(3, order.goodID);
            pstmt.setInt(4, order.goodsNumber);
            pstmt.setString(5, order.target);
            pstmt.setBoolean(6, false);
            pstmt.setDate(7, order.shipToday ? new java.sql.Date(System.currentTimeMillis()) 
                : new java.sql.Date(System.currentTimeMillis() + 86400000));
            pstmt.setString(8, carID);
            
            pstmt.executeUpdate();
            //System.out.println("✓ 創建配送訂單成功");
            
            updateOrderStatus(cn, order.orderID, order.goodID);
        }
    }

    private static void updateCarCapacity(Connection cn, String carID, int newCapacity) throws SQLException {
        String sql = "UPDATE car SET nowCapacity = ? WHERE carID = ?";
        try (PreparedStatement pstmt = cn.prepareStatement(sql)) {
            pstmt.setInt(1, newCapacity);
            pstmt.setString(2, carID);
            pstmt.executeUpdate();
            //System.out.println("✓ 更新車輛 " + carID + " 容量為: " + newCapacity);
        }
    }

//    private static int generateDeliverOrderID(Connection cn) throws SQLException {
//        String sql = "SELECT MAX(deliverOrderID) as maxID FROM deliverOrder";
//        try (Statement stmt = cn.createStatement();
//             ResultSet rs = stmt.executeQuery(sql)) {
//            if (rs.next()) {
//                return rs.getInt("maxID") + 1;
//            }
//            return 1;
//        }
//    }

    private static int getAvailableManagerID(Connection cn) throws SQLException {
        String sql = "SELECT TOP 1 managerID FROM manager";
        try (Statement stmt = cn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("managerID");
            }
            throw new SQLException("找不到管理員");
        }
    }

    private static void updateOrderStatus(Connection cn, int orderID, int goodID) throws SQLException {
        String sql = "UPDATE [order] SET isDriving = 1 WHERE orderID = ? AND goodID = ?";
        try (PreparedStatement pstmt = cn.prepareStatement(sql)) {
            pstmt.setInt(1, orderID);
            pstmt.setInt(2, goodID);
            pstmt.executeUpdate();
            //System.out.println("✓ 訂單狀態更新為已分配 (訂單ID: " + orderID + ", 商品ID: " + goodID + ")");
        }
    }
    
    // 新增更新車輛狀態
    private static void updateCarStatus(Connection cn, String carID) throws SQLException {
        if (carID == null) return;
        
        String sql = "UPDATE car SET isDriving = 1 WHERE carID = ?";
        try (PreparedStatement pstmt = cn.prepareStatement(sql)) {
            pstmt.setString(1, carID);
            pstmt.executeUpdate();
            //System.out.println("✓ 更新車輛 " + carID + " 狀態為運行中");
        }
    }
    
    //送貨單
    private static String displayDeliverOrder(Connection cn) throws SQLException {
        StringBuilder result = new StringBuilder("\n=== 配送單 ===\n");
        
        String sql = 
            "SELECT d.carID, dr.driverID, d.goodsID, SUM(d.goodsNumber) as totalNumber, " +
            "d.deliverDate, d.target, o.shipToday " +
            "FROM deliverOrder d " +
            "LEFT JOIN driver dr ON d.carID = dr.carID " +
            "LEFT JOIN [order] o ON d.goodsID = o.goodID " +
            "WHERE d.hasDriver = 0 " +
            "GROUP BY d.carID, dr.driverID, d.goodsID, d.deliverDate, d.target, o.shipToday " +
            "ORDER BY d.carID, d.target, d.goodsID";

        String updateSQL = 
            "UPDATE deliverOrder " +
            "SET hasDriver = 1 " +
            "WHERE carID = ? AND goodsID = ? AND hasDriver = 0";

        String currentCarID = null;
        String currentTarget = null;
        Integer currentGoodID = null;
        Date currentDate = null;
        
        try (Statement stmt = cn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             PreparedStatement updateStmt = cn.prepareStatement(updateSQL)) {
            
            while (rs.next()) {
                String carID = rs.getString("carID");
                String target = rs.getString("target");
                int goodID = rs.getInt("goodsID");
                Date deliverDate = rs.getDate("deliverDate");
                boolean shipToday = rs.getBoolean("shipToday");
                
                if (!carID.equals(currentCarID)) {
                    if (currentCarID != null) {
                        result.append("\n").append("=".repeat(50)).append("\n");
                    }
                    currentCarID = carID;
                    currentTarget = target;
                    currentGoodID = null;
                    currentDate = deliverDate;
                    
                    result.append("Car: ").append(carID)
                          .append(" 司機: ").append(rs.getObject("driverID") != null ? rs.getInt("driverID") : "未分配")
                          .append("\n");
                    result.append("是否今日出貨: ").append(shipToday ? "是" : "否").append("\n");
                    result.append("地區: ").append(target).append("\n");
                    result.append("貨物:\n");
                } 
                else if (!target.equals(currentTarget)) {
                    currentTarget = target;
                    currentGoodID = null;
                    result.append("\n地區: ").append(target).append("\n");
                    result.append("貨物:\n");
                }
                
                if (currentGoodID == null || goodID != currentGoodID) {
                    result.append(String.format("         ID: %d, 數量: %d%n", 
                        goodID, rs.getInt("totalNumber")));
                    currentGoodID = goodID;
                }
                
                updateStmt.setString(1, carID);
                updateStmt.setInt(2, goodID);
                updateStmt.executeUpdate();
            }
            result.append("\n").append("=".repeat(50));
        }
        return result.toString();
    }

    private static String displayPickingList(Connection cn) throws SQLException {
        StringBuilder result = new StringBuilder("\n=== 揀貨單 ===\n");
        
        String sql = 
            "SELECT d.carID, d.goodsID, SUM(d.goodsNumber) as totalNumber, " +
            "d.deliverDate, d.target, o.shipToday " +
            "FROM deliverOrder d " +
            "LEFT JOIN [order] o ON d.goodsID = o.goodID " +
            "WHERE d.hasDriver = 1 " +
            "GROUP BY d.carID, d.goodsID, d.deliverDate, d.target, o.shipToday " +
            "ORDER BY d.carID, d.target, d.goodsID";

        List<Integer> pickerIDs = new ArrayList<>();
        try (Statement stmt = cn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT pickerID FROM picker ORDER BY pickerID")) {
            while (rs.next()) {
                pickerIDs.add(rs.getInt("pickerID"));
            }
        }

        String currentCarID = null;
        String currentTarget = null;
        Integer currentGoodID = null;
        int pickerIndex = 0;
        
        try (Statement stmt = cn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String carID = rs.getString("carID");
                String target = rs.getString("target");
                int goodID = rs.getInt("goodsID");
                boolean shipToday = rs.getBoolean("shipToday");
                
                if (!carID.equals(currentCarID)) {
                    if (currentCarID != null) {
                        result.append("\n").append("=".repeat(50)).append("\n");
                    }
                    currentCarID = carID;
                    currentTarget = target;
                    currentGoodID = null;
                    
                    if (pickerIndex >= pickerIDs.size()) {
                        pickerIndex = 0;
                    }
                    
                    result.append("Car: ").append(carID)
                          .append(" 揀貨員: ").append(pickerIDs.get(pickerIndex)).append("\n");
                    result.append("是否今日出貨: ").append(shipToday ? "是" : "否").append("\n");
                    result.append("地區: ").append(target).append("\n");
                    result.append("貨物:\n");
                    
                    createPickingList(cn, carID, goodID, rs.getInt("totalNumber"), pickerIDs.get(pickerIndex));
                    pickerIndex++;
                } 
                else if (!target.equals(currentTarget)) {
                    currentTarget = target;
                    currentGoodID = null;
                    result.append("\n地區: ").append(target).append("\n");
                    result.append("貨物:\n");
                }
                
                if (currentGoodID == null || goodID != currentGoodID) {
                    result.append(String.format("         ID: %d, 數量: %d%n", 
                        goodID, rs.getInt("totalNumber")));
                    currentGoodID = goodID;
                }
            }
            result.append("\n").append("=".repeat(50));
        }
        return result.toString();
    }

    private static void createPickingList(Connection cn, String carID, int goodID, int goodsNumber, int pickerID) throws SQLException {
        int pickingListID = generatePickingListID(cn);
        
        String sql = "INSERT INTO pickingList (pickingListID, goodsID, goodsNumber, pickerID) " +
                     "VALUES (?, ?, ?, ?)";
                     
        try (PreparedStatement pstmt = cn.prepareStatement(sql)) {
            pstmt.setInt(1, pickingListID);
            pstmt.setInt(2, goodID);
            pstmt.setInt(3, goodsNumber);
            pstmt.setInt(4, pickerID);
            
            pstmt.executeUpdate();
            //System.out.println("✓ 創建揀貨單");
        }
    }

    private static int generatePickingListID(Connection cn) throws SQLException {
        String sql = "SELECT MAX(pickingListID) as maxID FROM pickingList";
        try (Statement stmt = cn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("maxID") + 1;
            }
            return 1;
        }
    }
    
    private static class Order {
        int orderID, goodID, goodsNumber;
        String target;
        boolean shipToday;
        
        Order(int orderID, int goodID, int goodsNumber, String target, boolean shipToday) {
            this.orderID = orderID;
            this.goodID = goodID;
            this.goodsNumber = goodsNumber;
            this.target = target;
            this.shipToday = shipToday;
        }
    }

    public static void main(String[] args) {
//    	String[] results = makePlan();
//        String deliverOrder = results[0];
//        String pickingList = results[1];
    }
}