package com.mbr.ampx.utilities

import java.io.ByteArrayOutputStream
import kotlin.math.sin

object COBS {

    fun Encode(src: ByteArray): ByteArray {
        val srcLen = src.size
        var code: Int = 1
        var currentIndex: Int = 0

        val maxEncodingLen = src.size + ((src.size + 1) / 254) + 1
        val sink = ByteArrayOutputStream(maxEncodingLen)

        var writeStartIndex: Int = -1
        val zero = 0.toByte()

        while (currentIndex < srcLen) {
            if (src[currentIndex] == zero) {
                code = finishBlock(code, sink, src, writeStartIndex, currentIndex - 1)
                writeStartIndex = -1
            } else {
                if (writeStartIndex < 0) {
                    writeStartIndex = currentIndex
                }
                code++
                if (code == 0xFF) {
                    code = finishBlock(code, sink, src, writeStartIndex, currentIndex)
                    writeStartIndex = -1
                }
            }
            currentIndex++
        }

        finishBlock(code, sink, src, writeStartIndex, currentIndex - 1)
        sink.write(0)

        return sink.toByteArray()
    }

    private fun finishBlock(code: Int, sink: ByteArrayOutputStream, src: ByteArray, begin: Int, end: Int): Int {
        sink.write(code)
        if (begin > -1) {
            sink.write(src, begin, (end - begin) + 1)
        }
        return 0x01
    }

    fun Decode(src: ByteArray): ByteArray {
        val srcLen = src.size
        var currentIndex = 0
        var code: Int

        val sink = ByteArrayOutputStream()

        while (currentIndex < srcLen) {
            code = (src[currentIndex++].toInt()) and 0xFF
            for (i in 1..code) {
                sink.write(src[currentIndex++].toInt())
                if ((currentIndex < srcLen) && (code < 0xFF)) {
                    sink.write(0)
                }
            }
        }

        return sink.toByteArray()
    }

}