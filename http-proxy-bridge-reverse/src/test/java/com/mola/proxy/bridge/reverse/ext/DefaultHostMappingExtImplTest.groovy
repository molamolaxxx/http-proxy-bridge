package com.mola.proxy.bridge.reverse.ext

import com.mola.proxy.bridge.core.ext.def.DefaultHostMappingExtImpl
import spock.lang.Specification

class DefaultHostMappingExtImplTest extends Specification {

    def hostMappingTest = new DefaultHostMappingExtImpl([
            "molalocal2" : "127.0.0.1:80",
            "molalocal:8080" : "localhost:6080",
            "molalocal" : "localhost"
    ])

    def "FetchMappedAddress"() {
        expect:
        mappedResult == hostMappingTest.fetchMappedAddress(host, port)

        where:
        host         | port | mappedResult
        "molalocal"  | 8080 | "localhost:6080"
        "molalocal"  | 433  | "localhost:433"
        "molalocal2" | 433  | "127.0.0.1:80"
        "baidu.com"  | 433  | null
        "molalocal"  | 7071 | "localhost:7071"
        "molalocal2" | 8080 | "127.0.0.1:80"
    }
}
