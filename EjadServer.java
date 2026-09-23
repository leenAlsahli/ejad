import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLDocument;

public class EjadServer extends JFrame {

    //  رقم البورت اللي السيرفر يسوي ليسننق عليه 
    // نفس الرقم اللي عند الكلاينت لازم يكونون متطابقين
    private static final int PORT = 12345;

    //   إحصائيات السيرفر اللي فوق باعلى الواجهه
    //هذي مجرد اضافه
    private final AtomicInteger totalConnections = new AtomicInteger(0); // كم اتصال وصل إجمالاً
    private final AtomicInteger activeClients    = new AtomicInteger(0); // كم كلاينت متصل الحين
    private final AtomicInteger totalRequests    = new AtomicInteger(0); // كم طلب معالج إجمالاً

    //ميثود بدايه السيرفر 
    //من هنا نستمع ونقبل الكونكشنز اللي جايه من الكلاينت
    private void startServer() {
        try (ServerSocket masterSocket = new ServerSocket(PORT)) {
            // السيرفر جاهز نسجل الرساله ونحدث الحالة في الهيدر
            log("SERVER", "Ejad Server active and listening on port " + PORT, "#669999");
            SwingUtilities.invokeLater(() -> lblStatus.setText("● LISTENING")); // نحدث الواجهة

            // :لوب لا نهائي تشتغل إلى الأبد تستقبل  كونشنز 
            while (true) {
                // أكسيبت: بتوقف هنا حتى يتصل كلاينت جديد
                // لما يتصل يرجع سوكيت خاص للتواصل مع ذلك الكلاينت فقط
                Socket clientSocket = masterSocket.accept();

                // نحدث الإحصائيات ( برضو اضافه من عندنا)
                totalConnections.incrementAndGet(); // اتصال جديد → نزيد المجموع
                activeClients.incrementAndGet();    // كلاينت نشط جديد
                updateCounters();                   // نحدث الأرقام في الواجهة

                String clientIP = clientSocket.getInetAddress().getHostAddress(); // نجيب آي بي الكلاينت
                log("CONNECT", "Client connected  |  IP: " + clientIP
                    + "  |  Connection #" + totalConnections.get(), "#44bb88");

                // ننشئ ثريد جديد لهذا الكلاينت ونشغله
                // من الآن هذا الثريد يتولى كل التواصل مع هذا الكلاينت
                new EquationProcessor(clientSocket, clientIP).start();

                // السيرفر يرجع فوراً لـ أكسيبت ينتظر الكلاينت اللي بعده
            }
        } catch (IOException e) {
            log("ERROR", "Socket error: " + e.getMessage(), "#cc4444");
        }
    }


    //  ميثود اللوق : زي السجل للنشاط اللي يصير في السيرفر
     //  يعني كل ما يصير حدث  (اتصال جديد، طلب جديد، كلاينت انقطع) نكتب عنه سطر في اللوق
    //  كل رسالة فيها: وقت + تاق (نوع الرسالة) + النص
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    void log(String tag, String message, String color) {
        String time = timeFormat.format(new Date()); 
        // نحسب الوقت الحالي بشكل ساعة:دقيقة:ثانية
        String htmlEntry = String.format(
            "<div style='margin-bottom:4px;'>"
            + "<span style='color:#99aaaa;'>[%s]</span>&nbsp;"       // الوقت: رمادي
            + "<span style='color:%s;'><b>[%s]</b></span>&nbsp;"     // التاق: ملون وغامق
            + "<span style='color:#1a2e2e;'>%s</span>"               // الرسالة: داكن
            + "</div>",
            time, color, tag, message
        );
        SwingUtilities.invokeLater(() -> {
            try {
                HTMLDocument doc  = (HTMLDocument) logPane.getDocument();
                javax.swing.text.Element body = doc.getElement("body");
                if (body != null)
                    doc.insertBeforeEnd(body, htmlEntry); // نضيف في نهاية الـ بودي
                else
                    logPane.setText("<html><body id='body'>" + htmlEntry + "</body></html>");
                logPane.setCaretPosition(doc.getLength()); // نسكرول للأسفل تلقائياً
            } catch (Exception ignored) {}
        });
    }

