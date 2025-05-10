import java.util.*;


class MarketDataService {
    public Map<String, Double> getLatestMarketData() {
        Map<String, Double> data = new HashMap<>();
        data.put("AAPL", 170.25);
        data.put("GOOGL", 2850.60);
        data.put("TSLA", 750.20);
        return data;
    }
}


class UserProfile {
    public String riskTolerance;
    public int investmentHorizon;

    public UserProfile(String riskTolerance, int investmentHorizon) {
        this.riskTolerance = riskTolerance;
        this.investmentHorizon = investmentHorizon;
    }
}


class InvestmentAdvisor {
    private MarketDataService marketDataService = new MarketDataService();

    public List<String> getRecommendations(UserProfile profile) {
        Map<String, Double> marketData = marketDataService.getLatestMarketData();
        List<String> recommendations = new ArrayList<>();

        if ("high".equalsIgnoreCase(profile.riskTolerance)) {
            if (marketData.containsKey("TSLA")) recommendations.add("Buy TSLA");
        }
        if ("medium".equalsIgnoreCase(profile.riskTolerance)) {
            if (marketData.containsKey("AAPL")) recommendations.add("Buy AAPL");
        }
        if ("low".equalsIgnoreCase(profile.riskTolerance)) {
            if (marketData.containsKey("GOOGL")) recommendations.add("Buy GOOGL");
        }

        return recommendations;
    }
}


class ApplicationController {
    private InvestmentAdvisor advisor = new InvestmentAdvisor();

    public List<String> handleRecommendationRequest(UserProfile profile) {
        return advisor.getRecommendations(profile);
    }
}


public class InvestmentApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Simulate user input
        System.out.print("Enter risk tolerance (low, medium, high): ");
        String risk = scanner.nextLine();

        System.out.print("Enter investment horizon (in years): ");
        int horizon = scanner.nextInt();

        UserProfile profile = new UserProfile(risk, horizon);

        // Simulate Web UI calling ApplicationController
        ApplicationController controller = new ApplicationController();
        List<String> recommendations = controller.handleRecommendationRequest(profile);

        // Display to UI
        System.out.println("Recommended Investments:");
        for (String rec : recommendations) {
            System.out.println(" - " + rec);
        }

        scanner.close();
    }
}
