import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Emergency Service Dispatch Prioritization System
 * Roll Numbers: 1RV25MC052, 1RV25MC053
 *
 * Algorithms Implemented:
 * 1. Priority Queue (Heap-based) — O(n log n)
 * 2. Merge Sort                  — O(n log n)
 * 3. Quick Sort                  — O(n log n) average, O(n²) worst
 *
 * Design Technique : Greedy — always dispatch the highest-priority request first.
 * Data Structures  : MaxHeap (PriorityQueue), ArrayList (Merge/Quick Sort)
 */
public class EmergencyDispatch extends JFrame {

    // ─────────────────────────────────────────────────────────────────
    //  Domain Model
    // ─────────────────────────────────────────────────────────────────

    enum Priority {
        CRITICAL(4), HIGH(3), MEDIUM(2), LOW(1);
        final int value;
        Priority(int v) { this.value = v; }
    }

    enum ServiceType { AMBULANCE, FIRE, POLICE }

    static class EmergencyRequest implements Comparable<EmergencyRequest> {
        int id;
        String callerName;
        ServiceType type;
        Priority priority;
        int responseTime;
        String location;

        EmergencyRequest(int id, String callerName, ServiceType type,
                         Priority priority, int responseTime, String location) {
            this.id = id; this.callerName = callerName; this.type = type;
            this.priority = priority; this.responseTime = responseTime; this.location = location;
        }

        @Override
        public int compareTo(EmergencyRequest o) {
            if (this.priority.value != o.priority.value)
                return Integer.compare(o.priority.value, this.priority.value);
            return Integer.compare(this.responseTime, o.responseTime);
        }