    // 
    //  الإيكويشن بروسيسور: الثريد اللي يتعامل مع كلاينت واحد
    //  كل كلاينت يتصل ياخذ  نسخة جديدة من هذا الكلاس تشتغل في ثريدها     //
    class EquationProcessor extends Thread {
        private final Socket socket; // سوكيت هذا الكلاينت بالذات
        private final String ip;     // آي بي الكلاينت للوق

        public EquationProcessor(Socket s, String ip) {
            this.socket = s;
            this.ip     = ip;
        }

        //  هذا الميثود يشتغل لما نستدعي ستارت على الثريد 
        @Override
        public void run() {
            // نفتح ستريمز القراءة والكتابة 
            try (DataInputStream  input  = new DataInputStream(socket.getInputStream());
                 DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

                //  اول خطوه رساله الترحيب تنرسل مرة وحدة بس عند فتح الكونكشن
                output.writeUTF("Connection Established. Welcome to Ejad Server.");
                output.flush();

                //  ثاني. خطوه اللوب تدور طالما الكلاينت متصل
                //    كل دورة = طلب واحد (أرقام وهي رايحه + جواب وهي راجعه)
                while (true) {
                    try {
                        // ننتظر القيم الأربعة من الكلاينت (يتوقف الثريد هنا لين ما تجي)
                        // مهم: نقرأهم بنفس الترتيب اللي الكلاينت يرسلهم فيه
                        String p1  = input.readUTF(); // المعامل الأول
                        String p2  = input.readUTF(); // المعامل الثاني
                        String p3  = input.readUTF(); // المعامل الثالث
                        String res = input.readUTF(); // النتيجة المطلوبة

                        // نزيد عداد الطلبات ونحدث الواجهة
                        totalRequests.incrementAndGet();
                        updateCounters();

                        // نسجل الطلب في اللوق
                        log("REQUEST", "IP: " + ip
                            + "  |  P1=" + p1 + "  P2=" + p2 + "  P3=" + p3 + "  R=" + res, "#cc9933");

                        // نحل المعادلة ونجيب الجواب
                        String solution = solveEquation(p1, p2, p3, res);

                        // نرسل الجواب للكلاينت على نفس الكونكشن
                        output.writeUTF(solution);
                        output.flush(); // نتأكد إن البيانات انرسلت فعلاً

                        log("RESPONSE", "IP: " + ip + "  |  " + solution, "#44aa77");

                    } catch (EOFException e) {
                        //    الكلاينت سكر الكونكشن من طرفه
                        break;
                    }
                }

            } catch (IOException e) {
                // خطأ غير متوقع في الاتصال (مثل انقطاع الشبكة فجأة)
                log("WARNING", "Session interrupted  |  IP: " + ip, "#cc4444");
            } finally {
                // هذا الكود يشتغل دائماً سواء خرجنا طبيعي أو بخطأ
                activeClients.decrementAndGet(); // كلاينت انتهى → ننقص العداد
                updateCounters();
                log("DISCONNECT", "Client disconnected  |  IP: " + ip, "#cc6644");
                try { socket.close(); } catch (IOException ignored) {} // نسكر السوكيت
            }
        }

