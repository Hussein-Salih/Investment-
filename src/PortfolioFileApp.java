import java.io.*;
import java.util.*;

// Class to represent an investment
class Investment {
    String userId;
    String symbol;
    int quantity;

    public Investment(String userId, String symbol, int quantity) {
        this.userId = userId;
        this.symbol = symbol;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return symbol + " (" + quantity + " shares)";
    }

    public String toFileString() {
        return userId + "," + symbol + "," + quantity;
    }

    public static Investment fromFileString(String line) {
        String[] parts = line.split(",");
        return new Investment(parts[0], parts[1], Integer.parseInt(parts[2]));
    }
}

// File-based "database" class
class FileDatabase {
    private static final String FILE_NAME = "portfolio.txt";

    public static void saveInvestment(Investment investment) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            writer.write(investment.toFileString());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public static List<Investment> getPortfolio(String userId) {
        List<Investment> portfolio = new ArrayList<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) return portfolio;

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Investment inv = Investment.fromFileString(line);
                if (inv.userId.equals(userId)) {
                    portfolio.add(inv);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return portfolio;
    }
}

// Business logic class
class PortfolioManager {
    public List<Investment> addToPortfolio(String userId, String symbol, int quantity) {
        Investment investment = new Investment(userId, symbol, quantity);
        FileDatabase.saveInvestment(investment);
        return FileDatabase.getPortfolio(userId);
    }
}

// Controller class
class ApplicationController {
    private final PortfolioManager portfolioManager = new PortfolioManager();

    public List<Investment> handleAddInvestment(String userId, String symbol, int quantity) {
        return portfolioManager.addToPortfolio(userId, symbol, quantity);
    }
}

// Simulated Web UI
public class PortfolioFileApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ApplicationController controller = new ApplicationController();

        System.out.print("Enter your user ID: ");
        String userId = scanner.nextLine().trim();

        System.out.print("Enter stock symbol to add: ");
        String symbol = scanner.nextLine().trim().toUpperCase();

        System.out.print("Enter quantity: ");
        int quantity = scanner.nextInt();

        List<Investment> updatedPortfolio = controller.handleAddInvestment(userId, symbol, quantity);

        System.out.println("\n✅ Updated Portfolio:");
        for (Investment inv : updatedPortfolio) {
            System.out.println(" - " + inv);
        }

        scanner.close();
    }
}
