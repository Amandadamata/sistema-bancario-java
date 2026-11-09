package com.github.amandadamata.bankingsystem.ui;

import javax.swing.*;
import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** One operation at a time. SQLite runs off the EDT, and components only change on it. */
final class UiTasks {
    private final Container root;
    private final Consumer<String> error;
    private boolean busy;

    UiTasks(Container root, Consumer<String> error) { this.root = root; this.error = error; }
    boolean busy() { return busy; }

    <T> void runMenu(Callable<T> work, Consumer<T> success) {
        if (busy) return;
        busy = true;
        Map<Component, Boolean> states = new IdentityHashMap<>();
        disableTree(root, states);
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception { return work.call(); }
            protected void done() {
                states.forEach(Component::setEnabled);
                busy = false;
                try {
                    success.accept(get());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    error.accept("Operation interrupted. Close and reopen the app to check the result.");
                } catch (ExecutionException failure) {
                    Throwable cause = failure.getCause();
                    if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                            || cause instanceof com.github.amandadamata.bankingsystem.InsufficientFundsException
                            || cause instanceof com.github.amandadamata.bankingsystem.PersistenceFailureException) {
                        error.accept(cause.getMessage());
                    } else {
                        error.accept("Could not finish. Reopen the app and check the statement before trying again.");
                    }
                }
            }
        }.execute();
    }

    private static void disableTree(Container container, Map<Component, Boolean> states) {
        for (Component component : container.getComponents()) {
            states.put(component, component.isEnabled());
            component.setEnabled(false);
            if (component instanceof Container child) disableTree(child, states);
        }
    }
}