        @Override
        public String toString() {
            return String.format("ID%-3d | %-10s | %-10s | %-8s | %3d min | %s",
                    id, callerName, type, priority, responseTime, location);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Visualisation Step Model
    // ─────────────────────────────────────────────────────────────────

    static class VizStep {
        List<EmergencyRequest> state;
        Set<Integer> highlighted;
        String description;

        VizStep(List<EmergencyRequest> state, Set<Integer> hl, String desc) {
            this.state = new ArrayList<>(state);
            this.highlighted = new HashSet<>(hl);
            this.description = desc;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Bar Chart Visualisation Panel
    // ─────────────────────────────────────────────────────────────────

    class VizPanel extends JPanel {
        private List<EmergencyRequest> barState = new ArrayList<>();
        private Set<Integer> highlighted = new HashSet<>();
        private Map<Integer, Float> animWidths = new HashMap<>();
        private Map<Integer, Float> targetWidths = new HashMap<>();
        private javax.swing.Timer animTimer;

        VizPanel() {
            setBackground(BG_CARD);
            setBorder(BorderFactory.createLineBorder(BORDER_CLR));
            setPreferredSize(new Dimension(0, 210));

            animTimer = new javax.swing.Timer(16, e -> {
                boolean changed = false;
                for (Integer id : targetWidths.keySet()) {
                    float cur = animWidths.getOrDefault(id, 0f);
                    float tgt = targetWidths.get(id);
                    float next = cur + (tgt - cur) * 0.14f;
                    if (Math.abs(next - tgt) < 0.002f) next = tgt;
                    else changed = true;
                    animWidths.put(id, next);
                }
                repaint();
                if (!changed) animTimer.stop();
            });
        }

        void setState(List<EmergencyRequest> state, Set<Integer> hl) {
            this.barState = new ArrayList<>(state);
            this.highlighted = new HashSet<>(hl);

            int maxScore = state.stream()
                    .mapToInt(r -> r.priority.value * 1000 - r.responseTime)
                    .max().orElse(1);

            for (EmergencyRequest r : state) {
                float tgt = Math.max(0.05f,
                        (float)(r.priority.value * 1000 - r.responseTime) / maxScore);
                targetWidths.put(r.id, tgt);
                animWidths.putIfAbsent(r.id, 0f);
            }
            animTimer.restart();
        }

        private Color prioColor(Priority p) {
            switch (p) {
                case CRITICAL: return new Color(226, 75,  74);
                case HIGH:     return new Color(239, 159, 39);
                case MEDIUM:   return new Color(55,  138, 221);
                default:       return new Color(99,  153, 34);
            }
        }

        private Color withAlpha(Color c, int alpha) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (barState.isEmpty()) {
                g2.setColor(TEXT_DIM);
                g2.setFont(new Font("Consolas", Font.PLAIN, 12));
                String msg = "Run Sort & Dispatch to see step-by-step visualisation.";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2,
                        getHeight() / 2 + 4);
                return;
            }

            int n = barState.size();
            int padL = 115, padR = 55, padT = 12, padB = 12;
            int totalH = getHeight() - padT - padB;
            int barH   = Math.min(22, (totalH / n) - 5);
            int gap    = Math.max(3, (totalH - n * barH) / Math.max(1, n - 1));
            int trackW = getWidth() - padL - padR;

            for (int i = 0; i < n; i++) {
                EmergencyRequest r = barState.get(i);
                int y = padT + i * (barH + gap);
                boolean isHL = highlighted.contains(r.id);
                Color col = prioColor(r.priority);

                // Name label
                g2.setFont(new Font("Consolas", Font.PLAIN, 11));
                g2.setColor(isHL ? TEXT_MAIN : TEXT_DIM);
                String lbl = r.callerName.length() > 13
                        ? r.callerName.substring(0, 12) + "…" : r.callerName;
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(lbl, padL - fm.stringWidth(lbl) - 7, y + barH / 2 + 4);

                // Track background
                g2.setColor(BG_INPUT);
                g2.fillRoundRect(padL, y, trackW, barH, 6, 6);

                // Animated fill
                float w = animWidths.getOrDefault(r.id,
                        targetWidths.getOrDefault(r.id, 0f));
                int fillW = Math.max(6, (int)(trackW * w));
                g2.setColor(isHL ? col : withAlpha(col, 110));
                g2.fillRoundRect(padL, y, fillW, barH, 6, 6);

                // Label inside bar
                if (fillW > 70) {
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    g2.setColor(isHL ? Color.WHITE : col.brighter());
                    g2.drawString(r.priority + " · " + r.responseTime + "m",
                            padL + 7, y + barH / 2 + 4);
                }

                // Highlight outline
                if (isHL) {
                    g2.setColor(col);
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawRoundRect(padL, y, trackW, barH, 6, 6);
                    g2.setStroke(new BasicStroke(1f));
                }

                // Dispatch badge on right
                if (dispatchOrder.containsKey(r.id)) {
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    g2.setColor(isHL ? col : TEXT_DIM);
                    g2.drawString("#" + dispatchOrder.get(r.id),
                            padL + trackW + 7, y + barH / 2 + 4);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Algorithm 1 — Priority Queue (Max-Heap) — O(n log n)
    // ─────────────────────────────────────────────────────────────────

    static List<EmergencyRequest> sortByPriorityQueue(List<EmergencyRequest> requests) {
        PriorityQueue<EmergencyRequest> maxHeap =
                new PriorityQueue<>(Comparator.reverseOrder());
        for (EmergencyRequest r : requests) maxHeap.offer(r);
        List<EmergencyRequest> result = new ArrayList<>();
        while (!maxHeap.isEmpty()) result.add(maxHeap.poll());
        return result;
    }

    static List<VizStep> buildStepsPQ(List<EmergencyRequest> input) {
        List<VizStep> steps = new ArrayList<>();
        List<EmergencyRequest> heap = new ArrayList<>(input);
        heap.sort(Comparator.reverseOrder());

        steps.add(new VizStep(input, Collections.emptySet(),
                "Step 0: All requests inserted into Max-Heap (heapified by priority)."));

        List<EmergencyRequest> remaining = new ArrayList<>(heap);
        List<EmergencyRequest> sorted = new ArrayList<>();

        for (int i = 0; i < heap.size(); i++) {
            EmergencyRequest top = remaining.get(0);
            Set<Integer> hl = Collections.singleton(top.id);
            List<EmergencyRequest> cur = new ArrayList<>(remaining);
            steps.add(new VizStep(cur, hl,
                    "Step " + (i + 1) + ": Extract-max → " + top.callerName +
                    " (" + top.priority + ", " + top.responseTime + " min) → Dispatch #" + (i + 1)));
            remaining.remove(0);
            sorted.add(top);
        }

        Set<Integer> allIds = new HashSet<>();
        for (EmergencyRequest r : sorted) allIds.add(r.id);
        steps.add(new VizStep(sorted, allIds,
                "Done: All " + sorted.size() + " requests extracted in optimal dispatch order."));
        return steps;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Algorithm 2 — Merge Sort — O(n log n)
    // ─────────────────────────────────────────────────────────────────

    static List<EmergencyRequest> mergeSort(List<EmergencyRequest> list) {
        if (list.size() <= 1) return new ArrayList<>(list);
        int mid = list.size() / 2;
        List<EmergencyRequest> L = mergeSort(list.subList(0, mid));
        List<EmergencyRequest> R = mergeSort(list.subList(mid, list.size()));
        return mergeTwo(L, R);
    }

    private static List<EmergencyRequest> mergeTwo(List<EmergencyRequest> L,
                                                     List<EmergencyRequest> R) {
        List<EmergencyRequest> merged = new ArrayList<>();
        int i = 0, j = 0;
        while (i < L.size() && j < R.size()) {
            if (L.get(i).compareTo(R.get(j)) <= 0) merged.add(L.get(i++));
            else merged.add(R.get(j++));
        }
        while (i < L.size())  merged.add(L.get(i++));
        while (j < R.size())  merged.add(R.get(j++));
        return merged;
    }

    static List<VizStep> buildStepsMS(List<EmergencyRequest> input) {
        List<VizStep> steps = new ArrayList<>();
        steps.add(new VizStep(input, Collections.emptySet(),
                "Step 0: Initial list. Merge Sort divides list in half recursively."));

        List<EmergencyRequest> working = new ArrayList<>(input);
        int size = 1, stepNum = 1;

        while (size < working.size()) {
            List<EmergencyRequest> next = new ArrayList<>();
            for (int lo = 0; lo < working.size(); lo += size * 2) {
                int mid = Math.min(lo + size, working.size());
                int hi  = Math.min(lo + size * 2, working.size());
                List<EmergencyRequest> L = new ArrayList<>(working.subList(lo, mid));
                List<EmergencyRequest> R = new ArrayList<>(working.subList(mid, hi));
                List<EmergencyRequest> merged = mergeTwo(L, R);
                next.addAll(merged);

                Set<Integer> hl = new HashSet<>();
                for (EmergencyRequest r : merged) hl.add(r.id);
                List<EmergencyRequest> display = new ArrayList<>(next);
                display.addAll(working.subList(hi, working.size()));
                steps.add(new VizStep(display, hl,
                        "Step " + stepNum + ": Merge (width=" + size + ") — " +
                        L.size() + "+" + R.size() + " → winner: " +
                        merged.get(0).callerName + " (" + merged.get(0).priority + ")"));
                stepNum++;
            }
            working = next;
            size *= 2;
        }

        Set<Integer> allIds = new HashSet<>();
        for (EmergencyRequest r : working) allIds.add(r.id);
        steps.add(new VizStep(working, allIds,
                "Done: Merge Sort complete — stable O(n log n) ordering."));
        return steps;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Algorithm 3 — Quick Sort — O(n log n) avg, O(n²) worst
    // ─────────────────────────────────────────────────────────────────

    static List<EmergencyRequest> quickSort(List<EmergencyRequest> input) {
        List<EmergencyRequest> list = new ArrayList<>(input);
        qsHelper(list, 0, list.size() - 1);
        return list;
    }

    private static void qsHelper(List<EmergencyRequest> list, int lo, int hi) {
        if (lo < hi) {
            int pi = qsPartition(list, lo, hi);
            qsHelper(list, lo, pi - 1);
            qsHelper(list, pi + 1, hi);
        }
    }

    private static int qsPartition(List<EmergencyRequest> list, int lo, int hi) {
        int ri = lo + (int)(Math.random() * (hi - lo + 1));
        Collections.swap(list, ri, hi);
        EmergencyRequest pivot = list.get(hi);
        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            if (list.get(j).compareTo(pivot) <= 0) { i++; Collections.swap(list, i, j); }
        }
        Collections.swap(list, i + 1, hi);
        return i + 1;
    }

    static List<VizStep> buildStepsQS(List<EmergencyRequest> input) {
        List<VizStep> steps = new ArrayList<>();
        List<EmergencyRequest> arr = new ArrayList<>(input);
        steps.add(new VizStep(arr, Collections.emptySet(),
                "Step 0: Initial list. Quick Sort selects a random pivot each pass."));
        int[] sn = {1};
        qsViz(arr, 0, arr.size() - 1, steps, sn);
        Set<Integer> all = new HashSet<>();
        for (EmergencyRequest r : arr) all.add(r.id);
        steps.add(new VizStep(new ArrayList<>(arr), all,
                "Done: Quick Sort complete — dispatching in order."));
        return steps;
    }

    private static void qsViz(List<EmergencyRequest> arr, int lo, int hi,
                               List<VizStep> steps, int[] sn) {
        if (lo >= hi) return;
        int ri = lo + (int)(Math.random() * (hi - lo + 1));
        Collections.swap(arr, ri, hi);
        EmergencyRequest piv = arr.get(hi);
        steps.add(new VizStep(new ArrayList<>(arr),
                Collections.singleton(piv.id),
                "Step " + sn[0]++ + ": Pivot → " + piv.callerName +
                " (" + piv.priority + ", " + piv.responseTime + " min). Partitioning sub-array [" + lo + ".." + hi + "]."));
        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            if (arr.get(j).compareTo(piv) <= 0) { i++; Collections.swap(arr, i, j); }
        }
        Collections.swap(arr, i + 1, hi);
        int p = i + 1;
        steps.add(new VizStep(new ArrayList<>(arr),
                Collections.singleton(arr.get(p).id),
                "Step " + sn[0]++ + ": Placed " + arr.get(p).callerName +
                " at position " + p + ". Left partition: " + (p - lo) + ", Right: " + (hi - p) + "."));
        qsViz(arr, lo, p - 1, steps, sn);
        qsViz(arr, p + 1, hi, steps, sn);
    }

    // ─────────────────────────────────────────────────────────────────
    //  GUI State
    // ─────────────────────────────────────────────────────────────────

    private List<EmergencyRequest> requests = new ArrayList<>();
    private int nextId = 1;
    private Map<Integer, Integer> dispatchOrder = new HashMap<>();

    private List<VizStep> vizSteps = new ArrayList<>();
    private int vizCur = 0;
    private javax.swing.Timer playTimer;

    private JTextField tfName, tfLocation, tfTime;
    private JComboBox<ServiceType> cbType;
    private JComboBox<Priority>    cbPriority;
    private JComboBox<String>      cbAlgorithm;

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblStatus, lblComplexity, lblStepInfo, lblStepCounter;
    private JTextArea taLog;
    private VizPanel vizPanel;
    private JButton btnPrev, btnNext, btnPlay;
    private JSlider sldSpeed;

    // Palette — works on light and dark system themes
    private static final Color BG_DARK    = new Color(13,  17,  30);
    private static final Color BG_CARD    = new Color(20,  26,  46);
    private static final Color BG_INPUT   = new Color(28,  36,  62);
    private static final Color ACCENT_RED = new Color(226, 75,  74);
    private static final Color ACCENT_GRN = new Color(99,  153, 34);
    private static final Color ACCENT_BLU = new Color(55,  138, 221);
    private static final Color TEXT_MAIN  = new Color(230, 235, 255);
    private static final Color TEXT_DIM   = new Color(120, 135, 170);
    private static final Color BORDER_CLR = new Color(38,  50,  82);

    // ─────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────

    public EmergencyDispatch() {
        super("Emergency Dispatch Prioritization System  |  1RV25MC052 & 1RV25MC053");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 860);
        setMinimumSize(new Dimension(1050, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildMain(),      BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        loadSampleData();
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Header
    // ─────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        p.setBackground(BG_CARD);
        p.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT_RED));

        JLabel icon = new JLabel("🚨");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JLabel title = new JLabel("Emergency Dispatch Prioritization");
        title.setFont(new Font("Georgia", Font.BOLD, 17));
        title.setForeground(TEXT_MAIN);

        JLabel sub = new JLabel("  —  Sorting Visualisation  |  1RV25MC052 & 1RV25MC053");
        sub.setFont(new Font("Consolas", Font.PLAIN, 11));
        sub.setForeground(TEXT_DIM);

        p.add(icon); p.add(title); p.add(sub);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Main Split
    // ─────────────────────────────────────────────────────────────────

    private JSplitPane buildMain() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeft(), buildRight());
        sp.setDividerLocation(318);
        sp.setDividerSize(4);
        sp.setBorder(null);
        sp.setBackground(BG_DARK);
        return sp;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Left Panel
    // ─────────────────────────────────────────────────────────────────

    private JScrollPane buildLeft() {
        JPanel p = new JPanel();
        p.setBackground(BG_DARK);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(10, 10, 10, 6));

        p.add(buildInputCard());
        p.add(vgap(8));
        p.add(buildAlgoCard());
        p.add(vgap(8));
        p.add(buildComplexityCard());
        p.add(Box.createVerticalGlue());

        JScrollPane sp = new JScrollPane(p);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG_DARK);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    private JPanel buildInputCard() {
        JPanel c = card("ADD EMERGENCY REQUEST");
        String[] labels = {"Caller Name", "Service Type", "Priority", "Response Time (min)", "Location"};
        tfName     = field("e.g. Ravi Kumar");
        cbType     = combo(ServiceType.values());
        cbPriority = combo(Priority.values());
        tfTime     = field("5");
        tfLocation = field("e.g. Koramangala");
        Component[] comps = {tfName, cbType, cbPriority, tfTime, tfLocation};

        for (int i = 0; i < labels.length; i++) {
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(new Font("Consolas", Font.PLAIN, 10));
            lbl.setForeground(TEXT_DIM);
            lbl.setAlignmentX(LEFT_ALIGNMENT);
            c.add(lbl); c.add(vgap(2));
            JComponent jc = (JComponent) comps[i];
            jc.setAlignmentX(LEFT_ALIGNMENT);
            jc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            c.add(jc); c.add(vgap(6));
        }

        JButton add = btn("＋  Add Request", ACCENT_RED);
        add.setAlignmentX(LEFT_ALIGNMENT);
        add.addActionListener(e -> addRequest());

        JButton clr = btn("⊘  Clear All", new Color(55, 30, 30));
        clr.setAlignmentX(LEFT_ALIGNMENT);
        clr.addActionListener(e -> clearAll());

        c.add(add); c.add(vgap(5)); c.add(clr);
        return c;
    }

    private JPanel buildAlgoCard() {
        JPanel c = card("SELECT & RUN ALGORITHM");
        cbAlgorithm = new JComboBox<>(new String[]{
            "1. Priority Queue (Max-Heap)",
            "2. Merge Sort",
            "3. Quick Sort"
        });
        cbAlgorithm.setBackground(BG_INPUT);
        cbAlgorithm.setForeground(TEXT_MAIN);
        cbAlgorithm.setFont(new Font("Consolas", Font.PLAIN, 12));
        cbAlgorithm.setAlignmentX(LEFT_ALIGNMENT);
        cbAlgorithm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        cbAlgorithm.addActionListener(e -> updateComplexity());

        JButton run = btn("▶  Sort & Dispatch", new Color(22, 101, 52));
        run.setAlignmentX(LEFT_ALIGNMENT);
        run.addActionListener(e -> runSort());

        c.add(cbAlgorithm); c.add(vgap(7)); c.add(run);
        return c;
    }

    private JPanel buildComplexityCard() {
        JPanel c = card("COMPLEXITY ANALYSIS");
        lblComplexity = new JLabel("<html>" + complexityHtml(0) + "</html>");
        lblComplexity.setForeground(TEXT_MAIN);
        lblComplexity.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblComplexity.setAlignmentX(LEFT_ALIGNMENT);
        c.add(lblComplexity);
        return c;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Right Panel
    // ─────────────────────────────────────────────────────────────────

    private JPanel buildRight() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(BG_DARK);
        p.setBorder(new EmptyBorder(10, 6, 10, 10));
        p.add(buildVizSection(), BorderLayout.NORTH);

        JScrollPane tableScroll = new JScrollPane(buildTable());
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER_CLR));
        tableScroll.getViewport().setBackground(BG_CARD);

        taLog = new JTextArea(5, 0);
        taLog.setEditable(false);
        taLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        taLog.setBackground(new Color(8, 11, 20));
        taLog.setForeground(new Color(100, 220, 140));
        taLog.setBorder(new EmptyBorder(5, 8, 5, 8));

        JLabel logTitle = new JLabel("  DISPATCH LOG");
        logTitle.setFont(new Font("Consolas", Font.BOLD, 10));
        logTitle.setForeground(ACCENT_GRN);
        logTitle.setBackground(new Color(8, 11, 20));
        logTitle.setOpaque(true);
        logTitle.setBorder(new EmptyBorder(3, 8, 3, 0));

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(BG_DARK);
        logPanel.add(logTitle, BorderLayout.NORTH);
        logPanel.add(new JScrollPane(taLog), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, logPanel);
        split.setResizeWeight(0.75);
        split.setDividerSize(4);
        split.setBorder(null);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildVizSection() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBackground(BG_DARK);
        wrapper.setBorder(new EmptyBorder(0, 0, 4, 0));

        // Title row
        JLabel title = new JLabel("SORT VISUALISATION");
        title.setFont(new Font("Consolas", Font.BOLD, 10));
        title.setForeground(ACCENT_BLU);

        lblStepCounter = new JLabel("");
        lblStepCounter.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblStepCounter.setForeground(TEXT_DIM);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(BG_DARK);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(lblStepCounter, BorderLayout.EAST);

        // Viz canvas
        vizPanel = new VizPanel();

        // Step description label
        lblStepInfo = new JLabel("Run Sort & Dispatch to see step-by-step visualisation.");
        lblStepInfo.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblStepInfo.setForeground(TEXT_DIM);
        lblStepInfo.setBorder(new EmptyBorder(3, 2, 0, 0));

        // Control buttons
        btnPrev = ctrlBtn("◀  Prev");
        btnNext = ctrlBtn("Next  ▶");
        btnPlay = ctrlBtn("▶  Play");
        btnPrev.addActionListener(e -> { stopPlay(); showStep(vizCur - 1); });
        btnNext.addActionListener(e -> { stopPlay(); showStep(vizCur + 1); });
        btnPlay.addActionListener(e -> togglePlay());

        sldSpeed = new JSlider(1, 5, 3);
        sldSpeed.setBackground(BG_DARK);
        sldSpeed.setForeground(TEXT_DIM);
        sldSpeed.setPreferredSize(new Dimension(80, 20));

        JLabel spdLbl = new JLabel("Speed");
        spdLbl.setFont(new Font("Consolas", Font.PLAIN, 10));
        spdLbl.setForeground(TEXT_DIM);

        setVizEnabled(false);

        JPanel ctrlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ctrlRow.setBackground(BG_DARK);
        ctrlRow.add(btnPrev); ctrlRow.add(btnNext); ctrlRow.add(btnPlay);
        ctrlRow.add(Box.createHorizontalStrut(10));
        ctrlRow.add(spdLbl); ctrlRow.add(sldSpeed);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG_DARK);
        bottom.add(lblStepInfo, BorderLayout.NORTH);
        bottom.add(ctrlRow,    BorderLayout.SOUTH);

        wrapper.add(titleRow, BorderLayout.NORTH);
        wrapper.add(vizPanel, BorderLayout.CENTER);
        wrapper.add(bottom,   BorderLayout.SOUTH);
        return wrapper;
    }

