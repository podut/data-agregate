package com.podut.dataagregate.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interests")
data class InterestEntity(
    @PrimaryKey val name: String,
    val isSelected: Boolean = true
)
