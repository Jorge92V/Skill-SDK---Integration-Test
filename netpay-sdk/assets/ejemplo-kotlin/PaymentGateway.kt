// Ejemplo de referencia — SDK Smart 1.1.9 / Smart PinPad 2.0+ — SIN COMPILAR (ver README.md)
package com.example.pagos

/** Estados de la app del integrador (transacciones.md §estado). */
enum class PaymentState { INICIADA, ENVIADA, APROBADA, DESCONOCIDA, RESUELTA }

/** Resultado de la reimpresión por folio, ya traducido (transacciones.md §consulta-por-folio). */
enum class FolioStatus {
    APROBADA,           // reprintModule C + transType A
    CANCELADA,          // C + V
    DECLINADA,          // D
    REVERSADA,          // RV
    PENDIENTE_REVERSO,  // PRV
    SIN_REGISTRO,       // success == false, "No se encontraron datos"
    NO_CATALOGADO       // cualquier otro: escalar a soporte
}

/** Solo campos permitidos en logs (seguridad.md §logging). Nada de tarjeta, EMV ni objeto completo. */
data class SaleOutcome(
    val success: Boolean,
    val responseCode: String?,
    val message: String?,
    val orderId: String?,
    val rrnNumber: String?,
    val transType: String?
)

data class FolioLookup(val status: FolioStatus, val message: String?)

/**
 * Único punto de contacto con el SDK. La lógica de la app depende solo de esta interfaz
 * (el SDK no expone una inyectable). Se prueba con FakePaymentGateway.
 */
interface PaymentGateway {
    /** Lanza la venta. El resultado llega después por [onActivityResult]. */
    fun startSale(amount: Double, folio: String, msi: Int? = null, tip: Double? = null)

    /** Lanza la reimpresión por folio (orderId vacío). El resultado llega por [onActivityResult]. */
    fun startLookupByFolio(folio: String)

    /** Lanza la cancelación por orderId. */
    fun startCancel(orderId: String)

    /** Reenvía el resultado del Activity. Devuelve null si no es una respuesta que este gateway entienda. */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?): GatewayResult?
}

sealed class GatewayResult {
    data class Sale(val outcome: SaleOutcome) : GatewayResult()
    data class Lookup(val result: FolioLookup) : GatewayResult()
    data class Cancel(val outcome: SaleOutcome) : GatewayResult()
    /** RESULT_CANCELED, data nulo o SmartApiException: la venta queda DESCONOCIDA. */
    data class Failure(val reason: String) : GatewayResult()
}
