package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.File


/*
val IMAGE = "image"
val VIDEO = "video"
val PAGE = "page"
val BUTTON = "button"
val SOUND = "sound"
*/



//todo: -?- instead of uid's, use hash for everything

// BaseColumns._ID is the best choice, because linking the results of a provider query
// to a ListView requires one of the retrieved columns to have the name _ID.

@Entity
data class Word(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "text") val text: String?,
    @ColumnInfo(name = "resource_id") val resourceId: Int,
    @ColumnInfo(name = "priority") val priority: Int
) {

    companion object {
        const val USER = 0x01
        const val DERIVED = 0x02
        const val CATEGORY = 0x04
        const val PARTIAL = 0x08
    }
}

@Dao
interface WordDao {

    @Query("SELECT * FROM word GROUP BY text ORDER BY text")
    fun getAllDistinct(): Cursor?

    // todo: paging
    @Query("SELECT * FROM word WHERE text LIKE :text")
    fun listByText(text: String): List<Word>

    @Query("SELECT * FROM word WHERE uid IN (:wordIds)")
    fun getAllByIds(wordIds: IntArray): Cursor?

    @Query("SELECT * FROM word WHERE text LIKE :text LIMIT :limit")
    fun getAllByText(text: String, limit: Int): Cursor?

    @Insert
    fun insertAll(vararg words: Word)

    @Delete
    fun delete(word: Word)
}

@Entity
data class Resource(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "resource_type") val resourceType: String,
    @ColumnInfo(name = "uri") val resourceUri: String = "",
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "data") val data: String = "",
    @ColumnInfo(name = "child_ids") val children: String = ""
) {
    enum class Type {
        IMAGE, VIDEO, PAGE, BUTTON, SOUND, KEYBOARD
    }

}

@Dao
interface ResourceDao {
    @Query("SELECT COUNT(uid) FROM resource")
    fun getCount(): Int?

    @Query("SELECT * FROM resource WHERE uid=:id")
    fun getLive(id: Int): LiveData<Resource?>?

    @Query("SELECT * FROM resource WHERE uid IN (:resourceIds)")
    fun listAllByIds(resourceIds: IntArray): List<Resource>

    @Query("SELECT * FROM resource WHERE resource_type = :type")
    fun listAllByType(type: String): List<Resource>

    @Query("SELECT * FROM resource WHERE resource_type = :type LIMIT 1")
    fun getLiveAny(type: String): LiveData<Resource?>?

    @Query("SELECT * FROM resource WHERE uid LIKE :text")
    fun listAllUriContains(text: String): List<Resource>

    @Query("SELECT * FROM resource WHERE uid LIKE :text")
    fun getAllUriContains(text: String): Cursor?

    // todo: paging
    @Query("SELECT * FROM resource WHERE resource_type IN (:types) LIMIT :limit")
    fun getAllByType(types: Array<String>, limit: Int = 10): Cursor?

    @Query("SELECT * FROM resource WHERE uid IN (:ids)")
    fun getLiveById(ids: IntArray): List<Resource>?

    @Query("SELECT * FROM resource WHERE uid IN (:ids)")
    fun getAllById(ids: IntArray): Cursor?

    @Query("SELECT * FROM resource WHERE uid IN (:ids)")
    fun getAllLiveById(ids: IntArray): LiveData<List<Resource>?>?

    @Query("SELECT * FROM resource WHERE resource_type = :type")
    fun getAllLiveByType(type: String): LiveData<List<Resource>?>?

    @Query("SELECT * FROM resource WHERE resource_type=:type AND uid IN (:ids)")
    fun getAllTypeById(type: String, ids: IntArray): List<Resource>?

    @Query("SELECT * FROM resource WHERE uid=:id LIMIT 1")
    fun get(id: Int): Cursor?

    @Query("SELECT * FROM resource WHERE uid=:id LIMIT 1")
    fun find(id: Int): Resource?

    /**
     * UPSERT following https://stackoverflow.com/a/50736568/7439163
     *
     * Insert an object in the database.
     *
     * @param obj the object to be inserted.
     * @return The SQLite row id
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(resource: Resource): Long

    /**
     * Insert an array of objects in the database.
     *
     * @param obj the objects to be inserted.
     * @return The SQLite row id for each, or -1 if failed
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(resources: List<Resource>): List<Long>

    /**
     * Update an object from the database.
     *
     * @param obj the object to be updated
     */
    @Update
    fun update(resource: Resource)

    /**
     * Update an array of objects from the database.
     *
     * @param obj the object to be updated
     */
    @Update
    fun update(resources: List<Resource>)

    /**
     * Delete an object from the database
     *
     * @param obj the object to be deleted
     */
    @Delete
    fun delete(resource: Resource)

    @Transaction
    fun upsert(resource: Resource) {
        insert(resource)
            .also { if (it == -1L) update(resource) }
    }

    @Transaction
    fun upsert(resources: List<Resource>) {
        Log.d("ResourceDao", "inserting resources: ${resources.size}")
        insert(resources)
            .mapIndexedNotNull { index, l ->
                if (l == -1L) resources.get(index) else null
            }
            .also { failed ->
                Log.d("ResourceDao", "updating existing resources: ${failed.size}")
                if (failed.isNotEmpty()) update(failed)
            }
    }
}




@Entity()
data class Recent(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name = "resourceId") val resourceId: Int = 0,
    @ColumnInfo(name = "actionType") val actionType: Int
) {
    enum class ActionType {
        ADD_TO_MESSAGE, START_MESSAGE, CLEAR_RECENTS
    }

}

