// Ejemplo de referencia — SIN COMPILAR (ver README.md)
// Manifiesto del Activity (lecciones-campo.md §ciclo): evitar recreación durante el cobro
//   android:launchMode="singleTask"
//   android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden|uiMode"
package com.example.pagos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class CheckoutActivity : ComponentActivity(), PaymentCoordinator.Listener {

    private lateinit var coordinator: PaymentCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coordinator = PaymentCoordinator(
            gateway = NetpaySmartGateway(this),
            store = FolioStore(this),
            ledger = VentasAprobadas(this),
            log = { android.util.Log.i(NetpaySmartGateway.TAG, it) }   // solo campos permitidos
        )
        coordinator.listener = this
        // NO revisar el folio pendiente aquí: el resultado puede llegar en onActivityResult.
        // targetSdk 35: pantalla de borde a borde, manejar insets en la UI.
    }

    override fun onResume() {
        super.onResume()
        coordinator.onResumeOnce()   // T11: folio pendiente => DESCONOCIDA + consulta, UNA vez por instancia
    }

    // Botón "Cobrar": deshabilitarlo en cuanto se toca (T12) y re-habilitarlo según canStartSale().
    fun onCobrarClicked(amountCents: Long, tipCents: Long? = null) {
        val error = coordinator.startSale(amountCents, tipCents = tipCents)
        if (error != null) { /* mostrar error al cajero; no se llamó al SDK */ }
    }

    @Deprecated("El SDK entrega el resultado por startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        coordinator.onActivityResult(requestCode, resultCode, data)
    }

    override fun onState(state: PaymentState, detail: String?) {
        // ENVIADA/DESCONOCIDA: UI bloqueada, pero SIEMPRE con salida: "Consultar de nuevo"
        //   (coordinator.resolveByFolio) y "Escalar a soporte".
        // NO_INICIADA: mostrar detail y liberar la UI (no hubo cobro).
        // APROBADA: mostrar resultado y liberar UI. Nunca "declinada" por un código no catalogado.
    }

    override fun onLookupResolved(status: FolioStatus, canRetry: Boolean) {
        // Mostrar el estatus. Si canRetry, ofrecer "Reintentar" con confirmación (retryConfirmedByCashier).
        // PENDIENTE_REVERSO: la venta NO es cobro válido; se reversa con la siguiente transacción.
        // NO_CATALOGADO: salidas "Consultar de nuevo" y "Escalar" (app, modelo, versión SDK, operación, folio).
    }

    override fun onCancel(result: CancelResult) {
        // SinConfirmar: ni cancelada ni vigente; pedir verificar la pantalla/ticket de la terminal y escalar.
    }
}
