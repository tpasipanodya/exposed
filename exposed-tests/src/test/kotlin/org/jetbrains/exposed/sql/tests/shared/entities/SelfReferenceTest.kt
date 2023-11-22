package org.jetbrains.exposed.sql.tests.shared.entities

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.tests.shared.assertEqualLists
import org.jetbrains.exposed.sql.tests.shared.dml.Cities
import org.jetbrains.exposed.sql.tests.shared.dml.UserData
import org.jetbrains.exposed.sql.tests.shared.dml.Users
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelfReferenceTest {

    @Test
    fun simpleTest() {
        assertEqualLists(listOf(Cities), SchemaUtils.sortTablesByReferences(listOf(Cities)))
        assertEqualLists(listOf(Cities, Users), SchemaUtils.sortTablesByReferences(listOf(Users)))

        val rightOrder = listOf(Cities, Users, UserData)
        val r1 = SchemaUtils.sortTablesByReferences(listOf(Cities, UserData, Users))
        val r2 = SchemaUtils.sortTablesByReferences(listOf(UserData, Cities, Users))
        val r3 = SchemaUtils.sortTablesByReferences(listOf(Users, Cities, UserData))
        assertEqualLists(rightOrder, r1)
        assertEqualLists(rightOrder, r2)
        assertEqualLists(rightOrder, r3)
    }

    object TestTables {
        object cities : Table() {
            val id = integer("id").autoIncrement()
            val name = varchar("name", 50)
            val strange_id = varchar("strange_id", 10).references(strangeTable.id)

            override val primaryKey = PrimaryKey(id)
        }

        object users : Table() {
            val id = varchar("id", 10)
            val name = varchar("name", length = 50)
            val cityId = (integer("city_id") references cities.id).nullable()

            override val primaryKey = PrimaryKey(id)
        }

        object noRefereeTable : Table() {
            val id = varchar("id", 10)
            val col1 = varchar("col1", 10)

            override val primaryKey = PrimaryKey(id)
        }

        object refereeTable : Table() {
            val id = varchar("id", 10)
            val ref = reference("ref", noRefereeTable.id)

            override val primaryKey = PrimaryKey(id)
        }

        object referencedTable : IntIdTable() {
            val col3 = varchar("col3", 10)
        }

        object strangeTable : Table() {
            val id = varchar("id", 10)
            val user_id = varchar("user_id", 10) references users.id
            val comment = varchar("comment", 30)
            val value = integer("value")

            override val primaryKey = PrimaryKey(id)
        }
    }

    @Test
    fun cycleReferencesCheckTest() {
        val original = listOf(
            TestTables.cities,
            TestTables.users,
            TestTables.strangeTable,
            TestTables.noRefereeTable,
            TestTables.refereeTable,
            TestTables.referencedTable
        )
        val sortedTables = SchemaUtils.sortTablesByReferences(original)
        val expected = listOf(
            TestTables.users,
            TestTables.strangeTable,
            TestTables.cities,
            TestTables.noRefereeTable,
            TestTables.refereeTable,
            TestTables.referencedTable
        )

        assertEqualLists(expected, sortedTables)
    }

    @Test
    fun testHasCycle() {
        assertFalse(SchemaUtils.checkCycle(TestTables.referencedTable))
        assertFalse(SchemaUtils.checkCycle(TestTables.refereeTable))
        assertFalse(SchemaUtils.checkCycle(TestTables.noRefereeTable))
        assertTrue(SchemaUtils.checkCycle(TestTables.users))
        assertTrue(SchemaUtils.checkCycle(TestTables.cities))
        assertTrue(SchemaUtils.checkCycle(TestTables.strangeTable))
    }
}
