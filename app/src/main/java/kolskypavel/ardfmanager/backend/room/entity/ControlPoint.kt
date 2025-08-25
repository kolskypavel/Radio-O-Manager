package kolskypavel.ardfmanager.backend.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kolskypavel.ardfmanager.backend.helpers.ControlPointsHelper.BEACON_CONTROL_MARKER
import kolskypavel.ardfmanager.backend.helpers.ControlPointsHelper.SPECTATOR_CONTROL_MARKER
import kolskypavel.ardfmanager.backend.room.enums.ControlPointType
import java.io.Serializable
import java.util.UUID

/**
 * Control point entity, used to define categories
 */
@Entity(
    tableName = "control_point",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("category_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class ControlPoint(
    @PrimaryKey var id: UUID,
    @ColumnInfo(name = "category_id") var categoryId: UUID,
    @ColumnInfo(name = "si_code") var siCode: Int,
    @ColumnInfo(name = "type") var type: ControlPointType,
    @ColumnInfo(name = "order") var order: Int
) : Serializable {

    fun toCsvString(): String {
        val type = when (type) {
            ControlPointType.BEACON -> BEACON_CONTROL_MARKER
            ControlPointType.SEPARATOR -> SPECTATOR_CONTROL_MARKER
            ControlPointType.CONTROL -> ""
        }
        return "${siCode}${type}"
    }

    constructor() : this(
        UUID.randomUUID(),
        UUID.randomUUID(),
        31,
        ControlPointType.CONTROL,
        0
    )
}