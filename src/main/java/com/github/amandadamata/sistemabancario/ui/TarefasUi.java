package com.github.amandadamata.sistemabancario.ui;

import javax.swing.*;
import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Uma operação por vez. SQLite fica fora da EDT, e componentes só mudam dentro dela. */
final class TarefasUi {
    private final Container raiz;
    private final Consumer<String> erro;
    private boolean ocupada;

    TarefasUi(Container raiz, Consumer<String> erro) { this.raiz = raiz; this.erro = erro; }
    boolean ocupada() { return ocupada; }

    <T> void executar(Callable<T> trabalho, Consumer<T> sucesso) {
        if (ocupada) return;
        ocupada = true;
        Map<Component, Boolean> estados = new IdentityHashMap<>();
        bloquear(raiz, estados);
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception { return trabalho.call(); }
            protected void done() {
                estados.forEach(Component::setEnabled);
                ocupada = false;
                try {
                    sucesso.accept(get());
                } catch (InterruptedException interrompida) {
                    Thread.currentThread().interrupt();
                    erro.accept("Operation interrupted. Close and reopen the app to check the result.");
                } catch (ExecutionException falha) {
                    Throwable causa = falha.getCause();
                    if (causa instanceof IllegalArgumentException || causa instanceof IllegalStateException
                            || causa instanceof com.github.amandadamata.sistemabancario.SaldoInsuficienteException
                            || causa instanceof com.github.amandadamata.sistemabancario.FalhaPersistenciaException) {
                        erro.accept(causa.getMessage());
                    } else {
                        erro.accept("Could not finish. Reopen the app and check the statement before trying again.");
                    }
                }
            }
        }.execute();
    }

    private static void bloquear(Container container, Map<Component, Boolean> estados) {
        for (Component componente : container.getComponents()) {
            estados.put(componente, componente.isEnabled());
            componente.setEnabled(false);
            if (componente instanceof Container filho) bloquear(filho, estados);
        }
    }
}
