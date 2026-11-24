package gimnasio;

import java.time.LocalDateTime;

record Visit(String memberName, String document, LocalDateTime enteredAt) {
}
