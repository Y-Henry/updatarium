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

import io.saagie.updatarium.config.UpdatariumConfiguration
import io.saagie.updatarium.model.UpdatariumError.ChangeSetError
import mu.KLoggable

data class ChangeSet(
    private val id: String,
    val author: String,
    val tags: List<String> = emptyList(),
    val preCondition: () -> Boolean = { true },
    val actions: List<Action> = emptyList()
) : KLoggable {
    override val logger = logger()

    /**
     * Generate an ID (changelogId id)
     */
    private fun computeId(changeLogId: String): String =
        if (changeLogId.isNotEmpty()) "${changeLogId}_$id"
        else id

    /**
     * The changeSet execution :
     * - check if the changeSet has already been executed (OK or KO)
     * - if not :
     *      - lock the changeSet
     *      - execute each action sequentially.
     *      - unlock the changeSet (with the correct status)
     *  Status => OK if all actions were OK, KO otherwise ...
     */
    fun execute(
        changeLogId: String,
        configuration: UpdatariumConfiguration = UpdatariumConfiguration()
    ): List<ChangeSetError> {
        val executionId = computeId(changeLogId)
        if (preCondition()) {
            return if (!configuration.persistEngine.notAlreadyExecuted(executionId)) {
                logger.info { "$executionId already executed" }
                emptyList()
            } else {
                logger.info { "$executionId will be executed" }
                val maybeError = with(configuration.persistEngine) {
                    runWithPersistEngine(executionId, lock = !configuration.dryRun) {
                        this.actions.forEach {
                            if (configuration.dryRun) {
                                logger.warn { "DryRun => don't run it" }
                            } else {
                                it.execute()
                            }
                        }
                    }
                }
                maybeError?.let { error ->
                    listOf(ChangeSetError(this, error))
                } ?: emptyList()
            }
        }
        logger.info("$executionId is ignored, the precondition return false")
        return emptyList()
    }
}
