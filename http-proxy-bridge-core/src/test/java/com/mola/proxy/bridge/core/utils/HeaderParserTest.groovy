package com.mola.proxy.bridge.core.utils

import com.mola.proxy.bridge.core.entity.ProxyHttpHeader
import spock.lang.Specification

class HeaderParserTest extends Specification {

    def "Parse"() {
        expect:
        ProxyHttpHeader res = HeaderParser.parse(header)
        res.host == host
        res.port == port
        res.connectMethod == isConnectMethod

        where:
        header | host | port | isConnectMethod
        "qqq" | "" | 80 | false
        "CONNECT www.microsoft.com:443 HTTP/1.0\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko\n" +
                "Host: www.microsoft.com:443\n" +
                "Content-Length: 0\n" +
                "DNT: 1\n" +
                "Connection: Keep-Alive\n" +
                "Pragma: no-cache" | "www.microsoft.com" | 443 | true
        "GET /api/resource HTTP/1.1\n" +
                "Host: example.com\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36\n" +
                "Accept: application/json\n" +
                "Authorization: Bearer token123" | "example.com" | 80 | false
        "POST /api/resource HTTP/1.1\n" +
                "Host: molalocal.com:8081\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36\n" +
                "Content-Type: application/json\n" +
                "Content-Length: 32\n" +
                "Authorization: Bearer token123" | "molalocal.com" | 8081 | false
    }
}
