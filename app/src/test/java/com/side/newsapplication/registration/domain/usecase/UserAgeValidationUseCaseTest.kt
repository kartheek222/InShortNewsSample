package com.onexp.remag.registration.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Calendar

class UserAgeValidationUseCaseTest {

    lateinit var useCase: UserAgeValidationUseCase

    @BeforeEach
    fun setUp() {
        useCase = UserAgeValidationUseCase()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
    }


    @Test
    fun `validate fields without selecting date of birth`() {
        val result = useCase(null)
        Truth.assertThat(result)
            .isInstanceOf(UserAgeValidationUseCase.AgeValidationResult.EmptyDOB::class.java)
    }

    @Test
    fun `validate fields dob having the current date`() {
        val result = useCase(Calendar.getInstance())
        Truth.assertThat(result)
            .isInstanceOf(UserAgeValidationUseCase.AgeValidationResult.AgeBelow13::class.java)
    }

    @Test
    fun `validate fields dob having age below 13`() {
        val result = useCase(Calendar.getInstance().apply {
            this[Calendar.YEAR] = this[Calendar.YEAR] - 11
        })
        Truth.assertThat(result)
            .isInstanceOf(UserAgeValidationUseCase.AgeValidationResult.AgeBelow13::class.java)
    }

    @Test
    fun `validate fields dob having age below 18`() {
        val result = useCase(Calendar.getInstance().apply {
            this[Calendar.YEAR] = this[Calendar.YEAR] - 17
        })
        Truth.assertThat(result)
            .isInstanceOf(UserAgeValidationUseCase.AgeValidationResult.AgeBelow18::class.java)
    }

    @Test
    fun `validate fields dob having age above 18`() {
        val result = useCase(Calendar.getInstance().apply {
            this[Calendar.YEAR] = this[Calendar.YEAR] - 19
        })
        Truth.assertThat(result)
            .isInstanceOf(UserAgeValidationUseCase.AgeValidationResult.Success::class.java)
    }

}
