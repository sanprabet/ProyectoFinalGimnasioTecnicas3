package gimnasio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

@SuppressWarnings("serial")
final class GymFrame extends JFrame {
    private static final Color NAVY = new Color(20, 32, 53);
    private static final Color INK = new Color(26, 39, 59);
    private static final Color MUTED = new Color(72, 87, 106);
    private static final Color SURFACE = new Color(240, 244, 248);
    private static final Color BORDER = new Color(214, 222, 230);
    private static final Color GREEN = new Color(0, 109, 84);
    private static final Color RED = new Color(157, 38, 57);
    private static final DateTimeFormatter HEADER_DATE = DateTimeFormatter
            .ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter VISIT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GymStore store;
    private final BusinessPanel business;
    private final DefaultTableModel memberModel =
            readOnlyModel("Nombre", "Documento", "Plan", "Vencimiento", "Estado");
    private final DefaultTableModel visitModel = readOnlyModel("Fecha y hora", "Nombre", "Documento");
    private final JTable memberTable = table(memberModel, "Socios");
    private final JTable visitTable = table(visitModel, "Historial de ingresos");
    private final TableRowSorter<DefaultTableModel> memberSorter = new TableRowSorter<>(memberModel);
    private final JTextField search = new JTextField(21);
    private final JButton add = new JButton("Agregar socio");
    private final JButton edit = new JButton("Editar");
    private final JButton delete = new JButton("Eliminar");
    private final JButton checkIn = new JButton("Registrar ingreso");
    private final JLabel date = label("", 15, Font.PLAIN, new Color(203, 217, 232));
    private final JLabel memberCount = label("", 29, Font.BOLD, INK);
    private final JLabel activeCount = label("", 29, Font.BOLD, GREEN);
    private final JLabel todayCount = label("", 29, Font.BOLD, INK);
    private final JLabel feedback = label("Agrega un socio para comenzar.", 13, Font.PLAIN, MUTED);
    private LocalDate displayedDate;

