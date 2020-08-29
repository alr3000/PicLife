package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.database.Cursor
import android.net.Uri
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



//todo: instead of uid's, use hash for everything

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

    @Query("SELECT * FROM resource WHERE uid IN (:resourceIds)")
    fun listAllByIds(resourceIds: IntArray): List<Resource>

    @Query("SELECT * FROM resource WHERE uid LIKE :text")
    fun listAllUriContains(text: String): List<Resource>

    @Query("SELECT * FROM resource WHERE uid LIKE :text")
    fun getAllUriContains(text: String): Cursor?

    // todo: paging
    @Query("SELECT * FROM resource WHERE resource_type IN (:types)")
    fun getAllByType(types: Array<String>) : LiveData<List<Resource>?>?

    @Query("SELECT * FROM resource WHERE uid IN (:ids)")
    fun getLiveById(ids: IntArray) : List<Resource>?

    @Query("SELECT * FROM resource WHERE uid IN (:ids)")
    fun getAllById(ids: IntArray) : Cursor?


    @Query("SELECT * FROM resource WHERE resource_type=:type AND uid IN (:ids)")
    fun getAllTypeById(type: String, ids: IntArray) : List<Resource>?

    @Query("SELECT * FROM resource WHERE uid=:id LIMIT 1")
    fun get(id: Int): Cursor?


    @Insert
    fun insertAll(vararg resources: Resource)

    @Delete
    fun delete(resource: Resource)
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



@Database(entities = arrayOf(Word::class, Resource::class, Recent::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun resourceDao(): ResourceDao
    abstract fun recentDao(): RecentDao



    val newUID: Int
        get() = (Math.random() * Int.MAX_VALUE).toInt()

  /*  // todo: make single queries for complex data that result in livedata lists
    fun getRecentButtons() : List<Resource>? {
        return recentDao().getAllSince(Recent.ActionType.CLEAR_RECENTS.ordinal)
            .map { it.resourceId }
            .filter { it != 0 }
            .toIntArray()
            .let { resourceDao().getLiveById(it)}
    }*/

    fun getLiveResourcesByWord(word: String) : List<Resource>? {
        return wordDao().listByText(word)
            .map { it.resourceId }
            .toIntArray()
            .let { resourceDao().getLiveById(it) }
    }

    fun getResourcesByWord(word: String) : Cursor? {
        return wordDao().listByText(word)
            .map { it.resourceId }
            .toIntArray()
            .let { resourceDao().getAllById(it) }
    }

    //todo: these should just be "create resource entities" then "create word entities(res[], text[])"
    // for each level
    fun enterKeyboard(pages: List<PageData>, uri: Uri?, name: String?) {
        val entries = mutableListOf<Any?>()
        val keyboardId = uri?.hashCode() ?: newUID

        entries.addAll(createPageEntries(pages))

        entries.add(Resource(
            keyboardId,
            Resource.Type.KEYBOARD.name,
            uri?.toString() ?: "",
            title = name ?: "",
            children = entries.filterIsInstance<Resource>()
                .filter { it.resourceType == Resource.Type.PAGE.name }
                .map { it.uid }.joinToString()
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
            .also { if (it.isNotEmpty()) resourceDao().insertAll(*it.toTypedArray()) }


    }

    fun createPageEntries(pages: List<PageData>) : Array<Any?> {
        val entries = mutableListOf<Any>()
        pages
            .forEach { page ->
                val pageId = newUID

                // create entries for buttons in page:
                entries.addAll(page.icons.flatMap { createIconEntries(it) })

                // add page as a resource, associated with children from above:
                entries.add(Resource(
                    uid = pageId,
                    title = page.name ?: "",
                    resourceType = Resource.Type.PAGE.name,
                    children = entries.filterIsInstance<Resource>()
                        .filter { it.resourceType == Resource.Type.BUTTON.name }
                        .map { it.uid }.joinToString()
                )
                )

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
                    *(icon.text?.let { text ->
                        arrayOf(
                            Word(newUID, text, buttonId, Word.DERIVED)
                        )
                    } ?: arrayOf())

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
        var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase? {
            return instance ?:
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "AppDatabase")
                    .build()
                    .also { instance = it }
        }
    }

}


