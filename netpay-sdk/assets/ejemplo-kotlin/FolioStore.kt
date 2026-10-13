// Ejemplo de referencia — SIN COMPILAR (ver README.md)
package com.example.pagos

import android.content.Context
import java.util.UUID

/**
 * Persiste el folio y el estado ANTES de llamar al SDK, para sobrevivir a un crash o cierre de la app.
 * Solo guarda folio, estado, monto, vínculo al folio anterior y timestamps. Nada de datos de tarjeta.
 *
 * Una sola transacción activa por vez (la UI se bloquea mientras haya INICIADA/ENVIADA/DESCONOCIDA).
 */
class FolioStore(context: Context) {

    private val prefs = context.getSharedPreferences("pagos_folio", Context.MODE_PRIVATE)

    data class Record(
        val folio: String,
        val state: PaymentState,
        val amount: Double,
        val previousFolio: String?, // vínculo en reintentos (transacciones.md §reintento)
        val sentAtMillis: Long?
    )

    /** Un folio nuevo por intención de cobro. Nunca se reutiliza. */
    fun newFolio(): String = "F-" + UUID.randomUUID().toString().replace("-", "").take(24)
    // TODO(soporte NetPay): confirmar formato permitido del folio (la reimpresión acepta hasta 250 caracteres).

    fun current(): Record? {
        val folio = prefs.getString("folio", null) ?: return null
        return Record(
            folio = folio,
            state = PaymentState.valueOf(prefs.getString("state", PaymentState.INICIADA.name)!!),
            amount = Double.fromBits(prefs.getLong("amount", 0.0.toRawBits())),
            previousFolio = prefs.getString("previous", null),
            sentAtMillis = if (prefs.contains("sentAt")) prefs.getLong("sentAt", 0) else null
        )
    }

    /** commit() síncrono: el folio debe estar en disco antes de llamar a doTrans. */
    fun save(r: Record) {
        prefs.edit()
            .putString("folio", r.folio)
            .putString("state", r.state.name)
            .putLong("amount", r.amount.toRawBits())
            .putString("previous", r.previousFolio)
            .apply { if (r.sentAtMillis != null) putLong("sentAt", r.sentAtMillis) else remove("sentAt") }
            .commit()
    }

    fun clear() { prefs.edit().clear().commit() }
}
