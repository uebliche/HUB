<template>
  <div class="config-builder" data-config-mode="proxy">
    <div class="cb-shell">
      <header class="cb-hero">
        <div class="cb-hero-copy">
          <p class="cb-kicker">Config Builder</p>
          <h1>Build a HUB config without touching YAML.</h1>
          <p class="cb-lede">Upload a config.yml, tweak lobbies, test regex, download.</p>
          <div class="cb-steps">
            <div class="cb-step"><span>1</span> Upload or start fresh</div>
            <div class="cb-step"><span>2</span> Edit lobbies and routing</div>
            <div class="cb-step"><span>3</span> Download and deploy</div>
          </div>
        </div>
        <div class="cb-hero-panel">
          <div class="cb-status" id="cb-status" data-tone="idle" aria-live="polite">Ready</div>
          <div class="cb-meta">Source: <span id="cb-source">defaults</span> | Updated: <span id="cb-updated">never</span></div>
          <label class="cb-field cb-field-toggle cb-mode-toggle">
            <span id="cb-mode-label" title="Switch between proxy and lobby server config">Config target: Proxy</span>
            <input id="cb-mode-toggle" type="checkbox" aria-label="Switch between proxy and lobby config" checked>
          </label>
          <p class="cb-note cb-mode-hint" id="cb-mode-hint">Proxy config (Velocity).</p>
          <div class="cb-hero-actions">
            <button type="button" class="cb-btn cb-btn-primary" id="cb-defaults">Use defaults</button>
            <button type="button" class="cb-btn cb-btn-ghost" id="cb-reset">Reset edits</button>
            <button type="button" class="cb-btn cb-btn-ghost" id="cb-tutorial-start">Tutorial</button>
          </div>
          <label class="cb-file">
            <span title="Load an existing config.yml to edit">Upload config.yml</span>
            <input type="file" id="cb-file" accept=".yml,.yaml">
          </label>
          <label class="cb-field cb-paste">
            <span title="Paste a full config.yml and load it into the builder">Paste YAML</span>
            <textarea id="cb-paste" rows="6" placeholder="Paste config.yml here"></textarea>
          </label>
          <div class="cb-paste-actions">
            <button type="button" class="cb-btn cb-btn-ghost" id="cb-load-paste">Load pasted YAML</button>
          <button type="button" class="cb-btn cb-btn-ghost" id="cb-clear-paste">Clear paste</button>
        </div>
          
        <p class="cb-note">Tip: Use (?i) at the start for case-insensitive filters.</p>
      </div>
    </header>

    <div class="cb-layout">
      <div class="cb-main">
        <div class="cb-tabs" role="tablist" aria-label="Config sections">
          <button type="button" class="cb-tab is-active" data-tab="core" role="tab" aria-selected="true">Core settings</button>
          <button type="button" class="cb-tab" data-tab="debug" role="tab" aria-selected="false">Debug</button>
          <button type="button" class="cb-tab" data-tab="messages" role="tab" aria-selected="false">Messages</button>
          <button type="button" class="cb-tab" data-tab="finder" role="tab" aria-selected="false">Finder + Data collection</button>
          <button type="button" class="cb-tab" data-tab="lobbies" role="tab" aria-selected="false">Lobbies</button>
        </div>

        <section class="cb-card cb-tab-panel is-active" data-tab-panel="core" style="--cb-delay: 0.05s;">
          <div class="cb-card-head">
            <h2>Core settings</h2>
            <p>Commands, routing, and debug toggles.</p>
          </div>
          <div class="cb-grid-2">
            <label class="cb-field" data-tutorial="core" data-tutorial-anchor="core">
              <span title="Primary command players use to open the hub">Base command</span>
              <input id="cb-base-command" type="text" placeholder="hub">
              <small>Players use /hub to open the selector.</small>
            </label>
            <label class="cb-field">
              <span title="Extra commands that also open the hub">Aliases</span>
              <input id="cb-aliases" type="text" placeholder="lobby leave">
              <small>Separate with spaces.</small>
            </label>
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Regex for server names where /hub should be hidden">Hide base command on lobby (regex)</span>
              <input id="cb-hide-on" type="text" placeholder="^(?!.*).$">
              <small>Matches the current server name. If it matches, /hub is hidden.</small>
            </label>
            <div class="cb-field cb-span-2">
              <span title="Map domains directly to server names">Forced hosts (direct servers)</span>
              <div class="cb-forced-list" id="cb-forced-hosts"></div>
              <div class="cb-forced-actions">
                <button type="button" class="cb-btn cb-btn-ghost cb-mini-btn" id="cb-forced-host-add">Add mapping</button>
              </div>
              <small>Direct server routing only. Forced hosts for lobbies live in each lobby.</small>
            </div>
            <label class="cb-field">
              <span title="Automatically select a lobby when a player joins the proxy">Auto-select on join</span>
              <input id="cb-auto-join" type="checkbox">
            </label>
            <label class="cb-field">
              <span title="Automatically select a lobby when a player is kicked from a server">Auto-select on server kick</span>
              <input id="cb-auto-kick" type="checkbox">
            </label>
            <label class="cb-field">
              <span title="Try to reconnect players to their last lobby">Remember last lobby</span>
              <input id="cb-last-lobby" type="checkbox">
            </label>
          </div>
        </section>

        <section class="cb-card cb-tab-panel" data-tab-panel="debug" style="--cb-delay: 0.055s;">
          <div class="cb-card-head">
            <h2>Debug</h2>
            <p>Control debug access and fine-grained logging categories.</p>
          </div>
          <div class="cb-grid-2">
            <label class="cb-field">
              <span title="Enable /hub debug commands">Debug enabled</span>
              <input id="cb-debug-enabled" type="checkbox">
            </label>
            <label class="cb-field">
              <span title="Permission required to use /hub debug">Debug permission</span>
              <input id="cb-debug-permission" type="text" placeholder="hub.debug">
            </label>
            <div class="cb-field cb-span-2" id="cb-debug-categories">
              <span title="Choose which debug areas are logged">Debug categories</span>
              <div class="cb-grid-2">
                <label class="cb-field">
                  <span>General</span>
                  <input id="cb-debug-cat-general" type="checkbox">
                </label>
                <label class="cb-field">
                  <span>Commands</span>
                  <input id="cb-debug-cat-commands" type="checkbox">
                </label>
                <label class="cb-field">
                  <span>Finder</span>
                  <input id="cb-debug-cat-finder" type="checkbox">
                </label>
                <label class="cb-field">
                  <span>Pings</span>
                  <input id="cb-debug-cat-pings" type="checkbox">
                </label>
                <label class="cb-field">
                  <span>Compass</span>
                  <input id="cb-debug-cat-compass" type="checkbox">
                </label>
                <label class="cb-field">
                  <span>Permissions</span>
                  <input id="cb-debug-cat-permissions" type="checkbox">
                </label>
                <label class="cb-field">
                  <span>Transfer</span>
                  <input id="cb-debug-cat-transfer" type="checkbox">
                </label>
                <label class="cb-field">
                  <span>Events</span>
                  <input id="cb-debug-cat-events" type="checkbox">
                </label>
                <label class="cb-field">
                  <span>Placeholders</span>
                  <input id="cb-debug-cat-placeholders" type="checkbox">
                </label>
                <label class="cb-field">
                  <span>Forced hosts</span>
                  <input id="cb-debug-cat-forced-hosts" type="checkbox">
                </label>
                <label class="cb-field">
                  <span>Last lobby</span>
                  <input id="cb-debug-cat-last-lobby" type="checkbox">
                </label>
                <label class="cb-field">
                  <span>Config</span>
                  <input id="cb-debug-cat-config" type="checkbox">
                </label>
              </div>
              <small>Pings are noisy; keep them off unless you need them.</small>
            </div>
          </div>
        </section>

        <section class="cb-card cb-tab-panel" data-tab-panel="messages" data-tutorial="messages" data-tutorial-anchor="messages" data-tutorial-scroll="false" data-tutorial-align="right" style="--cb-delay: 0.06s;">
          <div class="cb-card-head">
            <h2>Messages</h2>
            <p>Global and system message overrides.</p>
          </div>
          <div class="cb-grid-2 cb-details-grid">
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Message shown after successful hub transfer">Success message</span>
              <textarea id="cb-message-success" rows="2"></textarea>
              <div class="cb-message-preview" data-preview-for="cb-message-success">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Message shown when already in a hub">Already connected message</span>
              <textarea id="cb-message-already" rows="2"></textarea>
              <div class="cb-message-preview" data-preview-for="cb-message-already">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Message shown while connecting to a hub">Connection in progress message</span>
              <textarea id="cb-message-progress" rows="2"></textarea>
              <div class="cb-message-preview" data-preview-for="cb-message-progress">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Message shown if the target lobby server is offline">Server disconnected message</span>
              <textarea id="cb-message-disconnected" rows="2"></textarea>
              <div class="cb-message-preview" data-preview-for="cb-message-disconnected">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Message shown when a transfer is cancelled">Connection cancelled message</span>
              <textarea id="cb-message-cancelled" rows="2"></textarea>
              <div class="cb-message-preview" data-preview-for="cb-message-cancelled">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Message when console runs a player-only command">Players only command message</span>
              <textarea id="cb-system-players-only" rows="2"></textarea>
              <div class="cb-message-preview" data-preview-for="cb-system-players-only">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Message when no lobby server can be found">No lobby found message</span>
              <textarea id="cb-system-no-lobby" rows="2"></textarea>
              <div class="cb-message-preview" data-preview-for="cb-system-no-lobby">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
            <label class="cb-field">
              <span title="Enable the kick message wrapper">Kick message enabled</span>
              <input id="cb-kick-enabled" type="checkbox">
            </label>
            <label class="cb-field">
              <span title="Prefix added before kick message">Kick prefix</span>
              <input id="cb-kick-prefix" type="text">
            </label>
            <label class="cb-field">
              <span title="Suffix added after kick message">Kick suffix</span>
              <input id="cb-kick-suffix" type="text">
            </label>
          </div>
        </section>

        <section class="cb-card cb-tab-panel" data-tab-panel="finder" style="--cb-delay: 0.07s;">
          <div class="cb-card-head">
            <h2>Finder + Data collection</h2>
            <p>Configure search timings and runtime data dumps.</p>
          </div>
          <div class="cb-grid-2">
            <label class="cb-field" data-tutorial="finder" data-tutorial-anchor="finder">
              <span title="Seconds before the first lobby search retry">Finder start duration</span>
              <input id="cb-finder-start" type="number" min="1" step="1">
              <small>Seconds before first retry.</small>
            </label>
            <label class="cb-field">
              <span title="Seconds added per retry">Finder increment duration</span>
              <input id="cb-finder-increment" type="number" min="1" step="1">
            </label>
            <label class="cb-field">
              <span title="Maximum seconds spent searching">Finder max duration</span>
              <input id="cb-finder-max" type="number" min="1" step="1">
            </label>
            <label class="cb-field">
              <span title="How often to refresh the search (ticks)">Finder refresh (ticks)</span>
              <input id="cb-finder-refresh" type="number" min="1" step="1">
            </label>
            <label class="cb-field" data-tutorial="finder">
              <span title="Enable runtime data dumps for analysis">Data collection enabled</span>
              <input id="cb-data-collect-enabled" type="checkbox">
            </label>
            <label class="cb-field">
              <span title="Store player UUIDs in the data dump">Include UUIDs</span>
              <input id="cb-data-collect-uuid" type="checkbox">
            </label>
            <label class="cb-field">
              <span title="Filename for the data dump">Dump file</span>
              <input id="cb-data-collect-file" type="text" placeholder="data-dump.yml">
            </label>
            <label class="cb-field">
              <span title="How often to write the data dump">Dump interval (minutes)</span>
              <input id="cb-data-collect-interval" type="number" min="1" step="1">
            </label>
            <label class="cb-field">
              <span title="Max users stored in the data dump">Max users</span>
              <input id="cb-data-collect-users" type="number" min="1" step="1">
            </label>
            <label class="cb-field">
              <span title="Max server names stored in the data dump">Max servers</span>
              <input id="cb-data-collect-servers" type="number" min="1" step="1">
            </label>
          </div>
        </section>

        <section class="cb-card cb-lobbies cb-tab-panel" data-tab-panel="lobbies" style="--cb-delay: 0.15s;">
          <div class="cb-card-head cb-card-head-row">
            <div>
              <h2>Lobbies</h2>
              <p>Add each lobby and its regex filter.</p>
            </div>
            <button type="button" class="cb-btn cb-btn-primary" id="cb-add-lobby">Add lobby</button>
          </div>
            <div class="cb-lobby-layout">
            <div class="cb-lobby-side">
              <div class="cb-lobby-side-head">
                <span>Groups</span>
                <button type="button" class="cb-btn cb-btn-ghost cb-mini-btn" id="cb-add-group">Add group</button>
              </div>
              <div class="cb-lobby-list" id="cb-lobby-list" data-tutorial="lobbies"></div>
            </div>
            <div class="cb-lobby-detail">
              <div id="cb-lobbies"></div>
            </div>
          </div>
          <p class="cb-footnote">Each lobby can expose a direct command. Extra commands in uploads are kept as-is.</p>
        </section>

        <section class="cb-card cb-i18n-panel" style="--cb-delay: 0.16s;">
          <div class="cb-card-head">
            <h2>Localization</h2>
            <p>Server-side translations and per-player locale settings.</p>
          </div>
          <div class="cb-grid-2">
            <label class="cb-field">
              <span title="Fallback locale when client locale is not used">Default locale</span>
              <input id="cb-i18n-default-locale" type="text" placeholder="en_us">
            </label>
            <label class="cb-field">
              <span title="Use the player's client locale when available">Use client locale</span>
              <input id="cb-i18n-use-client" type="checkbox">
            </label>
            <div class="cb-field cb-span-2">
              <span title="Override i18n keys per locale">Translation overrides</span>
              <div class="cb-forced-list" id="cb-i18n-overrides"></div>
              <div class="cb-forced-actions">
                <button type="button" class="cb-btn cb-btn-ghost cb-mini-btn" id="cb-i18n-add-locale">Add locale</button>
              </div>
              <small>Overrides apply on top of bundled en_us/de_de. Supports MiniMessage.</small>
            </div>
          </div>
        </section>

        <section class="cb-card cb-lobby-mode-panel" style="--cb-delay: 0.18s;">
          <div class="cb-card-head">
            <h2>Lobby server</h2>
            <p>Basics for Paper, Fabric, and Minestom lobby configs.</p>
          </div>
          <div class="cb-grid-2">
            <label class="cb-field">
              <span>Compass enabled</span>
              <input id="cb-lobby-compass-enabled" type="checkbox">
            </label>
            <label class="cb-field">
              <span>Navigator enabled</span>
              <input id="cb-lobby-navigator-enabled" type="checkbox">
            </label>
            <label class="cb-field cb-span-2">
              <span>Compass title</span>
              <input id="cb-lobby-compass-title" type="text" placeholder="<aqua>Select Lobby">
            </label>
            <label class="cb-field cb-span-2">
              <span>Navigator title</span>
              <input id="cb-lobby-navigator-title" type="text" placeholder="<aqua>Navigator">
            </label>
            <label class="cb-field">
              <span>Spawn teleport enabled</span>
              <input id="cb-lobby-spawn-enabled" type="checkbox">
            </label>
            <label class="cb-field">
              <span>Join teleport mode</span>
              <select id="cb-lobby-join-teleport-mode">
                <option value="none">none</option>
                <option value="spawn">spawn</option>
                <option value="last">last</option>
              </select>
            </label>
            <label class="cb-field">
              <span>Join teleport delay (seconds)</span>
              <input id="cb-lobby-join-teleport-delay" type="number" min="0" step="1" placeholder="0">
            </label>
            <label class="cb-field">
              <span>Last location max age (seconds)</span>
              <input id="cb-lobby-join-teleport-max-age" type="number" min="0" step="1" placeholder="0">
            </label>
            <label class="cb-field">
              <span>Lobby NPCs enabled</span>
              <input id="cb-lobby-npcs-enabled" type="checkbox">
            </label>
            <label class="cb-field">
              <span>Lobby signs enabled</span>
              <input id="cb-lobby-signs-enabled" type="checkbox">
            </label>
            <label class="cb-field">
              <span>Disable damage</span>
              <input id="cb-lobby-gameplay-damage" type="checkbox">
            </label>
            <label class="cb-field">
              <span>Disable hunger</span>
              <input id="cb-lobby-gameplay-hunger" type="checkbox">
            </label>
            <label class="cb-field">
              <span>Heal on join</span>
              <input id="cb-lobby-gameplay-heal" type="checkbox">
            </label>
            <label class="cb-field">
              <span>bStats enabled</span>
              <input id="cb-lobby-bstats-enabled" type="checkbox">
            </label>
            <label class="cb-field cb-span-2">
              <span>Velocity forwarding secret</span>
              <input id="cb-lobby-minestom-secret" type="text" placeholder="hubsecret">
            </label>
          </div>
          <p class="cb-footnote">NPC and sign entries are kept from uploads. Edit entries in YAML if needed.</p>
        </section>

        <section class="cb-card cb-regex-lab" style="--cb-delay: 0.2s;">
          <div class="cb-card-head cb-regex-head">
            <div>
              <h2>Regex lab</h2>
              <p>Quick sanity checks before you download.</p>
            </div>
            <button type="button" class="cb-btn cb-btn-ghost cb-regex-toggle" id="cb-regex-toggle" aria-expanded="true" aria-label="Minimize regex lab">
              <svg class="cb-regex-icon cb-regex-icon-open" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 12h12M14 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <svg class="cb-regex-icon cb-regex-icon-closed" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M20 12H8M10 6l-6 6 6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
          <div class="cb-regex-body">
            <div class="cb-grid-2">
              <label class="cb-field">
                <span id="cb-regex-pattern-label" title="Regex pattern to test">Lobby regex</span>
                <input id="cb-regex-input" type="text" placeholder="(?i)^lobby.*">
              </label>
              <label class="cb-field">
                <span id="cb-regex-test-label" title="Server name to test against the regex">Test string</span>
                <div class="cb-input-wrap">
                  <input id="cb-regex-test" type="text" placeholder="lobby-eu-1" list="cb-server-suggestions">
                  <span id="cb-regex-result" class="cb-input-icon" data-state="idle" aria-label="Waiting for input" title="Waiting for input">
                    <svg class="cb-icon-idle" viewBox="0 0 20 20" aria-hidden="true">
                      <circle cx="10" cy="10" r="6" fill="none" stroke="currentColor" stroke-width="2"/>
                    </svg>
                    <svg class="cb-icon-ok" viewBox="0 0 20 20" aria-hidden="true">
                      <path d="M5 10l3 3 7-7" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <svg class="cb-icon-bad" viewBox="0 0 20 20" aria-hidden="true">
                      <path d="M6 6l8 8M14 6l-8 8" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                  </span>
                </div>
              </label>
            </div>
            <div class="cb-inline-test">
              <p class="cb-note">Checker runs in the browser. It supports (?i) at the start for case-insensitive matches.</p>
            </div>
          </div>
        </section>

        <section class="cb-card" style="--cb-delay: 0.25s;">
          <div class="cb-card-head">
            <h2>Download</h2>
            <p>Grab the updated YAML.</p>
          </div>
          <div class="cb-actions">
            <button type="button" class="cb-btn cb-btn-primary" id="cb-download">Download config.yml</button>
            <button type="button" class="cb-btn cb-btn-ghost" id="cb-copy">Copy YAML</button>
          </div>
          <pre id="cb-output" class="cb-output"></pre>
        </section>
      </div>
    </div>
  </div>

  <div class="cb-tutorial" id="cb-tutorial" hidden>
    <div class="cb-tutorial-card" role="dialog" aria-live="polite">
      <div class="cb-tutorial-head">
        <div>
          <p class="cb-tutorial-step" id="cb-tutorial-step">Step</p>
          <h3 id="cb-tutorial-title">Tutorial</h3>
        </div>
        <button type="button" class="cb-btn cb-btn-ghost cb-icon-btn" id="cb-tutorial-close" aria-label="Close tutorial">
          <svg class="cb-trash-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M6 6l12 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <path d="M18 6l-12 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
      <div class="cb-tutorial-body" id="cb-tutorial-body"></div>
      <div class="cb-tutorial-hint" id="cb-tutorial-hint"></div>
      <div class="cb-tutorial-actions">
        <button type="button" class="cb-btn cb-btn-ghost" id="cb-tutorial-prev">Back</button>
        <button type="button" class="cb-btn cb-btn-primary" id="cb-tutorial-next" disabled>Next</button>
      </div>
    </div>
  </div>

  <div class="cb-modal" id="cb-group-modal" hidden>
    <div class="cb-modal-backdrop" data-action="close"></div>
    <div class="cb-modal-card" role="dialog" aria-modal="true" aria-labelledby="cb-group-modal-title">
      <div class="cb-modal-head">
        <h3 id="cb-group-modal-title">Create lobby group</h3>
        <button type="button" class="cb-btn cb-btn-ghost cb-icon-btn" data-action="close" aria-label="Close">
          <svg class="cb-trash-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M6 6l12 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <path d="M18 6l-12 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
      <label class="cb-field">
        <span>Group name</span>
        <input id="cb-group-modal-input" type="text" placeholder="main">
      </label>
      <div class="cb-modal-actions">
        <button type="button" class="cb-btn cb-btn-ghost" id="cb-group-modal-cancel">Cancel</button>
        <button type="button" class="cb-btn cb-btn-primary" id="cb-group-modal-create">Create group</button>
      </div>
    </div>
  </div>

  <textarea id="cb-default-yaml" hidden>
# Thanks <3

debug:
  enabled: true
  permission: hub.debug
  categories:
    general: true
    commands: true
    finder: true
    pings: false
    compass: true
    permissions: true
    transfer: true
    events: true
    placeholders: true
    forced-hosts: true
    last-lobby: true
    config: true
messages:
  success-message: <#69d9ff>You are now in the <i>Hub</i>.
  already-connected-message: <#ff614d>You are already on the <i>Hub</i>.
  connection-in-progress-message: <#ff9c59>In Progress...
  server-disconnected-message: <#ff614d>The Lobby Server is Offline...
  connection-cancelled-message: <#ff614d>Transfer cancelled.
system-messages:
  players-only-command-message: <#ff9c59>This Command is only available to Players.
  no-lobby-found-message: <#ff9c59>I'm sorry! i was unable to find a Lobby Server
    for you.
i18n:
  default-locale: en_us
  use-client-locale: true
  overrides:
    en_us: {}
    de_de: {}
kick-message:
  enabled: true
  prefix: <red>
  suffix: ''
base-hub-command: hub
aliases:
- lobby
- leave
hide-hub-command-on-lobby: ^(?!.*).$
auto-select:
  on-join: true
  on-server-kick: true
last-lobby:
  enabled: true
forced-hosts: []
lobby-groups:
- name: main
  lobbies:
  - lobby
  - teamlobby
  - premiumlobby
  forced-hosts: []
- name: minigame
  lobbies:
  - ffa-lobby
  forced-hosts: []
lobbies:
- name: teamlobby
  filter: (?i)^teamlobby.*
  permission: hub.team
  priority: 2
  parent: ''
  parent-groups:
  - lobby
  forced-hosts: []
  commands:
    teamlobby:
      standalone: false
      subcommand: true
      hide-on: ^(?!.*).$
  autojoin: false
  overwrite-messages: {}
- name: premiumlobby
  filter: (?i)^premiumlobby.*
  permission: hub.premium
  priority: 1
  parent: ''
  parent-groups:
  - lobby
  forced-hosts: []
  commands:
    premiumlobby:
      standalone: true
      subcommand: false
      hide-on: ^(?!.*).$
  autojoin: true
  overwrite-messages:
    success-message: <#69d9ff>You are now in the <b>Premium Hub</b>.
- name: ffa-lobby
  filter: (?i)^lobby-minestom.*
  permission: ''
  priority: 0
  parent: ''
  parent-groups:
  - main
  forced-hosts: []
  commands:
    ffa:
      standalone: false
      subcommand: true
      hide-on: ^(?!.*).$
  autojoin: true
  overwrite-messages: {}
- name: lobby
  filter: (?i)^lobby.*
  permission: ''
  priority: 0
  parent: ''
  parent-groups: []
  forced-hosts: []
  commands:
    base:
      standalone: false
      subcommand: true
      hide-on: ^(?!.*).$
  autojoin: true
  overwrite-messages: {}
placeholder:
  server:
    example: lobby-1
    key: server
    enabled: true
  server-host:
    example: 127.0.0.1
    key: server-host
    enabled: false
  server-port:
    example: '25565'
    key: server-port
    enabled: false
  server-player-count:
    example: '0'
    key: server-player-count
    enabled: true
  server-player-per-player-username:
    example: apitoken
    key: server-player-%i-username
    enabled: false
    placeholder: '%i'
  server-player-per-player-uuid:
    example: f9de374c-cb78-4c5c-aa2f-4a53ae981f9d
    key: server-player-%i-uuid
    enabled: false
    placeholder: '%i'
  lobby:
    example: lobby
    key: lobby
    enabled: true
  lobby-filter:
    example: (?i)^lobby.*
    key: lobby-filter
    enabled: false
  lobby-require-permission:
    example: 'true'
    key: lobby-require-permission
    enabled: false
  lobby-permission:
    example: hub.user
    key: lobby-permission
    enabled: true
  lobby-priority:
    example: '0'
    key: lobby-priority
    enabled: false
  lobby-command-per-command-standalone:
    example: 'true'
    key: lobby-command-%s-standalone
    enabled: false
  lobby-command-per-command-subcommand:
    example: 'true'
    key: lobby-command-%s-subcommand
    enabled: false
  lobby-command-per-command-hide-on:
    example: ^(?!.*).$
    key: lobby-command-%s-hide-on
    enabled: false
  lobby-autojoin:
    example: 'true'
    key: lobby-autojoin
    enabled: false
  player:
    example: Freddiio
    key: player
    enabled: true
  player-uuid:
    example: c5eb5df7-b7a9-4919-a9bc-7f59c8bee980
    key: player-uuid
    enabled: false
finder:
  start-duration: 20
  increment-duration: 20
  max-duration: 200
  refresh-interval-in-ticks: 40
data-collection:
  enabled: true
  dump-file: data-dump.yml
  dump-interval-minutes: 10
  max-users: 500
  max-servers: 500
  include-uuid: true
update-checker:
  enabled: true
  notification: ''
  check-interval-in-min: 30
  </textarea>
  <textarea id="cb-default-yaml-lobby" hidden>
