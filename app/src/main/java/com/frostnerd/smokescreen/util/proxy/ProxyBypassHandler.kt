package com.frostnerd.smokescreen.util.proxy

import com.frostnerd.dnstunnelproxy.AbstractUDPDnsHandle
import com.frostnerd.dnstunnelproxy.Packet
import com.frostnerd.dnstunnelproxy.UpstreamAddress
import com.frostnerd.vpntunnelproxy.DeviceWriteToken
import com.frostnerd.vpntunnelproxy.FutureAnswer
import org.minidns.dnsmessage.DnsMessage
import java.net.DatagramPacket
import java.net.InetAddress

/*
 * Copyright (C) 2019 Daniel Wolf (Ch4t4r)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can contact the developer at daniel.wolf@frostnerd.com.
 */
class ProxyBypassHandler(private val searchDomains:List<String>, private val destinationDnsServer:InetAddress):AbstractUDPDnsHandle() {
    override val handlesSpecificRequests: Boolean = true
    private val upstreamAddress = UpstreamAddress(destinationDnsServer, 53)

    companion object {
        val knownSearchDomains = listOf("fast.com")
    }

    override fun shouldHandleRequest(dnsMessage: DnsMessage): Boolean {
        if(dnsMessage.questions.isEmpty()) return false
        val name = dnsMessage.question.name
        return searchDomains.any {
            name.endsWith(it)
        }
    }

    override fun name(): String {
        return "ProxyBypassHandler[$searchDomains]"
    }

    override fun forwardDnsQuestion(
        deviceWriteToken: DeviceWriteToken,
        dnsMessage: DnsMessage,
        originalEnvelope: Packet,
        realDestination: UpstreamAddress
    ) {
        val bytes = dnsMessage.toArray()
        val packet = DatagramPacket(bytes, bytes.size, destinationDnsServer, 53)
        sendPacketToUpstreamDNSServer(deviceWriteToken, packet, originalEnvelope)
    }

    override fun shouldHandleDestination(destinationAddress: InetAddress, port: Int): Boolean {
        return true
    }

    override fun informFailedRequest(request: FutureAnswer, failureReason:Throwable?) {
    }

    override fun modifyUpstreamResponse(dnsMessage: DnsMessage): DnsMessage {
        return dnsMessage
    }

    override fun remapDestination(destinationAddress: InetAddress, port: Int): UpstreamAddress {
        return upstreamAddress
    }

    override fun shouldModifyUpstreamResponse(dnsMessage: DnsMessage): Boolean {
        return false
    }

}