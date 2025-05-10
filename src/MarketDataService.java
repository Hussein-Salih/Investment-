import java.util.*; /**
 * Service class to provide market data and investment information
 */
public class MarketDataService {
    private List<Investment> investments;
    private final Random random = new Random();

    public MarketDataService() {
        // Initialize with some sample investments
        investments = new ArrayList<>();
        investments.add(new Investment(1, "Apple Inc.", "AAPL", 170.50, "Technology"));
        investments.add(new Investment(2, "Microsoft Corporation", "MSFT", 328.79, "Technology"));
        investments.add(new Investment(3, "Amazon.com Inc.", "AMZN", 145.20, "E-Commerce"));
        investments.add(new Investment(4, "Tesla Inc.", "TSLA", 230.45, "Automotive"));
        investments.add(new Investment(5, "Saudi Aramco", "2222.SR", 30.15, "Energy"));

        // Start price simulation
        startPriceSimulation();
    }

    public List<Investment> getAllInvestments() {
        return new ArrayList<>(investments);
    }

    public Investment getInvestmentById(int id) {
        return investments.stream()
                .filter(i -> i.getId() == id)
                .findFirst()
                .orElse(null);
    }

    private void startPriceSimulation() {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updatePrices();
            }
        }, 5000, 5000); // Update every 5 seconds
    }

    private void updatePrices() {
        for (Investment investment : investments) {
            // Simulate price changes (between -3% and +3%)
            double changePercent = (random.nextDouble() * 6) - 3;
            double newPrice = investment.getCurrentPrice() * (1 + (changePercent / 100));
            // Ensure price doesn't go below 0.01
            newPrice = Math.max(0.01, newPrice);
            investment.setCurrentPrice(newPrice);
        }
    }
}
