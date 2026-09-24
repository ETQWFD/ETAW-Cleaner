package com.etaw.cleaner

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class RecordDb(context: Context) : SQLiteOpenHelper(context, "etaw.db", null, 2) {

    companion object {
        const val TABLE = "deleted_apps"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "pkg TEXT, name TEXT, website TEXT, " +
                    "install_time INTEGER, uninstall_time INTEGER, " +
                    "sha256 TEXT, residue_count INTEGER, " +
                    "scanned_count INTEGER DEFAULT 0, " +
                    "deleted_count INTEGER DEFAULT 0, " +
                    "freed_bytes INTEGER DEFAULT 0, " +
                    "deleted_paths TEXT DEFAULT '', " +
                    "note TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE $TABLE ADD COLUMN scanned_count INTEGER DEFAULT 0") } catch (e: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE ADD COLUMN deleted_count INTEGER DEFAULT 0") } catch (e: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE ADD COLUMN freed_bytes INTEGER DEFAULT 0") } catch (e: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE ADD COLUMN deleted_paths TEXT DEFAULT ''") } catch (e: Exception) {}
        }
    }

    fun insert(item: RecordItem): Long {
        val cv = ContentValues().apply {
            put("pkg", item.pkg)
            put("name", item.name)
            put("website", item.website)
            put("install_time", item.installTime)
            put("uninstall_time", item.uninstallTime)
            put("sha256", item.sha256)
            put("residue_count", item.residueCount)
            put("scanned_count", item.scannedCount)
            put("deleted_count", item.deletedCount)
            put("freed_bytes", item.freedBytes)
            put("deleted_paths", item.deletedPaths)
            put("note", item.note)
        }
        return writableDatabase.insert(TABLE, null, cv)
    }

    fun all(): List<RecordItem> {
        val list = mutableListOf<RecordItem>()
        val c = readableDatabase.query(
            TABLE, null, null, null, null, null, "uninstall_time DESC"
        )
        c.use {
            val idIdx = it.getColumnIndexOrThrow("id")
            val pkgIdx = it.getColumnIndexOrThrow("pkg")
            val nameIdx = it.getColumnIndexOrThrow("name")
            val webIdx = it.getColumnIndexOrThrow("website")
            val itIdx = it.getColumnIndexOrThrow("install_time")
            val utIdx = it.getColumnIndexOrThrow("uninstall_time")
            val shaIdx = it.getColumnIndexOrThrow("sha256")
            val rcIdx = it.getColumnIndexOrThrow("residue_count")
            val scIdx = it.getColumnIndexOrThrow("scanned_count")
            val dcIdx = it.getColumnIndexOrThrow("deleted_count")
            val fbIdx = it.getColumnIndexOrThrow("freed_bytes")
            val dpIdx = it.getColumnIndexOrThrow("deleted_paths")
            val noteIdx = it.getColumnIndexOrThrow("note")
            while (it.moveToNext()) {
                list.add(
                    RecordItem(
                        id = it.getLong(idIdx),
                        pkg = it.getString(pkgIdx),
                        name = it.getString(nameIdx),
                        website = it.getString(webIdx),
                        installTime = it.getLong(itIdx),
                        uninstallTime = it.getLong(utIdx),
                        sha256 = it.getString(shaIdx),
                        residueCount = it.getInt(rcIdx),
                        scannedCount = it.getInt(scIdx),
                        deletedCount = it.getInt(dcIdx),
                        freedBytes = it.getLong(fbIdx),
                        deletedPaths = it.getString(dpIdx),
                        note = it.getString(noteIdx)
                    )
                )
            }
        }
        return list
    }

    fun delete(id: Long): Int =
        writableDatabase.delete(TABLE, "id=?", arrayOf(id.toString()))

    fun clear(): Int = writableDatabase.delete(TABLE, null, null)
}
