package gimnasio;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

final class GymStore implements AutoCloseable {
    private final Path file;
    private final FileChannel lockChannel;
    private List<Member> members = List.of();
    private List<Visit> visits = List.of();

    GymStore(Path directory) throws IOException {
        Files.createDirectories(directory);
        file = directory.resolve("gimnasio.properties");
        lockChannel = FileChannel.open(directory.resolve("gimnasio.lock"), CREATE, WRITE);
        try {
            if (lockChannel.tryLock() == null) {
                throw new IOException("Ya hay una instancia del gimnasio usando esta carpeta.");
            }
            load();
        } catch (IOException | RuntimeException error) {
            try {
                lockChannel.close();
            } catch (IOException closingError) {
                error.addSuppressed(closingError);
            }
            throw error;
        }
    }

    List<Member> members() {
        return members;
    }

    List<Visit> visits() {
        return visits;
    }

    void saveMember(Member member) throws IOException {
        for (Member other : members) {
            if (!other.id().equals(member.id())
                    && other.document().equalsIgnoreCase(member.document())) {
                throw new IllegalArgumentException("Ya existe un socio con ese documento.");
            }
        }

        List<Member> updated = new ArrayList<>(members);
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).id().equals(member.id())) {
                updated.set(index, member);
                save(updated, visits);
                return;
            }
        }
        updated.add(member);
        save(updated, visits);
    }

    void deleteMember(Member member) throws IOException {
        List<Member> updated = new ArrayList<>(members);
        updated.remove(member);
        save(updated, visits);
    }

    void registerVisit(Member member) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        if (!members.contains(member) || !member.isActiveOn(now.toLocalDate())) {
            throw new IllegalArgumentException("El socio necesita una membresía vigente para ingresar.");
        }
        List<Visit> updated = new ArrayList<>(visits);
        updated.add(new Visit(member.name(), member.document(), now));
        save(members, updated);
    }

    private void load() throws IOException {
        if (Files.notExists(file)) {
            return;
        }
        try (var reader = Files.newBufferedReader(file)) {
            Properties data = new Properties();
            data.load(reader);
            List<Member> loadedMembers = new ArrayList<>();
            int memberCount = count(data, "members");
            for (int index = 0; index < memberCount; index++) {
                String key = "member." + index + ".";
                loadedMembers.add(new Member(
                        UUID.fromString(data.getProperty(key + "id")),
                        data.getProperty(key + "name"),
                        data.getProperty(key + "document"),
                        Member.Plan.valueOf(data.getProperty(key + "plan")),
                        LocalDate.parse(data.getProperty(key + "expires"))));
            }
            List<Visit> loadedVisits = new ArrayList<>();
            int visitCount = count(data, "visits");
            for (int index = 0; index < visitCount; index++) {
                String key = "visit." + index + ".";
                loadedVisits.add(new Visit(
                        required(data, key + "name"),
                        required(data, key + "document"),
                        LocalDateTime.parse(data.getProperty(key + "timestamp"))));
            }
            members = List.copyOf(loadedMembers);
            visits = List.copyOf(loadedVisits);
        } catch (RuntimeException error) {
            throw new IOException("El archivo de datos contiene registros inválidos: " + file, error);
        }
    }

    private static String required(Properties data, String key) {
        String value = data.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Falta el campo " + key);
        }
        return value;
    }

    private static int count(Properties data, String key) {
        int total = Integer.parseInt(required(data, key));
        if (total < 0) {
            throw new IllegalArgumentException("Cantidad inválida en " + key);
        }
        return total;
    }

    private void save(List<Member> updatedMembers, List<Visit> updatedVisits) throws IOException {
        Properties data = new Properties();
        data.setProperty("members", Integer.toString(updatedMembers.size()));
        for (int index = 0; index < updatedMembers.size(); index++) {
            Member member = updatedMembers.get(index);
            String key = "member." + index + ".";
            data.setProperty(key + "id", member.id().toString());
            data.setProperty(key + "name", member.name());
            data.setProperty(key + "document", member.document());
            data.setProperty(key + "plan", member.plan().name());
            data.setProperty(key + "expires", member.expiresOn().toString());
        }
        data.setProperty("visits", Integer.toString(updatedVisits.size()));
        for (int index = 0; index < updatedVisits.size(); index++) {
            Visit visit = updatedVisits.get(index);
            String key = "visit." + index + ".";
            data.setProperty(key + "name", visit.memberName());
            data.setProperty(key + "document", visit.document());
            data.setProperty(key + "timestamp", visit.enteredAt().toString());
        }

        Path temporary = Files.createTempFile(file.getParent(), "gimnasio-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary)) {
                data.store(writer, "Gestión de gimnasio");
            }
            Files.move(temporary, file, ATOMIC_MOVE, REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        members = List.copyOf(updatedMembers);
        visits = List.copyOf(updatedVisits);
    }

    @Override
    public void close() throws IOException {
        lockChannel.close();
    }
}
