package dev.springrad.tui.app;

import java.util.concurrent.atomic.AtomicReference;

public final class AppStore {
    private final AtomicReference<AppState> stateRef;

    public AppStore(AppState initialState) {
        this.stateRef = new AtomicReference<>(initialState);
    }

    public AppState state() {
        return stateRef.get();
    }

    public void dispatch(AppAction action) {
        stateRef.updateAndGet(current -> current.reduce(action));
    }
}
