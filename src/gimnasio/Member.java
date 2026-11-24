package gimnasio;

import java.time.LocalDate;
import java.util.UUID;

record Member(UUID id, String name, String document, Plan plan, LocalDate expiresOn) {
    Member {
        name = name.strip();
        document = document.strip();
        if (name.isEmpty() || document.isEmpty()) {
            throw new IllegalArgumentException("Escribe el nombre y el documento del socio.");
        }
    }

    boolean isActiveOn(LocalDate date) {
        return !expiresOn.isBefore(date);
    }

    enum Plan {
        MONTHLY("Mensual"),
        QUARTERLY("Trimestral"),
        ANNUAL("Anual");

        private final String label;

        Plan(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
