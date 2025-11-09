/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright

import android.app.Application
import com.tas33n.droidwright.data.repository.ScriptRepository
import com.tas33n.droidwright.domain.UIAutomatorEngine

class AutomationApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ScriptRepository.initialize(this)
        UIAutomatorEngine.initialize(this)
    }
}
