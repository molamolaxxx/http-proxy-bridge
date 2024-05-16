package com.mola.proxy.bridge.core.router


import spock.lang.Specification

class ConnectionRouterTest extends Specification {

    def "Match"() {
        expect:
        ConnectionRouter.HostTrieTree hostTrieTree = new ConnectionRouter.HostTrieTree()
        hostTrieTree.consume(rule)
        hostTrieTree.match(input) == res

        where:
        rule | input | res
        "*.com"| "www.molaspace.kh.com" | true
        "a.com"| "www.molaspace.kh.com" | false
        "www.*.*.com"| "www.molaspace.kh.com" | true
        "www.*.com"| "www.molaspace.kh.com" | true
        "www.molaspace.com"| "www.molaspace.kh.com" | false
        "*.molaspace.com"| "www.molaspace.kh.com" | false
        "www.*"| "www.molaspace.kh.com" | true
        "www.*.*"| "www.molaspace.kh.com" | true
        "www.*.kh"| "www.molaspace.kh.com" | false
        "www.*.kh.*"| "www.molaspace.kh.com" | true
        "www.*.kh.*"| "" | false
        "*.molaspace.com"| "www.molaspace.com" | true
        "ttt.*.com"| "www.molaspace.kh.com" | false
        "*.molaspace.com"| "molaspace.com" | false
        "*.baidu.com" | "hpd.baidu.com" | true
    }
}
