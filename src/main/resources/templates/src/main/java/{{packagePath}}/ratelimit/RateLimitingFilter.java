package {{packageName}}.ratelimit;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final Supplier<Bucket> bucketSupplier;
    private final Map<String, Bucket> bucketsByClient = new ConcurrentHashMap<>();

    public RateLimitingFilter(Supplier<Bucket> bucketSupplier) {
        this.bucketSupplier = bucketSupplier;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String key = resolveClientKey(request);
        Bucket bucket = bucketsByClient.computeIfAbsent(key, ignored -> bucketSupplier.get());
        if (!bucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
