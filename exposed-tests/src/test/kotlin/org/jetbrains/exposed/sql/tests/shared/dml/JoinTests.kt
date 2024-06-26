package org.jetbrains.exposed.sql.tests.shared.dml

import nl.altindag.log.LogCaptor
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.tests.DatabaseTestsBase
import org.jetbrains.exposed.sql.tests.shared.assertEquals
import org.jetbrains.exposed.sql.tests.shared.assertTrue
import org.jetbrains.exposed.sql.tests.shared.expectException
import org.junit.Test

class JoinTests : DatabaseTestsBase() {
    // manual join
    @Test
    fun testJoin01() {
        withCitiesAndUsers {
            (users innerJoin cities)
                .slice(users.name, cities.name)
                .select {
                    (users.id.eq("andrey") or
                        users.name.eq("Sergey")) and
                        users.cityId.eq(cities.id)
                }.forEach {
                    val userName = it[users.name]
                    val cityName = it[cities.name]
                    when (userName) {
                        "Andrey" -> assertEquals("St. Petersburg", cityName)
                        "Sergey" -> assertEquals("Munich", cityName)
                        else -> error("Unexpected user $userName")
                    }
                }

            // Joining to a table that doesn't have a default scope
            (scopedUsers innerJoin cities)
                .slice(scopedUsers.name, cities.name)
                .select {
                    (scopedUsers.id.eq("andrey") or
                        scopedUsers.name.eq("Sergey")) and
                        scopedUsers.cityId.eq(cities.id)
                }.let {
                    it.forEach { r ->
                        val userName = r[scopedUsers.name]
                        val cityName = r[cities.name]
                        when (userName) {
                            "Sergey" -> assertEquals("Munich", cityName)
                            else -> error("Unexpected user $userName")
                        }
                    }
                }

            // Joining to a table that doesn't have a default scope after
            // striping the left table's default scope
            (scopedUsers.stripDefaultFilter() innerJoin cities)
                .slice(scopedUsers.name, cities.name)
                .select {
                    (scopedUsers.id.eq("andrey") or
                        scopedUsers.name.eq("Sergey")) and
                        scopedUsers.cityId.eq(cities.id)
                }.let {
                    it.forEach { r ->
                        val userName = r[scopedUsers.name]
                        val cityName = r[cities.name]
                        when (userName) {
                            "Andrey" -> assertEquals("St. Petersburg", cityName)
                            "Sergey" -> assertEquals("Munich", cityName)
                            else -> error("Unexpected user $userName")
                        }
                    }
                }

            // Joining to a table that has a default scope. Some
            // records are filtered out by the right table's scope
            (scopedUsers innerJoin scopedUserData)
                .slice(scopedUsers.name, scopedUserData.comment)
                .select {
                    (scopedUsers.id.eq("eugene") or
                        scopedUsers.name.eq("Sergey")) and
                        scopedUsers.id.eq(scopedUserData.userId)
                }.let {
                    it.forEach { r ->
                        val userName = r[scopedUsers.name]
                        val comment = r[scopedUserData.comment]
                        when (userName) {
                            "Sergey" -> assertEquals("Comment for Sergey", comment)
                            else -> error("Unexpected user $userName")
                        }
                    }
                }

            // Joining to a table that has a default scope. Some
            // records are filtered out by the right table's scope
            (scopedUsers innerJoin scopedUserData.stripDefaultFilter())
                .slice(scopedUsers.name, scopedUserData.comment)
                .select {
                    (scopedUsers.id.eq("eugene") or
                        scopedUsers.name.eq("Sergey")) and
                        scopedUsers.id.eq(scopedUserData.userId)
                }.let {
                    it.forEach { r ->
                        val userName = r[scopedUsers.name]
                        val comment = r[scopedUserData.comment]
                        when (userName) {
                            "Eugene" -> assertEquals("Comment for Eugene", comment)
                            "Sergey" -> assertEquals("Comment for Sergey", comment)
                            else -> error("Unexpected user $userName")
                        }
                    }
                }

            // Joining 2 tables that have default scopes but the scopes are stripped.
            (scopedUsers.stripDefaultFilter() innerJoin scopedUserData.stripDefaultFilter())
                .slice(scopedUsers.name, scopedUserData.comment)
                .select { scopedUsers.id.isNotNull() }
                .let { assertEquals(it.count(), 4) }
        }
    }

