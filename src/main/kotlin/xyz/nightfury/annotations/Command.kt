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
package xyz.nightfury.annotations

/**
 * Signifies that a command invokes cooldown just prior to execution, automatically.
 *
 * When this is not annotated in the class overhead of the command, the command will
 * not invoke cooldown unless CommandEvent#invokeCooldown() is called in the execution.
 *
 * @author Kaidan Gustave
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoInvokeCooldown

/**
 * Signifies that a command must have arguments to proceed.
 *
 * When this is annotated in the class overhead of the command, the command
 * will only execute if the arguments of the call [are not empty]
 * [kotlin.text.isNotEmpty].
 *
 * @author Kaidan Gustave
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MustHaveArguments(val error: String = "")

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasDocumentation

