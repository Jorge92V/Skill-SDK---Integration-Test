// Ejemplo de referencia — SIN COMPILAR (ver README.md)
package com.example.pagos

import android.content.Intent

/**
 * Lógica de la app: folio, máquina de estados, bloqueo de cobro. Depende solo de PaymentGateway,
 * así que se prueba en JVM con FakePaymentGateway.
 *
 * Reglas que implementa (SKILL.md §6, transacciones.md):
 *  - Folio único por intención, persistido ANTES de doTrans.
 *  - Mientras haya INICIADA/ENVIADA/DESCONOCIDA no se acepta otra venta (doble toque, T12).
 *  - Cualquier cosa que no sea éxito claro => DESCONOCIDA => consulta por folio.
 *  - La app NUNCA reversa. El reverso lo hace la Smart PinPad.
 *  - Reintento: nunca automático; folio NUEVO vinculado al anterior; solo con confirmación del cajero.
 */
class PaymentCoordinator(
    private val gateway: PaymentGateway,
    private val store: FolioStore,
    private val log: (String) -> Unit,              // solo campos permitidos (seguridad.md §logging)
    private val now: () -> Long = System::currentTimeMillis
) {
    interface Listener {
        fun onState(state: PaymentState, detail: String?)
        fun onLookupResolved(status: FolioStatus, canRetry: Boolean)
    }

    var listener: Listener? = null

    /** El botón de cobrar solo se habilita si esto es true. */
    fun canStartSale(): Boolean {
        val cur = store.current() ?: return true
        return cur.state == PaymentState.APROBADA || cur.state == PaymentState.RESUELTA
    }

    fun startSale(amount: Double, msi: Int? = null, tip: Double? = null, previousFolio: String? = null) {
        if (!canStartSale()) return                      // bloqueo de doble toque / multi-instancia
        val folio = store.newFolio()
        store.save(FolioStore.Record(folio, PaymentState.INICIADA, amount, previousFolio, null))
        log("state=INICIADA folio=$folio")
        listener?.onState(PaymentState.INICIADA, null)

        store.save(FolioStore.Record(folio, PaymentState.ENVIADA, amount, previousFolio, now()))
        log("state=ENVIADA folio=$folio")
        listener?.onState(PaymentState.ENVIADA, null)
        try {
            gateway.startSale(amount, folio, msi, tip)
        } catch (e: Exception) {
            markUnknown(folio, "excepción al invocar: ${e.javaClass.simpleName}")
        }
    }

    /** Llamar desde Activity.onActivityResult. */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val cur = store.current() ?: return
        when (val r = gateway.onActivityResult(requestCode, resultCode, data) ?: return) {
            is GatewayResult.Sale -> handleSale(cur, r.outcome)
            is GatewayResult.Lookup -> handleLookup(cur, r.result)
            is GatewayResult.Cancel -> log("cancel success=${r.outcome.success} orderId=${r.outcome.orderId}")
            is GatewayResult.Failure -> markUnknown(cur.folio, r.reason)
        }
    }

    private fun handleSale(cur: FolioStore.Record, o: SaleOutcome) {
        log("sale folio=${cur.folio} success=${o.success} responseCode=${o.responseCode} transType=${o.transType}")
        // Solo "00" + success es APROBADA. Cualquier otro código no está catalogado => DESCONOCIDA.
        if (o.success && o.responseCode == "00") {
            store.save(cur.copy(state = PaymentState.APROBADA))
            listener?.onState(PaymentState.APROBADA, o.orderId)
        } else {
            markUnknown(cur.folio, "responseCode=${o.responseCode} message=${o.message}")
        }
    }

    private fun markUnknown(folio: String, reason: String) {
        val cur = store.current() ?: return
        store.save(cur.copy(state = PaymentState.DESCONOCIDA))
        log("state=DESCONOCIDA folio=$folio reason=$reason")
        listener?.onState(PaymentState.DESCONOCIDA, "Pendiente de confirmación")
    }

    /**
     * Consulta por folio (reimpresión). Llamar: ante DESCONOCIDA, y al reabrir la app con un folio
     * persistido sin resultado final (T11). No pasar a DESCONOCIDA por tiempo antes de 3 min (5 con cashback).
     */
    fun resolveByFolio() {
        val cur = store.current() ?: return
        gateway.startLookupByFolio(cur.folio)
    }

    /** Llamar al iniciar la app. */
    fun onAppStart() {
        val cur = store.current() ?: return
        if (cur.state == PaymentState.INICIADA || cur.state == PaymentState.ENVIADA) {
            markUnknown(cur.folio, "app reabierta sin resultado final")
        }
        if (store.current()?.state == PaymentState.DESCONOCIDA) resolveByFolio()
    }

    private fun handleLookup(cur: FolioStore.Record, l: FolioLookup) {
        log("lookup folio=${cur.folio} status=${l.status}")
        when (l.status) {
            FolioStatus.APROBADA -> {
                store.save(cur.copy(state = PaymentState.APROBADA))
                listener?.onLookupResolved(l.status, canRetry = false)
            }
            FolioStatus.NO_CATALOGADO -> {
                // Sigue DESCONOCIDA; escalar a soporte. No decidir.
                listener?.onLookupResolved(l.status, canRetry = false)
            }
            else -> {
                // CANCELADA, DECLINADA, REVERSADA, PENDIENTE_REVERSO, SIN_REGISTRO => RESUELTA.
                store.save(cur.copy(state = PaymentState.RESUELTA))
                // Política del skill (pendiente de visto bueno de NetPay): ver transacciones.md §consulta-por-folio.
                // Reintento ofrecido, siempre con confirmación del cajero. SIN_REGISTRO: solo tras verificar
                // la pantalla/ticket de la terminal. CANCELADA no es reintento.
                val canRetry = l.status in setOf(
                    FolioStatus.DECLINADA, FolioStatus.REVERSADA,
                    FolioStatus.PENDIENTE_REVERSO, FolioStatus.SIN_REGISTRO
                )
                listener?.onLookupResolved(l.status, canRetry)
            }
        }
    }

    /**
     * El cajero confirmó el reintento. Folio NUEVO vinculado al anterior.
     * Riesgo residual: sin deduplicación por folio, una transacción en vuelo puede aprobarse
     * después de la consulta y este reintento sería un segundo cobro.
     */
    fun retryConfirmedByCashier(amount: Double, msi: Int? = null, tip: Double? = null) {
        val prev = store.current()?.folio
        startSale(amount, msi, tip, previousFolio = prev)
    }
}
