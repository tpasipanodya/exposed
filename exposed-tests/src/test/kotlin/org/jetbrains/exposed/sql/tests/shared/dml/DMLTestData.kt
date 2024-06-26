package org.jetbrains.exposed.sql.tests.shared.dml

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.tests.DatabaseTestsBase
import org.jetbrains.exposed.sql.tests.TestDB
import java.math.BigDecimal
import java.util.*
import org.jetbrains.exposed.sql.tests.shared.assertEquals
import org.jetbrains.exposed.sql.tests.shared.assertFalse
import org.jetbrains.exposed.sql.tests.shared.assertTrue


object Cities : Table() {
    val id: Column<Int> = integer("cityId").autoIncrement()
    val name: Column<String> = varchar("name", 50)
    override val primaryKey = PrimaryKey(id)
}

object Users : Table() {
    val id: Column<String> = varchar("id", 10)
    val name: Column<String> = varchar("name", length = 50)
    val cityId: Column<Int?> = reference("city_id", Cities.id).nullable()
    val flags: Column<Int> = integer("flags").default(0)
    override val primaryKey = PrimaryKey(id)

    object Flags {
        const val IS_ADMIN = 0b1
        const val HAS_DATA = 0b1000
    }
}

fun munichId() = Cities
    .select { Cities.name eq "Munich" }
    .first() get Cities.id

object ScopedUsers : Table("scoped_users") {
    val id: Column<String> = varchar("id", 10)
    val name: Column<String> = varchar("name", length = 50)
    val cityId: Column<Int?> = reference("city_id", Cities.id).nullable()
    val flags: Column<Int> = integer("flags").default(0)
    override val primaryKey = PrimaryKey(id)
    override val defaultFilter = { cityId eq munichId() }

    object Flags {
        const val IS_ADMIN = 0b1
        const val HAS_DATA = 0b1000
    }
}

object UserData : Table() {
    val user_id: Column<String> = reference("user_id", Users.id)
    val comment: Column<String> = varchar("comment", 30)
    val value: Column<Int> = integer("value")
}

object ScopedUserData : Table(name = "scoped_user_data") {
    val userId: Column<String> = reference("user_id", ScopedUsers.id)
    val comment: Column<String> = varchar("comment", 30)
    val value: Column<Int> = integer("value")
    override val defaultFilter = { userId eq "sergey" }
}

object Sales : Table() {
    val year: Column<Int> = integer("year")
    val month: Column<Int> = integer("month")
    val product: Column<String?> = varchar("product", 30).nullable()
    val amount: Column<BigDecimal> = decimal("amount", 8, 2)
}

class DmlTestRuntime(val transaction: Transaction,
                     val cities: Cities,
                     val users: Users,
                     val userData: UserData,
                     val scopedUsers: ScopedUsers,
                     val scopedUserData: ScopedUserData){
    fun assertTrue(actual: Boolean) = transaction.assertTrue(actual)
    fun assertFalse(actual: Boolean) = transaction.assertFalse(actual)
    fun <T> assertEquals(exp: T, act: T) = transaction.assertEquals(exp, act)
    fun <T> assertEquals(exp: T, act: List<T>) = transaction.assertEquals(exp, act)
}


