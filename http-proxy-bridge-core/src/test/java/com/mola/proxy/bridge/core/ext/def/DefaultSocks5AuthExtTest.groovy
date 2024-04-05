package com.mola.proxy.bridge.core.ext.def

import com.mola.proxy.bridge.core.config.Socks5Config
import spock.lang.Specification

class DefaultSocks5AuthExtTest extends Specification {

    DefaultSocks5AuthExt socksCnfExt
    {
        def socks5Config = new Socks5Config()
        socks5Config.username = "mola"
        socks5Config.passwd = "123"
        socksCnfExt = new DefaultSocks5AuthExt(socks5Config)
    }

    def "Auth"() {
        expect:
        res == socksCnfExt.auth(username, passwd)

        where:
        username | passwd | res
        "mola" | "123" | true
        "mola" | "1234"| false
    }

    def "RequireAuth"() {
        expect:
        socksCnfExt.requireAuth()
    }
}
