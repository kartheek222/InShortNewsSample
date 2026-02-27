package com.side.newsapplication.data

import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor

/**
 * Created by kartheek.sabbisetty on 01-02-2024
 */

val ERROR_MESSAGE = "Its not you its us, please check again"
val ERROR_MESSAGE_SESSION_EXPIRED = "Your session has expired, please try logging again"

fun <T> SessionTimeoutResponseModel(
    data: T? = null,
    statusCode: Int = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
    status: String = ApiConstants.STATUS.IAT,
    message: String = ERROR_MESSAGE_SESSION_EXPIRED,
) = BaseResponseModel(
    statusCode = statusCode,
    data = data,
    status = status,
    message = message
)

val SuccessResponseModel: BaseResponseModel<Unit>
    get() = BaseResponseModel(
        statusCode = ApiConstants.RESPONSE_SUCCESS_CODE,
        data = null,
        status = ApiConstants.RESPONSE_SUCCESS,
        message = "data fetched"
    )

fun <T> SuccessResponseModel(
    data: T? = null,
    statusCode: Int = ApiConstants.RESPONSE_SUCCESS_CODE,
    status: String = ApiConstants.RESPONSE_SUCCESS,
    message: String = "data fetched",
) = BaseResponseModel(
    statusCode = statusCode,
    data = data,
    status = status,
    message = message
)

fun <T> ErrorResponseModel(
    data: T? = null,
    statusCode: Int = 201,
    status: String = ApiConstants.RESPONSE_FAIL,
    message: String = ERROR_MESSAGE,
) = BaseResponseModel(
    statusCode = statusCode,
    data = data,
    status = status,
    message = message
)

