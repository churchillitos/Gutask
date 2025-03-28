package database;
import java.util.Random;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.geometry.Pos;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;	

public class TaskManager extends Application {
	private TaskManager taskManager;
	private VBox stickyNoteInputPanel; // Declare at the class level
    private static FlowPane taskGrid;
    private TextField searchField, titleField;
    private TextArea descriptionField;
    private DatePicker dueDatePicker;
    private static Connection conn;
    private ObservableList<Task> taskList = FXCollections.observableArrayList();
    private Map<Integer, String> taskColors = new HashMap<>(); // Store task colors

    @Override
    public void start(Stage primaryStage) {
        try {
            conn = Database.connect();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
            Parent root = loader.load();

            // ✅ Get the MainController instance from FXMLLoader
            MainController controller = loader.getController();
            controller.setTaskManager(this); // ✅ Pass the TaskManager instance

            Scene scene = new Scene(root);
            String css = getClass().getResource("/database/styles.css").toExternalForm(); // Fix path issue
            scene.getStylesheets().add(css);

            primaryStage.setTitle("GoTask - Task Manager");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();

            setupUI(root);

            // ✅ Ensure taskGrid is not null before calling loadTasks()
            if (taskGrid == null) {
                showAlert("Error", "Task grid is not found in FXML. Check your MainView.fxml file.");
            } else {
                loadTasks();
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load UI: " + e.getMessage());
        }
    }

    
    private void setupUI(Parent root) {
        taskGrid = (FlowPane) root.lookup("#taskGrid");
        searchField = (TextField) root.lookup("#searchField");

        // ✅ Find and use the existing input panel from the FXML
        stickyNoteInputPanel = (VBox) root.lookup("#stickyNoteInput");

        // Ensure the right panel contains the input panel (in case it was removed or not attached)
        VBox rightPanel = (VBox) root.lookup("#rightPanel");
        if (rightPanel != null && !rightPanel.getChildren().contains(stickyNoteInputPanel)) {
            rightPanel.getChildren().add(stickyNoteInputPanel);
        }

        // ✅ Assign UI elements to variables for event handling
        TextField titleField = (TextField) root.lookup("#titleField");
        TextArea descriptionField = (TextArea) root.lookup("#descriptionField");
        DatePicker dueDatePicker = (DatePicker) root.lookup("#dueDatePicker");
        Button addButton = (Button) root.lookup("#addButton");

        addButton.setOnAction(e -> {
            addTask(titleField.getText(), descriptionField.getText(), dueDatePicker.getValue());

            // ✅ Clear fields after adding a task
            titleField.setText("");
            descriptionField.setText("");
            dueDatePicker.setValue(null);
        });


        // Set up search button
        Button searchButton = (Button) root.lookup("#searchButton");
        searchButton.setOnAction(e -> searchTask());
    }
    
    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String query = "SELECT * FROM tasks";

        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                LocalDate dueDate = rs.getDate("dueDate").toLocalDate();
                tasks.add(new Task(id, title, description, dueDate));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    private static VBox createStickyNote(int id, String title, String description, String dueDate) {
        VBox note = new VBox();
        note.setPadding(new Insets(10));
        note.getStyleClass().add("sticky-note"); // Use CSS class

        // Slightly randomize the size for a more natural look
        double randomSize = 140 + new Random().nextInt(20); // Between 140 and 160
        note.setPrefSize(randomSize, randomSize);
        note.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", 16));
        titleLabel.setTextFill(Color.BLACK);
        note.getChildren().add(titleLabel);

        // Random sticky note colors
        String[] colors = {"#F7DC6F", "#F1948A", "#A9DFBF", "#85C1E9", "#D7BDE2"};
        note.setStyle("-fx-background-color: " + colors[new Random().nextInt(colors.length)] + ";");


        // Add random rotation to make it look like real sticky notes on a wall
        double randomRotation = (new Random().nextDouble() * 10) - 5; // Between -5 and 5 degrees
        note.setRotate(randomRotation);	

        // Set click event for task options
        note.setOnMouseClicked(event -> {
            openTaskOptions(id, title, description, dueDate);
        });

        return note;
    }
    
    private VBox createStickyNoteInputPanel() {
        // Generate a pastel sticky note color
        String[] colors = {"#F9E79F", "#AED6F1", "#F5B7B1", "#D5F5E3", "#FAD7A0"};
        String randomColor = colors[(int) (Math.random() * colors.length)];

        VBox stickyNote = new VBox(10);
        stickyNote.setPadding(new Insets(20, 25, 20, 25)); // Adjusted padding for better spacing
        stickyNote.setMaxWidth(380);
        stickyNote.setPrefHeight(350); // Set preferred height to fit the content
        stickyNote.setStyle("-fx-background-color: " + randomColor + "; "
                + "-fx-background-radius: 10px; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.3), 10, 0, 3, 3);");

        // Small label at the top
        Label header = new Label("Make New Sticky Note");
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Title Field
        Label titleLabel = new Label("Title:");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        titleField = new TextField();
        titleField.setPromptText("Enter task title...");
        titleField.setStyle("-fx-border-color: transparent; -fx-background-color: transparent; "
                + "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Description Field
        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        descriptionField = new TextArea();
        descriptionField.setPromptText("Enter task description...");
        descriptionField.setWrapText(true);
        descriptionField.setPrefRowCount(4);
        descriptionField.setMaxWidth(350);
        descriptionField.setMaxHeight(120);
        descriptionField.setStyle("-fx-control-inner-background: transparent; "
                + "-fx-border-color: transparent; -fx-padding: 5px; "
                + "-fx-font-size: 14px; -fx-text-fill: #333;");

        // Due Date Field with Calendar Icon
        Label dateLabel = new Label("Due Date:");
        dateLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        dueDatePicker = new DatePicker();
        dueDatePicker.setPromptText("Enter Due Date...");
        dueDatePicker.setStyle("-fx-font-size: 14px;");

        Label calendarIcon = new Label("📅");
        calendarIcon.setStyle("-fx-font-size: 20px; -fx-padding: 5px;");

        VBox dateContainer = new VBox(5, calendarIcon, dueDatePicker);
        dateContainer.setAlignment(Pos.CENTER_LEFT);

        // Add Task Button
        Button addButton = new Button("Add Task");
        addButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        addButton.setOnAction(e -> {
            taskManager.addTask(
                titleField.getText(), 
                descriptionField.getText(), 
                dueDatePicker.getValue()
            );
        });

        // Add all elements to the sticky note
        stickyNote.getChildren().addAll(header, titleLabel, titleField, descLabel, descriptionField, dateLabel, dateContainer, addButton);

        return stickyNote;
    }	
        
    private static void showTaskDetails(Task task) {
        Stage detailsStage = new Stage();
        detailsStage.initModality(Modality.APPLICATION_MODAL);
        detailsStage.initStyle(StageStyle.TRANSPARENT); // Transparent background

        // Generate a soft pastel color for a natural sticky note effect
        String[] colors = {"#F9E79F", "#AED6F1", "#F5B7B1", "#D5F5E3", "#FAD7A0"};
        String randomColor = colors[(int) (Math.random() * colors.length)];

        // Main sticky note layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20, 25, 15, 25)); // Reduced bottom padding
        layout.setMaxWidth(380);
        layout.setMaxHeight(350); // Set max height to prevent overflow
        layout.setStyle("-fx-background-color: " + randomColor + "; "
                      + "-fx-background-radius: 10px; "
                      + "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.3), 10, 0, 3, 3);");

        // Close Button
        Button closeButton = new Button("✖");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: black; -fx-font-size: 16px; -fx-cursor: hand;");
        closeButton.setOnAction(e -> detailsStage.close());

        // Modern Font Styling
        String modernFont = "'Arial', 'Segoe UI', 'Tahoma'";

        // Title
        Label titleLabel = new Label("Title:");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: " + modernFont + ";");
        Label titleValue = new Label(task.getTitle());
        titleValue.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-font-family: " + modernFont + "; -fx-text-fill: #333;");

        // Description
        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: " + modernFont + ";");
        Label descValue = new Label(task.getDescription());
        descValue.setWrapText(true);
        descValue.setMaxWidth(350);
        descValue.setStyle("-fx-font-size: 14px; -fx-font-family: " + modernFont + "; -fx-text-fill: #555;");

        // Due Date
        Label dateLabel = new Label("Due Date:");
        dateLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: " + modernFont + ";");
        Label dateValue = new Label(task.getDueDate().toString());
        dateValue.setStyle("-fx-font-size: 14px; -fx-font-style: italic; -fx-font-family: " + modernFont + "; -fx-text-fill: #777;");

        // Organizing Layout
        VBox closeContainer = new VBox();
        closeContainer.setAlignment(Pos.TOP_RIGHT);
        closeContainer.getChildren().add(closeButton);

        layout.getChildren().addAll(closeContainer, titleLabel, titleValue, descLabel, descValue, dateLabel, dateValue);

        // Full-screen StackPane (Detect clicks outside)
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0);"); // Transparent background
        root.setPrefSize(800, 600);
        root.getChildren().add(layout);

        // Force layout alignment to the center
        StackPane.setAlignment(layout, Pos.CENTER);

        // Scene setup
        Scene scene = new Scene(root, 800, 600, Color.TRANSPARENT);

        // Close when clicking outside the sticky note
        scene.setOnMouseClicked(event -> {
            if (!layout.getBoundsInParent().contains(event.getSceneX(), event.getSceneY())) {
                detailsStage.close();
            }
        });

        detailsStage.setScene(scene);
        detailsStage.show();
    }

    public static void loadTasks() {
        taskGrid.getChildren().clear(); // Clear existing sticky notes
        String query = "SELECT * FROM tasks";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int taskId = rs.getInt("id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                String dueDate = rs.getString("dueDate");

                // Create sticky note for each task
                VBox stickyNote = createStickyNote(taskId, title, description, dueDate);
                taskGrid.getChildren().add(stickyNote);
                taskGrid.setHgap(20); // Adds horizontal spacing between sticky notes
                taskGrid.setVgap(20); // Adds vertical spacing between sticky notes
                taskGrid.setPadding(new Insets(10)); // Ensures notes are not too close to edges
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load tasks: " + e.getMessage());
        }
    }
    private void populateTaskGrid(List<Task> tasks) {
        taskGrid.getChildren().clear();
        Random random = new Random();

        for (Task task : tasks) {
            VBox note = createStickyNote(task);

            // Random positioning within the Pane
            double maxX = taskGrid.getBoundsInLocal().getWidth() - 120;
            double maxY = taskGrid.getBoundsInLocal().getHeight() - 120;
            double posX = random.nextDouble() * maxX;
            double posY = random.nextDouble() * maxY;

            note.setLayoutX(posX);
            note.setLayoutY(posY);
           
            taskGrid.getChildren().add(note);
        }
    }


    private Task selectedTask = null;

    private void selectTask(Task task) {
        selectedTask = task;
        titleField.setText(task.getTitle());
        descriptionField.setText(task.getDescription());
        dueDatePicker.setValue(task.getDueDate());
    }


    public VBox createStickyNote(Task task) {
        VBox note = new VBox();
        note.setPadding(new Insets(10));
        note.getStyleClass().add("sticky-note"); // Use CSS class

        // 🎨 Slightly randomize the size for a more natural look
        double randomSize = 140 + new Random().nextInt(20); // Between 140 and 160
        note.setPrefSize(randomSize, randomSize);
        note.setAlignment(Pos.CENTER);

        Label title = new Label(task.getTitle());
        title.setFont(Font.font("Arial", 16));
        title.setTextFill(Color.BLACK);

        note.getChildren().add(title); // ✅ Only add title, removing description

     // Random sticky note colors
        String[] colors = {"#F7DC6F", "#F1948A", "#A9DFBF", "#85C1E9", "#D7BDE2"};
        note.setStyle("-fx-background-color: " + colors[new Random().nextInt(colors.length)] + ";");	

        // 🔄 Reduce rotation to prevent messy overlap
        double randomRotation = (new Random().nextDouble() * 6) - 3; // Between -3 and 3 degrees
        note.setRotate(randomRotation);

        // 🏷 Add margin to prevent overlap
        VBox.setMargin(note, new Insets(5, 5, 5, 5)); // Add spacing around each sticky note

        // 🎯 Open task options when clicked
        note.setOnMouseClicked(event -> {
            System.out.println("Sticky note clicked: " + task.getTitle());
            openTaskOptions(task.getId(), task.getTitle(), task.getDescription(), task.getDueDate().toString());
        });

        return note;
    }

    public static void openTaskOptions(int id, String title, String description, String dueDate) {
        Stage optionsStage = new Stage();
        optionsStage.initModality(Modality.APPLICATION_MODAL);
        optionsStage.initStyle(StageStyle.TRANSPARENT); // Transparent background

        // Generate a soft pastel color for a natural sticky note effect
        String[] colors = {"#F9E79F", "#AED6F1", "#F5B7B1", "#D5F5E3", "#FAD7A0"};
        String randomColor = colors[(int) (Math.random() * colors.length)];

        // Main sticky note layout (Smaller than showTaskDetails)
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(15, 20, 15, 20));
        layout.setMaxWidth(260);
        layout.setMaxHeight(210);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: " + randomColor + "; "
                      + "-fx-background-radius: 10px; "
                      + "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.3), 10, 0, 3, 3);");

