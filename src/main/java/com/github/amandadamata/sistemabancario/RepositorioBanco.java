package com.github.amandadamata.sistemabancario;

import java.util.List;

/**
 * Contrato de armazenamento usado pelo {@link Banco}.
 *
 * <p>O domínio depende desta interface, e não de um banco de dados específico. A implementação
 * real usa SQLite; os testes de regras usam {@link #semPersistencia()}.
 */
public interface RepositorioBanco {

    /** Lê todas as contas gravadas, em ordem de número. */
    List<ContaPersistida> carregar();

    /**
     * Grava as contas informadas como uma única operação: ou todas as alterações são gravadas,
     * ou nenhuma é.
     *
     * @throws FalhaPersistenciaException se a gravação falhar; nesse caso nada foi gravado
     */
    void salvar(List<ContaPersistida> contas);

    /** Repositório que não guarda nada; os dados existem só em memória. */
    static RepositorioBanco semPersistencia() {
        return new RepositorioBanco() {
            @Override
            public List<ContaPersistida> carregar() {
                return List.of();
            }

            @Override
            public void salvar(List<ContaPersistida> contas) {
                // Nada a gravar.
            }
        };
    }
}
