package gimnasio;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static gimnasio.PropertiesFile.count;
import static gimnasio.PropertiesFile.required;

final class BusinessStore {
    private final Path file;
    private List<Item> items = List.of();
    private List<Sale> sales = List.of();
    private List<Expense> expenses = List.of();

    BusinessStore(Path file) throws IOException {
        this.file = file;
        if (Files.notExists(file)) {
            return;
        }
        try {
            Properties data = PropertiesFile.read(file);
            List<Item> loadedItems = new ArrayList<>();
            int itemCount = count(data, "items");
            for (int index = 0; index < itemCount; index++) {
                String key = "item." + index + ".";
                loadedItems.add(new Item(required(data, key + "code"), required(data, key + "name"),
                        Kind.valueOf(required(data, key + "kind")),
                        new BigDecimal(required(data, key + "price")),
                        Integer.parseInt(required(data, key + "stock"))));
            }
            List<Sale> loadedSales = new ArrayList<>();
            int saleCount = count(data, "sales");
            for (int index = 0; index < saleCount; index++) {
                String key = "sale." + index + ".";
                loadedSales.add(new Sale(LocalDateTime.parse(required(data, key + "date")),
                        required(data, key + "code"), required(data, key + "name"),
                        Integer.parseInt(required(data, key + "quantity")),
                        new BigDecimal(required(data, key + "price")),
                        Payment.valueOf(required(data, key + "payment"))));
            }
            List<Expense> loadedExpenses = new ArrayList<>();
            int expenseCount = count(data, "expenses");
            for (int index = 0; index < expenseCount; index++) {
                String key = "expense." + index + ".";
                loadedExpenses.add(new Expense(LocalDate.parse(required(data, key + "date")),
                        required(data, key + "description"), new BigDecimal(required(data, key + "amount"))));
            }
            items = List.copyOf(loadedItems);
            sales = List.copyOf(loadedSales);
            expenses = List.copyOf(loadedExpenses);
        } catch (RuntimeException error) {
            throw new IOException("El archivo de negocio contiene registros inválidos: " + file, error);
        }
    }

    List<Item> items() {
        return items;
    }

    List<Sale> sales() {
        return sales;
    }

    List<Expense> expenses() {
        return expenses;
    }

    void saveItem(Item item, boolean editing) throws IOException {
        int index = findItem(item.code());
        if (!editing && index >= 0) {
            throw new IllegalArgumentException("Ya existe un artículo con ese código.");
        }
        if (editing && index < 0) {
            throw new IllegalArgumentException("El artículo ya no está en el catálogo.");
        }
        List<Item> updated = new ArrayList<>(items);
        if (editing) {
            updated.set(index, item);
        } else {
            updated.add(item);
        }
        save(updated, sales, expenses);
    }

    void registerSale(String code, int quantity, Payment payment) throws IOException {
        int index = findItem(code);
        if (index < 0) {
            throw new IllegalArgumentException("Selecciona un artículo del catálogo.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        Item item = items.get(index);
        List<Item> updatedItems = new ArrayList<>(items);
        if (item.kind() == Kind.PRODUCT) {
            if (quantity > item.stock()) {
                throw new IllegalArgumentException("Existencias insuficientes. Unidades disponibles: " + item.stock());
            }
            updatedItems.set(index, new Item(item.code(), item.name(), item.kind(), item.price(),
                    item.stock() - quantity));
        }
        List<Sale> updatedSales = new ArrayList<>(sales);
        updatedSales.add(new Sale(LocalDateTime.now(), code, item.name(), quantity, item.price(), payment));
        save(updatedItems, updatedSales, expenses);
    }

    void registerExpense(Expense expense) throws IOException {
        List<Expense> updated = new ArrayList<>(expenses);
        updated.add(expense);
        save(items, sales, updated);
    }

    private int findItem(String code) {
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).code().equals(code)) {
                return index;
            }
        }
        return -1;
    }

    private void save(List<Item> updatedItems, List<Sale> updatedSales, List<Expense> updatedExpenses)
            throws IOException {
        Properties data = new Properties();
        data.setProperty("items", Integer.toString(updatedItems.size()));
        for (int index = 0; index < updatedItems.size(); index++) {
            Item item = updatedItems.get(index);
            String key = "item." + index + ".";
            data.setProperty(key + "code", item.code());
            data.setProperty(key + "name", item.name());
            data.setProperty(key + "kind", item.kind().name());
            data.setProperty(key + "price", item.price().toPlainString());
            data.setProperty(key + "stock", Integer.toString(item.stock()));
        }
        data.setProperty("sales", Integer.toString(updatedSales.size()));
        for (int index = 0; index < updatedSales.size(); index++) {
            Sale sale = updatedSales.get(index);
            String key = "sale." + index + ".";
            data.setProperty(key + "date", sale.date().toString());
            data.setProperty(key + "code", sale.code());
            data.setProperty(key + "name", sale.name());
            data.setProperty(key + "quantity", Integer.toString(sale.quantity()));
            data.setProperty(key + "price", sale.unitPrice().toPlainString());
            data.setProperty(key + "payment", sale.payment().name());
        }
        data.setProperty("expenses", Integer.toString(updatedExpenses.size()));
        for (int index = 0; index < updatedExpenses.size(); index++) {
            Expense expense = updatedExpenses.get(index);
            String key = "expense." + index + ".";
            data.setProperty(key + "date", expense.date().toString());
            data.setProperty(key + "description", expense.description());
            data.setProperty(key + "amount", expense.amount().toPlainString());
        }
        PropertiesFile.write(file, data);
        items = List.copyOf(updatedItems);
        sales = List.copyOf(updatedSales);
        expenses = List.copyOf(updatedExpenses);
    }

    private static void requireAmount(BigDecimal value) {
        if (value.signum() <= 0 || value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El monto debe ser positivo y tener hasta dos decimales.");
        }
    }

    record Item(String code, String name, Kind kind, BigDecimal price, int stock) {
        Item {
            code = code.strip().toUpperCase(Locale.ROOT);
            name = name.strip();
            if (code.isEmpty() || name.isEmpty()) {
                throw new IllegalArgumentException("Escribe el código y el nombre del artículo.");
            }
            requireAmount(price);
            if (stock < 0 || (kind == Kind.SERVICE && stock != 0)) {
                throw new IllegalArgumentException("Las existencias deben ser válidas para el tipo de artículo.");
            }
        }

        @Override
        public String toString() {
            return code + " · " + name;
        }
    }

    record Sale(LocalDateTime date, String code, String name, int quantity, BigDecimal unitPrice, Payment payment) {
        Sale {
            if (quantity <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
            }
            requireAmount(unitPrice);
        }

        BigDecimal total() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    record Expense(LocalDate date, String description, BigDecimal amount) {
        Expense {
            description = description.strip();
            if (description.isEmpty()) {
                throw new IllegalArgumentException("Escribe el concepto del gasto.");
            }
            requireAmount(amount);
        }
    }

    enum Kind {
        PRODUCT("Producto"), SERVICE("Servicio");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    enum Payment {
        CASH("Efectivo"), CARD("Tarjeta"), TRANSFER("Transferencia");

        private final String label;

        Payment(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
