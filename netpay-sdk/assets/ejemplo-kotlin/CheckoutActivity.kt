// Ejemplo de referencia — SIN COMPILAR (ver README.md)
package com.example.pagos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CheckoutActivity : AppCompatActivity(), PaymentCoordinator.Listener {

    private lateinit var coordinator: PaymentCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coordinator = PaymentCoordinator(
            gateway = NetpaySmartGateway(this),
            store = FolioStore(this),
            log = { android.util.Log.i("pagos", it) }   // el coordinator solo emite campos permitidos
        )
        coordinator.listener = this
        coordinator.onAppStart()          // T11: folio pendiente => DESCONOCIDA + consulta por folio
    }

    // Botón "Cobrar": deshabilitarlo en cuanto se toca (T12) y re-habilitarlo según canStartSale().
    fun onCobrarClicked(amount: Double) {
        if (!coordinator.canStartSale()) return
        coordinator.startSale(amount)
    }

    @Deprecated("API de Activity; el SDK lo documenta así")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        coordinator.onActivityResult(requestCode, resultCode, data)
    }

    override fun onState(state: PaymentState, detail: String?) {
        // INICIADA/ENVIADA/DESCONOCIDA: UI bloqueada. DESCONOCIDA: mostrar "Pendiente de confirmación".
        // APROBADA: mostrar resultado y liberar UI. Nunca mostrar "declinada" por un código no catalogado.
    }

    override fun onLookupResolved(status: FolioStatus, canRetry: Boolean) {
        // Mostrar el estatus al cajero. Si canRetry, ofrecer "Reintentar" con confirmación explícita
        // (coordinator.retryConfirmedByCashier). PENDIENTE_REVERSO: avisar que la venta NO es un cobro válido.
        // NO_CATALOGADO: escalar a soporte con (app, modelo, versión SDK, operación, folio).
    }
}
