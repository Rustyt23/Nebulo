package com.frostnerd.smokescreen.util.proxy
import android.content.Context
import com.frostnerd.dnstunnelproxy.DnsCache
import org.minidns.dnsmessage.DnsMessage
import org.minidns.dnsmessage.Question
import org.minidns.record.Record

class DnsCache(private val context: Context) : DnsCache {


    override fun cacheAnswer(question: Question, dnsMessage: DnsMessage) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun getCachedAnswer(question: Question): List<Record<*>>? {
        TODO("Not yet implemented")
    }

    override fun isCached(question: Question): Boolean {
        TODO("Not yet implemented")
    }
}