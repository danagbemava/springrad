package dev.springrad.tui.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AppState {
    private final AppRoute route;
    private final String status;
    private final List<String> activity;

    public AppState(AppRoute route, String status, List<String> activity) {
        this.route = route;
        this.status = status == null ? "" : status;
        this.activity = List.copyOf(activity == null ? List.of() : activity);
    }

    public static AppState initial(AppRoute route) {
        return new AppState(route, "", List.of());
    }

    public AppRoute route() {
        return route;
    }

    public String status() {
        return status;
    }

    public List<String> activity() {
        return activity;
    }

    public AppState reduce(AppAction action) {
        if (action == null) {
            return this;
        }
        return switch (action.type()) {
            case navigate -> new AppState(action.route() == null ? route : action.route(), status, activity);
            case set_status -> new AppState(route, action.message(), activity);
            case append_activity -> {
                List<String> next = new ArrayList<>(activity);
                if (action.message() != null && !action.message().isBlank()) {
                    next.add(action.message());
                }
                yield new AppState(route, status, Collections.unmodifiableList(next));
            }
        };
    }
}
