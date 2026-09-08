
package com.marketintelligence.ai.domain.engine

/**
 * A base marker interface for all engine types.
 * This is used to enable Hilt's multibinding feature.
 */
interface Engine {
    val engineName: String
}