@Dao
abstract class RecentDao {

    @Insert
    abstract fun insert(recent: Recent)

    @Delete
    abstract fun delete(recent: Recent)

    fun clearRecents() {
        Recent(actionType = Recent.ActionType.CLEAR_RECENTS.ordinal)
            .also { insert(it) }
    }

    fun startMessage() {
        Recent(actionType = Recent.ActionType.START_MESSAGE.ordinal)
            .also { insert(it) }
    }

    fun addResource(resourceId: Int) {
        Recent(resourceId = resourceId, actionType = Recent.ActionType.ADD_TO_MESSAGE.ordinal)
    }

    @Query("SELECT * FROM recent WHERE uid >= (SELECT MAX(uid) FROM recent WHERE actionType=:action)")
    abstract fun getAllSince(action: Int) : List<Recent>

}



@Database(entities = arrayOf(Word::class, Resource::class, Recent::class), version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun resourceDao(): ResourceDao
    abstract fun recentDao(): RecentDao

    val TAG = "AppDatabase"

    val newUID: Int
        get() = (Math.random() * Int.MAX_VALUE).toInt()

    /*  // todo: make livedata list of ids
      fun getRecentButtons() : List<Resource>? {
          return recentDao().getAllSince(Recent.ActionType.CLEAR_RECENTS.ordinal)
              .map { it.resourceId }
              .filter { it != 0 }
              .toIntArray()
              .let { resourceDao().getLiveById(it)}
      }*/


    fun getResourcesByWord(word: String) : Cursor? {
        return wordDao().listByText(word)
            .map { it.resourceId }
            .toIntArray()
            .let { resourceDao().getAllById(it) }
    }

    fun enterKeyboard(pages: List<PageData>, uri: Uri?, name: String?) {
        Log.i(TAG, "enterKeyboard: $name")
        val entries = mutableListOf<Any?>()

        val keyboardId = uri?.hashCode() ?: newUID

        val pageEntries = createPageEntries(pages)
        entries.addAll(pageEntries)

        entries.add(Resource(
            keyboardId,
            Resource.Type.KEYBOARD.name,
            uri?.toString() ?: "",
            title = name ?: "Untitled",
            children = pageEntries.filterIsInstance<Resource>()
                .filter { it.resourceType == Resource.Type.PAGE.name }
                .map { it.uid }.joinToString(DELIMITER)
        )
        )

        uri?.lastPathSegment?.also {
            entries.add(Word(newUID, it, keyboardId, Word.DERIVED))
        }
        name?.also {
            entries.add(Word(newUID, it, keyboardId, Word.USER))
        }


        entries.filterIsInstance(Word::class.java)
            .also { if (it.isNotEmpty()) wordDao().insertAll(*it.toTypedArray()) }
        entries.filterIsInstance(Resource::class.java)
            .also { if (it.isNotEmpty()) resourceDao().upsert(it) }

        Log.i(TAG, "entered keyboard $name with ${pageEntries.size} resources/words from pages")
    }

    fun createPageEntries(pages: List<PageData>) : Array<Any?> {
        val entries = mutableListOf<Any>()
        pages
            .forEach { page ->
                val pageId = newUID

                // create entries for buttons in page:
                val iconEntries = page.icons.flatMap { createIconEntries(it) }
                entries.addAll(iconEntries)

                // add page as a resource, associated with children from above:
                entries.add(Resource(
                    uid = pageId,
                    title = page.name ?: "",
                    resourceType = Resource.Type.PAGE.name,
                    children = iconEntries.filterIsInstance<Resource>()
                        .filter { it.resourceType == Resource.Type.BUTTON.name }
                        .map { it.uid }.joinToString(DELIMITER)
                ))

                // create dictionary entry for the page resource:
                entries.add(Word(newUID, page.name, pageId, Word.DERIVED))

            }

        return entries.toTypedArray()
    }

    fun createIconEntries(icon: IconData) : List<Any> {
        return icon.path
            ?.let { File(it) }
            ?.let { if (it.exists()) Uri.fromFile(it) else null }
            .let { uri ->

                val buttonId = newUID

                listOf(

                    // create entries for the image in case this is it's first use:
                    *createResourceEntries(uri, icon.text),

                    // create resource for this icon:
                    Resource(
                        uid = buttonId,
                        resourceType = Resource.Type.BUTTON.name,
                        resourceUri = uri?.toString() ?: "",
                        title = icon.text ?: ""
                    ),

                    // add the word to the dictionary for the button:
                    icon.text?.let { text -> Word(newUID, text, buttonId, Word.DERIVED) }

                ).filterNotNull()
            }
    }

    // add image as it's own resource entity and create associated word entities from uri text:
    fun createResourceEntries(uri: Uri?, text: String?) : Array<Any?> {
        uri ?: return emptyArray()

        val id = uri.hashCode()
        return  arrayOf(
            Resource(id, uri.toString(), Resource.Type.IMAGE.name),

            // add dictionary entries based on text in path:
            Word(newUID, uri.lastPathSegment, id, Word.DERIVED),

            // add dictionary entries based on provided text:
            text?.let { Word(newUID, it, id, Word.USER) }
        )
    }

    companion object {
        val DELIMITER: String = "&"
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase? {
            return instance ?:
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "AppDatabase")
                // no migrations created for newer versions -- just remove old stuff
                .fallbackToDestructiveMigration()
                .build()
                .also {
                    instance = it
                }
        }
    }

}


