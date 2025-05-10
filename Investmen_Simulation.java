import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;
import java.text.DecimalFormat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class investment extends JFrame {
    public static void main(String[] args) {
        // Make sure JDBC driver (SQLite) is loaded
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found. Please add sqlite-jdbc-3.xx.jar to your classpath.");
            return;
        }

        // Start the application
        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseManager.getInstance(); // Initialize database
                new investment().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error starting application: " + e.getMessage());
            }
        });
    }

    private SimulationPanel simulationPanel;

    public investment() {
        setTitle("Investment Simulator");
        setSize(900, 600);
        setMinimumSize(new Dimension(800, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Add main panel
        simulationPanel = new SimulationPanel();
        add(simulationPanel);
    }

    // Investment model
    static class Investment {
        private int id;
        private String name;
        private String symbol;
        private String type;
        private double currentPrice;
        private double historicVolatility;
        private double annualReturn;

        public Investment(int id, String name, String symbol, String type,
                          double currentPrice, double historicVolatility, double annualReturn) {
            this.id = id;
            this.name = name;
            this.symbol = symbol;
            this.type = type;
            this.currentPrice = currentPrice;
            this.historicVolatility = historicVolatility;
            this.annualReturn = annualReturn;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getSymbol() { return symbol; }
        public String getType() { return type; }
        public double getCurrentPrice() { return currentPrice; }
        public double getHistoricVolatility() { return historicVolatility; }
        public double getAnnualReturn() { return annualReturn; }

        @Override
        public String toString() {
            return name + " (" + symbol + ")";
        }
    }

    // Simulation Result model
    static class SimulationResult {
        private double initialInvestment;
        private int periodInYears;
        private Investment investment;
        private LocalDate startDate;
        private LocalDate endDate;
        private double finalValueOptimistic;
        private double finalValueModerate;
        private double finalValuePessimistic;
        private List<DataPoint> dataPoints;

        public SimulationResult(double initialInvestment, int periodInYears, Investment investment) {
            this.initialInvestment = initialInvestment;
            this.periodInYears = periodInYears;
            this.investment = investment;
            this.startDate = LocalDate.now();
            this.endDate = startDate.plusYears(periodInYears);
            this.dataPoints = new ArrayList<>();
        }

        // Data point for chart
        public static class DataPoint {
            private LocalDate date;
            private double optimisticValue;
            private double moderateValue;
            private double pessimisticValue;

            public DataPoint(LocalDate date, double optimisticValue, double moderateValue, double pessimisticValue) {
                this.date = date;
                this.optimisticValue = optimisticValue;
                this.moderateValue = moderateValue;
                this.pessimisticValue = pessimisticValue;
            }

            public LocalDate getDate() { return date; }
            public double getOptimisticValue() { return optimisticValue; }
            public double getModerateValue() { return moderateValue; }
            public double getPessimisticValue() { return pessimisticValue; }
        }

        public double getInitialInvestment() { return initialInvestment; }
        public int getPeriodInYears() { return periodInYears; }
        public Investment getInvestment() { return investment; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }

        public double getFinalValueOptimistic() { return finalValueOptimistic; }
        public void setFinalValueOptimistic(double finalValueOptimistic) {
            this.finalValueOptimistic = finalValueOptimistic;
        }

        public double getFinalValueModerate() { return finalValueModerate; }
        public void setFinalValueModerate(double finalValueModerate) {
            this.finalValueModerate = finalValueModerate;
        }

        public double getFinalValuePessimistic() { return finalValuePessimistic; }
        public void setFinalValuePessimistic(double finalValuePessimistic) {
            this.finalValuePessimistic = finalValuePessimistic;
        }

        public List<DataPoint> getDataPoints() { return dataPoints; }
        public void addDataPoint(DataPoint point) {
            this.dataPoints.add(point);
        }
    }

    // Database management
    static class DatabaseManager {
        private static final String DB_URL = "jdbc:sqlite:investment_app.db";
        private static DatabaseManager instance;

        private DatabaseManager() {
            initDatabase();
        }

        public static synchronized DatabaseManager getInstance() {
            if (instance == null) {
                instance = new DatabaseManager();
            }
            return instance;
        }

        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(DB_URL);
        }

        private void initDatabase() {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                // Create investments table if it doesn't exist
                String createInvestmentTable =
                        "CREATE TABLE IF NOT EXISTS investments (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "name TEXT NOT NULL," +
                                "symbol TEXT NOT NULL," +
                                "type TEXT NOT NULL," +
                                "current_price REAL NOT NULL," +
                                "historic_volatility REAL NOT NULL," +
                                "annual_return REAL NOT NULL" +
                                ");";
                stmt.execute(createInvestmentTable);

                // Insert sample data if the table is empty
                if (stmt.executeQuery("SELECT COUNT(*) FROM investments").getInt(1) == 0) {
                    insertSampleData(conn);
                }
            } catch (SQLException e) {
                System.err.println("Error initializing database: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void insertSampleData(Connection conn) throws SQLException {
            Statement stmt = conn.createStatement();

            // Sample investment data
            String[] insertStatements = {
                    "INSERT INTO investments (name, symbol, type, current_price, historic_volatility, annual_return) " +
                            "VALUES ('Apple Inc.', 'AAPL', 'STOCK', 170.50, 0.25, 0.15)",

                    "INSERT INTO investments (name, symbol, type, current_price, historic_volatility, annual_return) " +
                            "VALUES ('S&P 500 ETF', 'SPY', 'ETF', 420.00, 0.16, 0.10)",

                    "INSERT INTO investments (name, symbol, type, current_price, historic_volatility, annual_return) " +
                            "VALUES ('10-Year Treasury Bond', 'BOND10', 'BOND', 95.00, 0.05, 0.035)",

                    "INSERT INTO investments (name, symbol, type, current_price, historic_volatility, annual_return) " +
                            "VALUES ('Bitcoin', 'BTC', 'CRYPTO', 36000.00, 0.75, 0.30)",

                    "INSERT INTO investments (name, symbol, type, current_price, historic_volatility, annual_return) " +
                            "VALUES ('Real Estate Fund', 'REIT', 'FUND', 210.75, 0.18, 0.08)"
            };

            for (String sql : insertStatements) {
                stmt.execute(sql);
            }
        }
    }

    // Retrieving investment data from database
    static class InvestmentDao {
        private final DatabaseManager dbManager;

        public InvestmentDao() {
            this.dbManager = DatabaseManager.getInstance();
        }

        public List<Investment> getAllInvestments() {
            List<Investment> investments = new ArrayList<>();
            String sql = "SELECT * FROM investments";

            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    Investment investment = new Investment(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("symbol"),
                            rs.getString("type"),
                            rs.getDouble("current_price"),
                            rs.getDouble("historic_volatility"),
                            rs.getDouble("annual_return")
                    );
                    investments.add(investment);
                }
            } catch (SQLException e) {
                System.err.println("Error fetching investments: " + e.getMessage());
                e.printStackTrace();
            }

            return investments;
        }
    }

    // Market data service
    static class MarketDataService {
        private final InvestmentDao investmentDao;

        public MarketDataService() {
            this.investmentDao = new InvestmentDao();
        }

        public List<Investment> getAllInvestments() {
            return investmentDao.getAllInvestments();
        }
    }

    // Simulation engine
    static class SimulationEngine {
        private final Random random = new Random();

        public SimulationResult simulateInvestment(double initialAmount, int periodInYears, Investment investment) {
            SimulationResult result = new SimulationResult(initialAmount, periodInYears, investment);

            double baseReturn = investment.getAnnualReturn();
            double volatility = investment.getHistoricVolatility();

            // Optimistic path: return 30% higher than average
            double optimisticReturn = baseReturn * 1.3;

            // Pessimistic path: return 30% lower than average
            double pessimisticReturn = Math.max(baseReturn * 0.7, 0.01); // Not less than 1%

            // Initial values
            double optimisticValue = initialAmount;
            double moderateValue = initialAmount;
            double pessimisticValue = initialAmount;

            // Add starting point
            LocalDate currentDate = LocalDate.now();
            result.addDataPoint(new SimulationResult.DataPoint(currentDate, optimisticValue, moderateValue, pessimisticValue));

            // Simulate investment progress over time
            for (int year = 1; year <= periodInYears; year++) {
                // Add data every quarter (4 points per year)
                for (int quarter = 1; quarter <= 4; quarter++) {
                    currentDate = currentDate.plusMonths(3);

                    // Calculate investment value for each scenario with random volatility
                    optimisticValue *= (1 + (optimisticReturn / 4) + (random.nextGaussian() * volatility / 8));
                    moderateValue *= (1 + (baseReturn / 4) + (random.nextGaussian() * volatility / 8));
                    pessimisticValue *= (1 + (pessimisticReturn / 4) + (random.nextGaussian() * volatility / 8));

                    // Add data point to result
                    result.addDataPoint(new SimulationResult.DataPoint(currentDate, optimisticValue, moderateValue, pessimisticValue));
                }
            }

            // Store final values
            result.setFinalValueOptimistic(optimisticValue);
            result.setFinalValueModerate(moderateValue);
            result.setFinalValuePessimistic(pessimisticValue);

            return result;
        }
    }

    // Graph panel
    static class GraphPanel extends JPanel {
        private SimulationResult result;
        private final Color OPTIMISTIC_COLOR = new Color(46, 139, 87);  // SeaGreen
        private final Color MODERATE_COLOR = new Color(25, 25, 112);    // MidnightBlue
        private final Color PESSIMISTIC_COLOR = new Color(178, 34, 34); // Firebrick
        private final int PADDING = 50;

        public GraphPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }

        public void setSimulationResult(SimulationResult result) {
            this.result = result;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (result == null || result.getDataPoints().isEmpty()) {
                g2.drawString("No simulation data to display", getWidth() / 2 - 100, getHeight() / 2);
                return;
            }

            // Get maximum value to determine drawing scale
            double maxValue = 0;
            for (SimulationResult.DataPoint point : result.getDataPoints()) {
                maxValue = Math.max(maxValue, point.getOptimisticValue());
            }

            int width = getWidth() - 2 * PADDING;
            int height = getHeight() - 2 * PADDING;

            // Draw axes
            g2.setColor(Color.BLACK);
            g2.drawLine(PADDING, getHeight() - PADDING, PADDING, PADDING); // Y-axis
            g2.drawLine(PADDING, getHeight() - PADDING, getWidth() - PADDING, getHeight() - PADDING); // X-axis

            // Draw Y-axis marks (values)
            DecimalFormat formatter = new DecimalFormat("$#,###");
            int numYMarks = 5;
            for (int i = 0; i <= numYMarks; i++) {
                int y = getHeight() - PADDING - (i * height / numYMarks);
                double value = maxValue * i / numYMarks;
                g2.drawLine(PADDING - 5, y, PADDING, y);
                g2.drawString(formatter.format(value), 5, y + 5);
            }

            // Draw X-axis marks (time)
            int numPoints = result.getDataPoints().size();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM yyyy");

            for (int i = 0; i < numPoints; i += numPoints / 5) {
                if (i < numPoints) {
                    int x = PADDING + i * width / (numPoints - 1);
                    LocalDate date = result.getDataPoints().get(i).getDate();
                    g2.drawLine(x, getHeight() - PADDING, x, getHeight() - PADDING + 5);

                    // Rotate text for readability
                    AffineTransform originalTransform = g2.getTransform();
                    g2.rotate(Math.PI / 4, x, getHeight() - PADDING + 10);
                    g2.drawString(date.format(dateFormatter), x, getHeight() - PADDING + 10);
                    g2.setTransform(originalTransform);
                }
            }

            // Draw lines for all three scenarios
            drawLine(g2, result.getDataPoints(), maxValue, SimulationResult.DataPoint::getOptimisticValue, OPTIMISTIC_COLOR);
            drawLine(g2, result.getDataPoints(), maxValue, SimulationResult.DataPoint::getModerateValue, MODERATE_COLOR);
            drawLine(g2, result.getDataPoints(), maxValue, SimulationResult.DataPoint::getPessimisticValue, PESSIMISTIC_COLOR);

            // Draw color legend
            drawLegend(g2);
        }

        private interface ValueExtractor {
            double getValue(SimulationResult.DataPoint point);
        }

        private void drawLine(Graphics2D g2, List<SimulationResult.DataPoint> points, double maxValue,
                              ValueExtractor valueExtractor, Color color) {
            int numPoints = points.size();
            int width = getWidth() - 2 * PADDING;
            int height = getHeight() - 2 * PADDING;

            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));

            int[] xPoints = new int[numPoints];
            int[] yPoints = new int[numPoints];

            for (int i = 0; i < numPoints; i++) {
                double value = valueExtractor.getValue(points.get(i));
                xPoints[i] = PADDING + i * width / (numPoints - 1);
                yPoints[i] = getHeight() - PADDING - (int)((value / maxValue) * height);
            }

            g2.drawPolyline(xPoints, yPoints, numPoints);

            // Draw points on the line
            for (int i = 0; i < numPoints; i++) {
                g2.fill(new Ellipse2D.Double(xPoints[i] - 3, yPoints[i] - 3, 6, 6));
            }
        }

        private void drawLegend(Graphics2D g2) {
            int legendX = getWidth() - 200;
            int legendY = 50;
            int lineLength = 30;
            int lineHeight = 20;

            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString("Legend:", legendX, legendY);

            // Optimistic
            g2.setColor(OPTIMISTIC_COLOR);
            g2.drawLine(legendX, legendY + lineHeight, legendX + lineLength, legendY + lineHeight);
            g2.fill(new Ellipse2D.Double(legendX + lineLength/2 - 3, legendY + lineHeight - 3, 6, 6));
            g2.drawString("Optimistic", legendX + lineLength + 10, legendY + lineHeight + 5);

            // Moderate
            g2.setColor(MODERATE_COLOR);
            g2.drawLine(legendX, legendY + 2*lineHeight, legendX + lineLength, legendY + 2*lineHeight);
            g2.fill(new Ellipse2D.Double(legendX + lineLength/2 - 3, legendY + 2*lineHeight - 3, 6, 6));
            g2.drawString("Moderate", legendX + lineLength + 10, legendY + 2*lineHeight + 5);

            // Pessimistic
            g2.setColor(PESSIMISTIC_COLOR);
            g2.drawLine(legendX, legendY + 3*lineHeight, legendX + lineLength, legendY + 3*lineHeight);
            g2.fill(new Ellipse2D.Double(legendX + lineLength/2 - 3, legendY + 3*lineHeight - 3, 6, 6));
            g2.drawString("Pessimistic", legendX + lineLength + 10, legendY + 3*lineHeight + 5);
        }
    }

    // Investment simulation panel
    static class SimulationPanel extends JPanel {
        private final MarketDataService marketDataService;
        private final SimulationEngine simulationEngine;

        // UI components
        private JComboBox<Investment> investmentComboBox;
        private JTextField amountField;
        private JComboBox<Integer> yearsComboBox;
        private JButton simulateButton;
        private JPanel resultPanel;
        private GraphPanel graphPanel;
        private JLabel optimisticValueLabel, moderateValueLabel, pessimisticValueLabel;

        private SimulationResult currentResult;
        private final DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");

        public SimulationPanel() {
            this.marketDataService = new MarketDataService();
            this.simulationEngine = new SimulationEngine();

            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Input panel
            setupInputPanel();

            // Results panel
            resultPanel = new JPanel();
            resultPanel.setLayout(new BorderLayout(10, 10));
            resultPanel.setVisible(false);
            add(resultPanel, BorderLayout.CENTER);
        }

        private void setupInputPanel() {
            JPanel inputPanel = new JPanel(new GridLayout(0, 2, 10, 10));
            inputPanel.setBorder(BorderFactory.createTitledBorder("Simulation Parameters"));

            // Investment amount field
            inputPanel.add(new JLabel("Investment Amount:"));
            amountField = new JTextField("10000");
            inputPanel.add(amountField);

            // Investment period dropdown
            inputPanel.add(new JLabel("Investment Period (Years):"));
            yearsComboBox = new JComboBox<>(new Integer[] {1, 3, 5, 10, 20, 30});
            inputPanel.add(yearsComboBox);

            // Investment type dropdown
            inputPanel.add(new JLabel("Investment Type:"));
            investmentComboBox = new JComboBox<>();
            List<Investment> investments = marketDataService.getAllInvestments();
            for (Investment investment : investments) {
                investmentComboBox.addItem(investment);
            }
            inputPanel.add(investmentComboBox);

            // Simulation button
            simulateButton = new JButton("Run Simulation");
            simulateButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    runSimulation();
                }
            });
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(simulateButton);

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(inputPanel, BorderLayout.CENTER);
            topPanel.add(buttonPanel, BorderLayout.SOUTH);

            add(topPanel, BorderLayout.NORTH);
        }

        private void runSimulation() {
            try {
                // Read user inputs
                double amount = Double.parseDouble(amountField.getText().replace(",", "").replace("$", ""));
                int years = (Integer) yearsComboBox.getSelectedItem();
                Investment selectedInvestment = (Investment) investmentComboBox.getSelectedItem();

                // Validate inputs
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid amount", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (selectedInvestment == null) {
                    JOptionPane.showMessageDialog(this, "Please select an investment", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Run simulation
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                currentResult = simulationEngine.simulateInvestment(amount, years, selectedInvestment);

                // Display results
                displayResults();
                setCursor(Cursor.getDefaultCursor());

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for amount", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void displayResults() {
            resultPanel.removeAll();
            resultPanel.setVisible(true);

            // Summary panel
            JPanel summaryPanel = new JPanel(new GridLayout(0, 2, 5, 5));
            summaryPanel.setBorder(BorderFactory.createTitledBorder("Simulation Results"));

            // Investment details
            summaryPanel.add(new JLabel("Investment:"));
            summaryPanel.add(new JLabel(currentResult.getInvestment().getName()));

            summaryPanel.add(new JLabel("Initial Amount:"));
            summaryPanel.add(new JLabel(currencyFormat.format(currentResult.getInitialInvestment())));

            summaryPanel.add(new JLabel("Time Period:"));
            summaryPanel.add(new JLabel(currentResult.getPeriodInYears() + " years"));

            // Expected results
            summaryPanel.add(new JLabel("Optimistic Final Value:"));
            optimisticValueLabel = new JLabel(currencyFormat.format(currentResult.getFinalValueOptimistic()));
            optimisticValueLabel.setForeground(new Color(46, 139, 87)); // SeaGreen
            optimisticValueLabel.setFont(optimisticValueLabel.getFont().deriveFont(Font.BOLD));
            summaryPanel.add(optimisticValueLabel);

            summaryPanel.add(new JLabel("Moderate Final Value:"));
            moderateValueLabel = new JLabel(currencyFormat.format(currentResult.getFinalValueModerate()));
            moderateValueLabel.setForeground(new Color(25, 25, 112)); // MidnightBlue
            moderateValueLabel.setFont(moderateValueLabel.getFont().deriveFont(Font.BOLD));
            summaryPanel.add(moderateValueLabel);

            summaryPanel.add(new JLabel("Pessimistic Final Value:"));
            pessimisticValueLabel = new JLabel(currencyFormat.format(currentResult.getFinalValuePessimistic()));
            pessimisticValueLabel.setForeground(new Color(178, 34, 34)); // Firebrick
            pessimisticValueLabel.setFont(pessimisticValueLabel.getFont().deriveFont(Font.BOLD));
            summaryPanel.add(pessimisticValueLabel);

            // Graph panel
            graphPanel = new GraphPanel();
            graphPanel.setSimulationResult(currentResult);

            // Add panels to results panel
            resultPanel.add(summaryPanel, BorderLayout.NORTH);
            resultPanel.add(graphPanel, BorderLayout.CENTER);

            // Update UI
            revalidate();
            repaint();
        }
    }
}