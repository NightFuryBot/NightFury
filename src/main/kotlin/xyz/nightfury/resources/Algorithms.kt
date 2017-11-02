/*
 * Copyright 2017 Kaidan Gustave
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
package xyz.nightfury.resources

/**
 * @author Kaidan Gustave
 */
object Algorithms
{
    fun selectResultURL(query: String, results: List<String>): String
    {
        // Start with last index of the results
        var index = results.size - 1

        // Subtract the length of the query
        index -= query.length

        // If the index has fallen below or is at 0 return the first result
        if(index <= 0)
            return results[0]

        // If there is more than 2 spaces, divide the results by the number of them
        val spaces = query.split(Arguments.commandArgs).size
        if(spaces > 2)
            index /= spaces - 1

        // Once again, grab first index if it's below or at 0
        if(index <= 0)
            return results[0]

        // return a random result between first index and the calculated maximum
        return results[(Math.random() * (index)).toInt()]
    }
}