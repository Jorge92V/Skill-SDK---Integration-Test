// Ejemplo de referencia — SIN COMPILAR (ver README.md)
package com.example.pagos

import android.content.Context
import java.util.UUID

/**
 * Persiste el folio y el estado ANTES de llamar al SDK, para sobrevivir a un crash o cierre de la app.
 * Solo guarda folio, estado, monto en centavos, vínculo al folio anterior y timestamps. Nada de tarjeta.
 * Ojo: sobrevive también a `adb install -r` (lecciones-campo.md §terminal).
 */
class FolioStore(context: Context) {

    private val prefs = context.getSharedPreferences("pagos_folio", Context.MODE_PRIVATE)

    data class Record(
        val folio: String,
        val state: PaymentState,
        val amountCents: Long,
        val tipCents: Long?,
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
            amountCents = prefs.getLong("amount", 0),
            tipCents = if (prefs.contains("tip")) prefs.getLong("tip", 0) else null,
            previousFolio = prefs.getString("previous", null),
            sentAtMillis = if (prefs.contains("sentAt")) prefs.getLong("sentAt", 0) else null
        )
    }

    /** commit() síncrono: el folio debe estar en disco antes de llamar a doTrans. */
    fun save(r: Record) {
        val e = prefs.edit()
            .putString("folio", r.folio)
            .putString("state", r.state.name)
            .putLong("amount", r.amountCents)
            .putString("previous", r.previousFolio)
        if (r.tipCents != null) e.putLong("tip", r.tipCents) else e.remove("tip")
        if (r.sentAtMillis != null) e.putLong("sentAt", r.sentAtMillis) else e.remove("sentAt")
        e.commit()
    }

    /** Solo para builds de desarrollo (BuildConfig.DEBUG): limpia un folio atascado de pruebas anteriores. */
    fun clearForDevelopment() { prefs.edit().clear().commit() }
}
