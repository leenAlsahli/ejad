import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
//كلاس الكلاينت
//  وظيفته يأخذ أرقام من اليوزر يرسلها للسيرفر، ويجيب النتيجة
public class EjadClient extends JFrame {


    //  معلومات السيرفر اللي نتصل فيه 
    // هنا نحدد وين السيرفر موجود وعلى أي بورت يستمع
    // الاي بي هو عنوان الجهاز اللي عليه السيرفر في نفس الشبكة
    private static final String SERVER_HOST = "192.168.1.32"; // آي بي السيرفر على الشبكة المحلية
    private static final int    SERVER_PORT = 12345;          // رقم البورت اللي السيرفر شايله

    //  هنا اهم جزء التي سي بي كونكشن  (برسستنت كونكشن)    //
    //  نفتح الكونكشن مرة واحدة بس لما البرنامج يشتغل
    //  نستخدم نفس الكونكشن لكل الطلبات لانه برسستنت

    //اتربيوت السوكيت
    private Socket           persistentSocket = null; 
    // اتربيوت الاوتبوت بالنسبه للكلاينت  وهي الداتا اللي بتطلع من الكلاينت وبتروح للسيرفر
    private DataOutputStream persistentOut    = null; 
     // اتربيوت الانبوت بالنسبه للكلاينت  وهي الداتا اللي بترجع من السيرفر وبتدخل الكلاينت
    private DataInputStream  persistentIn     = null; 
    //اتربيوت الكاونتر يحسب كم طلب ارسلنا بنفس الكونكشن
    private int              requestCount     = 0;  

    //  ميثود فتح الكونكشن مع السيرفر
    //  هذي الميثود اساس البيرسيستنت كونكشن
    //  يتأكد ان الكونكشن مفتوح ولو مو مفتوحة يفتحها
    //  بعدين يبقى نفس السوكيت مستخدم لكل الطلبات الجايه
    private void openPersistentConnection() throws IOException {
        // نشوف هل السوكيت أصلاً مو موجود ولا موجود لكن مسكر ؟
        // لو السوكيت مو موجود اساسا او مومجود بس مسكر .. نفتح سوكيت جديد
        if (persistentSocket == null || persistentSocket.isClosed()) {

            // اول خطوة انشئنا السوكيت 
            persistentSocket = new Socket();

            // ثاني خطوه اتصلنا بالسيرفر عن طريق الاي بي والبورت
            //   التايم اوت ٣ ثواني  لو ما رد السيرفر بنطلع ايرور
            persistentSocket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), 3000);//٣٠٠٠ ملي ثانيه

            // الحين بنسوي كاست للأوتبوت ستريم لداتا أوتبوت ستريم
            // عشان السوكيت يفهم بايتس بس، فبنحتاج نحول النصوص والأرقام لبايتس عشان تنرسل للسيرفر
            // وبنسوي كاست للانبوت ستريم لداتا انبوت ستريم عشان نستقبل البايتس من السيرفر ونحولها لنصوص وأرقام نقدر نستخدمها

            // ثالث خطوة نقرا من السيرفر بعد ما الكونكشن اتفتحت 
            //التايم اوت ١٠ ثواني لو ماوصلني الرد نطلع ايرور
            persistentSocket.setSoTimeout(10000);

            // رابع خطوة نجهز الداتا أوتبوت ستريم 
            // ونسوي كاست للاوتبوت ستريم العادي اللي يفهم بايتس بس  - الى أوتبوت ستريم يفهم نصوص وأرقام
            persistentOut = new DataOutputStream(persistentSocket.getOutputStream());

            // خامس خطوة نجهز الداتا انبوت ستريم 
            // ونسوي كاست للانبوت ستريم اللي يفهم يفهم نصوص وأرقام - الى انبوت ستريم اللي يفهم بايتس بس 
            //يعني عكس عمليه الاوتبوت ستريم
            persistentIn  = new DataInputStream(persistentSocket.getInputStream());


            //  سادس خطوة نقرا رسالة الترحيب من السيرفر
            //السيرفر يرسلها مرة وحدة بس عند أول كونكشن
            String welcome = persistentIn.readUTF();
            System.out.println("[✓] Connected. Server says: " + welcome);

