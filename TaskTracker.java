import java.util.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TaskTracker {

    static String FILE_NAME = "tasks.json";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<Task> tasks = loadTasks();

        while (true) {
            System.out.println("\nWhat do you want to do?");
            System.out.println("1. View tasks");
            System.out.println("2. Add a task");
            System.out.println("3. Delete a task");
            System.out.println("4. Mark task in-progress");
            System.out.println("5. Mark task done");
            System.out.println("6. Exit");
            System.out.println("7. List tasks by status");
            System.out.print("Enter choice (1-7): ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1":
                    showTasks(tasks);
                    break;
                case "2":
                    System.out.print("Enter new task description: ");
                    String desc = sc.nextLine();
                    int id = tasks.size() + 1;
                    tasks.add(new Task(id, desc));
                    saveTasks(tasks);
                    System.out.println("Task added!");
                    break;
                case "3":
                    showTasks(tasks);
                    System.out.print("Enter task number to delete: ");
                    int numDel = Integer.parseInt(sc.nextLine());
                    if (numDel >= 1 && numDel <= tasks.size()) {
                        Task removed = tasks.remove(numDel - 1);
                        saveTasks(tasks);
                        System.out.println("Deleted: " + removed.description);
                        // Re-assign IDs
                        for (int i = 0; i < tasks.size(); i++)
                            tasks.get(i).id = i + 1;
                    } else {
                        System.out.println("Invalid number.");
                    }
                    break;
                case "4":
                    showTasks(tasks);
                    System.out.print("Enter task number to mark in-progress: ");
                    int numProg = Integer.parseInt(sc.nextLine());
                    if (numProg >= 1 && numProg <= tasks.size()) {
                        tasks.get(numProg - 1).markInProgress();
                        saveTasks(tasks);
                        System.out.println("Task marked in-progress!");
                    } else {
                        System.out.println("Invalid number.");
                    }
                    break;
                case "5":
                    showTasks(tasks);
                    System.out.print("Enter task number to mark done: ");
                    int numDone = Integer.parseInt(sc.nextLine());
                    if (numDone >= 1 && numDone <= tasks.size()) {
                        tasks.get(numDone - 1).markDone();
                        saveTasks(tasks);
                        System.out.println("Task marked done!");
                    } else {
                        System.out.println("Invalid number.");
                    }
                    break;
                case "6":
                    System.out.println("Goodbye!");
                    sc.close();
                    return;
                case "7":
                    System.out.print("Enter status to filter (todo, in-progress, done): ");
                    String statusFilter = sc.nextLine().toLowerCase();
                    listTasksByStatus(tasks, statusFilter);
                    break;
                default:
                    System.out.println("Please enter a valid option (1-7).");
                    break;
            }
        }
    }

    static class Task {
        int id;
        String description;
        String status;
        String createdAt;
        String updatedAt;

        Task(int id, String description) {
            this.id = id;
            this.description = description;
            this.status = "todo";
            this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.updatedAt = this.createdAt;
        }

        void markInProgress() {
            this.status = "in-progress";
            this.updatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        void markDone() {
            this.status = "done";
            this.updatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        @Override
        public String toString() {
            return id + ". [" + status + "] " + description + " (Created: " + createdAt + ", Updated: " + updatedAt
                    + ")";
        }
    }

    private static void showTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("No tasks yet.");
        } else {
            for (Task t : tasks) {
                System.out.println(t);
            }
        }
    }

    private static void listTasksByStatus(List<Task> tasks, String status) {
        boolean found = false;
        for (Task t : tasks) {
            if (t.status.equalsIgnoreCase(status)) {
                System.out.println(t);
                found = true;
            }
        }
        if (!found) {
            System.out.println("No tasks found with status: " + status);
        }
    }

    private static List<Task> loadTasks() {
        List<Task> tasks = new ArrayList<>();
        File file = new File(FILE_NAME);
        if (!file.exists())
            return tasks;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String json = br.lines().reduce("", String::concat).trim();
            if (json.isEmpty() || json.equals("[]"))
                return tasks;

            json = json.substring(1, json.length() - 1); // remove [ and ]
            String[] items = json.split("\\},\\{");

            for (String item : items) {
                try {
                    if (!item.startsWith("{"))
                        item = "{" + item;
                    if (!item.endsWith("}"))
                        item = item + "}";

                    Map<String, String> map = new HashMap<>();
                    String inner = item.substring(1, item.length() - 1);
                    String[] fields = inner.split("\",\"");
                    for (String f : fields) {
                        String[] kv = f.replace("\"", "").split(":", 2);
                        if (kv.length == 2)
                            map.put(kv[0], kv[1]);
                    }

                    int id = Integer.parseInt(map.getOrDefault("id", "0"));
                    String desc = map.getOrDefault("description", "");
                    Task t = new Task(id, desc);
                    t.status = map.getOrDefault("status", "todo");
                    t.createdAt = map.getOrDefault("createdAt", LocalDateTime.now().toString());
                    t.updatedAt = map.getOrDefault("updatedAt", t.createdAt);
                    tasks.add(t);
                } catch (Exception ex) {
                    // skip corrupted task
                    System.out.println("Skipping corrupted task entry");
                }
            }

        } catch (IOException e) {
            System.out.println("Error loading tasks: " + e.getMessage());
        }

        return tasks;
    }

    private static void saveTasks(List<Task> tasks) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            bw.write("[\n");
            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                bw.write(String.format(
                        "  {\"id\":%d,\"description\":\"%s\",\"status\":\"%s\",\"createdAt\":\"%s\",\"updatedAt\":\"%s\"}",
                        t.id, t.description.replace("\"", "\\\""), t.status, t.createdAt, t.updatedAt));
                if (i != tasks.size() - 1)
                    bw.write(",");
                bw.write("\n");
            }
            bw.write("]");
        } catch (IOException e) {
            System.out.println("Error saving tasks: " + e.getMessage());
        }
    }
}