compass:
  enabled: true
  gui-title: "<aqua>Select Lobby"
  cache-ttl-millis: 10000
  item:
    name: "<gold>Lobby Compass"
    lore:
      - "<gray>Right-click to choose a lobby"
    material: MAGMA_CREAM
    slot: 0
    allow-move: false
    allow-drop: false
    drop-on-death: true
    restore-on-respawn: true
  allow-drop: false
  drop-on-death: true
  restore-on-respawn: true
  list-item:
    name: "<gold><lobby>"
    lore:
      - "<gray>Server: <yellow><server>"
      - "<gray>Players: <green><online></dark_gray>/<green><max>"
      - "<dark_gray>Click to join"

navigator:
  enabled: true
  gui-title: "<aqua>Navigator"
  gui-rows: 0
  item:
    name: "<gold>Navigator"
    lore:
      - "<gray>Open navigator"
    material: COMPASS
    slot: 4
    allow-move: false
    allow-drop: false
    drop-on-death: true
    restore-on-respawn: true
  entries:
    spawn:
      name: "<gold>Spawn"
      lore:
        - "<gray>Teleport to spawn"
      icon: COMPASS
      action: teleport
      world: world
      x: 0
      y: 64
      z: 0
      yaw: 0
      pitch: 0
      slot: 13
    lobbies:
      name: "<gold>Lobbies"
      lore:
        - "<gray>Open lobby selector"
      icon: BOOK
      action: lobby
      slot: 11
    lobby:
      name: "<gold>Lobby"
      lore:
        - "<gray>Join lobby server"
      icon: ENDER_PEARL
      action: server
      server: lobby
      slot: 15

i18n:
  default-locale: en_us
  use-client-locale: true
  overrides:
    en_us: {}
    de_de: {}

spawn-teleport:
  enabled: false
  world: world
  x: 0
  y: 64
  z: 0
  yaw: 0
  pitch: 0

join-teleport:
  mode: none
  delay-seconds: 0
  last-location:
    max-age-seconds: 0

lobby-npcs:
  enabled: false
  entries:
    lobby:
      world: world
      x: 0
      y: 64
      z: 0
      yaw: 0
      pitch: 0
      name: "<gold>Lobby"
      entity: VILLAGER
      action: server
      server: lobby

lobby-signs:
  enabled: false
  entries:
    lobby:
      world: world
      x: 2
      y: 64
      z: 0
      lines:
        - "<gold>Lobby"
        - "<gray>Click to join"
      action: server
      server: lobby

gameplay:
  disable-damage: true
  disable-hunger: true
  heal-on-join: true

minestom:
  velocity-secret: "hubsecret"

bstats:
  enabled: true
  service-id: 0
  </textarea>
  <datalist id="cb-server-suggestions"></datalist>
</div>
</template>

<script setup>
import { onMounted } from 'vue';