            // نصفر العداد اللي يحسب الطلبات لان الحين فتحنا كونكشن جديد
            requestCount = 0;
        }
        // لو الكونكشن أصلاً مفتوحة ف ما مابنسوي شي، بنستخدم اللي عندنا مباشرة
    }

    //  ميثود تسكير الكونكشن مع السيرفر
    //  يستدعى لما يسكر اليوزر البرنامج
    //  أو لما يصير خطأ في الاتصال ونحتاج نبدأ كونكشن جديدة
    private void closePersistentConnection() {
        try {
            // لو السوكيت موجود وما زال مفتوح بنسكره 
            if (persistentSocket != null && !persistentSocket.isClosed())
                persistentSocket.close(); // هذا يسكر الأوتبوت والإنبوت برضو تلقائياً
        } catch (IOException ignored) {} // نتجاهل أي خطأ أثناء التسكير

        // نفضي الفاربلز عشان يعرف الكود انه ما عندنا اي كونكشن نشطة
        persistentSocket = null;
        persistentOut    = null;
        persistentIn     = null;
    }

    // ══════════════════════════════════════════════════════════════════
    //  UI
    // ══════════════════════════════════════════════════════════════════

    //  الألوان اللي نستخدمها في الواجهة 
    private static final Color BRAND_MAIN   = new Color(102, 153, 153); // اللون الأساسي حق البرنامج
    private static final Color BRAND_DARK   = new Color(77,  128, 128); // نفس اللون بس أغمق شوي
    private static final Color BRAND_LIGHT  = new Color(194, 214, 214); // فاتح للخلفيات الثانوية
    private static final Color BRAND_XLIGHT = new Color(232, 242, 242); // فاتح جداً للبلوكات الخلفية
    private static final Color BG_WHITE     = Color.WHITE;              // أبيض عادي
    private static final Color SOFT_GRAY    = new Color(244, 247, 247); // رمادي ناعم للخلفية الرئيسية
    private static final Color BORDER_COLOR = new Color(208, 224, 224); // لون الحدود بين العناصر
    private static final Color TEXT_DARK    = new Color(26,  46,  46);  // لون النصوص الرئيسية
    private static final Color TEXT_MUTED   = new Color(100, 120, 120); // لون النصوص الثانوية الباهتة

    //تنقل بين الشاشات
    private final CardLayout cardLayout    = new CardLayout();
    private final JPanel     mainContainer = new JPanel(cardLayout);

    //  معلومات اليوزر اللي دخل 
    private String userName = ""; // اسم الشخص اللي سجّل دخول
    private String userRole = ""; // دوره: طالب ولا أستاذ

    //  ليبلز الهيدر 
    private final JLabel lblHeaderName = new JLabel("USER NAME");
    private final JLabel lblHeaderRole = new JLabel("USER ROLE");

    // هذي الميثود تشتغل اول ما نسوي رن للبروقرام
    public EjadClient() {
        setTitle("Ejad");
        setSize(1100, 820); 
        setMinimumSize(new Dimension(900, 700)); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLocationRelativeTo(null); 
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        mainContainer.add(buildSplashPanel(),   "Splash");    
        mainContainer.add(new LoginPanel(),     "Login");     
        mainContainer.add(new DashboardPanel(), "Dashboard"); 
        add(mainContainer); 
        setVisible(true);   
        // لما نسكر صفحةالانترفيس برضو تتسكر الكونكشن مع السيرفر بشكل نظيف
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                closePersistentConnection(); // نقطع الاتصال قبل ما البرنامج يوقف
            }
        });
        Timer t = new Timer(2200, e -> cardLayout.show(mainContainer, "Login"));
        t.setRepeats(false); 
        t.start();
        new Thread(() -> {
            try {
                openPersistentConnection(); // افتح الكونكشن
                System.out.println("[✓] Connected to server on startup.");
            } catch (Exception ex) {
                System.out.println("[✗] Could not connect to server: " + ex.getMessage());
            }
        }).start();
    }

    private JPanel buildSplashPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RadialGradientPaint vignette = new RadialGradientPaint(
                    new Point(getWidth() / 2, getHeight() / 2),
                    Math.max(getWidth(), getHeight()) * 0.7f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(240, 248, 248, 0), new Color(180, 210, 210, 55)}
                );
                g2.setPaint(vignette);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        panel.setBackground(BG_WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 20, 0);
        panel.add(buildLogo("logoo.png", 220, false), gbc); // نضع اللوقو في المنتصف
        return panel;
    }
    private JComponent buildLogo(String filename, int size, boolean fallbackBg) {
        try {
            ImageIcon raw = new ImageIcon(filename);
            if (raw.getIconWidth() > 0) { // تأكد إن الصورة اتحملت صح
                Image scaled = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new JLabel(new ImageIcon(scaled)); // نرجع اللوقو كليبل
            }
        } catch (Exception ignored) {}
        Color fill = fallbackBg ? new Color(255, 255, 255, 50) : BRAND_MAIN;
        JPanel box = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12); // مربع بزوايا مدورة
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, (int)(size * 0.36)));
                FontMetrics fm = g2.getFontMetrics();
                String t = "EJ"; // الأحرف البديلة
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent()); // نرسم النص في المنتصف
                g2.dispose();
            }
        };
        box.setOpaque(false);
        Dimension d = new Dimension(size, size);
        box.setPreferredSize(d); box.setMaximumSize(d); box.setMinimumSize(d);
        return box;
    }
    private JPanel buildHeader(String title, String subtitle, boolean showUserInfo) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(BRAND_MAIN); // خلفية الهيدر بلون البروند
        h.setPreferredSize(new Dimension(0, 72)); // ارتفاع ثابت ٧٢ بيكسل
        h.setBorder(new EmptyBorder(0, 28, 0, 28)); // مسافة داخلية يمين ويسار
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(14, 0, 0, 0));
        left.add(buildLogo("logo.png", 40, true)); // لوقو صغير
        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel(title);
        t1.setFont(new Font("SansSerif", Font.BOLD, 15));
        t1.setForeground(Color.WHITE);
        JLabel t2 = new JLabel(subtitle);
        t2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        t2.setForeground(BRAND_LIGHT);
        titleBox.add(t1); titleBox.add(t2);
        left.add(titleBox);
        h.add(left, BorderLayout.WEST);
        if (showUserInfo) {
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            right.setOpaque(false);
            right.setBorder(new EmptyBorder(16, 0, 0, 0));
            JPanel userBox = new JPanel();
            userBox.setOpaque(false);
            userBox.setLayout(new BoxLayout(userBox, BoxLayout.Y_AXIS));
            lblHeaderName.setFont(new Font("SansSerif", Font.BOLD, 13));
            lblHeaderName.setForeground(Color.WHITE);
            lblHeaderName.setAlignmentX(Component.RIGHT_ALIGNMENT);
            lblHeaderRole.setFont(new Font("SansSerif", Font.PLAIN, 10));
            lblHeaderRole.setForeground(BRAND_LIGHT);
            lblHeaderRole.setAlignmentX(Component.RIGHT_ALIGNMENT);
            userBox.add(lblHeaderName);
            userBox.add(lblHeaderRole);
            right.add(userBox);
            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(100, 220, 150)); // لون أخضر
                    g2.fillOval(0, 2, 10, 10); // دائرة صغيرة
                    g2.dispose();
                }
            };
            dot.setOpaque(false);
            dot.setPreferredSize(new Dimension(10, 14));
            right.add(dot);
            h.add(right, BorderLayout.EAST);
        } else {
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.setOpaque(false);
            right.setBorder(new EmptyBorder(20, 0, 0, 0));
            JLabel badge = new JLabel("v2.0 Pro") { // شارة الإصدار
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 255, 255, 38)); // أبيض شفاف للخلفية
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setFont(new Font("SansSerif", Font.BOLD, 11));
            badge.setForeground(Color.WHITE);
            badge.setBorder(new EmptyBorder(5, 14, 5, 14));
            badge.setOpaque(false);
            right.add(badge);
            h.add(right, BorderLayout.EAST);
        }
        return h;
    }
    private JButton createPrimaryButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // لو الزر مضغوط أو فوقه الماوس → لون أغمق، غير ذلك اللون الأساسي
                g2.setColor(getModel().isPressed() || getModel().isRollover() ? BRAND_DARK : BRAND_MAIN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR)); // يخلي المؤشر يصير يد لما يحوم فوقه
        b.setPreferredSize(new Dimension(220, 46));
        b.setMaximumSize(new Dimension(340, 46));
        return b;
    }
    private JButton createOutlineButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BRAND_XLIGHT : BG_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BRAND_MAIN);
                g2.setStroke(new BasicStroke(1.5f)); // سمك الحد
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12); // نرسم الحد فقط
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(BRAND_MAIN);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(110, 46));
        return b;
    }
    class RoundTextField extends JTextField {
        RoundTextField(int cols) {
            super(cols);
            setOpaque(false); // نخلي الخلفية شفافة حتى نرسمها نحن بشكل مخصص
            setFont(new Font("SansSerif", Font.PLAIN, 13));
            setForeground(TEXT_DARK);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SOFT_GRAY); // خلفية رمادية ناعمة
            g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20); // مستطيل مدور
            super.paintComponent(g2); // نرسم النص الأصلي فوق الخلفية
            g2.dispose();
        }
        @Override protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BORDER_COLOR);
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20); // نرسم الحد المدور
            g2.dispose();
        }
    }
    class LoginPanel extends JPanel {
        private String selectedRole = "student"; 
        private JPanel cardStudent, cardFaculty; 
        LoginPanel() {
            setLayout(new BorderLayout());
            setBackground(BG_WHITE);
            add(buildHeader("Ejad", "Where Equations Meet Solutions", false), BorderLayout.NORTH);
            JPanel split = new JPanel(new GridLayout(1, 2, 0, 0));
            split.add(buildBrandingPanel()); // اليسار
            split.add(buildFormPanel());     // اليمين
            add(split, BorderLayout.CENTER);
        }
        private JPanel buildBrandingPanel() {
            JPanel p = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(102, 153, 153, 22));
                    g2.fillOval(getWidth() - 130, -70, 210, 210); // دائرة في أعلى اليمين
                    g2.setColor(new Color(102, 153, 153, 12));
                    g2.fillOval(-60, getHeight() - 130, 180, 180); // دائرة في أسفل اليسار
                    g2.dispose();
                }
            };
            p.setBackground(BRAND_XLIGHT);
            p.setLayout(new BorderLayout());
            p.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_COLOR)); // حد يمين فقط يفصل النصين
            JPanel inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
            inner.setBorder(new EmptyBorder(50, 44, 40, 36));
            JComponent logo = buildLogo("ايجاد.png", 200, false);
            logo.setAlignmentX(LEFT_ALIGNMENT);
            JLabel sectionLabel = new JLabel("ABOUT THE SYSTEM");
            sectionLabel.setFont(new Font("SansSerif", Font.BOLD, 9));
            sectionLabel.setForeground(new Color(160, 180, 180));
            sectionLabel.setAlignmentX(LEFT_ALIGNMENT);
            JLabel desc = new JLabel(
                "<html><body style='width:210px; line-height:1.7'>"
                + "A network computing system for solving complex equations "
                + "and delivering real-time results via the network."
                + "</body></html>"
            );
            desc.setFont(new Font("SansSerif", Font.PLAIN, 12));
            desc.setForeground(TEXT_MUTED);
            desc.setAlignmentX(LEFT_ALIGNMENT);
            String[] features = {
                "Centralized server-side equation solving",
                "Real-time network data synchronization",
                "Multi-parameter linear system processing",
                "Bi-directional data exchange between nodes"
            };
            JPanel featureList = new JPanel();
            featureList.setOpaque(false);
            featureList.setLayout(new BoxLayout(featureList, BoxLayout.Y_AXIS));
            featureList.setAlignmentX(LEFT_ALIGNMENT);
            for (String f : features) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
                row.setOpaque(false);
                row.setAlignmentX(LEFT_ALIGNMENT);
                JPanel dot = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(BRAND_MAIN);
                        g2.fillOval(0, 3, 7, 7);
                        g2.dispose();
                    }
                };
                dot.setOpaque(false);
                dot.setPreferredSize(new Dimension(7, 13));

                JLabel fl = new JLabel(f);
                fl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                fl.setForeground(TEXT_MUTED);
                row.add(dot); row.add(fl);
                featureList.add(row);
            }
            inner.add(logo);
            inner.add(Box.createRigidArea(new Dimension(0, 28)));
            inner.add(sectionLabel);
            inner.add(Box.createRigidArea(new Dimension(0, 10)));
            inner.add(desc);
            inner.add(Box.createRigidArea(new Dimension(0, 26)));
            inner.add(featureList);
            p.add(inner, BorderLayout.NORTH);
            return p;
        }
        private JPanel buildFormPanel() {
            JPanel p = new JPanel(new GridBagLayout()); // نستخدم جريد باغ لنمركز الفورم
            p.setBackground(BG_WHITE);
            JPanel form = new JPanel();
            form.setOpaque(false);
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setBorder(new EmptyBorder(0, 8, 0, 8));
            form.setPreferredSize(new Dimension(340, 440));
            JLabel secLabel = new JLabel("  IDENTITY VERIFICATION  ");
            secLabel.setFont(new Font("SansSerif", Font.BOLD, 9));
            secLabel.setForeground(BRAND_MAIN);
            secLabel.setOpaque(true);
            secLabel.setBackground(BRAND_XLIGHT);
            secLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
            secLabel.setAlignmentX(LEFT_ALIGNMENT);
            JLabel nameLabel = new JLabel("Full Name");
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            nameLabel.setForeground(TEXT_DARK);
            nameLabel.setAlignmentX(LEFT_ALIGNMENT);
            RoundTextField txtName = new RoundTextField(20); // حقل الإدخال المدور
            txtName.setMaximumSize(new Dimension(340, 48));
            txtName.setBorder(new EmptyBorder(0, 18, 0, 18));
            txtName.setAlignmentX(LEFT_ALIGNMENT);
            JLabel roleLabel = new JLabel("Select Role");
            roleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            roleLabel.setForeground(TEXT_DARK);
            roleLabel.setAlignmentX(LEFT_ALIGNMENT);
            JPanel roleRow = new JPanel(new GridLayout(1, 2, 12, 0));
            roleRow.setOpaque(false);
            roleRow.setMaximumSize(new Dimension(340, 72));
            roleRow.setAlignmentX(LEFT_ALIGNMENT);
            cardStudent = buildRoleCard("Student");
            cardFaculty = buildRoleCard("Faculty");
            cardStudent.putClientProperty("selected", true);  // الطالب مختار افتراضياً
            cardFaculty.putClientProperty("selected", false);
            cardStudent.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { setRole("student"); }
            });
            cardFaculty.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { setRole("faculty"); }
            });
            roleRow.add(cardStudent);
            roleRow.add(cardFaculty);
            JButton btnLogin = createPrimaryButton("ENTER  \u2192"); // زر الدخول (السهم من يوني كود)
            btnLogin.setAlignmentX(LEFT_ALIGNMENT);
            btnLogin.setMaximumSize(new Dimension(340, 48));
            JLabel hint = new JLabel("  Your session is encrypted and monitored.");
            hint.setFont(new Font("SansSerif", Font.PLAIN, 10));
            hint.setForeground(new Color(170, 185, 185));
            hint.setAlignmentX(LEFT_ALIGNMENT);
            form.add(secLabel);
            form.add(Box.createRigidArea(new Dimension(0, 26)));
            form.add(nameLabel);
            form.add(Box.createRigidArea(new Dimension(0, 8)));
            form.add(txtName);
            form.add(Box.createRigidArea(new Dimension(0, 24)));
            form.add(roleLabel);
            form.add(Box.createRigidArea(new Dimension(0, 10)));
            form.add(roleRow);
            form.add(Box.createRigidArea(new Dimension(0, 30)));
            form.add(btnLogin);
            form.add(Box.createRigidArea(new Dimension(0, 14)));
            form.add(hint);
            p.add(form);
            btnLogin.addActionListener(e -> {
                String name = txtName.getText().trim();
                if (name.isEmpty()) { txtName.requestFocus(); return; } // لو ما كتب اسم نركز على الحقل
                userName = name;
                userRole = selectedRole.equals("faculty") ? "Faculty" : "Student";
                lblHeaderName.setText(userName.toUpperCase());
                lblHeaderRole.setText(userRole);
                cardLayout.show(mainContainer, "Dashboard");
            });
            return p;
        }
        private JPanel buildRoleCard(String name) {
            JPanel card = new JPanel(new BorderLayout()) {
                boolean hovered = false; // هل الماوس فوق الكارد؟
                {
                    addMouseListener(new MouseAdapter() {
                        public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                        public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
                    });
                }
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean sel = Boolean.TRUE.equals(getClientProperty("selected"));
                    g2.setColor(sel || hovered ? BRAND_XLIGHT : BG_WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.setColor(sel ? BRAND_MAIN : BORDER_COLOR);
                    g2.setStroke(new BasicStroke(sel ? 2f : 1f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                    g2.dispose();
                }
            };
            JLabel label = new JLabel(name, SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 14));
            label.setForeground(TEXT_DARK);
            card.add(label, BorderLayout.CENTER);
            card.setPreferredSize(new Dimension(150, 62));
            card.setOpaque(false);
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return card;
        }
        private void setRole(String role) {
            selectedRole = role;
            cardStudent.putClientProperty("selected", role.equals("student"));
            cardFaculty.putClientProperty("selected", role.equals("faculty"));
            cardStudent.repaint();
            cardFaculty.repaint();
        }
    }
    class DashboardPanel extends JPanel {
        private JLabel statusLabel;      // نص الحالة (جاهز / يتحمل / تم)
        private JPanel resultPanel;      // اللوح اللي يظهر فيه الجواب
        private JLabel resultText;       // النص الفعلي للجواب
        private JLabel requestCountLabel; // يعرض عدد الطلبات على الكونكشن الحالية
        DashboardPanel() {
            setLayout(new BorderLayout());
            setBackground(SOFT_GRAY);
            add(buildHeader("Ejad", "Active Session", true), BorderLayout.NORTH); // هيدر مع معلومات المستخدم
            JPanel scrollContent = new JPanel();
            scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
            scrollContent.setOpaque(false);
            scrollContent.setBorder(new EmptyBorder(36, 44, 44, 44));
            scrollContent.add(buildEquationCard()); // نضيف الكارد الرئيسي
            JScrollPane scroll = new JScrollPane(scrollContent);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(14); // سرعة السكرول
            add(scroll, BorderLayout.CENTER);
        }
        private JPanel buildEquationCard() {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(BG_WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER_COLOR, 16),      // حد خارجي مدور
                new EmptyBorder(34, 38, 34, 38)         // مسافة داخلية
            ));
            JPanel titleRow = new JPanel(new BorderLayout());
            titleRow.setOpaque(false);
            titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

            JLabel cardTitle = new JLabel("Linear Equation Solver");
            cardTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
            cardTitle.setForeground(TEXT_DARK);
            JLabel formulaBadge = new JLabel("  P1\u00B7x + P2\u00B7y + P3\u00B7z = Result  ");
            formulaBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
            formulaBadge.setForeground(BRAND_MAIN);
            formulaBadge.setOpaque(true);
            formulaBadge.setBackground(BRAND_XLIGHT);
            formulaBadge.setBorder(new EmptyBorder(5, 14, 5, 14));
            titleRow.add(cardTitle,    BorderLayout.WEST);
            titleRow.add(formulaBadge, BorderLayout.EAST);
            JSeparator sep = new JSeparator();
            sep.setForeground(BORDER_COLOR);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            JPanel inputGrid = new JPanel(new GridLayout(2, 2, 22, 20));
            inputGrid.setOpaque(false);
            inputGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
            RoundTextField txP1 = new RoundTextField(10);
            RoundTextField txP2 = new RoundTextField(10);
            RoundTextField txP3 = new RoundTextField(10);
            RoundTextField txR  = new RoundTextField(10);
            for (RoundTextField tf : new RoundTextField[]{txP1, txP2, txP3, txR})
                tf.setBorder(new EmptyBorder(0, 16, 0, 16));
            inputGrid.add(buildInputBlock("Coefficient P1", txP1));
            inputGrid.add(buildInputBlock("Coefficient P2", txP2));
            inputGrid.add(buildInputBlock("Coefficient P3", txP3));
            inputGrid.add(buildInputBlock("Result Value",   txR));
            JLabel hint = new JLabel("x, y, z \u2208 [0, 100]   \u2014   O(n\u00B3) brute-force search on server");
            hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
            hint.setForeground(new Color(170, 185, 185));
            hint.setAlignmentX(LEFT_ALIGNMENT);
            JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
            actionRow.setOpaque(false);
            actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            JButton btnSend  = createPrimaryButton("SEND TO SERVER"); // زر الإرسال
            JButton btnClear = createOutlineButton("CLEAR");           // زر المسح
            statusLabel = new JLabel("\u25CF Ready");
            statusLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            statusLabel.setForeground(BRAND_MAIN);
            statusLabel.setOpaque(true);
            statusLabel.setBackground(BRAND_XLIGHT);
            statusLabel.setBorder(new EmptyBorder(7, 16, 7, 16));
            requestCountLabel = new JLabel("Requests on this connection: 0");
            requestCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
            requestCountLabel.setForeground(TEXT_MUTED);
            actionRow.add(btnSend);
            actionRow.add(btnClear);
            actionRow.add(statusLabel);
            resultPanel = new JPanel(new BorderLayout(0, 8));
            resultPanel.setBackground(BRAND_XLIGHT);
            resultPanel.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BRAND_LIGHT, 12),
                new EmptyBorder(18, 22, 18, 22)
            ));
            resultPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            resultPanel.setVisible(false); // نخبيه حتى يجي رد من السيرفر

            JLabel resTitle = new JLabel("SERVER RESPONSE");
            resTitle.setFont(new Font("SansSerif", Font.BOLD, 9));
            resTitle.setForeground(new Color(130, 160, 160));

            resultText = new JLabel("..."); // نص الجواب، يتحدث بعد الرد
            resultText.setFont(new Font("SansSerif", Font.PLAIN, 13));
            resultText.setForeground(TEXT_DARK);

            resultPanel.add(resTitle,   BorderLayout.NORTH);
            resultPanel.add(resultText, BorderLayout.CENTER);
            btnSend.addActionListener(e -> {
                // نجمع النصوص من الحقول
                String s1 = txP1.getText().trim();
                String s2 = txP2.getText().trim();
                String s3 = txP3.getText().trim();
                String s4 = txR.getText().trim();
                if (s1.isEmpty() || s2.isEmpty() || s3.isEmpty() || s4.isEmpty()) {
                    statusLabel.setText("\u26A0 Fill all fields"); // رمز تحذير + نص
                    statusLabel.setForeground(new Color(180, 80, 60)); // أحمر
                    return;
                }

                statusLabel.setText("\u23F3 Connecting..."); // رمز ساعة رملية
                statusLabel.setForeground(new Color(160, 120, 40));
                resultPanel.setVisible(false); // نخبي الجواب القديم

                //  السوينق ووركر يشتغل بثريد ثاني غير عن ثريد الواجهه لأن إرسال البيانات للسيرفر وانتظار الجواب يمكن ياخذ وقت
                //  ولو شغلناه على ثريد الواجهة  الواجهة تجمد ما تتحرك
                new SwingWorker<String, Void>() {

                    @Override
                    protected String doInBackground() throws Exception {


                        // أول خطوة نفتح البيرسيستنت كونكشن مع السيرفر
                        // لو الكونكشن اوريدي مفتوحة يتخطى هذا السطر ويستخدم اللي عنده
                        openPersistentConnection();

                        //  ثاني خطوه نبدا نقيس الراوند تريب تايم  
                        //    نانو تايم: أدق من ميلي ثانية لقياس الوقت
                        long rttStart = System.nanoTime();


                        // ثالث خطوة نرسل القيم الأربعة للسيرفر عبر الداتا أوتبوت ستريم
                        // الترتيب مهم لأن السيرفر يقرأهم بنفس الترتيب اللي ارسلناهم فيه
                        persistentOut.writeUTF(s1); // المعامل الأول P1
                        persistentOut.writeUTF(s2); // المعامل الثاني P2
                        persistentOut.writeUTF(s3); // المعامل الثالث P3
                        persistentOut.writeUTF(s4); // النتيجة 
                        persistentOut.flush();       // نتأكد إن كل شي انرسل 

                        // رابع خطوة ننتظر الجواب من السيرفر
                        // الثريد يسوي بلوك هنا ويوقف حتى يجي الرد، لو ما جاء في ١٠ ثواني يطلع إيرور
                        String result = persistentIn.readUTF(); // رد السيرفر

                        //خامس خطوه نحسب معادله الار تي تي
                        double rtt = (System.nanoTime() - rttStart) / 1_000_000.0; // نحوّل لميلي ثانية

                        //  سادس خطوة
                        //  نزيد الكاونتر بما انه طلب  ثاني على نفس الكونكشن
                        requestCount++;

                        return String.format(
                            "<html>%s &nbsp;&nbsp;|&nbsp;&nbsp; RTT: <b>%.2f ms</b> &nbsp;|&nbsp; Request #%d on same TCP connection</html>",
                            result, rtt, requestCount
                        );
                    }
                    @Override
                    protected void done() {
                        try {
                            resultText.setText(get()); // نجيب الجواب ونعرضه
                            resultPanel.setVisible(true); // نظهر لوح النتيجة
                            statusLabel.setText("\u2713 Done"); // علامة صح
                            statusLabel.setForeground(new Color(50, 150, 95)); // أخضر
                            requestCountLabel.setText("Requests on this connection: " + requestCount);
                        } catch (Exception ex) {
                            // لو صار خطأ (الكونكشن انقطعت مثلاً)
                            closePersistentConnection(); // نسكر الكونكشن المعطوبة
                            // المرة الجاية يضغط الزر ستُفتح كونكشن جديدة تلقائياً
                            statusLabel.setText("\u26A0 Server offline?");
                            statusLabel.setForeground(new Color(180, 70, 60));
                            resultText.setText(
                                "<html><b>Could not reach server.</b> &nbsp;"
                                + "Make sure EjadServer is running on port " + SERVER_PORT + ".</html>"
                            );
                            resultPanel.setVisible(true);
                        }
                        revalidate(); repaint(); // نجدد رسم الواجهة
                    }
                }.execute(); // نطلق السوينج ووركر
            });
            btnClear.addActionListener(e -> {
                txP1.setText(""); txP2.setText("");
                txP3.setText(""); txR.setText("");
                statusLabel.setText("\u25CF Ready");
                statusLabel.setForeground(BRAND_MAIN);
                resultPanel.setVisible(false);
            });
            card.add(titleRow);
            card.add(Box.createRigidArea(new Dimension(0, 20)));
            card.add(sep);
            card.add(Box.createRigidArea(new Dimension(0, 26)));
            card.add(inputGrid);
            card.add(Box.createRigidArea(new Dimension(0, 12)));
            card.add(hint);
            card.add(Box.createRigidArea(new Dimension(0, 22)));
            card.add(actionRow);
            card.add(Box.createRigidArea(new Dimension(0, 8)));
            card.add(requestCountLabel); // العداد تحت الأزرار
            card.add(Box.createRigidArea(new Dimension(0, 10)));
            card.add(resultPanel);
            return card;
        }
        private JPanel buildInputBlock(String title, JTextField field) {
            JPanel p = new JPanel(new BorderLayout(0, 7));
            p.setOpaque(false);
            JLabel lbl = new JLabel(title.toUpperCase()); // العنوان بأحرف كبيرة
            lbl.setFont(new Font("SansSerif", Font.BOLD, 9));
            lbl.setForeground(new Color(150, 170, 170));
            p.add(lbl,   BorderLayout.NORTH);
            p.add(field, BorderLayout.CENTER);
            return p;
        }
    }
    static class RoundBorder extends javax.swing.border.AbstractBorder {
        private final Color color;  // لون الحد
        private final int   radius; // نصف قطر الاستدارة

        RoundBorder(Color c, int r) { color = c; radius = r; }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y, w-1, h-1, radius, radius); // نرسم الحد المدور
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) {
            int i = radius / 2;
            return new Insets(i, i, i, i); // مسافة داخلية تعتمد على الراديوس
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EjadClient::new);
    }
}