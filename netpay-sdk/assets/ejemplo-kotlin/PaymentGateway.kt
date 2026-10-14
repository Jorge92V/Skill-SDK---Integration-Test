// Ejemplo de referencia — SDK Smart 1.1.9 / Smart PinPad 2.0+ (ver README.md)
package com.example.pagos

/**
 * Estados de la app del integrador (transacciones.md §estado, lecciones-campo.md §salida).
 * NO_INICIADA: el SDK falló al invocar y la consulta por folio tampoco pudo invocarse; la transacción
 * nunca llegó a la PinPad, no hay cobro posible y la UI se libera.
 */
enum class PaymentState { INICIADA, ENVIADA, APROBADA, DESCONOCIDA, RESUELTA, NO_INICIADA }

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
    val code: String?,        // en la doc "responseCode"; en el SDK 1.1.9 la propiedad es `code`
    val message: String?,
    val orderId: String?,
    val rrnNumber: String?,
    val transType: String?
)

data class FolioLookup(val status: FolioStatus, val orderId: String?, val message: String?)

/** El SDK falló al INVOCAR (excepción síncrona, la PinPad no respondió nada). */
class InvocationFailed(val reason: String) : Exception(reason)

/**
 * Único punto de contacto con el SDK. La lógica de la app depende solo de esta interfaz.
 * Los `start*` lanzan [InvocationFailed] si el SDK no pudo invocar a la PinPad.
 */
interface PaymentGateway {
    /** amountCents/tipCents en centavos; la conversión a Double ocurre solo dentro del gateway. */
    fun startSale(amountCents: Long, folio: String, msi: Int? = null, tipCents: Long? = null)
    fun startLookupByFolio(folio: String)
    fun startCancel(orderId: String)

    /** Reenvía el resultado del Activity. Devuelve null si no es una respuesta de este gateway. */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?): GatewayResult?
}

sealed class GatewayResult {
    data class Sale(val outcome: SaleOutcome) : GatewayResult()
    data class Lookup(val result: FolioLookup) : GatewayResult()
    data class Cancel(val outcome: SaleOutcome) : GatewayResult()
    /** RESULT_CANCELED, data nulo o excepción al leer la respuesta: la terminal respondió algo no concluyente. */
    data class Inconclusive(val reason: String) : GatewayResult()
}
