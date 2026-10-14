// Ejemplo de referencia — SIN COMPILAR (ver README.md)
package com.example.pagos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * El SDK no expone histórico: para cancelar, la app guarda localmente sus ventas aprobadas.
 * Solo folio, orderId, montos, hora y estado. NUNCA datos de tarjeta.
 * Reglas de cancelación (transacciones.md §ventanas): mismo día y antes de las 20:00 en
 * America/Mexico_City (no la zona del dispositivo), monto total, sin cancelar dos veces (T07).
 */
enum class EstadoVenta { APROBADA, CANCELACION_EN_CURSO, CANCELADA, CANCELACION_SIN_CONFIRMAR }

data class Venta(
    val folio: String,
    val orderId: String,
    val amountCents: Long,
    val tipCents: Long?,
    val approvedAtMillis: Long,
    val estado: EstadoVenta
)

class VentasAprobadas(context: Context) {

    private val prefs = context.getSharedPreferences("pagos_ventas", Context.MODE_PRIVATE)
    private val cdmx: ZoneId = ZoneId.of("America/Mexico_City")

    fun add(folio: String, orderId: String, amountCents: Long, tipCents: Long?, atMillis: Long) =
        save(all().filterNot { it.folio == folio } + Venta(folio, orderId, amountCents, tipCents, atMillis, EstadoVenta.APROBADA))

    fun find(folio: String): Venta? = all().firstOrNull { it.folio == folio }
    fun findCanceling(): Venta? = all().firstOrNull { it.estado == EstadoVenta.CANCELACION_EN_CURSO }

    fun mark(folio: String, estado: EstadoVenta) =
        save(all().map { if (it.folio == folio) it.copy(estado = estado) else it })

    /** Devuelve el motivo si NO se puede cancelar; null si sí. */
    fun checkCancelable(v: Venta, nowMillis: Long): String? {
        if (v.estado != EstadoVenta.APROBADA) return when (v.estado) {
            EstadoVenta.CANCELADA -> "La venta ya está cancelada"
            EstadoVenta.CANCELACION_SIN_CONFIRMAR -> "Cancelación sin confirmar: verifica en la terminal y escala"
            else -> "Hay una cancelación en curso"
        }
        val now = Instant.ofEpochMilli(nowMillis).atZone(cdmx)
        val sold = Instant.ofEpochMilli(v.approvedAtMillis).atZone(cdmx)
        if (now.toLocalDate() != sold.toLocalDate()) return "Solo se cancelan ventas del mismo día"
        if (!now.toLocalTime().isBefore(LocalTime.of(20, 0))) return "Las cancelaciones son antes de las 20:00 (hora CDMX)"
        return null
    }

    private fun all(): List<Venta> {
        val arr = JSONArray(prefs.getString("ventas", "[]"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Venta(
                o.getString("folio"), o.getString("orderId"), o.getLong("amount"),
                if (o.has("tip")) o.getLong("tip") else null,
                o.getLong("at"), EstadoVenta.valueOf(o.getString("estado"))
            )
        }
    }

    private fun save(list: List<Venta>) {
        val arr = JSONArray()
        list.forEach { v ->
            arr.put(JSONObject().apply {
                put("folio", v.folio); put("orderId", v.orderId); put("amount", v.amountCents)
                v.tipCents?.let { put("tip", it) }
                put("at", v.approvedAtMillis); put("estado", v.estado.name)
            })
        }
        prefs.edit().putString("ventas", arr.toString()).commit()
    }
}
