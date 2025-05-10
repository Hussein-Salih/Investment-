import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

// User Model
class User implements Serializable {
    String email;
    String password;
    String preferences;

    public User(String email, String password, String preferences) {
        this.email = email;
        this.password = password;
        this.preferences = preferences;
    }

    @Override
    public String toString() {
        return email + "," + password + "," + preferences;
    }

    public static User fromString(String line) {
        String[] parts = line.split(",", 3);
        return new User(parts[0], parts[1], parts.length > 2 ? parts[2] : "");
    }
}

// File-Based User Database
class UserDatabase {
    private static final String FILE_PATH = "users.txt";
    private static final Map<String, User> users = new HashMap<>();

    static {
        loadUsersFromFile();
    }

    public static boolean emailExists(String email) {
        return users.containsKey(email);
    }

    public static void saveUser(User user) {
        users.put(user.email, user);
        saveUsersToFile();
    }

    public static User getUser(String email) {
        return users.get(email);
    }

    private static void loadUsersFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                User user = User.fromString(line);
                users.put(user.email, user);
            }
        } catch (IOException e) {
            System.out.println("Error loading users: " + e.getMessage());
        }
    }

    private static void saveUsersToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (User user : users.values()) {
                writer.write(user.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving users: " + e.getMessage());
        }
    }
}

// Authentication Service
class AuthenticationService {
    public static String registerUser(String email, String password, String preferences) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return "Invalid input. Email and password are required.";
        }

        if (UserDatabase.emailExists(email)) {
            return "Email already registered.";
        }

        User newUser = new User(email, password, preferences);
        UserDatabase.saveUser(newUser);
        return "Registration Successful";
    }

    public static String loginUser(String email, String password) {
        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            return "Invalid login input.";
        }

        User user = UserDatabase.getUser(email);
        if (user == null) {
            return "User not found.";
        }

        if (!user.password.equals(password)) {
            return "Incorrect password.";
        }

        return "Login Successful. Welcome, " + email + "!";
    }
}

// Main App
 class MainApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Welcome ===");
            System.out.println("1 - Register");
            System.out.println("2 - Login");
            System.out.println("0 - Exit");
            System.out.print("Enter your choice: ");
            int choice;

            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            if (choice == 1) {
                System.out.println("=== User Registration ===");
                System.out.print("Enter Email: ");
                String email = scanner.nextLine();

                System.out.print("Enter Password: ");
                String password = scanner.nextLine();

                System.out.print("Enter Preferences: ");
                String preferences = scanner.nextLine();

                String response = AuthenticationService.registerUser(email, password, preferences);
                System.out.println(response);

            } else if (choice == 2) {
                System.out.println("=== User Login ===");
                System.out.print("Enter Email: ");
                String email = scanner.nextLine();

                System.out.print("Enter Password: ");
                String password = scanner.nextLine();

                String response = AuthenticationService.loginUser(email, password);
                System.out.println(response);

            } else if (choice == 0) {
                System.out.println("Goodbye!");
                break;

            } else {
                System.out.println("Invalid choice.");
            }
        }

        scanner.close();
    }
}