        // Close Button (X)
        Button closeButton = new Button("✖");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: black; -fx-font-size: 14px; -fx-cursor: hand;");
        closeButton.setOnAction(e -> optionsStage.close());

        // Task Options Buttons
        Button showTaskButton = new Button("Show Task");
        Button editTaskButton = new Button("Edit Task");
        Button deleteTaskButton = new Button("Delete Task");

        // Styling buttons for a clean UI
        showTaskButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 180px; -fx-padding: 6px;");
        editTaskButton.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-size: 14px; -fx-pref-width: 180px; -fx-padding: 6px;");
        deleteTaskButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 180px; -fx-padding: 6px;");

        // Button Actions (Same as your original code)
        showTaskButton.setOnAction(e -> {
            optionsStage.close();
            showTaskDetails(new Task(id, title, description, LocalDate.parse(dueDate)));
        });

        editTaskButton.setOnAction(e -> {
            optionsStage.close();
            openEditTaskWindow(id, title, description, dueDate);
        });

        deleteTaskButton.setOnAction(e -> {
            optionsStage.close();
            confirmDeleteTask(id);
        });

        // Layout Organization
        VBox closeContainer = new VBox();
        closeContainer.setAlignment(Pos.TOP_RIGHT);
        closeContainer.getChildren().add(closeButton);

