import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.sql.*;

/**
 * نظام الإشعارات
 * ملف منفصل يتكامل مع تطبيق محاكاة الاستثمار
 */
public class Receive_Notifications {

    // نقطة الدخول للاختبار المستقل
    public static void main(String[] args) {
        // التأكد من تحميل مشغل SQLite JDBC
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found. Please add sqlite-jdbc-3.xx.jar to your classpath.");
            return;
        }

        // بدء التطبيق
        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseManager.getInstance(); // تهيئة قاعدة البيانات
                MarketDataService marketDataService = new MarketDataService();
                NotificationService notificationService = new NotificationService(marketDataService);

                JFrame frame = new JFrame("Notification System Demo");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(800, 600);
                frame.add(new NotificationPanel(notificationService));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                // إضافة مستمع لإيقاف خدمة الإشعارات عند الإغلاق
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        notificationService.shutdown();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error starting application: " + e.getMessage());
            }
        });
    }
}

// نموذج المستخدم
class User {
    private int id;
    private String name;
    private String email;
    private NotificationPreference notificationPreference;

    public User(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.notificationPreference = NotificationPreference.ALL;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public NotificationPreference getNotificationPreference() { return notificationPreference; }
    public void setNotificationPreference(NotificationPreference notificationPreference) {
        this.notificationPreference = notificationPreference;
    }

    @Override
    public String toString() {
        return name + " (" + email + ")";
    }
}

// تفضيلات الإشعارات
enum NotificationPreference {
    ALL("All Notifications"),
    IMPORTANT("Important Notifications Only"),
    NONE("No Notifications");

    private final String description;

