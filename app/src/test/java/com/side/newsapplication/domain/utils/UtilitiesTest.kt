package com.onexp.remag.domain.utils

import android.content.Context
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test


/**
 * Created by kartheek.sabbisetty on 29-01-2024
 */
class UtilitiesTest {

    @Test
    fun testGetDeviceId() {
        val mockedContext = mockk<Context>(relaxed = true)

        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceid"

        assertEquals("FakeDeviceid", Utilities.getDeviceId(mockedContext));
        unmockkStatic(Settings.Secure::class)

    }
}