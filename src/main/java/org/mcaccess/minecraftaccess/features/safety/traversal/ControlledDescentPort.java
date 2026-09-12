package org.mcaccess.minecraftaccess.features.safety.traversal;

import org.jetbrains.annotations.NotNull;

/**
 * Porta di disaccoppiamento per l'acquisizione e il rinnovo delle lease di discesa controllata (Contratto D5).
 */
public interface ControlledDescentPort {

    /**
     * Acquisisce una lease di discesa per una specifica colonna arrampicabile validata.
     * @param columnId identificatore univoco della colonna.
     * @param requiresSneak true se il tipo cinematico (es. scaffolding) richiede abbassamento sintetico.
     */
    void acquireDescentLease(@NotNull String columnId, boolean requiresSneak);

    /**
     * Rinnova la lease di discesa attiva a ogni tick di transito.
     */
    void renewDescentLease(@NotNull String columnId);

    /**
     * Rilascia la lease di discesa in modo idempotente.
     */
    void releaseDescentLease(@NotNull String columnId);

    /**
     * Verifica se è attiva una qualsiasi lease di discesa assistita.
     */
    boolean hasActiveDescentLease();

    /**
     * Verifica se la lease attiva appartiene alla colonna specificata.
     */
    boolean isDescentLeaseActiveFor(@NotNull String columnId);
}
