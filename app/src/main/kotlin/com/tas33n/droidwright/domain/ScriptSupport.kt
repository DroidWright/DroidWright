/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright.domain

object ScriptTemplates {
    fun blankTemplate(): String {
        return """
            // ==DroidScript==
            // @id              your-script-id
            // @name            My Automation Script
            // @description     Describe what this automation accomplishes.
            // @author          you
            // @version         1.0.0
            // @targetApp       com.example.app
            // @url             https://example.com
            // @created         2024-11-08
            // ==/DroidScript==

            /**
             * Main automation function
             * @param {Object} ctx - DroidWright automation context
             * @param {Object} ctx.device - Device control helpers (sleep, press, getScreenSize, showToast)
             * @param {Object} ctx.ui - UI automation helpers (find, tap, setText, swipe, exists)
             * @param {Object} ctx.app - App lifecycle helpers (launch, stop, isRunning)
             * @returns {Object} Result payload
             */
            function droidRun(ctx) {
              // Configuration
              const settleMs = 1200;
              const swipeDurationMs = 500;
              const TOTAL_STEPS = 3;
              const targetPackage = typeof targetApp === "string" ? targetApp : "com.example.app";

              // Launch target app
              ctx.app.launch(targetPackage);
              ctx.device.sleep(settleMs);

              // Screen metrics for swipe helpers
              const screen = ctx.device.getScreenSize();
              const swipe = {
                startX: Math.round(screen.width * 0.5),
                startY: Math.round(screen.height * 0.8),
                endX: Math.round(screen.width * 0.5),
                endY: Math.round(screen.height * 0.3),
                duration: swipeDurationMs,
              };

              // TODO: Replace selectors with your target UI elements
              const primarySelector = { text: "Continue" };

              for (let step = 0; step < TOTAL_STEPS; step++) {
                ctx.device.showToast(`Step ${"$"}{step + 1} of ${"$"}{TOTAL_STEPS}`);

                if (ctx.ui.exists(primarySelector)) {
                  ctx.ui.tap(primarySelector);
                  ctx.device.sleep(settleMs);
                } else {
                  ctx.ui.swipe(swipe.startX, swipe.startY, swipe.endX, swipe.endY, swipe.duration);
                  ctx.device.sleep(settleMs);
                }
              }

              return {
                status: "ok",
                note: "Customize this template with your own automation steps."
              };
            }
        """.trimIndent()
    }
}
