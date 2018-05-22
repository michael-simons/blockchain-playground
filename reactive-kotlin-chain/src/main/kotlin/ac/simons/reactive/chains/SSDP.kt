/*
 * Copyright 2018 michael-simons.eu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ac.simons.reactive.chains

import reactor.core.publisher.Mono
import java.net.*
import java.time.Duration

private const val NT = "urn:blockchain-playground:node"

private val SSDP_MULTICAST_ADDRESS = InetAddress.getByName("239.255.255.250")
private const val SSDP_PORT = 1900

fun openMulticastSocket(address: InetAddress): MulticastSocket {
    val socket = MulticastSocket(SSDP_PORT)
    socket.reuseAddress = true
    socket.soTimeout = Duration.ofMinutes(1).toMillis().toInt()
    socket.joinGroup(InetSocketAddress(SSDP_MULTICAST_ADDRESS, SSDP_PORT), NetworkInterface.getByInetAddress(address))
    return socket
}

private fun Chain.usn() = "uuid:${nodeId}::${NT}"

fun Chain.createSSDPAlivePacket(address: InetAddress, port: Int) = """
            NOTIFY * HTTP/1.1
            LOCATION: http://${address.hostAddress}:${port}
            NTS: ssdp:alive
            NT: ${NT}
            USN: ${usn()}
            HOST: 239.255.255.250:1900

        """.trimIndent().toByteArray().let { DatagramPacket(it, it.size, SSDP_MULTICAST_ADDRESS, SSDP_PORT) }

fun Chain.readSSDPAlivePacket(dp: DatagramPacket): Mono<String> {
    val ssdpRequest = String(dp.data)
            .lines().drop(1)
            .map { it.split(": ") }
            .filter { it.size == 2 }
            .associateBy({ it[0] }, { it[1] })
    return if (ssdpRequest["NT"].equals(NT, true) && ssdpRequest.containsKey("LOCATION") && ssdpRequest["USN"] != usn()) {
        Mono.just(ssdpRequest["LOCATION"]!!)
    } else Mono.empty<String>()
}