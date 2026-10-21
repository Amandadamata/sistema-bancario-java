package com.github.amandadamata.sistemabancario;

/** Os dados não puderam ser lidos ou gravados, ou estão inconsistentes. */
public class FalhaPersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FalhaPersistenciaException(String mensagem) {
        super(mensagem);
    }

    public FalhaPersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
