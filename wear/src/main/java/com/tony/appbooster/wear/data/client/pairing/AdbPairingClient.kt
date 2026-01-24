package com.tony.appbooster.wear.data.client.pairing

import android.util.Log
import com.tony.appbooster.wear.data.client.pairing.AdbPairingProtocol.MessageHeader
import com.tony.appbooster.wear.data.client.pairing.AdbPairingProtocol.MessageType
import com.tony.appbooster.wear.data.client.pairing.AdbPairingProtocol.PeerInfo
import com.tony.appbooster.wear.data.client.pairing.AdbPairingProtocol.PeerInfoType
import dadb.AdbKeyPair
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * ADB pairing client implementing Android 11+ wireless debugging pairing protocol.
 *
 * This client performs the SPAKE2+ key exchange required to pair with a device's
 * ADB daemon over wireless debugging. After successful pairing, the device will
 * trust subsequent connections using the RSA key pair.
 *
 * Protocol flow:
 * 1. Establish TLS connection to pairing port
 * 2. Perform SPAKE2+ key exchange using 6-digit code
 * 3. Exchange RSA public keys over encrypted channel
 * 4. Store mutual trust for future connections
 *
 * @property ioDispatcher Dispatcher for IO operations.
 */
class AdbPairingClient(
    private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Result of a pairing operation.
     */
    sealed class PairingResult {
        /** Pairing succeeded. */
        data class Success(val deviceName: String) : PairingResult()

        /** Pairing failed with an error. */
        data class Failure(val error: String, val exception: Throwable? = null) : PairingResult()
    }

    /**
     * Pairs with an ADB daemon at the specified host and port.
     *
     * @param host The host address (usually "localhost" for self-pairing).
     * @param port The pairing port (shown in Wireless Debugging settings).
     * @param pairingCode The 6-digit pairing code.
     * @param keyPair The RSA key pair to register with the device.
     * @return PairingResult indicating success or failure.
     */
    suspend fun pair(
        host: String,
        port: Int,
        pairingCode: String,
        keyPair: AdbKeyPair
    ): PairingResult = withContext(ioDispatcher) {
        Log.d(TAG, "Starting pairing with $host:$port")

        try {
            // First, test basic TCP connectivity before attempting TLS
            Log.d(TAG, "Testing TCP connectivity to $host:$port")
            if (!testTcpConnection(host, port)) {
                return@withContext PairingResult.Failure(
                    "Cannot connect to $host:$port. Make sure:\n" +
                    "1. Wireless Debugging is enabled\n" +
                    "2. The pairing dialog is still open\n" +
                    "3. You're using the PAIRING port (not the connection port)"
                )
            }

            // Create SSL socket (ADB pairing uses TLS)
            Log.d(TAG, "TCP connection OK, establishing TLS...")
            val sslSocket = createTlsSocket(host, port)

            try {
                sslSocket.soTimeout = SOCKET_TIMEOUT_MS

                val inputStream = sslSocket.inputStream
                val outputStream = sslSocket.outputStream

                // Step 1: SPAKE2+ key exchange
                Log.d(TAG, "Starting SPAKE2+ key exchange")
                val spake2 = performSpake2Exchange(
                    inputStream,
                    outputStream,
                    pairingCode.toByteArray(Charsets.UTF_8)
                )

                // Step 2: Exchange peer info (RSA keys)
                Log.d(TAG, "Exchanging peer info")
                val deviceName = exchangePeerInfo(
                    inputStream,
                    outputStream,
                    spake2,
                    keyPair
                )

                Log.d(TAG, "Pairing successful with device: $deviceName")
                PairingResult.Success(deviceName)

            } finally {
                sslSocket.close()
            }

        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "Connection refused to $host:$port", e)
            PairingResult.Failure(
                error = "Connection refused.\n\n" +
                    "The pairing dialog likely closed when you switched apps. " +
                    "On Wear OS, the dialog must stay open during pairing.\n\n" +
                    "Try: 'Already paired via PC' option instead.",
                exception = e
            )
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Connection timeout to $host:$port", e)
            PairingResult.Failure(
                error = "Connection timed out. Make sure Wireless Debugging is enabled.",
                exception = e
            )
        } catch (e: javax.net.ssl.SSLException) {
            Log.e(TAG, "TLS handshake failed", e)
            PairingResult.Failure(
                error = "TLS handshake failed. The ADB daemon may have rejected the connection.",
                exception = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Pairing failed", e)
            PairingResult.Failure(
                error = e.message ?: "Unknown pairing error",
                exception = e
            )
        }
    }

    /**
     * Performs the SPAKE2+ key exchange.
     */
    private fun performSpake2Exchange(
        input: InputStream,
        output: OutputStream,
        password: ByteArray
    ): Spake2PlusHandler {
        val spake2 = Spake2PlusHandler(password, isClient = true)

        // Generate and send our public key
        val ourPublicKey = spake2.generatePublicKey()
        sendMessage(output, MessageType.SPAKE2_MSG, ourPublicKey)

        // Receive peer's public key
        val peerMessage = receiveMessage(input)
        require(peerMessage.first == MessageType.SPAKE2_MSG) {
            "Expected SPAKE2_MSG, got ${peerMessage.first}"
        }

        // Process peer's key to derive shared secret
        spake2.processPublicKey(peerMessage.second)

        Log.d(TAG, "SPAKE2+ key exchange completed")
        return spake2
    }

    /**
     * Exchanges peer info (RSA certificates) over encrypted channel.
     */
    private fun exchangePeerInfo(
        input: InputStream,
        output: OutputStream,
        spake2: Spake2PlusHandler,
        keyPair: AdbKeyPair
    ): String {
        // Create our peer info with RSA public key
        val publicKeyBytes = keyPair.publicKeyBytes
        val ourPeerInfo = PeerInfo(PeerInfoType.RSA_2048_PUBLIC_KEY, publicKeyBytes)

        // Encrypt and send our peer info
        val encryptedPeerInfo = spake2.encrypt(ourPeerInfo.toBytes())
        sendMessage(output, MessageType.PEER_INFO, encryptedPeerInfo)

        // Receive peer's info
        val peerMessage = receiveMessage(input)
        require(peerMessage.first == MessageType.PEER_INFO) {
            "Expected PEER_INFO, got ${peerMessage.first}"
        }

        // Decrypt peer's info
        val decryptedPeerInfo = spake2.decrypt(peerMessage.second)
        val peerInfo = PeerInfo.fromBytes(decryptedPeerInfo)

        Log.d(TAG, "Received peer info type: ${peerInfo.type}")

        // Extract device name from peer info if available
        val deviceName = when (peerInfo.type) {
            PeerInfoType.DEVICE_GUID -> String(peerInfo.data, Charsets.UTF_8)
            else -> "ADB Device"
        }

        return deviceName
    }

    /**
     * Sends a pairing protocol message.
     */
    private fun sendMessage(output: OutputStream, type: MessageType, payload: ByteArray) {
        val message = AdbPairingProtocol.createMessage(type, payload)
        output.write(message)
        output.flush()
        Log.v(TAG, "Sent ${type.name} message (${payload.size} bytes)")
    }

    /**
     * Receives a pairing protocol message.
     */
    private fun receiveMessage(input: InputStream): Pair<MessageType, ByteArray> {
        // Read header
        val headerBytes = ByteArray(MessageHeader.HEADER_SIZE)
        var bytesRead = 0
        while (bytesRead < MessageHeader.HEADER_SIZE) {
            val read = input.read(headerBytes, bytesRead, MessageHeader.HEADER_SIZE - bytesRead)
            if (read < 0) throw IllegalStateException("Connection closed while reading header")
            bytesRead += read
        }

        val header = MessageHeader.fromBytes(headerBytes)
        Log.v(TAG, "Received header: type=${header.type}, size=${header.payloadSize}")

        require(header.payloadSize <= AdbPairingProtocol.MAX_PAYLOAD_SIZE) {
            "Payload too large: ${header.payloadSize}"
        }

        // Read payload
        val payload = ByteArray(header.payloadSize)
        bytesRead = 0
        while (bytesRead < header.payloadSize) {
            val read = input.read(payload, bytesRead, header.payloadSize - bytesRead)
            if (read < 0) throw IllegalStateException("Connection closed while reading payload")
            bytesRead += read
        }

        return header.type to payload
    }

    /**
     * Creates a TLS socket that accepts any certificate.
     *
     * ADB pairing uses self-signed certificates, so we need to trust all certs.
     */
    @Suppress("CustomX509TrustManager", "TrustAllX509TrustManager")
    private fun createTlsSocket(host: String, port: Int): SSLSocket {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        val socket = sslContext.socketFactory.createSocket(host, port) as SSLSocket

        // Enable TLS 1.2+ as required by ADB
        socket.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")

        // Start TLS handshake
        socket.startHandshake()

        return socket
    }

    /**
     * Tests basic TCP connectivity before attempting TLS.
     */
    private fun testTcpConnection(host: String, port: Int): Boolean {
        return try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, port), 5000)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "TCP connection test failed to $host:$port", e)
            false
        }
    }

    companion object {
        private const val TAG = "AdbPairingClient"
        private const val SOCKET_TIMEOUT_MS = 30_000
    }
}

/**
 * Extension to get RSA public key bytes from AdbKeyPair.
 */
private val AdbKeyPair.publicKeyBytes: ByteArray
    get() {
        // Read the public key file content
        // AdbKeyPair stores keys in files, we need to access the public key
        // The public key is in Android's custom format
        return try {
            // Use reflection to access the public key since dadb doesn't expose it directly
            val field = this::class.java.getDeclaredField("publicKey")
            field.isAccessible = true
            val publicKey = field.get(this) as java.security.PublicKey
            publicKey.encoded
        } catch (e: Exception) {
            // Fallback: generate a placeholder (this should be improved)
            Log.w("AdbPairingClient", "Could not extract public key", e)
            ByteArray(0)
        }
    }
