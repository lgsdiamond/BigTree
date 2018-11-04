package com.lgsdiamond.bigtree.amway

import android.content.ContentValues
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.lgsdiamond.bigtree.APP_LOG_TAG
import com.lgsdiamond.bigtree.gMainActivity
import com.lgsdiamond.bigtree.tree.AmNode
import com.lgsdiamond.lgsutility.toToastTitle

enum class DBKeysMembers(val type: String) {
    KEY_ID_STAMP("Long"), KEY_MEMBER_CLASS("Int"), KEY_MEMBER_NAME("String"), KEY_PV_PERSONAL("Float"), KEY_SPONSOR_ID("Long");

    val key: String
        get() = toString()

    fun toDBField(): String = when (type) {
        "Long" -> "$key INT"
        "Int" -> "$key SMALLINT"
        "String" -> "$key TEXT"
        "Float" -> "$key FLOAT"
        "Double" -> "$key DOUBLE"
        else -> ""
    }

    companion object {
        fun toSqlCreate(tableName: String): String {
            var sqlCmd = "CREATE TABLE $tableName ("
            val itsValues = values()
            if (itsValues.isNotEmpty()) {
                sqlCmd += itsValues[0].toDBField()
            }
            for (i in 1..(itsValues.size - 1)) {
                sqlCmd += ", ${itsValues[i].toDBField()}"
            }
            sqlCmd += ");"
            return sqlCmd
        }
    }
}

enum class DBKeysMonthlyRecord(val type: String) {
    KEY_ID_STAMP("Long"), KEY_MEMBER_CLASS("Int"), KEY_MEMBER_NAME("String"), KEY_PV_PERSONAL("Float"), KEY_SPONSOR_ID("Long");

    val key: String
        get() = toString()

    fun toDBField(): String = when (type) {
        "Long" -> "$key INT"
        "Int" -> "$key SMALLINT"
        "String" -> "$key TEXT"
        "Float" -> "$key FLOAT"
        "Double" -> "$key DOUBLE"
        else -> ""
    }

    companion object {
        fun toSqlCreate(tableName: String): String {
            var sqlCmd = "CREATE TABLE $tableName ("
            val itsValues = values()
            if (itsValues.isNotEmpty()) {
                sqlCmd += itsValues[0].toDBField()
            }
            for (i in 1..(itsValues.size - 1)) {
                sqlCmd += ", ${itsValues[i].toDBField()}"
            }
            sqlCmd += ");"
            return sqlCmd
        }
    }
}

class MonthlyRecord(val month: AmMonth) {
    var groupPV = 0f
    var personalPV = 0f
    var personalGroupPV = 0f
    var pin = PinTitle.NONE

    var firstBonus: Float = 0f
    var leadershipBonus: Float = 0f
    var rubyBonus: Float = 0f
    var monthlyDepthBonus: Float = 0f
    var emeraldBonus: Float = 0f
    var diamondBonus: Float = 0f
    var diamondPlusBonus: Float = 0f
    var oneTimeBonus: Float = 0f
    var faaBonus: Float = 0f
}

class CurrentRecord(val abo: ABO, val month: AmMonth? = null) {
    val groupPV: Float
        get() = abo.pv.group
    val personalPV: Float
        get() = abo.pv.personalGroup
    val personalGroupPV: Float
        get() = abo.pv.personalGroup

    var pin = PinTitle.NONE

    val firstBonus: Float
        get() = abo.bonus.first.amount
    val leadershipBonus: Float
        get() = abo.bonus.leadership.amount
    val rubyBonus: Float
        get() = abo.bonus.ruby.amount
    val monthlyDepthBonus: Float
        get() = abo.bonus.monthlyDepth.amount
    val emeraldBonus: Float
        get() = abo.bonus.emerald.amount
    val diamondBonus: Float
        get() = abo.bonus.diamond.amount
    val diamondPlusBonus: Float
        get() = abo.bonus.diamondPlus.amount
    val oneTimeBonus: Float
        get() = abo.bonus.oneTime.amount
    val faaBonus: Float
        get() = abo.bonus.faa.amount
}

