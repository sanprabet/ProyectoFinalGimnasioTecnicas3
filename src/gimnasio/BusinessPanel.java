package gimnasio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import gimnasio.BusinessStore.Expense;
import gimnasio.BusinessStore.Item;
import gimnasio.BusinessStore.Kind;
import gimnasio.BusinessStore.Payment;
import gimnasio.BusinessStore.Sale;

@SuppressWarnings("serial")
final class BusinessPanel extends JPanel {
    private static final Locale LOCALE = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE);
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(LOCALE);
    private final BusinessStore store;
    private final DefaultTableModel catalog = GymFrame.readOnlyModel("Código", "Artículo", "Tipo", "Precio", "Existencias");
    private final DefaultTableModel sales = GymFrame.readOnlyModel("Fecha y hora", "Código", "Artículo",
            "Cantidad", "Precio unitario", "Total", "Medio de pago");
    private final DefaultTableModel expenses = GymFrame.readOnlyModel("Fecha", "Concepto", "Monto");
    private final JTable items = GymFrame.table(catalog, "Catálogo e inventario");
    private final JButton edit = new JButton("Editar artículo");
    private final JButton sell = new JButton("Registrar venta");
    private final JLabel monthlySales = GymFrame.label("", 24, Font.BOLD, Color.BLACK);
    private final JLabel monthlyExpenses = GymFrame.label("", 24, Font.BOLD, Color.BLACK);
    private final JLabel monthlyBalance = GymFrame.label("", 24, Font.BOLD, Color.BLACK);
    private final JLabel period = new JLabel();

    BusinessPanel(BusinessStore store) {
        super(new BorderLayout(0, 12));
        this.store = store;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 0));
        cards.setOpaque(false);
        cards.add(GymFrame.metric("Ventas del mes", monthlySales));
        cards.add(GymFrame.metric("Gastos del mes", monthlyExpenses));
        cards.add(GymFrame.metric("Saldo del mes", monthlyBalance));
        JPanel summary = new JPanel(new BorderLayout(0, 6));
        summary.setOpaque(false);
        summary.add(period, BorderLayout.NORTH);
        summary.add(cards, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        edit.addActionListener(event -> {
            int row = items.getSelectedRow();
            if (row >= 0) {
                editItem(store.items().get(row));
            }
        });
        items.getSelectionModel().addListSelectionListener(event -> edit.setEnabled(items.getSelectedRow() >= 0));
        sell.addActionListener(event -> registerSale());
        items.getColumnModel().getColumn(1).setPreferredWidth(230);
        tabs.addTab("Catálogo e inventario", tablePanel(items, edit, button("Agregar artículo", () -> editItem(null))));
        tabs.addTab("Ventas", tablePanel(GymFrame.table(sales, "Ventas"), sell));
        tabs.addTab("Gastos", tablePanel(GymFrame.table(expenses, "Gastos"), button("Registrar gasto", this::registerExpense)));
        add(summary, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        refresh();
    }

    private static JPanel tablePanel(JTable table, JButton... buttons) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.setOpaque(false);
        for (JButton button : buttons) {
            actions.add(button);
        }
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(GymFrame.tablePane(table), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private static JButton button(String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void editItem(Item existing) {
        JTextField code = new JTextField(existing == null ? "" : existing.code(), 16);
        JTextField name = new JTextField(existing == null ? "" : existing.name(), 24);
        JComboBox<Kind> kind = new JComboBox<>(Kind.values());
        JTextField price = new JTextField(existing == null ? "" : existing.price().toPlainString(), 12);
        JTextField stock = new JTextField(existing == null ? "0" : Integer.toString(existing.stock()), 12);
        if (existing != null) {
            code.setEditable(false);
            kind.setSelectedItem(existing.kind());
        }
        stock.setEnabled(kind.getSelectedItem() == Kind.PRODUCT);
        kind.addActionListener(event -> stock.setEnabled(kind.getSelectedItem() == Kind.PRODUCT));

        JPanel form = form();
        addField(form, "Código", code);
        addField(form, "Nombre", name);
        addField(form, "Tipo", kind);
        addField(form, "Precio (COP)", price);
        addField(form, "Existencias", stock);
        showForm(existing == null ? "Agregar artículo" : "Editar artículo", form, () -> {
            Kind selected = (Kind) kind.getSelectedItem();
            int units = selected == Kind.PRODUCT ? wholeNumber(stock.getText()) : 0;
            store.saveItem(new Item(code.getText(), name.getText(), selected, amount(price.getText()), units),
                    existing != null);
        });
    }

    private void registerSale() {
        JComboBox<Item> item = new JComboBox<>(store.items().toArray(Item[]::new));
        JTextField quantity = new JTextField("1", 10);
        JComboBox<Payment> payment = new JComboBox<>(Payment.values());
        JLabel details = new JLabel();
        Runnable updateDetails = () -> {
            Item selected = (Item) item.getSelectedItem();
            String text = currency.format(selected.price()) + " por unidad";
            if (selected.kind() == Kind.PRODUCT) {
                text += " · " + selected.stock() + " disponibles";
            }
            details.setText(text);
        };
        item.addActionListener(event -> updateDetails.run());
        updateDetails.run();

        JPanel form = form();
        addField(form, "Artículo", item);
        addField(form, "Precio y existencia", details);
        addField(form, "Cantidad", quantity);
        addField(form, "Medio de pago", payment);
        showForm("Registrar venta", form, () -> {
            Item selected = (Item) item.getSelectedItem();
            store.registerSale(selected.code(), wholeNumber(quantity.getText()), (Payment) payment.getSelectedItem());
        });
    }

    private void registerExpense() {
        JTextField date = new JTextField(LocalDate.now().toString(), 12);
        JTextField description = new JTextField(24);
        JTextField value = new JTextField(12);
        JPanel form = form();
        addField(form, "Fecha (AAAA-MM-DD)", date);
        addField(form, "Concepto", description);
        addField(form, "Monto (COP)", value);
        showForm("Registrar gasto", form, () -> {
            LocalDate day = LocalDate.parse(date.getText().strip());
            store.registerExpense(new Expense(day, description.getText(), amount(value.getText())));
        });
    }

    private static JPanel form() {
        return new JPanel(new GridLayout(0, 2, 10, 8));
    }

    private static void addField(JPanel form, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        form.add(label);
        form.add(field);
    }

    private void showForm(String title, JPanel form, SaveAction save) {
        Object[] options = {"Guardar", "Cancelar"};
        while (JOptionPane.showOptionDialog(this, form, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]) == 0) {
            try {
                save.run();
                refresh();
                return;
            } catch (DateTimeParseException error) {
                showError("Escribe una fecha válida en formato AAAA-MM-DD.");
            } catch (IllegalArgumentException error) {
                showError(error.getMessage());
            } catch (IOException error) {
                error.printStackTrace(System.err);
                showError("No se pudo guardar el registro. Intenta de nuevo.");
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Registro pendiente", JOptionPane.WARNING_MESSAGE);
    }

    private static int wholeNumber(String text) {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Escribe una cantidad entera.");
        }
    }

    private static BigDecimal amount(String text) {
        String value = text.strip();
        if (!value.matches("[0-9]+([.,][0-9]{1,2})?")) {
            throw new IllegalArgumentException("Escribe un monto como 15000 o 15000,50, sin separador de miles.");
        }
        return new BigDecimal(value.replace(',', '.'));
    }

    private void refresh() {
        catalog.setRowCount(0);
        for (Item item : store.items()) {
            catalog.addRow(new Object[]{item.code(), item.name(), item.kind(), currency.format(item.price()),
                item.kind() == Kind.PRODUCT ? item.stock() : "—"});
        }
        edit.setEnabled(items.getSelectedRow() >= 0);
        sell.setEnabled(!store.items().isEmpty());

        sales.setRowCount(0);
        for (Sale sale : store.sales().reversed()) {
            sales.addRow(new Object[]{sale.date().format(TIMESTAMP), sale.code(), sale.name(), sale.quantity(),
                currency.format(sale.unitPrice()), currency.format(sale.total()), sale.payment()});
        }
        expenses.setRowCount(0);
        for (Expense expense : store.expenses().stream().sorted(Comparator.comparing(Expense::date).reversed()).toList()) {
            expenses.addRow(new Object[]{expense.date(), expense.description(), currency.format(expense.amount())});
        }
        refreshSummary();
    }

    void refreshSummary() {
        YearMonth current = YearMonth.now();
        BigDecimal sold = store.sales().stream()
                .filter(sale -> YearMonth.from(sale.date()).equals(current))
                .map(Sale::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal spent = store.expenses().stream()
                .filter(expense -> YearMonth.from(expense.date()).equals(current))
                .map(Expense::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        period.setText(current.format(MONTH));
        monthlySales.setText(currency.format(sold));
        monthlyExpenses.setText(currency.format(spent));
        monthlyBalance.setText(currency.format(sold.subtract(spent)));
    }

    @FunctionalInterface
    private interface SaveAction {
        void run() throws IOException;
    }
}
