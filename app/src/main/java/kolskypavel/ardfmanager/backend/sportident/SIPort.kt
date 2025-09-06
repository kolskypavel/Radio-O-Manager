package kolskypavel.ardfmanager.backend.sportident

import android.util.Log
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.sportident.SIConstants.GET_SI_CARD8_9_SIAC
import kolskypavel.ardfmanager.backend.sportident.SIConstants.GET_SYSTEM_INFO
import kolskypavel.ardfmanager.backend.sportident.SIConstants.READ_WRITE_TIMEOUT
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD10_11_SIAC_MAX_PUNCHES
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD10_11_SIAC_SERIES
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD5
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD6
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD8_9_SIAC
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD8_MAX_PUNCHES
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD8_SERIES
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD9_MAX_PUNCHES
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD9_SERIES
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD_10_11_SIAC_MIN_NUMBER
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD_PCARD_MAX_PUNCHES
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD_PCARD_SERIES
import kolskypavel.ardfmanager.backend.sportident.SIConstants.SI_CARD_REMOVED
import kolskypavel.ardfmanager.backend.sportident.SIConstants.ZERO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Integer.min
import java.time.DateTimeException
import java.time.LocalTime
import kotlin.experimental.and

class SIPort(
    private val port: UsbSerialDevice,
    private val dataProcessor: DataProcessor = DataProcessor.get()
) {

    private val msgCache: ArrayList<ByteArray> = ArrayList()
    private var extendedMode =
        false                            //  Marks if station uses SI extended mode
    private var serialNo: Int = 0                           //  Serial number of the SI station
    private var lastReadCardId: Int? = null

    /**
     * Stores the temp readout data
     */
    data class CardData(
        var cardType: Byte,
        var siNumber: Int,
        var checkTime: SITime? = null,
        var startTime: SITime? = null,
        var finishTime: SITime? = null,
        var punchData: ArrayList<PunchData>
    )

    /**
     * Stores the temp punch data
     */
    data class PunchData(var siCode: Int, var siTime: SITime)

    fun workJob(): Job {
        val job = CoroutineScope(Dispatchers.IO).launch {

            while (true) {
                delay(SIConstants.PERIOD)

                if (dataProcessor.currentState.value!!.siReaderState.status == SIReaderStatus.DISCONNECTED && probeDevice()) {
                    setStatusConnected()
                }
                if (dataProcessor.currentState.value!!.siReaderState.status != SIReaderStatus.DISCONNECTED
                    && dataProcessor.currentState.value!!.currentRace != null
                ) {
                    readCardOnce(dataProcessor.currentState.value!!.currentRace!!)
                }
            }
        }
        return job
    }


    private fun waitForCardInsert(cardData: CardData): Boolean {

        val reply: ByteArray? = readMsg(READ_WRITE_TIMEOUT)
        if (reply != null && reply.isNotEmpty()) {
            when (reply[1]) {
                SI_CARD5, SI_CARD6, SI_CARD8_9_SIAC -> {
                    cardData.siNumber =
                        (byteToUnsignedInt(reply[6]) shl 16) + (byteToUnsignedInt(reply[7]) shl 8) + byteToUnsignedInt(
                            reply[8]
                        )
                    cardData.cardType = reply[1]
                    Log.d("SI", "Got card inserted event (CardID: " + cardData.siNumber + ")")
                    return true
                }

                SI_CARD_REMOVED -> {
                    val tmpCardId =
                        (byteToUnsignedInt(reply[5]) shl 24) + (byteToUnsignedInt(reply[6]) shl 16) + (byteToUnsignedInt(
                            reply[7]
                        ) shl 8) + byteToUnsignedInt(reply[8])
                }

                else -> Log.d("SI", "Got unknown command waiting for card inserted event")
            }
        }
        return false
    }

    /**
     * Attempts to read out the data from the SI card, based on the cardType
     */
    private suspend fun readCardOnce(race: Race) {
        val cardData = CardData(ZERO, 0, punchData = ArrayList())
        var valid = false

        if (waitForCardInsert(cardData)) {
            setStatusReading(cardData.siNumber)

            when (cardData.cardType) {
                SI_CARD5 -> valid = card5Readout(cardData)
                SI_CARD6 -> valid = card6Readout(cardData)
                SI_CARD8_9_SIAC -> valid = card89SiacReadout(cardData)
            }

            //If the readout was valid, process the data further
            if (valid) {
                if (dataProcessor.processCardData(cardData, race) == true) {
                    lastReadCardId = cardData.siNumber
                    setStatusRead(cardData.siNumber)
                } else {
                    setStatusConnected()
                }
            } else {
                setStatusError(cardData.siNumber)
            }
        }
    }

    /**
     * Write the given message using the SI protocol
     */
    private fun writeMsg(command: Byte, data: ByteArray?, extended: Boolean): Int {
        val dataLen = data?.size ?: 0

        val size: Int = if (extended) {
            dataLen + 7
        } else {
            dataLen + 4
        }

        val buffer = ByteArray(size)
        buffer[0] = SIConstants.WAKEUP
        buffer[1] = SIConstants.STX
        buffer[2] = command

        if (extended) {
            buffer[3] = dataLen.toByte()
            data?.let { System.arraycopy(it, 0, buffer, 4, it.size) }
            val crc = calcSICrc(dataLen + 2, buffer.copyOfRange(2, buffer.size))
            buffer[dataLen + 4] = (crc and 0xff00 shr 8).toByte()
            buffer[dataLen + 5] = (crc and 0xff).toByte()
            buffer[dataLen + 6] = SIConstants.ETX
        } else {
            data?.let { System.arraycopy(it, 0, buffer, 3, it.size) }
            buffer[dataLen + 3] = SIConstants.ETX
        }

        val writtenBytes = port.syncWrite(buffer, READ_WRITE_TIMEOUT)
        return if (writtenBytes == buffer.size) 0 else -1
    }

    private fun writeAck(): Int {
        val buffer = ByteArray(4)
        buffer[0] = 0xff.toByte()
        buffer[1] = SIConstants.STX
        buffer[2] = SIConstants.ACK
        buffer[3] = SIConstants.ETX

        val writtenBytes = port.syncWrite(buffer, READ_WRITE_TIMEOUT)
        return if (writtenBytes == buffer.size) 0 else -1
    }

    fun writeNak(): Int {
        val buffer = ByteArray(4)
        buffer[0] = 0xff.toByte()
        buffer[1] = SIConstants.STX
        buffer[2] = SIConstants.NAK
        buffer[3] = SIConstants.ETX
        val writtenBytes = port.syncWrite(buffer, READ_WRITE_TIMEOUT)
        return if (writtenBytes == buffer.size) 0 else -1
    }

    private fun enqueueCache(buffer: ByteArray) {
        msgCache.add(buffer)
    }


    private fun readMsg(timeout: Int): ByteArray? {
        return this.readMsg(timeout, ZERO)
    }

    private fun readMsg(timeout: Int, filter: Byte): ByteArray? {
        var bufferSize = 0
        var tmpBuffer: ByteArray? = dequeueCache(filter)
        var tmpBufferIndex = 0
        var bytesRead = 0
        var eof = false
        var dle = false

        //Check for cached message
        if (tmpBuffer != null) {
            return tmpBuffer
        }

        // Try to read all bytes from port
        val buffer = ByteArray(4096)
        tmpBuffer = ByteArray(4096)
        do {
            if (tmpBufferIndex >= bytesRead) {
                bytesRead = port.syncRead(tmpBuffer, timeout)
                if (bytesRead <= 0) {
                    break
                }
                tmpBufferIndex = 0
            }
            val incByte = tmpBuffer[tmpBufferIndex++]
            if (!(bufferSize == 0 && incByte == 0xff.toByte()) &&
                !(bufferSize == 0 && incByte == ZERO) &&
                !(bufferSize == 1 && incByte == SIConstants.STX)
            ) {
                buffer[bufferSize++] = incByte
            }

            // Check if we have received a NAK
            if (bufferSize == 1 && incByte == SIConstants.NAK) {
                eof = true
            }

            // If we have got to message type
            if (bufferSize > 1) {
                // If the command is in extended range
                if (byteToUnsignedInt(buffer[1]) > 0x80) {
                    if (bufferSize > 2 && bufferSize >= byteToUnsignedInt(buffer[2]) + 6) {
                        eof = true
                        // TODO check crc
                    }
                } else {
                    // If last char was a DLE, just continue
                    if (dle) {
                        dle = false
                    } else if (incByte == SIConstants.DLE) {
                        dle = true
                    } else if (incByte == SIConstants.ETX) {
                        eof = true
                    }
                }
            }

            // Check if message should be cached
            if (eof && filter != ZERO && bufferSize > 1 && filter != buffer[1]) {
                enqueueCache(buffer.copyOfRange(0, bufferSize))
                eof = false
                dle = false
                bufferSize = 0
            }
        } while (!eof)
        return if (eof && bufferSize > 0) {
            buffer.copyOfRange(0, bufferSize)
        } else {
            null
        }
    }

    private fun dequeueCache(filter: Byte): ByteArray? {
        for (i in msgCache.indices) {
            if (filter == ZERO || msgCache[i][1] == filter) {
                return msgCache.removeAt(i)
            }
        }
        return null
    }

    private fun byteToUnsignedInt(inByte: Byte): Int {
        return inByte.toInt() and 0xff
    }

    private fun probeDevice(): Boolean {
        var ret = false
        var msg: ByteArray
        var reply: ByteArray?

        //Set the serial ports parameters
        port.syncOpen()
        port.setDataBits(UsbSerialInterface.DATA_BITS_8)
        port.setParity(UsbSerialInterface.PARITY_NONE)
        port.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)

        // Start with determining baud rate
        port.setBaudRate(SIConstants.BAUDRATE_HIGH)

        msg = byteArrayOf(0x4d)
        writeMsg(0xf0.toByte(), msg, true)
        reply = readMsg(1000, 0xf0.toByte())

        if (reply == null || reply.isEmpty()) {
            Log.d("SI", "No response on high baud rate mode, trying low baud rate")
            port.setBaudRate(SIConstants.BAUDRATE_LOW)
        }

        writeMsg(0xf0.toByte(), msg, true)
        reply = readMsg(1000, 0xf0.toByte())

        if (reply != null && reply.isNotEmpty()) {
            Log.d("SI", "Unit responded, reading device info")
            msg = byteArrayOf(ZERO, 0x75)
            writeMsg(GET_SYSTEM_INFO, msg, true)
            reply = readMsg(6000, GET_SYSTEM_INFO)!!

            //Long info response
            if (reply.size >= 124) {
                Log.d("SI", "Got device info response")

                extendedMode =
                    (reply[122] and SIConstants.EXTENDED_MODE).compareTo(SIConstants.EXTENDED_MODE) == 0
                serialNo =
                    ((byteToUnsignedInt(reply[6]) shl 24) + (byteToUnsignedInt(reply[7]) shl 16)
                            + (byteToUnsignedInt(reply[8]) shl 8) + byteToUnsignedInt(reply[9]))
                ret = true
            }

            //Short info response
            else {
                Log.d("SI", "Invalid device info response, trying short info")

                msg = byteArrayOf(ZERO, 0x07)
                writeMsg(GET_SYSTEM_INFO, msg, extendedMode)
                reply = readMsg(6000, GET_SYSTEM_INFO)

                if (reply != null && reply.size >= 10) {
                    Log.d("SI", "Got device info response")

                    extendedMode = false
                    serialNo =
                        ((byteToUnsignedInt(reply[6]) shl 24) + (byteToUnsignedInt(reply[7]) shl 16) + (byteToUnsignedInt(
                            reply[8]
                        ) shl 8) + byteToUnsignedInt(reply[9]))
                    ret = true
                }
            }
        }

        if (!ret) {
            port.syncClose()
        }
        return ret
    }

    /**
     * This function calculates the SportIdent CRC
     */
    private fun calcSICrc(uiCount: Int, pucDat: ByteArray): Int {
        var uiTmp1: Int
        var uiVal: Int
        var pucDatIndex = 0
        if (uiCount < 2) return 0
        uiTmp1 = pucDat[pucDatIndex++].toInt()
        uiTmp1 = (uiTmp1 shl 8) + pucDat[pucDatIndex++]
        if (uiCount == 2) return uiTmp1
        for (iTmp in uiCount shr 1 downTo 1) {
            if (iTmp > 1) {
                uiVal = pucDat[pucDatIndex++].toInt()
                uiVal = (uiVal shl 8) + pucDat[pucDatIndex++]
            } else {
                if (uiCount and 1 == 1) {
                    uiVal = pucDat[pucDatIndex].toInt()
                    uiVal = uiVal shl 8
                } else {
                    uiVal = 0
                }
            }
            for (uiTmp in 0..15) {
                if (uiTmp1 and 0x8000 == 0x8000) {
                    uiTmp1 = uiTmp1 shl 1
                    if (uiVal and 0x8000 == 0x8000) uiTmp1++
                    uiTmp1 = uiTmp1 xor SIConstants.POLYNOM
                } else {
                    uiTmp1 = uiTmp1 shl 1
                    if (uiVal and 0x8000 == 0x8000) uiTmp1++
                }
                uiVal = uiVal shl 1
            }
        }
        return uiTmp1 and 0xffff
    }

    private fun card5Readout(cardData: CardData): Boolean {

        writeMsg(SIConstants.GET_SI_CARD5, null, extendedMode)
        val reply: ByteArray? = readMsg(5000, SIConstants.GET_SI_CARD5)
        if (reply != null && card5EntryParse(reply, cardData)) {
            writeAck()
            return true
        }
        return false
    }

    private fun card5EntryParse(data: ByteArray, cardData: CardData): Boolean {
        var ret = false
        var offset = 0
        if (data.size == 136) {
            // Start at data part
            offset += 5

            // Get card number
            if (data[offset + 6] == ZERO || data[offset + 6].toInt() == 0x01) {
                cardData.siNumber =
                    (byteToUnsignedInt(data[offset + 4]) shl 8) + byteToUnsignedInt(
                        data[offset + 5]
                    )
            } else if (byteToUnsignedInt(data[offset + 6]) < 5) {
                cardData.siNumber =
                    byteToUnsignedInt(data[offset + 6]) * 100000 + (byteToUnsignedInt(
                        data[offset + 4]
                    ) shl 8) + byteToUnsignedInt(data[offset + 5])

            } else {
                cardData.siNumber =
                    (byteToUnsignedInt(data[offset + 6]) shl 16) + (byteToUnsignedInt(
                        data[offset + 4]
                    ) shl 8) + byteToUnsignedInt(data[offset + 5])
            }

            cardData.startTime = SITime(
                ((byteToUnsignedInt(data[offset + 19]) shl 8) + byteToUnsignedInt(
                    data[offset + 20]
                )).toLong()
            )
            cardData.finishTime = SITime(
                ((byteToUnsignedInt(data[offset + 21]) shl 8) + byteToUnsignedInt(
                    data[offset + 22]
                )).toLong()
            )
            cardData.checkTime = SITime(
                ((byteToUnsignedInt(data[offset + 25]) shl 8) + byteToUnsignedInt(
                    data[offset + 26]
                )).toLong()
            )
            val punchCount = byteToUnsignedInt(data[offset + 23]) - 1
            run {
                var i = 0
                while (i < punchCount && i < 30) {
                    val punch = PunchData(0, SITime())
                    val baseOffset = offset + 32 + i / 5 * 16 + 1 + 3 * (i % 5)
                    punch.siCode = byteToUnsignedInt(data[baseOffset])
                    punch.siTime = SITime(
                        ((byteToUnsignedInt(data[baseOffset + 1]) shl 8) + byteToUnsignedInt(
                            data[baseOffset + 2]
                        )).toLong()
                    )
                    cardData.punchData.add(punch)
                    i++
                }
            }
            //Read additional punches
            for (i in 30 until punchCount) {
                val punch = PunchData(0, SITime())
                val baseOffset = offset + 32 + (i - 30) * 16
                punch.siCode = data[baseOffset].toInt()
                punch.siTime = SITime()
                cardData.punchData.add(punch)
            }
            ret = true
        }
        return ret
    }

    private fun card6Readout(cardData: CardData): Boolean {

        val reply = ByteArray(7 * 128)
        val msg = byteArrayOf(ZERO)
        val blocks = byteArrayOf(0, 6, 7, 2, 3, 4, 5)
        var i = 0
        while (i < 7) {

            msg[0] = blocks[i]
            writeMsg(SIConstants.GET_SI_CARD6, msg, extendedMode)
            val tmpReply: ByteArray? = readMsg(5000, SIConstants.GET_SI_CARD6)
            if (tmpReply == null || tmpReply.size != 128 + 6 + 3) {
                return false
            }
            System.arraycopy(tmpReply, 6, reply, i * 128, 128)
            if (i > 0) {
                if (tmpReply[124] == 0xee.toByte() && tmpReply[125] == 0xee.toByte() &&
                    tmpReply[126] == 0xee.toByte() && tmpReply[127] == 0xee.toByte()
                ) {
                    break   // Stop reading, no more punches
                }
            }
            i++
        }
        if (card6EntryParse(reply, cardData)) {
            writeAck()
            return true
        }
        return false
    }

    private fun card6EntryParse(data: ByteArray, cardData: CardData): Boolean {
        cardData.siNumber =
            byteToUnsignedInt(data[10]) shl 24 or (byteToUnsignedInt(data[11]) shl 16) or (byteToUnsignedInt(
                data[12]
            ) shl 8) or byteToUnsignedInt(data[13])

        //Parse the special punches
        val checkPunch = parseNewPunch(data.copyOfRange(28, 32))
        val startPunch = parseNewPunch(data.copyOfRange(24, 28))
        val finishPunch = parseNewPunch(data.copyOfRange(20, 24))

        if (checkPunch != null) {
            cardData.checkTime = checkPunch.siTime
        }

        if (startPunch != null) {
            cardData.startTime = startPunch.siTime
        }

        if (finishPunch != null) {
            cardData.finishTime = finishPunch.siTime
        }

        //Parse the regular punches
        val punches: Int = min(data[18].toInt(), 192)       //TODO: verify

        for (i in 0 until punches) {
            val tmpPunchData = parseNewPunch(
                data.copyOfRange(128 + 4 * i, 128 + 4 * i + 4)
            )

            if (tmpPunchData != null) {
                cardData.punchData.add(tmpPunchData)
            } else {
                //Failed to parse a punchData
                return false
            }
        }

        return true
    }

    private fun card89SiacReadout(cardData: CardData): Boolean {

        //Request the first block with service data
        val msg = byteArrayOf(ZERO)
        val reply: ByteArray

        writeMsg(GET_SI_CARD8_9_SIAC, msg, extendedMode)
        var tmpReply: ByteArray? = readMsg(5000, GET_SI_CARD8_9_SIAC)

        if (tmpReply == null || tmpReply.size != 128 + 6 + 3) {
            return false
        }

        //Proceed with punch data blocks
        var nextBlock = 1
        var blockCount = 1

        //Check if the card is SIAC - if so, adjust the blocks size
        if (cardData.siNumber >= SI_CARD_10_11_SIAC_MIN_NUMBER) {
            nextBlock = 4
            blockCount = 7       //(tmpReply[22] + 31) / 32
        }
        reply = ByteArray(128 * (1 + blockCount))
        System.arraycopy(tmpReply, 6, reply, 0, 128)

        var i = nextBlock

        //Read the punch data blocks from the device
        while (i <= blockCount) {
            msg[0] = i.toByte() //Request a block by number
            writeMsg(GET_SI_CARD8_9_SIAC, msg, extendedMode)
            tmpReply = readMsg(5000, GET_SI_CARD8_9_SIAC)

            if (tmpReply == null || tmpReply.size != 128 + 6 + 3) {
                // EMIT card read failed
                return false
            }
            System.arraycopy(tmpReply, 6, reply, (i - nextBlock + 1) * 128, 128)
            i++
        }

        //Parse the punchData and return status
        return if (card9EntryParse(reply, cardData)) {
            writeAck()
            true
        } else {
            false
        }
    }

    private fun card9EntryParse(data: ByteArray, cardData: CardData): Boolean {
        cardData.siNumber =
            byteToUnsignedInt(data[25]) shl 16 or (byteToUnsignedInt(data[26]) shl 8) or byteToUnsignedInt(
                data[27]
            )

        val series = data[24].toInt() and SI_CARD10_11_SIAC_SERIES

        //Parse the special punches
        val checkPunch = parseNewPunch(data.copyOfRange(8, 12))
        val startPunch = parseNewPunch(data.copyOfRange(12, 16))
        val finishPunch = parseNewPunch(data.copyOfRange(16, 20))

        if (checkPunch != null) {
            cardData.checkTime = checkPunch.siTime
        }

        if (startPunch != null) {
            cardData.startTime = startPunch.siTime
        }

        if (finishPunch != null) {
            cardData.finishTime = finishPunch.siTime
        }

        when (series) {
            SI_CARD8_SERIES -> {
                //Determine number of punches by reading the pointer
                val punches: Int = min(
                    data[22].toInt(),
                    SI_CARD8_MAX_PUNCHES
                )

                //Go through all the punches and parse them
                for (i in 0 until punches) {
                    val tmpPunch = parseNewPunch(
                        data.copyOfRange(34 * 4 + 4 * i, 34 * 4 + 4 * i + 4)
                    )
                    if (tmpPunch != null) {
                        cardData.punchData.add(tmpPunch)
                    } else {
                        return false
                    }
                }
            }

            SI_CARD9_SERIES -> {
                //Determine number of punches by reading the pointer
                val punches: Int = min(
                    data[22].toInt(),
                    SI_CARD9_MAX_PUNCHES
                )

                //Go through all the punches and parse them
                for (i in 0 until punches) {
                    val tmpPunch = parseNewPunch(
                        data.copyOfRange(14 * 4 + 4 * i, 14 * 4 + 4 * i + 4)
                    )
                    if (tmpPunch != null) {
                        cardData.punchData.add(tmpPunch)
                    } else {
                        return false
                    }
                }
            }

            SI_CARD_PCARD_SERIES -> {
                //Determine number of punches by reading the pointer
                val punches: Int = min(
                    data[22].toInt(),
                    SI_CARD_PCARD_MAX_PUNCHES
                )

                //Go through all the punches and parse them
                for (i in 0 until punches) {
                    val tmpPunch = parseNewPunch(
                        data.copyOfRange(44 * 4 + 4 * i, 44 * 4 + 4 * i + 4)
                    )
                    if (tmpPunch != null) {
                        cardData.punchData.add(tmpPunch)
                    } else {
                        return false
                    }
                }
            }

            SI_CARD10_11_SIAC_SERIES -> {

                //Determine number of punches by reading the pointer
                val punches: Int = min(
                    data[22].toInt(),
                    SI_CARD10_11_SIAC_MAX_PUNCHES
                )

                //Go through all the punches and parse them
                for (i in 0 until punches) {
                    val tmpPunch: PunchData? =
                        parseNewPunch(
                            data.copyOfRange(128 + 4 * i, 128 + 4 * i + 4)
                        )

                    if (tmpPunch != null) {
                        cardData.punchData.add(tmpPunch)
                    } else {
                        return false

                    }
                }
            }
        }

        return true
    }

    /**
     * Parses the given data into a PunchData object
     * @param data - The data read from the SI card
     * @return Parsed PunchData object, or null if invalid
     */
    private fun parseNewPunch(data: ByteArray): PunchData? {
        val punchData = PunchData(0, SITime())

        if (data[0] == 0xee.toByte() && data[1] == 0xee.toByte() && data[2] == 0xee.toByte() && data[3] == 0xee.toByte()) {
            return null
        }
        punchData.siCode =
            byteToUnsignedInt(data[1]) + 256 * (byteToUnsignedInt(data[0]) shr 6 and 0x03)
        punchData.siTime.setDayOfWeek((data[0].toInt() shr 1 and 0x07))

        //TODO: Set week
        // punchData.siTime.setWeek(byteToUnsignedInt(data[0]) shr 4)

        //Read and parse the seconds
        val seconds =
            (byteToUnsignedInt(data[2]) shl 8 or byteToUnsignedInt(data[3])).toLong()

        try {
            punchData.siTime.setTime(LocalTime.ofSecondOfDay(seconds))
        } catch (dateE: DateTimeException) {
            Log.d(
                "SI",
                "Invalid punch time - Code: ${punchData.siCode} Seconds: $seconds"
            )
            return null
        }

        //Check the HALF_DAY offset
        if (data[0].toInt() and 0x01 == 0x01) {
            punchData.siTime.addHalfDay()
        }
        return punchData
    }

    // STATUS BAR
    private fun setStatusConnected() {
        dataProcessor.updateReaderState(
            SIReaderState(
                SIReaderStatus.CONNECTED,
                serialNo,
                null, lastReadCardId
            )
        )
    }

    private fun setStatusReading(cardNo: Int) {
        dataProcessor.updateReaderState(
            SIReaderState(
                SIReaderStatus.READING,
                serialNo,
                cardNo, lastReadCardId
            )
        )
    }

    private fun setStatusError(cardNo: Int) {
        dataProcessor.updateReaderState(
            SIReaderState(
                SIReaderStatus.ERROR,
                serialNo,
                cardNo, lastReadCardId
            )
        )
    }

    private fun setStatusRead(cardNo: Int) {
        dataProcessor.updateReaderState(
            SIReaderState(
                SIReaderStatus.CARD_READ,
                serialNo,
                cardNo, lastReadCardId
            )
        )
    }
}