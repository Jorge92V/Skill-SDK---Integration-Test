// Ejemplo de referencia — SIN COMPILAR (ver README.md)
package com.example.pagos

import android.content.Intent

/**
 * Lógica de la app: folio, máquina de estados, bloqueo de cobro. Depende solo de PaymentGateway,
 * así que se prueba en JVM con FakePaymentGateway.
 *
 * Reglas (SKILL.md §7, transacciones.md, lecciones-campo.md):
 *  - Folio único por intención, persistido ANTES de doTrans. Montos en centavos.
 *  - Mientras haya INICIADA/ENVIADA/DESCONOCIDA no se acepta otra venta (doble toque, T12).
 *  - Falla al INVOCAR ≠ respuesta no concluyente. Si ni la venta ni la consulta pueden invocarse:
 *    NO_INICIADA y se libera la UI (§salida). Lo demás no concluyente => DESCONOCIDA => consulta por folio.
 *  - La app NUNCA reversa. El reverso lo hace la Smart PinPad.
 *  - Reintento: nunca automático; folio NUEVO vinculado al anterior; solo con confirmación del cajero.
 */
class PaymentCoordinator(
    private val gateway: PaymentGateway,
    private val store: FolioStore,
    private val ledger: VentasAprobadas,
    private val log: (String) -> Unit,              // solo campos permitidos (seguridad.md §logging)
    private val now: () -> Long = System::currentTimeMillis
) {
    interface Listener {
        fun onState(state: PaymentState, detail: String?)
        fun onLookupResolved(status: FolioStatus, canRetry: Boolean)
        fun onCancel(result: CancelResult)
    }

    var listener: Listener? = null
    private var pendingCheckDone = false            // revisión de folio pendiente: UNA vez por instancia

    /** El botón de cobrar solo se habilita si esto es true. */
    fun canStartSale(): Boolean {
        val cur = store.current() ?: return true
        return cur.state in setOf(PaymentState.APROBADA, PaymentState.RESUELTA, PaymentState.NO_INICIADA)
    }

    /** Valida y lanza la venta. Devuelve un mensaje para el cajero si la validación local falla. */
    fun startSale(amountCents: Long, msi: Int? = null, tipCents: Long? = null, previousFolio: String? = null): String? {
        if (!canStartSale()) return null                                  // doble toque / multi-instancia
        if (amountCents !in 1L..99_999_999L) return "Monto inválido"      // mismo rango que valida el SDK
        if (tipCents != null && tipCents * 4 > amountCents) return "La propina no puede exceder el 25%"

        val folio = store.newFolio()
        store.save(FolioStore.Record(folio, PaymentState.INICIADA, amountCents, tipCents, previousFolio, null))
        log("state=INICIADA folio=$folio")
        store.save(FolioStore.Record(folio, PaymentState.ENVIADA, amountCents, tipCents, previousFolio, now()))
        log("state=ENVIADA folio=$folio")
        listener?.onState(PaymentState.ENVIADA, null)
        try {
            gateway.startSale(amountCents, folio, msi, tipCents)
        } catch (e: InvocationFailed) {
            onInvocationFailed(folio, e.reason)
        }
        return null
    }

    /** §salida: la venta no se pudo invocar. Se intenta la consulta; si tampoco, NO_INICIADA. */
    private fun onInvocationFailed(folio: String, reason: String) {
        log("invoke_failed folio=$folio reason=$reason")
        markUnknown(folio, reason)
        try {
            gateway.startLookupByFolio(folio)
        } catch (e: InvocationFailed) {
            val cur = store.current() ?: return
            store.save(cur.copy(state = PaymentState.NO_INICIADA))
            log("state=NO_INICIADA folio=$folio")
            listener?.onState(
                PaymentState.NO_INICIADA,
                "No se pudo comunicar con la Smart PinPad; el cobro no se inició."
            )
        }
    }

    /** Llamar desde Activity.onActivityResult (corre ANTES de onResume). */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val r = gateway.onActivityResult(requestCode, resultCode, data) ?: return
        if (r is GatewayResult.Cancel) { handleCancel(r.outcome); return }
        val cur = store.current() ?: return
        when (r) {
            is GatewayResult.Sale -> handleSale(cur, r.outcome)
            is GatewayResult.Lookup -> handleLookup(cur, r.result)
            is GatewayResult.Inconclusive -> markUnknown(cur.folio, r.reason)
            is GatewayResult.Cancel -> Unit
        }
    }

    private fun handleSale(cur: FolioStore.Record, o: SaleOutcome) {
        log("sale folio=${cur.folio} success=${o.success} code=${o.code} transType=${o.transType}")
        // Solo success + "00" es APROBADA. Cualquier otro código no está catalogado => DESCONOCIDA.
        if (o.success && o.code == "00") approve(cur, o.orderId) else markUnknown(cur.folio, "code=${o.code}")
    }

    private fun approve(cur: FolioStore.Record, orderId: String?) {
        store.save(cur.copy(state = PaymentState.APROBADA))
        if (orderId != null) ledger.add(cur.folio, orderId, cur.amountCents, cur.tipCents, now())
        listener?.onState(PaymentState.APROBADA, orderId)
    }

    private fun markUnknown(folio: String, reason: String) {
        val cur = store.current() ?: return
        store.save(cur.copy(state = PaymentState.DESCONOCIDA))
        log("state=DESCONOCIDA folio=$folio reason=$reason")
        listener?.onState(PaymentState.DESCONOCIDA, "Pendiente de confirmación")
    }

    /**
     * Consulta por folio. Es también la acción de salida del bloqueo ("Consultar de nuevo").
     * No pasar a DESCONOCIDA por tiempo antes de 3 min (5 con cashback).
     */
    fun resolveByFolio() {
        val cur = store.current() ?: return
        try {
            gateway.startLookupByFolio(cur.folio)
        } catch (e: InvocationFailed) {
            // La venta SÍ pudo haber llegado a la PinPad: no es NO_INICIADA. Sigue DESCONOCIDA con salida:
            // revisar pantalla/ticket de la terminal, <queries> (lecciones-campo.md §visibilidad) o escalar.
            log("lookup_invoke_failed folio=${cur.folio} reason=${e.reason}")
            listener?.onState(PaymentState.DESCONOCIDA, "No se pudo consultar el folio. Revisa la terminal o escala.")
        }
    }

    /**
     * Llamar en onResume (no en onCreate: el resultado puede estar por llegar en onActivityResult).
     * Se ejecuta UNA vez por instancia para no ciclar consulta → onResume → consulta (§ciclo).
     */
    fun onResumeOnce() {
        if (pendingCheckDone) return
        pendingCheckDone = true
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
                approve(cur, l.orderId)
                listener?.onLookupResolved(l.status, canRetry = false)
            }
            FolioStatus.NO_CATALOGADO -> {
                // Sigue DESCONOCIDA. Salidas: consultar de nuevo o escalar a soporte. No decidir.
                listener?.onLookupResolved(l.status, canRetry = false)
            }
            else -> {
                store.save(cur.copy(state = PaymentState.RESUELTA))
                // Política del skill (pendiente de visto bueno de NetPay): transacciones.md §consulta-por-folio.
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
    fun retryConfirmedByCashier(msi: Int? = null): String? {
        val prev = store.current() ?: return null
        return startSale(prev.amountCents, msi, prev.tipCents, previousFolio = prev.folio)
    }

    // ---- Cancelación (lecciones-campo.md §propina-cancelacion) ----

    fun cancel(folio: String): CancelResult {
        val venta = ledger.find(folio) ?: return CancelResult.NoPermitida("Venta no encontrada en este dispositivo")
        ledger.checkCancelable(venta, now())?.let { return CancelResult.NoPermitida(it) }
        ledger.mark(folio, EstadoVenta.CANCELACION_EN_CURSO)
        return try {
            gateway.startCancel(venta.orderId)
            CancelResult.EnCurso
        } catch (e: InvocationFailed) {
            ledger.mark(folio, EstadoVenta.APROBADA)    // nunca llegó a la PinPad
            CancelResult.NoPermitida("No se pudo comunicar con la Smart PinPad; la cancelación no se inició.")
        }
    }

    private fun handleCancel(o: SaleOutcome) {
        val venta = ledger.findCanceling() ?: return
        log("cancel folio=${venta.folio} success=${o.success} code=${o.code}")
        val result = if (o.success && o.code == "00") {
            ledger.mark(venta.folio, EstadoVenta.CANCELADA); CancelResult.Cancelada
        } else {
            // No concluyente: ni cancelada ni vigente. Verificar en la terminal y escalar.
            ledger.mark(venta.folio, EstadoVenta.CANCELACION_SIN_CONFIRMAR); CancelResult.SinConfirmar
        }
        listener?.onCancel(result)
    }
}

sealed class CancelResult {
    object EnCurso : CancelResult()
    object Cancelada : CancelResult()
    object SinConfirmar : CancelResult()
    data class NoPermitida(val motivo: String) : CancelResult()
}
