// Ejemplo de referencia — SDK Smart 1.1.9 / Smart PinPad 2.0+ — SIN COMPILAR (ver README.md)
// Firmas tomadas de la doc pública de NetPay. Lo no confirmado está marcado TODO(soporte NetPay).
package com.example.pagos

import android.app.Activity
import android.content.Intent
import mx.com.netpay.sdk.SmartApiFactory
import mx.com.netpay.sdk.exceptions.SmartApiException
import mx.com.netpay.sdk.models.*

class NetpaySmartGateway(private val activity: Activity) : PaymentGateway {

    // Kotlin. En Java: SmartApiFactory.INSTANCE.createSmartApi(activity)
    private val smartApi = SmartApiFactory.createSmartApi(activity)

    // appId = package name de LA APP DEL INTEGRADOR (no el de la Smart PinPad).
    private val appId: String = activity.packageName

    override fun startSale(amount: Double, folio: String, msi: Int?, tip: Double?) {
        // Argumentos NOMBRADOS: el orden posicional difiere entre los ejemplos Kotlin y Java de NetPay.
        // amount: máx. 2 decimales. msi ∈ {3,6,9,12,18}. tip: máx. 25%, requiere activarse en la PinPad.
        val request = SaleRequest(
            appId = appId,
            amount = amount,
            msi = msi,
            tip = tip,
            folio = folio
        )
        // TODO(soporte NetPay): confirmar nombre del campo (folio vs folioId) y si doTrans lanza
        // SmartApiException de forma síncrona; en ese caso el llamador pasa a DESCONOCIDA.
        smartApi.doTrans(request)
    }

    override fun startLookupByFolio(folio: String) {
        // orderId VACÍO: si se envían ambos, gana orderId y la recuperación por folio no se completa.
        smartApi.doTrans(ReprintRequest(appId, "", folioId = folio))
    }

    override fun startCancel(orderId: String) {
        // Solo mismo día, antes de las 20:00 (CDMX), por el monto total.
        smartApi.doTrans(CancelRequest(appId, orderId))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): GatewayResult? {
        if (requestCode != Constants.SALE_REQUEST &&
            requestCode != Constants.CANCEL_REQUEST &&
            requestCode != Constants.REPRINT_REQUEST
        ) return null

        // RESULT_CANCELED (usuario se retiró o falló) o data nulo: NO es declinada; es DESCONOCIDA.
        if (resultCode != Activity.RESULT_OK || data == null) {
            return GatewayResult.Failure("resultCode=$resultCode data=${data != null}")
        }
        return try {
            // TODO(soporte NetPay): confirmar el tipo de retorno de onResult y el callback vigente
            // (la doc usa onActivityResult y onActivityCallResult).
            val raw = smartApi.onResult(requestCode, resultCode, data)
            when (requestCode) {
                Constants.SALE_REQUEST -> GatewayResult.Sale(toOutcome(raw as SaleResponse))
                Constants.CANCEL_REQUEST -> GatewayResult.Cancel(toOutcome(raw as CancelResponse))
                else -> GatewayResult.Lookup(toLookup(raw as ReprintResponse))
            }
        } catch (e: SmartApiException) {
            // No loguear el objeto completo. Solo el mensaje de la excepción.
            GatewayResult.Failure("SmartApiException: ${e.message}")
        } catch (e: ClassCastException) {
            GatewayResult.Failure("respuesta con tipo inesperado")
        }
    }

    // Se copian SOLO los campos permitidos. cardNumber, bin, arqc, aid, etc. nunca salen de aquí.
    // TODO(soporte NetPay): confirmar los nombres de acceso (success/isSuccess, etc.) en las clases reales.
    private fun toOutcome(r: SaleResponse) = SaleOutcome(
        success = r.success == true,
        responseCode = r.responseCode,
        message = r.message,
        orderId = r.orderId,
        rrnNumber = r.rrnNumber,
        transType = r.transType
    )

    private fun toOutcome(r: CancelResponse) = SaleOutcome(
        success = r.success == true,
        responseCode = r.responseCode,
        message = r.message,
        orderId = r.orderId,
        rrnNumber = r.rrnNumber,
        transType = r.transType
    )

    // La reimpresión se valida con reprintModule + transType, NUNCA con responseCode.
    private fun toLookup(r: ReprintResponse): FolioLookup {
        val module = r.reprintModule
        val type = r.transType
        val status = when {
            r.success != true -> FolioStatus.SIN_REGISTRO // "No se encontraron datos"
            module == "C" && type == "A" -> FolioStatus.APROBADA
            module == "C" && type == "V" -> FolioStatus.CANCELADA
            module == "D" -> FolioStatus.DECLINADA
            module == "RV" -> FolioStatus.REVERSADA
            module == "PRV" -> FolioStatus.PENDIENTE_REVERSO
            else -> FolioStatus.NO_CATALOGADO
        }
        return FolioLookup(status, r.message)
    }
}
