/*
 * Copyright 2017-2018 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.nightfury.api.util

import org.eclipse.jetty.http.HttpStatus

/** 200 - OK */
const val OK = HttpStatus.OK_200
/** 201 - CREATED */
const val CREATED = HttpStatus.CREATED_201
/** 202 - ACCEPTED */
const val ACCEPTED = HttpStatus.ACCEPTED_202
/** 204 - NO CONTENT */
const val NO_CONTENT = HttpStatus.NO_CONTENT_204
/** 302 - FOUND */
const val FOUND = HttpStatus.FOUND_302
/** 400 - BAD REQUEST */
const val BAD_REQUEST = HttpStatus.BAD_REQUEST_400
/** 401 - UNAUTHORIZED */
const val UNAUTHORIZED = HttpStatus.UNAUTHORIZED_401
/** 404 - NOT FOUND */
const val NOT_FOUND = HttpStatus.NOT_FOUND_404
/** 500 - INTERNAL SERVER ERROR */
const val INTERNAL_SERVER_ERROR = HttpStatus.INTERNAL_SERVER_ERROR_500
/** 501 - NOT IMPLEMENTED */
const val NOT_IMPLEMENTED = HttpStatus.NOT_IMPLEMENTED_501