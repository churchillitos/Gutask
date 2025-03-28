package database;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;	
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.shape.Rectangle;
import javafx.event.ActionEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.animation.Animation;

public class MainController {
	@FXML
	private VBox stickyNoteInput;
	@FXML
	private HBox topBar; 
	@FXML
	private ScrollPane taskScrollPane;
    @FXML
    private TaskManager taskManager;
    @FXML
    private FlowPane taskGrid;
    @FXML
    private TextField titleField, searchField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private DatePicker dueDatePicker;
    @FXML
    private Button searchButton, addButton, editTaskButton, deleteTaskButton, showTaskButton, clearButton;
    @FXML
    private Label messageLabel, themeIcon;
    @FXML
    private BorderPane mainPane;
    @FXML
    private VBox rightPanel;
    
    private Task selectedTask;
    
    private boolean isDarkMode = false;  
    
    private Timeline hideScrollbarTimer;
    
    @FXML
    private ToggleButton themeToggle;

    @FXML
    public void initialize() {  	
    	    // Ensure correct spacing and padding on startup
    	    Platform.runLater(() -> {
    	        topBar.applyCss();
    	        topBar.layout();
    	    });
    	    topBar.getStyleClass().add("light-mode"); // Add light-mode class  	   	    	
    	// Add a listener to detect scrolling
        taskScrollPane.setOnScroll(event -> {
        	taskScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            showScrollbar();
            
            if (hideScrollbarTimer.getStatus() == Animation.Status.RUNNING) {
                hideScrollbarTimer.stop();
            }
            hideScrollbarTimer.playFromStart();
            resetHideScrollbarTimer();
        });

        // Also detect when using the scroll bar directly
        taskScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            showScrollbar();
            resetHideScrollbarTimer();
        });

        taskScrollPane.hvalueProperty().addListener((obs, oldVal, newVal) -> {
            showScrollbar();
            resetHideScrollbarTimer();
        });

        // Set up a timer to hide the scrollbar after scrolling stops
        hideScrollbarTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            Platform.runLater(() -> {              
                hideScrollbar();
            });
        }));
        hideScrollbarTimer.setCycleCount(1);
        hideScrollbarTimer.play();
        
        isDarkMode = false;       
        
        titleField.focusedProperty().addListener((obs, oldVal, newVal) -> updateShadowState());
        descriptionField.focusedProperty().addListener((obs, oldVal, newVal) -> updateShadowState());
        dueDatePicker.focusedProperty().addListener((obs, oldVal, newVal) -> updateShadowState());
    

        if (messageLabel != null) {
            messageLabel.setVisible(false);
        }
        taskGrid.setPrefSize(500, 600);
        loadTasks();
        assert titleField != null : "titleField is not injected. Check FXML.";
        assert descriptionField != null : "descriptionField is not injected. Check FXML.";
        assert dueDatePicker != null : "dueDatePicker is not injected. Check FXML.";
    }
    
    private void updateShadowState() {
        boolean isInputFocused = titleField.isFocused() || 
                                descriptionField.isFocused() || 
                                dueDatePicker.isFocused();
        
        if (isInputFocused) {
            mainPane.getStyleClass().add("input-focused");
        } else {
            mainPane.getStyleClass().remove("input-focused");
        }
    }
    
 // Show the scrollbar by adding a class
    private void showScrollbar() {
        taskScrollPane.getStyleClass().add("show-scroll");
    }
    
 // Hide the scrollbar after a delay
    private void hideScrollbar() {
        taskScrollPane.getStyleClass().remove("show-scroll");        
        taskScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }
    
 // Restart the timer when scrolling happens
    private void resetHideScrollbarTimer() {
        hideScrollbarTimer.stop();
        hideScrollbarTimer.playFromStart();
    }



    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    private void loadTasks() {
        if (taskGrid == null) {
            System.out.println("⚠️ taskGrid is null. Check FXML fx:id.");
            return;
        }
        taskGrid.getChildren().clear();

        List<Task> tasks = Database.getAllTasks();
        System.out.println("Loaded tasks count: " + tasks.size());

        for (Task task : tasks) {
            Node stickyNote = createStickyNote(task);

            if (stickyNote == null) {
                System.out.println("❌ Error: createStickyNote returned null for task: " + task.getTitle());
            } else {
                System.out.println("✅ Adding sticky note: " + task.getTitle() + " | Type: " + stickyNote.getClass().getSimpleName());
                taskGrid.getChildren().add(stickyNote);
            }
        }
    }
    
    @FXML
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        
        // Remove ALL instances of light-mode and dark-mode classes
        topBar.getStyleClass().removeAll("light-mode", "dark-mode");

        if (isDarkMode) {
            mainPane.setStyle("-fx-background-color: #444444;");         
            taskGrid.setStyle("-fx-background-color: #525252;");
            
            stickyNoteInput.getStyleClass().remove("light-mode");
            stickyNoteInput.getStyleClass().add("dark-mode");

            topBar.getStyleClass().remove("light-mode");
            topBar.getStyleClass().add("dark-mode");

            taskScrollPane.getStyleClass().remove("light-mode");
            taskScrollPane.getStyleClass().add("dark-mode");

            themeToggle.setSelected(true); // Slide to moon position
        } else {
            mainPane.setStyle("-fx-background-color: #ECF0F1;");           
            taskGrid.setStyle("-fx-background-color: #FFFFFF;");
            
            stickyNoteInput.getStyleClass().remove("dark-mode");
            stickyNoteInput.getStyleClass().add("light-mode");

            topBar.getStyleClass().remove("dark-mode");
            topBar.getStyleClass().add("light-mode");

            taskScrollPane.getStyleClass().remove("dark-mode");
            taskScrollPane.getStyleClass().add("light-mode");

            themeToggle.setSelected(false); // Slide to sun position
        }

        // Force JavaFX to refresh styles properly
        Platform.runLater(() -> {
            mainPane.applyCss();
            mainPane.layout();
            topBar.applyCss();
            topBar.layout();
            taskScrollPane.applyCss();
            taskScrollPane.layout();
            stickyNoteInput.applyCss();
            stickyNoteInput.layout();
        });
    }
    
    public boolean isDarkMode() {
        return isDarkMode;
    }

    @FXML
    private void searchTask() {
        if (taskGrid == null) {
            System.out.println("⚠️ taskGrid is null. Check FXML fx:id.");
            return;
        }

        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            loadTasks();
            return;
        }

        List<Task> filteredTasks = Database.getAllTasks().stream()
            .filter(task -> task.getTitle().toLowerCase().contains(query))
            .collect(Collectors.toList());

        taskGrid.getChildren().clear();

        for (Task task : filteredTasks) {
            StackPane stickyNote = createStickyNote(task);
            stickyNote.setOnMouseClicked(event -> {
                TaskManager.openTaskOptions(task.getId(), task.getTitle(), task.getDescription(), task.getDueDate().toString());
            });
            taskGrid.getChildren().add(stickyNote);
        }
    }

    private void attachClickHandler(StackPane stickyNote, Task task) {
        stickyNote.setOnMouseClicked(e -> {
            titleField.setText(task.getTitle());
            descriptionField.setText(task.getDescription());
            dueDatePicker.setValue(task.getDueDate());
        });
    }

    private StackPane createStickyNote(Task task) {
        StackPane note = new StackPane();
        note.setPrefSize(200, 150);

        Rectangle bg = new Rectangle(200, 150);
        bg.setFill(Color.LIGHTYELLOW);
        bg.setStroke(null);

        Text text = new Text(task.getTitle());
        text.setWrappingWidth(180);

        note.getChildren().addAll(bg, text);
        note.getStyleClass().add("sticky-note");

        System.out.println("Adding sticky note: " + task.getTitle() + " | Style Classes: " + note.getStyleClass());

        note.setOnMouseClicked(event -> {
            selectedTask = task;
            showMessage("✔ Task Selected: " + task.getTitle(), Color.BLUE);
            titleField.setText(task.getTitle());
            descriptionField.setText(task.getDescription());
            dueDatePicker.setValue(task.getDueDate());
        });

        return note;
    }

    @FXML
    private void addTask() {
        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        LocalDate dueDate = dueDatePicker.getValue();

        if (title.isEmpty() || dueDate == null) {
            showMessage("❌ Title and Due Date are required!", Color.RED);
            return;
        }

        if (dueDate.isBefore(LocalDate.now())) {
            showMessage("❌ Due date cannot be in the past!", Color.RED);
            return;
        }

        try {
            taskManager.addTask(title, description, dueDate);
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void editTask(ActionEvent event) {
        if (selectedTask == null) {
            showMessage("❌ No task selected!", Color.RED);
            return;
        }
        openEditWindow(selectedTask);
    }

    private void openEditWindow(Task task) {
        titleField.setText(task.getTitle());
        descriptionField.setText(task.getDescription());
        dueDatePicker.setValue(task.getDueDate());
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Edit Task");
        alert.setHeaderText("Save changes?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            task.setTitle(titleField.getText().trim());
            task.setDescription(descriptionField.getText().trim());
            task.setDueDate(dueDatePicker.getValue());
            Database.updateTask(task);
            loadTasks();
            showMessage("✅ Task updated!", Color.GREEN);
        }
    }

    @FXML
    private void deleteTask(ActionEvent event) {
        if (selectedTask == null) {
            showMessage("❌ No task selected!", Color.RED);
            return;
        }
        confirmAndDelete(selectedTask);
    }

    private void confirmAndDelete(Task task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Task");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Task: " + task.getTitle());
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Database.deleteTask(task.getId());
            loadTasks();
            showMessage("✅ Task deleted!", Color.GREEN);
            selectedTask = null;
        }
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        taskGrid.getChildren().clear();

        List<Task> tasks = Database.getAllTasks();

        for (Task task : tasks) {
            Node stickyNote = taskManager.createStickyNote(task);
            taskGrid.getChildren().add(stickyNote);
        }
    }

    private void openTaskOptions(int id, String title, String description, String dueDate) {
        taskManager = new TaskManager();
        System.out.println("Opening task options for: " + title);

        if (taskManager != null) {
            taskManager.openTaskOptions(id, title, description, dueDate);
        } else {
            System.err.println("Error: TaskManager instance is null!");
        }
    }

    private void showMessage(String message, Color color) {
        messageLabel.setText(message);
        messageLabel.setTextFill(color);
        messageLabel.setVisible(true);
    }

    private void clearFields() {
        System.out.println("🧹 clearFields() called!");
        titleField.clear();
        descriptionField.clear();
        dueDatePicker.setValue(null);
    }
}