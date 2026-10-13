// Ejemplo de referencia — SIN COMPILAR (ver README.md)
// Fake para pruebas unitarias/CI sin terminal. NO sustituye al sandbox ni sirve para certificar.
package com.example.pagos

import android.content.Intent

class FakePaymentGateway : PaymentGateway {
    val sentSales = mutableListOf<String>()      // folios enviados, para afirmar "un solo folio, un solo cobro"
    val lookups = mutableListOf<String>()
    var throwOnSale: Exception? = null           // simula SmartApiException al invocar
    private var next: GatewayResult? = null

    /** Programa el siguiente resultado que devolverá onActivityResult. */
    fun willReturn(result: GatewayResult) { next = result }

    override fun startSale(amount: Double, folio: String, msi: Int?, tip: Double?) {
        throwOnSale?.let { throw it }
        sentSales += folio
    }

    override fun startLookupByFolio(folio: String) { lookups += folio }
    override fun startCancel(orderId: String) {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): GatewayResult? =
        next.also { next = null }

    companion object {
        fun approved() = GatewayResult.Sale(SaleOutcome(true, "00", "APROBADA", "ORD1", "RRN1", "A"))
        fun declined() = GatewayResult.Sale(SaleOutcome(false, "05", "SIN COMUNICACIÓN", null, null, "A"))
        fun lookup(s: FolioStatus) = GatewayResult.Lookup(FolioLookup(s, null))
        fun canceled() = GatewayResult.Failure("RESULT_CANCELED")
    }
}

/* Pruebas que este fake permite (JUnit, ejemplo):
 *  - T12: llamar startSale dos veces seguidas => sentSales.size == 1.
 *  - T05: willReturn(canceled()) => estado DESCONOCIDA, lookups.size == 1 tras resolveByFolio(), sin reintento.
 *  - T08: willReturn(lookup(PENDIENTE_REVERSO)) => la app NO reversa; canRetry solo con confirmación.
 *  - Código no catalogado ("05"): willReturn(declined()) => DESCONOCIDA, no "declinada".
 *  - T11: guardar ENVIADA en FolioStore, crear coordinator nuevo, onAppStart() => DESCONOCIDA y consulta.
 */
