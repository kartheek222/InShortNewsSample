package com.onexp.remag.registration.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PasswordValidationUseCaseTest {

    private lateinit var passwordValidationUseCase: PasswordValidationUseCase

    @BeforeEach
    fun setUp() {
        passwordValidationUseCase = PasswordValidationUseCase()
    }

    @Test
    fun `empty password returns error`() {
        var result = passwordValidationUseCase("")
        assertThat(result).isInstanceOf(PasswordValidationUseCase.PasswordUseCaseResult.Error::class.java)
        assertThat((result as PasswordValidationUseCase.PasswordUseCaseResult.Error).errorType is PasswordValidationUseCase.PasswordErrorType.Empty).isTrue()

        result = passwordValidationUseCase("  ")
        assertThat(result).isInstanceOf(PasswordValidationUseCase.PasswordUseCaseResult.Error::class.java)
        assertThat((result as PasswordValidationUseCase.PasswordUseCaseResult.Error).errorType is PasswordValidationUseCase.PasswordErrorType.Empty).isTrue()
    }

    @Test
    fun `password does not contain uppercase letter returns error`() {
        val result = passwordValidationUseCase("pole@123")
        assertThat(result).isInstanceOf(PasswordValidationUseCase.PasswordUseCaseResult.Error::class.java)
        assertThat((result as PasswordValidationUseCase.PasswordUseCaseResult.Error).errorType is PasswordValidationUseCase.PasswordErrorType.UpperCaseError).isTrue()
    }

    @Test
    fun `password does not contain lowercase letter returns error`() {
        val result = passwordValidationUseCase("POLE@123")
        assertThat(result).isInstanceOf(PasswordValidationUseCase.PasswordUseCaseResult.Error::class.java)
        assertThat((result as PasswordValidationUseCase.PasswordUseCaseResult.Error).errorType is PasswordValidationUseCase.PasswordErrorType.LowerCaseError).isTrue()
    }

    @Test
    fun `password does not contain number returns error`() {
        val result = passwordValidationUseCase("Pole@abc")
        assertThat(result).isInstanceOf(PasswordValidationUseCase.PasswordUseCaseResult.Error::class.java)
        assertThat((result as PasswordValidationUseCase.PasswordUseCaseResult.Error).errorType is PasswordValidationUseCase.PasswordErrorType.NumbersError).isTrue()
    }

    @Test
    fun `password does not contain special character returns error`() {
        val result = passwordValidationUseCase("Pole0123")
        assertThat(result).isInstanceOf(PasswordValidationUseCase.PasswordUseCaseResult.Error::class.java)
        assertThat((result as PasswordValidationUseCase.PasswordUseCaseResult.Error).errorType is PasswordValidationUseCase.PasswordErrorType.SpecialCharacterError).isTrue()
    }

    @Test
    fun `password whose length is less than eight character returns error`() {
        val result = passwordValidationUseCase("Pole@12")
        assertThat(result).isInstanceOf(PasswordValidationUseCase.PasswordUseCaseResult.Error::class.java)
        assertThat((result as PasswordValidationUseCase.PasswordUseCaseResult.Error).errorType is PasswordValidationUseCase.PasswordErrorType.LengthError).isTrue()
    }

    @Test
    fun `valid password returns success`() {
        val result = passwordValidationUseCase("Pole@123")
        assertThat(result).isInstanceOf(PasswordValidationUseCase.PasswordUseCaseResult.Success::class.java)
    }

    @Test
    fun `validate different password errors`(){
        //when password is empty
        var result = passwordValidationUseCase.validateAndPasswordErrors("")
        assertThat(
            result.contains(PasswordValidationUseCase.PasswordErrorType.Empty)
        ).isTrue()

        //when password has spaces
        result = passwordValidationUseCase.validateAndPasswordErrors("   ")
        assertThat(
            result.contains(PasswordValidationUseCase.PasswordErrorType.Empty)
        ).isTrue()

        //when password does not contain upper case symbol
        result = passwordValidationUseCase.validateAndPasswordErrors("pole@123")
        assertThat(
            result.contains(PasswordValidationUseCase.PasswordErrorType.UpperCaseError)
        ).isTrue()

        //when password does not contain lower case symbol
        result = passwordValidationUseCase.validateAndPasswordErrors("POLE@123")
        assertThat(
            result.contains(PasswordValidationUseCase.PasswordErrorType.LowerCaseError)
        ).isTrue()

        //when password does not contain digit
        result = passwordValidationUseCase.validateAndPasswordErrors("POLE@pole")
        assertThat(
            result.contains(PasswordValidationUseCase.PasswordErrorType.NumbersError)
        ).isTrue()

        //when password does not contain allowed special character given by product team
        result = passwordValidationUseCase.validateAndPasswordErrors("Pole+123")
        assertThat(
            result.contains(PasswordValidationUseCase.PasswordErrorType.SpecialCharacterError)
        ).isTrue()
    }

    @Test
    fun `password which does not meet length criteria returns false`(){
        var result = passwordValidationUseCase.isPasswordLengthValidated("Pole@12")
        assertEquals(result, false)

        //when password is empty
        result = passwordValidationUseCase.isPasswordLengthValidated("")
        assertEquals(result, false)
    }

    @Test
    fun `password which does not contain upper case symbol returns false`(){
        var result = passwordValidationUseCase.isUpperCaseValidated("pole@123")
        assertEquals(result, false)

        //when password is empty
        result = passwordValidationUseCase.isUpperCaseValidated("")
        assertEquals(result, false)
    }

    @Test
    fun `password which does not contain lower case symbol returns false`(){
        var result = passwordValidationUseCase.isLowerCaseValidated("POLE@123")
        assertEquals(result, false)

        //when password is empty
        result = passwordValidationUseCase.isLowerCaseValidated("")
        assertEquals(result, false)
    }

    @Test
    fun `password which does not contain digit returns false`(){
        var result = passwordValidationUseCase.isNumberCaseValidated("POLE@pole")
        assertEquals(result, false)

        //when password is empty
        result = passwordValidationUseCase.isNumberCaseValidated("")
        assertEquals(result, false)
    }

    @Test
    fun `password which does not contain allowed special character returns false`(){
        var result = passwordValidationUseCase.isSpecialCharCaseValidated("Pole+123")
        assertEquals(result, false)

        //when password is empty
        result = passwordValidationUseCase.isSpecialCharCaseValidated("")
        assertEquals(result, false)
    }
}