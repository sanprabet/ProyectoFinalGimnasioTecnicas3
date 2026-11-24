package gimnasio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
final class MemberDialog extends JDialog {
    private final JTextField name = new JTextField(24);
    private final JTextField document = new JTextField(24);
    private final JComboBox<Member.Plan> plan = new JComboBox<>(Member.Plan.values());
    private final JTextField expiration = new JTextField(24);
    private final JLabel error = new JLabel(" ");
    private final GymStore store;
    private final UUID memberId;
    private boolean saved;

    MemberDialog(JFrame owner, GymStore store, Member member) {
        super(owner, member == null ? "Agregar socio" : "Editar socio", true);
        this.store = store;
        memberId = member == null ? UUID.randomUUID() : member.id();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        if (member != null) {
            name.setText(member.name());
            document.setText(member.document());
            plan.setSelectedItem(member.plan());
            expiration.setText(member.expiresOn().toString());
        } else {
            setSuggestedExpiration();
        }
        plan.addActionListener(event -> {
            if (member == null) {
                setSuggestedExpiration();
            }
        });

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        addField(form, "Nombre completo", name);
        addField(form, "Documento", document);
        addField(form, "Plan", plan);
        addField(form, "Vencimiento (AAAA-MM-DD)", expiration);

        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(event -> dispose());
        JButton save = new JButton("Guardar");
        save.addActionListener(event -> save());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.add(cancel);
        actions.add(save);
        error.setForeground(new Color(159, 34, 53));
        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.add(error, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        content.add(form, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(save);
        getRootPane().registerKeyboardAction(event -> dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setMinimumSize(new java.awt.Dimension(440, getHeight()));
        setSize(getMinimumSize());
        setLocationRelativeTo(owner);
    }

    private static void addField(JPanel form, String label, javax.swing.JComponent field) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setLabelFor(field);
        form.add(fieldLabel);
        form.add(field);
    }

    private void setSuggestedExpiration() {
        LocalDate today = LocalDate.now();
        Member.Plan selectedPlan = (Member.Plan) plan.getSelectedItem();
        LocalDate date = switch (selectedPlan) {
            case MONTHLY -> today.plusMonths(1);
            case QUARTERLY -> today.plusMonths(3);
            case ANNUAL -> today.plusYears(1);
        };
        expiration.setText(date.toString());
    }

    private void save() {
        try {
            LocalDate expiresOn = LocalDate.parse(expiration.getText().strip());
            Member member = new Member(memberId, name.getText(), document.getText(),
                    (Member.Plan) plan.getSelectedItem(), expiresOn);
            store.saveMember(member);
            saved = true;
            dispose();
        } catch (DateTimeParseException invalidDate) {
            error.setText("Escribe una fecha válida en formato AAAA-MM-DD.");
            expiration.requestFocusInWindow();
        } catch (IllegalArgumentException validation) {
            error.setText(validation.getMessage());
        } catch (IOException failure) {
            failure.printStackTrace(System.err);
            error.setText("No se pudo guardar el socio. Intenta de nuevo.");
        }
    }

    boolean showForm() {
        setVisible(true);
        return saved;
    }
}
