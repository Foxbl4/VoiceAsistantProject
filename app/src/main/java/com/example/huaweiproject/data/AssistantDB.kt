package com.example.huaweiproject.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Assistant::class], version = 1, exportSchema = false)
abstract class AssistantDB: RoomDatabase() {

    abstract val assistantDao: AssistantDao

    companion object{
        @Volatile
        private var INSTANCE: AssistantDB? = null

        fun getInstance(context: Context): AssistantDB {
            synchronized(this){
                var instance = INSTANCE
                if (instance == null){
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AssistantDB::class.java,
                        "assistant_messages_database"
                    ).fallbackToDestructiveMigration().build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }

}