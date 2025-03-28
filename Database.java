package database;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;

public class Database {
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=TaskManagerDB;encrypt=false";
    private static final String USER = "taskmanager_user";
    private static final String PASSWORD = "gotaskapp";

    public static Connection connect() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            createTableIfNotExists(conn);
            return conn;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ JDBC Driver not found!", e);
        } catch (SQLException e) {
            throw new RuntimeException("❌ SQL Connection Error!", e);
        }
    }

    private static void createTableIfNotExists(Connection conn) {
        String checkTableSQL = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'tasks'";
        String createTableSQL = "CREATE TABLE tasks (" +
                                "id INT IDENTITY(1,1) PRIMARY KEY, " +
                                "title NVARCHAR(255) NOT NULL, " +
                                "description NVARCHAR(500), " +
                                "dueDate DATE)";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkTableSQL)) {
            if (!rs.next()) {
                stmt.execute(createTableSQL);
                System.out.println("✅ Table 'tasks' created.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Table creation failed: " + e.getMessage());
        }
    }

    public static List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tasks.add(new Task(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getDate("dueDate").toLocalDate()
                ));
            }
        } catch (SQLException e) {
            System.out.println("❌ Error retrieving tasks: " + e.getMessage());
        }
        return tasks;
    }

    public static void addTask(Task task) {
        String sql = "INSERT INTO tasks (title, description, dueDate) VALUES (?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setDate(3, Date.valueOf(task.getDueDate()));
            pstmt.executeUpdate();
            System.out.println("✅ Task added successfully.");
        } catch (SQLException e) {
            System.out.println("❌ Error adding task: " + e.getMessage());
        }
    }

    public static void updateTask(Task task) {
        String sql = "UPDATE tasks SET title = ?, description = ?, dueDate = ? WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setDate(3, Date.valueOf(task.getDueDate()));
            pstmt.setInt(4, task.getId());
            pstmt.executeUpdate();
            System.out.println("✅ Task updated successfully.");
        } catch (SQLException e) {
            System.out.println("❌ Error updating task: " + e.getMessage());
        }
    }

    public static boolean deleteTask(int id) {
        String checkSQL = "SELECT COUNT(*) FROM tasks WHERE id = ?";
        String deleteSQL = "DELETE FROM tasks WHERE id = ?";
        
        try (Connection conn = connect();
             PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSQL)) {

            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {  // Check if task exists
                deleteStmt.setInt(1, id);
                deleteStmt.executeUpdate();
                System.out.println("✅ Task deleted successfully.");
                return true;
            } else {
                System.out.println("❌ Task not found.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error deleting task: " + e.getMessage());
            return false;
        }
    }

}
    

   

   

