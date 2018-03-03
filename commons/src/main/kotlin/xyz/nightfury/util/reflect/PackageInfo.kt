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
@file:Suppress("CanBeParameter", "MemberVisibilityCanBePrivate", "Unused")
package xyz.nightfury.util.reflect

import kotlin.reflect.KClass

/**
 * @author Kaidan Gustave
 */
class PackageInfo internal constructor(klazz: KClass<*>) {
    private val javaPackage = klazz.java.`package`

    val name: String? = javaPackage.name

    val isSealed: Boolean = javaPackage.isSealed

    val version: VersionInfo by lazy {
        VersionInfo(javaPackage.implementationVersion, javaPackage.specificationVersion)
    }

    val title: TitleInfo by lazy {
        TitleInfo(javaPackage.implementationTitle, javaPackage.specificationTitle)
    }

    val vendor: VendorInfo by lazy {
        VendorInfo(javaPackage.implementationVendor, javaPackage.specificationVendor)
    }

    data class VersionInfo(val implementation: String?, val specification: String?)

    data class TitleInfo(val implementation: String?, val specification: String?)

    data class VendorInfo(val implementation: String?, val specification: String?)

    override fun hashCode(): Int = (name?.hashCode() ?: 0) + title.hashCode()
    override fun equals(other: Any?): Boolean {
        if(other !is PackageInfo) {
            return false
        }

        return name == other.name && isSealed == other.isSealed &&
               version == other.version &&
               title == other.title &&
               vendor == other.vendor
    }
    override fun toString(): String {
        return "${name ?: title.let { it.implementation ?: it.specification } ?: ""} - $version"
    }
}