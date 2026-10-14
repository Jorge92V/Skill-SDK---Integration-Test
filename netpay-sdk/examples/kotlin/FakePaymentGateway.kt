// Ejemplo de referencia — SIN COMPILAR (ver README.md)
// Fake para pruebas unitarias/CI sin terminal. NO sustituye al sandbox ni sirve para certificar.
package com.example.pagos

import android.content.Intent

class FakePaymentGateway : PaymentGateway {
    val sentSales = mutableListOf<String>()      // folios enviados: "un solo folio, un solo cobro"
    val lookups = mutableListOf<String>()
    val cancels = mutableListOf<String>()
    var failSaleInvoke = false                   // simula IllegalArgumentException/SmartApiException al invocar
    var failLookupInvoke = false
    private var next: GatewayResult? = null

    /** Programa el siguiente resultado que devolverá onActivityResult. */
    fun willReturn(result: GatewayResult) { next = result }

    override fun startSale(amountCents: Long, folio: String, msi: Int?, tipCents: Long?) {
        if (failSaleInvoke) throw InvocationFailed("IllegalArgumentException: Unknown URL")
        sentSales += folio
    }

    override fun startLookupByFolio(folio: String) {
        if (failLookupInvoke) throw InvocationFailed("IllegalArgumentException: Unknown URL")
        lookups += folio
    }

    override fun startCancel(orderId: String) { cancels += orderId }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): GatewayResult? =
        next.also { next = null }

    companion object {
        fun approved() = GatewayResult.Sale(SaleOutcome(true, "00", "APROBADA", "ORD1", "RRN1", "A"))
        fun uncatalogued() = GatewayResult.Sale(SaleOutcome(false, "05", "SIN COMUNICACIÓN", null, null, "A"))
        fun lookup(s: FolioStatus) = GatewayResult.Lookup(FolioLookup(s, "ORD1", null))
        fun canceled() = GatewayResult.Inconclusive("RESULT_CANCELED")
    }
}

/* Pruebas que este fake permite (JUnit, ejemplo):
 *  - T12: startSale dos veces seguidas => sentSales.size == 1.
 *  - T05: willReturn(canceled()) => DESCONOCIDA; resolveByFolio() => lookups.size == 1; sin reintento.
 *  - T08: willReturn(lookup(PENDIENTE_REVERSO)) => la app NO reversa; canRetry solo con confirmación.
 *  - Código no catalogado: willReturn(uncatalogued()) => DESCONOCIDA, no "declinada".
 *  - T11: guardar ENVIADA en FolioStore, coordinator nuevo, onResumeOnce() => DESCONOCIDA y 1 consulta;
 *         llamar onResumeOnce() otra vez => sigue 1 consulta (sin bucle).
 *  - T19: failSaleInvoke = true y failLookupInvoke = true => NO_INICIADA y canStartSale() == true.
 *  - T20: cancelar venta ya cancelada o fuera de horario CDMX => NoPermitida, sin llamar al gateway.
 */