abstract class AppDBHelper : SQLiteOpenHelper(gMainActivity, DB_FILE_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w("TaskDBAdapter", "Upgrading from version $oldVersion to $newVersion, which will destroy all old member")
        db.execSQL("DROP TABLE IF EXISTS " + "LOGIN")
        onCreate(db)
    }

    val databaseInstance: SQLiteDatabase
        get() = bigtreeDB

    // Method to open the Database
    @Throws(SQLException::class)
    fun open(): AppDBHelper {
        bigtreeDB = writableDatabase
        return this
    }

    // Method to close the Database
    override fun close() {
        bigtreeDB.close()
    }

    fun complteTransction() {
        bigtreeDB.setTransactionSuccessful()
        bigtreeDB.endTransaction()
    }

    companion object {
        const val DB_FILE_NAME = "bigtree.db"
        const val DB_VERSION = 1
        internal lateinit var bigtreeDB: SQLiteDatabase
    }
}

class MemberDBHelper(private val tableName: String) : AppDBHelper() {

    private val tableFullName: String
        get() = "${tableName}_MEMBERS"

    private val tableFullNameOld: String
        get() = "${tableFullName}_OLD"

    private fun backupMemberTable() {
        try {
            bigtreeDB.beginTransaction()
            val sqlCmd = "DROP TABLE IF EXISTS $tableFullNameOld"
            bigtreeDB.execSQL(sqlCmd)
        } catch (e: SQLiteException) {
            Log.e(APP_LOG_TAG, e.toString(), e)
        } finally {
            complteTransction()
        }
        try {
            bigtreeDB.beginTransaction()
            val sqlCmd = "ALTER TABLE $tableFullName RENAME TO $tableFullNameOld"
            bigtreeDB.execSQL(sqlCmd)
        } catch (e: SQLiteException) {
            Log.e(APP_LOG_TAG, e.toString(), e)
        } finally {
            complteTransction()
        }
    }

    private fun createNewMemberTable() {

        backupMemberTable()

        try {
            bigtreeDB.beginTransaction()
            val sqlCmd = "DROP TABLE IF EXISTS $tableFullName"
            bigtreeDB.execSQL(sqlCmd)
        } catch (e: SQLiteException) {
            Log.e(APP_LOG_TAG, e.toString(), e)
        } finally {
            complteTransction()
        }

        try {
            bigtreeDB.beginTransaction()
            val sqlCmd = DBKeysMembers.toSqlCreate(tableFullName)
            bigtreeDB.execSQL(sqlCmd)
        } catch (e: SQLiteException) {
            Log.e(APP_LOG_TAG, e.toString(), e)
        } finally {
            complteTransction()
        }
    }

    fun writeNetworkEntries(nodes: List<AmNode>) {
        open()
        bigtreeDB = writableDatabase

        val allNodeValues = ArrayList<ContentValues>()
        nodes.forEach { it.addUpContentValues(allNodeValues) }

        createNewMemberTable()

        for (nodeValues in allNodeValues) {
            try {
                // Insert the row into your table
                val result = bigtreeDB.insert(tableFullName, null, nodeValues)
                Log.d(APP_LOG_TAG, "member db inserted at row $result")
            } catch (ex: Exception) {
                println("Exceptions $ex")
                Log.e(APP_LOG_TAG, "member db insertion failed")
            }
        }
        ("현재 네트워크가 저장되었습니다.").toToastTitle()

        complteTransction()
        close()
    }


    private fun isTableExists(tableName: String): Boolean {
        val cursor = bigtreeDB.rawQuery("SELECT DISTINCT tbl_name from sqlite_master where tbl_name = '$tableName'", null)

        if (cursor != null) {
            if (cursor.count > 0) {
                cursor.close()
                return true
            }
            cursor.close()
        }
        return false
    }