        //  ميثود حل المعادلة 
        private String solveEquation(String s1, String s2, String s3, String s4) {
            int a, b, c, r;

            //  الحالة الأولى تحقق إن الانبوت أرقام صحيحة 
            // بارس انتجر يحول النص لرقم و لو فيه حرف أو رمز يطلع إكسبشن
            try {
                a = Integer.parseInt(s1.trim()); // المعامل الأول
                b = Integer.parseInt(s2.trim()); // المعامل الثاني
                c = Integer.parseInt(s3.trim()); // المعامل الثالث
                r = Integer.parseInt(s4.trim()); // النتيجة المطلوبة
            } catch (NumberFormatException e) {
                // المدخل مو رقم (فيه حروف أو رموز) → نرد بخطأ واضح
                return "Error: Invalid input — numbers only, no letters or symbols.";
            }

            //  الحالة الثانية تحقق إن النتيجة في النطاق  
            if (r > 100 * (a + b + c))
                return "Error: Result is out of range — no possible solution exists.";

            //  الحالة الثالثة البروت فورس 
            // ثلاثه نستد لوبز تجرب كل تركيبة ممكنة
            // أول تركيبة تنجح نرجعها فوراً (ما نكمل الباقي)
            for (int x = 0; x <= 100; x++)       
                for (int y = 0; y <= 100; y++)     
                    for (int z = 0; z <= 100; z++)
                        if ((a * x) + (b * y) + (c * z) == r)
                            return String.format("Success! x=%d, y=%d, z=%d", x, y, z); // وجدنا حل!

            // لو خرجنا من اللوبز بدون ما نلاقي حل يعني ما في حل في النطاق المطلوب
            return "No valid solution exists in the range [0-100].";
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  UI
    // ══════════════════════════════════════════════════════════════════

    // ── الألوان اللي نستخدمها في الواجهة ──
    // نفس البروند حق الكلاينت، خلّينا الشكل موحد بين البرنامجين
    private static final Color BRAND_MAIN   = new Color(102, 153, 153);
    private static final Color BRAND_LIGHT  = new Color(194, 214, 214);
    private static final Color BRAND_XLIGHT = new Color(232, 242, 242);
    private static final Color BG_WHITE     = Color.WHITE;
    private static final Color SOFT_GRAY    = new Color(244, 247, 247);
    private static final Color BORDER_COLOR = new Color(208, 224, 224);
    private static final Color TEXT_DARK    = new Color(26,  46,  46);
    private static final Color TEXT_MUTED   = new Color(100, 120, 120);

    // ── مكونات الواجهة ──
    private JTextPane logPane;                                            // منطقة عرض اللوق (السجل)
    private JLabel lblConnections, lblActive, lblRequests, lblStatus;    // أرقام الإحصائيات والحالة

    // ── المنشئ الرئيسي للسيرفر ──
    // يبني الواجهة ثم يشغل السيرفر في خيط منفصل
    public EjadServer() {
        setTitle("Ejad — Server Console");
        setSize(920, 680);
        setMinimumSize(new Dimension(750, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // يظهر في وسط الشاشة

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // نبني الواجهة من ثلاث مناطق: هيدر فوق، محتوى وسط، فوتر تحت
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        setVisible(true);

        // نشغل السيرفر في خيط منفصل حتى ما يجمد الواجهة
        // لو شغّلناه على نفس خيط الواجهة، الواجهة تتجمد وما تتحرك
        new Thread(this::startServer, "ServerMainThread").start();
    }

    // ══════════════════════════════════════════════════════════════════
    //  ميثود بناء اللوقو (نفس فكرة الكلاينت)
    // ══════════════════════════════════════════════════════════════════
    //  يحاول يحمل الصورة من مسار الكلاس، لو ما لاقاها يرسم بديل نصي
    private JComponent buildLogo(String filename, int size, boolean fallbackBg) {
        try {
            // نحاول نلاقي الصورة داخل مجلد الكلاس (مناسب للجار فايل)
            String imagePath = "/javaapplication26/" + filename;
            java.net.URL imgURL = EjadServer.class.getResource(imagePath);
            if (imgURL == null) imgURL = EjadServer.class.getResource("/" + filename);
            if (imgURL != null) {
                ImageIcon raw = new ImageIcon(imgURL);
                if (raw.getIconWidth() > 0) {
                    Image scaled = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    return new JLabel(new ImageIcon(scaled));
                }
            }
        } catch (Exception ignored) {}

        // لو ما لاقى الصورة → ارسم مربع ملون فيه حروف "EJ" بديلاً عنها
        Color fill = fallbackBg ? new Color(255, 255, 255, 50) : BRAND_MAIN;
        JPanel box = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, (int)(size * 0.36)));
                FontMetrics fm = g2.getFontMetrics();
                String text = "EJ";
                g2.drawString(text,
                    (getWidth()  - fm.stringWidth(text)) / 2,
                    (getHeight() - fm.getHeight())        / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        box.setOpaque(false);
        Dimension d = new Dimension(size, size);
        box.setPreferredSize(d); box.setMaximumSize(d); box.setMinimumSize(d);
        return box;
    }
    //  ميثود تحديث أرقام الإحصائيات في الواجهة
    void updateCounters() {
        SwingUtilities.invokeLater(() -> {
            lblConnections.setText(String.valueOf(totalConnections.get())); // مجموع الاتصالات
            lblActive.setText(String.valueOf(activeClients.get()));         // الكلاينتات النشطة الحين
            lblRequests.setText(String.valueOf(totalRequests.get()));       // مجموع الطلبات المعالجة
        });
    }
    // ══════════════════════════════════════════════════════════════════
    //  بناء الهيدر العلوي
    // ══════════════════════════════════════════════════════════════════
    //  يحتوي: اللوقو + اسم البرنامج يسار، وحالة السيرفر يمين
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BRAND_MAIN);
        header.setPreferredSize(new Dimension(0, 72));
        header.setBorder(new EmptyBorder(0, 28, 0, 28));

        // الجانب الأيسر: اللوقو + العنوانين
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(new EmptyBorder(14, 0, 0, 0));
        leftPanel.add(buildLogo("logo.png", 40, true));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel mainTitle = new JLabel("Ejad");
        mainTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        mainTitle.setForeground(Color.WHITE);
        JLabel subTitle = new JLabel("Server Console");
        subTitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subTitle.setForeground(BRAND_LIGHT);
        titlePanel.add(mainTitle);
        titlePanel.add(subTitle);
        leftPanel.add(titlePanel);

        // الجانب الأيمن: ليبل حالة السيرفر (ستارتنق → ليستننق)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        lblStatus = new JLabel("● STARTING..."); // يتغير لـ LISTENING بعد ما يشتغل السيرفر
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setBorder(new EmptyBorder(5, 14, 5, 14));
        rightPanel.add(lblStatus);

        header.add(leftPanel,  BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    // ══════════════════════════════════════════════════════════════════
    //  بناء المنطقة الوسطى (الإحصائيات + اللوق)
    // ══════════════════════════════════════════════════════════════════
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setBackground(SOFT_GRAY);
        center.setBorder(new EmptyBorder(24, 28, 16, 28));
        center.add(buildStatsRow(), BorderLayout.NORTH);  // صف الإحصائيات فوق
        center.add(buildLogCard(),  BorderLayout.CENTER); // منطقة اللوق تحته
        return center;
    }

    // ── صف الكاردات الإحصائية الثلاث ──
    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0)); // ثلاث كاردات جنب بعض
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, 88));

        // نعرف الليبيلز هنا حتى نقدر نحدثها لاحقاً من الكود
        lblConnections = new JLabel("0");
        lblActive      = new JLabel("0");
        lblRequests    = new JLabel("0");

        // كل كارد بلون مختلف في الشريط الجانبي
        row.add(buildStatCard("Total Connections", lblConnections, new Color(100, 180, 200))); // أزرق
        row.add(buildStatCard("Active Clients",    lblActive,      new Color(100, 200, 150))); // أخضر
        row.add(buildStatCard("Requests Handled",  lblRequests,    new Color(180, 140, 200))); // بنفسجي
        return row;
    }

    // ── بناء كارد إحصائية واحدة ──
    private JPanel buildStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_WHITE);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14); // بطاقة بيضاء مدورة
                g2.setColor(accent);
                g2.fillRect(0, 0, 5, getHeight()); // شريط ملون على اليسار (زخرفة)
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 20, 14, 14));

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 28)); // الرقم بخط كبير
        valueLabel.setForeground(TEXT_DARK);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        titleLabel.setForeground(TEXT_MUTED);

        card.add(valueLabel, BorderLayout.CENTER); // الرقم في المنتصف
        card.add(titleLabel, BorderLayout.SOUTH);  // العنوان تحته
        return card;
    }

    // ── بناء كارد اللوق (سجل النشاط المباشر) ──
    private JPanel buildLogCard() {
        // لوح خارجي أبيض مدور يحتوي اللوق بالكامل
        JPanel outerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_WHITE);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.dispose();
            }
        };
        outerPanel.setOpaque(false);

        // منطقة النص: تعرض رسائل اللوق بفورمات HTML (ألوان مختلفة لكل نوع رسالة)
        logPane = new JTextPane();
        logPane.setEditable(false); // المستخدم ما يقدر يعدل فيه
        logPane.setContentType("text/html"); // نستخدم HTML حتى نلوّن النصوص
        logPane.setText("<html><body id='body' style='font-family:SansSerif; font-size:11px;'></body></html>");
        logPane.setBorder(new EmptyBorder(10, 16, 10, 16));
        logPane.setBackground(new Color(250, 253, 253));

        JScrollPane scroll = new JScrollPane(logPane);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        outerPanel.add(scroll, BorderLayout.CENTER);

        // زر "كلير": يمسح كل رسائل اللوق ويبدأ من أول
        JButton btnClear = new JButton("CLEAR") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BRAND_XLIGHT : BG_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BRAND_MAIN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnClear.setFont(new Font("SansSerif", Font.BOLD, 10));
        btnClear.setForeground(BRAND_MAIN);
        btnClear.setOpaque(false); btnClear.setContentAreaFilled(false);
        btnClear.setBorderPainted(false); btnClear.setFocusPainted(false);
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.setPreferredSize(new Dimension(80, 28));
        // لما يضغط كلير: يرجع اللوق لوضع HTML الابتدائي الفاضي
        btnClear.addActionListener(e ->
            logPane.setText("<html><body id='body' style='font-family:SansSerif; font-size:11px;'></body></html>")
        );

        // شريط علوي فيه العنوان وزر الكلير
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(12, 16, 8, 16));
        JLabel titleLog = new JLabel("Live Activity Log");
        titleLog.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLog.setForeground(TEXT_DARK);
        topBar.add(titleLog, BorderLayout.WEST);
        topBar.add(btnClear, BorderLayout.EAST);
        outerPanel.add(topBar, BorderLayout.NORTH);

        return outerPanel;
    }

    // ══════════════════════════════════════════════════════════════════
    //  بناء الفوتر السفلي
    // ══════════════════════════════════════════════════════════════════
    //  يعرض معلومات ثابتة عن البرنامج في أسفل الشاشة
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.setBackground(SOFT_GRAY);
        footer.setBorder(new EmptyBorder(0, 28, 8, 28));
        JLabel infoLabel = new JLabel("Ejad System \u00A9 2026  \u00B7  Multi-threaded TCP  \u00B7  Port " + PORT);
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        infoLabel.setForeground(TEXT_MUTED);
        footer.add(infoLabel);
        return footer;
    }

    // ══════════════════════════════════════════════════════════════════
    //  نقطة دخول البرنامج (ماين ميثود)
    // ══════════════════════════════════════════════════════════════════
    //  إنفوك ليتر: يشغل البرنامج على خيط الواجهة الرسمي (إي دي تي)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(EjadServer::new);
    }
}