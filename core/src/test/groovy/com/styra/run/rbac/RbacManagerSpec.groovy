package com.styra.run.rbac

import com.fasterxml.jackson.jr.ob.JSON
import com.styra.run.ApiClient
import com.styra.run.StyraRun
import com.styra.run.session.TenantSession
import spock.lang.Specification
import spock.lang.Unroll

import static com.styra.run.ApiClient.Method.DELETE
import static com.styra.run.ApiClient.Method.GET
import static com.styra.run.ApiClient.Method.POST
import static com.styra.run.ApiClient.Method.PUT
import static com.styra.run.test.apiClients.mockApiClient
import static com.styra.run.test.apiClients.reply
import static com.styra.run.test.apiClients.response

class RbacManagerSpec extends Specification {
    @Unroll
    def "RBAC-Manager can fetch roles"() {
        def baseUri = 'https://example.com/my/proj/env'
        def authzInput = new TenantSession('alice', 'AcmeCorp')

        given: 'a StyraRun instance that expects a fixed set of requests'
        def apiClient = mockApiClient([
                // authz
                reply(POST, "$baseUri/data/rbac/manage/allow") { headers, body ->
                    def deserializedBody = JSON.std.mapFrom(body)
                    assert deserializedBody.input == authzInput.value
                    return response(200, true)
                },
                // roles fetch
                reply(POST, "$baseUri/data/rbac/roles") { headers, body ->
                    return response(200, roles)
                }
        ])
        def styraRun = makeStyraRun(apiClient, baseUri)

        and: 'an RBAC-Manager'
        def rbacManager = new RbacManager(styraRun)

        when: 'roles are requested'
        def fetchedRoles = rbacManager.getRoles(authzInput).get()

        then: 'they are as expected'
        fetchedRoles == roles

        and: 'the expected calls to the Styra Run API were made'
        apiClient.assertExhausted()

        where:
        roles << [
                [],
                ['READER', 'WRITER']
        ]
    }

    @Unroll
    def "RBAC-Manager can get a user-binding"() {
        def user = new User(userId)
        def roleList = roles.collect { new Role(it as String) }
        def expectedBinding = new UserBinding(user, roleList)

        def baseUri = 'https://example.com/my/proj/env'
        def authzInput = new TenantSession('alice', tenant)

        given: 'a StyraRun instance that expects a fixed set of requests'
        def apiClient = mockApiClient([
                // authz
                reply(POST, "$baseUri/data/rbac/manage/allow") { headers, body ->
                    def deserializedBody = JSON.std.mapFrom(body)
                    assert deserializedBody.input == authzInput.value
                    return response(200, true)
                },
                // user-binding get
                reply(GET, "$baseUri/data/rbac/user_bindings/$tenant/$userId") { headers, body ->
                    return response(200, roles)
                }
        ])
        def styraRun = makeStyraRun(apiClient, baseUri)

        and: 'an RBAC-Manager'
        def rbacManager = new RbacManager(styraRun)

        when: 'binding is fetched'
        def fetchedBinding = rbacManager.getUserBinding(user, authzInput).get()

        then: 'it is as expected'
        fetchedBinding == expectedBinding

        and: 'the expected calls to the Styra Run API were made'
        apiClient.assertExhausted()

        where:
        tenant     | userId  | roles
        'AcmeCorp' | 'alice' | []
        'AcmeCorp' | 'alice' | ['foo']
        'AcmeCorp' | 'alice' | ['READER', 'WRITER']
        'LexCorp'  | 'bob'   | ['READER', 'WRITER']
    }

    @Unroll
    def "RBAC-Manager can set a user-binding"() {
        def user = new User(userId)
        def roleList = roles.collect { new Role(it as String) }

        def baseUri = 'https://example.com/my/proj/env'
        def authzInput = new TenantSession('alice', tenant)

        given: 'a StyraRun instance that expects a fixed set of requests'
        def apiClient = mockApiClient([
                // authz
                reply(POST, "$baseUri/data/rbac/manage/allow") { headers, body ->
                    def deserializedBody = JSON.std.mapFrom(body)
                    assert deserializedBody.input == authzInput.value
                    return response(200, true)
                },
                // user-binding set
                reply(PUT, "$baseUri/data/rbac/user_bindings/$tenant/$userId") { headers, body ->
                    def deserializedBody = JSON.std.listFrom(body)
                    assert deserializedBody == roles
                    return response(200, '{}')
                }
        ])
        def styraRun = makeStyraRun(apiClient, baseUri)

        and: 'an RBAC-Manager'
        def rbacManager = new RbacManager(styraRun)

        when: 'binding is set'
        rbacManager.putUserBinding(new UserBinding(user, roleList), authzInput).get()

        then: 'the expected calls to the Styra Run API were made'
        apiClient.assertExhausted()

        where:
        tenant     | userId  | roles
        'AcmeCorp' | 'alice' | []
        'AcmeCorp' | 'alice' | ['foo']
        'AcmeCorp' | 'alice' | ['READER', 'WRITER']
        'LexCorp'  | 'bob'   | ['READER', 'WRITER']
    }