onMounted(() => {
  const statusEl = document.getElementById('cb-status');
  const sourceEl = document.getElementById('cb-source');
  const updatedEl = document.getElementById('cb-updated');
  const modeToggle = document.getElementById('cb-mode-toggle');
  const modeLabel = document.getElementById('cb-mode-label');
  const modeHint = document.getElementById('cb-mode-hint');
  const outputEl = document.getElementById('cb-output');
  const fileInput = document.getElementById('cb-file');
  const defaultsBtn = document.getElementById('cb-defaults');
  const resetBtn = document.getElementById('cb-reset');
  const downloadBtn = document.getElementById('cb-download');
  const copyBtn = document.getElementById('cb-copy');
  const pasteArea = document.getElementById('cb-paste');
  const pasteLoadBtn = document.getElementById('cb-load-paste');
  const pasteClearBtn = document.getElementById('cb-clear-paste');
  const dataFileInput = document.getElementById('cb-data-file');
  const dataPaste = document.getElementById('cb-data-paste');
  const dataLoadBtn = document.getElementById('cb-data-load');
  const dataClearBtn = document.getElementById('cb-data-clear');
  const dataSummary = document.getElementById('cb-data-summary');
  const dataSummaryInline = document.getElementById('cb-data-summary-inline');
  const testUserSelect = document.getElementById('cb-test-user');
  const serverSuggestions = document.getElementById('cb-server-suggestions');
  const groupsEl = document.getElementById('cb-groups');
  const lobbiesListEl = document.getElementById('cb-lobby-list');
  const lobbiesEl = document.getElementById('cb-lobbies');
  const debugCategoriesEl = document.getElementById('cb-debug-categories');
  const groupModal = document.getElementById('cb-group-modal');
  const groupModalInput = document.getElementById('cb-group-modal-input');
  const groupModalCreate = document.getElementById('cb-group-modal-create');
  const groupModalCancel = document.getElementById('cb-group-modal-cancel');
  const regexInput = document.getElementById('cb-regex-input');
  const regexTest = document.getElementById('cb-regex-test');
  const regexResult = document.getElementById('cb-regex-result');
  const regexPatternLabel = document.getElementById('cb-regex-pattern-label');
  const regexTestLabel = document.getElementById('cb-regex-test-label');
  const regexLab = document.querySelector('.cb-regex-lab');
  const regexToggle = document.getElementById('cb-regex-toggle');
  const summaryEl = document.getElementById('cb-summary');
  const activityList = document.getElementById('cb-activity-list');
  const activityEmpty = document.getElementById('cb-activity-empty');
  const activityToggle = document.getElementById('cb-activity-toggle');
  const activityReset = document.getElementById('cb-activity-reset');
  const activityCollapse = document.getElementById('cb-activity-collapse');
  const activityReveal = document.getElementById('cb-activity-reveal');
  const activityBody = document.getElementById('cb-activity-body');
  const activityStatus = document.querySelector('.cb-activity-status');
  const activityLabel = document.querySelector('.cb-activity-label');
  const activityCard = document.querySelector('.cb-activity');
  const zonesEl = document.getElementById('cb-zones');
  const offlineList = document.getElementById('cb-offline-list');
  const tutorialEl = document.getElementById('cb-tutorial');
  const tutorialStart = document.getElementById('cb-tutorial-start');
  const tutorialTitle = document.getElementById('cb-tutorial-title');
  const tutorialBody = document.getElementById('cb-tutorial-body');
  const tutorialStep = document.getElementById('cb-tutorial-step');
  const tutorialHint = document.getElementById('cb-tutorial-hint');
  const tutorialPrev = document.getElementById('cb-tutorial-prev');
  const tutorialNext = document.getElementById('cb-tutorial-next');
  const tutorialClose = document.getElementById('cb-tutorial-close');
  const tabButtons = Array.from(document.querySelectorAll('[data-tab]'));
  const tabPanels = Array.from(document.querySelectorAll('[data-tab-panel]'));

  if (!statusEl || !sourceEl || !updatedEl || !modeToggle || !modeLabel || !modeHint || !outputEl || !fileInput || !defaultsBtn || !resetBtn || !downloadBtn
    || !copyBtn || !pasteArea || !pasteLoadBtn || !pasteClearBtn || !serverSuggestions || !lobbiesEl || !lobbiesListEl
    || !groupModal || !groupModalInput || !groupModalCreate || !groupModalCancel
    || !regexLab || !regexToggle
    || !regexInput || !regexTest || !regexResult) {
    return;
  }

  let regexContext = 'lobby';
  const regexTestCache = { lobby: '', hide: '' };
  const isProxyMode = () => configMode === 'proxy';
  const isLobbyMode = () => configMode === 'lobby';
  const withLobbyMode = (handler) => () => {
    if (!isLobbyMode()) {
      return;
    }
    handler();
  };

  const applyModeUi = () => {
    if (builderRoot) {
      builderRoot.dataset.configMode = configMode;
    }
    if (modeToggle) {
      modeToggle.checked = configMode === 'proxy';
    }
    if (modeLabel) {
      modeLabel.textContent = configMode === 'proxy' ? 'Config target: Proxy' : 'Config target: Lobby';
    }
    if (modeHint) {
      modeHint.textContent = configMode === 'proxy'
        ? 'Proxy config (Velocity).'
        : 'Lobby server config (Paper/Fabric/Minestom).';
    }
  };

  const setConfigMode = (nextMode, options = {}) => {
    const desired = nextMode === 'lobby' ? 'lobby' : 'proxy';
    if (configMode === desired && !options.force) {
      return;
    }
    configMode = desired;
    defaultConfig = configMode === 'lobby' ? defaultConfigLobby : defaultConfigProxy;
    applyModeUi();
    if (options.loadDefaults) {
      applyLoadedConfig(defaultConfig, 'defaults', { mode: configMode });
    }
  };

  const detectConfigMode = (raw) => {
    if (!raw || typeof raw !== 'object') {
      return null;
    }
    const proxyKeys = ['lobbies', 'lobby-groups', 'base-hub-command', 'hide-hub-command-on-lobby', 'auto-select', 'last-lobby'];
    const lobbyKeys = ['compass', 'navigator', 'spawn-teleport', 'join-teleport', 'gameplay', 'lobby-npcs', 'lobby-signs', 'minestom', 'bstats'];
    let proxyScore = 0;
    let lobbyScore = 0;
    proxyKeys.forEach((key) => {
      if (raw[key] !== undefined) {
        proxyScore += 1;
      }
    });
    lobbyKeys.forEach((key) => {
      if (raw[key] !== undefined) {
        lobbyScore += 1;
      }
    });
    if (proxyScore === 0 && lobbyScore === 0) {
      return null;
    }
    if (lobbyScore > proxyScore) {
      return 'lobby';
    }
    if (proxyScore > lobbyScore) {
      return 'proxy';
    }
    return null;
  };

  const setRegexContext = (tabId) => {
    if (!regexInput || !regexTest || !regexResult) {
      return;
    }
    let next = regexContext;
    if (tabId === 'core') {
      next = 'hide';
    } else if (tabId === 'lobbies') {
      next = 'lobby';
    }
    if (next === regexContext) {
      return;
    }
    regexTestCache[regexContext] = regexTest.value;
    regexContext = next;
    if (regexContext === 'hide') {
      if (regexPatternLabel) {
        regexPatternLabel.textContent = 'Hide regex';
      }
      regexInput.value = inputs.hideOn.value || '';
      regexTest.placeholder = 'lobby-1';
      regexTest.value = regexTestCache.hide || '';
    } else {
      if (regexPatternLabel) {
        regexPatternLabel.textContent = 'Lobby regex';
      }
      const filter = activeLobby && typeof activeLobby.filter === 'string' ? activeLobby.filter : '';
      regexInput.value = filter;
      regexTest.placeholder = 'lobby-eu-1';
      if (regexTestCache.lobby) {
        regexTest.value = regexTestCache.lobby;
      } else if (filter) {
        const match = findServerForFilter(filter);
        regexTest.value = match || (activeLobby?.name || '');
      } else {
        regexTest.value = '';
      }
    }
    updateRegexResult(regexInput.value, regexTest.value, regexResult);
  };

  const setActiveTab = (tabId) => {
    if (!tabId) {
      return;
    }
    tabButtons.forEach((button) => {
      const active = button.dataset.tab === tabId;
      button.classList.toggle('is-active', active);
      button.setAttribute('aria-selected', active ? 'true' : 'false');
      button.setAttribute('tabindex', active ? '0' : '-1');
    });
    tabPanels.forEach((panel) => {
      panel.classList.toggle('is-active', panel.dataset.tabPanel === tabId);
    });
    setRegexContext(tabId);
    if (tutorialEl && !tutorialEl.hidden && !tutorialState.dismissed && !tutorialState.completed) {
      const step = tutorialSteps[tutorialState.stepIndex || 0];
      const stepId = step ? step.id : '';
      updateTutorialHighlights(stepId);
      scheduleTutorialPosition(stepId);
    }
  };

  const inputs = {
    baseCommand: document.getElementById('cb-base-command'),
    aliases: document.getElementById('cb-aliases'),
    hideOn: document.getElementById('cb-hide-on'),
    autoJoin: document.getElementById('cb-auto-join'),
    autoKick: document.getElementById('cb-auto-kick'),
    lastLobby: document.getElementById('cb-last-lobby'),
    debugEnabled: document.getElementById('cb-debug-enabled'),
    debugPermission: document.getElementById('cb-debug-permission'),
    messageSuccess: document.getElementById('cb-message-success'),
    messageAlready: document.getElementById('cb-message-already'),
    messageProgress: document.getElementById('cb-message-progress'),
    messageDisconnected: document.getElementById('cb-message-disconnected'),
    messageCancelled: document.getElementById('cb-message-cancelled'),
    systemPlayersOnly: document.getElementById('cb-system-players-only'),
    systemNoLobby: document.getElementById('cb-system-no-lobby'),
    kickEnabled: document.getElementById('cb-kick-enabled'),
    kickPrefix: document.getElementById('cb-kick-prefix'),
    kickSuffix: document.getElementById('cb-kick-suffix'),
    i18nDefaultLocale: document.getElementById('cb-i18n-default-locale'),
    i18nUseClient: document.getElementById('cb-i18n-use-client'),
    finderStart: document.getElementById('cb-finder-start'),
    finderIncrement: document.getElementById('cb-finder-increment'),
    finderMax: document.getElementById('cb-finder-max'),
    finderRefresh: document.getElementById('cb-finder-refresh'),
    dataCollectEnabled: document.getElementById('cb-data-collect-enabled'),
    dataCollectUuid: document.getElementById('cb-data-collect-uuid'),
    dataCollectFile: document.getElementById('cb-data-collect-file'),
    dataCollectInterval: document.getElementById('cb-data-collect-interval'),
    dataCollectUsers: document.getElementById('cb-data-collect-users'),
    dataCollectServers: document.getElementById('cb-data-collect-servers'),
  };
  const lobbyInputs = {
    compassEnabled: document.getElementById('cb-lobby-compass-enabled'),
    compassTitle: document.getElementById('cb-lobby-compass-title'),
    navigatorEnabled: document.getElementById('cb-lobby-navigator-enabled'),
    navigatorTitle: document.getElementById('cb-lobby-navigator-title'),
    spawnTeleportEnabled: document.getElementById('cb-lobby-spawn-enabled'),
    joinTeleportMode: document.getElementById('cb-lobby-join-teleport-mode'),
    joinTeleportDelay: document.getElementById('cb-lobby-join-teleport-delay'),
    joinTeleportMaxAge: document.getElementById('cb-lobby-join-teleport-max-age'),
    npcEnabled: document.getElementById('cb-lobby-npcs-enabled'),
    signEnabled: document.getElementById('cb-lobby-signs-enabled'),
    gameplayDamage: document.getElementById('cb-lobby-gameplay-damage'),
    gameplayHunger: document.getElementById('cb-lobby-gameplay-hunger'),
    gameplayHeal: document.getElementById('cb-lobby-gameplay-heal'),
    bstatsEnabled: document.getElementById('cb-lobby-bstats-enabled'),
    minestomSecret: document.getElementById('cb-lobby-minestom-secret')
  };
  const debugCategoryInputs = {
    general: document.getElementById('cb-debug-cat-general'),
    commands: document.getElementById('cb-debug-cat-commands'),
    finder: document.getElementById('cb-debug-cat-finder'),
    pings: document.getElementById('cb-debug-cat-pings'),
    compass: document.getElementById('cb-debug-cat-compass'),
    permissions: document.getElementById('cb-debug-cat-permissions'),
    transfer: document.getElementById('cb-debug-cat-transfer'),
    events: document.getElementById('cb-debug-cat-events'),
    placeholders: document.getElementById('cb-debug-cat-placeholders'),
    'forced-hosts': document.getElementById('cb-debug-cat-forced-hosts'),
    'last-lobby': document.getElementById('cb-debug-cat-last-lobby'),
    config: document.getElementById('cb-debug-cat-config'),
  };

  const fallbackDebugCategories = {
    general: true,
    commands: true,
    finder: true,
    pings: false,
    compass: true,
    permissions: true,
    transfer: true,
    events: true,
    placeholders: true,
    'forced-hosts': true,
    'last-lobby': true,
    config: true
  };
  const fallbackI18nLocales = ['en_us', 'de_de'];
  const forcedHostsList = document.getElementById('cb-forced-hosts');
  const forcedHostsAdd = document.getElementById('cb-forced-host-add');
  const i18nOverridesList = document.getElementById('cb-i18n-overrides');
  const i18nAddLocale = document.getElementById('cb-i18n-add-locale');

  const builderRoot = document.querySelector('.config-builder');
  let yamlLib;
  let config = {};
  let baselineConfig = {};
  let defaultConfig = {};
  let defaultConfigProxy = {};
  let defaultConfigLobby = {};
  let configMode = 'proxy';
  let currentDownloadUrl = null;
  let analysisData = { servers: [], users: [] };
  let selectedUserId = '';
  let activeLobby = null;
  let lobbySubTab = 'details';
  let draggingLobby = null;
  let draggingGroup = '';
  let pendingGroupSelect = null;
  let pendingGroupLobby = null;
  let pendingGroupPrevious = '';
  let userIndex = new Map();
  let forcedHostDrafts = [];
  let i18nOverrideDrafts = [];
  const messagePreviewMap = new Map();
  const overridePreviewMap = new Map();
  const fallbackBound = new WeakSet();
  const avatarCache = new Map();
  const fallbackActivity = {
    servers: ['lobby-eu-1', 'lobby-us-1', 'teamlobby-eu-1', 'premiumlobby-1', 'ffa-1', 'lobby-minestom-1'],
    users: [
      { id: 'freddiio', name: 'Freddiio', uuid: 'c5eb5df7-b7a9-4919-a9bc-7f59c8bee980', permissions: ['hub.user', 'hub.team', 'hub.premium'] },
      { id: 'luma', name: 'Luma', uuid: '9d6f3c0e-124a-41d8-8f7e-7b7a7d0b2c11', permissions: ['hub.user', 'hub.team'] },
      { id: 'pixelpanda', name: 'PixelPanda', uuid: '7a4f1d2b-6b7c-4f06-84b1-8ed6a8134c35', permissions: ['hub.user'] },
      { id: 'rook', name: 'Rook', uuid: '2f9f18a2-45e4-4b7a-9b53-7c7f4d5c2b88', permissions: ['hub.user', 'hub.premium'] }
    ]
  };
  const activityState = {
    running: true,
    timer: null,
    maxItems: 6,
    lastServerByUser: new Map(),
    serverUsers: new Map(),
    activeServer: '',
    journeyTimers: new Set(),
    historyByUser: new Map(),
    historyLimit: 8
  };
  const userStatus = new Map();
  const tutorialStorageKey = 'hub-config-builder-tutorial-v1';
  let tutorialState = loadTutorialState();
  const tutorialSteps = [
    {
      id: 'core',
      tab: 'core',
      title: 'Core settings',
      body: 'Core settings are already good. Just review them before moving on.',
      requirement: 'No action needed.',
      isComplete: () => true
    },
    {
      id: 'messages',
      tab: 'messages',
      title: 'Messages',
      body: 'Edit at least one message so players get the right feedback.',
      requirement: 'Optional: change a message if you want custom copy.',
      isComplete: () => tutorialState.actions.messagesEdited
    },
    {
      id: 'finder',
      tab: 'finder',
      title: 'Finder + Data',
      body: 'Adjust finder timings or data collection so routing is tuned.',
      requirement: 'Optional: tweak a finder or data collection field.',
      isComplete: () => tutorialState.actions.finderEdited
    },
    {
      id: 'lobbies',
      tab: 'lobbies',
      title: 'Lobbies',
      body: 'Edit a lobby filter and test grouping via drag & drop.',
      requirement: 'Optional: try editing a filter or moving a lobby between groups.',
      isComplete: () => tutorialState.actions.lobbyFilterEdited
        && tutorialState.actions.lobbyGrouped
        && tutorialState.actions.lobbyUngrouped
    }
  ];

  const isTutorialActive = () => Boolean(tutorialEl && !tutorialEl.hidden && !tutorialState.dismissed && !tutorialState.completed);

  if (tabButtons.length && tabPanels.length) {
    setActiveTab(tabButtons[0].dataset.tab);
    tabButtons.forEach((button) => {
      button.addEventListener('click', () => {
        if (isTutorialActive()) {
          return;
        }
        setActiveTab(button.dataset.tab);
      });
    });
  }

  function showStatus(text, tone) {
    statusEl.textContent = text;
    statusEl.dataset.tone = tone || 'idle';
  }

  function setSource(text) {
    sourceEl.textContent = text || 'custom';
  }

  function setUpdated() {
    updatedEl.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  function loadTutorialState() {
    const base = {
      stepIndex: 0,
      dismissed: false,
      completed: false,
      actions: {
        coreEdited: false,
        messagesEdited: false,
        finderEdited: false,
        lobbyFilterEdited: false,
        lobbyGrouped: false,
        lobbyUngrouped: false
      }
    };
    if (typeof window === 'undefined' || !tutorialStorageKey) {
      return base;
    }
    try {
      const stored = localStorage.getItem(tutorialStorageKey);
      if (!stored) {
        return base;
      }
      const parsed = JSON.parse(stored);
      return {
        ...base,
        ...parsed,
        actions: {
          ...base.actions,
          ...(parsed && parsed.actions ? parsed.actions : {})
        }
      };
    } catch (error) {
      return base;
    }
  }

  function saveTutorialState() {
    if (typeof window === 'undefined' || !tutorialStorageKey) {
      return;
    }
    try {
      localStorage.setItem(tutorialStorageKey, JSON.stringify(tutorialState));
    } catch (error) {
      // ignore storage errors
    }
  }

  function resetTutorialState() {
    tutorialState = loadTutorialState();
    tutorialState.stepIndex = 0;
    tutorialState.dismissed = false;
    tutorialState.completed = false;
    tutorialState.actions = {
      coreEdited: false,
      messagesEdited: false,
      finderEdited: false,
      lobbyFilterEdited: false,
      lobbyGrouped: false,
      lobbyUngrouped: false
    };
    saveTutorialState();
  }

  function markTutorialAction(actionKey) {
    if (!tutorialEl || !tutorialState.actions || !actionKey) {
      return;
    }
    if (tutorialState.actions[actionKey]) {
      return;
    }
    tutorialState.actions[actionKey] = true;
    saveTutorialState();
    renderTutorial();
  }

  function clearTutorialHighlights() {
    document.querySelectorAll('.cb-tutorial-highlight').forEach((el) => {
      el.classList.remove('cb-tutorial-highlight');
    });
  }

  function updateTutorialHighlights(stepId) {
    clearTutorialHighlights();
    if (!stepId) {
      const builder = document.querySelector('.config-builder');
      if (builder) {
        builder.removeAttribute('data-tutorial-step');
      }
      return;
    }
    const builder = document.querySelector('.config-builder');
    if (builder) {
      builder.setAttribute('data-tutorial-step', stepId);
    }
    document.querySelectorAll('[data-tutorial]').forEach((el) => {
      const tokens = (el.getAttribute('data-tutorial') || '').split(/\s+/).filter(Boolean);
      if (tokens.includes(stepId)) {
        el.classList.add('cb-tutorial-highlight');
      }
    });
  }

  function resetTutorialPosition() {
    if (!tutorialEl) {
      return;
    }
    tutorialEl.style.left = '';
    tutorialEl.style.top = '';
    tutorialEl.style.right = '';
    tutorialEl.style.bottom = '';
  }

  function getTutorialAnchor(stepId) {
    if (!stepId) {
      return null;
    }
    return document.querySelector(`[data-tutorial-anchor~="${stepId}"]`)
      || document.querySelector(`[data-tutorial~="${stepId}"]`);
  }

  function positionTutorial(stepId) {
    if (!tutorialEl || tutorialEl.hidden) {
      return;
    }
    const card = tutorialEl.querySelector('.cb-tutorial-card');
    if (!card) {
      return;
    }
    const target = getTutorialAnchor(stepId);
    if (!target) {
      resetTutorialPosition();
      return;
    }
    const rect = target.getBoundingClientRect();
    const cardRect = card.getBoundingClientRect();
    const viewportW = window.innerWidth;
    const viewportH = window.innerHeight;
    const margin = 16;
    const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
    const align = (target.getAttribute('data-tutorial-align') || '').toLowerCase();
    let left = margin;
    let top = margin;

    if (align === 'right') {
      const preferredLeft = rect.right + margin;
      const rightDock = viewportW - cardRect.width - margin;
      left = preferredLeft + cardRect.width <= viewportW - margin
        ? preferredLeft
        : rightDock;
      top = clamp(rect.top, margin, viewportH - cardRect.height - margin);
      tutorialEl.style.left = `${Math.round(left)}px`;
      tutorialEl.style.top = `${Math.round(top)}px`;
      tutorialEl.style.right = 'auto';
      tutorialEl.style.bottom = 'auto';
      const allowAutoScroll = target.getAttribute('data-tutorial-scroll') !== 'false';
      const focusMargin = 80;
      if (allowAutoScroll && (rect.top < focusMargin || rect.bottom > viewportH - focusMargin)) {
        target.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      return;
    }

    const fitsRight = rect.right + margin + cardRect.width <= viewportW - margin;
    const fitsLeft = rect.left - margin - cardRect.width >= margin;
    const fitsBelow = rect.bottom + margin + cardRect.height <= viewportH - margin;
    const fitsAbove = rect.top - margin - cardRect.height >= margin;

    if (fitsRight) {
      left = rect.right + margin;
      top = clamp(rect.top, margin, viewportH - cardRect.height - margin);
    } else if (fitsLeft) {
      left = rect.left - margin - cardRect.width;
      top = clamp(rect.top, margin, viewportH - cardRect.height - margin);
    } else if (fitsBelow) {
      left = clamp(rect.left, margin, viewportW - cardRect.width - margin);
      top = rect.bottom + margin;
    } else if (fitsAbove) {
      left = clamp(rect.left, margin, viewportW - cardRect.width - margin);
      top = rect.top - margin - cardRect.height;
    } else {
      left = clamp(rect.left, margin, viewportW - cardRect.width - margin);
      top = clamp(rect.top, margin, viewportH - cardRect.height - margin);
    }

    const allowAutoScroll = target.getAttribute('data-tutorial-scroll') !== 'false';
    const focusMargin = 80;
    if (allowAutoScroll && (rect.top < focusMargin || rect.bottom > viewportH - focusMargin)) {
      target.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    tutorialEl.style.left = `${Math.round(left)}px`;
    tutorialEl.style.top = `${Math.round(top)}px`;
    tutorialEl.style.right = 'auto';
    tutorialEl.style.bottom = 'auto';
  }

  let tutorialPositionRaf = null;
  function scheduleTutorialPosition(stepId) {
    if (tutorialPositionRaf) {
      cancelAnimationFrame(tutorialPositionRaf);
    }
    tutorialPositionRaf = requestAnimationFrame(() => positionTutorial(stepId));
  }

  function setTutorialStep(index) {
    if (!tutorialEl || !tutorialSteps.length) {
      return;
    }
    const maxIndex = tutorialSteps.length - 1;
    const nextIndex = Math.min(Math.max(index, 0), maxIndex);
    tutorialState.stepIndex = nextIndex;
    saveTutorialState();
    const step = tutorialSteps[nextIndex];
    if (step && step.tab) {
      setActiveTab(step.tab);
    }
    renderTutorial();
  }

  function renderTutorial() {
    if (!tutorialEl || !tutorialTitle || !tutorialBody || !tutorialStep || !tutorialHint || !tutorialPrev || !tutorialNext) {
      return;
    }
    if (tutorialState.dismissed || tutorialState.completed) {
      tutorialEl.hidden = true;
      clearTutorialHighlights();
      resetTutorialPosition();
      const builder = document.querySelector('.config-builder');
      if (builder) {
        builder.removeAttribute('data-tutorial-step');
      }
      tabButtons.forEach((button) => {
        button.disabled = false;
        button.setAttribute('aria-disabled', 'false');
      });
      return;
    }
    const stepIndex = Math.min(Math.max(tutorialState.stepIndex || 0, 0), tutorialSteps.length - 1);
    const step = tutorialSteps[stepIndex];
    if (!step) {
      tutorialEl.hidden = true;
      clearTutorialHighlights();
      resetTutorialPosition();
      const builder = document.querySelector('.config-builder');
      if (builder) {
        builder.removeAttribute('data-tutorial-step');
      }
      tabButtons.forEach((button) => {
        button.disabled = false;
        button.setAttribute('aria-disabled', 'false');
      });
      return;
    }
    tutorialEl.hidden = false;
    tabButtons.forEach((button) => {
      button.disabled = true;
      button.setAttribute('aria-disabled', 'true');
    });
    tutorialStep.textContent = `Step ${stepIndex + 1} / ${tutorialSteps.length}`;
    tutorialTitle.textContent = step.title;
    tutorialBody.innerHTML = `<p>${step.body}</p>`;
    tutorialHint.textContent = step.requirement;
    tutorialNext.disabled = false;
    tutorialPrev.disabled = stepIndex === 0;
    tutorialNext.textContent = stepIndex === tutorialSteps.length - 1 ? 'Done' : 'Next';
    updateTutorialHighlights(step.id);
    scheduleTutorialPosition(step.id);
  }

  function enhanceHints(root) {
    const scope = root || document;
    const hintTargets = scope.querySelectorAll('.cb-field > span[title], label.cb-file > span[title]');
    hintTargets.forEach((el) => {
      const hint = el.getAttribute('title');
      if (!hint || el.dataset.hint) {
        return;
      }
      el.dataset.hint = hint;
      el.removeAttribute('title');
      el.classList.add('cb-hint');
      const icon = document.createElement('span');
      icon.className = 'cb-hint-icon';
      icon.textContent = 'i';
      icon.setAttribute('aria-hidden', 'true');
      el.appendChild(icon);
    });
  }

  function updateSummary() {
    if (!summaryEl) {
      return;
    }
    if (configMode === 'lobby') {
      summaryEl.innerHTML = '<p class="cb-summary-note">Lobby config loaded.</p>';
      return;
    }
    const groupsCount = Array.isArray(config['lobby-groups']) ? config['lobby-groups'].length : 0;
    const lobbies = Array.isArray(config.lobbies) ? config.lobbies : [];
    const commandNames = [];
    const addCommandName = (name) => {
      const clean = String(name || '').trim().replace(/^\//, '');
      if (!clean || commandNames.includes(clean)) {
        return;
      }
      commandNames.push(clean);
    };
    lobbies.forEach((lobby) => {
      if (lobby && typeof lobby.commands === 'object' && lobby.commands) {
        Object.keys(lobby.commands).forEach(addCommandName);
      }
    });
    const baseCommand = config['base-hub-command'] || 'hub';
    const aliases = Array.isArray(config.aliases) ? config.aliases : [];
    const aliasCount = aliases.length;
    const autoJoin = Boolean(config['auto-select']?.['on-join']);
    const autoKick = Boolean(config['auto-select']?.['on-server-kick']);
    const lastLobby = Boolean(config['last-lobby']?.enabled);
    const finder = config.finder || {};
    const dataCollection = config['data-collection'] || {};
    const debugEnabled = Boolean(config.debug?.enabled);
    const debugPerm = config.debug?.permission || '';
    const hideRegex = config['hide-hub-command-on-lobby'] || '';
    const kickEnabled = Boolean(config['kick-message']?.enabled);
    const kickPrefix = config['kick-message']?.prefix ?? '';
    const kickSuffix = config['kick-message']?.suffix ?? '';
    const includeUuid = Boolean(dataCollection['include-uuid']);

    const trim = (value, max = 22) => {
      const text = String(value || '');
      if (!text) {
        return '—';
      }
      return text.length > max ? `${text.slice(0, max - 1)}…` : text;
    };

    const aliasPreview = aliasCount
      ? aliasCount <= 3
        ? aliases.join(', ')
        : `${aliases.slice(0, 2).join(', ')} +${aliasCount - 2}`
      : '—';
    const commandPreview = commandNames.length
      ? commandNames.length <= 4
        ? commandNames.join(', ')
        : `${commandNames.slice(0, 4).join(', ')} +${commandNames.length - 4}`
      : '—';
    const groups = [
      {
        title: 'Commands',
        items: [
          { label: 'Command', value: `/${baseCommand}` },
          { label: 'Aliases', value: aliasPreview },
          { label: 'Hide regex', value: trim(hideRegex, 26) },
          { label: `Commands (${commandNames.length})`, value: commandPreview }
        ]
      },
      {
        title: 'Routing',
        items: [
          { label: 'Lobbies', value: `${lobbies.length} (${groupsCount} groups)` },
          { label: 'Auto-select', value: `${autoJoin ? 'join' : 'off'} / ${autoKick ? 'kick' : 'off'}` },
          { label: 'Last lobby', value: lastLobby ? 'on' : 'off' }
        ]
      },
      {
        title: 'System',
        items: [
          { label: 'Debug', value: debugEnabled ? `on (${debugPerm || 'perm'})` : 'off' },
          { label: 'Kick msg', value: kickEnabled ? `${trim(kickPrefix, 10)}…${trim(kickSuffix, 10)}` : 'off' }
        ]
      },
      {
        title: 'Finder + Data',
        items: [
          {
            label: 'Finder',
            value: `${finder['start-duration'] ?? finder.startDuration ?? 0}s +${finder['increment-duration'] ?? finder.incrementDuration ?? 0}s max ${finder['max-duration'] ?? finder.maxDuration ?? 0}s / ${finder['refresh-interval-in-ticks'] ?? finder.refreshIntervalInTicks ?? 0}t`
          },
          {
            label: 'Data dump',
            value: dataCollection.enabled
              ? `${dataCollection['dump-file'] || 'data-dump.yml'} / ${dataCollection['dump-interval-minutes'] ?? 0}m`
              : 'off'
          },
          {
            label: 'Data caps',
            value: dataCollection.enabled
              ? `${dataCollection['max-users'] ?? 0} users / ${dataCollection['max-servers'] ?? 0} servers`
              : '—'
          },
          { label: 'UUIDs', value: dataCollection.enabled ? (includeUuid ? 'on' : 'off') : '—' }
        ]
      }
    ];
    summaryEl.innerHTML = '';
    groups.forEach((group) => {
      const wrapper = document.createElement('div');
      wrapper.className = 'cb-summary-group';
      const title = document.createElement('h3');
      title.className = 'cb-summary-title';
      title.textContent = group.title;
      const list = document.createElement('div');
      list.className = 'cb-summary-list';
      group.items.forEach((item) => {
        const row = document.createElement('div');
        row.className = 'cb-summary-row';
        const label = document.createElement('span');
        label.className = 'cb-summary-label';
        label.textContent = item.label;
        const value = document.createElement('span');
        value.className = 'cb-summary-value';
        value.textContent = item.value;
        row.appendChild(label);
        row.appendChild(value);
        list.appendChild(row);
      });
      wrapper.appendChild(title);
      wrapper.appendChild(list);
      summaryEl.appendChild(wrapper);
    });
  }

  function setDataSummary(text, state) {
    if (dataSummary) {
      dataSummary.textContent = text;
      dataSummary.dataset.state = state || 'idle';
    }
    if (dataSummaryInline) {
      dataSummaryInline.textContent = text;
      dataSummaryInline.dataset.state = state || 'idle';
    }
  }

  function getActivitySource() {
    const useDump = analysisData.users.length || analysisData.servers.length;
    const source = useDump ? analysisData : fallbackActivity;
    const users = source.users.length ? source.users : fallbackActivity.users;
    const servers = source.servers.length ? source.servers : fallbackActivity.servers;
    return { users, servers };
  }

  function hashString(value) {
    let hash = 0;
    for (let i = 0; i < value.length; i += 1) {
      hash = (hash * 31 + value.charCodeAt(i)) >>> 0;
    }
    return hash;
  }

  function initialsFor(name) {
    const clean = String(name || '').trim();
    if (!clean) {
      return 'U';
    }
    const parts = clean.split(/\s+/).filter(Boolean);
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  function buildFallbackAvatar(user) {
    const seed = user.id || user.uuid || user.name || 'user';
    if (avatarCache.has(seed)) {
      return avatarCache.get(seed);
    }
    const hash = hashString(seed);
    const hueA = hash % 360;
    const hueB = (hash * 7 + 120) % 360;
    const colorA = `hsl(${hueA}, 62%, 56%)`;
    const colorB = `hsl(${hueB}, 70%, 42%)`;
    const initials = initialsFor(user.name || user.uuid || 'User');
    const svg = `
      <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64">
        <defs>
          <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${colorA}"/>
            <stop offset="100%" stop-color="${colorB}"/>
          </linearGradient>
        </defs>
        <rect width="64" height="64" rx="18" fill="url(#g)"/>
        <text x="50%" y="54%" text-anchor="middle" font-family="Arial, sans-serif" font-size="26" font-weight="700" fill="#ffffff">${initials}</text>
      </svg>
    `.trim();
    const dataUri = `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
    avatarCache.set(seed, dataUri);
    return dataUri;
  }

  function avatarSources(user) {
    const name = user.name || '';
    const uuid = user.uuid || '';
    const sources = [];
    if (uuid) {
      sources.push(`https://crafatar.com/avatars/${uuid}?size=64&overlay`);
      sources.push(`https://mc-heads.net/avatar/${uuid}/64`);
      sources.push(`https://minotar.net/helm/${uuid}/64`);
    }
    if (name) {
      sources.push(`https://minotar.net/helm/${encodeURIComponent(name)}/64`);
      sources.push(`https://mc-heads.net/avatar/${encodeURIComponent(name)}/64`);
    }
    sources.push(buildFallbackAvatar(user));
    return sources;
  }

  function setAvatarImage(img, sources) {
    if (!sources.length) {
      return;
    }
    let index = 0;
    const setNext = () => {
      if (index >= sources.length) {
        return;
      }
      img.src = sources[index];
    };
    img.onerror = () => {
      index += 1;
      if (index < sources.length) {
        setNext();
      }
    };
    setNext();
  }

  function formatAge(ms) {
    if (ms < 4000) {
      return 'just now';
    }
    const seconds = Math.floor(ms / 1000);
    if (seconds < 60) {
      return `${seconds}s ago`;
    }
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) {
      return `${minutes}m ago`;
    }
    const hours = Math.floor(minutes / 60);
    return `${hours}h ago`;
  }

  function updateActivityTimes() {
    const now = Date.now();
    const items = activityList.querySelectorAll('.cb-activity-item');
    items.forEach((item) => {
      const ts = Number.parseInt(item.dataset.timestamp || '0', 10);
      const meta = item.querySelector('.cb-activity-meta');
      if (meta && ts) {
        meta.textContent = formatAge(now - ts);
      }
    });
  }

  function getUserKey(user) {
    if (!user) {
      return '';
    }
    return user.id || user.uuid || user.name || '';
  }

  function setUserStatus(user, status) {
    const key = getUserKey(user);
    if (!key) {
      return;
    }
    userStatus.set(key, status);
  }

  function getUserStatus(user) {
    const key = getUserKey(user);
    if (!key) {
      return 'offline';
    }
    return userStatus.get(key) || 'offline';
  }

  function syncUserStatus() {
    const users = getActivitySource().users;
    const validKeys = new Set();
    users.forEach((user) => {
      const key = getUserKey(user);
      if (!key) {
        return;
      }
      validKeys.add(key);
      if (!userStatus.has(key)) {
        userStatus.set(key, 'offline');
      }
    });
    Array.from(userStatus.keys()).forEach((key) => {
      if (!validKeys.has(key)) {
        userStatus.delete(key);
      }
    });
  }

  function zoneBucket(name) {
    const lower = name.toLowerCase();
    if (lower.includes('eu')) {
      return 'EU';
    }
    if (lower.includes('us')) {
      return 'US';
    }
    if (lower.includes('premium')) {
      return 'Premium';
    }
    if (lower.includes('team')) {
      return 'Team';
    }
    if (lower.includes('minestom')) {
      return 'Minestom';
    }
    if (lower.includes('ffa')) {
      return 'FFA';
    }
    return 'Core';
  }

  function buildZones() {
    const { servers } = getActivitySource();
    const map = new Map();
    servers.forEach((server) => {
      const zone = zoneBucket(server);
      if (!map.has(zone)) {
        map.set(zone, []);
      }
      map.get(zone).push(server);
    });
    const order = ['EU', 'US', 'Premium', 'Team', 'Minestom', 'FFA', 'Core'];
    const zones = [];
    order.forEach((zone) => {
      if (map.has(zone)) {
        zones.push({ name: zone, servers: map.get(zone) });
        map.delete(zone);
      }
    });
    map.forEach((servers, zone) => zones.push({ name: zone, servers }));
    if (!zones.length) {
      zones.push({ name: 'Core', servers: [] });
    }
    return zones;
  }

  function matchLobbyForServer(serverName) {
    if (!config || !Array.isArray(config.lobbies)) {
      return null;
    }
    for (const lobby of config.lobbies) {
      const filter = typeof lobby.filter === 'string' ? lobby.filter : '';
      if (!filter) {
        continue;
      }
      let regex;
      try {
        regex = buildRegex(filter);
      } catch (error) {
        regex = null;
      }
      if (regex && regex.test(serverName)) {
        return lobby;
      }
    }
    return null;
  }

  function groupsForLobby(lobbyName) {
    if (!lobbyName || !config || !Array.isArray(config['lobby-groups'])) {
      return [];
    }
    return config['lobby-groups']
      .filter((group) => Array.isArray(group.lobbies) && group.lobbies.includes(lobbyName))
      .map((group) => group.name)
      .filter(Boolean);
  }

  function lobbyInfoForServer(serverName) {
    const lobby = matchLobbyForServer(serverName);
    if (!lobby) {
      return { lobbyName: '', groups: [] };
    }
    const lobbyName = lobby.name || '';
    const groups = groupsForLobby(lobbyName);
    return { lobbyName, groups };
  }

  function getCommandOptions() {
    const list = [];
    const add = (cmd) => {
      if (!cmd) {
        return;
      }
      const clean = String(cmd).trim().replace(/^\//, '');
      if (!clean || list.includes(clean)) {
        return;
      }
      list.push(clean);
    };
    add(config['base-hub-command']);
    if (Array.isArray(config.aliases)) {
      config.aliases.forEach(add);
    }
    if (Array.isArray(config.lobbies)) {
      config.lobbies.forEach((lobby) => {
        if (lobby && typeof lobby.commands === 'object' && lobby.commands) {
          Object.keys(lobby.commands).forEach(add);
        }
      });
    }
    if (!list.length) {
      list.push('hub');
    }
    return list;
  }

  function eventActionLabel(event) {
    if (!event) {
      return 'Idle';
    }
    if (event.command) {
      return `${event.user.name || event.user.id} used ${event.command}`;
    }
    const name = event.user.name || event.user.id;
    const phase = event.phase || '';
    if (phase === 'join') {
      return `${name} joined the proxy`;
    }
    if (phase === 'lobby-pick') {
      return `${name} picked a lobby`;
    }
    if (phase === 'game-pick') {
      return `${name} entered a game`;
    }
    if (phase === 'game-end') {
      return `${name} finished a game`;
    }
    if (phase === 'lobby-return') {
      return `${name} returned to lobby`;
    }
    if (phase === 'lobby-switch') {
      return `${name} switched lobby`;
    }
    if (phase === 'proxy-leave') {
      return `${name} left the proxy`;
    }
    if (event.type === 'leave') {
      return `${name} left the proxy`;
    }
    return `${name} ${event.type === 'kick' ? 'kicked' : event.type === 'switch' ? 'switched' : 'connected'}`;
  }

  function eventActionShort(event) {
    if (!event) {
      return 'Idle';
    }
    const phase = event.phase || '';
    if (phase === 'join') {
      return 'Joined proxy';
    }
    if (phase === 'lobby-pick') {
      return 'Picked lobby';
    }
    if (phase === 'game-pick') {
      return 'Entered game';
    }
    if (phase === 'game-end') {
      return 'Game ended';
    }
    if (phase === 'lobby-return') {
      return 'Returned to lobby';
    }
    if (phase === 'lobby-switch') {
      return 'Switched lobby';
    }
    if (phase === 'proxy-leave') {
      return 'Left proxy';
    }
    if (event.type === 'kick') {
      return 'Kicked';
    }
    if (event.type === 'switch') {
      return 'Switched';
    }
    if (event.type === 'leave') {
      return 'Left proxy';
    }
    return 'Connected';
  }

  function eventDetailLabel(event) {
    if (!event) {
      return 'No activity';
    }
    if (event.command) {
      return `Command: ${event.command}`;
    }
    if (event.detail === 'system-transfer') {
      return 'System: lobby transfer';
    }
    if (event.type === 'kick') {
      if (event.kickType === 'game') {
        return event.reason ? `System: game server kick (${event.reason})` : 'System: game server kick';
      }
      if (event.kickType === 'proxy') {
        return event.reason ? `System: proxy kick (${event.reason})` : 'System: proxy kick';
      }
      return event.reason ? `System: kicked (${event.reason})` : 'System: kicked';
    }
    if (event.type === 'leave' || event.phase === 'proxy-leave') {
      return 'System: disconnected';
    }
    return 'System: auto-join';
  }

  function buildHistoryEntry(type, label, short, tone, detail) {
    return { type, label, short, tone, detail };
  }

  function getHistoryEntriesForEvent(event) {
    if (!event) {
      return [];
    }
    const entries = [];
    const phase = event.phase || '';
    const server = event.server || '';
    const prev = event.prevServer || '';
    if (event.command) {
      entries.push(buildHistoryEntry('command', `Command ${event.command}`, 'CMD', 'info', event.command));
    }
    if (phase === 'join') {
      entries.push(buildHistoryEntry('join', 'Joined proxy', 'JOIN', 'ok', server));
      return entries;
    }
    if (phase === 'lobby-pick') {
      entries.push(buildHistoryEntry('lobby', 'Picked lobby', 'LOBBY', 'ok', server));
      return entries;
    }
    if (phase === 'game-pick') {
      entries.push(buildHistoryEntry('game', 'Entered game', 'GAME', 'ok', server));
      return entries;
    }
    if (phase === 'game-end') {
      entries.push(buildHistoryEntry('game-end', 'Game ended', 'END', 'warn', server));
      return entries;
    }
    if (phase === 'lobby-return') {
      entries.push(buildHistoryEntry('lobby-return', 'Returned to lobby', 'BACK', 'ok', server));
      return entries;
    }
    if (phase === 'lobby-switch') {
      const detail = prev && server ? `${prev} -> ${server}` : server;
      entries.push(buildHistoryEntry('switch', 'Switched lobby', 'SWAP', 'warn', detail));
      return entries;
    }
    if (phase === 'proxy-leave') {
      entries.push(buildHistoryEntry('leave', 'Left proxy', 'LEAVE', 'warn', 'proxy'));
      return entries;
    }
    if (event.type === 'kick') {
      const label = event.kickType === 'proxy'
        ? 'Proxy kick'
        : event.kickType === 'game'
          ? 'Game kick'
          : 'Kicked';
      entries.push(buildHistoryEntry('kick', label, 'KICK', 'bad', event.reason || server));
      return entries;
    }
    if (event.type === 'switch') {
      const detail = prev && server ? `${prev} -> ${server}` : server;
      entries.push(buildHistoryEntry('switch', 'Switched server', 'SWAP', 'warn', detail));
      return entries;
    }
    if (event.type === 'leave') {
      entries.push(buildHistoryEntry('leave', 'Left proxy', 'LEAVE', 'warn', 'proxy'));
      return entries;
    }
    entries.push(buildHistoryEntry('connect', 'Connected', 'JOIN', 'ok', server));
    return entries;
  }

  function updateHistoryForEvent(event) {
    if (!event || !event.user) {
      return;
    }
    const key = getUserKey(event.user);
    if (!key) {
      return;
    }
    const entries = getHistoryEntriesForEvent(event);
    if (!entries.length) {
      return;
    }
    const list = activityState.historyByUser.get(key) || [];
    entries.forEach((entry) => list.push(entry));
    while (list.length > activityState.historyLimit) {
      list.shift();
    }
    activityState.historyByUser.set(key, list);
  }

  function createActivityHistoryIcon(type) {
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.classList.add('cb-activity-icon');
    const strokeProps = {
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': '2',
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round'
    };
    const addPath = (d) => {
      const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
      path.setAttribute('d', d);
      Object.entries(strokeProps).forEach(([key, value]) => path.setAttribute(key, value));
      svg.appendChild(path);
    };
    if (type === 'command') {
      const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
      rect.setAttribute('x', '4');
      rect.setAttribute('y', '5');
      rect.setAttribute('width', '16');
      rect.setAttribute('height', '14');
      rect.setAttribute('rx', '2');
      Object.entries(strokeProps).forEach(([key, value]) => rect.setAttribute(key, value));
      svg.appendChild(rect);
      addPath('M8 9l3 3-3 3');
      addPath('M12 15h4');
      return svg;
    }
    if (type === 'lobby') {
      addPath('M4 12l8-6 8 6');
      addPath('M6 11v7h12v-7');
      return svg;
    }
    if (type === 'game' || type === 'game-end') {
      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      circle.setAttribute('cx', '12');
      circle.setAttribute('cy', '12');
      circle.setAttribute('r', '7');
      Object.entries(strokeProps).forEach(([key, value]) => circle.setAttribute(key, value));
      svg.appendChild(circle);
      addPath('M10 9l5 3-5 3z');
      return svg;
    }
    if (type === 'switch') {
      addPath('M7 7h10');
      addPath('M13 4l4 3-4 3');
      addPath('M17 17H7');
      addPath('M11 20l-4-3 4-3');
      return svg;
    }
    if (type === 'kick') {
      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      circle.setAttribute('cx', '12');
      circle.setAttribute('cy', '12');
      circle.setAttribute('r', '7');
      Object.entries(strokeProps).forEach(([key, value]) => circle.setAttribute(key, value));
      svg.appendChild(circle);
      addPath('M9 9l6 6');
      addPath('M15 9l-6 6');
      return svg;
    }
    if (type === 'leave') {
      addPath('M5 6h7v12H5z');
      addPath('M12 12h7');
      addPath('M16 9l3 3-3 3');
      return svg;
    }
    const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    circle.setAttribute('cx', '12');
    circle.setAttribute('cy', '12');
    circle.setAttribute('r', '7');
    Object.entries(strokeProps).forEach(([key, value]) => circle.setAttribute(key, value));
    svg.appendChild(circle);
    addPath('M9 12h6');
    addPath('M13 9l3 3-3 3');
    return svg;
  }

  function getPreferredUser(event) {
    if (event && event.user) {
      return event.user;
    }
    const selected = selectedUserId ? userIndex.get(selectedUserId) : null;
    if (selected) {
      return selected;
    }
    if (analysisData.users && analysisData.users.length) {
      return analysisData.users[0];
    }
    return fallbackActivity.users[0];
  }

  function userHasPermission(user, permission) {
    if (!permission) {
      return true;
    }
    const perms = user && Array.isArray(user.permissions) ? user.permissions : [];
    return perms.includes(permission);
  }

  function chooseLobbyServerForUser(user) {
    const servers = getActivitySource().servers;
    if (!servers.length) {
      return '';
    }
    const lobbies = Array.isArray(config.lobbies) ? config.lobbies : [];
    const candidates = lobbies
      .filter((lobby) => lobby && typeof lobby === 'object')
      .filter((lobby) => userHasPermission(user, (lobby.permission || '').trim()))
      .map((lobby) => ({
        lobby,
        priority: Number.isFinite(Number(lobby.priority)) ? Number(lobby.priority) : 0
      }))
      .sort((a, b) => b.priority - a.priority);
    for (const entry of candidates) {
      const filter = typeof entry.lobby.filter === 'string' ? entry.lobby.filter : '';
      if (!filter) {
        continue;
      }
      let regex;
      try {
        regex = buildRegex(filter);
      } catch (error) {
        regex = null;
      }
      if (!regex) {
        continue;
      }
      const match = servers.find((server) => regex.test(server));
      if (match) {
        return match;
      }
    }
    return findLobbyServer(servers, '');
  }

  function getGameServerOptions(currentServer) {
    const servers = getActivitySource().servers;
    if (!servers.length) {
      return [];
    }
    const nonLobby = servers.filter((server) => !matchLobbyForServer(server));
    const source = nonLobby.length ? nonLobby : servers;
    return source.filter((server) => server !== currentServer);
  }

  function buildPermissionMatrix(user) {
    const lobbies = Array.isArray(config.lobbies) ? config.lobbies : [];
    const groupIndex = new Map();
    if (Array.isArray(config['lobby-groups'])) {
      config['lobby-groups'].forEach((group) => {
        if (!group || !Array.isArray(group.lobbies)) {
          return;
        }
        group.lobbies.forEach((lobbyName) => {
          if (!groupIndex.has(lobbyName)) {
            groupIndex.set(lobbyName, []);
          }
          groupIndex.get(lobbyName).push(group.name);
        });
      });
    }
    return lobbies.map((lobby) => {
      const name = lobby.name || 'Lobby';
      const permission = (lobby.permission || '').trim();
      const groups = groupIndex.get(name) || [];
      const match = permission ? userHasPermission(user, permission) : true;
      return { name, permission, groups, match };
    });
  }

  function createPermissionMatrixEl(user) {
    const wrapper = document.createElement('div');
    wrapper.className = 'cb-perm-matrix';
    const title = document.createElement('div');
    title.className = 'cb-perm-matrix-title';
    title.textContent = 'Permission matrix';
    wrapper.appendChild(title);

    const list = document.createElement('div');
    list.className = 'cb-perm-matrix-list';
    const rows = buildPermissionMatrix(user);
    if (!rows.length) {
      const empty = document.createElement('div');
      empty.className = 'cb-perm-matrix-empty';
      empty.textContent = 'No lobbies configured';
      list.appendChild(empty);
    } else {
      rows.forEach((row) => {
        const item = document.createElement('div');
        item.className = 'cb-perm-matrix-row';
        item.dataset.match = row.match ? 'ok' : 'bad';
        const name = document.createElement('div');
        name.className = 'cb-perm-matrix-name';
        name.textContent = row.name;
        const perm = document.createElement('div');
        perm.className = 'cb-perm-matrix-perm';
        perm.textContent = row.permission || 'public';
        const status = document.createElement('div');
        status.className = 'cb-perm-matrix-status';
        status.textContent = row.match ? 'match' : 'blocked';
        item.appendChild(name);
        item.appendChild(perm);
        item.appendChild(status);
        if (row.groups && row.groups.length) {
          const groups = document.createElement('div');
          groups.className = 'cb-perm-matrix-groups';
          groups.textContent = `groups: ${row.groups.join(', ')}`;
          item.appendChild(groups);
        }
        list.appendChild(item);
      });
    }
    wrapper.appendChild(list);
    return wrapper;
  }

  function connectOfflineUser(user) {
    const server = chooseLobbyServerForUser(user);
    if (!server) {
      return;
    }
    const now = Date.now();
    setUserStatus(user, 'online');
    renderOfflinePlayers();
    pushActivityItem({
      user,
      server,
      type: 'connect',
      tone: 'ok',
      reason: '',
      command: '',
      phase: 'join',
      detail: 'system',
      prevServer: '',
      timestamp: now
    });
    scheduleJourneyStep(600, {
      user,
      server,
      type: 'connect',
      tone: 'ok',
      reason: '',
      command: '',
      phase: 'lobby-pick',
      detail: 'system-transfer',
      prevServer: '',
      timestamp: now + 600
    });
  }

  function createZoneControls(server, user, event) {
    const controls = document.createElement('div');
    controls.className = 'cb-zone-controls';
    const commandSelect = document.createElement('select');
    commandSelect.className = 'cb-zone-select';
    commandSelect.setAttribute('aria-label', 'Command');
    getCommandOptions().forEach((command) => {
      const option = document.createElement('option');
      option.value = `/${command}`;
      option.textContent = `/${command}`;
      commandSelect.appendChild(option);
    });
    const execBtn = document.createElement('button');
    execBtn.type = 'button';
    execBtn.className = 'cb-btn cb-btn-ghost cb-zone-btn';
    execBtn.textContent = 'Execute';
    execBtn.addEventListener('click', () => {
      const targetUser = user || getPreferredUser(event);
      const command = commandSelect.value || '';
      const now = Date.now();
      pushActivityItem({
        user: targetUser,
        server,
        type: 'connect',
        tone: 'ok',
        reason: '',
        command,
        prevServer: '',
        timestamp: now
      });
    });
    const gameKickBtn = document.createElement('button');
    gameKickBtn.type = 'button';
    gameKickBtn.className = 'cb-btn cb-btn-danger cb-zone-btn';
    gameKickBtn.setAttribute('title', 'Game kick');
    gameKickBtn.setAttribute('aria-label', 'Game kick');
    gameKickBtn.innerHTML = `
      <svg class="cb-kick-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M5 5h9l3 3v11H5z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
        <path d="M14 5v3h3" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
        <path d="M8 13l6 0" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <path d="M12 10l2 3-2 3" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    `;
    gameKickBtn.addEventListener('click', () => {
      const targetUser = user || getPreferredUser(event);
      const now = Date.now();
      const kickEvent = {
        user: targetUser,
        server,
        type: 'kick',
        tone: 'bad',
        reason: 'Game ended',
        kickType: 'game',
        command: '',
        prevServer: '',
        timestamp: now
      };
      pushActivityItem(kickEvent);
      const lobbyServer = findLobbyServer(getActivitySource().servers, server);
      if (lobbyServer && lobbyServer !== server) {
        scheduleJourneyStep(800, {
          user: targetUser,
          server: lobbyServer,
          type: 'switch',
          tone: 'ok',
          reason: '',
          command: '',
          phase: 'lobby-return',
          detail: 'system-transfer',
          prevServer: server,
          timestamp: now + 800
        });
      }
    });
    const proxyKickBtn = document.createElement('button');
    proxyKickBtn.type = 'button';
    proxyKickBtn.className = 'cb-btn cb-btn-danger cb-zone-btn';
    proxyKickBtn.setAttribute('title', 'Proxy kick');
    proxyKickBtn.setAttribute('aria-label', 'Proxy kick');
    proxyKickBtn.innerHTML = `
      <svg class="cb-kick-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 12h10" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <path d="M10 8l4 4-4 4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M15 5h5v14h-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
    `;
    proxyKickBtn.addEventListener('click', () => {
      const targetUser = user || getPreferredUser(event);
      const now = Date.now();
      pushActivityItem({
        user: targetUser,
        server,
        type: 'kick',
        tone: 'bad',
        reason: 'Proxy disconnect',
        kickType: 'proxy',
        command: '',
        prevServer: '',
        timestamp: now
      });
    });
    const targetSelect = document.createElement('select');
    targetSelect.className = 'cb-zone-select cb-zone-select-target';
    targetSelect.setAttribute('aria-label', 'Target game server');
    const targetOptions = getGameServerOptions(server);
    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = 'Pick game server...';
    targetSelect.appendChild(placeholder);
    targetOptions.forEach((target) => {
      const option = document.createElement('option');
      option.value = target;
      option.textContent = target;
      targetSelect.appendChild(option);
    });
    const moveBtn = document.createElement('button');
    moveBtn.type = 'button';
    moveBtn.className = 'cb-btn cb-btn-ghost cb-zone-btn';
    moveBtn.setAttribute('title', 'Send to server');
    moveBtn.setAttribute('aria-label', 'Send to server');
    moveBtn.innerHTML = `
      <svg class="cb-send-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 12h10" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <path d="M10 8l4 4-4 4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M14 5h6v14h-6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
    `;
    moveBtn.addEventListener('click', () => {
      const target = targetSelect.value;
      if (!target) {
        return;
      }
      const targetUser = user || getPreferredUser(event);
      const now = Date.now();
      pushActivityItem({
        user: targetUser,
        server: target,
        type: 'switch',
        tone: 'warn',
        reason: '',
        command: '',
        phase: 'game-pick',
        detail: 'system-transfer',
        prevServer: server,
        timestamp: now
      });
    });

    controls.appendChild(commandSelect);
    controls.appendChild(execBtn);
    controls.appendChild(targetSelect);
    controls.appendChild(moveBtn);
    controls.appendChild(gameKickBtn);
    controls.appendChild(proxyKickBtn);
    return controls;
  }

  function renderZones() {
    zonesEl.innerHTML = '';
    const zones = buildZones();
    zones.forEach((zone) => {
      const zoneEl = document.createElement('div');
      zoneEl.className = 'cb-zone';
      const title = document.createElement('div');
      title.className = 'cb-zone-title';
      title.textContent = zone.name;
      zoneEl.appendChild(title);
      const list = document.createElement('div');
      list.className = 'cb-zone-list';
      if (!zone.servers.length) {
        const empty = document.createElement('span');
        empty.className = 'cb-zone-empty';
        empty.textContent = 'No servers';
        list.appendChild(empty);
      } else {
        zone.servers.forEach((server) => {
          const entry = document.createElement('div');
          entry.className = 'cb-zone-entry';
          entry.dataset.server = server;
          if (activityState.activeServer && activityState.activeServer === server) {
            entry.classList.add('is-hot');
          }

          const header = document.createElement('div');
          header.className = 'cb-zone-header';
          const serverName = document.createElement('div');
          serverName.className = 'cb-zone-server';
          serverName.textContent = server;
          header.appendChild(serverName);

          const lobbyInfo = lobbyInfoForServer(server);
          if (lobbyInfo.groups.length) {
            const groups = document.createElement('div');
            groups.className = 'cb-zone-groups';
            lobbyInfo.groups.forEach((group) => {
              const chip = document.createElement('span');
              chip.className = 'cb-zone-group';
              chip.textContent = group;
              groups.appendChild(chip);
            });
            header.appendChild(groups);
          } else if (lobbyInfo.lobbyName) {
            const group = document.createElement('span');
            group.className = 'cb-zone-group';
            group.textContent = lobbyInfo.lobbyName;
            header.appendChild(group);
          }

          const players = document.createElement('div');
          players.className = 'cb-zone-players';
          renderZonePlayers(players, server);

          entry.appendChild(header);
          entry.appendChild(players);
          list.appendChild(entry);
        });
      }
      zoneEl.appendChild(list);
      zonesEl.appendChild(zoneEl);
    });
  }

  function setActiveServer(name) {
    activityState.activeServer = name || '';
    const entries = zonesEl.querySelectorAll('.cb-zone-entry');
    entries.forEach((entry) => {
      if (activityState.activeServer && entry.dataset.server === activityState.activeServer) {
        entry.classList.add('is-hot');
      } else {
        entry.classList.remove('is-hot');
      }
    });
  }

  function updateZoneEntry(server) {
    if (!server) {
      return;
    }
    const entry = zonesEl.querySelector(`.cb-zone-entry[data-server="${CSS.escape(server)}"]`);
    if (!entry) {
      return;
    }
    const players = entry.querySelector('.cb-zone-players');
    if (players) {
      renderZonePlayers(players, server);
    }
  }

  function renderZonePlayers(container, server) {
    container.innerHTML = '';
    const map = activityState.serverUsers.get(server);
    if (!map || map.size === 0) {
      const empty = document.createElement('div');
      empty.className = 'cb-zone-empty';
      empty.textContent = 'Empty';
      container.appendChild(empty);
      return;
    }
    map.forEach((entry) => {
      const row = document.createElement('div');
      row.className = 'cb-zone-player';

      const avatar = document.createElement('div');
      avatar.className = 'cb-zone-avatar';
      const img = document.createElement('img');
      img.alt = `${entry.user.name || 'Player'} avatar`;
      img.loading = 'lazy';
      img.decoding = 'async';
      setAvatarImage(img, avatarSources(entry.user));
      avatar.appendChild(img);

      const info = document.createElement('div');
      info.className = 'cb-zone-player-info';
      const name = document.createElement('div');
      name.className = 'cb-zone-player-name';
      name.textContent = entry.user.name || entry.user.id || 'Player';
      const action = document.createElement('div');
      action.className = 'cb-zone-action';
      action.textContent = eventActionShort(entry.event);

      const controls = createZoneControls(server, entry.user, entry.event);

      info.appendChild(name);
      info.appendChild(action);
      info.appendChild(controls);

      row.appendChild(avatar);
      row.appendChild(info);
      row.appendChild(createPermissionMatrixEl(entry.user));
      container.appendChild(row);
    });
  }

  function renderOfflinePlayers() {
    if (!offlineList) {
      return;
    }
    syncUserStatus();
    offlineList.innerHTML = '';
    const users = getActivitySource().users;
    const offlineUsers = users.filter((user) => getUserStatus(user) !== 'online');
    if (!users.length) {
      const empty = document.createElement('div');
      empty.className = 'cb-offline-empty';
      empty.textContent = 'No players available';
      offlineList.appendChild(empty);
      return;
    }
    if (!offlineUsers.length) {
      const empty = document.createElement('div');
      empty.className = 'cb-offline-empty';
      empty.textContent = 'All players are online';
      offlineList.appendChild(empty);
      return;
    }
    offlineUsers.forEach((user) => {
      const card = document.createElement('div');
      card.className = 'cb-offline-card';

      const avatar = document.createElement('div');
      avatar.className = 'cb-offline-avatar';
      const img = document.createElement('img');
      img.alt = `${user.name || 'User'} avatar`;
      img.loading = 'lazy';
      img.decoding = 'async';
      setAvatarImage(img, avatarSources(user));
      avatar.appendChild(img);

      const info = document.createElement('div');
      info.className = 'cb-offline-info';
      const name = document.createElement('div');
      name.className = 'cb-offline-name';
      name.textContent = user.name || user.id || 'Player';
      const meta = document.createElement('div');
      meta.className = 'cb-offline-meta';
      meta.textContent = 'Offline';

      const actions = document.createElement('div');
      actions.className = 'cb-offline-actions';
      const connectBtn = document.createElement('button');
      connectBtn.type = 'button';
      connectBtn.className = 'cb-btn cb-btn-primary cb-offline-btn';
      connectBtn.textContent = 'Connect';
      connectBtn.addEventListener('click', () => {
        connectOfflineUser(user);
      });
      const simulateBtn = document.createElement('button');
      simulateBtn.type = 'button';
      simulateBtn.className = 'cb-btn cb-btn-ghost cb-offline-btn';
      simulateBtn.textContent = 'Simulate journey';
      simulateBtn.addEventListener('click', () => {
        simulateJourney(user);
      });
      actions.appendChild(connectBtn);
      actions.appendChild(simulateBtn);

      info.appendChild(name);
      info.appendChild(meta);
      info.appendChild(actions);

      card.appendChild(avatar);
      card.appendChild(info);
      card.appendChild(createPermissionMatrixEl(user));
      offlineList.appendChild(card);
    });
  }

  function pickServerByHints(servers, hints, fallback) {
    if (!servers.length) {
      return fallback || '';
    }
    if (hints && hints.length) {
      const match = servers.find((server) => hints.some((hint) => server.toLowerCase().includes(hint)));
      if (match) {
        return match;
      }
    }
    return fallback || servers[0];
  }

  function findLobbyServer(servers, currentServer) {
    if (!servers.length) {
      return '';
    }
    let candidates = servers.filter((server) => matchLobbyForServer(server));
    if (!candidates.length) {
      candidates = servers.filter((server) => /lobby|hub/i.test(server));
    }
    if (!candidates.length) {
      candidates = servers.slice();
    }
    if (candidates.length > 1 && currentServer) {
      candidates = candidates.filter((server) => server !== currentServer);
    }
    return candidates[0] || currentServer || servers[0];
  }

  function scheduleJourneyStep(delayMs, event) {
    const timer = window.setTimeout(() => {
      pushActivityItem(event);
      activityState.journeyTimers.delete(timer);
    }, delayMs);
    activityState.journeyTimers.add(timer);
  }

  function simulateJourney(user) {
    const { servers } = getActivitySource();
    if (!servers.length) {
      return;
    }
    const lobbyServer = pickServerByHints(servers, ['lobby', 'hub'], servers[0]);
    const gameServer = pickServerByHints(
      servers.filter((server) => server !== lobbyServer),
      ['game', 'ffa', 'minestom', 'arena'],
      servers[1] || lobbyServer
    );
    const altLobbyServer = pickServerByHints(
      servers.filter((server) => server !== lobbyServer),
      ['lobby', 'hub'],
      servers.find((server) => server !== lobbyServer) || lobbyServer
    );
    const commandOptions = getCommandOptions();
    const lobbyCommand = commandOptions.length ? `/${commandOptions[0]}` : '/hub';
    const now = Date.now();

    setUserStatus(user, 'online');
    renderOfflinePlayers();

    scheduleJourneyStep(0, {
      user,
      server: lobbyServer,
      type: 'connect',
      tone: 'ok',
      reason: '',
      command: '',
      phase: 'join',
      detail: 'system',
      prevServer: '',
      timestamp: now
    });

    scheduleJourneyStep(900, {
      user,
      server: lobbyServer,
      type: 'connect',
      tone: 'ok',
      reason: '',
      command: lobbyCommand,
      phase: 'lobby-pick',
      detail: 'command',
      prevServer: '',
      timestamp: now + 900
    });

    scheduleJourneyStep(1800, {
      user,
      server: gameServer,
      type: 'switch',
      tone: 'warn',
      reason: '',
      command: '',
      phase: 'game-pick',
      detail: 'system-transfer',
      prevServer: lobbyServer,
      timestamp: now + 1800
    });

    scheduleJourneyStep(3000, {
      user,
      server: gameServer,
      type: 'kick',
      tone: 'bad',
      reason: 'Game ended',
      kickType: 'game',
      command: '',
      phase: 'game-end',
      detail: 'system',
      prevServer: '',
      timestamp: now + 3000
    });

    scheduleJourneyStep(4200, {
      user,
      server: lobbyServer,
      type: 'switch',
      tone: 'ok',
      reason: '',
      command: '',
      phase: 'lobby-return',
      detail: 'system-transfer',
      prevServer: gameServer,
      timestamp: now + 4200
    });

    scheduleJourneyStep(5400, {
      user,
      server: altLobbyServer,
      type: 'switch',
      tone: 'warn',
      reason: '',
      command: lobbyCommand,
      phase: 'lobby-switch',
      detail: 'command',
      prevServer: lobbyServer,
      timestamp: now + 5400
    });

    scheduleJourneyStep(6600, {
      user,
      server: altLobbyServer,
      type: 'leave',
      tone: 'warn',
      reason: '',
      command: '',
      phase: 'proxy-leave',
      detail: 'system',
      prevServer: '',
      timestamp: now + 6600
    });
  }

  function createActivityChip(text, tone) {
    const chip = document.createElement('span');
    chip.className = 'cb-activity-chip';
    if (tone) {
      chip.dataset.tone = tone;
    }
    chip.textContent = text;
    return chip;
  }

  function createActivityArrow() {
    const arrow = document.createElement('span');
    arrow.className = 'cb-activity-arrow';
    arrow.textContent = '->';
    return arrow;
  }

  function buildActivityEvent() {
    const { users, servers } = getActivitySource();
    if (!users.length || !servers.length) {
      return null;
    }
    const user = users[Math.floor(Math.random() * users.length)];
    const kickReasons = ['Server full', 'Whitelist', 'Idle timeout', 'Maintenance', 'Connection lost'];
    const roll = Math.random();
    const type = roll > 0.72 ? 'kick' : roll > 0.42 ? 'switch' : 'connect';
    let server = servers[Math.floor(Math.random() * servers.length)];
    const lastServer = activityState.lastServerByUser.get(user.id);
    if (servers.length > 1 && lastServer && server === lastServer) {
      server = servers.find((entry) => entry !== lastServer) || server;
    }
    const prevServer = type === 'switch' && lastServer ? lastServer : '';
    const commandList = [];
    const baseCommand = typeof config['base-hub-command'] === 'string' ? config['base-hub-command'].trim() : '';
    if (baseCommand) {
      commandList.push(baseCommand);
    }
    if (Array.isArray(config.aliases)) {
      config.aliases.forEach((alias) => {
        const trimmed = typeof alias === 'string' ? alias.trim() : '';
        if (trimmed) {
          commandList.push(trimmed);
        }
      });
    }
    if (!commandList.length) {
      commandList.push('hub');
    }
    const command = Math.random() > 0.4 ? `/${commandList[Math.floor(Math.random() * commandList.length)]}` : '';
    const tone = type === 'kick' ? 'bad' : type === 'switch' ? 'warn' : 'ok';
    const reason = type === 'kick' ? kickReasons[Math.floor(Math.random() * kickReasons.length)] : '';
    const now = Date.now();
    activityState.lastServerByUser.set(user.id, server);
    return {
      user,
      server,
      type,
      tone,
      reason,
      command,
      prevServer,
      timestamp: now
    };
  }

  function renderActivityItem(event) {
    const item = document.createElement('div');
    item.className = 'cb-activity-item';
    item.dataset.tone = event.tone;
    item.dataset.timestamp = String(event.timestamp);

    const avatarWrap = document.createElement('div');
    avatarWrap.className = 'cb-activity-avatar';
    const avatarImg = document.createElement('img');
    avatarImg.alt = `${event.user.name || 'User'} avatar`;
    avatarImg.loading = 'lazy';
    avatarImg.decoding = 'async';
    setAvatarImage(avatarImg, avatarSources(event.user));
    avatarWrap.appendChild(avatarImg);

    const body = document.createElement('div');
    body.className = 'cb-activity-body';

    const title = document.createElement('div');
    title.className = 'cb-activity-title';
    const nameEl = document.createElement('strong');
    nameEl.className = 'cb-activity-name';
    nameEl.textContent = event.user.name || event.user.id;
    const actionEl = document.createElement('span');
    actionEl.className = 'cb-activity-action';
    if (event.command) {
      actionEl.textContent = `used ${event.command}`;
    } else {
      actionEl.textContent = event.type === 'kick' ? 'kicked' : event.type === 'switch' ? 'switched' : 'connected';
    }
    title.appendChild(nameEl);
    title.appendChild(actionEl);

    const flow = document.createElement('div');
    flow.className = 'cb-activity-flow';
    if (event.command) {
      flow.appendChild(createActivityChip(`CMD ${event.command}`, 'info'));
      flow.appendChild(createActivityArrow());
    }
    flow.appendChild(createActivityChip(event.type === 'switch' ? 'SWITCH' : 'CONNECT'));
    flow.appendChild(createActivityArrow());
    if (event.prevServer) {
      const prevEl = document.createElement('span');
      prevEl.className = 'cb-activity-prev';
      prevEl.textContent = `from ${event.prevServer}`;
      flow.appendChild(prevEl);
      flow.appendChild(createActivityArrow());
    }
    const serverEl = document.createElement('span');
    serverEl.className = 'cb-activity-server';
    serverEl.textContent = event.server;
    flow.appendChild(serverEl);
    flow.appendChild(createActivityArrow());
    flow.appendChild(createActivityChip(event.type === 'kick' ? 'KICK' : 'OK', event.tone));
    if (event.reason) {
      const reasonEl = document.createElement('span');
      reasonEl.className = 'cb-activity-reason';
      reasonEl.textContent = event.reason;
      flow.appendChild(reasonEl);
    }

    const trail = document.createElement('div');
    trail.className = 'cb-activity-trail';
    const key = getUserKey(event.user);
    const history = key ? activityState.historyByUser.get(key) || [] : [];
    history.forEach((entry) => {
      const step = document.createElement('span');
      step.className = 'cb-activity-step';
      step.dataset.type = entry.type || 'connect';
      if (entry.tone) {
        step.dataset.tone = entry.tone;
      }
      const titleParts = [entry.label || 'Event'];
      if (entry.detail) {
        titleParts.push(entry.detail);
      }
      step.title = titleParts.join(' · ');
      const iconType = entry.type === 'lobby-return'
        ? 'lobby'
        : entry.type === 'game-end'
          ? 'game'
          : entry.type === 'join' || entry.type === 'connect'
            ? 'connect'
            : entry.type;
      step.appendChild(createActivityHistoryIcon(iconType));
      const label = document.createElement('span');
      label.className = 'cb-activity-step-label';
      label.textContent = entry.short || '';
      step.appendChild(label);
      trail.appendChild(step);
    });

    const meta = document.createElement('div');
    meta.className = 'cb-activity-meta';
    meta.textContent = 'just now';

    body.appendChild(title);
    body.appendChild(flow);
    if (history.length) {
      body.appendChild(trail);
    }
    body.appendChild(meta);
    item.appendChild(avatarWrap);
    item.appendChild(body);
    return item;
  }

  function pushActivityItem(event) {
    if (!event) {
      return;
    }
    const affectedServers = new Set();
    let prevServer = '';
    if (event.user) {
      const key = getUserKey(event.user);
      if (key) {
        prevServer = activityState.lastServerByUser.get(key) || '';
      }
    }
    if (!event.prevServer && prevServer) {
      event.prevServer = prevServer;
    }
    updateHistoryForEvent(event);
    const isProxyKick = event.type === 'kick' && event.kickType === 'proxy';
    const isGameKick = event.type === 'kick' && event.kickType === 'game';
    const isLeave = event.type === 'leave' || event.phase === 'proxy-leave';
    const isServerExit = isLeave || isProxyKick || isGameKick || event.type === 'kick';
    const isServerEnter = event.type === 'connect'
      || event.type === 'switch'
      || ['join', 'lobby-pick', 'game-pick', 'lobby-return', 'lobby-switch'].includes(event.phase);

    if (isServerExit && event.server) {
      const map = activityState.serverUsers.get(event.server);
      if (map && event.user) {
        const key = getUserKey(event.user);
        if (key) {
          map.delete(key);
        }
      }
      affectedServers.add(event.server);
      if (event.user) {
        const key = getUserKey(event.user);
        if (key && activityState.lastServerByUser.get(key) === event.server) {
          activityState.lastServerByUser.delete(key);
        }
      }
    }

    if (isServerEnter && event.server) {
      if (event.user) {
        const key = getUserKey(event.user);
        if (key) {
          activityState.serverUsers.forEach((map, serverName) => {
            if (serverName === event.server) {
              return;
            }
            if (map.has(key)) {
              map.delete(key);
              affectedServers.add(serverName);
            }
          });
        }
      }
      const previous = event.prevServer || prevServer;
      if (previous && previous !== event.server) {
        const map = activityState.serverUsers.get(previous);
        if (map && event.user) {
          const key = getUserKey(event.user);
          if (key) {
            map.delete(key);
          }
        }
        affectedServers.add(previous);
      }
      if (event.user) {
        const key = getUserKey(event.user);
        if (key) {
          const map = activityState.serverUsers.get(event.server) || new Map();
          map.set(key, { user: event.user, event });
          activityState.serverUsers.set(event.server, map);
          affectedServers.add(event.server);
        }
      }
      if (event.user) {
        const key = getUserKey(event.user);
        if (key) {
          activityState.lastServerByUser.set(key, event.server);
        }
      }
    }
    const item = renderActivityItem(event);
    activityList.prepend(item);
    activityEmpty.style.display = 'none';
    if (isServerEnter && event.server) {
      setActiveServer(event.server);
    } else if (isServerExit && event.server && activityState.activeServer === event.server) {
      setActiveServer('');
    }
    if (affectedServers.size) {
      affectedServers.forEach((server) => updateZoneEntry(server));
    } else if (event.server) {
      updateZoneEntry(event.server);
    }
    if (event.user) {
      const isProxyKick = event.type === 'kick' && event.kickType === 'proxy';
      const nextStatus = event.type === 'leave' || event.phase === 'proxy-leave' || isProxyKick ? 'offline' : 'online';
      setUserStatus(event.user, nextStatus);
      renderOfflinePlayers();
    }
    while (activityList.children.length > activityState.maxItems) {
      activityList.removeChild(activityList.lastChild);
    }
    updateActivityTimes();
  }

  function clearActivityItems() {
    activityList.innerHTML = '';
    activityEmpty.style.display = '';
  }

  function resetActivityFeed() {
    activityState.lastServerByUser = new Map();
    activityState.serverUsers = new Map();
    activityState.activeServer = '';
    activityState.historyByUser = new Map();
    activityState.journeyTimers.forEach((timer) => window.clearTimeout(timer));
    activityState.journeyTimers.clear();
    clearActivityItems();
    for (let i = 0; i < 2; i += 1) {
      pushActivityItem(buildActivityEvent());
    }
    renderZones();
    renderOfflinePlayers();
  }

  function startActivityTimer() {
    if (activityState.timer) {
      window.clearInterval(activityState.timer);
    }
    activityState.timer = window.setInterval(() => {
      if (activityState.running) {
        pushActivityItem(buildActivityEvent());
      }
      updateActivityTimes();
    }, 2400);
  }
  function normalizeDataDump(raw) {
    if (!raw || typeof raw !== 'object') {
      return null;
    }
    const serversRaw = Array.isArray(raw.servers) ? raw.servers : Array.isArray(raw.serverNames) ? raw.serverNames : [];
    const usersRaw = Array.isArray(raw.users) ? raw.users : [];
    const servers = Array.from(new Set(
      serversRaw
        .filter((entry) => typeof entry === 'string')
        .map((entry) => entry.trim())
        .filter(Boolean)
    ));

    const users = usersRaw
      .filter((entry) => entry && typeof entry === 'object')
      .map((entry) => {
        const name = typeof entry.name === 'string' ? entry.name.trim() : '';
        const uuid = typeof entry.uuid === 'string' ? entry.uuid.trim() : '';
        let permissionsRaw = [];
        if (Array.isArray(entry.permissions)) {
          permissionsRaw = entry.permissions;
        } else if (typeof entry.permissions === 'string') {
          permissionsRaw = [entry.permissions];
        }
        const permissions = Array.from(new Set(
          permissionsRaw
            .filter((perm) => typeof perm === 'string')
            .map((perm) => perm.trim())
            .filter(Boolean)
        ));
        const id = uuid || name;
        if (!id) {
          return null;
        }
        return { id, name: name || uuid, uuid, permissions };
      })
      .filter(Boolean);

    if (!servers.length && !users.length) {
      return null;
    }

    return { servers, users };
  }

  function updateServerSuggestions() {
    serverSuggestions.innerHTML = '';
    analysisData.servers.forEach((serverName) => {
      const option = document.createElement('option');
      option.value = serverName;
      serverSuggestions.appendChild(option);
    });
  }

  function populateUserSelect() {
    userIndex = new Map();
    analysisData.users.forEach((user) => {
      userIndex.set(user.id, user);
    });
    if (testUserSelect) {
      testUserSelect.innerHTML = '';
      const placeholder = document.createElement('option');
      placeholder.value = '';
      placeholder.textContent = 'Select user...';
      testUserSelect.appendChild(placeholder);
      analysisData.users.forEach((user) => {
        const label = user.uuid ? `${user.name} (${user.uuid.slice(0, 8)})` : user.name;
        const option = document.createElement('option');
        option.value = user.id;
        option.textContent = label;
        testUserSelect.appendChild(option);
      });
      testUserSelect.disabled = analysisData.users.length === 0;
      if (!userIndex.has(selectedUserId)) {
        selectedUserId = '';
      }
      testUserSelect.value = selectedUserId;
      return;
    }
    if (!userIndex.has(selectedUserId)) {
      selectedUserId = analysisData.users[0]?.id || '';
    }
  }

  function applyDataDump(data) {
    analysisData = data;
    updateServerSuggestions();
    populateUserSelect();
    setDataSummary(`Loaded ${analysisData.servers.length} servers, ${analysisData.users.length} users`, 'ok');
    refreshPermissionPills();
  }

  function clearDataDump() {
    analysisData = { servers: [], users: [] };
    userIndex = new Map();
    selectedUserId = '';
    updateServerSuggestions();
    populateUserSelect();
    setDataSummary('No data loaded', 'idle');
    refreshPermissionPills();
  }

  function loadDataDumpText(text) {
    if (!text || !text.trim()) {
      setDataSummary('Paste or upload a data dump', 'idle');
      return;
    }
    let raw;
    try {
      raw = JSON.parse(text);
    } catch (error) {
      if (!yamlLib) {
        setDataSummary('YAML parser not ready', 'bad');
        return;
      }
      raw = yamlLib.load(text);
    }
    const normalized = normalizeDataDump(raw);
    if (!normalized) {
      setDataSummary('Invalid data dump', 'bad');
      return;
    }
    applyDataDump(normalized);
  }

  function getSelectedUser() {
    if (!selectedUserId) {
      return null;
    }
    return userIndex.get(selectedUserId) || null;
  }

  function refreshPermissionPills() {
    const user = getSelectedUser();
    const permissionSet = user ? new Set(user.permissions || []) : null;
    const pills = lobbiesEl.querySelectorAll('.cb-perm-pill');
    pills.forEach((pill) => {
      const index = Number.parseInt(pill.dataset.index || '', 10);
      const lobby = Number.isFinite(index) ? config.lobbies[index] : null;
      if (!user || !lobby) {
        pill.textContent = 'Select user';
        pill.dataset.state = 'idle';
        return;
      }
      const permission = (lobby.permission || '').trim();
      if (!permission) {
        pill.textContent = 'No permission required';
        pill.dataset.state = 'ok';
        return;
      }
      const allowed = permissionSet ? permissionSet.has(permission) : false;
      pill.textContent = allowed ? `Has ${permission}` : `Missing ${permission}`;
      pill.dataset.state = allowed ? 'ok' : 'bad';
    });
  }

  function clone(value) {
    if (typeof structuredClone === 'function') {
      return structuredClone(value);
    }
    return JSON.parse(JSON.stringify(value || {}));
  }

  function mergeDefaults(target, defaults) {
    if (Array.isArray(defaults)) {
      return Array.isArray(target) ? target : clone(defaults);
    }
    if (!defaults || typeof defaults !== 'object') {
      return target === undefined ? defaults : target;
    }
    const out = {};
    const targetObj = target && typeof target === 'object' ? target : {};
    for (const key of Object.keys(defaults)) {
      if (targetObj[key] === undefined) {
        out[key] = clone(defaults[key]);
      } else if (defaults[key] && typeof defaults[key] === 'object' && !Array.isArray(defaults[key])) {
        out[key] = mergeDefaults(targetObj[key], defaults[key]);
      } else {
        out[key] = targetObj[key];
      }
    }
    for (const key of Object.keys(targetObj)) {
      if (!(key in out)) {
        out[key] = targetObj[key];
      }
    }
    return out;
  }

  function setDebugCategoryVisibility(enabled) {
    if (!debugCategoriesEl) {
      return;
    }
    debugCategoriesEl.hidden = !enabled;
    Object.values(debugCategoryInputs).forEach((input) => {
      if (input) {
        input.disabled = !enabled;
      }
    });
  }

  function getDefaultDebugCategories() {
    const defaults = defaultConfig?.debug?.categories;
    const base = defaults && typeof defaults === 'object' ? defaults : {};
    return { ...fallbackDebugCategories, ...base };
  }

  function normalizeDebugCategories(raw) {
    const base = getDefaultDebugCategories();
    const data = raw && typeof raw === 'object' ? raw : {};
    const out = { ...base, ...data };
    if (out['forced-hosts'] === undefined && out.forcedHosts !== undefined) {
      out['forced-hosts'] = out.forcedHosts;
    }
    if (out['last-lobby'] === undefined && out.lastLobby !== undefined) {
      out['last-lobby'] = out.lastLobby;
    }
    delete out.forcedHosts;
    delete out.lastLobby;
    return out;
  }

  function normalizeProxyConfig(raw) {
    let next = raw && typeof raw === 'object' ? raw : {};
    next = mergeDefaults(next, defaultConfig);
    next.aliases = Array.isArray(next.aliases) ? next.aliases : [];
    next['auto-select'] = next['auto-select'] && typeof next['auto-select'] === 'object' ? next['auto-select'] : {};
    next['last-lobby'] = next['last-lobby'] && typeof next['last-lobby'] === 'object' ? next['last-lobby'] : {};
    next.debug = next.debug && typeof next.debug === 'object' ? next.debug : {};
    next.debug.categories = normalizeDebugCategories(next.debug.categories);
    next.messages = next.messages && typeof next.messages === 'object' ? next.messages : {};
    next['system-messages'] = next['system-messages'] && typeof next['system-messages'] === 'object' ? next['system-messages'] : {};
    next.i18n = next.i18n && typeof next.i18n === 'object' ? next.i18n : {};
    if (defaultConfig?.i18n && typeof defaultConfig.i18n === 'object') {
      next.i18n = mergeDefaults(next.i18n, defaultConfig.i18n);
    }
    if (!next.i18n['default-locale']) {
      next.i18n['default-locale'] = 'en_us';
    }
    if (next.i18n['use-client-locale'] === undefined) {
      next.i18n['use-client-locale'] = true;
    }
    next.i18n.overrides = next.i18n.overrides && typeof next.i18n.overrides === 'object' ? next.i18n.overrides : {};
    next['kick-message'] = next['kick-message'] && typeof next['kick-message'] === 'object' ? next['kick-message'] : {};
    next.finder = next.finder && typeof next.finder === 'object' ? next.finder : {};
    next['forced-hosts'] = normalizeForcedHostEntries(next['forced-hosts']);
    next['data-collection'] = next['data-collection'] && typeof next['data-collection'] === 'object'
      ? next['data-collection']
      : {};
    next['lobby-groups'] = Array.isArray(next['lobby-groups']) ? next['lobby-groups'] : [];
    next.lobbies = Array.isArray(next.lobbies) ? next.lobbies : [];

    const lobbyDefaults = {
      name: '',
      filter: '',
      permission: '',
      priority: 0,
      parent: '',
      'parent-groups': [],
      'forced-hosts': [],
      commands: {},
      autojoin: true,
      'overwrite-messages': {}
    };

    next['lobby-groups'] = next['lobby-groups'].map((group) => {
      const normalized = group && typeof group === 'object' ? group : {};
      const merged = mergeDefaults(normalized, { name: '', lobbies: [], 'forced-hosts': [] });
      merged.lobbies = Array.isArray(merged.lobbies) ? merged.lobbies : [];
      merged['forced-hosts'] = Array.isArray(merged['forced-hosts']) ? merged['forced-hosts'] : [];
      return merged;
    });

    next.lobbies = next.lobbies.map((lobby) => {
      const normalized = lobby && typeof lobby === 'object' ? lobby : {};
      const merged = mergeDefaults(normalized, lobbyDefaults);
      merged['parent-groups'] = Array.isArray(merged['parent-groups']) ? merged['parent-groups'] : [];
      merged['forced-hosts'] = Array.isArray(merged['forced-hosts']) ? merged['forced-hosts'] : [];
      merged.commands = merged.commands && typeof merged.commands === 'object' ? merged.commands : {};
      merged['overwrite-messages'] = merged['overwrite-messages'] && typeof merged['overwrite-messages'] === 'object' ? merged['overwrite-messages'] : {};
      merged.priority = Number.isFinite(Number(merged.priority)) ? Number(merged.priority) : 0;
      return merged;
    });

    return next;
  }

  function normalizeLobbyConfig(raw) {
    let next = raw && typeof raw === 'object' ? raw : {};
    next = mergeDefaults(next, defaultConfigLobby);
    next.compass = next.compass && typeof next.compass === 'object' ? next.compass : {};
    next.navigator = next.navigator && typeof next.navigator === 'object' ? next.navigator : {};
    next.i18n = next.i18n && typeof next.i18n === 'object' ? next.i18n : {};
    if (defaultConfigLobby?.i18n && typeof defaultConfigLobby.i18n === 'object') {
      next.i18n = mergeDefaults(next.i18n, defaultConfigLobby.i18n);
    }
    if (!next.i18n['default-locale']) {
      next.i18n['default-locale'] = 'en_us';
    }
    if (next.i18n['use-client-locale'] === undefined) {
      next.i18n['use-client-locale'] = true;
    }
    next.i18n.overrides = next.i18n.overrides && typeof next.i18n.overrides === 'object' ? next.i18n.overrides : {};
    next['spawn-teleport'] = next['spawn-teleport'] && typeof next['spawn-teleport'] === 'object' ? next['spawn-teleport'] : {};
    next['join-teleport'] = next['join-teleport'] && typeof next['join-teleport'] === 'object' ? next['join-teleport'] : {};
    next['join-teleport'].mode = next['join-teleport'].mode
      || (next['spawn-teleport']?.enabled ? 'spawn' : 'none');
    next['join-teleport']['delay-seconds'] = Number.isFinite(Number(next['join-teleport']['delay-seconds']))
      ? Number(next['join-teleport']['delay-seconds'])
      : 0;
    next['join-teleport']['last-location'] = next['join-teleport']['last-location']
      && typeof next['join-teleport']['last-location'] === 'object'
      ? next['join-teleport']['last-location']
      : {};
    next['join-teleport']['last-location']['max-age-seconds'] = Number.isFinite(Number(next['join-teleport']['last-location']['max-age-seconds']))
      ? Number(next['join-teleport']['last-location']['max-age-seconds'])
      : 0;
    next.gameplay = next.gameplay && typeof next.gameplay === 'object' ? next.gameplay : {};
    next['lobby-npcs'] = next['lobby-npcs'] && typeof next['lobby-npcs'] === 'object' ? next['lobby-npcs'] : {};
    next['lobby-signs'] = next['lobby-signs'] && typeof next['lobby-signs'] === 'object' ? next['lobby-signs'] : {};
    next.minestom = next.minestom && typeof next.minestom === 'object' ? next.minestom : {};
    next.bstats = next.bstats && typeof next.bstats === 'object' ? next.bstats : {};
    return next;
  }

  function normalizeConfig(raw, mode) {
    if (mode === 'lobby') {
      return normalizeLobbyConfig(raw);
    }
    return normalizeProxyConfig(raw);
  }

  function parseList(value) {
    return value
      .split(',')
      .map((entry) => entry.trim())
      .filter(Boolean);
  }

  function parseAliasList(value) {
    return value
      .split(/\s+/)
      .map((entry) => entry.trim())
      .filter(Boolean);
  }

  function parseHostList(value) {
    return value
      .split(/[,\s]+/)
      .map((entry) => entry.trim())
      .filter(Boolean);
  }

  function escapeHtml(value) {
    return String(value || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function renderMiniMessage(input) {
    const text = String(input || '');
    if (!text) {
      return '';
    }
    const namedColors = {
      black: '#1b2a29',
      dark_gray: '#4f615f',
      gray: '#9ba9a6',
      white: '#ffffff',
      red: '#ff614d',
      green: '#36c28a',
      yellow: '#f3d46b',
      gold: '#f0b356',
      blue: '#4f7cff',
      aqua: '#69d9ff',
      light_purple: '#c48bff'
    };
    const tokens = text.split(/(<[^>]+>)/g).filter(Boolean);
    let html = '';
    let colorOpen = false;
    const openTags = [];

    const openTag = (tag) => {
      html += `<${tag}>`;
      openTags.push(tag);
    };
    const closeTag = (tag) => {
      html += `</${tag}>`;
      const idx = openTags.lastIndexOf(tag);
      if (idx >= 0) {
        openTags.splice(idx, 1);
      }
    };
    const closeAll = () => {
      for (let i = openTags.length - 1; i >= 0; i -= 1) {
        html += `</${openTags[i]}>`;
      }
      openTags.length = 0;
      if (colorOpen) {
        html += '</span>';
        colorOpen = false;
      }
    };

    tokens.forEach((token) => {
      if (token.startsWith('<') && token.endsWith('>')) {
        const raw = token.slice(1, -1).trim();
        const lower = raw.toLowerCase();
        if (!raw) {
          return;
        }
        if (lower === 'i' || lower === 'italic') {
          openTag('em');
          return;
        }
        if (lower === '/i' || lower === '/italic') {
          closeTag('em');
          return;
        }
        if (lower === 'b' || lower === 'bold') {
          openTag('strong');
          return;
        }
        if (lower === '/b' || lower === '/bold') {
          closeTag('strong');
          return;
        }
        if (lower === 'u' || lower === 'underlined' || lower === 'underline') {
          html += '<span class="cb-mini-underline">';
          openTags.push('span');
          return;
        }
        if (lower === '/u' || lower === '/underlined' || lower === '/underline') {
          closeTag('span');
          return;
        }
        if (lower === 'br') {
          html += '<br>';
          return;
        }
        if (lower === 'reset' || lower === 'r') {
          closeAll();
          return;
        }
        if (raw.startsWith('#') && /^[0-9a-fA-F]{6}$/.test(raw.slice(1))) {
          if (colorOpen) {
            html += '</span>';
          }
          html += `<span style="color:#${raw.slice(1)}">`;
          colorOpen = true;
          return;
        }
        if (namedColors[lower]) {
          if (colorOpen) {
            html += '</span>';
          }
          html += `<span style="color:${namedColors[lower]}">`;
          colorOpen = true;
          return;
        }
        if (lower === '/#' || lower === '/color' || lower === '/c') {
          if (colorOpen) {
            html += '</span>';
            colorOpen = false;
          }
          return;
        }
        html += escapeHtml(token);
        return;
      }
      html += escapeHtml(token);
    });
    if (colorOpen) {
      html += '</span>';
    }
    for (let i = openTags.length - 1; i >= 0; i -= 1) {
      html += `</${openTags[i]}>`;
    }
    return html.replace(/\n/g, '<br>');
  }

  function initMessagePreviews() {
    document.querySelectorAll('.cb-message-preview').forEach((preview) => {
      const id = preview.getAttribute('data-preview-for');
      const input = id ? document.getElementById(id) : null;
      const body = preview.querySelector('.cb-message-preview-body');
      if (!input || !body) {
        return;
      }
      messagePreviewMap.set(input, body);
      preview.addEventListener('click', () => {
        input.focus();
        input.selectionStart = input.value.length;
        input.selectionEnd = input.value.length;
      });
    });
  }

  function updateMessagePreview(input) {
    const target = messagePreviewMap.get(input);
    if (!target) {
      return;
    }
    target.innerHTML = renderMiniMessage(input.value || '');
  }

  function updateAllMessagePreviews() {
    messagePreviewMap.forEach((_, input) => {
      updateMessagePreview(input);
    });
  }

  function updateOverridePreview(input) {
    const entry = overridePreviewMap.get(input);
    if (!entry) {
      return;
    }
    const value = input.value.trim();
    const fallbackValue = entry.fallbackInput ? entry.fallbackInput.value.trim() : '';
    const useFallback = !value && !!fallbackValue;
    const message = useFallback ? fallbackValue : input.value;
    entry.body.innerHTML = renderMiniMessage(message || '');
    entry.preview.classList.toggle('is-fallback', useFallback);
    if (entry.label) {
      entry.label.textContent = useFallback ? 'Global config' : 'Preview';
    }
  }

  function updateOverridePreviewsForFallback(fallbackInput) {
    overridePreviewMap.forEach((entry, input) => {
      if (entry.fallbackInput === fallbackInput) {
        updateOverridePreview(input);
      }
    });
  }

  function updateAllOverridePreviews() {
    overridePreviewMap.forEach((_, input) => updateOverridePreview(input));
  }

  function bindInlineMessagePreview(input, preview, fallbackInput) {
    if (!input || !preview) {
      return;
    }
    const body = preview.querySelector('.cb-message-preview-body');
    const label = preview.querySelector('.cb-message-preview-label');
    if (!body) {
      return;
    }
    overridePreviewMap.set(input, { preview, body, label, fallbackInput });
    const render = () => updateOverridePreview(input);
    preview.addEventListener('click', () => {
      input.focus();
      input.selectionStart = input.value.length;
      input.selectionEnd = input.value.length;
    });
    input.addEventListener('input', render);
    if (fallbackInput && !fallbackBound.has(fallbackInput)) {
      fallbackBound.add(fallbackInput);
      fallbackInput.addEventListener('input', () => updateOverridePreviewsForFallback(fallbackInput));
    }
    render();
  }

  function parseForcedHostEntries(value) {
    return value
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        const parts = line.split(/\s*(?:=|->|=>)\s*/);
        if (parts.length >= 2) {
          const host = parts.shift() || '';
          const server = parts.join(' ').trim();
          return { host: host.trim(), server };
        }
        const fallback = line.split(/\s+/);
        const host = fallback.shift() || '';
        const server = fallback.join(' ').trim();
        return { host: host.trim(), server };
      })
      .filter((entry) => entry.host && entry.server);
  }

  function normalizeForcedHostEntries(value) {
    if (Array.isArray(value)) {
      return value
        .map((entry) => {
          if (!entry || typeof entry !== 'object') {
            return null;
          }
          const host = typeof entry.host === 'string' ? entry.host.trim() : '';
          const server = typeof entry.server === 'string' ? entry.server.trim() : '';
          return { host, server };
        })
        .filter((entry) => entry && (entry.host || entry.server));
    }
    if (value && typeof value === 'object') {
      return Object.entries(value)
        .map(([host, server]) => ({
          host: String(host || '').trim(),
          server: String(server || '').trim()
        }))
        .filter((entry) => entry.host || entry.server);
    }
    return [];
  }

  function listToText(list) {
    return Array.isArray(list) ? list.join(', ') : '';
  }

  function aliasListToText(list) {
    return Array.isArray(list) ? list.join(' ') : '';
  }

  function hostListToText(list) {
    return Array.isArray(list) ? list.join(' ') : '';
  }

  function forcedHostEntriesToText(list) {
    if (!Array.isArray(list)) {
      return '';
    }
    return list
      .map((entry) => {
        if (!entry || typeof entry !== 'object') {
          return '';
        }
        const host = typeof entry.host === 'string' ? entry.host.trim() : '';
        const server = typeof entry.server === 'string' ? entry.server.trim() : '';
        if (!host || !server) {
          return '';
        }
        return `${host} = ${server}`;
      })
      .filter(Boolean)
      .join('\n');
  }

  function setForcedHostDrafts(list) {
    forcedHostDrafts = normalizeForcedHostEntries(list).map((entry) => ({
      host: entry.host || '',
      server: entry.server || ''
    }));
    if (!forcedHostDrafts.length) {
      forcedHostDrafts = [{ host: '', server: '' }];
    }
  }

  function syncForcedHostsConfig() {
    config['forced-hosts'] = forcedHostDrafts
      .map((entry) => ({
        host: String(entry.host || '').trim(),
        server: String(entry.server || '').trim()
      }))
      .filter((entry) => entry.host && entry.server);
  }

  function renderForcedHosts() {
    if (!forcedHostsList) {
      return;
    }
    forcedHostsList.innerHTML = '';
    forcedHostDrafts.forEach((entry, index) => {
      const row = document.createElement('div');
      row.className = 'cb-forced-row';
      const hostInput = document.createElement('input');
      hostInput.type = 'text';
      hostInput.placeholder = 'play.example.com';
      hostInput.value = entry.host || '';
      hostInput.setAttribute('aria-label', 'Forced host');
      const serverInput = document.createElement('input');
      serverInput.type = 'text';
      serverInput.placeholder = 'minigame-1';
      serverInput.value = entry.server || '';
      serverInput.setAttribute('aria-label', 'Target server');
      const removeBtn = document.createElement('button');
      removeBtn.type = 'button';
      removeBtn.className = 'cb-btn cb-btn-ghost cb-icon-btn';
      removeBtn.setAttribute('aria-label', 'Remove mapping');
      removeBtn.title = 'Remove mapping';
      removeBtn.innerHTML = `
        <svg class="cb-trash-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M4 7h16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M9 7V5h6v2" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M7 7l1 12h8l1-12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M10 11v6M14 11v6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      `;
      hostInput.addEventListener('input', () => {
        forcedHostDrafts[index].host = hostInput.value;
        syncForcedHostsConfig();
        commitChange();
      });
      serverInput.addEventListener('input', () => {
        forcedHostDrafts[index].server = serverInput.value;
        syncForcedHostsConfig();
        commitChange();
      });
      removeBtn.addEventListener('click', () => {
        forcedHostDrafts.splice(index, 1);
        if (!forcedHostDrafts.length) {
          forcedHostDrafts = [{ host: '', server: '' }];
        }
        syncForcedHostsConfig();
        renderForcedHosts();
        commitChange();
      });
      row.appendChild(hostInput);
      row.appendChild(serverInput);
      row.appendChild(removeBtn);
      forcedHostsList.appendChild(row);
    });
  }

  function normalizeI18nOverrides(raw) {
    const data = raw && typeof raw === 'object' ? raw : {};
    const out = {};
    Object.entries(data).forEach(([locale, entries]) => {
      if (!locale) {
        return;
      }
      const entryMap = entries && typeof entries === 'object' ? entries : {};
      const normalizedEntries = {};
      Object.entries(entryMap).forEach(([key, value]) => {
        if (!key) {
          return;
        }
        normalizedEntries[String(key)] = value == null ? '' : String(value);
      });
      out[String(locale)] = normalizedEntries;
    });
    return out;
  }

  function setI18nOverrideDrafts(raw) {
    const normalized = normalizeI18nOverrides(raw);
    const locales = Object.keys(normalized);
    i18nOverrideDrafts = locales.map((locale) => ({
      locale,
      entries: Object.entries(normalized[locale] || {}).map(([key, value]) => ({
        key,
        value
      }))
    }));
    if (!i18nOverrideDrafts.length && fallbackI18nLocales.length) {
      i18nOverrideDrafts = fallbackI18nLocales.map((locale) => ({
        locale,
        entries: []
      }));
    }
  }

  function syncI18nOverridesConfig() {
    config.i18n = config.i18n || {};
    const overrides = {};
    i18nOverrideDrafts.forEach((localeEntry) => {
      const locale = String(localeEntry.locale || '').trim();
      if (!locale) {
        return;
      }
      const entries = {};
      (localeEntry.entries || []).forEach((entry) => {
        const key = String(entry.key || '').trim();
        if (!key) {
          return;
        }
        entries[key] = entry.value == null ? '' : String(entry.value);
      });
      overrides[locale] = entries;
    });
    config.i18n.overrides = overrides;
  }

  function nextI18nLocaleName() {
    const base = 'custom';
    const existing = new Set(
      i18nOverrideDrafts.map((entry) => String(entry.locale || '').trim()).filter(Boolean)
    );
    let candidate = base;
    let i = 1;
    while (existing.has(candidate)) {
      candidate = `${base}_${i}`;
      i += 1;
    }
    return candidate;
  }

  function nextI18nKey(entries) {
    const base = 'example.key';
    const existing = new Set((entries || []).map((entry) => String(entry.key || '').trim()).filter(Boolean));
    let candidate = base;
    let i = 1;
    while (existing.has(candidate)) {
      candidate = `${base}.${i}`;
      i += 1;
    }
    return candidate;
  }

  function renderI18nOverrides() {
    if (!i18nOverridesList) {
      return;
    }
    i18nOverridesList.innerHTML = '';
    if (!i18nOverrideDrafts.length) {
      const empty = document.createElement('div');
      empty.className = 'cb-empty';
      empty.textContent = 'No overrides yet.';
      i18nOverridesList.appendChild(empty);
      return;
    }
    i18nOverrideDrafts.forEach((localeEntry, localeIndex) => {
      const localeCard = document.createElement('div');
      localeCard.className = 'cb-i18n-locale';
      const head = document.createElement('div');
      head.className = 'cb-i18n-locale-head';
      const localeInput = document.createElement('input');
      localeInput.type = 'text';
      localeInput.placeholder = 'en_us';
      localeInput.value = localeEntry.locale || '';
      localeInput.setAttribute('aria-label', 'Locale');
      const removeLocaleBtn = document.createElement('button');
      removeLocaleBtn.type = 'button';
      removeLocaleBtn.className = 'cb-btn cb-btn-ghost cb-icon-btn';
      removeLocaleBtn.setAttribute('aria-label', 'Remove locale');
      removeLocaleBtn.title = 'Remove locale';
      removeLocaleBtn.innerHTML = `
        <svg class="cb-trash-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M4 7h16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M9 7V5h6v2" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M7 7l1 12h8l1-12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M10 11v6M14 11v6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      `;
      localeInput.addEventListener('input', () => {
        i18nOverrideDrafts[localeIndex].locale = localeInput.value;
        syncI18nOverridesConfig();
        commitChange();
      });
      removeLocaleBtn.addEventListener('click', () => {
        i18nOverrideDrafts.splice(localeIndex, 1);
        syncI18nOverridesConfig();
        renderI18nOverrides();
        commitChange();
      });
      head.appendChild(localeInput);
      head.appendChild(removeLocaleBtn);
      localeCard.appendChild(head);

      const entriesList = document.createElement('div');
      entriesList.className = 'cb-i18n-entries';
      const entries = localeEntry.entries || [];
      if (!entries.length) {
        const empty = document.createElement('div');
        empty.className = 'cb-empty';
        empty.textContent = 'No overrides for this locale.';
        entriesList.appendChild(empty);
      } else {
        entries.forEach((entry, entryIndex) => {
          const row = document.createElement('div');
          row.className = 'cb-i18n-row';
          const keyInput = document.createElement('input');
          keyInput.type = 'text';
          keyInput.placeholder = 'lobby.jump.started';
          keyInput.value = entry.key || '';
          keyInput.setAttribute('aria-label', 'Translation key');
          const valueInput = document.createElement('input');
          valueInput.type = 'text';
          valueInput.placeholder = '<green>Jump run started!</green>';
          valueInput.value = entry.value || '';
          valueInput.setAttribute('aria-label', 'Translation value');
          const removeBtn = document.createElement('button');
          removeBtn.type = 'button';
          removeBtn.className = 'cb-btn cb-btn-ghost cb-icon-btn';
          removeBtn.setAttribute('aria-label', 'Remove override');
          removeBtn.title = 'Remove override';
          removeBtn.innerHTML = `
            <svg class="cb-trash-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4 7h16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <path d="M9 7V5h6v2" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <path d="M7 7l1 12h8l1-12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <path d="M10 11v6M14 11v6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          `;
          keyInput.addEventListener('input', () => {
            i18nOverrideDrafts[localeIndex].entries[entryIndex].key = keyInput.value;
            syncI18nOverridesConfig();
            commitChange();
          });
          valueInput.addEventListener('input', () => {
            i18nOverrideDrafts[localeIndex].entries[entryIndex].value = valueInput.value;
            syncI18nOverridesConfig();
            commitChange();
          });
          removeBtn.addEventListener('click', () => {
            i18nOverrideDrafts[localeIndex].entries.splice(entryIndex, 1);
            syncI18nOverridesConfig();
            renderI18nOverrides();
            commitChange();
          });
          row.appendChild(keyInput);
          row.appendChild(valueInput);
          row.appendChild(removeBtn);
          entriesList.appendChild(row);
        });
      }
      localeCard.appendChild(entriesList);

      const localeActions = document.createElement('div');
      localeActions.className = 'cb-i18n-locale-actions';
      const addEntryBtn = document.createElement('button');
      addEntryBtn.type = 'button';
      addEntryBtn.className = 'cb-btn cb-btn-ghost cb-mini-btn';
      addEntryBtn.textContent = 'Add entry';
      addEntryBtn.addEventListener('click', () => {
        const entryList = i18nOverrideDrafts[localeIndex].entries;
        entryList.push({ key: nextI18nKey(entryList), value: '' });
        syncI18nOverridesConfig();
        renderI18nOverrides();
        commitChange();
      });
      localeActions.appendChild(addEntryBtn);
      localeCard.appendChild(localeActions);

      i18nOverridesList.appendChild(localeCard);
    });
  }

  function applyI18nToForm() {
    config.i18n = config.i18n && typeof config.i18n === 'object' ? config.i18n : {};
    if (inputs.i18nDefaultLocale) {
      inputs.i18nDefaultLocale.value = config.i18n['default-locale'] || 'en_us';
    }
    if (inputs.i18nUseClient) {
      inputs.i18nUseClient.checked = config.i18n['use-client-locale'] !== false;
    }
    setI18nOverrideDrafts(config.i18n.overrides);
    renderI18nOverrides();
  }

  function applyLobbyConfigToForm() {
    if (lobbyInputs.compassEnabled) {
      lobbyInputs.compassEnabled.checked = Boolean(config.compass?.enabled);
    }
    if (lobbyInputs.compassTitle) {
      lobbyInputs.compassTitle.value = config.compass?.['gui-title'] || '';
    }
    if (lobbyInputs.navigatorEnabled) {
      lobbyInputs.navigatorEnabled.checked = Boolean(config.navigator?.enabled);
    }
    if (lobbyInputs.navigatorTitle) {
      lobbyInputs.navigatorTitle.value = config.navigator?.['gui-title'] || '';
    }
    if (lobbyInputs.spawnTeleportEnabled) {
      lobbyInputs.spawnTeleportEnabled.checked = Boolean(config['spawn-teleport']?.enabled);
    }
    if (lobbyInputs.joinTeleportMode) {
      lobbyInputs.joinTeleportMode.value = config['join-teleport']?.mode || 'none';
    }
    if (lobbyInputs.joinTeleportDelay) {
      const delayValue = config['join-teleport']?.['delay-seconds'];
      lobbyInputs.joinTeleportDelay.value = Number.isFinite(Number(delayValue)) ? String(delayValue) : '0';
    }
    if (lobbyInputs.joinTeleportMaxAge) {
      const maxAgeValue = config['join-teleport']?.['last-location']?.['max-age-seconds'];
      lobbyInputs.joinTeleportMaxAge.value = Number.isFinite(Number(maxAgeValue)) ? String(maxAgeValue) : '0';
    }
    if (lobbyInputs.npcEnabled) {
      lobbyInputs.npcEnabled.checked = Boolean(config['lobby-npcs']?.enabled);
    }
    if (lobbyInputs.signEnabled) {
      lobbyInputs.signEnabled.checked = Boolean(config['lobby-signs']?.enabled);
    }
    if (lobbyInputs.gameplayDamage) {
      lobbyInputs.gameplayDamage.checked = Boolean(config.gameplay?.['disable-damage']);
    }
    if (lobbyInputs.gameplayHunger) {
      lobbyInputs.gameplayHunger.checked = Boolean(config.gameplay?.['disable-hunger']);
    }
    if (lobbyInputs.gameplayHeal) {
      lobbyInputs.gameplayHeal.checked = Boolean(config.gameplay?.['heal-on-join']);
    }
    if (lobbyInputs.bstatsEnabled) {
      lobbyInputs.bstatsEnabled.checked = Boolean(config.bstats?.enabled);
    }
    if (lobbyInputs.minestomSecret) {
      lobbyInputs.minestomSecret.value = config.minestom?.['velocity-secret'] || '';
    }
    applyI18nToForm();
  }

  function applyConfigToForm() {
    if (configMode === 'lobby') {
      applyLobbyConfigToForm();
      return;
    }
    inputs.baseCommand.value = config['base-hub-command'] || '';
    inputs.aliases.value = aliasListToText(config.aliases);
    inputs.hideOn.value = config['hide-hub-command-on-lobby'] || '';
    inputs.autoJoin.checked = Boolean(config['auto-select']?.['on-join']);
    inputs.autoKick.checked = Boolean(config['auto-select']?.['on-server-kick']);
    inputs.lastLobby.checked = Boolean(config['last-lobby']?.enabled);
    inputs.debugEnabled.checked = Boolean(config.debug?.enabled);
    inputs.debugPermission.value = config.debug?.permission || '';
    setDebugCategoryVisibility(inputs.debugEnabled.checked);
    const debugCategories = normalizeDebugCategories(config.debug?.categories);
    config.debug = config.debug || {};
    config.debug.categories = debugCategories;
    Object.entries(debugCategoryInputs).forEach(([key, input]) => {
      if (!input) {
        return;
      }
      input.checked = Boolean(debugCategories[key]);
    });
    inputs.messageSuccess.value = config.messages?.['success-message'] || '';
    inputs.messageAlready.value = config.messages?.['already-connected-message'] || '';
    inputs.messageProgress.value = config.messages?.['connection-in-progress-message'] || '';
    inputs.messageDisconnected.value = config.messages?.['server-disconnected-message'] || '';
    inputs.messageCancelled.value = config.messages?.['connection-cancelled-message'] || '';
    inputs.systemPlayersOnly.value = config['system-messages']?.['players-only-command-message'] || '';
    inputs.systemNoLobby.value = config['system-messages']?.['no-lobby-found-message'] || '';
    inputs.kickEnabled.checked = Boolean(config['kick-message']?.enabled);
    inputs.kickPrefix.value = config['kick-message']?.prefix ?? '';
    inputs.kickSuffix.value = config['kick-message']?.suffix ?? '';
    inputs.finderStart.value = config.finder?.['start-duration'] ?? config.finder?.startDuration ?? '';
    inputs.finderIncrement.value = config.finder?.['increment-duration'] ?? config.finder?.incrementDuration ?? '';
    inputs.finderMax.value = config.finder?.['max-duration'] ?? config.finder?.maxDuration ?? '';
    inputs.finderRefresh.value = config.finder?.['refresh-interval-in-ticks'] ?? config.finder?.refreshIntervalInTicks ?? '';
    setForcedHostDrafts(config['forced-hosts']);
    renderForcedHosts();
    inputs.dataCollectEnabled.checked = Boolean(config['data-collection']?.enabled);
    inputs.dataCollectUuid.checked = Boolean(config['data-collection']?.['include-uuid']);
    inputs.dataCollectFile.value = config['data-collection']?.['dump-file'] || '';
    inputs.dataCollectInterval.value = config['data-collection']?.['dump-interval-minutes'] ?? '';
    inputs.dataCollectUsers.value = config['data-collection']?.['max-users'] ?? '';
    inputs.dataCollectServers.value = config['data-collection']?.['max-servers'] ?? '';
    updateHideRegexStatus();
    setRegexContext(tabButtons.find((btn) => btn.classList.contains('is-active'))?.dataset.tab);
    updateAllMessagePreviews();
    updateAllOverridePreviews();
    applyI18nToForm();
  }

  function updateOutput() {
    if (!yamlLib) {
      return false;
    }
    try {
      const text = yamlLib.dump(config, { lineWidth: 120, noRefs: true });
      outputEl.textContent = text;
      if (currentDownloadUrl) {
        URL.revokeObjectURL(currentDownloadUrl);
        currentDownloadUrl = null;
      }
      const blob = new Blob([text], { type: 'text/yaml' });
      currentDownloadUrl = URL.createObjectURL(blob);
      setUpdated();
      return true;
    } catch (error) {
      showStatus('Failed to build YAML: ' + error.message, 'bad');
      return false;
    }
  }

  function commitChange() {
    if (updateOutput()) {
      showStatus('Edited', 'warn');
    }
    updateSummary();
  }

  function applyLoadedConfig(raw, sourceLabel, options = {}) {
    const mode = options.mode || configMode;
    if (mode !== configMode) {
      configMode = mode;
      defaultConfig = configMode === 'lobby' ? defaultConfigLobby : defaultConfigProxy;
      applyModeUi();
    }
    config = normalizeConfig(raw, mode);
    baselineConfig = clone(config);
    applyConfigToForm();
    if (mode === 'proxy') {
      renderGroups();
      renderLobbies();
    } else {
      activeLobby = null;
    }
    updateOutput();
    setSource(sourceLabel);
    showStatus('Loaded ' + sourceLabel, 'ok');
    refreshPermissionPills();
    updateSummary();
  }

  function resetToBaseline() {
    config = clone(baselineConfig);
    applyConfigToForm();
    if (configMode === 'proxy') {
      renderGroups();
      renderLobbies();
    } else {
      activeLobby = null;
    }
    updateOutput();
    showStatus('Reset to ' + (sourceEl.textContent || 'defaults'), 'idle');
    refreshPermissionPills();
    updateSummary();
  }

  function buildRegex(raw) {
    const trimmed = raw.trim();
    if (!trimmed) {
      return null;
    }
    let source = trimmed;
    let flags = '';
    const slashMatch = source.match(/^\/(.+)\/([a-z]*)$/);
    if (slashMatch) {
      source = slashMatch[1];
      flags = slashMatch[2];
    }
    const inlineMatch = source.match(/^\(\?([a-zA-Z]+)\)/);
    if (inlineMatch) {
      const inlineFlags = inlineMatch[1].toLowerCase();
      for (const ch of inlineFlags) {
        if ('imsu'.includes(ch) && !flags.includes(ch)) {
          flags += ch;
        }
      }
      source = source.slice(inlineMatch[0].length);
    }
    return new RegExp(source, flags);
  }

  function findServerForFilter(filter) {
    const servers = getActivitySource().servers;
    if (!filter || !servers.length) {
      return '';
    }
    let regex;
    try {
      regex = buildRegex(filter);
    } catch (error) {
      regex = null;
    }
    if (!regex) {
      return '';
    }
    return servers.find((server) => regex.test(server)) || '';
  }

  function syncRegexLabWithLobby(lobby, options = {}) {
    if (regexContext !== 'lobby' || !lobby || !regexInput || !regexTest || !regexResult) {
      return;
    }
    const filter = typeof lobby.filter === 'string' ? lobby.filter : '';
    regexInput.value = filter;
    if (!options.keepTest) {
      const match = findServerForFilter(filter);
      regexTest.value = match || lobby.name || '';
    }
    updateRegexResult(regexInput.value, regexTest.value, regexResult);
  }

  function updateRegexResult(pattern, value, resultEl) {
    const cleanedPattern = pattern.trim();
    if (!cleanedPattern) {
      resultEl.dataset.state = 'idle';
      resultEl.setAttribute('aria-label', 'Enter a regex');
      resultEl.setAttribute('title', 'Enter a regex');
      return;
    }
    let regex;
    try {
      regex = buildRegex(cleanedPattern);
    } catch (error) {
      resultEl.dataset.state = 'bad';
      resultEl.setAttribute('aria-label', 'Invalid regex');
      resultEl.setAttribute('title', 'Invalid regex');
      return;
    }
    if (!value.trim()) {
      resultEl.dataset.state = 'idle';
      resultEl.setAttribute('aria-label', 'Enter a test string');
      resultEl.setAttribute('title', 'Enter a test string');
      return;
    }
    const match = regex ? regex.test(value) : false;
    resultEl.dataset.state = match ? 'ok' : 'bad';
    resultEl.setAttribute('aria-label', match ? 'Match' : 'No match');
    resultEl.setAttribute('title', match ? 'Match' : 'No match');
  }

  function updateHideRegexStatus() {
    if (regexContext !== 'hide') {
      return;
    }
    regexInput.value = inputs.hideOn.value || '';
    updateRegexResult(regexInput.value, regexTest.value, regexResult);
  }

  function deriveGroupParent(group) {
    if (!group) {
      return '';
    }
    const allLobbies = Array.isArray(config.lobbies) ? config.lobbies : [];
    const parents = (Array.isArray(group.lobbies) ? group.lobbies : [])
      .map((name) => allLobbies.find((lobby) => lobby.name === name))
      .filter(Boolean)
      .map((lobby) => lobby.parent || '')
      .filter((value) => value !== '');
    const uniqueParents = Array.from(new Set(parents));
    return uniqueParents.length === 1 ? uniqueParents[0] : '';
  }

  function openGroupModal(selectEl, lobby) {
    pendingGroupSelect = selectEl || null;
    pendingGroupLobby = lobby || null;
    pendingGroupPrevious = selectEl ? (selectEl.dataset.current || selectEl.value || '') : '';
    groupModal.hidden = false;
    groupModal.classList.add('is-open');
    groupModalInput.value = '';
    groupModalInput.focus();
  }

  function closeGroupModal() {
    groupModal.classList.remove('is-open');
    groupModal.hidden = true;
    pendingGroupSelect = null;
    pendingGroupLobby = null;
    pendingGroupPrevious = '';
  }

  function assignLobbyToGroup(lobby, groupName) {
    const previousGroups = groupsForLobby(lobby.name || '');
    const groups = Array.isArray(config['lobby-groups']) ? config['lobby-groups'] : [];
    groups.forEach((group) => {
      const list = Array.isArray(group.lobbies) ? group.lobbies.slice() : [];
      const next = list.filter((name) => name !== lobby.name);
      if (group.name === groupName) {
        next.push(lobby.name || '');
      }
      group.lobbies = Array.from(new Set(next.filter(Boolean)));
    });
    const selectedGroup = groups.find((group) => group.name === groupName);
    lobby.parent = deriveGroupParent(selectedGroup);
    lobby['parent-groups'] = [];
    const wasGrouped = previousGroups.length > 0;
    const viaDrag = Boolean(draggingLobby);
    if (viaDrag && groupName && !wasGrouped) {
      markTutorialAction('lobbyGrouped');
    }
    if (viaDrag && !groupName && wasGrouped) {
      markTutorialAction('lobbyUngrouped');
    }
  }

  function createGroupFromModal() {
    const raw = groupModalInput.value.trim();
    if (!raw) {
      showStatus('Enter a group name', 'warn');
      return;
    }
    if (!Array.isArray(config['lobby-groups'])) {
      config['lobby-groups'] = [];
    }
    const groups = config['lobby-groups'];
    const existing = groups.find((group) => (group.name || '').toLowerCase() === raw.toLowerCase());
    const group = existing || { name: raw, lobbies: [], 'forced-hosts': [] };
    if (!existing) {
      groups.push(group);
    }
    if (pendingGroupSelect && pendingGroupLobby) {
      pendingGroupSelect.value = group.name;
      pendingGroupSelect.dataset.current = group.name;
      assignLobbyToGroup(pendingGroupLobby, group.name);
      renderGroups(false);
      renderLobbies();
      commitChange();
    } else {
      renderGroups();
      commitChange();
    }
    closeGroupModal();
  }

  function renderGroups(updateLobbies = true) {
    if (!groupsEl) {
      if (updateLobbies) {
        renderLobbies();
      }
      return;
    }
    groupsEl.innerHTML = '';
    const groups = Array.isArray(config['lobby-groups']) ? config['lobby-groups'] : [];
    const getGroupName = (group) => (group && typeof group.name === 'string' ? group.name.trim() : '');
    const getParentGroup = (group) => {
      const raw = group && typeof group['parent-group'] === 'string' ? group['parent-group'].trim() : '';
      const name = getGroupName(group);
      return raw && raw !== name ? raw : '';
    };
    const byName = new Map();
    groups.forEach((group) => {
      const name = getGroupName(group);
      if (name) {
        byName.set(name, group);
      }
    });
    const children = new Map();
    groups.forEach((group) => {
      const parent = getParentGroup(group);
      const name = getGroupName(group);
      if (parent && byName.has(parent) && parent !== name) {
        if (!children.has(parent)) {
          children.set(parent, []);
        }
        children.get(parent).push(group);
      }
    });
    const roots = groups.filter((group) => {
      const parent = getParentGroup(group);
      return !parent || !byName.has(parent);
    });
    const rendered = new Set();

    const renderGroupRow = (group, depth) => {
      const index = config['lobby-groups'].indexOf(group);
      const card = document.createElement('div');
      card.className = 'cb-group-row';
      card.style.setProperty('--cb-depth', String(depth));
      card.dataset.depth = String(depth);
      card.innerHTML = `
        <label class="cb-field cb-group-field">
          <span title="Name of the lobby group">Group</span>
          <input type="text" data-field="name" placeholder="main">
        </label>
        <label class="cb-field cb-group-field">
          <span title="Parent group for nesting">Parent group</span>
          <select data-field="parentGroup"></select>
        </label>
        <label class="cb-field cb-group-field">
          <span title="Lobbies that belong to this group">Lobbies</span>
          <input type="text" data-field="lobbies" placeholder="lobby, teamlobby">
        </label>
        <label class="cb-field cb-group-field">
          <span title="Parent lobby used when /hub is run from a lobby in this group">Parent lobby</span>
          <select data-field="parent"></select>
        </label>
        <label class="cb-field cb-group-field cb-group-span">
          <span title="Domains that route directly into this group">Forced hosts</span>
          <input type="text" data-field="forcedHosts" placeholder="play.example.com minigame.example.com">
        </label>
        <button type="button" class="cb-btn cb-btn-danger cb-icon-btn cb-group-remove" data-action="remove" aria-label="Remove group" title="Remove group">
          <svg class="cb-trash-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 7h16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <path d="M9 7V5h6v2" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <path d="M7 7l1 12h8l1-12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <path d="M10 11v6M14 11v6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      `;
      const nameInput = card.querySelector('[data-field="name"]');
      const lobbiesInput = card.querySelector('[data-field="lobbies"]');
      const parentGroupSelect = card.querySelector('[data-field="parentGroup"]');
      const parentSelect = card.querySelector('[data-field="parent"]');
      const forcedHostsInput = card.querySelector('[data-field="forcedHosts"]');
      const removeBtn = card.querySelector('[data-action="remove"]');
      const allLobbies = Array.isArray(config.lobbies) ? config.lobbies : [];
      const allGroups = Array.isArray(config['lobby-groups']) ? config['lobby-groups'] : [];
      const currentName = getGroupName(group);
      const parentGroupOptions = ['<option value="">No parent</option>'].concat(
        allGroups
          .filter((candidate) => candidate.name && candidate.name !== currentName)
          .map((candidate) => `<option value="${candidate.name}">${candidate.name}</option>`)
      );
      parentGroupSelect.innerHTML = parentGroupOptions.join('');
      const currentParentGroup = getParentGroup(group);
      parentGroupSelect.value = currentParentGroup;
      const lobbyOptions = ['<option value="">No parent</option>'].concat(
        allLobbies.map((lobby) => `<option value="${lobby.name}">${lobby.name}</option>`)
      );
      parentSelect.innerHTML = lobbyOptions.join('');
      parentSelect.value = deriveGroupParent(group);
      nameInput.value = group.name || '';
      lobbiesInput.value = listToText(group.lobbies);
      forcedHostsInput.value = hostListToText(group['forced-hosts']);
      enhanceHints(card);
      nameInput.addEventListener('input', () => {
        const previousName = group.name || '';
        group.name = nameInput.value.trim();
        if (previousName && previousName !== group.name) {
          groups.forEach((entry) => {
            if (entry['parent-group'] === previousName) {
              entry['parent-group'] = group.name;
            }
          });
        }
        renderLobbies();
        commitChange();
      });
      nameInput.addEventListener('blur', () => {
        renderGroups(false);
      });
      parentGroupSelect.addEventListener('change', () => {
        const value = parentGroupSelect.value;
        group['parent-group'] = value && value !== group.name ? value : '';
        renderGroups(false);
        commitChange();
      });
      lobbiesInput.addEventListener('input', () => {
        group.lobbies = parseList(lobbiesInput.value);
        const currentParent = parentSelect.value;
        if (currentParent) {
          const lobbyMap = new Map(allLobbies.map((lobby) => [lobby.name, lobby]));
          group.lobbies.forEach((name) => {
            const lobby = lobbyMap.get(name);
            if (lobby) {
              lobby.parent = currentParent;
              lobby['parent-groups'] = [];
            }
          });
        }
        commitChange();
      });
      forcedHostsInput.addEventListener('input', () => {
        group['forced-hosts'] = parseHostList(forcedHostsInput.value);
        commitChange();
      });
      forcedHostsInput.addEventListener('blur', () => {
        forcedHostsInput.value = hostListToText(group['forced-hosts']);
      });
      parentSelect.addEventListener('change', () => {
        const value = parentSelect.value;
        const lobbyMap = new Map(allLobbies.map((lobby) => [lobby.name, lobby]));
        group.lobbies = Array.isArray(group.lobbies) ? group.lobbies : [];
        group.lobbies.forEach((name) => {
          const lobby = lobbyMap.get(name);
          if (lobby) {
            lobby.parent = value;
            lobby['parent-groups'] = [];
          }
        });
        commitChange();
      });
      removeBtn.addEventListener('click', () => {
        const previousName = group.name || '';
        if (index >= 0) {
          config['lobby-groups'].splice(index, 1);
        }
        if (previousName) {
          groups.forEach((entry) => {
            if (entry['parent-group'] === previousName) {
              entry['parent-group'] = '';
            }
          });
        }
        renderGroups();
        commitChange();
      });
      groupsEl.appendChild(card);
    };

    const walk = (group, depth) => {
      if (rendered.has(group)) {
        return;
      }
      renderGroupRow(group, depth);
      rendered.add(group);
      const name = getGroupName(group);
      const kids = children.get(name) || [];
      kids.forEach((child) => walk(child, depth + 1));
    };

    roots.forEach((group) => walk(group, 0));
    groups.forEach((group) => {
      if (!rendered.has(group)) {
        renderGroupRow(group, 0);
      }
    });
    if (updateLobbies) {
      renderLobbies();
    }
  }

  function getPrimaryCommand(lobby) {
    const keys = lobby.commands ? Object.keys(lobby.commands) : [];
    if (keys.length === 0) {
      return { key: '', value: null };
    }
    return { key: keys[0], value: lobby.commands[keys[0]] };
  }

  function buildLobbyStructure() {
    const groups = Array.isArray(config['lobby-groups']) ? config['lobby-groups'] : [];
    const getGroupName = (group) => (group && typeof group.name === 'string' ? group.name.trim() : '');
    const groupEntries = groups
      .map((group) => ({ group, name: getGroupName(group) }))
      .filter((entry) => entry.name);
    const groupByName = new Map();
    groupEntries.forEach((entry) => groupByName.set(entry.name, entry.group));
    const getParentGroup = (group) => {
      const raw = group && typeof group['parent-group'] === 'string' ? group['parent-group'].trim() : '';
      const name = getGroupName(group);
      return raw && raw !== name && groupByName.has(raw) ? raw : '';
    };
    const children = new Map();
    groupEntries.forEach((entry) => {
      const parent = getParentGroup(entry.group);
      if (parent) {
        if (!children.has(parent)) {
          children.set(parent, []);
        }
        children.get(parent).push(entry);
      }
    });
    const rootGroups = groupEntries.filter((entry) => !getParentGroup(entry.group));

    const lobbyByName = new Map();
    config.lobbies.forEach((lobby) => {
      const name = (lobby && lobby.name ? String(lobby.name) : '').trim();
      if (name && !lobbyByName.has(name)) {
        lobbyByName.set(name, lobby);
      }
    });
    const lobbyGroupMap = new Map();
    groupEntries.forEach((entry) => {
      const list = Array.isArray(entry.group.lobbies) ? entry.group.lobbies : [];
      list.forEach((name) => {
        if (name && lobbyByName.has(name) && !lobbyGroupMap.has(name)) {
          lobbyGroupMap.set(name, entry.name);
        }
      });
    });
    const lobbyBuckets = new Map();
    groupEntries.forEach((entry) => {
      const list = Array.isArray(entry.group.lobbies) ? entry.group.lobbies : [];
      const items = list.map((name) => lobbyByName.get(name)).filter(Boolean);
      lobbyBuckets.set(entry.name, items);
    });
    const ungrouped = config.lobbies.filter((lobby) => {
      const name = (lobby && lobby.name ? String(lobby.name) : '').trim();
      return name && !lobbyGroupMap.has(name);
    });
    lobbyBuckets.set('', ungrouped);

    return {
      groups,
      groupEntries,
      groupByName,
      getParentGroup,
      children,
      rootGroups,
      lobbyGroupMap,
      lobbyBuckets
    };
  }

  function getLobbyDisplayOrder() {
    const { lobbyBuckets, children, rootGroups } = buildLobbyStructure();
    const order = [];
    const pushGroup = (groupName) => {
      const items = lobbyBuckets.get(groupName) || [];
      items.forEach((lobby) => {
        if (lobby) {
          order.push(lobby);
        }
      });
    };
    pushGroup('');
    const walk = (entry) => {
      pushGroup(entry.name);
      const kids = children.get(entry.name) || [];
      kids.forEach((child) => walk(child));
    };
    rootGroups.forEach((entry) => walk(entry));
    return order;
  }

  function renderLobbies() {
    lobbiesEl.innerHTML = '';
    lobbiesListEl.innerHTML = '';
    overridePreviewMap.clear();
    if (activeLobby && !config.lobbies.includes(activeLobby)) {
      activeLobby = null;
    }
    let activeIndex = activeLobby ? config.lobbies.indexOf(activeLobby) : -1;

    if (!config.lobbies.length) {
      const empty = document.createElement('div');
      empty.className = 'cb-lobby-empty';
      empty.textContent = 'No lobbies yet. Add one to start.';
      lobbiesListEl.appendChild(empty);
      return;
    }

    const lobbyIndices = new Map();
    config.lobbies.forEach((lobby, index) => lobbyIndices.set(lobby, index));

    const {
      groupEntries,
      groupByName,
      children,
      rootGroups,
      lobbyGroupMap,
      lobbyBuckets
    } = buildLobbyStructure();

    const clearDropTargets = () => {
      lobbiesListEl.querySelectorAll('.cb-lobby-group.is-drop-target').forEach((el) => {
        el.classList.remove('is-drop-target');
      });
      lobbiesListEl.querySelectorAll('.cb-lobby-item.is-drop-target').forEach((el) => {
        el.classList.remove('is-drop-target');
      });
    };

    const clearGroupDropTargets = () => {
      lobbiesListEl.querySelectorAll('.cb-lobby-group.is-group-drop').forEach((el) => {
        el.classList.remove('is-group-drop');
      });
    };

    const isDescendant = (sourceName, targetName) => {
      if (!sourceName || !targetName) {
        return false;
      }
      const stack = (children.get(sourceName) || []).map((entry) => entry.name);
      while (stack.length) {
        const current = stack.pop();
        if (current === targetName) {
          return true;
        }
        const kids = children.get(current) || [];
        kids.forEach((child) => stack.push(child.name));
      }
      return false;
    };

    const reorderNameList = (list, lobbyName, beforeName) => {
      const next = list.filter((name) => name && name !== lobbyName);
      if (lobbyName) {
        if (beforeName && beforeName !== lobbyName) {
          const index = next.indexOf(beforeName);
          if (index >= 0) {
            next.splice(index, 0, lobbyName);
          } else {
            next.push(lobbyName);
          }
        } else {
          next.push(lobbyName);
        }
      }
      return next;
    };

    const reorderGroupLobbies = (groupName, lobbyName, beforeName) => {
      const group = groupByName.get(groupName);
      if (!group) {
        return;
      }
      const list = Array.isArray(group.lobbies) ? group.lobbies : [];
      group.lobbies = reorderNameList(list, lobbyName, beforeName);
    };

    const reorderUngroupedLobbies = (lobbyName, beforeName) => {
      const currentGroupMap = new Map();
      const groups = Array.isArray(config['lobby-groups']) ? config['lobby-groups'] : [];
      groups.forEach((group) => {
        if (!Array.isArray(group.lobbies)) {
          return;
        }
        group.lobbies.forEach((name) => {
          if (name) {
            currentGroupMap.set(name, group.name || '');
          }
        });
      });
      const ungrouped = config.lobbies.filter((lobby) => {
        const name = (lobby && lobby.name ? String(lobby.name) : '').trim();
        return name && !currentGroupMap.has(name);
      });
      const names = ungrouped.map((lobby) => lobby.name).filter(Boolean);
      const nextNames = reorderNameList(names, lobbyName, beforeName);
      const lobbyByName = new Map(config.lobbies.map((lobby) => [lobby.name, lobby]));
      const nextUngrouped = nextNames.map((name) => lobbyByName.get(name)).filter(Boolean);
      let nextIndex = 0;
      config.lobbies = config.lobbies.map((lobby) => {
        const name = (lobby && lobby.name ? String(lobby.name) : '').trim();
        if (name && !currentGroupMap.has(name)) {
          const replacement = nextUngrouped[nextIndex];
          nextIndex += 1;
          return replacement || lobby;
        }
        return lobby;
      });
    };

    const createLobbyListItem = (lobby) => {
      const index = lobbyIndices.get(lobby) ?? 0;
      const lobbyName = (lobby && lobby.name ? String(lobby.name) : '').trim();
      const groupName = lobbyGroupMap.get(lobbyName) || '';
      const listItem = document.createElement('button');
      listItem.type = 'button';
      listItem.className = 'cb-lobby-item';
      listItem.dataset.lobby = lobbyName;
      listItem.dataset.group = groupName;
      if (lobby === activeLobby) {
        listItem.classList.add('is-active');
      }
      const name = lobbyName || `Lobby ${index + 1}`;
      const priority = Number.isFinite(Number(lobby.priority)) ? Number(lobby.priority) : 0;
      const autojoin = Boolean(lobby.autojoin);
      const autojoinIcon = autojoin
        ? `
            <span class="cb-lobby-item-autojoin" title="Autojoin enabled" aria-label="Autojoin enabled">A</span>
          `
        : '';
      listItem.innerHTML = `
        <div class="cb-lobby-item-row">
          <span class="cb-lobby-item-name">${name}</span>
          <div class="cb-lobby-item-meta">
            <span class="cb-lobby-item-priority">prio ${priority}</span>
            ${autojoinIcon}
          </div>
        </div>
      `;
      listItem.draggable = true;
      listItem.addEventListener('click', () => {
        activeLobby = lobby;
        renderLobbies();
        syncRegexLabWithLobby(lobby);
      });
      listItem.addEventListener('dragstart', (event) => {
        draggingLobby = lobby;
        draggingGroup = '';
        listItem.classList.add('is-dragging');
        if (event.dataTransfer) {
          event.dataTransfer.effectAllowed = 'move';
          event.dataTransfer.setData('text/plain', lobby.name || '');
        }
      });
      listItem.addEventListener('dragend', () => {
        listItem.classList.remove('is-dragging');
        draggingLobby = null;
        clearDropTargets();
      });
      attachItemDropHandlers(listItem, lobby);
      return listItem;
    };

    const resolveDraggedLobby = (event) => {
      if (draggingLobby) {
        return draggingLobby;
      }
      const name = event?.dataTransfer ? event.dataTransfer.getData('text/plain') : '';
      if (name) {
        return config.lobbies.find((lobby) => lobby.name === name) || null;
      }
      return null;
    };

    const moveLobby = (lobby, targetGroup, beforeName) => {
      if (!lobby) {
        return;
      }
      const lobbyName = (lobby && lobby.name ? String(lobby.name) : '').trim();
      if (!lobbyName) {
        return;
      }
      const currentGroup = lobbyGroupMap.get(lobbyName) || '';
      if (currentGroup === targetGroup) {
        if (targetGroup) {
          reorderGroupLobbies(targetGroup, lobbyName, beforeName);
        } else {
          reorderUngroupedLobbies(lobbyName, beforeName);
        }
      } else {
        assignLobbyToGroup(lobby, targetGroup);
        if (targetGroup) {
          reorderGroupLobbies(targetGroup, lobbyName, beforeName);
        } else {
          reorderUngroupedLobbies(lobbyName, beforeName);
        }
      }
      activeLobby = lobby;
      renderGroups(false);
      renderLobbies();
      commitChange();
    };

    const attachItemDropHandlers = (item, targetLobby) => {
      item.addEventListener('dragenter', (event) => {
        if (draggingGroup) {
          return;
        }
        event.stopPropagation();
        event.preventDefault();
        item.classList.add('is-drop-target');
      });
      item.addEventListener('dragover', (event) => {
        if (draggingGroup) {
          return;
        }
        event.stopPropagation();
        event.preventDefault();
        if (event.dataTransfer) {
          event.dataTransfer.dropEffect = 'move';
        }
        item.classList.add('is-drop-target');
      });
      item.addEventListener('dragleave', (event) => {
        if (draggingGroup) {
          return;
        }
        event.stopPropagation();
        if (!item.contains(event.relatedTarget)) {
          item.classList.remove('is-drop-target');
        }
      });
      item.addEventListener('drop', (event) => {
        if (draggingGroup) {
          return;
        }
        event.stopPropagation();
        event.preventDefault();
        item.classList.remove('is-drop-target');
        const lobby = resolveDraggedLobby(event);
        if (!lobby || lobby === targetLobby) {
          return;
        }
        const targetName = (targetLobby && targetLobby.name ? String(targetLobby.name) : '').trim();
        const targetGroup = lobbyGroupMap.get(targetName) || '';
        moveLobby(lobby, targetGroup, targetName);
      });
    };

    const attachDropHandlers = (target, groupName) => {
      target.addEventListener('dragenter', (event) => {
        if (draggingGroup) {
          return;
        }
        event.preventDefault();
        target.classList.add('is-drop-target');
      });
      target.addEventListener('dragover', (event) => {
        if (draggingGroup) {
          return;
        }
        event.preventDefault();
        if (event.dataTransfer) {
          event.dataTransfer.dropEffect = 'move';
        }
        target.classList.add('is-drop-target');
      });
      target.addEventListener('dragleave', (event) => {
        if (draggingGroup) {
          return;
        }
        if (!target.contains(event.relatedTarget)) {
          target.classList.remove('is-drop-target');
        }
      });
      target.addEventListener('drop', (event) => {
        if (draggingGroup) {
          return;
        }
        event.preventDefault();
        target.classList.remove('is-drop-target');
        const lobby = resolveDraggedLobby(event);
        if (!lobby) {
          return;
        }
        moveLobby(lobby, groupName, '');
      });
    };

    const attachGroupDropHandlers = (target, groupName) => {
      target.addEventListener('dragenter', (event) => {
        if (!draggingGroup || draggingLobby) {
          return;
        }
        if (draggingGroup === groupName) {
          return;
        }
        if (groupName && isDescendant(draggingGroup, groupName)) {
          return;
        }
        event.preventDefault();
        target.classList.add('is-group-drop');
      });
      target.addEventListener('dragover', (event) => {
        if (!draggingGroup || draggingLobby) {
          return;
        }
        if (draggingGroup === groupName) {
          return;
        }
        if (groupName && isDescendant(draggingGroup, groupName)) {
          return;
        }
        event.preventDefault();
        if (event.dataTransfer) {
          event.dataTransfer.dropEffect = 'move';
        }
        target.classList.add('is-group-drop');
      });
      target.addEventListener('dragleave', (event) => {
        if (!draggingGroup || draggingLobby) {
          return;
        }
        if (!target.contains(event.relatedTarget)) {
          target.classList.remove('is-group-drop');
        }
      });
      target.addEventListener('drop', (event) => {
        if (!draggingGroup || draggingLobby) {
          return;
        }
        event.preventDefault();
        target.classList.remove('is-group-drop');
        if (draggingGroup === groupName) {
          return;
        }
        if (groupName && isDescendant(draggingGroup, groupName)) {
          return;
        }
        const draggedGroup = groupByName.get(draggingGroup);
        if (!draggedGroup) {
          return;
        }
        draggedGroup['parent-group'] = groupName || '';
        draggingGroup = '';
        clearGroupDropTargets();
        renderLobbies();
        commitChange();
      });
    };

    const renderGroupSection = (label, groupName, depth) => {
      const isUngrouped = !groupName;
      const section = document.createElement('div');
      section.className = 'cb-lobby-group';
      section.dataset.group = groupName;
      section.dataset.depth = String(depth || 0);
      section.style.setProperty('--cb-depth', depth || 0);
      const head = document.createElement('div');
      head.className = 'cb-lobby-group-head';
      if (!isUngrouped) {
        head.draggable = true;
        head.addEventListener('dragstart', (event) => {
          draggingGroup = groupName;
          draggingLobby = null;
          section.classList.add('is-group-dragging');
          if (event.dataTransfer) {
            event.dataTransfer.effectAllowed = 'move';
            event.dataTransfer.setData('text/plain', groupName);
          }
        });
        head.addEventListener('dragend', () => {
          section.classList.remove('is-group-dragging');
          draggingGroup = '';
          clearGroupDropTargets();
        });
      }
      const title = document.createElement('span');
      title.className = 'cb-lobby-group-title';
      title.textContent = label;
      const count = document.createElement('span');
      count.className = 'cb-lobby-group-count';
      const items = lobbyBuckets.get(groupName) || [];
      count.textContent = `${items.length}`;
      const actions = document.createElement('div');
      actions.className = 'cb-lobby-group-actions';
      actions.appendChild(count);
      head.appendChild(title);
      head.appendChild(actions);
      const body = document.createElement('div');
      body.className = 'cb-lobby-group-body';
      if (!items.length) {
        const empty = document.createElement('div');
        empty.className = 'cb-lobby-group-empty';
        empty.textContent = 'Drop lobby here';
        body.appendChild(empty);
      } else {
        items.forEach((lobby) => {
          body.appendChild(createLobbyListItem(lobby));
        });
      }
      section.appendChild(head);
      section.appendChild(body);
      attachDropHandlers(section, groupName);
      attachGroupDropHandlers(section, groupName);
      lobbiesListEl.appendChild(section);
    };

    renderGroupSection('No group', '', 0);
    const renderTree = (entry, depth) => {
      renderGroupSection(entry.name, entry.name, depth);
      const kids = children.get(entry.name) || [];
      kids.forEach((child) => renderTree(child, depth + 1));
    };
    rootGroups.forEach((entry) => renderTree(entry, 0));

    if (activeIndex < 0 && config.lobbies.length) {
      activeIndex = 0;
      activeLobby = config.lobbies[0];
    }

    const lobby = config.lobbies[activeIndex];
    if (!lobby) {
      return;
    }
    const index = activeIndex;
      const card = document.createElement('div');
      card.className = 'cb-lobby';
      card.innerHTML = `
        <div class="cb-mini-head">
          <h3 data-role="title">Lobby ${index + 1}</h3>
          <div class="cb-mini-actions">
            <button type="button" class="cb-btn cb-btn-ghost cb-icon-btn" data-action="duplicate" aria-label="Duplicate lobby" title="Duplicate lobby">
              <svg class="cb-duplicate-icon" viewBox="0 0 24 24" aria-hidden="true">
                <rect x="9" y="9" width="10" height="10" rx="2" fill="none" stroke="currentColor" stroke-width="2"/>
                <rect x="5" y="5" width="10" height="10" rx="2" fill="none" stroke="currentColor" stroke-width="2"/>
              </svg>
            </button>
            <button type="button" class="cb-btn cb-btn-danger cb-icon-btn" data-action="remove" aria-label="Remove lobby" title="Remove lobby">
              <svg class="cb-trash-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 7h16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <path d="M9 7V5h6v2" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <path d="M7 7l1 12h8l1-12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <path d="M10 11v6M14 11v6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
        </div>
        <div class="cb-subtabs" role="tablist" aria-label="Lobby sections">
          <button type="button" class="cb-subtab is-active" data-subtab="details" aria-selected="true">Details</button>
          <button type="button" class="cb-subtab" data-subtab="commands" aria-selected="false">Commands</button>
          <button type="button" class="cb-subtab" data-subtab="messages" aria-selected="false">Messages</button>
        </div>
        <div class="cb-subtab-panel is-active" data-subtab-panel="details">
          <div class="cb-grid-2">
            <label class="cb-field">
              <span title="Internal lobby name">Name</span>
              <input type="text" data-field="name">
            </label>
            <label class="cb-field" data-tutorial="lobbies" data-tutorial-anchor="lobbies">
              <span title="Regex that matches server names for this lobby">Filter regex</span>
              <input type="text" data-field="filter">
            </label>
            <label class="cb-field">
              <span title="Permission required to access this lobby">Permission</span>
              <input type="text" data-field="permission">
            </label>
            <label class="cb-field">
              <span title="Higher numbers are picked first">Priority</span>
              <input type="number" data-field="priority" min="-10" max="100" step="1">
              <small>Higher numbers are picked first.</small>
            </label>
            <label class="cb-field cb-span-2">
              <span title="Domains that force this lobby on join">Forced hosts</span>
              <input type="text" data-field="forcedHosts" placeholder="play.example.com minigame.example.com">
              <small>Separate with spaces.</small>
            </label>
            <label class="cb-field cb-field-toggle">
              <span title="Allow autojoin to this lobby">Autojoin flag</span>
              <input type="checkbox" data-field="autojoin">
            </label>
          </div>
        </div>
        <div class="cb-subtab-panel" data-subtab-panel="commands">
          <div class="cb-grid-2 cb-command-block">
            <label class="cb-field">
              <span title="Command that targets this lobby">Command name</span>
              <input type="text" data-field="commandName" placeholder="lobby">
            </label>
            <label class="cb-field">
              <span title="How the command can be used">Command mode</span>
              <select data-field="commandMode">
                <option value="both">Standalone + subcommand</option>
                <option value="standalone">Standalone only</option>
                <option value="subcommand">Subcommand only</option>
                <option value="disabled">Disabled</option>
              </select>
            </label>
            <label class="cb-field">
              <span title="Regex to hide this command on specific servers">Hide command regex</span>
              <input type="text" data-field="commandHide" placeholder="^(?!.*).$">
            </label>
          </div>
          <p class="cb-note">First command is editable here. Extra commands stay untouched.</p>
        </div>
        <div class="cb-subtab-panel" data-subtab-panel="messages">
          <div class="cb-grid-2 cb-details-grid">
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Override for success message (this lobby only)">Success message</span>
              <textarea rows="2" data-field="ovSuccess"></textarea>
              <div class="cb-message-preview" data-preview-field="ovSuccess">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Override for already connected message">Already connected message</span>
              <textarea rows="2" data-field="ovAlready"></textarea>
              <div class="cb-message-preview" data-preview-field="ovAlready">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Override for connection in progress message">Connection in progress message</span>
              <textarea rows="2" data-field="ovProgress"></textarea>
              <div class="cb-message-preview" data-preview-field="ovProgress">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Override for server disconnected message">Server disconnected message</span>
              <textarea rows="2" data-field="ovDisconnected"></textarea>
              <div class="cb-message-preview" data-preview-field="ovDisconnected">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
            <label class="cb-field cb-span-2 cb-message-field">
              <span title="Override for connection cancelled message">Connection cancelled message</span>
              <textarea rows="2" data-field="ovCancelled"></textarea>
              <div class="cb-message-preview" data-preview-field="ovCancelled">
                <span class="cb-message-preview-label">Preview</span>
                <div class="cb-message-preview-body"></div>
              </div>
            </label>
          </div>
          <p class="cb-footnote">Leave empty to inherit the global message.</p>
        </div>
    `;

      const title = card.querySelector('[data-role="title"]');
      const removeBtn = card.querySelector('[data-action="remove"]');
      const duplicateBtn = card.querySelector('[data-action="duplicate"]');
      const subTabs = Array.from(card.querySelectorAll('[data-subtab]'));
      const subPanels = Array.from(card.querySelectorAll('[data-subtab-panel]'));
      enhanceHints(card);

      const setSubTab = (subtabId) => {
        if (!subtabId) {
          return;
        }
        lobbySubTab = subtabId;
        subTabs.forEach((button) => {
          const active = button.dataset.subtab === subtabId;
          button.classList.toggle('is-active', active);
          button.setAttribute('aria-selected', active ? 'true' : 'false');
        });
        subPanels.forEach((panel) => {
          panel.classList.toggle('is-active', panel.dataset.subtabPanel === subtabId);
        });
      };

      if (subTabs.length && subPanels.length) {
        const available = subTabs.map((button) => button.dataset.subtab).filter(Boolean);
        const initial = available.includes(lobbySubTab) ? lobbySubTab : available[0];
        setSubTab(initial);
        subTabs.forEach((button) => {
          button.addEventListener('click', () => {
            setSubTab(button.dataset.subtab);
          });
        });
      }

      const fields = {
        name: card.querySelector('[data-field="name"]'),
        filter: card.querySelector('[data-field="filter"]'),
        permission: card.querySelector('[data-field="permission"]'),
        priority: card.querySelector('[data-field="priority"]'),
        forcedHosts: card.querySelector('[data-field="forcedHosts"]'),
        autojoin: card.querySelector('[data-field="autojoin"]'),
        commandName: card.querySelector('[data-field="commandName"]'),
        commandMode: card.querySelector('[data-field="commandMode"]'),
        commandHide: card.querySelector('[data-field="commandHide"]')
      };

    const overrideFields = {
      success: card.querySelector('[data-field="ovSuccess"]'),
      already: card.querySelector('[data-field="ovAlready"]'),
      progress: card.querySelector('[data-field="ovProgress"]'),
      disconnected: card.querySelector('[data-field="ovDisconnected"]'),
      cancelled: card.querySelector('[data-field="ovCancelled"]')
    };
    const overridePreviews = {
      success: card.querySelector('[data-preview-field="ovSuccess"]'),
      already: card.querySelector('[data-preview-field="ovAlready"]'),
      progress: card.querySelector('[data-preview-field="ovProgress"]'),
      disconnected: card.querySelector('[data-preview-field="ovDisconnected"]'),
      cancelled: card.querySelector('[data-preview-field="ovCancelled"]')
    };

      fields.name.value = lobby.name || '';
      fields.filter.value = lobby.filter || '';
      fields.permission.value = lobby.permission || '';
      fields.priority.value = Number.isFinite(Number(lobby.priority)) ? lobby.priority : 0;
      fields.forcedHosts.value = hostListToText(lobby['forced-hosts']);
      fields.autojoin.checked = Boolean(lobby.autojoin);

    const primaryCommand = getPrimaryCommand(lobby);
      fields.commandName.value = primaryCommand.key || '';
      fields.commandHide.value = primaryCommand.value?.['hide-on'] || '^(?!.*).$';
      const mode = primaryCommand.value
        ? (primaryCommand.value.standalone && primaryCommand.value.subcommand)
          ? 'both'
          : primaryCommand.value.standalone
            ? 'standalone'
            : primaryCommand.value.subcommand
              ? 'subcommand'
              : 'disabled'
        : 'disabled';
      fields.commandMode.value = mode;
      fields.commandHide.disabled = mode === 'disabled';

    title.textContent = lobby.name ? lobby.name : `Lobby ${index + 1}`;

    card.addEventListener('click', (event) => {
      if (event.target.closest('[data-action="remove"], [data-action="duplicate"]')) {
        return;
      }
      activeLobby = lobby;
      syncRegexLabWithLobby(lobby);
    });

    const overwrite = lobby['overwrite-messages'] && typeof lobby['overwrite-messages'] === 'object'
      ? lobby['overwrite-messages']
      : {};
    lobby['overwrite-messages'] = overwrite;
    overrideFields.success.value = overwrite['success-message'] ?? '';
    overrideFields.already.value = overwrite['already-connected-message'] ?? '';
    overrideFields.progress.value = overwrite['connection-in-progress-message'] ?? '';
    overrideFields.disconnected.value = overwrite['server-disconnected-message'] ?? '';
    overrideFields.cancelled.value = overwrite['connection-cancelled-message'] ?? '';
    const globalMessages = {
      success: document.getElementById('cb-message-success'),
      already: document.getElementById('cb-message-already'),
      progress: document.getElementById('cb-message-progress'),
      disconnected: document.getElementById('cb-message-disconnected'),
      cancelled: document.getElementById('cb-message-cancelled')
    };
    bindInlineMessagePreview(overrideFields.success, overridePreviews.success, globalMessages.success);
    bindInlineMessagePreview(overrideFields.already, overridePreviews.already, globalMessages.already);
    bindInlineMessagePreview(overrideFields.progress, overridePreviews.progress, globalMessages.progress);
    bindInlineMessagePreview(overrideFields.disconnected, overridePreviews.disconnected, globalMessages.disconnected);
    bindInlineMessagePreview(overrideFields.cancelled, overridePreviews.cancelled, globalMessages.cancelled);

      fields.name.addEventListener('input', () => {
        const previousName = lobby.name || '';
        lobby.name = fields.name.value.trim();
        title.textContent = lobby.name ? lobby.name : `Lobby ${index + 1}`;
        if (previousName && previousName !== lobby.name) {
          const groups = Array.isArray(config['lobby-groups']) ? config['lobby-groups'] : [];
          groups.forEach((group) => {
            if (!Array.isArray(group.lobbies)) {
              return;
            }
            const next = group.lobbies.map((name) => (name === previousName ? lobby.name : name));
            group.lobbies = Array.from(new Set(next.filter(Boolean)));
          });
          renderGroups(false);
        }
        const listName = lobbiesListEl.querySelector('.cb-lobby-item.is-active .cb-lobby-item-name');
        if (listName) {
          listName.textContent = lobby.name || `Lobby ${index + 1}`;
        }
        commitChange();
      });

      fields.filter.addEventListener('input', () => {
        lobby.filter = fields.filter.value;
        if (activeLobby === lobby) {
          syncRegexLabWithLobby(lobby, { keepTest: true });
        }
        markTutorialAction('lobbyFilterEdited');
        commitChange();
      });

    fields.permission.addEventListener('input', () => {
      lobby.permission = fields.permission.value;
      commitChange();
      refreshPermissionPills();
    });

      fields.priority.addEventListener('input', () => {
        const value = Number.parseInt(fields.priority.value, 10);
        lobby.priority = Number.isFinite(value) ? value : 0;
        commitChange();
      });

      fields.forcedHosts.addEventListener('input', () => {
        lobby['forced-hosts'] = parseHostList(fields.forcedHosts.value);
        commitChange();
      });

      fields.forcedHosts.addEventListener('blur', () => {
        fields.forcedHosts.value = hostListToText(lobby['forced-hosts']);
      });

      fields.autojoin.addEventListener('change', () => {
        lobby.autojoin = fields.autojoin.checked;
        commitChange();
      });

      fields.commandName.addEventListener('input', () => {
        const newName = fields.commandName.value.trim();
        const currentKey = getPrimaryCommand(lobby).key;
        if (!newName) {
          if (currentKey) {
            delete lobby.commands[currentKey];
          }
          fields.commandMode.value = 'disabled';
          fields.commandHide.disabled = true;
          commitChange();
          return;
        }
        if (!lobby.commands || typeof lobby.commands !== 'object') {
          lobby.commands = {};
        }
        if (!currentKey) {
          lobby.commands[newName] = { standalone: true, subcommand: true, 'hide-on': '^(?!.*).$' };
          fields.commandMode.value = 'both';
          fields.commandHide.disabled = false;
          commitChange();
          return;
        }
        if (currentKey !== newName) {
          lobby.commands[newName] = lobby.commands[currentKey];
          delete lobby.commands[currentKey];
        }
        const updated = lobby.commands[newName];
        const nextMode = updated && (updated.standalone || updated.subcommand)
          ? (updated.standalone && updated.subcommand)
            ? 'both'
            : updated.standalone
              ? 'standalone'
              : 'subcommand'
          : 'disabled';
        fields.commandMode.value = nextMode;
        fields.commandHide.disabled = nextMode === 'disabled';
        commitChange();
      });

      fields.commandHide.addEventListener('input', () => {
        const key = getPrimaryCommand(lobby).key;
        if (!key) {
          return;
        }
        lobby.commands[key]['hide-on'] = fields.commandHide.value;
        commitChange();
      });

      fields.commandMode.addEventListener('change', () => {
        const mode = fields.commandMode.value;
        let key = getPrimaryCommand(lobby).key;
        if (!key && mode !== 'disabled') {
          const fallbackName = fields.commandName.value.trim() || lobby.name || `lobby-${index + 1}`;
          if (!lobby.commands || typeof lobby.commands !== 'object') {
            lobby.commands = {};
          }
          lobby.commands[fallbackName] = { standalone: true, subcommand: true, 'hide-on': '^(?!.*).$' };
          key = fallbackName;
          fields.commandName.value = fallbackName;
        }
        if (key) {
          const cmd = lobby.commands[key] || { standalone: true, subcommand: true, 'hide-on': '^(?!.*).$' };
          if (mode === 'both') {
            cmd.standalone = true;
            cmd.subcommand = true;
          } else if (mode === 'standalone') {
            cmd.standalone = true;
            cmd.subcommand = false;
          } else if (mode === 'subcommand') {
            cmd.standalone = false;
            cmd.subcommand = true;
          } else {
            cmd.standalone = false;
            cmd.subcommand = false;
          }
          lobby.commands[key] = cmd;
        }
        fields.commandHide.disabled = mode === 'disabled';
        commitChange();
      });

    function setOverwriteValue(key, value) {
      const trimmed = value.trim();
      if (!trimmed) {
        delete overwrite[key];
      } else {
        overwrite[key] = value;
      }
      commitChange();
    }

    overrideFields.success.addEventListener('input', () => setOverwriteValue('success-message', overrideFields.success.value));
    overrideFields.already.addEventListener('input', () => setOverwriteValue('already-connected-message', overrideFields.already.value));
    overrideFields.progress.addEventListener('input', () => setOverwriteValue('connection-in-progress-message', overrideFields.progress.value));
    overrideFields.disconnected.addEventListener('input', () => setOverwriteValue('server-disconnected-message', overrideFields.disconnected.value));
    overrideFields.cancelled.addEventListener('input', () => setOverwriteValue('connection-cancelled-message', overrideFields.cancelled.value));

      removeBtn.addEventListener('click', () => {
        const lobbyName = lobby.name;
        config.lobbies.splice(index, 1);
        if (lobbyName) {
          const groups = Array.isArray(config['lobby-groups']) ? config['lobby-groups'] : [];
          groups.forEach((group) => {
            if (!Array.isArray(group.lobbies)) {
              return;
            }
            group.lobbies = group.lobbies.filter((name) => name !== lobbyName);
          });
          renderGroups(false);
        }
        renderLobbies();
        commitChange();
      });

      duplicateBtn.addEventListener('click', () => {
        const copy = clone(lobby);
        copy.name = copy.name ? `${copy.name}-copy` : `lobby-${config.lobbies.length + 1}`;
        config.lobbies.splice(index + 1, 0, copy);
        renderLobbies();
        commitChange();
      });

      lobbiesEl.appendChild(card);
    refreshPermissionPills();
    if (tutorialEl && !tutorialState.dismissed && !tutorialState.completed) {
      const step = tutorialSteps[tutorialState.stepIndex || 0];
      const stepId = step ? step.id : '';
      updateTutorialHighlights(stepId);
      scheduleTutorialPosition(stepId);
    }
  }

  function addGroup() {
    openGroupModal(null, null);
  }

  function addLobby() {
    const index = config.lobbies.length + 1;
    const name = `lobby-${index}`;
    config.lobbies.push({
      name,
      filter: `(?i)^${name}.*`,
      permission: '',
      priority: 0,
      parent: '',
      'parent-groups': [],
      'forced-hosts': [],
      commands: {
        [name]: { standalone: true, subcommand: true, 'hide-on': '^(?!.*).$' }
      },
      autojoin: true,
      'overwrite-messages': {}
    });
    renderLobbies();
    commitChange();
  }

  async function init() {
    try {
      yamlLib = await import('https://cdn.jsdelivr.net/npm/js-yaml@4.1.0/dist/js-yaml.mjs');
    } catch (error) {
      showStatus('Failed to load YAML parser. Check your connection.', 'bad');
      return;
    }
    const defaultYamlProxy = document.getElementById('cb-default-yaml')?.value?.trim() || '';
    const defaultYamlLobby = document.getElementById('cb-default-yaml-lobby')?.value?.trim() || '';
    defaultConfigProxy = defaultYamlProxy ? yamlLib.load(defaultYamlProxy) : {};
    defaultConfigLobby = defaultYamlLobby ? yamlLib.load(defaultYamlLobby) : {};
    defaultConfig = defaultConfigProxy;
    setConfigMode('proxy', { loadDefaults: true, force: true });

    defaultsBtn.addEventListener('click', () => applyLoadedConfig(defaultConfig, 'defaults', { mode: configMode }));
    resetBtn.addEventListener('click', () => resetToBaseline());
    modeToggle.addEventListener('change', () => {
      const nextMode = modeToggle.checked ? 'proxy' : 'lobby';
      setConfigMode(nextMode, { loadDefaults: true, force: true });
    });
  fileInput.addEventListener('change', async () => {
    const file = fileInput.files && fileInput.files[0];
    if (!file) {
      return;
    }
      try {
        const text = await file.text();
        const parsed = yamlLib.load(text);
        const detected = detectConfigMode(parsed);
        if (detected) {
          setConfigMode(detected, { force: true });
        }
        applyLoadedConfig(parsed, file.name, { mode: detected || configMode });
      } catch (error) {
      showStatus('Failed to parse YAML: ' + error.message, 'bad');
    }
  });

  if (dataFileInput && dataPaste && dataLoadBtn && dataClearBtn) {
    dataFileInput.addEventListener('change', async () => {
      const file = dataFileInput.files && dataFileInput.files[0];
      if (!file) {
        return;
      }
      try {
        const text = await file.text();
        loadDataDumpText(text);
      } catch (error) {
        setDataSummary('Failed to read data dump', 'bad');
      }
    });

    dataLoadBtn.addEventListener('click', () => {
      loadDataDumpText(dataPaste.value);
    });

    dataClearBtn.addEventListener('click', () => {
      dataPaste.value = '';
      clearDataDump();
    });
  }

  if (testUserSelect) {
    testUserSelect.addEventListener('change', () => {
      selectedUserId = testUserSelect.value;
      refreshPermissionPills();
    });
  }

    pasteLoadBtn.addEventListener('click', () => {
      const text = pasteArea.value.trim();
      if (!text) {
        showStatus('Paste YAML first', 'warn');
        return;
      }
      try {
        const parsed = yamlLib.load(text);
        const detected = detectConfigMode(parsed);
        if (detected) {
          setConfigMode(detected, { force: true });
        }
        applyLoadedConfig(parsed, 'pasted', { mode: detected || configMode });
      } catch (error) {
        showStatus('Failed to parse YAML: ' + error.message, 'bad');
      }
    });

    pasteClearBtn.addEventListener('click', () => {
      pasteArea.value = '';
      showStatus('Paste cleared', 'idle');
    });

    downloadBtn.addEventListener('click', () => {
      if (!currentDownloadUrl) {
        return;
      }
      const link = document.createElement('a');
      link.href = currentDownloadUrl;
      link.download = 'config.yml';
      link.click();
      showStatus('Downloaded config.yml', 'ok');
    });

    copyBtn.addEventListener('click', async () => {
      try {
        await navigator.clipboard.writeText(outputEl.textContent || '');
        showStatus('Copied YAML to clipboard', 'ok');
      } catch (error) {
        showStatus('Copy failed. Select and copy manually.', 'bad');
      }
    });

  const addGroupBtn = document.getElementById('cb-add-group');
  if (addGroupBtn) {
    addGroupBtn.addEventListener('click', addGroup);
  }
  document.getElementById('cb-add-lobby').addEventListener('click', addLobby);
  groupModalCreate.addEventListener('click', createGroupFromModal);
  groupModalCancel.addEventListener('click', closeGroupModal);
  groupModal.querySelectorAll('[data-action="close"]').forEach((btn) => {
    btn.addEventListener('click', closeGroupModal);
  });
  groupModalInput.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      createGroupFromModal();
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      closeGroupModal();
    }
  });

  if (tutorialStart && tutorialEl) {
    tutorialStart.addEventListener('click', () => {
      resetTutorialState();
      setTutorialStep(0);
    });
  }
  if (tutorialClose) {
    tutorialClose.addEventListener('click', () => {
      tutorialState.dismissed = true;
      saveTutorialState();
      renderTutorial();
    });
  }
  if (tutorialPrev) {
    tutorialPrev.addEventListener('click', () => {
      setTutorialStep((tutorialState.stepIndex || 0) - 1);
    });
  }
  if (tutorialNext) {
    tutorialNext.addEventListener('click', () => {
      const stepIndex = tutorialState.stepIndex || 0;
      const step = tutorialSteps[stepIndex];
      if (!step) {
        return;
      }
      if (stepIndex >= tutorialSteps.length - 1) {
        tutorialState.completed = true;
        saveTutorialState();
        renderTutorial();
        return;
      }
      setTutorialStep(stepIndex + 1);
    });
  }

  const handleTutorialReposition = () => {
    if (!tutorialEl || tutorialEl.hidden || tutorialState.dismissed || tutorialState.completed) {
      return;
    }
    const step = tutorialSteps[tutorialState.stepIndex || 0];
    scheduleTutorialPosition(step ? step.id : '');
  };
  window.addEventListener('resize', handleTutorialReposition);
  window.addEventListener('scroll', handleTutorialReposition, { passive: true });

    inputs.baseCommand.addEventListener('input', () => {
      config['base-hub-command'] = inputs.baseCommand.value.trim();
      markTutorialAction('coreEdited');
      commitChange();
    });
    inputs.aliases.addEventListener('input', () => {
      config.aliases = parseAliasList(inputs.aliases.value);
      markTutorialAction('coreEdited');
      commitChange();
    });
    inputs.aliases.addEventListener('blur', () => {
      inputs.aliases.value = aliasListToText(config.aliases);
    });
    if (forcedHostsAdd) {
      forcedHostsAdd.addEventListener('click', () => {
        forcedHostDrafts.push({ host: '', server: '' });
        renderForcedHosts();
      });
    }
    inputs.hideOn.addEventListener('input', () => {
      config['hide-hub-command-on-lobby'] = inputs.hideOn.value;
      updateHideRegexStatus();
      commitChange();
    });
    inputs.autoJoin.addEventListener('change', () => {
      config['auto-select'] = config['auto-select'] || {};
      config['auto-select']['on-join'] = inputs.autoJoin.checked;
      commitChange();
    });
    inputs.autoKick.addEventListener('change', () => {
      config['auto-select'] = config['auto-select'] || {};
      config['auto-select']['on-server-kick'] = inputs.autoKick.checked;
      commitChange();
    });
    inputs.lastLobby.addEventListener('change', () => {
      config['last-lobby'] = config['last-lobby'] || {};
      config['last-lobby'].enabled = inputs.lastLobby.checked;
      commitChange();
    });
    inputs.debugEnabled.addEventListener('change', () => {
      config.debug = config.debug || {};
      config.debug.enabled = inputs.debugEnabled.checked;
      setDebugCategoryVisibility(inputs.debugEnabled.checked);
      commitChange();
    });
    inputs.debugPermission.addEventListener('input', () => {
      config.debug = config.debug || {};
      config.debug.permission = inputs.debugPermission.value;
      commitChange();
    });
    Object.entries(debugCategoryInputs).forEach(([key, input]) => {
      if (!input) {
        return;
      }
      input.addEventListener('change', () => {
        config.debug = config.debug || {};
        config.debug.categories = normalizeDebugCategories(config.debug.categories);
        config.debug.categories[key] = input.checked;
        commitChange();
      });
    });
    inputs.messageSuccess.addEventListener('input', () => {
      config.messages = config.messages || {};
      config.messages['success-message'] = inputs.messageSuccess.value;
      markTutorialAction('messagesEdited');
      updateMessagePreview(inputs.messageSuccess);
      commitChange();
    });
    inputs.messageAlready.addEventListener('input', () => {
      config.messages = config.messages || {};
      config.messages['already-connected-message'] = inputs.messageAlready.value;
      markTutorialAction('messagesEdited');
      updateMessagePreview(inputs.messageAlready);
      commitChange();
    });
    inputs.messageProgress.addEventListener('input', () => {
      config.messages = config.messages || {};
      config.messages['connection-in-progress-message'] = inputs.messageProgress.value;
      markTutorialAction('messagesEdited');
      updateMessagePreview(inputs.messageProgress);
      commitChange();
    });
    inputs.messageDisconnected.addEventListener('input', () => {
      config.messages = config.messages || {};
      config.messages['server-disconnected-message'] = inputs.messageDisconnected.value;
      markTutorialAction('messagesEdited');
      updateMessagePreview(inputs.messageDisconnected);
      commitChange();
    });
    inputs.messageCancelled.addEventListener('input', () => {
      config.messages = config.messages || {};
      config.messages['connection-cancelled-message'] = inputs.messageCancelled.value;
      markTutorialAction('messagesEdited');
      updateMessagePreview(inputs.messageCancelled);
      commitChange();
    });
    inputs.systemPlayersOnly.addEventListener('input', () => {
      config['system-messages'] = config['system-messages'] || {};
      config['system-messages']['players-only-command-message'] = inputs.systemPlayersOnly.value;
      markTutorialAction('messagesEdited');
      updateMessagePreview(inputs.systemPlayersOnly);
      commitChange();
    });
    inputs.systemNoLobby.addEventListener('input', () => {
      config['system-messages'] = config['system-messages'] || {};
      config['system-messages']['no-lobby-found-message'] = inputs.systemNoLobby.value;
      markTutorialAction('messagesEdited');
      updateMessagePreview(inputs.systemNoLobby);
      commitChange();
    });
    inputs.kickEnabled.addEventListener('change', () => {
      config['kick-message'] = config['kick-message'] || {};
      config['kick-message'].enabled = inputs.kickEnabled.checked;
      markTutorialAction('messagesEdited');
      commitChange();
    });
    inputs.kickPrefix.addEventListener('input', () => {
      config['kick-message'] = config['kick-message'] || {};
      config['kick-message'].prefix = inputs.kickPrefix.value;
      markTutorialAction('messagesEdited');
      commitChange();
    });
    inputs.kickSuffix.addEventListener('input', () => {
      config['kick-message'] = config['kick-message'] || {};
      config['kick-message'].suffix = inputs.kickSuffix.value;
      markTutorialAction('messagesEdited');
      commitChange();
    });
    if (inputs.i18nDefaultLocale) {
      inputs.i18nDefaultLocale.addEventListener('input', () => {
        config.i18n = config.i18n || {};
        config.i18n['default-locale'] = inputs.i18nDefaultLocale.value.trim() || 'en_us';
        commitChange();
      });
    }
    if (inputs.i18nUseClient) {
      inputs.i18nUseClient.addEventListener('change', () => {
        config.i18n = config.i18n || {};
        config.i18n['use-client-locale'] = inputs.i18nUseClient.checked;
        commitChange();
      });
    }
    if (i18nAddLocale) {
      i18nAddLocale.addEventListener('click', () => {
        i18nOverrideDrafts.push({ locale: nextI18nLocaleName(), entries: [] });
        syncI18nOverridesConfig();
        renderI18nOverrides();
        commitChange();
      });
    }

    inputs.finderStart.addEventListener('input', () => {
      config.finder = config.finder || {};
      const value = Number.parseInt(inputs.finderStart.value, 10);
      config.finder['start-duration'] = Number.isFinite(value) ? value : 0;
      markTutorialAction('finderEdited');
      commitChange();
    });
    inputs.finderIncrement.addEventListener('input', () => {
      config.finder = config.finder || {};
      const value = Number.parseInt(inputs.finderIncrement.value, 10);
      config.finder['increment-duration'] = Number.isFinite(value) ? value : 0;
      markTutorialAction('finderEdited');
      commitChange();
    });
    inputs.finderMax.addEventListener('input', () => {
      config.finder = config.finder || {};
      const value = Number.parseInt(inputs.finderMax.value, 10);
      config.finder['max-duration'] = Number.isFinite(value) ? value : 0;
      markTutorialAction('finderEdited');
      commitChange();
    });
    inputs.finderRefresh.addEventListener('input', () => {
      config.finder = config.finder || {};
      const value = Number.parseInt(inputs.finderRefresh.value, 10);
      config.finder['refresh-interval-in-ticks'] = Number.isFinite(value) ? value : 0;
      markTutorialAction('finderEdited');
      commitChange();
    });
    inputs.dataCollectEnabled.addEventListener('change', () => {
      config['data-collection'] = config['data-collection'] || {};
      config['data-collection'].enabled = inputs.dataCollectEnabled.checked;
      markTutorialAction('finderEdited');
      commitChange();
    });
    inputs.dataCollectUuid.addEventListener('change', () => {
      config['data-collection'] = config['data-collection'] || {};
      config['data-collection']['include-uuid'] = inputs.dataCollectUuid.checked;
      markTutorialAction('finderEdited');
      commitChange();
    });
    inputs.dataCollectFile.addEventListener('input', () => {
      config['data-collection'] = config['data-collection'] || {};
      config['data-collection']['dump-file'] = inputs.dataCollectFile.value.trim();
      markTutorialAction('finderEdited');
      commitChange();
    });
    inputs.dataCollectInterval.addEventListener('input', () => {
      config['data-collection'] = config['data-collection'] || {};
      const value = Number.parseInt(inputs.dataCollectInterval.value, 10);
      config['data-collection']['dump-interval-minutes'] = Number.isFinite(value) ? value : 0;
      markTutorialAction('finderEdited');
      commitChange();
    });
    inputs.dataCollectUsers.addEventListener('input', () => {
      config['data-collection'] = config['data-collection'] || {};
      const value = Number.parseInt(inputs.dataCollectUsers.value, 10);
      config['data-collection']['max-users'] = Number.isFinite(value) ? value : 0;
      markTutorialAction('finderEdited');
      commitChange();
    });
    inputs.dataCollectServers.addEventListener('input', () => {
      config['data-collection'] = config['data-collection'] || {};
      const value = Number.parseInt(inputs.dataCollectServers.value, 10);
      config['data-collection']['max-servers'] = Number.isFinite(value) ? value : 0;
      markTutorialAction('finderEdited');
      commitChange();
    });

    if (lobbyInputs.compassEnabled) {
      lobbyInputs.compassEnabled.addEventListener('change', withLobbyMode(() => {
        config.compass = config.compass || {};
        config.compass.enabled = lobbyInputs.compassEnabled.checked;
        commitChange();
      }));
    }
    if (lobbyInputs.compassTitle) {
      lobbyInputs.compassTitle.addEventListener('input', withLobbyMode(() => {
        config.compass = config.compass || {};
        config.compass['gui-title'] = lobbyInputs.compassTitle.value;
        commitChange();
      }));
    }
    if (lobbyInputs.navigatorEnabled) {
      lobbyInputs.navigatorEnabled.addEventListener('change', withLobbyMode(() => {
        config.navigator = config.navigator || {};
        config.navigator.enabled = lobbyInputs.navigatorEnabled.checked;
        commitChange();
      }));
    }
    if (lobbyInputs.navigatorTitle) {
      lobbyInputs.navigatorTitle.addEventListener('input', withLobbyMode(() => {
        config.navigator = config.navigator || {};
        config.navigator['gui-title'] = lobbyInputs.navigatorTitle.value;
        commitChange();
      }));
    }
    if (lobbyInputs.spawnTeleportEnabled) {
      lobbyInputs.spawnTeleportEnabled.addEventListener('change', withLobbyMode(() => {
        config['spawn-teleport'] = config['spawn-teleport'] || {};
        config['spawn-teleport'].enabled = lobbyInputs.spawnTeleportEnabled.checked;
        if (lobbyInputs.joinTeleportMode && lobbyInputs.spawnTeleportEnabled.checked) {
          const current = lobbyInputs.joinTeleportMode.value || 'none';
          if (current === 'none') {
            lobbyInputs.joinTeleportMode.value = 'spawn';
            config['join-teleport'] = config['join-teleport'] || {};
            config['join-teleport'].mode = 'spawn';
          }
        }
        commitChange();
      }));
    }
    if (lobbyInputs.joinTeleportMode) {
      lobbyInputs.joinTeleportMode.addEventListener('change', withLobbyMode(() => {
        config['join-teleport'] = config['join-teleport'] || {};
        config['join-teleport'].mode = lobbyInputs.joinTeleportMode.value || 'none';
        commitChange();
      }));
    }
    if (lobbyInputs.joinTeleportDelay) {
      lobbyInputs.joinTeleportDelay.addEventListener('input', withLobbyMode(() => {
        config['join-teleport'] = config['join-teleport'] || {};
        const value = Number.parseInt(lobbyInputs.joinTeleportDelay.value, 10);
        config['join-teleport']['delay-seconds'] = Number.isFinite(value) ? value : 0;
        commitChange();
      }));
    }
    if (lobbyInputs.joinTeleportMaxAge) {
      lobbyInputs.joinTeleportMaxAge.addEventListener('input', withLobbyMode(() => {
        config['join-teleport'] = config['join-teleport'] || {};
        config['join-teleport']['last-location'] = config['join-teleport']['last-location'] || {};
        const value = Number.parseInt(lobbyInputs.joinTeleportMaxAge.value, 10);
        config['join-teleport']['last-location']['max-age-seconds'] = Number.isFinite(value) ? value : 0;
        commitChange();
      }));
    }
    if (lobbyInputs.npcEnabled) {
      lobbyInputs.npcEnabled.addEventListener('change', withLobbyMode(() => {
        config['lobby-npcs'] = config['lobby-npcs'] || {};
        config['lobby-npcs'].enabled = lobbyInputs.npcEnabled.checked;
        commitChange();
      }));
    }
    if (lobbyInputs.signEnabled) {
      lobbyInputs.signEnabled.addEventListener('change', withLobbyMode(() => {
        config['lobby-signs'] = config['lobby-signs'] || {};
        config['lobby-signs'].enabled = lobbyInputs.signEnabled.checked;
        commitChange();
      }));
    }
    if (lobbyInputs.gameplayDamage) {
      lobbyInputs.gameplayDamage.addEventListener('change', withLobbyMode(() => {
        config.gameplay = config.gameplay || {};
        config.gameplay['disable-damage'] = lobbyInputs.gameplayDamage.checked;
        commitChange();
      }));
    }
    if (lobbyInputs.gameplayHunger) {
      lobbyInputs.gameplayHunger.addEventListener('change', withLobbyMode(() => {
        config.gameplay = config.gameplay || {};
        config.gameplay['disable-hunger'] = lobbyInputs.gameplayHunger.checked;
        commitChange();
      }));
    }
    if (lobbyInputs.gameplayHeal) {
      lobbyInputs.gameplayHeal.addEventListener('change', withLobbyMode(() => {
        config.gameplay = config.gameplay || {};
        config.gameplay['heal-on-join'] = lobbyInputs.gameplayHeal.checked;
        commitChange();
      }));
    }
    if (lobbyInputs.bstatsEnabled) {
      lobbyInputs.bstatsEnabled.addEventListener('change', withLobbyMode(() => {
        config.bstats = config.bstats || {};
        config.bstats.enabled = lobbyInputs.bstatsEnabled.checked;
        commitChange();
      }));
    }
    if (lobbyInputs.minestomSecret) {
      lobbyInputs.minestomSecret.addEventListener('input', withLobbyMode(() => {
        config.minestom = config.minestom || {};
        config.minestom['velocity-secret'] = lobbyInputs.minestomSecret.value.trim();
        commitChange();
      }));
    }

  regexInput.addEventListener('input', () => {
    updateRegexResult(regexInput.value, regexTest.value, regexResult);
  });
  regexTest.addEventListener('input', () => {
    regexTestCache[regexContext] = regexTest.value;
    updateRegexResult(regexInput.value, regexTest.value, regexResult);
  });

  const regexStorageKey = 'hub-regex-lab-collapsed';
  const storedRegexState = window.localStorage.getItem(regexStorageKey);
  if (storedRegexState === 'true') {
    regexLab.classList.add('is-collapsed');
    regexToggle.setAttribute('aria-expanded', 'false');
    regexToggle.setAttribute('aria-label', 'Expand regex lab');
  }

  regexToggle.addEventListener('click', () => {
    const isCollapsed = regexLab.classList.toggle('is-collapsed');
    regexToggle.setAttribute('aria-expanded', isCollapsed ? 'false' : 'true');
    regexToggle.setAttribute('aria-label', isCollapsed ? 'Expand regex lab' : 'Minimize regex lab');
    window.localStorage.setItem(regexStorageKey, isCollapsed ? 'true' : 'false');
  });

  clearDataDump();
  enhanceHints();
  initMessagePreviews();
  updateAllMessagePreviews();
  if (tutorialEl) {
    if (!tutorialState.dismissed && !tutorialState.completed) {
      setTutorialStep(tutorialState.stepIndex || 0);
    } else {
      renderTutorial();
    }
  }
}

  init();
});
</script>
