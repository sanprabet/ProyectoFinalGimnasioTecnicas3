package gimnasio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

final class PropertiesFile {
    private PropertiesFile() {
    }

    static Properties read(Path file) throws IOException {
        Properties data = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            data.load(reader);
        }
        return data;
    }

    static void write(Path file, Properties data) throws IOException {
        Path temporary = Files.createTempFile(file.getParent(), file.getFileName() + "-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary)) {
                data.store(writer, null);
            }
            Files.move(temporary, file, ATOMIC_MOVE, REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static String required(Properties data, String key) {
        String value = data.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Falta el campo " + key);
        }
        return value;
    }

    static int count(Properties data, String key) {
        int total = Integer.parseInt(required(data, key));
        if (total < 0) {
            throw new IllegalArgumentException("Cantidad inválida en " + key);
        }
        return total;
    }
}
