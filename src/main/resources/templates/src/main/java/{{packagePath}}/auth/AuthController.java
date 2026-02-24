package {{packageName}}.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User registered",
                "email", request.email()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String authMode = "{{authStyle}}";
        if ("jwt".equals(authMode)) {
            return ResponseEntity.ok(Map.of(
                    "tokenType", "Bearer",
                    "accessToken", "replace-with-generated-token",
                    "email", request.email()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "email", request.email()
        ));
    }

    public record RegisterRequest(String email, String password) {
    }

    public record LoginRequest(String email, String password) {
    }
}
