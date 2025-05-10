/**
 * تمثل استثمارًا في النظام
 */
public class Investment {
    private int id;
    private String name;
    private String symbol;
    private double currentPrice;
    private double openPrice;
    private String description;
    private String category;

    public Investment(int id, String name, String symbol, double currentPrice, String category) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.openPrice = currentPrice;
        this.category = category;
    }

    // الدوال get و set
    public int getId() { return id; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getOpenPrice() { return openPrice; }
    public void setOpenPrice(double openPrice) { this.openPrice = openPrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }

    @Override
    public String toString() {
        return name + " (" + symbol + "): $" + String.format("%.2f", currentPrice);
    }
}
