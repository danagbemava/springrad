package dev.springrad.tui.app;

public record AppAction(Type type, AppRoute route, String message) {
    public enum Type {
        navigate,
        set_status,
        append_activity
    }

    public static AppAction navigate(AppRoute route) {
        return new AppAction(Type.navigate, route, null);
    }

    public static AppAction setStatus(String message) {
        return new AppAction(Type.set_status, null, message);
    }

    public static AppAction appendActivity(String message) {
        return new AppAction(Type.append_activity, null, message);
    }
}