    fun readNetworkEntries(): MutableList<AmNode>? {
        open()
        bigtreeDB = readableDatabase

        if (!isTableExists(tableFullName)) return null

        val sqlCmd = "SELECT * FROM $tableFullName"
        val cursor = bigtreeDB.rawQuery(sqlCmd, null, null)
        val aMemberNodes: MutableList<AmNode> = ArrayList()
        val valuesArray: MutableList<ContentValues> = ArrayList()
        try {
            if (cursor.moveToFirst()) {
                do {
                    val nodeValues = ContentValues()
                    // Passing values
                    val idStamp = cursor.getLong(0)
                    val classOrdinal = cursor.getInt(1)
                    val name = cursor.getString(2)
                    val pv = cursor.getFloat(3)
                    val sponsorID = cursor.getLong(4)

                    nodeValues.put(DBKeysMembers.KEY_MEMBER_CLASS.key, classOrdinal)
                    nodeValues.put(DBKeysMembers.KEY_ID_STAMP.key, idStamp)
                    nodeValues.put(DBKeysMembers.KEY_MEMBER_NAME.key, name)
                    nodeValues.put(DBKeysMembers.KEY_PV_PERSONAL.key, pv)
                    nodeValues.put(DBKeysMembers.KEY_SPONSOR_ID.key, sponsorID)

                    valuesArray.add(nodeValues)
                } while (cursor.moveToNext())
            }
            cursor.close()

            var prevMember: AmMember? = null
            for (nodeValues in valuesArray) {
                val (member, sponsorID) = AmNode.values2member(nodeValues)
                if (sponsorID == 0L) {
                    aMemberNodes.add(AmNode(member))
                } else {
                    if (prevMember == null) {
                        throw NetworkDBException("Error: null in 'prevMember'")
                    }
                    val sponsor = prevMember.findSponsorByID(sponsorID)
                    sponsor?.let { sponsor.node.addMemberNode(AmNode(member)) }
                            ?: throw NetworkDBException("Error: null in 'sponsor'")
                }
                prevMember = member
            }
        } catch (e: NetworkDBException) {
            aMemberNodes.clear()
            ("네트워크 DB에 오류가 있습니다. 기본 네트워크로 돌아갑니다.").toToastTitle()
            return null
        } finally {
            close()
        }
        return aMemberNodes
    }
}

class RecordDBHelper(private val tableName: String) : AppDBHelper() {

    private val tableFullName: String
        get() = "${tableName}_RECORD"

    private val tableFullNameOld: String
        get() = "${tableFullName}_OLD"

    private fun backupMemberTable() {
        try {
            bigtreeDB.beginTransaction()
            val sqlCmd = "DROP TABLE IF EXISTS $tableFullNameOld"
            bigtreeDB.execSQL(sqlCmd)
        } catch (e: SQLiteException) {
            Log.e(APP_LOG_TAG, e.toString(), e)
        } finally {
            complteTransction()
        }
        try {
            bigtreeDB.beginTransaction()
            val sqlCmd = "ALTER TABLE $tableFullName RENAME TO $tableFullNameOld"
            bigtreeDB.execSQL(sqlCmd)
        } catch (e: SQLiteException) {
            Log.e(APP_LOG_TAG, e.toString(), e)
        } finally {
            complteTransction()
        }
    }

    private fun createNewMemberTable() {

        backupMemberTable()

        try {
            bigtreeDB.beginTransaction()
            val sqlCmd = "DROP TABLE $tableFullName"
            bigtreeDB.execSQL(sqlCmd)
        } catch (e: SQLiteException) {
            Log.e(APP_LOG_TAG, e.toString(), e)
        } finally {
            complteTransction()
        }

        try {
            bigtreeDB.beginTransaction()
            val sqlCmd = DBKeysMembers.toSqlCreate(tableFullName)
            bigtreeDB.execSQL(sqlCmd)
        } catch (e: SQLiteException) {
            Log.e(APP_LOG_TAG, e.toString(), e)
        } finally {
            complteTransction()
        }
    }

    fun writeNetworkEntries(nodes: List<AmNode>) {
        open()
        bigtreeDB = writableDatabase

        val allNodeValues = ArrayList<ContentValues>()
        nodes.forEach { it.addUpContentValues(allNodeValues) }

        createNewMemberTable()

        for (nodeValues in allNodeValues) {
            try {
                // Insert the row into your table
                val result = bigtreeDB.insert(tableFullName, null, nodeValues)
                Log.d(APP_LOG_TAG, "member db inserted at row $result")
            } catch (ex: Exception) {
                println("Exceptions $ex")
                Log.e(APP_LOG_TAG, "member db insertion failed")
            }
        }
        ("현재 네트워크가 저장되었습니다.").toToastTitle()

        complteTransction()
        close()
    }