    GymFrame(GymStore store) {
        super("Gestión de negocio");
        this.store = store;
        business = new BusinessPanel(store.business());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(940, 580));
        setSize(1100, 690);
        setLocationRelativeTo(null);
        setContentPane(layoutContent());
        connectActions();
        refresh(null);
        if (!store.members().isEmpty()) {
            feedback.setText("Selecciona un socio para gestionar su membresía o registrar un ingreso.");
        }
        new Timer(30_000, event -> {
            if (!LocalDate.now().equals(displayedDate)) {
                refresh(selectedMember());
                business.refreshSummary();
            }
        }).start();
    }

    private JPanel layoutContent() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));
        header.add(label("Gestión de negocio", 27, Font.BOLD, Color.WHITE), BorderLayout.WEST);
        header.add(date, BorderLayout.EAST);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0));
        metrics.setOpaque(false);
        metrics.add(metric("Socios registrados", memberCount));
        metrics.add(metric("Membresías vigentes", activeCount));
        metrics.add(metric("Ingresos hoy", todayCount));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Socios", membersPanel());
        JPanel history = new JPanel(new BorderLayout());
        history.setBackground(Color.WHITE);
        history.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        history.add(tablePane(visitTable));
        tabs.addTab("Historial de ingresos", history);

        JPanel gym = new JPanel(new BorderLayout(0, 16));
        gym.setOpaque(false);
        gym.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        gym.add(metrics, BorderLayout.NORTH);
        gym.add(tabs, BorderLayout.CENTER);
        gym.add(feedback, BorderLayout.SOUTH);

        JTabbedPane sections = new JTabbedPane();
        sections.addTab("Gimnasio", gym);
        sections.addTab("Negocio", business);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(SURFACE);
        body.setBorder(BorderFactory.createEmptyBorder(18, 24, 14, 24));
        body.add(sections, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private JPanel membersPanel() {
        JLabel searchLabel = label("Nombre o documento", 13, Font.PLAIN, INK);
        searchLabel.setLabelFor(search);
        JPanel filter = new JPanel(new BorderLayout(7, 0));
        filter.setOpaque(false);
        filter.add(searchLabel, BorderLayout.WEST);
        filter.add(search, BorderLayout.CENTER);

        JPanel primary = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        primary.setOpaque(false);
        add.setFont(add.getFont().deriveFont(Font.BOLD));
        primary.add(add);
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.add(filter, BorderLayout.WEST);
        toolbar.add(primary, BorderLayout.EAST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.setOpaque(false);
        actions.add(edit);
        actions.add(delete);
        checkIn.setFont(checkIn.getFont().deriveFont(Font.BOLD));
        actions.add(checkIn);

        memberTable.setRowSorter(memberSorter);
        memberTable.getColumnModel().getColumn(0).setPreferredWidth(260);
        memberTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                if (!selected) {
                    cell.setForeground("Vigente".equals(value) ? GREEN : RED);
                }
                return cell;
            }
        });

        JPanel panel = new JPanel(new BorderLayout(0, 11));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(tablePane(memberTable), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    static JPanel metric(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(11, 16, 11, 16)));
        card.add(label(title, 13, Font.PLAIN, MUTED), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(style, (float) size));
        label.setForeground(color);
        return label;
    }

    static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static JTable table(DefaultTableModel model, String name) {
        JTable table = new JTable(model);
        table.getAccessibleContext().setAccessibleName(name);
        table.setRowHeight(31);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    static JScrollPane tablePane(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createLineBorder(BORDER));
        return pane;
    }

    private void connectActions() {
        add.addActionListener(event -> openForm(null));
        edit.addActionListener(event -> openForm(selectedMember()));
        delete.addActionListener(event -> deleteSelectedMember());
        checkIn.addActionListener(event -> registerVisit());
        memberTable.getSelectionModel().addListSelectionListener(event -> updateActions());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                filterMembers();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                filterMembers();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                filterMembers();
            }
        });
        visitTable.setAutoCreateRowSorter(true);
    }

    private void filterMembers() {
        String query = search.getText().strip();
        memberSorter.setRowFilter(query.isEmpty() ? null
                : RowFilter.regexFilter("(?iu)" + Pattern.quote(query), 0, 1));
        updateActions();
    }

    private Member selectedMember() {
        int row = memberTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int index = memberTable.convertRowIndexToModel(row);
        return index < store.members().size() ? store.members().get(index) : null;
    }

    private void updateActions() {
        Member member = selectedMember();
        edit.setEnabled(member != null);
        delete.setEnabled(member != null);
        checkIn.setEnabled(member != null && member.isActiveOn(LocalDate.now()));
    }

    private void openForm(Member member) {
        if (new MemberDialog(this, store, member).showForm()) {
            refresh(member);
            feedback.setText(member == null ? "Socio agregado." : "Datos del socio actualizados.");
        }
    }

    private void deleteSelectedMember() {
        Member member = selectedMember();
        if (member == null || JOptionPane.showConfirmDialog(this,
                "¿Eliminar a " + member.name() + "? Su historial de ingresos se conservará.",
                "Eliminar socio", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            store.deleteMember(member);
            refresh(null);
            feedback.setText("Socio eliminado.");
        } catch (IOException error) {
            showSaveError(error);
        }
    }

    private void registerVisit() {
        Member member = selectedMember();
        if (member == null) {
            return;
        }
        try {
            store.registerVisit(member);
            refresh(member);
            feedback.setText("Ingreso registrado para " + member.name() + ".");
        } catch (IllegalArgumentException validation) {
            refresh(member);
            feedback.setText(validation.getMessage());
        } catch (IOException error) {
            showSaveError(error);
        }
    }

    private void showSaveError(IOException error) {
        error.printStackTrace(System.err);
        JOptionPane.showMessageDialog(this, "No se pudo guardar el cambio. Intenta de nuevo.",
                "Error al guardar", JOptionPane.ERROR_MESSAGE);
    }

    private void refresh(Member previousSelection) {
        displayedDate = LocalDate.now();
        date.setText(displayedDate.format(HEADER_DATE));
        memberTable.clearSelection();
        memberModel.setRowCount(0);
        long active = 0;
        int selectedIndex = -1;
        for (Member member : store.members()) {
            boolean valid = member.isActiveOn(displayedDate);
            if (valid) {
                active++;
            }
            if (previousSelection != null && previousSelection.id().equals(member.id())) {
                selectedIndex = memberModel.getRowCount();
            }
            memberModel.addRow(new Object[] {
                member.name(), member.document(), member.plan(), member.expiresOn(), valid ? "Vigente" : "Vencida"
            });
        }
        if (selectedIndex >= 0) {
            int visibleRow = memberTable.convertRowIndexToView(selectedIndex);
            if (visibleRow >= 0) {
                memberTable.setRowSelectionInterval(visibleRow, visibleRow);
            }
        }

        visitModel.setRowCount(0);
        long today = 0;
        List<Visit> visits = store.visits();
        for (Visit visit : visits.reversed()) {
            if (visit.enteredAt().toLocalDate().equals(displayedDate)) {
                today++;
            }
            visitModel.addRow(new Object[] {
                visit.enteredAt().format(VISIT_DATE), visit.memberName(), visit.document()
            });
        }
        memberCount.setText(Integer.toString(store.members().size()));
        activeCount.setText(Long.toString(active));
        todayCount.setText(Long.toString(today));
        updateActions();
    }
}
