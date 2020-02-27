/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2020 Pierre Leresteux.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.saagie.updatarium.model

import mu.KotlinLogging

typealias Tag = String

typealias Predicate = () -> Boolean
infix fun Predicate.and(predicate: Predicate) : Predicate = { this() && predicate() }
infix fun Predicate.or(predicate: Predicate) : Predicate = { this() || predicate() }
fun not(predicate: Predicate) : Predicate = { !predicate() }

@DslMarker
annotation class ChangelogDslMarker

/**
 * Allow [ChangeLog] creation with a typesafe builder
 * Sample usage:
 * ```
 * import io.saagie.updatarium.model.changeLog
 *
 * changeLog {
 *   changeSet(id = "ChangeSet-1", author = "Hello World") {
 *     action {
 *       (1..5).forEach {
 *         logger.info {"Hello ${"$"}it!"}
 *       }
 *     }
 *   }
 * }
 * ```
 * @param id the changeLog id
 * @param block the content block
 * @return the [ChangeLog]
 */
fun changeLog(id: String = "", block: ChangeLogDsl.() -> Unit): ChangeLog =
    ChangeLogDsl(id)
        .apply(block)
        .build()

/**
 * A ChangeLog typesafe builder
 *
 * @param id the changeLog id
 */
@ChangelogDslMarker
class ChangeLogDsl(val id: String) {
    var preCondition: Predicate = { true }
    private var changeSetsDsl: MutableList<ChangeSetDsl> = mutableListOf()

    fun changeSet(id: String, author: String, block: ChangeSetDsl.() -> Unit) =
        this.changeSetsDsl.add(ChangeSetDsl(id, author).apply(block))

    @Deprecated(
        "Cannot be used in changeLog block.", level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("changeSet(\"...\")")
    )
    fun changeLog(id: String = "", block: ChangeLogDsl.() -> Unit): Nothing = error("...")

    internal fun build(): ChangeLog =
        ChangeLog(id, preCondition, this.changeSetsDsl.map(ChangeSetDsl::build))
}

/**
 * A ChangeSet typesafe builder
 *
 * @param id the changeSet id
 * @param author the changeSet author
 */
@ChangelogDslMarker
class ChangeSetDsl(val id: String, val author: String) {
    var preCondition: Predicate = { true }
    private var actions: MutableList<ActionDsl> = mutableListOf()

    var tags: List<Tag> = emptyList()

    fun action(name: String = "basicAction", block: ActionDsl.() -> Unit) =
        this.actions.add(ActionDsl(name, block))

    @Deprecated(
        "Cannot be used in changeSet block.", level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("action(\"...\")")
    )
    fun changeLog(id: String = "", block: ChangeLogDsl.() -> Unit): Nothing = error("...")

    internal fun build(): ChangeSet =
        ChangeSet(
            id = id,
            author = author,
            tags = tags,
            preCondition = preCondition,
            actions = actions.map(ActionDsl::build)
        )
}

/**
 * An Action typesafe builder
 * Just provide a `logger`
 *
 * @param name the action name, used by the logger
 * @param block the code to run in the action
 */
@ChangelogDslMarker
class ActionDsl(val name: String, val block: ActionDsl.() -> Unit) {
    val logger = KotlinLogging.logger(name)

    internal fun build(): Action = Action {
        block()
    }
}
