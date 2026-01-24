package com.tony.appbooster.data.client.pairing

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ADB pairing protocol message types and structures.
 *
 * Based on AOSP `system/core/adb/pairing_connection.cpp`.
 * The pairing protocol uses TLS-encrypted messages with a specific header format.
 */
object AdbPairingProtocol {

    /** Protocol version for ADB pairing. */
    const val PAIRING_PROTOCOL_VERSION = 1

    /** Maximum size of peer info payload. */
    const val MAX_PEER_INFO_SIZE = 8192

    /** Maximum payload size for any message. */
    const val MAX_PAYLOAD_SIZE = 2 * MAX_PEER_INFO_SIZE

    /**
     * Message types in the ADB pairing protocol.
     */
    enum class MessageType(val value: Byte) {
        /** SPAKE2 key exchange message. */
        SPAKE2_MSG(0),

        /** Peer information (certificate) exchange. */
        PEER_INFO(1);

        companion object {
            fun fromValue(value: Byte): MessageType? = entries.find { it.value == value }
        }
    }

    /**
     * Peer info types for certificate exchange.
     */
    enum class PeerInfoType(val value: Byte) {
        /** RSA 2048-bit public key. */
        RSA_2048_PUBLIC_KEY(0),

        /** Device GUID identifier. */
        DEVICE_GUID(1);

        companion object {
            fun fromValue(value: Byte): PeerInfoType? = entries.find { it.value == value }
        }
    }

    /**
     * Header structure for pairing messages.
     *
     * Format (8 bytes):
     * - version: 1 byte
     * - type: 1 byte
     * - payload_size: 4 bytes (big-endian)
     * - reserved: 2 bytes
     */
    data class MessageHeader(
        val version: Byte = PAIRING_PROTOCOL_VERSION.toByte(),
        val type: MessageType,
        val payloadSize: Int
    ) {
        fun toBytes(): ByteArray {
            return ByteBuffer.allocate(HEADER_SIZE)
                .order(ByteOrder.BIG_ENDIAN)
                .put(version)
                .put(type.value)
                .putInt(payloadSize)
                .putShort(0) // reserved
                .array()
        }

        companion object {
            const val HEADER_SIZE = 8

            fun fromBytes(data: ByteArray): MessageHeader {
                require(data.size >= HEADER_SIZE) { "Invalid header size" }

                val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
                val version = buffer.get()
                val typeValue = buffer.get()
                val payloadSize = buffer.getInt()

                val type = MessageType.fromValue(typeValue)
                    ?: throw IllegalArgumentException("Unknown message type: $typeValue")

                return MessageHeader(version, type, payloadSize)
            }
        }
    }

    /**
     * Peer info structure for certificate exchange.
     *
     * Format:
     * - type: 1 byte
     * - data: variable length
     */
    data class PeerInfo(
        val type: PeerInfoType,
        val data: ByteArray
    ) {
        fun toBytes(): ByteArray {
            return ByteBuffer.allocate(1 + data.size)
                .put(type.value)
                .put(data)
                .array()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PeerInfo
            return type == other.type && data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }

        companion object {
            fun fromBytes(data: ByteArray): PeerInfo {
                require(data.isNotEmpty()) { "Empty peer info data" }

                val type = PeerInfoType.fromValue(data[0])
                    ?: throw IllegalArgumentException("Unknown peer info type: ${data[0]}")

                return PeerInfo(type, data.copyOfRange(1, data.size))
            }
        }
    }

    /**
     * Creates a complete pairing message with header and payload.
     */
    fun createMessage(type: MessageType, payload: ByteArray): ByteArray {
        val header = MessageHeader(type = type, payloadSize = payload.size)
        return header.toBytes() + payload
    }
}
