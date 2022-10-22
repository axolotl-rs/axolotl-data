package dev.kingtux.axolotl.data.build.packets

import com.google.gson.Gson
import net.minecraft.core.Vec3i
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

enum class PacketType {
    CLIENTBOUND, SERVERBOUND
}

enum class PacketState {
    HANDSHAKING, STATUS, LOGIN, GAME
}

enum class FieldType {
    INT, BYTE, SHORT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING, VARINT, VARLONG, UUID, POSITION, CHAT, NBT, ROTATION, Unknown, Optional, LIST, ByteArray, ConnectionProtocol, MAP, ResourceLocation;

    companion object {
        fun fromClass(clazz: Class<*>): FieldType {

            return when (clazz) {
                Int::class.java -> {
                    INT
                }

                Byte::class.java -> {
                    BYTE
                }

                Short::class.java -> {
                    SHORT
                }

                Long::class.java -> {
                    LONG
                }

                Float::class.java -> {
                    FLOAT
                }

                Double::class.java -> {
                    DOUBLE
                }

                Boolean::class.java -> {
                    BOOLEAN
                }

                String::class.java -> {
                    STRING
                }

                java.util.UUID::class.java -> {
                    UUID
                }

                net.minecraft.core.BlockPos::class.java -> {
                    POSITION
                }

                Vec3i::class.java -> {
                    POSITION
                }

                net.minecraft.network.chat.Component::class.java -> {
                    CHAT
                }

                net.minecraft.nbt.CompoundTag::class.java -> {
                    NBT
                }

                Gson::class.java -> {
                    STRING
                }

                java.util.Optional::class.java -> {
                    Optional
                }

                net.minecraft.resources.ResourceLocation::class.java -> {
                    ResourceLocation
                }

                FriendlyByteBuf::class.java -> {
                    ByteArray

                }

                net.minecraft.network.ConnectionProtocol::class.java -> {
                    ConnectionProtocol
                }

                else -> {
                    if (clazz.isAssignableFrom(List::class.java)) {
                        return FieldType.LIST
                    } else if (clazz.isAssignableFrom(java.util.Set::class.java)) {
                        return FieldType.LIST
                    } else if (clazz.isAssignableFrom(java.util.Map::class.java)) {
                        return FieldType.MAP

                    } else if (clazz.isArray) {
                        // Check if it is a byte array
                        if (clazz.componentType == Byte::class.java) {
                            return FieldType.ByteArray
                        } else if (clazz.componentType == Int::class.java) {
                            return FieldType.ByteArray

                        } else {
                            return FieldType.Unknown
                        }
                    } else {
                        Unknown
                    }
                }


            }
        }
    }
}

data class PacketField(val name: String, val type: FieldType, val optional: Boolean = false, val list: Boolean = false)
data class PacketData(
    val packetId: Int,
    val name: String,
    val state: PacketState,
    val packetType: PacketType,
    val fields: List<PacketField>

)

class PacketExport {
    private fun buildPacket(
        id: Int, packet: Class<out Packet<*>>, state: PacketState, packetType: PacketType
    ): PacketData {
        val fields = mutableListOf<PacketField>();
        for (it in packet.declaredFields) {
            if (Modifier.isFinal(it.modifiers) && Modifier.isPublic(it.modifiers)) {
                continue
            }
            val field = when (val type = FieldType.fromClass(it.type)) {
                FieldType.Unknown -> {
                    println("Unknown Type ${it.type} for ${it.name} in ${packet.name}")
                    PacketField(it.name, type)
                }

                FieldType.Optional -> {
                    val genericType = it.genericType as ParameterizedType
                    val genericClass = genericType.actualTypeArguments[0] as Class<*>
                    PacketField(it.name, FieldType.fromClass(genericClass), true)
                }

                FieldType.LIST -> {
                    val genericType = it.genericType as ParameterizedType
                    val genericClass = when (genericType.actualTypeArguments[0]) {
                        is Class<*> -> {
                            genericType.actualTypeArguments[0] as Class<*>
                        }

                        is ParameterizedType -> {
                            (genericType.actualTypeArguments[0] as ParameterizedType).rawType as Class<*>
                        }

                        else -> {
                            throw IllegalStateException("Unknown Type ${genericType.actualTypeArguments[0]}")
                        }
                    }
                    PacketField(it.name, FieldType.fromClass(genericClass), list = true)
                }

                else -> PacketField(it.name, type)
            }
            fields.add(field)
        }

        return PacketData(id, packet.simpleName, state, packetType, fields)
    }


    fun getPackets(): List<PacketData> {
        val handshakingClientPackets =
            ConnectionProtocol.HANDSHAKING.getPacketsByIds(PacketFlow.CLIENTBOUND).map { (id, packet) ->
                this.buildPacket(
                    id, packet, PacketState.HANDSHAKING, PacketType.CLIENTBOUND
                )
            }.toList()
        val handshakingServerPackets =
            ConnectionProtocol.HANDSHAKING.getPacketsByIds(PacketFlow.SERVERBOUND).map { (id, packet) ->
                this.buildPacket(
                    id, packet, PacketState.HANDSHAKING, PacketType.SERVERBOUND
                )
            }.toList()
        val gameClientPackets = ConnectionProtocol.PLAY.getPacketsByIds(PacketFlow.CLIENTBOUND).map { (id, packet) ->
            this.buildPacket(
                id, packet, PacketState.GAME, PacketType.CLIENTBOUND
            )
        }.toList()

        val gameServerPackets = ConnectionProtocol.PLAY.getPacketsByIds(PacketFlow.SERVERBOUND).map { (id, packet) ->
            this.buildPacket(
                id, packet, PacketState.GAME, PacketType.SERVERBOUND
            )
        }.toList()

        val loginClientPackets = ConnectionProtocol.LOGIN.getPacketsByIds(PacketFlow.CLIENTBOUND).map { (id, packet) ->
            this.buildPacket(
                id, packet, PacketState.LOGIN, PacketType.CLIENTBOUND
            )
        }.toList()

        val loginServerPackets = ConnectionProtocol.LOGIN.getPacketsByIds(PacketFlow.SERVERBOUND).map { (id, packet) ->
            this.buildPacket(
                id, packet, PacketState.LOGIN, PacketType.SERVERBOUND
            )
        }.toList()

        val statusClientPackets =
            ConnectionProtocol.STATUS.getPacketsByIds(PacketFlow.CLIENTBOUND).map { (id, packet) ->
                this.buildPacket(
                    id, packet, PacketState.STATUS, PacketType.CLIENTBOUND
                )
            }.toList()

        val statusServerPackets =
            ConnectionProtocol.STATUS.getPacketsByIds(PacketFlow.SERVERBOUND).map { (id, packet) ->
                this.buildPacket(
                    id, packet, PacketState.STATUS, PacketType.SERVERBOUND
                )
            }.toList()

        // Merge all the lists
        return handshakingClientPackets + handshakingServerPackets + gameClientPackets + gameServerPackets + loginClientPackets + loginServerPackets + statusClientPackets + statusServerPackets
    }
}