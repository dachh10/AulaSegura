package com.example.aulasegura.domain.model

/**
 * Nombre que el bloque 4 le dio a la fase de sincronización del pase de lista.
 *
 * Se conserva como alias porque el historial, la pantalla de lista y sus tests ya
 * lo usan; las entidades nuevas (incidentes) hablan de [SyncPhase], que es el
 * mismo tipo. Un segundo enum idéntico solo habría duplicado el mapeo desde
 * `SyncState` y abierto la puerta a que los dos se desincronicen.
 */
typealias RollCallSync = SyncPhase
