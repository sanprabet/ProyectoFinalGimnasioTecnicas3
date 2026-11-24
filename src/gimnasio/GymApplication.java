package gimnasio;

import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public final class GymApplication {
    private GymApplication() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GymApplication::open);
    }

    private static void open() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException error) {
            System.err.println(error.getMessage());
        }

        try {
            String directory = System.getenv().getOrDefault("GYM_DATA_DIR", "data");
            GymStore store = new GymStore(Path.of(directory).toAbsolutePath());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    store.close();
                } catch (IOException error) {
                    System.err.println(error.getMessage());
                }
            }));
            new GymFrame(store).setVisible(true);
        } catch (IOException | RuntimeException error) {
            error.printStackTrace(System.err);
            JOptionPane.showMessageDialog(null, error.getMessage(),
                    "No se pudo abrir el gimnasio", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
