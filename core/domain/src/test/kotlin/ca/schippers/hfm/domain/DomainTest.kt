// File path and name: core/domain/src/test/kotlin/ca/schippers/hfm/domain/DomainTest.kt
// Modified On Timestamp: 2026-10-26 @ 03:06 EDT
// Created On Timestamp: 2026-10-01 @ 15:53 EDT
// File Description: Tests of the domain module: record ids, access policy and the other shared types.
// Uses: core/domain, kotlin.test.
// Used By: ./gradlew :core:domain:test.
// Purpose: Guard the rules every other module relies on.
package ca.schippers.hfm.domain

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdsTest {

    @Test
    fun `ids are version 7, unique and time ordered`() {
        val ids = List(10_000) { Ids.newUuid() }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.version() == 7 && it.variant() == 2 })
        assertEquals(ids.map { it.toString() }, ids.map { it.toString() }.sorted())
    }

    @Test
    fun `ids stay ordered when the clock steps back`() {
        val a = Ids.newUuid(nowMillis = 2_000_000_000_000)
        val b = Ids.newUuid(nowMillis = 1_000_000_000_000)
        assertTrue(a.toString() < b.toString())
    }

    @Test
    fun `name-based ids are version 5 and match the RFC 9562 example`() {
        // RFC 9562 appendix A.4: the DNS namespace and "www.example.com".
        assertEquals("2ed6657d-e927-568b-95e1-2665a8aea6a2", Ids.nameBased("6ba7b810-9dad-11d1-80b4-00c04fd430c8", "www.example.com"))
        val household = Ids.newId()
        val id = Ids.nameBased(household, "food.groceries")
        assertEquals(id, Ids.nameBased(household, "food.groceries"))
        assertTrue(Ids.isValid(id))
        assertEquals(5, UUID.fromString(id).version())
        assertEquals(2, UUID.fromString(id).variant())
        assertFalse(id == Ids.nameBased(Ids.newId(), "food.groceries"))
        assertFalse(Ids.nameBased(household, "a", "bc") == Ids.nameBased(household, "ab", "c"))
    }

    @Test
    fun `validation`() {
        assertTrue(Ids.isValid(Ids.newId()))
        assertFalse(Ids.isValid("not-an-id"))
        assertTrue(Ids.isValid(UUID.randomUUID().toString()))
    }
}

class AccessPolicyTest {

    private val admin = "admin"
    private val spouse = "spouse"
    private val teen = "teen"

    private val shared = AccountGroupAccess("shared", ownerUserId = null, grants = mapOf(spouse to PermissionLevel.EDIT, teen to PermissionLevel.VIEW))
    private val spousePrivate = AccountGroupAccess("spouse-private", ownerUserId = spouse, grants = emptyMap())

    @Test
    fun `administrator edits shared groups`() {
        assertEquals(PermissionLevel.EDIT, AccessPolicy.levelFor(admin, Role.ADMINISTRATOR, shared))
    }

    @Test
    fun `members get what they are granted`() {
        assertEquals(PermissionLevel.EDIT, AccessPolicy.levelFor(spouse, Role.MEMBER, shared))
        assertEquals(PermissionLevel.VIEW, AccessPolicy.levelFor(teen, Role.MEMBER, shared))
        assertEquals(PermissionLevel.NONE, AccessPolicy.levelFor("stranger", Role.MEMBER, shared))
    }

    @Test
    fun `private groups are private even from the administrator`() {
        assertEquals(PermissionLevel.EDIT, AccessPolicy.levelFor(spouse, Role.MEMBER, spousePrivate))
        assertEquals(PermissionLevel.NONE, AccessPolicy.levelFor(admin, Role.ADMINISTRATOR, spousePrivate))
        val sharedWithAdmin = spousePrivate.copy(grants = mapOf(admin to PermissionLevel.VIEW))
        assertEquals(PermissionLevel.VIEW, AccessPolicy.levelFor(admin, Role.ADMINISTRATOR, sharedWithAdmin))
    }

    @Test
    fun `viewers never edit`() {
        val group = shared.copy(grants = mapOf(teen to PermissionLevel.EDIT))
        assertEquals(PermissionLevel.VIEW, AccessPolicy.levelFor(teen, Role.VIEWER, group))
        assertFalse(AccessPolicy.can(teen, Role.VIEWER, group, PermissionLevel.CAPTURE_ONLY))
    }
}