        VBox buttonContainer = new VBox(10, showTaskButton, editTaskButton, deleteTaskButton);
        buttonContainer.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(closeContainer, buttonContainer);

        // Full-screen StackPane (Detect clicks outside)
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0);"); // Transparent background
        root.setPrefSize(800, 600);
        root.getChildren().add(layout);

        // Force layout alignment to the center
        StackPane.setAlignment(layout, Pos.CENTER);

        // Scene setup
        Scene scene = new Scene(root, 800, 600, Color.TRANSPARENT);

        // Close when clicking outside the sticky note
        scene.setOnMouseClicked(event -> {
            if (!layout.getBoundsInParent().contains(event.getSceneX(), event.getSceneY())) {
                optionsStage.close();
            }
        });

        optionsStage.setScene(scene);
        optionsStage.show();
    }


    private static void openEditTaskWindow(int id, String title, String description, String dueDate) {
        Stage editStage = new Stage();
        editStage.initModality(Modality.APPLICATION_MODAL);
        editStage.initStyle(StageStyle.TRANSPARENT); // Transparent background

        // Generate a soft pastel color for a sticky note effect
        String[] colors = {"#F9E79F", "#AED6F1", "#F5B7B1", "#D5F5E3", "#FAD7A0"};
        String randomColor = colors[(int) (Math.random() * colors.length)];

        // Main sticky note layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20, 25, 15, 25));
        layout.setMaxWidth(380);
        layout.setMaxHeight(350);
        layout.setStyle("-fx-background-color: " + randomColor + "; "
                      + "-fx-background-radius: 10px; "
                      + "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.3), 10, 0, 3, 3);");

        // Close Button
        Button closeButton = new Button("✖");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: black; -fx-font-size: 16px; -fx-cursor: hand;");
        closeButton.setOnAction(e -> editStage.close());

        // Save Button
        Button saveButton = new Button("Save");
        saveButton.setStyle("-fx-background-color: #0078D4; -fx-text-fill: white; -fx-font-weight: bold;");

        // Font Styling
        String modernFont = "'Arial', 'Segoe UI', 'Tahoma'";

        // Title Label and Field
        Label titleLabel = new Label("Title:");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: " + modernFont + ";");

        TextField titleField = new TextField(title);
        titleField.setStyle("-fx-border-color: transparent; -fx-background-color: transparent; "
                          + "-fx-font-size: 22px; -fx-font-weight: bold; -fx-font-family: " + modernFont + "; -fx-text-fill: #333;");

        // Description Label and Transparent Field
        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: " + modernFont + ";");

        TextArea descriptionField = new TextArea(description);
        descriptionField.setWrapText(true);
        descriptionField.setPrefRowCount(4);
        descriptionField.setMaxWidth(350);
        descriptionField.setMaxHeight(120);
        descriptionField.setStyle("-fx-background-color: transparent; "
                                + "-fx-border-color: transparent; "
                                + "-fx-padding: 5px; -fx-font-size: 14px; "
                                + "-fx-font-family: " + modernFont + "; -fx-text-fill: #333;");
        descriptionField.setPromptText("Enter task description...");

        // Remove focus highlight (blue glow) when clicked
        descriptionField.setOnMouseClicked(event -> descriptionField.setStyle(
            "-fx-background-color: transparent; -fx-border-color: transparent; "
            + "-fx-padding: 5px; -fx-font-size: 14px; "
            + "-fx-font-family: " + modernFont + "; -fx-text-fill: #333;"
        ));

        // Due Date Label and Modern DatePicker
        Label dateLabel = new Label("Due Date:");
        dateLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: " + modernFont + ";");

        DatePicker dueDatePicker = new DatePicker();
        dueDatePicker.setStyle("-fx-background-color: transparent; "
                             + "-fx-border-color: transparent; "
                             + "-fx-font-size: 14px; "
                             + "-fx-font-family: " + modernFont + "; "
                             + "-fx-text-fill: #333;");

        // Ensure the DatePicker dropdown text is visible
        dueDatePicker.setOnShowing(event -> dueDatePicker.setStyle(
            "-fx-background-color: white; "
            + "-fx-border-color: #ccc; "
            + "-fx-font-size: 14px; "
            + "-fx-text-fill: black;"
        ));
        dueDatePicker.setOnHiding(event -> dueDatePicker.setStyle(
            "-fx-background-color: transparent; "
            + "-fx-border-color: transparent; "
            + "-fx-font-size: 14px; "
            + "-fx-text-fill: #333;"
        ));

        // Pre-fill DatePicker if a valid date exists
        if (dueDate != null && !dueDate.isEmpty()) {
            dueDatePicker.setValue(LocalDate.parse(dueDate));
        }

        // Save Button Action
        saveButton.setOnAction(e -> {
            updateTask(id, titleField.getText(), descriptionField.getText(), dueDatePicker.getValue());
            editStage.close();
        });

        // Close Button Layout
        VBox closeContainer = new VBox();
        closeContainer.setAlignment(Pos.TOP_RIGHT);
        closeContainer.getChildren().add(closeButton);

        // Layout structure
        layout.getChildren().addAll(closeContainer, titleLabel, titleField, descLabel, descriptionField, dateLabel, dueDatePicker, saveButton);

        // Full-screen StackPane (Detect clicks outside)
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0);"); // Transparent background
        root.setPrefSize(800, 600);
        root.getChildren().add(layout);

        // Ensure proper alignment
        StackPane.setAlignment(layout, Pos.CENTER);

        // Scene setup
        Scene scene = new Scene(root, 800, 600, Color.TRANSPARENT);

        // Close when clicking outside the sticky note
        scene.setOnMouseClicked(event -> {
            if (!layout.getBoundsInParent().contains(event.getSceneX(), event.getSceneY())) {
                editStage.close();
            }
        });

        editStage.setScene(scene);
        editStage.show();
    }
  
    private static void confirmDeleteTask(int taskId) {
        Stage confirmStage = new Stage();
        confirmStage.initModality(Modality.APPLICATION_MODAL);
        confirmStage.initStyle(StageStyle.TRANSPARENT);

        // Background color (light pink for delete confirmation)
        String backgroundColor = "#FADBD8";

        // Main layout (compact size)
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(15, 20, 15, 20));
        layout.setAlignment(Pos.CENTER);
        layout.setPrefSize(350, 150); // 🔹 Adjusted to match alert size
        layout.setStyle("-fx-background-color: " + backgroundColor + "; "
                      + "-fx-background-radius: 10px; "
                      + "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.3), 8, 0, 3, 3);");

        // Close Button (X)
        Button closeButton = new Button("✖");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: black; "
                            + "-fx-font-size: 12px; -fx-cursor: hand;");
        closeButton.setOnAction(e -> confirmStage.close());

        // Message Label
        Label messageLabel = new Label("Are you sure you want to delete this task?");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(280);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setStyle("-fx-font-size: 14px; "
                             + "-fx-font-family: 'Segoe UI', 'Arial', 'Tahoma'; "
                             + "-fx-font-weight: bold; "
                             + "-fx-text-fill: #333; "
                             + "-fx-text-alignment: center;");

        // OK Button
        Button okButton = new Button("OK");
        okButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
                         + "-fx-font-size: 12px; -fx-font-weight: bold; "
                         + "-fx-pref-width: 75px; -fx-background-radius: 5px;");
        okButton.setOnAction(e -> {
            deleteTask(taskId);
            confirmStage.close();
        });

        // Cancel Button
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: black; "
                             + "-fx-font-size: 12px; -fx-pref-width: 75px; -fx-background-radius: 5px;");
        cancelButton.setOnAction(e -> confirmStage.close());

        // Button Layout
        VBox buttonContainer = new VBox(10, okButton, cancelButton);
        buttonContainer.setAlignment(Pos.CENTER);

        // Close button layout
        VBox closeContainer = new VBox();
        closeContainer.setAlignment(Pos.TOP_RIGHT);
        closeContainer.getChildren().add(closeButton);

        layout.getChildren().addAll(closeContainer, messageLabel, buttonContainer);

        // Scene setup
        Scene scene = new Scene(layout, Color.TRANSPARENT);
        confirmStage.setScene(scene);
        confirmStage.show();
    }

    private static void showAlert(String title,String message) {
        Stage alertStage = new Stage();
        alertStage.initModality(Modality.APPLICATION_MODAL);
        alertStage.initStyle(StageStyle.TRANSPARENT);

        // Sticky note layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20, 25, 20, 25)); // Adjusted padding
        layout.setAlignment(Pos.CENTER);
        layout.setPrefSize(300, 150);
        layout.setStyle("-fx-background-color: #FFDDDD; "  // Light red background
                      + "-fx-background-radius: 10px; "
                      + "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.3), 10, 0, 3, 3);");

        // Close Button (Moved closer to the top-right corner)
        Button closeButton = new Button("✖");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: black; -fx-font-size: 16px; -fx-cursor: hand;");
        closeButton.setOnAction(e -> alertStage.close());

        // Force alignment higher in the corner
        StackPane closeContainer = new StackPane(closeButton);
        closeContainer.setAlignment(Pos.TOP_RIGHT);
        closeContainer.setPadding(new Insets(-30, -10, 0, 0)); // Move closer to top-right

        // Message Label
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #800000;"); // Dark red text

        // Layout structure
        layout.getChildren().addAll(closeContainer, messageLabel);

        // Scene and Stage
        StackPane root = new StackPane(layout);
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0);");
        
        Scene scene = new Scene(root, Color.TRANSPARENT);
        alertStage.setScene(scene);
        alertStage.show();
    }

    public void addTask(String title, String description, LocalDate dueDate) {
    	 System.out.println("📌 addTask() called in TaskManager");
    	    Thread.dumpStack(); // Prints the call stack
        if (title.isEmpty() || description.isEmpty() || dueDate == null) {
            showAlert("Error", "All fields must be filled.");
            return;
        }

        if (dueDate.isBefore(LocalDate.now())) {
            showAlert("Error", "Due date cannot be in the past.");
            return;
        }

        try {
            String sql = "INSERT INTO tasks (title, description, dueDate) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setDate(3, Date.valueOf(dueDate));
            stmt.executeUpdate();
            

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                taskColors.put(id, getRandomColor()); // Assign color to new task

                // ✅ ADD TASK TO THE UI
                Task newTask = new Task(id, title, description, dueDate);
                Platform.runLater(() -> taskGrid.getChildren().add(createStickyNote(newTask)));
             
            }           
            showAlert("Success", "Task added successfully.");           

        } catch (SQLException e) {
            showAlert("Error", "Failed to add task.");
        }
    }

    private static void updateTask(int id, String newTitle, String newDescription, LocalDate newDueDate) {
        if (newTitle.isEmpty() || newDescription.isEmpty() || newDueDate == null) {
            showAlert("Error", "All fields must be filled.");
            return;          
        }

        if (newDueDate.isBefore(LocalDate.now())) {
            showAlert("Error", "Due date cannot be in the past.");
            return;
        }

        try {
            String sql = "UPDATE tasks SET title = ?, description = ?, dueDate = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newTitle);
            stmt.setString(2, newDescription);
            stmt.setDate(3, Date.valueOf(newDueDate));
            stmt.setInt(4, id);
            stmt.executeUpdate();

            showSuccessMessage("Task updated successfully.");
            loadTasks(); // Refresh task list
        } catch (SQLException e) {
            showAlert("Error", "Failed to update task.");
        }
    }
    public static void showSuccessMessage(String message) {
        Stage successStage = new Stage();
        successStage.initModality(Modality.APPLICATION_MODAL);
        successStage.initStyle(StageStyle.TRANSPARENT); // Transparent background

        // Generate a soft pastel color for the sticky note effect
        String[] colors = {"#F9E79F", "#AED6F1", "#F5B7B1", "#D5F5E3", "#FAD7A0"};
        String randomColor = colors[(int) (Math.random() * colors.length)];

        // Main sticky note layout
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20, 25, 20, 25));
        layout.setAlignment(Pos.CENTER);
        layout.setMaxWidth(280);
        layout.setMaxHeight(150);
        layout.setStyle("-fx-background-color: " + randomColor + "; "
                      + "-fx-background-radius: 10px; "
                      + "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.3), 10, 0, 3, 3);");

        // Close Button (X)
        Button closeButton = new Button("✖");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: black; -fx-font-size: 14px; -fx-cursor: hand;");
        closeButton.setOnAction(e -> successStage.close());

     // Message Label
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(250);
        messageLabel.setAlignment(Pos.CENTER); // Center align text
        messageLabel.setStyle("-fx-font-size: 16px; "
                            + "-fx-font-family: 'Segoe UI', 'Arial', 'Tahoma'; "
                            + "-fx-font-weight: bold; "
                            + "-fx-text-fill: #333; "
                            + "-fx-text-alignment: center;"); // Apply text alignment

        // OK Button
        Button okButton = new Button("OK");
        okButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; "
                         + "-fx-font-size: 14px; -fx-font-weight: bold; "
                         + "-fx-pref-width: 100px; -fx-background-radius: 5px;");
        okButton.setOnAction(e -> successStage.close());

        // Layout adjustments
        VBox buttonContainer2 = new VBox(20, messageLabel, okButton); // Increased spacing
        buttonContainer2.setAlignment(Pos.CENTER);

        // Layout Organization
        VBox closeContainer = new VBox();
        closeContainer.setAlignment(Pos.TOP_RIGHT);
        closeContainer.getChildren().add(closeButton);

        VBox buttonContainer = new VBox(10, messageLabel, okButton);
        buttonContainer.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(closeContainer, buttonContainer);

        // Full-screen StackPane (Detect clicks outside)
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0);"); // Transparent background
        root.setPrefSize(800, 600);
        root.getChildren().add(layout);

        // Force layout alignment to the center
        StackPane.setAlignment(layout, Pos.CENTER);

        // Scene setup
        Scene scene = new Scene(root, 800, 600, Color.TRANSPARENT);

        // Close when clicking outside the sticky note
        scene.setOnMouseClicked(event -> {
            if (!layout.getBoundsInParent().contains(event.getSceneX(), event.getSceneY())) {
                successStage.close();
            }
        });

        successStage.setScene(scene);
        successStage.show();
    }


    private void searchTask() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadTasks();
            return;
        }

        taskList.clear();
        taskGrid.getChildren().clear();
        try {
            String sql = "SELECT * FROM tasks WHERE title LIKE ? OR description LIKE ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();

            boolean found = false;
            while (rs.next()) {
                found = true;
                taskList.add(new Task(rs.getInt("id"), rs.getString("title"), rs.getString("description"), rs.getDate("dueDate").toLocalDate()));
            }

            if (!found) {
                showAlert("No Results", "No tasks found matching: " + keyword);
            }

            populateTaskGrid(taskList);
        } catch (SQLException e) {
            showAlert("Error", "Failed to search tasks.");
        }
    }
    public static void deleteTask(int taskId) {
        String query = "DELETE FROM tasks WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                showAlert("Success", "Task deleted successfully!");
                loadTasks(); // Refresh UI
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to delete task: " + e.getMessage());
        }
    }

    private String getRandomColor() {
        String[] colors = {"#F7DC6F", "#F1948A", "#A9DFBF", "#85C1E9", "#D7BDE2"};
        return colors[new Random().nextInt(colors.length)];
    }

    public static void main(String[] args) {
        launch(args);
    }
}