    // join with foreign key
    @Test
    fun testJoin02() {
        withCitiesAndUsers {
            val stPetersburgUser =
                (users innerJoin cities)
                    .slice(users.name, users.cityId, cities.name)
                    .select {
                        cities.name.eq("St. Petersburg") or users.cityId.isNull()
                    }.single()
            assertEquals("Andrey", stPetersburgUser[users.name])
            assertEquals("St. Petersburg", stPetersburgUser[cities.name])

            // Joining to a table that has a default scope. Some
            // records are filtered out by the both table's scopes
            (scopedUsers innerJoin scopedUserData)
                .slice(scopedUsers.name, scopedUserData.comment)
                .select {
                    (scopedUsers.id.eq("andrey") or
                        scopedUsers.name.eq("Sergey"))
                }.let {
                    it.forEach { r ->
                        val userName = r[scopedUsers.name]
                        val comment = r[scopedUserData.comment]
                        when (userName) {
                            "Sergey" -> assertEquals("Comment for Sergey", comment)
                            else -> error("Unexpected user $userName")
                        }
                    }
                }

            // Striping the right default scope for the left table alone
            // won't lead to more records being returned because of the
            // right's filter.
            (scopedUsers.stripDefaultFilter() innerJoin scopedUserData)
                .slice(scopedUsers.name, scopedUserData.comment)
                .select {
                    (scopedUsers.id.eq("andrey") or
                        scopedUsers.name.eq("Sergey"))
                }.let {
                    it.forEach { r ->
                        val userName = r[scopedUsers.name]
                        val comment = r[scopedUserData.comment]
                        when (userName) {
                            "Sergey" -> assertEquals("Comment for Sergey", comment)
                            else -> error("Unexpected user $userName")
                        }
                    }
                }

            // Stripping the right table's scope too then changes the result set
            (scopedUsers.stripDefaultFilter() innerJoin scopedUserData.stripDefaultFilter())
                .slice(scopedUsers.name, scopedUserData.comment)
                .select {
                    (scopedUsers.id.eq("andrey") or
                        scopedUsers.name.eq("Sergey"))
                }.let {
                    it.forEach { r ->
                        val userName = r[scopedUsers.name]
                        val comment = r[scopedUserData.comment]
                        when (userName) {
                            "Sergey" -> assertEquals("Comment for Sergey", comment)
                            "Andrey" -> assertEquals("Comment for Andrey", comment)
                            else -> error("Unexpected user $userName")
                        }
                    }
                }

            // Joining to a table that has a default scope. Some
            // records are filtered out by the right table's scope
            (scopedUsers innerJoin scopedUserData)
                .slice(scopedUsers.name, scopedUserData.comment)
                .select {
                    (scopedUsers.id.eq("eugene") or
                        scopedUsers.name.eq("Sergey"))
                }.let {
                    it.forEach { r ->
                        val userName = r[scopedUsers.name]
                        val comment = r[scopedUserData.comment]
                        when (userName) {
                            "Sergey" -> assertEquals("Comment for Sergey", comment)
                            else -> error("Unexpected user $userName")
                        }
                    }
                }
        }
    }

    // triple join
    @Test
    fun testJoin03() {
        withCitiesAndUsers {
            (cities innerJoin users innerJoin userData)
                .selectAll().orderBy(users.id)
                .toList().let { r ->
                    assertEquals(2, r.size)
                    assertEquals("Eugene", r[0][users.name])
                    assertEquals("Comment for Eugene", r[0][userData.comment])
                    assertEquals("Munich", r[0][cities.name])
                    assertEquals("Sergey", r[1][users.name])
                    assertEquals("Comment for Sergey", r[1][userData.comment])
                    assertEquals("Munich", r[1][cities.name])
                }

            // Only Sergey is in scope
            (cities innerJoin scopedUsers innerJoin scopedUserData)
                .selectAll().orderBy(scopedUsers.id)
                .toList().let { r ->
                    assertEquals(1, r.size)
                    assertEquals("Sergey", r[0][scopedUsers.name])
                    assertEquals("Comment for Sergey", r[0][scopedUserData.comment])
                    assertEquals("Munich", r[0][cities.name])
                }

            // Sergey and Eugene are in scope
            (cities innerJoin scopedUsers innerJoin scopedUserData.stripDefaultFilter())
                .selectAll().orderBy(scopedUsers.id, SortOrder.DESC)
                .toList().let { r ->
                    assertEquals(2, r.size)
                    assertEquals("Sergey", r[0][scopedUsers.name])
                    assertEquals("Comment for Sergey", r[0][scopedUserData.comment])
                    assertEquals("Munich", r[0][cities.name])
                    assertEquals("Eugene", r[1][scopedUsers.name])
                    assertEquals("Comment for Eugene", r[1][scopedUserData.comment])
                    assertEquals("Munich", r[1][cities.name])
                }

            // Only Sergey is in scope
            (cities innerJoin scopedUsers.stripDefaultFilter() innerJoin scopedUserData)
                .selectAll().orderBy(scopedUsers.id, SortOrder.DESC)
                .toList().let { r ->
                    assertEquals(1, r.size)
                    assertEquals("Sergey", r[0][scopedUsers.name])
                    assertEquals("Comment for Sergey", r[0][scopedUserData.comment])
                    assertEquals("Munich", r[0][cities.name])
                }

            // Only 2 users have city ids and comments
            (cities innerJoin scopedUsers.stripDefaultFilter() innerJoin scopedUserData.stripDefaultFilter())
                .selectAll().orderBy(scopedUsers.id, SortOrder.ASC)
                .toList().let { r ->
                    assertEquals(2, r.size)
                    assertEquals("Eugene", r[0][scopedUsers.name])
                    assertEquals("Comment for Eugene", r[0][scopedUserData.comment])
                    assertEquals("Munich", r[0][cities.name])
                    assertEquals("Sergey", r[1][scopedUsers.name])
                    assertEquals("Comment for Sergey", r[1][scopedUserData.comment])
                    assertEquals("Munich", r[1][cities.name])
                }
        }
    }

