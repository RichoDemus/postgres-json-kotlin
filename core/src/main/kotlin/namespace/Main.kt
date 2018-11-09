package namespace

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet
import java.util.Random
import java.util.UUID
import java.util.UUID.randomUUID


data class Header(
        val id: UUID,
        val customer: String,
        val type: Type,
        val lines: List<Line>
)

data class Line(
        val amount: Int,
        val currency: String
)

private fun name() = listOf("Arne", "Anna", "Lotta", "Aron", "Aslan").random()
private fun age() = Random().nextInt(99)

private fun <E> List<E>.random(): E? = if (size > 0) get(Random().nextInt(size)) else null

private val mapper = jacksonObjectMapper()

fun main(args: Array<String>) {
    println("Hello world!")



    transaction(db = Database.connect(
            url = "jdbc:postgresql://localhost:5432/mydb",
            driver = "org.postgresql.Driver",
            user = "usr",
            password = "passw"
    )) {

        exec("drop table documents")
//        SchemaUtils.create(Documents)
        exec("""
            CREATE TABLE documents (
                id uuid NOT NULL PRIMARY KEY,
                type varchar(20) NOT NULL,
                body json NOT NULL
            );
            """)

        listOf(
                Header(randomUUID(), "Telia", Type.Invoice, listOf(Line(10, "SEK"))),
                Header(randomUUID(), "Telenor", Type.Invoice, emptyList()),
                Header(randomUUID(), "3", Type.Invoice, listOf(
                        Line(20, "SEK"),
                        Line(100, "EUR"))
                )
        ).forEach { header ->
            exec("""
                INSERT INTO documents (id,type, body)
                VALUES(
                    '${header.id}',
                    '${header.type}',
                    '${mapper.writeValueAsString(header)}'
                );
                """)
        }

//        for (i in 1..10) {
//            //language=JSON
//            val json = "{\"name\":\"${name()}\",\"age\":${age()}}"
////            Documents.inse
//            exec("""
//                INSERT INTO documents (id,type, body)
//                VALUES(
//                    '${randomUUID()}',
//                    'aasd',
//                    '$json'
//                );
//                """)
//        }

//        for (city in Documents.selectAll()) {
//            println("${city[Documents.id]}: ${city[Documents.body]}")
//        }

        fun transform(rs: ResultSet) = mapper.readValue<Header>(rs.getString("body"))

        println("\nAll:")
        val result = arrayListOf<Header>()
        exec("Select * from Documents") { rs ->
            while (rs.next()) {
                result += transform(rs)
            }
        }
        result.forEach { println(it) }

        println("\n\nFilter based on json, exclude telenor")
        val result2 = arrayListOf<Header>()
//        exec("""
//            Select body ->> 'customer' as customer,
//            sum((json_array_elements(body -> 'lines') ->> 'amount')::int) as lines
//            from Documents
//            """.trimIndent()) { rs ->
//            exec("""
//                SELECT
//
//                    body ->> 'customer',
//                       json_array_elements_text(body->'lines')
//                FROM documents;
//            """.trimIndent()) { rs ->
        exec("""
            SELECT body ->> 'customer',
            body
            from documents
            where body ->> 'customer' <> 'Telenor'
            """.trimIndent()) { rs ->
            while (rs.next()) {
                println("${rs.getString(1)}\t${rs.getString(2)}")
//                println(rs.getString(1) + " - " + rs.getString(2))
            }
        }
        result2.forEach { println(it) }

    }
}
