// Ejemplo de referencia — SDK Smart 1.1.9 / Smart PinPad 2.0+ (ver README.md)
// Firmas: lecciones-campo.md §firmas (verificadas en el AAR 1.1.9). Lo no confirmado: TODO(soporte NetPay).
// Requisito: <queries> en el manifiesto (lecciones-campo.md §visibilidad), si no doTrans lanza
// IllegalArgumentException "Unknown URL content://..." antes de abrir la PinPad.
package com.example.pagos

import android.app.Activity
import android.content.Intent
import android.util.Log
import mx.com.netpay.sdk.SmartApiFactory
import mx.com.netpay.sdk.models.*

class NetpaySmartGateway(private val activity: Activity) : PaymentGateway {

    // Kotlin. En Java: SmartApiFactory.INSTANCE.createSmartApi(activity)
    private val smartApi = SmartApiFactory.createSmartApi(activity)

    // appId = package name de LA APP DEL INTEGRADOR (no el de la Smart PinPad; el ambiente lo marca la PinPad).
    private val appId: String = activity.packageName

    // TODO(soporte NetPay): qué valor lleva storeId en Cancel/Reprint y si es opcional. NO es una credencial:
    // nunca poner aquí Store ID/Password de login de la PinPad.
    private val storeId: String = ""

    override fun startSale(amountCents: Long, folio: String, msi: Int?, tipCents: Long?) = invoke("sale") {
        smartApi.doTrans(
            SaleRequest(                       // SIEMPRE argumentos nombrados: el orden real difiere de la doc
                appId = appId,
                amount = toAmount(amountCents),
                tip = tipCents?.let(::toAmount), // SUPUESTO: tip va aparte de amount (confirmar con T17)
                msi = msi,
                folio = folio
                // waiter, settlementId, settlementDate, table: no se usan en este ejemplo.
                // TODO(soporte NetPay): confirmar que tienen valor por defecto en el AAR.
            )
        )
    }

    override fun startLookupByFolio(folio: String) = invoke("lookup") {
        // orderId = "" explícito: si van ambos, gana orderId y la recuperación por folio no se completa.
        smartApi.doTrans(ReprintRequest(appId = appId, orderId = "", storeId = storeId, folioId = folio))
    }

    override fun startCancel(orderId: String) = invoke("cancel") {
        smartApi.doTrans(CancelRequest(appId = appId, orderId = orderId, storeId = storeId))
    }

    /**
     * Captura Exception, no solo SmartApiException: doTrans también lanza IllegalArgumentException
     * (provider no visible). Loguea clase, mensaje y 3 frames; nunca datos de tarjeta (no hay: la PinPad no abrió).
     */
    private inline fun invoke(op: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            val frames = e.stackTrace.take(3).joinToString(" | ")
            Log.w(TAG, "invoke op=$op ${e.javaClass.simpleName}: ${e.message} @ $frames")
            throw InvocationFailed("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): GatewayResult? {
        if (requestCode != Constants.SALE_REQUEST &&
            requestCode != Constants.CANCEL_REQUEST &&
            requestCode != Constants.REPRINT_REQUEST
        ) return null

        // RESULT_CANCELED (usuario se retiró o falló) o data nulo: NO es declinada; es no concluyente.
        if (resultCode != Activity.RESULT_OK || data == null) {
            return GatewayResult.Inconclusive("resultCode=$resultCode data=${data != null}")
        }
        return try {
            // onResult devuelve BaseResponse. CancelResponse y ReprintResponse HEREDAN de SaleResponse:
            // las subclases van primero en el when.
            when (val r = smartApi.onResult(requestCode, resultCode, data)) {
                is ReprintResponse -> GatewayResult.Lookup(toLookup(r))
                is CancelResponse -> GatewayResult.Cancel(toOutcome(r))
                is SaleResponse -> GatewayResult.Sale(toOutcome(r))
                else -> GatewayResult.Inconclusive("respuesta de tipo inesperado")
            }
        } catch (e: Exception) {
            GatewayResult.Inconclusive("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    // Se copian SOLO campos permitidos. cardNumber, bin, arqc, aid, etc. nunca salen de aquí.
    private fun toOutcome(r: SaleResponse) = SaleOutcome(
        success = r.success,          // Boolean no nulo en 1.1.9
        code = r.code,                // la doc lo llama responseCode
        message = r.message,
        orderId = r.orderId,
        rrnNumber = r.rrnNumber,
        transType = r.transType
    )

    // La reimpresión se valida con reprintModule + transType, NUNCA con code/responseCode.
    private fun toLookup(r: ReprintResponse): FolioLookup {
        val module = r.reprintModule
        val type = r.transType
        val status = when {
            !r.success -> FolioStatus.SIN_REGISTRO // "No se encontraron datos"
            module == "C" && type == "A" -> FolioStatus.APROBADA
            module == "C" && type == "V" -> FolioStatus.CANCELADA
            module == "D" -> FolioStatus.DECLINADA
            module == "RV" -> FolioStatus.REVERSADA
            module == "PRV" -> FolioStatus.PENDIENTE_REVERSO
            else -> FolioStatus.NO_CATALOGADO
        }
        return FolioLookup(status, r.orderId, r.message)
    }

    companion object {
        const val TAG = "pagos"   // etiqueta propia: leer solo con `adb logcat -s pagos`

        /** Centavos → Double con 2 decimales. El SDK valida 0.01–999,999.99 ("Monto inválido"). */
        fun toAmount(cents: Long): Double = java.math.BigDecimal.valueOf(cents, 2).toDouble()
    }
}