    // triple join
    @Test
    fun testJoin04() {
        val numbers = object : Table() {
            val id = integer("id")

            override val primaryKey = PrimaryKey(id)
        }

        val names = object : Table() {
            val name = varchar("name", 10)

            override val primaryKey = PrimaryKey(name)
        }

        val map = object : Table() {
            val id_ref = integer("id_ref") references numbers.id
            val name_ref = varchar("name_ref", 10) references names.name
        }

        withTables(numbers, names, map) {
            numbers.insert { it[id] = 1 }
            numbers.insert { it[id] = 2 }
            names.insert { it[name] = "Foo" }
            names.insert { it[name] = "Bar" }
            map.insert {
                it[id_ref] = 2
                it[name_ref] = "Foo"
            }

            val r = (numbers innerJoin map innerJoin names).selectAll().toList()
            assertEquals(1, r.size)
            assertEquals(2, r[0][numbers.id])
            assertEquals("Foo", r[0][names.name])
        }
    }

    // cross join
    @Test
    fun testJoin05() {
        withCitiesAndUsers {
            (users crossJoin cities)
                .slice(users.name, users.cityId, cities.name)
                .select { cities.name.eq("St. Petersburg") }
                .map { it[users.name] to it[cities.name] }
                .let { allUsersToStPetersburg ->
                    assertTrue(allUsersToStPetersburg.all { it.second == "St. Petersburg" })
                    assertEquals(setOf("Andrey", "Sergey", "Eugene", "Alex", "Something"),
                                 allUsersToStPetersburg.map { it.first }.toSet())
                }

            // Only Eugene and Sergey are in scope
            (scopedUsers crossJoin cities)
                .slice(scopedUsers.name, scopedUsers.cityId, cities.name)
                .select { cities.name.eq("Munich") }
                .map { it[scopedUsers.name] to it[cities.name] }
                .let { allUsersToMunich ->
                    assertTrue(allUsersToMunich.all { it.second == "Munich" })
                    assertEquals(setOf("Sergey", "Eugene"),
                                 allUsersToMunich.map { it.first }.toSet())
                }

            // All users are now in scope
            (scopedUsers.stripDefaultFilter() crossJoin cities)
                .slice(scopedUsers.name, scopedUsers.cityId, cities.name)
                .select { cities.name.eq("Munich") }
                .map { it[scopedUsers.name] to it[cities.name] }
                .let { allUsersToMunich ->
                    assertTrue(allUsersToMunich.all { it.second == "Munich" })
                    assertEquals(setOf("Andrey", "Sergey", "Eugene", "Alex", "Something"),
                                 allUsersToMunich.map { it.first }.toSet())
                }
        }
    }

    @Test
    fun testMultipleReferenceJoin01() {
        val foo = object : IntIdTable("foo") {
            val baz = integer("baz").uniqueIndex()
        }
        val bar = object : IntIdTable("bar") {
            val foo = reference("foo", foo)
            val baz = integer("baz") references foo.baz
        }
        withTables(foo, bar) {
            val fooId = foo.insertAndGetId {
                it[baz] = 5
            }

            bar.insert {
                it[this.foo] = fooId
                it[baz] = 5
            }

            val result = foo.innerJoin(bar).selectAll()
            assertEquals(1L, result.count())
        }
    }