    @Unroll
    def "RBAC-Manager can delete a user-binding"() {
        def user = new User(userId)

        def baseUri = 'https://example.com/my/proj/env'
        def authzInput = new TenantSession('alice', tenant)

        given: 'a StyraRun instance that expects a fixed set of requests'
        def apiClient = mockApiClient([
                // authz
                reply(POST, "$baseUri/data/rbac/manage/allow") { headers, body ->
                    def deserializedBody = JSON.std.mapFrom(body)
                    assert deserializedBody.input == authzInput.value
                    return response(200, true)
                },
                // user-binding delete
                reply(DELETE, "$baseUri/data/rbac/user_bindings/$tenant/$userId") { headers, body ->
                    return response(200, roles)
                }
        ])
        def styraRun = makeStyraRun(apiClient, baseUri)

        and: 'an RBAC-Manager'
        def rbacManager = new RbacManager(styraRun)

        when: 'binding is deleted'
        rbacManager.deleteUserBinding(user, authzInput).get()

        then: 'the expected calls to the Styra Run API were made'
        apiClient.assertExhausted()

        where:
        tenant     | userId  | roles
        'AcmeCorp' | 'alice' | []
        'AcmeCorp' | 'alice' | ['foo']
        'AcmeCorp' | 'alice' | ['READER', 'WRITER']
        'LexCorp'  | 'bob'   | ['READER', 'WRITER']
    }

    @Unroll
    def "RBAC-Manager can list all user-bindings"() {
        def baseUri = 'https://example.com/my/proj/env'
        def authzInput = new TenantSession('alice', tenant)

        given: 'a StyraRun instance that expects a fixed set of requests'
        def apiClient = mockApiClient([
                // authz
                reply(POST, "$baseUri/data/rbac/manage/allow") { headers, body ->
                    def deserializedBody = JSON.std.mapFrom(body)
                    assert deserializedBody.input == authzInput.value
                    return response(200, true)
                },
                // user-binding get
                reply(GET, "$baseUri/data/rbac/user_bindings/$tenant") { headers, body ->
                    return response(200, bindings)
                }
        ])
        def styraRun = makeStyraRun(apiClient, baseUri)

        and: 'an RBAC-Manager'
        def rbacManager = new RbacManager(styraRun)

        when: 'bindings are requested'
        def fetchedBindings = rbacManager.listUserBindings(authzInput).get()

        then: 'they are as expected'
        fetchedBindings.size() == bindings.size()
        fetchedBindings.forEach { binding ->
            assert bindings.containsKey(binding.user.id)
            assert bindings[binding.user.id] == binding.roles.collect { it.name }
        }

        and: 'the expected calls to the Styra Run API were made'
        apiClient.assertExhausted()

        where:
        tenant     | bindings
        'AcmeCorp' | [:]
        'AcmeCorp' | ['alice': ['foo']]
        'AcmeCorp' | ['alice': ['READER', 'WRITER']]
        'LexCorp'  | ['alice': ['READER', 'WRITER'], 'bob': []]
        'LexCorp'  | ['alice': ['READER', 'WRITER'], 'bob': ['foo']]
    }

    @Unroll
    def "RBAC-Manager can get user-bindings for a list of users"() {
        def baseUri = 'https://example.com/my/proj/env'
        def authzInput = new TenantSession('alice', tenant)
        def userList = users.collect { new User(it) }

        given: 'a StyraRun instance that expects a fixed set of requests'
        def expectedRequests = [
                // authz
                reply(POST, "$baseUri/data/rbac/manage/allow") { headers, body ->
                    def deserializedBody = JSON.std.mapFrom(body)
                    assert deserializedBody.input == authzInput.value
                    return response(200, true)
                }
        ]
        // All requests in this test are made synchronously, so we can rely on these request handlers to execute in order
        users.forEach { userId ->
            expectedRequests << reply(GET, "$baseUri/data/rbac/user_bindings/$tenant/$userId") { headers, body ->
                return response(200, bindings[userId])
            }
        }
        def apiClient = mockApiClient(expectedRequests)
        def styraRun = makeStyraRun(apiClient, baseUri)

        and: 'an RBAC-Manager'
        def rbacManager = new RbacManager(styraRun)

        when: 'bindings are requested'
        def fetchedBindings = rbacManager.getUserBindings(userList, authzInput).get()

        then: 'they are as expected'
        fetchedBindings.size() == users.size()
        fetchedBindings.forEach { binding ->
            if (bindings.containsKey(binding.user.id)) {
                assert bindings[binding.user.id] == binding.roles.collect { it.name }
            }
        }

        and: 'the expected calls to the Styra Run API were made'
        apiClient.assertExhausted()

        where:
        tenant     | users                       | bindings
        'AcmeCorp' | []                          | [:]
        'AcmeCorp' | ['alice']                   | ['alice': []]
        'AcmeCorp' | ['alice']                   | ['alice': ['foo']]
        'LexCorp'  | ['alice', 'bob']            | ['alice': ['READER', 'WRITER'], 'bob': []]
        'LexCorp'  | ['alice', 'bob']            | ['alice': ['READER', 'WRITER'], 'bob': ['foo'], 'charles': ['bar']]
        'LexCorp'  | ['alice', 'bob', 'charles'] | ['alice': ['READER', 'WRITER'], 'bob': ['foo'], 'charles': ['bar']]
        'LexCorp'  | ['alice', 'bob', 'charles'] | ['alice': ['READER', 'WRITER'], 'charles': ['bar']]
    }

    static StyraRun makeStyraRun(ApiClient client, String baseUri, token = 'foobar') {
        // We initialize the builder with a static list of gateways to not dynamically fetch gateways
        return StyraRun.builder([baseUri], token)
                .apiClientFactory({ _ -> client })
                .build()
    }
}
