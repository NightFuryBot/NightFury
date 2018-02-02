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
package xyz.nightfury.api

import xyz.nightfury.api.util.path

/**
 * @author Kaidan Gustave
 */
abstract class APIRouteGroup(val parent: APIRouteGroup?, override val path: String): AppendableRoute {
    private val subRoutes = HashSet<AppendableRoute>()

    operator fun AppendableRoute.unaryPlus() {
        require(this !== this@APIRouteGroup) { "Groups may not be descendants of themselves!" }

        var ancestor = parent
        while(ancestor !== null) {
            require(ancestor !== this@APIRouteGroup) { "Groups may not be descendants of themselves!" }
            ancestor = ancestor.parent
        }

        subRoutes.add(this)
    }

    abstract fun initChildren()

    override fun append() = path(path) {
        initChildren()
        subRoutes.forEach {
            it.append()
        }
    }
}