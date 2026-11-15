package com.cherryclip.app

import com.cherryclip.app.model.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeGeneratorTest {

    @Test
    fun generateMatrixProducesValidDimensions() {
        val matrix = QrCodeGenerator.generateMatrix("https://github.com/CherryHQ/cherry-studio-app", 256)
        assertEquals(256, matrix.width)
        assertEquals(256, matrix.height)
        var darkModules = 0
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) darkModules++
            }
        }
        assertTrue(darkModules > 100)
    }
}
