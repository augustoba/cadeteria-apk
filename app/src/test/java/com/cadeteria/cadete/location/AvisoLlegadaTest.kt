package com.cadeteria.cadete.location

import com.cadeteria.cadete.data.remote.dto.LookupDto
import com.cadeteria.cadete.data.remote.dto.ParadaDto
import com.cadeteria.cadete.data.remote.dto.PedidoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvisoLlegadaTest {

    // Origen y destino en Tucumán, a ~1,5 km entre sí.
    private val oLat = -26.8300
    private val oLng = -65.2040
    private val dLat = -26.8150
    private val dLng = -65.2100
    /** ~60 m al norte del origen: adentro del radio de 150 m. */
    private val cercaLat = oLat + 0.00055

    private fun pedido(
        estado: String = "EN_CURSO", retirado: Boolean = false, paradas: List<ParadaDto> = emptyList(),
    ) = PedidoDto(
        id = "p1", numero = 1500001, clienteTelefono = "3815550000", clienteNombre = "Ana",
        origenDireccion = "Maipú 700", origenLat = oLat, origenLng = oLng,
        destinoDireccion = "Av. Colón 1200", destinoLat = dLat, destinoLng = dLng,
        precio = 1000.0, montoDeclarado = null, detalle = null, requiereMoto = false,
        estado = LookupDto(estado, estado), cadeteAsignado = null, programado = false, fechaProgramada = null,
        creadoEn = null, asignadoEn = null, aceptadoEn = null, retiradoEn = if (retirado) "2026-09-26T12:00:00Z" else null,
        finalizadoEn = null, fotoRecepcionUrl = null, entregaReceptorNombre = null, entregaFotoUrl = null,
        tokenSeguimiento = "tok", paradas = paradas,
    )

    @Test
    fun `sin retirar solo esta pendiente el retiro, retirado las paradas y la entrega`() {
        val aviso = AvisoLlegada()
        assertEquals(listOf("p1:RETIRO"), aviso.puntosPendientes(listOf(pedido())).map { it.clave })

        val paradas = listOf(
            ParadaDto("s2", 2, "B", 0.0, 0.0, null),
            ParadaDto("s1", 1, "A", 0.0, 0.0, "2026-09-26T12:10:00Z"),
        )
        assertEquals(listOf("p1:PARADA:s2", "p1:ENTREGA"),
            aviso.puntosPendientes(listOf(pedido(retirado = true, paradas = paradas))).map { it.clave })

        assertTrue("uno PENDIENTE (sin aceptar) no avisa", aviso.puntosPendientes(listOf(pedido(estado = "PENDIENTE"))).isEmpty())
    }

    @Test
    fun `avisa recien despues de quedarse adentro, y una sola vez`() {
        val aviso = AvisoLlegada()
        val pendientes = aviso.puntosPendientes(listOf(pedido()))

        assertTrue(aviso.procesar(pendientes, cercaLat, oLng, 20f, 0).isEmpty()) // entra
        assertTrue(aviso.procesar(pendientes, cercaLat, oLng, 20f, 20_000).isEmpty()) // 20 s: todavía no
        val avisos = aviso.procesar(pendientes, cercaLat, oLng, 20f, 45_000) // 45 s adentro
        assertEquals(1, avisos.size)
        assertTrue(avisos[0].cuerpo.contains("Retirado"))

        assertTrue("no repite", aviso.procesar(pendientes, cercaLat, oLng, 20f, 120_000).isEmpty())
    }

    @Test
    fun `pasar por al lado sin quedarse no avisa`() {
        val aviso = AvisoLlegada()
        val pendientes = aviso.puntosPendientes(listOf(pedido()))

        aviso.procesar(pendientes, cercaLat, oLng, 20f, 0) // pasa cerca
        aviso.procesar(pendientes, oLat + 0.01, oLng, 20f, 45_000) // ya a ~1 km
        assertTrue(aviso.procesar(pendientes, cercaLat, oLng, 20f, 60_000).isEmpty()) // vuelve: cuenta de cero
    }

    @Test
    fun `un ping muy impreciso no cuenta`() {
        val aviso = AvisoLlegada()
        val pendientes = aviso.puntosPendientes(listOf(pedido()))
        aviso.procesar(pendientes, cercaLat, oLng, 500f, 0)
        assertTrue(aviso.procesar(pendientes, cercaLat, oLng, 500f, 60_000).isEmpty())
    }

    @Test
    fun `al marcar Retirado el aviso queda resuelto y se pasa a la entrega`() {
        val aviso = AvisoLlegada()
        val antes = aviso.puntosPendientes(listOf(pedido()))
        aviso.procesar(antes, cercaLat, oLng, 20f, 0)
        aviso.procesar(antes, cercaLat, oLng, 20f, 45_000)

        val despues = aviso.puntosPendientes(listOf(pedido(retirado = true)))
        assertEquals(setOf("p1:RETIRO"), aviso.clavesResueltas(despues))
        assertEquals(listOf("p1:ENTREGA"), despues.map { it.clave })

        aviso.procesar(despues, dLat, dLng, 15f, 100_000)
        val entrega = aviso.procesar(despues, dLat, dLng, 15f, 150_000)
        assertEquals(1, entrega.size)
        assertTrue(entrega[0].cuerpo.contains("Entregado"))
    }

    @Test
    fun `distancia aproximada`() {
        val d = AvisoLlegada.distanciaM(oLat, oLng, cercaLat, oLng)
        assertTrue("~61 m, dio $d", d in 55.0..67.0)
    }
}