    @Test
    fun testMultipleReferenceJoin02() {
        val foo = object : IntIdTable("foo") {
            val baz = integer("baz").uniqueIndex()
        }
        val bar = object : IntIdTable("bar") {
            val foo = reference("foo", foo)
            val foo2 = reference("foo2", foo)
            val baz = integer("baz") references foo.baz
        }
        withTables(foo, bar) {
            expectException<IllegalStateException> {
                val fooId = foo.insertAndGetId {
                    it[baz] = 5
                }

                bar.insert {
                    it[this.foo] = fooId
                    it[this.foo2] = fooId
                    it[baz] = 5
                }

                val result = foo.innerJoin(bar).selectAll()
                assertEquals(1L, result.count())
            }
        }
    }

    @Test
    fun testJoinWithAlias01() {
        withCitiesAndUsers {
            val usersAlias = users.alias("u2")
            Join(users)
                .join(usersAlias,
                      JoinType.LEFT,
                      usersAlias[users.id],
                      stringLiteral("smth"))
                .select { users.id eq "alex" }
                .single().let { resultRow ->
                    assert(resultRow[users.name] == "Alex")
                    assert(resultRow[usersAlias[users.name]] == "Something")
                }

            // Only Eugene is in scope
            var scopedUsersAlias : Alias<Table> = scopedUsers.alias("u3")
            Join(scopedUsers)
                .join(scopedUsersAlias,
                      JoinType.LEFT,
                      scopedUsersAlias[scopedUsers.id],
                      stringLiteral("alex"))
                .select { scopedUsers.id inList listOf("smth", "eugene") }
                .single().let { resultRow ->
                    assert(resultRow[scopedUsers.name] == "Eugene")
                    assert(resultRow[scopedUsersAlias[scopedUsers.name]] == "Alex")
                }

            // Stripping the default scope leads to alex falling into scope too
            scopedUsersAlias = scopedUsers.stripDefaultFilter().alias("u3")
            Join(scopedUsers.stripDefaultFilter())
                .join(scopedUsersAlias,
                      JoinType.LEFT,
                      scopedUsersAlias[scopedUsers.id],
                      stringLiteral("alex"))
                .select { scopedUsers.id inList listOf("smth", "eugene") }
                .orderBy(scopedUsers.id, SortOrder.ASC)
                .toList().let { resultRows ->
                    assertEquals(resultRows[0][scopedUsers.name],"Eugene")
                    assertEquals(resultRows[0][scopedUsersAlias[scopedUsers.name]], "Alex")
                    assertEquals(resultRows[1][scopedUsers.name], "Something")
                    assertEquals(resultRows[1][scopedUsersAlias[scopedUsers.name]], "Alex")
                }
        }
    }

    @Test
    fun testJoinWithJoin01() {
        withCitiesAndUsers {
             (cities innerJoin (users innerJoin userData))
                 .selectAll()
                 .let { rows -> assertEquals(2L, rows.count()) }

            (cities innerJoin (scopedUsers innerJoin scopedUserData))
                .selectAll()
                .let { rows -> assertEquals(1L, rows.count()) }
        }
    }

    @Test
    fun testJoinWithAdditionalConstraint() {
        withCitiesAndUsers {
            val usersAlias = users.alias("name")
            cities.join(usersAlias, JoinType.INNER, cities.id, usersAlias[users.cityId]) {
                cities.id greater 1 and (cities.name.neq(usersAlias[users.name]))
            }.let { join -> assertEquals(2L, join.selectAll().count()) }

            val scopedUserAlias = scopedUsers.alias("su2")

            cities.join(scopedUserAlias, JoinType.INNER, cities.id, scopedUserAlias[scopedUsers.cityId]) {
                cities.id greater 1 and (cities.name.neq(scopedUserAlias[scopedUsers.name]))
            }.let { join -> assertEquals(2L, join.selectAll().count()) }
        }
    }

    @Test
    fun testNoWarningsOnLeftJoinRegression() {
        val logCaptor = LogCaptor.forName(exposedLogger.name)

        val mainTable = object : Table("maintable") {
            val id = integer("idCol")
        }
        val joinTable = object : Table("jointable") {
            val id = integer("idCol")
            val data = integer("dataCol").default(42)
        }

        withTables(mainTable, joinTable) {
            mainTable.insert { it[id] = 2 }

            mainTable.join(joinTable, JoinType.LEFT, joinTable.id, mainTable.id)
                .slice(joinTable.data)
                .selectAll()
                .single()
                .getOrNull(joinTable.data)

            // Assert no logging took place
            assertTrue(logCaptor.warnLogs.isEmpty())
            assertTrue(logCaptor.errorLogs.isEmpty())
        }
    }
}
