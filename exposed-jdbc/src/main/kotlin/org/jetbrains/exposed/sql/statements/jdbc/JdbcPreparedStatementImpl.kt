package org.jetbrains.exposed.sql.statements.jdbc

import org.jetbrains.exposed.sql.BinaryColumnType
import org.jetbrains.exposed.sql.BlobColumnType
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.vendors.SQLiteDialect
import org.jetbrains.exposed.sql.vendors.currentDialect
import java.io.InputStream
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types

class JdbcPreparedStatementImpl(
    val statement: PreparedStatement,
    val wasGeneratedKeysRequested: Boolean,
    private val supportsGetGeneratedKeys: Boolean
) : PreparedStatementApi {
    override val resultSet: ResultSet?
        get() = when {
            !wasGeneratedKeysRequested -> statement.resultSet
            supportsGetGeneratedKeys -> statement.generatedKeys
            currentDialect is SQLiteDialect -> {
                statement.connection.prepareStatement("select last_insert_rowid();").executeQuery()
            }
            else -> statement.resultSet
        }

    override var fetchSize: Int?
        get() = statement.fetchSize
        set(value) {
            value?.let { statement.fetchSize = value }
        }

    override fun addBatch() {
        statement.addBatch()
    }

    override fun executeQuery(): ResultSet = statement.executeQuery()

    override fun executeUpdate(): Int = statement.executeUpdate()

    override fun set(index: Int, value: Any) {
        statement.setObject(index, value)
    }

    override fun setNull(index: Int, columnType: IColumnType) {
        if (columnType is BinaryColumnType || columnType is BlobColumnType) {
            statement.setNull(index, Types.LONGVARBINARY)
        } else {
            statement.setObject(index, null)
        }
    }

    override fun setInputStream(index: Int, inputStream: InputStream) {
        statement.setBinaryStream(index, inputStream, inputStream.available())
    }

    override fun closeIfPossible() {
        if (!statement.isClosed) statement.close()
    }

    override fun executeBatch(): List<Int> {
        return statement.executeBatch().map {
            when (it) {
                Statement.SUCCESS_NO_INFO -> 1
                Statement.EXECUTE_FAILED -> 0
                else -> it
            }
        }
    }

    override fun cancel() {
        if (!statement.isClosed) statement.cancel()
    }
}