    private JTable buildTable() {
        String[] cols = {"#", "Caller", "Type", "Priority", "ETA (min)", "Location", "Dispatch"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_MAIN);
        table.setFont(new Font("Consolas", Font.PLAIN, 12));
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(40, 60, 100));
        table.setSelectionForeground(TEXT_MAIN);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        JTableHeader hdr = table.getTableHeader();
        hdr.setBackground(new Color(12, 16, 30));
        hdr.setForeground(TEXT_DIM);
        hdr.setFont(new Font("Consolas", Font.BOLD, 10));
        hdr.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT_RED));
        hdr.setReorderingAllowed(false);

        // Priority renderer
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? new Color(40, 60, 100) : BG_CARD);
                setHorizontalAlignment(CENTER);
                String s = val == null ? "" : val.toString();
                switch (s) {
                    case "CRITICAL": setForeground(new Color(226, 75,  74));  break;
                    case "HIGH":     setForeground(new Color(239, 159, 39));  break;
                    case "MEDIUM":   setForeground(new Color(55,  138, 221)); break;
                    default:         setForeground(new Color(99,  153, 34));  break;
                }
                return this;
            }
        });

        // Dispatch order renderer
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? new Color(40, 60, 100) : BG_CARD);
                setHorizontalAlignment(CENTER);
                String s = val == null ? "" : val.toString();
                setForeground(s.isEmpty() ? TEXT_DIM : new Color(99, 200, 150));
                return this;
            }
        });

        DefaultTableCellRenderer ctr = new DefaultTableCellRenderer();
        ctr.setHorizontalAlignment(JLabel.CENTER);
        ctr.setBackground(BG_CARD);
        ctr.setForeground(TEXT_MAIN);
        table.getColumnModel().getColumn(4).setCellRenderer(ctr);

        int[] widths = {32, 110, 88, 80, 64, 185, 80};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        return table;
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        p.setBackground(new Color(8, 10, 18));
        p.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_CLR));
        lblStatus = new JLabel("Ready.");
        lblStatus.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblStatus.setForeground(TEXT_DIM);
        p.add(lblStatus);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Viz Control
    // ─────────────────────────────────────────────────────────────────

    private void showStep(int i) {
        if (vizSteps.isEmpty()) return;
        vizCur = Math.max(0, Math.min(i, vizSteps.size() - 1));
        VizStep s = vizSteps.get(vizCur);
        vizPanel.setState(s.state, s.highlighted);
        lblStepInfo.setText(s.description);
        lblStepCounter.setText("Step " + (vizCur + 1) + " / " + vizSteps.size());
        btnPrev.setEnabled(vizCur > 0);
        btnNext.setEnabled(vizCur < vizSteps.size() - 1);
    }

    private void togglePlay() {
        if (playTimer != null && playTimer.isRunning()) { stopPlay(); return; }
        if (!vizSteps.isEmpty() && vizCur >= vizSteps.size() - 1) showStep(0);
        int[] delays = {700, 480, 300, 160, 70};
        int delay = delays[Math.max(0, Math.min(sldSpeed.getValue() - 1, 4))];
        playTimer = new javax.swing.Timer(delay, e -> {
            if (vizCur >= vizSteps.size() - 1) { stopPlay(); return; }
            showStep(vizCur + 1);
        });
        playTimer.start();
        btnPlay.setText("⏸  Pause");
        btnPlay.setBackground(new Color(100, 55, 10));
    }

    private void stopPlay() {
        if (playTimer != null) { playTimer.stop(); playTimer = null; }
        btnPlay.setText("▶  Play");
        btnPlay.setBackground(new Color(18, 55, 90));
    }

    private void setVizEnabled(boolean on) {
        btnPrev.setEnabled(false);
        btnNext.setEnabled(on);
        btnPlay.setEnabled(on);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Business Logic
    // ─────────────────────────────────────────────────────────────────

    private void addRequest() {
        try {
            String name = tfName.getText().trim();
            if (name.isEmpty()) { showError("Caller name required."); return; }
            int time = Integer.parseInt(tfTime.getText().trim());
            if (time <= 0) { showError("Response time must be > 0."); return; }
            EmergencyRequest r = new EmergencyRequest(nextId++, name,
                    (ServiceType) cbType.getSelectedItem(),
                    (Priority)    cbPriority.getSelectedItem(),
                    time, tfLocation.getText().trim());
            requests.add(r);
            dispatchOrder.clear();
            tableModel.addRow(new Object[]{r.id, r.callerName, r.type, r.priority,
                    r.responseTime, r.location, ""});
            vizPanel.setState(requests, Collections.emptySet());
            log("+ Added #" + r.id + ": " + r.callerName + " — " + r.priority
                    + " " + r.type + " " + r.responseTime + " min");
            setStatus("Request #" + r.id + " added. Total: " + requests.size());
        } catch (NumberFormatException ex) {
            showError("Response time must be a number.");
        }
    }

    private void runSort() {
        if (requests.isEmpty()) { showError("No requests to sort."); return; }
        stopPlay();
        int idx = cbAlgorithm.getSelectedIndex();
        long t0 = System.nanoTime();

        List<EmergencyRequest> sorted;
        List<VizStep> steps;
        String algoName;

        switch (idx) {
            case 0: sorted = sortByPriorityQueue(requests); steps = buildStepsPQ(requests);
                    algoName = "Priority Queue (Max-Heap)"; break;
            case 1: sorted = mergeSort(requests);           steps = buildStepsMS(requests);
                    algoName = "Merge Sort"; break;
            default:sorted = quickSort(requests);           steps = buildStepsQS(requests);
                    algoName = "Quick Sort"; break;
        }

        long ns = System.nanoTime() - t0;
        dispatchOrder.clear();
        for (int i = 0; i < sorted.size(); i++) dispatchOrder.put(sorted.get(i).id, i + 1);

        tableModel.setRowCount(0);
        for (int i = 0; i < sorted.size(); i++) {
            EmergencyRequest r = sorted.get(i);
            tableModel.addRow(new Object[]{r.id, r.callerName, r.type, r.priority,
                    r.responseTime, r.location, "#" + (i + 1)});
        }

        vizSteps = steps;
        vizCur = 0;
        showStep(0);
        setVizEnabled(true);
        updateComplexity();

        log("─────────────────────────────────");
        log("▶ " + algoName + " | " + requests.size() + " requests | " + ns + " ns");
        for (int i = 0; i < sorted.size(); i++)
            log("  [" + (i + 1) + "] " + sorted.get(i));
        setStatus("✔  " + algoName + " — " + sorted.size() + " requests in " + ns + " ns");
    }

    private void clearAll() {
        stopPlay();
        requests.clear(); nextId = 1; dispatchOrder.clear();
        tableModel.setRowCount(0);
        vizSteps.clear(); vizCur = 0;
        vizPanel.setState(Collections.emptyList(), Collections.emptySet());
        lblStepInfo.setText("Add requests and run Sort & Dispatch.");
        lblStepCounter.setText("");
        setVizEnabled(false);
        log("Cleared.");
        setStatus("Cleared.");
    }

    private void loadSampleData() {
        Object[][] s = {
            {"Ravi Kumar",   ServiceType.AMBULANCE, Priority.CRITICAL, 3,  "Koramangala"},
            {"Priya Sharma", ServiceType.FIRE,      Priority.HIGH,     7,  "Indiranagar"},
            {"Arun Das",     ServiceType.POLICE,    Priority.MEDIUM,  12,  "Whitefield"},
            {"Sneha Menon",  ServiceType.AMBULANCE, Priority.LOW,     20,  "Hebbal"},
            {"Vikram Nair",  ServiceType.FIRE,      Priority.CRITICAL, 5,  "Electronic City"},
            {"Deepa Rao",    ServiceType.POLICE,    Priority.HIGH,     9,  "Jayanagar"},
            {"Kiran Pillai", ServiceType.AMBULANCE, Priority.MEDIUM,  15,  "Yelahanka"},
            {"Meena Iyer",   ServiceType.FIRE,      Priority.LOW,     25,  "Rajajinagar"},
        };
        for (Object[] row : s) {
            EmergencyRequest r = new EmergencyRequest(nextId++, (String)row[0],
                    (ServiceType)row[1], (Priority)row[2], (int)row[3], (String)row[4]);
            requests.add(r);
            tableModel.addRow(new Object[]{r.id, r.callerName, r.type, r.priority,
                    r.responseTime, r.location, ""});
        }
        vizPanel.setState(requests, Collections.emptySet());
        log("* Sample data loaded: " + requests.size() + " requests ready.");
        setStatus("Sample data loaded. Select an algorithm and click Sort & Dispatch.");
    }

    private void updateComplexity() {
        lblComplexity.setText("<html>" + complexityHtml(cbAlgorithm.getSelectedIndex()) + "</html>");
    }

    private String complexityHtml(int i) {
        String[][] d = {
            {"Priority Queue (Max-Heap)", "O(n log n)", "O(n log n)", "O(n)",
             "Greedy — extract-max gives optimal dispatch at each step."},
            {"Merge Sort", "O(n log n)", "O(n log n)", "O(n)",
             "Divide &amp; Conquer — stable, guaranteed O(n log n) always."},
            {"Quick Sort", "O(n log n)", "O(n\u00B2)", "O(log n)",
             "Divide &amp; Conquer — random pivot mitigates worst case."}
        };
        return "<b style='color:#e24b4a'>" + d[i][0] + "</b><br><br>"
             + "<span style='color:#78899a'>Best  : </span><b>" + d[i][1] + "</b><br>"
             + "<span style='color:#78899a'>Worst : </span><b>" + d[i][2] + "</b><br>"
             + "<span style='color:#78899a'>Space : </span><b>" + d[i][3] + "</b><br><br>"
             + "<span style='color:#506070'>" + d[i][4] + "</span>";
    }

    private void log(String msg) {
        taLog.append(msg + "\n");
        taLog.setCaretPosition(taLog.getDocument().getLength());
    }
    private void setStatus(String msg) { lblStatus.setText("  " + msg); }
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ─────────────────────────────────────────────────────────────────
    //  UI Helpers
    // ─────────────────────────────────────────────────────────────────

    private JPanel card(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR),
                new EmptyBorder(10, 12, 12, 12)));
        p.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Consolas", Font.BOLD, 9));
        lbl.setForeground(ACCENT_RED);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lbl); p.add(vgap(8));
        return p;
    }

    private JTextField field(String hint) {
        JTextField f = new JTextField(hint);
        f.setBackground(BG_INPUT); f.setForeground(TEXT_MAIN);
        f.setCaretColor(TEXT_MAIN);
        f.setFont(new Font("Consolas", Font.PLAIN, 12));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR),
                new EmptyBorder(2, 6, 2, 6)));
        return f;
    }

    private <T> JComboBox<T> combo(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setBackground(BG_INPUT); cb.setForeground(TEXT_MAIN);
        cb.setFont(new Font("Consolas", Font.PLAIN, 12));
        return cb;
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(new Font("Consolas", Font.BOLD, 11));
        b.setBorder(new EmptyBorder(6, 12, 6, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
        });
        return b;
    }

    private JButton ctrlBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(18, 55, 90));
        b.setForeground(TEXT_MAIN);
        b.setFont(new Font("Consolas", Font.BOLD, 10));
        b.setBorder(new EmptyBorder(4, 10, 4, 10));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private Component vgap(int h) { return Box.createVerticalStrut(h); }

    // ─────────────────────────────────────────────────────────────────
    //  Main Execution Entry
    // ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            // Set system look and feel for cleaner cross-platform rendering
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Graceful fallback to default java theme components if error occurs
        }
        
        // Launch Swing Application safely on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            EmergencyDispatch frame = new EmergencyDispatch();
            frame.setVisible(true);
        });
    }
}