    fun readNetworkEntries(): MutableList<AmNode> {
        open()
        bigtreeDB = readableDatabase

        val sqlSelectAll = "SELECT * FROM $tableFullName"

        val cursor = bigtreeDB.rawQuery(sqlSelectAll, null, null)

        val aMemberNodes: MutableList<AmNode> = ArrayList()

        try {
            val valuesArray: MutableList<ContentValues> = ArrayList()

            if (cursor.moveToFirst()) {
                do {
                    val nodeValues = ContentValues()
                    // Passing values
                    val idStamp = cursor.getLong(0)
                    val classOrdinal = cursor.getInt(1)
                    val name = cursor.getString(2)
                    val pv = cursor.getFloat(3)
                    val sponsorID = cursor.getLong(4)

                    nodeValues.put(DBKeysMembers.KEY_MEMBER_CLASS.key, classOrdinal)
                    nodeValues.put(DBKeysMembers.KEY_ID_STAMP.key, idStamp)
                    nodeValues.put(DBKeysMembers.KEY_MEMBER_NAME.key, name)
                    nodeValues.put(DBKeysMembers.KEY_PV_PERSONAL.key, pv)
                    nodeValues.put(DBKeysMembers.KEY_SPONSOR_ID.key, sponsorID)

                    valuesArray.add(nodeValues)
                } while (cursor.moveToNext())
            }
            cursor.close()

            var prevMember: AmMember? = null
            for (nodeValues in valuesArray) {
                val (member, sponsorID) = AmNode.values2member(nodeValues)
                if (sponsorID == 0L) {
                    aMemberNodes.add(AmNode(member))
                } else {
                    if (prevMember == null) {
                        throw NetworkDBException("Network Data Error: null prevNode")
                    }

                    val sponsor = prevMember.findSponsorByID(sponsorID)
                    if (sponsor == null) {
                        throw NetworkDBException("Network Data Error: null sponsor")
                    }
                    sponsor.node.addMemberNode(AmNode(member))
                }
                prevMember = member
            }
        } catch (e: NetworkDBException) {
            aMemberNodes.clear()
            ("네트워크 DB에 오류가 있습니다. 기본 네트워크로 돌아갑니다.").toToastTitle()
        } finally {
            close()
        }

        return aMemberNodes
    }
}

open class NetworkDBException : Exception {

    /**
     * Constructs an `IOException` with `null`
     * as its error detail message.
     */
    constructor() : super()

    /**
     * Constructs an `IOException` with the specified detail message.
     *
     * @param message
     * The detail message (which is saved for later retrieval
     * by the [.getMessage] method)
     */
    constructor(message: String) : super(message)

    /**
     * Constructs an `IOException` with the specified detail message
     * and cause.
     *
     *
     *  Note that the detail message associated with `cause` is
     * *not* automatically incorporated into this exception's detail
     * message.
     *
     * @param message
     * The detail message (which is saved for later retrieval
     * by the [.getMessage] method)
     *
     * @param cause
     * The cause (which is saved for later retrieval by the
     * [.getCause] method).  (A null value is permitted,
     * and indicates that the cause is nonexistent or unknown.)
     *
     * @since 1.6
     */
    constructor(message: String, cause: Throwable) : super(message, cause)

    /**
     * Constructs an `IOException` with the specified cause and a
     * detail message of `(cause==null ? null : cause.toString())`
     * (which typically contains the class and detail message of `cause`).
     * This constructor is useful for IO exceptions that are little more
     * than wrappers for other throwables.
     *
     * @param cause
     * The cause (which is saved for later retrieval by the
     * [.getCause] method).  (A null value is permitted,
     * and indicates that the cause is nonexistent or unknown.)
     *
     * @since 1.6
     */
    constructor(cause: Throwable) : super(cause)
}