    NotificationPreference(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

// نوع الإشعار
enum NotificationType {
    MARKET_CHANGE("Market Change"),
    PRICE_ALERT("Price Alert"),
    VOLATILITY_ALERT("Volatility Alert"),
    SYSTEM_NOTIFICATION("System Notification");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

// أولوية الإشعار
enum NotificationPriority {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    private final String description;

    NotificationPriority(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

// نموذج الإشعار
class Notification {
    private int id;
    private int userId;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationPriority priority;
    private LocalDateTime timestamp;
    private boolean read;

    public Notification(int id, int userId, String title, String message,
                        NotificationType type, NotificationPriority priority) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.priority = priority;
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }

    // للإشعارات الجديدة التي لم تُحفظ بعد في قاعدة البيانات
    public Notification(int userId, String title, String message,
                        NotificationType type, NotificationPriority priority) {
        this(-1, userId, title, message, type, priority);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public NotificationPriority getPriority() { return priority; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    @Override
    public String toString() {
        return title;
    }
}

// DAO للمستخدمين
class UserDAO {
    private final DatabaseManager dbManager;

    public UserDAO() {
        this.dbManager = DatabaseManager.getInstance();
        initializeUserTable();
    }

    private void initializeUserTable() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // إنشاء جدول المستخدمين إذا لم يكن موجوداً
            String createUserTable =
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "name TEXT NOT NULL," +
                            "email TEXT UNIQUE NOT NULL," +
                            "notification_preference TEXT DEFAULT 'ALL'" +
                            ");";
            stmt.execute(createUserTable);

            // إدخال بيانات نموذجية إذا كان الجدول فارغاً
            if (stmt.executeQuery("SELECT COUNT(*) FROM users").getInt(1) == 0) {
                String[] insertStatements = {
                        "INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com')",
                        "INSERT INTO users (name, email) VALUES ('Jane Smith', 'jane@example.com')",
                        "INSERT INTO users (name, email) VALUES ('Bob Johnson', 'bob@example.com')"
                };

                for (String sql : insertStatements) {
                    stmt.execute(sql);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error initializing user table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email")
                );

                String prefString = rs.getString("notification_preference");
                if (prefString != null) {
                    user.setNotificationPreference(NotificationPreference.valueOf(prefString));
                }

                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
            e.printStackTrace();
        }

        return users;
    }

    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email")
                );

                String prefString = rs.getString("notification_preference");
                if (prefString != null) {
                    user.setNotificationPreference(NotificationPreference.valueOf(prefString));
                }

                return user;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateNotificationPreference(int userId, NotificationPreference preference) {
        String sql = "UPDATE users SET notification_preference = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, preference.name());
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating notification preference: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

// DAO للإشعارات
class NotificationDAO {
    private final DatabaseManager dbManager;

    public NotificationDAO() {
        this.dbManager = DatabaseManager.getInstance();
        initializeNotificationTable();
    }

    private void initializeNotificationTable() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // إنشاء جدول الإشعارات إذا لم يكن موجوداً
            String createNotificationTable =
                    "CREATE TABLE IF NOT EXISTS notifications (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "user_id INTEGER NOT NULL," +
                            "title TEXT NOT NULL," +
                            "message TEXT NOT NULL," +
                            "type TEXT NOT NULL," +
                            "priority TEXT NOT NULL," +
                            "timestamp TEXT NOT NULL," +
                            "read INTEGER DEFAULT 0," +
                            "FOREIGN KEY (user_id) REFERENCES users(id)" +
                            ");";
            stmt.execute(createNotificationTable);

        } catch (SQLException e) {
            System.err.println("Error initializing notification table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean saveNotification(Notification notification) {
        String sql = "INSERT INTO notifications (user_id, title, message, type, priority, timestamp, read) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, notification.getUserId());
            pstmt.setString(2, notification.getTitle());
            pstmt.setString(3, notification.getMessage());
            pstmt.setString(4, notification.getType().name());
            pstmt.setString(5, notification.getPriority().name());
            pstmt.setString(6, notification.getTimestamp().toString());
            pstmt.setInt(7, notification.isRead() ? 1 : 0);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        notification.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error saving notification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Notification> getNotificationsByUserId(int userId) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY timestamp DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Notification notification = new Notification(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("title"),
                        rs.getString("message"),
                        NotificationType.valueOf(rs.getString("type")),
                        NotificationPriority.valueOf(rs.getString("priority"))
                );

                notification.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
                notification.setRead(rs.getInt("read") == 1);

                notifications.add(notification);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching notifications: " + e.getMessage());
            e.printStackTrace();
        }

        return notifications;
    }

    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET read = 1 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, notificationId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error marking notification as read: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

// واجهة للاستماع للإشعارات
interface NotificationListener {
    void onNotificationReceived(Notification notification);
}

// خدمة الإشعارات: تراقب التغيرات وترسل الإشعارات
class NotificationService {
    private final MarketDataService marketDataService;
    private final NotificationDAO notificationDAO;
    private final UserDAO userDAO;
    private final Timer timer;
    private final Random random = new Random();
    private final List<NotificationListener> listeners = new ArrayList<>();

    // تخزين القيم السابقة للأسهم لاكتشاف التغيرات
    private final Map<Integer, Double> previousPrices = new HashMap<>();

    public NotificationService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
        this.notificationDAO = new NotificationDAO();
        this.userDAO = new UserDAO();
        this.timer = new Timer(true);

        // تخزين الأسعار الأولية
        for (Investment investment : marketDataService.getAllInvestments()) {
            previousPrices.put(investment.getId(), investment.getCurrentPrice());
        }

        // بدء جدولة المراقبة (كل 10 ثوانٍ للعرض التوضيحي، في الواقع يمكن أن تكون أطول)
        timer.schedule(new MarketMonitorTask(), 5000, 10000);
    }

    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Notification notification) {
        for (NotificationListener listener : listeners) {
            listener.onNotificationReceived(notification);
        }
    }

    // التحقق من تفضيلات المستخدم للإشعارات
    private boolean shouldNotifyUser(User user, NotificationPriority priority) {
        NotificationPreference preference = user.getNotificationPreference();

        if (preference == NotificationPreference.ALL) {
            return true;
        } else if (preference == NotificationPreference.IMPORTANT &&
                (priority == NotificationPriority.HIGH || priority == NotificationPriority.MEDIUM)) {
            return true;
        }

        return false;
    }

    // الحصول على قائمة الإشعارات للمستخدم
    public List<Notification> getUserNotifications(int userId) {
        return notificationDAO.getNotificationsByUserId(userId);
    }

    // وضع علامة "مقروء" على الإشعار
    public boolean markNotificationAsRead(int notificationId) {
        return notificationDAO.markAsRead(notificationId);
    }

    // إيقاف خدمة الإشعارات
    public void shutdown() {
        timer.cancel();
    }

    // مهمة مراقبة السوق
    private class MarketMonitorTask extends TimerTask {
        @Override
        public void run() {
            // في بيئة حقيقية، هنا سنجلب البيانات المحدثة من السوق
            // لغرض العرض التوضيحي، سنولد بعض التغيرات العشوائية

            List<Investment> investments = marketDataService.getAllInvestments();
            List<User> users = userDAO.getAllUsers();

            for (Investment investment : investments) {
                // تحقق ما إذا كان هناك تغير كبير في السعر (افتراضي للعرض)
                if (shouldGenerateAlert(investment)) {
                    double previousPrice = previousPrices.getOrDefault(investment.getId(), investment.getCurrentPrice());
                    double priceChange = ((investment.getCurrentPrice() - previousPrice) / previousPrice) * 100;
                    String direction = priceChange > 0 ? "increased" : "decreased";

                    // إنشاء إشعار لكل مستخدم (حسب تفضيلات الإشعارات)
                    for (User user : users) {
                        // تحقق من تفضيلات المستخدم
                        if (shouldNotifyUser(user, NotificationPriority.MEDIUM)) {
                            Notification notification = new Notification(
                                    user.getId(),
                                    "Price Alert: " + investment.getName(),
                                    investment.getName() + " has " + direction + " by " +
                                            String.format("%.2f", Math.abs(priceChange)) + "%. Current price: $" +
                                            String.format("%.2f", investment.getCurrentPrice()),
                                    NotificationType.PRICE_ALERT,
                                    Math.abs(priceChange) > 5 ? NotificationPriority.HIGH : NotificationPriority.MEDIUM
                            );

                            // حفظ الإشعار في قاعدة البيانات
                            if (notificationDAO.saveNotification(notification)) {
                                // إشعار المستمعين (مثل واجهة المستخدم)
                                notifyListeners(notification);

                                // محاكاة إرسال بريد إلكتروني
                                sendEmailNotification(user, notification);
                            }
                        }
                    }

                    // تحديث السعر السابق
                    previousPrices.put(investment.getId(), investment.getCurrentPrice());
                }
            }
        }

        // محاكاة التغيرات في السوق (للعرض التوضيحي فقط)
        private boolean shouldGenerateAlert(Investment investment) {
            // 20% فرصة لإنشاء تنبيه كل مرة (للعرض فقط)
            return random.nextDouble() < 0.2;
        }

        // محاكاة إرسال بريد إلكتروني (في التطبيق الحقيقي، سنستخدم خدمة بريد إلكتروني)
        private void sendEmailNotification(User user, Notification notification) {
            System.out.println("Sending email to " + user.getEmail() + ": " + notification.getTitle());
            // هنا سيكون شيفرة الاتصال بخدمة البريد الإلكتروني
        }
    }
}

class NotificationPanel extends JPanel implements NotificationListener {
    private final NotificationService notificationService;
    private JList<Notification> notificationList;
    private DefaultListModel<Notification> notificationModel;
    private JTextArea detailsArea;
    private JComboBox<User> userSelector;
    private JComboBox<NotificationPreference> preferenceSelector;
    private User currentUser;

    public NotificationPanel(NotificationService notificationService) {
        this.notificationService = notificationService;
        notificationService.addListener(this);
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // User selection and preferences panel
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // User selector
        userPanel.add(new JLabel("Select User:"));
        userSelector = new JComboBox<>(new UserDAO().getAllUsers().toArray(new User[0]));
        userSelector.addActionListener(e -> {
            currentUser = (User) userSelector.getSelectedItem();
            preferenceSelector.setSelectedItem(currentUser.getNotificationPreference());
            refreshNotifications();
        });
        userPanel.add(userSelector);

        // Notification preferences
        userPanel.add(new JLabel("Notification Preference:"));
        preferenceSelector = new JComboBox<>(NotificationPreference.values());
        preferenceSelector.addActionListener(e -> {
            if (currentUser != null) {
                NotificationPreference newPreference = (NotificationPreference) preferenceSelector.getSelectedItem();
                new UserDAO().updateNotificationPreference(currentUser.getId(), newPreference);
                currentUser.setNotificationPreference(newPreference);
            }
        });
        userPanel.add(preferenceSelector);

        topPanel.add(userPanel, BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshNotifications());
        topPanel.add(refreshButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Notification list
        notificationModel = new DefaultListModel<>();
        notificationList = new JList<>(notificationModel);
        notificationList.setCellRenderer(new NotificationCellRenderer());
        notificationList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && notificationList.getSelectedValue() != null) {
                Notification selected = notificationList.getSelectedValue();
                detailsArea.setText("Title: " + selected.getTitle() +
                        "\nMessage: " + selected.getMessage() +
                        "\nType: " + selected.getType().getDescription() +
                        "\nPriority: " + selected.getPriority().getDescription() +
                        "\nTime: " + formatDateTime(selected.getTimestamp()) +
                        "\nStatus: " + (selected.isRead() ? "Read" : "Unread"));

                // Mark as read if not already
                if (!selected.isRead()) {
                    notificationService.markNotificationAsRead(selected.getId());
                    selected.setRead(true);
                    notificationList.repaint();
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(notificationList);
        listScrollPane.setPreferredSize(new Dimension(300, 400));

        // Details panel
        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        JScrollPane detailsScrollPane = new JScrollPane(detailsArea);

        // Split pane for list and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                listScrollPane, detailsScrollPane);
        splitPane.setResizeWeight(0.4);
        add(splitPane, BorderLayout.CENTER);

        // Set initial user and load notifications
        if (userSelector.getItemCount() > 0) {
            currentUser = (User) userSelector.getItemAt(0);
            preferenceSelector.setSelectedItem(currentUser.getNotificationPreference());
            refreshNotifications();
        }
    }

    private void refreshNotifications() {
        if (currentUser != null) {
            notificationModel.clear();
            List<Notification> notifications = notificationService.getUserNotifications(currentUser.getId());
            for (Notification notification : notifications) {
                notificationModel.addElement(notification);
            }
        }
    }

    @Override
    public void onNotificationReceived(Notification notification) {
        if (currentUser != null && notification.getUserId() == currentUser.getId()) {
            SwingUtilities.invokeLater(() -> {
                notificationModel.add(0, notification);
            });
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Custom cell renderer for notifications
    private class NotificationCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            if (value instanceof Notification) {
                Notification notification = (Notification) value;

                // Format display text
                label.setText(notification.getTitle());

                // Style based on read status and priority
                Font font = label.getFont();
                if (!notification.isRead()) {
                    label.setFont(font.deriveFont(Font.BOLD));
                }

                // Color based on priority if not selected
                if (!isSelected) {
                    switch (notification.getPriority()) {
                        case HIGH:
                            label.setForeground(Color.RED);
                            break;
                        case MEDIUM:
                            label.setForeground(Color.BLUE);
                            break;
                        default:
                            label.setForeground(Color.BLACK);
                    }
                }

                // Add icon based on notification type
                // (Would need to create/load appropriate icons)
                // label.setIcon(...);
            }

            return label;
        }
    }
}