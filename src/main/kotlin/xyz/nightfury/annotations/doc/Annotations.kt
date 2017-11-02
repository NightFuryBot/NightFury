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
package xyz.nightfury.annotations.doc

import com.jagrosh.jdautilities.doc.ConvertedBy
import com.jagrosh.jdautilities.doc.DocConverter

@ConvertedBy(DocumentationConverter::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Documentation(
    val name: Array<String>,
    val usage: String = "",
    val description: String,
    val requirements: Array<String> = []
)

class DocumentationConverter: DocConverter<Documentation>
{
    override fun read(annotation: Documentation): String
    {
        val names = annotation.name
        val usage = annotation.usage
        val description = annotation.description
        val requirements = annotation.requirements
        val b = StringBuilder()

        if(names.isNotEmpty())
        {
            b.append("**Name:** `").append(names[0]).append("`").append("\n\n")
            if(names.size > 1)
            {
                b.append("**Aliases:**")
                for(i in 1 until names.size)
                    b.append(" `").append(names[i]).append("`").append(if (i != names.size - 1) "," else "\n\n")
            }
        }

        if(!usage.isEmpty())
            b.append("**Usage:** `").append(usage).append("`\n\n")

        if(!description.isEmpty())
            b.append("**Description:** ").append(description).append("\n\n")

        if(requirements.size == 1)
            b.append("**Requirement:** ").append(requirements[0]).append("\n\n")
        else if(requirements.size > 1)
        {
            b.append("**Requirements:**\n")
            for(i in 1..requirements.size)
            {
                b.append(i).append(") ").append(requirements[i - 1])
                if(i != requirements.size)
                    b.append("\n")
            }
        }

        return b.toString()
    }
}