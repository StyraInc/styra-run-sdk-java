package com.styra.run.discovery

import spock.lang.Specification

import static com.styra.run.test.helpers.gatewayFrom

class StaticGatewaySelectorSpec extends Specification {
    def "Returned list of gateways is same as what the selector was initialized with"() {
        def gws = gateways.collect { gatewayFrom(it) }
        given: 'a gateway-selector'
        def selector = new StaticGatewaySelector(null, 0, gws)

        when: 'the selector is asked to fetch gateways'
        def fetchedGateways = selector.fetchGateways()

        then: 'they are as expected'
        fetchedGateways == gws

        where:
        gateways << [
                [],
                ["http://localhost"],
                ["http://localhost:1", "http://localhost:2"],
                ["http://localhost:1", "http://localhost:2", "http://localhost:3"]
        ]
    }
}
