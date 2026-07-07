package bulb.gui;

import bulb.model.BulbState;
import bulb.model.DaySchedule;
import bulb.service.BulbController;
import bulb.service.HolidayService;
import bulb.service.ScheduleEngine;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BulbSchedulerGUI extends JFrame {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy (EEEE)");

    private final HolidayService holidayService = new HolidayService();
    private final ScheduleEngine scheduleEngine = new ScheduleEngine();
    private final BulbController bulbController = new BulbController();

    private Map<LocalDate, String> holidays = new ConcurrentHashMap<>();

    private final JComboBox<String> countryBox =
            new JComboBox<>(new String[]{"IN - India", "US - United States", "GB - United Kingdom",
                    "AU - Australia", "DE - Germany", "CA - Canada"});
    private final JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(
            LocalDate.now().getYear(), 2000, 2100, 1));
    private final JLabel loadStatusLabel = new JLabel("Holidays not loaded yet.");

    private final BulbIndicator[] bulbIndicators = new BulbIndicator[8]; // 1..7 used
    private final JLabel todayLabel = new JLabel();

    private final DefaultTableModel weekTableModel =
            new DefaultTableModel(new Object[]{"Date", "Day", "Status", "Holiday"}, 0);
    private final JTable weekTable = new JTable(weekTableModel);

    private final JSpinner simDateSpinner;
    private final JTextArea logArea = new JTextArea();

    private final Timer clockTimer;
    private LocalDate lastAutoTriggeredDate = null;

    public BulbSchedulerGUI() {
        super("Bulb Trigger Scheduler - Odd/Even Weekly Rotation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setSize(980, 720);
        setLocationRelativeTo(null);

        // ---- Top: holiday calendar controls ----
        add(buildTopPanel(), BorderLayout.NORTH);

        // ---- Center: bulbs + today status + week table ----
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(buildBulbPanel(), BorderLayout.NORTH);
        center.add(buildWeekTablePanel(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // ---- Simulation controls ----
        SpinnerDateModel dateModel = new SpinnerDateModel();
        simDateSpinner = new JSpinner(dateModel);
        simDateSpinner.setEditor(new JSpinner.DateEditor(simDateSpinner, "dd-MM-yyyy"));

        // ---- Bottom: simulate + trigger + log ----
        add(buildBottomPanel(), BorderLayout.SOUTH);

        bulbController.setLogSink(this::appendLog);

        // Load holidays for current year/country immediately, then refresh view.
        loadHolidaysAsync();

        // Auto-check every 30s whether the calendar date changed -> auto trigger today's pattern.
        clockTimer = new Timer(30_000, e -> autoCheckDayRollover());
        clockTimer.start();
    }

    // ---------------------------------------------------------------- UI ----

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Government Holiday Calendar (live via date.nager.at)"));
        panel.add(new JLabel("Country:"));
        panel.add(countryBox);
        panel.add(new JLabel("Year:"));
        panel.add(yearSpinner);
        JButton loadBtn = new JButton("Load Holidays");
        loadBtn.addActionListener(e -> loadHolidaysAsync());
        panel.add(loadBtn);
        panel.add(loadStatusLabel);
        return panel;
    }

    private JPanel buildBulbPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createTitledBorder("Bulbs (1-7)"));

        JPanel bulbs = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        for (int i = 1; i <= 7; i++) {
            BulbIndicator b = new BulbIndicator(i);
            bulbIndicators[i] = b;
            bulbs.add(b);
        }
        wrapper.add(bulbs, BorderLayout.CENTER);

        todayLabel.setFont(todayLabel.getFont().deriveFont(Font.BOLD, 15f));
        todayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        wrapper.add(todayLabel, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildWeekTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("This Week's Schedule (Mon-Sun, Sunday counted as working)"));
        weekTable.setRowHeight(24);
        weekTable.setEnabled(false);
        panel.add(new JScrollPane(weekTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel outer = new JPanel(new BorderLayout(8, 8));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.setBorder(BorderFactory.createTitledBorder("Simulate / Trigger"));
        controls.add(new JLabel("Pick a date:"));
        controls.add(simDateSpinner);
        JButton simulateBtn = new JButton("Simulate This Date");
        simulateBtn.addActionListener(e -> simulateSelectedDate());
        controls.add(simulateBtn);

        JButton triggerTodayBtn = new JButton("Trigger Today Now");
        triggerTodayBtn.addActionListener(e -> triggerRealDay(LocalDate.now()));
        controls.add(triggerTodayBtn);

        outer.add(controls, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(900, 160));
        logScroll.setBorder(BorderFactory.createTitledBorder("Event Log"));
        outer.add(logScroll, BorderLayout.CENTER);

        return outer;
    }

    // ----------------------------------------------------------- actions ----

    private void loadHolidaysAsync() {
        loadStatusLabel.setText("Loading holidays...");
        String countryCode = ((String) countryBox.getSelectedItem()).substring(0, 2);
        int year = (Integer) yearSpinner.getValue();

        new SwingWorker<Map<LocalDate, String>, Void>() {
            @Override
            protected Map<LocalDate, String> doInBackground() {
                return holidayService.getHolidays(year, countryCode);
            }

            @Override
            protected void done() {
                try {
                    holidays = get();
                    loadStatusLabel.setText(holidays.size() + " holiday(s) loaded for "
                            + countryCode + " " + year + ".");
                    appendLog("Loaded " + holidays.size() + " holidays for " + countryCode + " " + year);
                } catch (Exception ex) {
                    loadStatusLabel.setText("Failed to load holidays: " + ex.getMessage());
                }
                refreshWeekView(LocalDate.now());
                triggerRealDay(LocalDate.now());
            }
        }.execute();
    }

    private void refreshWeekView(LocalDate anchorDate) {
        List<DaySchedule> week = scheduleEngine.computeWeek(anchorDate, holidays);
        weekTableModel.setRowCount(0);
        for (DaySchedule ds : week) {
            weekTableModel.addRow(new Object[]{
                    ds.getDate(),
                    ds.getDayOfWeek(),
                    ds.isHoliday() ? "HOLIDAY (shifted)" : ds.getState().toString(),
                    ds.getHolidayName() == null ? "-" : ds.getHolidayName()
            });
        }
    }

    /** Applies (and physically/virtually triggers) the pattern for a real calendar day. */
    private void triggerRealDay(LocalDate date) {
        refreshWeekView(date);
        DaySchedule ds = scheduleEngine.getForDate(date, holidays);
        applyToBulbs(ds);
        lastAutoTriggeredDate = date;
        appendLog("TRIGGERED " + ds.describe());
    }

    /** Preview-only: shows what the bulbs WOULD do on a chosen date, without altering "last real trigger". */
    private void simulateSelectedDate() {
        java.util.Date d = (java.util.Date) simDateSpinner.getValue();
        LocalDate picked = d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        refreshWeekView(picked);
        DaySchedule ds = scheduleEngine.getForDate(picked, holidays);
        applyToBulbs(ds);
        appendLog("SIMULATED " + ds.describe());
    }

    private void applyToBulbs(DaySchedule ds) {
        bulbController.applyPattern(ds.getState().activeBulbs());
        for (int i = 1; i <= 7; i++) {
            bulbIndicators[i].setOn(bulbController.isOn(i));
        }
        String status;
        if (ds.isHoliday()) {
            status = ds.getDate().format(FMT) + "  -->  HOLIDAY"
                    + (ds.getHolidayName() != null ? " (" + ds.getHolidayName() + ")" : "")
                    + " - no bulbs triggered, shifted to next working day";
            todayLabel.setForeground(new Color(180, 40, 40));
        } else {
            status = ds.getDate().format(FMT) + "  -->  " + ds.getState()
                    + " bulbs triggered " + java.util.Arrays.toString(ds.getState().activeBulbs());
            todayLabel.setForeground(new Color(20, 110, 20));
        }
        todayLabel.setText(status);
    }

    private void autoCheckDayRollover() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastAutoTriggeredDate)) {
            appendLog("Date changed -> re-evaluating schedule for " + today);
            triggerRealDay(today);
        }
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // --------------------------------------------------------- component ----

    /** Small custom-drawn round bulb indicator. */
    private static class BulbIndicator extends JPanel {
        private final int id;
        private boolean on = false;

        BulbIndicator(int id) {
            this.id = id;
            setPreferredSize(new Dimension(80, 100));
        }

        void setOn(boolean on) {
            this.on = on;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int diameter = 55;
            int x = (getWidth() - diameter) / 2;
            int y = 5;

            g2.setColor(on ? new Color(255, 215, 60) : new Color(90, 90, 90));
            g2.fillOval(x, y, diameter, diameter);
            g2.setColor(Color.DARK_GRAY);
            g2.drawOval(x, y, diameter, diameter);

            if (on) {
                g2.setColor(new Color(255, 230, 120, 120));
                g2.fillOval(x - 8, y - 8, diameter + 16, diameter + 16);
            }

            g2.setColor(Color.BLACK);
            String label = "Bulb " + id;
            FontMetrics fm = g2.getFontMetrics();
            int textX = (getWidth() - fm.stringWidth(label)) / 2;
            g2.drawString(label, textX, y + diameter + 20);
        }
    }
}