@Suppress("LongMethod")
fun DatabaseTestsBase.withCitiesAndUsers(exclude: List<TestDB> = emptyList(),
                                         statement: DmlTestRuntime.() -> Unit) {
    withTables(exclude, Cities, Users, UserData, ScopedUsers, ScopedUserData) {
        val saintPetersburgId = Cities.insert {
            it[name] = "St. Petersburg"
        } get Cities.id

        val munichId = Cities.insert {
            it[name] = "Munich"
        } get Cities.id

        Cities.insert {
            it[name] = "Prague"
        }

        Users.insert {
            it[id] = "andrey"
            it[name] = "Andrey"
            it[cityId] = saintPetersburgId
            it[flags] = Users.Flags.IS_ADMIN
        }

        ScopedUsers.insert {
            it[id] = "andrey"
            it[name] = "Andrey"
            it[cityId] = saintPetersburgId
            it[flags] = ScopedUsers.Flags.IS_ADMIN
        }

        Users.insert {
            it[id] = "sergey"
            it[name] = "Sergey"
            it[cityId] = munichId
            it[flags] = Users.Flags.IS_ADMIN or Users.Flags.HAS_DATA
        }

        ScopedUsers.insert {
            it[id] = "sergey"
            it[name] = "Sergey"
            it[cityId] = munichId
            it[flags] = ScopedUsers.Flags.IS_ADMIN or ScopedUsers.Flags.HAS_DATA
        }

        Users.insert {
            it[id] = "eugene"
            it[name] = "Eugene"
            it[cityId] = munichId
            it[flags] = Users.Flags.HAS_DATA
        }

        ScopedUsers.insert {
            it[id] = "eugene"
            it[name] = "Eugene"
            it[cityId] = munichId
            it[flags] = ScopedUsers.Flags.HAS_DATA
        }

        Users.insert {
            it[id] = "alex"
            it[name] = "Alex"
            it[cityId] = null
        }

        ScopedUsers.insert {
            it[id] = "alex"
            it[name] = "Alex"
            it[cityId] = null
        }

        Users.insert {
            it[id] = "smth"
            it[name] = "Something"
            it[cityId] = null
            it[flags] = Users.Flags.HAS_DATA
        }

        ScopedUsers.insert {
            it[id] = "smth"
            it[name] = "Something"
            it[cityId] = null
            it[flags] = ScopedUsers.Flags.HAS_DATA
        }

        UserData.insert {
            it[user_id] = "smth"
            it[comment] = "Something is here"
            it[value] = 10
        }

        ScopedUserData.insert {
            it[userId] = "smth"
            it[comment] = "Something is here"
            it[value] = 10
        }

        UserData.insert {
            it[user_id] = "smth"
            it[comment] = "Comment #2"
            it[value] = 20
        }

        ScopedUserData.insert {
            it[userId] = "smth"
            it[comment] =  "Comment #2"
            it[value] = 20
        }

        UserData.insert {
            it[user_id] = "eugene"
            it[comment] = "Comment for Eugene"
            it[value] = 20
        }

        ScopedUserData.insert {
            it[userId] = "eugene"
            it[comment] =  "Comment for Eugene"
            it[value] = 20
        }

        UserData.insert {
            it[user_id] = "sergey"
            it[comment] = "Comment for Sergey"
            it[value] = 30
        }

        ScopedUserData.insert {
            it[userId] = "sergey"
            it[comment] =  "Comment for Sergey"
            it[value] = 30
        }

        DmlTestRuntime(
            this,
            Cities,
            Users,
            UserData,
            ScopedUsers,
            ScopedUserData
        ).apply(statement)
    }
}

fun DatabaseTestsBase.withSales(
    statement: Transaction.(testDb: TestDB, sales: Sales) -> Unit
) {
    withTables(Sales) {
        insertSale(2018, 11, "tea", "550.10")
        insertSale(2018, 12, "coffee", "1500.25")
        insertSale(2018, 12, "tea", "900.30")
        insertSale(2019, 1, "coffee", "1620.10")
        insertSale(2019, 1, "tea", "650.70")
        insertSale(2019, 2, "coffee", "1870.90")
        insertSale(2019, 2, null, "10.20")

        statement(it, Sales)
    }
}

private fun insertSale(year: Int, month: Int, product: String?, amount: String) {
    Sales.insert {
        it[Sales.year] = year
        it[Sales.month] = month
        it[Sales.product] = product
        it[Sales.amount] = BigDecimal(amount)
    }
}

object OrgMemberships : IntIdTable() {
    val orgId = reference("org", Orgs.uid)
}

class OrgMembership(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<OrgMembership>(OrgMemberships)

    val orgId by OrgMemberships.orgId
    var org by Org referencedOn OrgMemberships.orgId
}

object Orgs : IntIdTable() {
    val uid = varchar("uid", 36).uniqueIndex().clientDefault { UUID.randomUUID().toString() }
    val name = varchar("name", 256)
}

class Org(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Org>(Orgs)

    var uid by Orgs.uid
    var name by Orgs.name
}

internal fun Iterable<ResultRow>.toCityNameList(): List<String> = map { it[Cities.name] }
