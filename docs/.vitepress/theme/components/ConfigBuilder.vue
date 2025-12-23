<template>
  <div class="config-builder">
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
          <div class="cb-hero-actions">
            <button type="button" class="cb-btn cb-btn-primary" id="cb-defaults">Use defaults</button>
            <button type="button" class="cb-btn cb-btn-ghost" id="cb-reset">Reset edits</button>
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
        <div class="cb-divider">Analysis data</div>
        <label class="cb-file">
          <span title="Upload a runtime data dump (YAML/JSON) for autocomplete and testing">Upload data dump</span>
          <input type="file" id="cb-data-file" accept=".yml,.yaml,.json">
        </label>
        <label class="cb-field cb-paste">
          <span title="Paste a runtime data dump to load users and servers">Paste data dump (YAML/JSON)</span>
          <textarea id="cb-data-paste" rows="4" placeholder="Paste data dump here"></textarea>
        </label>
        <div class="cb-paste-actions">
          <button type="button" class="cb-btn cb-btn-ghost" id="cb-data-load">Load data dump</button>
          <button type="button" class="cb-btn cb-btn-ghost" id="cb-data-clear">Clear data</button>
        </div>
        <div class="cb-pill cb-data-summary-inline" id="cb-data-summary-inline" data-state="idle">No data loaded</div>
        <p class="cb-note">Tip: Use (?i) at the start for case-insensitive filters.</p>
      </div>
    </header>

    <div class="cb-layout">
      <div class="cb-main">
        <section class="cb-card" style="--cb-delay: 0.05s;">
          <div class="cb-card-head">
            <h2>Core settings</h2>
            <p>Commands, routing, and debug toggles.</p>
          </div>
          <div class="cb-grid-2">
            <label class="cb-field">
              <span title="Primary command players use to open the hub">Base command</span>
              <input id="cb-base-command" type="text" placeholder="hub">
              <small>Players use /hub to open the selector.</small>
            </label>
            <label class="cb-field">
              <span title="Extra commands that also open the hub">Aliases (comma separated)</span>
              <input id="cb-aliases" type="text" placeholder="lobby, leave">
            </label>
            <label class="cb-field cb-span-2">
              <span title="Regex for server names where /hub should be hidden">Hide base command on lobby (regex)</span>
              <input id="cb-hide-on" type="text" placeholder="^(?!.*).$">
              <small>Matches the current server name. If it matches, /hub is hidden.</small>
            </label>
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
            <label class="cb-field">
              <span title="Enable /hub debug commands">Debug enabled</span>
              <input id="cb-debug-enabled" type="checkbox">
            </label>
            <label class="cb-field">
              <span title="Permission required to use /hub debug">Debug permission</span>
              <input id="cb-debug-permission" type="text" placeholder="hub.debug">
            </label>
          </div>
          <div class="cb-inline-test">
            <label class="cb-field">
              <span title="Test if the hide regex matches a server name">Test hide regex</span>
              <input id="cb-hide-test" type="text" placeholder="lobby-1" list="cb-server-suggestions">
            </label>
            <div class="cb-pill" id="cb-hide-result" data-state="idle">Waiting for input</div>
          </div>
          <details class="cb-details">
            <summary>Optional messages</summary>
            <div class="cb-grid-2 cb-details-grid">
              <label class="cb-field cb-span-2">
                <span title="Message shown after successful hub transfer">Success message</span>
                <textarea id="cb-message-success" rows="2"></textarea>
              </label>
              <label class="cb-field cb-span-2">
                <span title="Message shown when already in a hub">Already connected message</span>
                <textarea id="cb-message-already" rows="2"></textarea>
              </label>
              <label class="cb-field cb-span-2">
                <span title="Message shown while connecting to a hub">Connection in progress message</span>
                <textarea id="cb-message-progress" rows="2"></textarea>
              </label>
              <label class="cb-field cb-span-2">
                <span title="Message shown if the target lobby server is offline">Server disconnected message</span>
                <textarea id="cb-message-disconnected" rows="2"></textarea>
              </label>
              <label class="cb-field cb-span-2">
                <span title="Message shown when a transfer is cancelled">Connection cancelled message</span>
                <textarea id="cb-message-cancelled" rows="2"></textarea>
              </label>
              <label class="cb-field cb-span-2">
                <span title="Message when console runs a player-only command">Players only command message</span>
                <textarea id="cb-system-players-only" rows="2"></textarea>
              </label>
              <label class="cb-field cb-span-2">
                <span title="Message when no lobby server can be found">No lobby found message</span>
                <textarea id="cb-system-no-lobby" rows="2"></textarea>
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
          </details>
        </section>

        <section class="cb-card" style="--cb-delay: 0.07s;">
          <div class="cb-card-head">
            <h2>Finder + Data collection</h2>
            <p>Configure search timings and runtime data dumps.</p>
          </div>
          <div class="cb-grid-2">
            <label class="cb-field">
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
            <label class="cb-field">
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

        <section class="cb-card" style="--cb-delay: 0.1s;">
          <div class="cb-card-head">
            <h2>Lobby groups</h2>
            <p>Group lobbies for fallbacks. Optional.</p>
          </div>
          <div id="cb-groups" class="cb-list"></div>
          <button type="button" class="cb-btn cb-btn-ghost" id="cb-add-group">Add group</button>
        </section>

        <section class="cb-card cb-lobbies" style="--cb-delay: 0.15s;">
          <div class="cb-card-head">
            <h2>Lobbies</h2>
            <p>Add each lobby and its regex filter.</p>
          </div>
          <div id="cb-lobbies" class="cb-list"></div>
          <button type="button" class="cb-btn cb-btn-primary" id="cb-add-lobby">Add lobby</button>
          <p class="cb-footnote">Each lobby can expose a direct command. Extra commands in uploads are kept as-is.</p>
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
          <span title="Regex pattern to test">Regex</span>
          <input id="cb-regex-input" type="text" placeholder="(?i)^lobby.*">
        </label>
        <label class="cb-field">
          <span title="Server name to test against the regex">Test string</span>
          <input id="cb-regex-test" type="text" placeholder="lobby-eu-1" list="cb-server-suggestions">
        </label>
        </div>
        <div class="cb-inline-test">
          <div class="cb-pill" id="cb-regex-result" data-state="idle">Waiting for input</div>
          <p class="cb-note">Checker runs in the browser. It supports (?i) at the start for case-insensitive matches.</p>
        </div>
      </div>
        </section>

        <section class="cb-card cb-summary" style="--cb-delay: 0.22s;">
          <div class="cb-card-head">
            <h2>Quick summary</h2>
            <p>Compact overview before download.</p>
          </div>
          <div class="cb-summary-grid" id="cb-summary"></div>
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

      <aside class="cb-side">
        <section class="cb-card cb-activity" style="--cb-delay: 0.12s;">
          <div class="cb-card-head">
            <h2>Live activity</h2>
            <p>Simulated timeline based on the latest data dump.</p>
          </div>
          <div class="cb-activity-head">
            <div class="cb-activity-status" data-state="live">
              <span class="cb-live-dot" aria-hidden="true"></span>
              <span class="cb-activity-label">Live</span>
            </div>
            <div class="cb-activity-controls">
              <button type="button" class="cb-btn cb-btn-ghost" id="cb-activity-toggle">Pause</button>
              <button type="button" class="cb-btn cb-btn-ghost" id="cb-activity-reset">Reset</button>
            </div>
          </div>
          <div class="cb-activity-grid">
            <div class="cb-zone-map">
            <div class="cb-zone-map-head">
              <span>Server tree</span>
              <span class="cb-zone-map-hint">Active server highlighted</span>
            </div>
              <div class="cb-zones" id="cb-zones"></div>
            </div>
            <div class="cb-activity-history">
              <div class="cb-activity-empty" id="cb-activity-empty">Waiting for activity...</div>
              <div class="cb-activity-list" id="cb-activity-list" aria-live="polite"></div>
            </div>
          </div>
          <div class="cb-offline-inline">
            <div class="cb-card-head cb-offline-head">
              <h3>Offline players</h3>
              <p>Simulate a full journey: join, lobby pick, game, kick, return, switch, leave.</p>
            </div>
            <div class="cb-offline-list" id="cb-offline-list"></div>
          </div>
        </section>
        <section class="cb-card" style="--cb-delay: 0.16s;">
          <div class="cb-card-head">
            <h2>Analysis data</h2>
            <p>Load a runtime dump to autocomplete server names and test permissions.</p>
          </div>
          <div class="cb-inline-test">
            <label class="cb-field">
              <span title="Select a user to preview permissions against lobbies">Test user</span>
              <select id="cb-test-user"></select>
            </label>
            <div class="cb-pill" id="cb-data-summary" data-state="idle">No data loaded</div>
          </div>
        </section>
      </aside>
    </div>
  </div>
  </div>

  <textarea id="cb-default-yaml" hidden>
# Thanks <3

debug:
  enabled: true
  permission: hub.debug
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
lobby-groups:
- name: main
  lobbies:
  - lobby
  - teamlobby
  - premiumlobby
- name: minigame
  lobbies:
  - ffa-lobby
lobbies:
- name: teamlobby
  filter: (?i)^teamlobby.*
  permission: hub.team
  priority: 2
  parent: ''
  parent-groups:
  - lobby
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
  <datalist id="cb-server-suggestions"></datalist>
</template>

<script setup>
import { onMounted } from 'vue';

onMounted(() => {
  const statusEl = document.getElementById('cb-status');
  const sourceEl = document.getElementById('cb-source');
  const updatedEl = document.getElementById('cb-updated');
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
  const lobbiesEl = document.getElementById('cb-lobbies');
  const regexInput = document.getElementById('cb-regex-input');
  const regexTest = document.getElementById('cb-regex-test');
  const regexResult = document.getElementById('cb-regex-result');
  const regexLab = document.querySelector('.cb-regex-lab');
  const regexToggle = document.getElementById('cb-regex-toggle');
  const hideTest = document.getElementById('cb-hide-test');
  const hideResult = document.getElementById('cb-hide-result');
  const summaryEl = document.getElementById('cb-summary');
  const activityList = document.getElementById('cb-activity-list');
  const activityEmpty = document.getElementById('cb-activity-empty');
  const activityToggle = document.getElementById('cb-activity-toggle');
  const activityReset = document.getElementById('cb-activity-reset');
  const activityStatus = document.querySelector('.cb-activity-status');
  const activityLabel = document.querySelector('.cb-activity-label');
  const zonesEl = document.getElementById('cb-zones');
  const offlineList = document.getElementById('cb-offline-list');

  if (!statusEl || !sourceEl || !updatedEl || !outputEl || !fileInput || !defaultsBtn || !resetBtn || !downloadBtn
    || !copyBtn || !pasteArea || !pasteLoadBtn || !pasteClearBtn || !dataFileInput || !dataPaste || !dataLoadBtn
    || !dataClearBtn || !dataSummary || !testUserSelect || !serverSuggestions || !groupsEl || !lobbiesEl
    || !regexLab || !regexToggle
    || !regexInput || !regexTest || !regexResult || !hideTest || !hideResult || !summaryEl
    || !activityList || !activityEmpty || !activityToggle || !activityReset || !activityStatus || !activityLabel || !zonesEl || !offlineList) {
    return;
  }

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
    finderStart: document.getElementById('cb-finder-start'),
    finderIncrement: document.getElementById('cb-finder-increment'),
    finderMax: document.getElementById('cb-finder-max'),
    finderRefresh: document.getElementById('cb-finder-refresh'),
    dataCollectEnabled: document.getElementById('cb-data-collect-enabled'),
    dataCollectUuid: document.getElementById('cb-data-collect-uuid'),
    dataCollectFile: document.getElementById('cb-data-collect-file'),
    dataCollectInterval: document.getElementById('cb-data-collect-interval'),
    dataCollectUsers: document.getElementById('cb-data-collect-users'),
    dataCollectServers: document.getElementById('cb-data-collect-servers')
  };

  let yamlLib;
  let config = {};
  let baselineConfig = {};
  let defaultConfig = {};
  let currentDownloadUrl = null;
  let analysisData = { servers: [], users: [] };
  let selectedUserId = '';
  let activeLobby = null;
  let userIndex = new Map();
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

  function enhanceHints(root) {
    const scope = root || document;
    const hintTargets = scope.querySelectorAll('label.cb-field > span[title], label.cb-file > span[title]');
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
    dataSummary.textContent = text;
    dataSummary.dataset.state = state || 'idle';
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
    testUserSelect.innerHTML = '';
    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = 'Select user...';
    testUserSelect.appendChild(placeholder);
    userIndex = new Map();
    analysisData.users.forEach((user) => {
      userIndex.set(user.id, user);
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
  }

  function applyDataDump(data) {
    analysisData = data;
    updateServerSuggestions();
    populateUserSelect();
    setDataSummary(`Loaded ${analysisData.servers.length} servers, ${analysisData.users.length} users`, 'ok');
    refreshPermissionPills();
    resetActivityFeed();
    renderZones();
    renderOfflinePlayers();
  }

  function clearDataDump() {
    analysisData = { servers: [], users: [] };
    userIndex = new Map();
    selectedUserId = '';
    updateServerSuggestions();
    populateUserSelect();
    setDataSummary('No data loaded', 'idle');
    refreshPermissionPills();
    resetActivityFeed();
    renderZones();
    renderOfflinePlayers();
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

  function normalizeConfig(raw) {
    let next = raw && typeof raw === 'object' ? raw : {};
    next = mergeDefaults(next, defaultConfig);
    next.aliases = Array.isArray(next.aliases) ? next.aliases : [];
    next['auto-select'] = next['auto-select'] && typeof next['auto-select'] === 'object' ? next['auto-select'] : {};
    next['last-lobby'] = next['last-lobby'] && typeof next['last-lobby'] === 'object' ? next['last-lobby'] : {};
    next.debug = next.debug && typeof next.debug === 'object' ? next.debug : {};
    next.messages = next.messages && typeof next.messages === 'object' ? next.messages : {};
    next['system-messages'] = next['system-messages'] && typeof next['system-messages'] === 'object' ? next['system-messages'] : {};
    next['kick-message'] = next['kick-message'] && typeof next['kick-message'] === 'object' ? next['kick-message'] : {};
    next.finder = next.finder && typeof next.finder === 'object' ? next.finder : {};
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
      commands: {},
      autojoin: true,
      'overwrite-messages': {}
    };

    next['lobby-groups'] = next['lobby-groups'].map((group) => {
      const normalized = group && typeof group === 'object' ? group : {};
      const merged = mergeDefaults(normalized, { name: '', lobbies: [] });
      merged.lobbies = Array.isArray(merged.lobbies) ? merged.lobbies : [];
      return merged;
    });

    next.lobbies = next.lobbies.map((lobby) => {
      const normalized = lobby && typeof lobby === 'object' ? lobby : {};
      const merged = mergeDefaults(normalized, lobbyDefaults);
      merged['parent-groups'] = Array.isArray(merged['parent-groups']) ? merged['parent-groups'] : [];
      merged.commands = merged.commands && typeof merged.commands === 'object' ? merged.commands : {};
      merged['overwrite-messages'] = merged['overwrite-messages'] && typeof merged['overwrite-messages'] === 'object' ? merged['overwrite-messages'] : {};
      merged.priority = Number.isFinite(Number(merged.priority)) ? Number(merged.priority) : 0;
      return merged;
    });

    return next;
  }

  function parseList(value) {
    return value
      .split(',')
      .map((entry) => entry.trim())
      .filter(Boolean);
  }

  function listToText(list) {
    return Array.isArray(list) ? list.join(', ') : '';
  }

  function applyConfigToForm() {
    inputs.baseCommand.value = config['base-hub-command'] || '';
    inputs.aliases.value = listToText(config.aliases);
    inputs.hideOn.value = config['hide-hub-command-on-lobby'] || '';
    inputs.autoJoin.checked = Boolean(config['auto-select']?.['on-join']);
    inputs.autoKick.checked = Boolean(config['auto-select']?.['on-server-kick']);
    inputs.lastLobby.checked = Boolean(config['last-lobby']?.enabled);
    inputs.debugEnabled.checked = Boolean(config.debug?.enabled);
    inputs.debugPermission.value = config.debug?.permission || '';
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
    inputs.dataCollectEnabled.checked = Boolean(config['data-collection']?.enabled);
    inputs.dataCollectUuid.checked = Boolean(config['data-collection']?.['include-uuid']);
    inputs.dataCollectFile.value = config['data-collection']?.['dump-file'] || '';
    inputs.dataCollectInterval.value = config['data-collection']?.['dump-interval-minutes'] ?? '';
    inputs.dataCollectUsers.value = config['data-collection']?.['max-users'] ?? '';
    inputs.dataCollectServers.value = config['data-collection']?.['max-servers'] ?? '';
    updateHideRegexStatus();
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

  function applyLoadedConfig(raw, sourceLabel) {
    config = normalizeConfig(raw);
    baselineConfig = clone(config);
    applyConfigToForm();
    renderGroups();
    renderLobbies();
    updateOutput();
    setSource(sourceLabel);
    showStatus('Loaded ' + sourceLabel, 'ok');
    refreshPermissionPills();
    updateSummary();
  }

  function resetToBaseline() {
    config = clone(baselineConfig);
    applyConfigToForm();
    renderGroups();
    renderLobbies();
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
    if (!lobby || !regexInput || !regexTest || !regexResult) {
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
      resultEl.textContent = 'Enter a regex';
      resultEl.dataset.state = 'idle';
      return;
    }
    let regex;
    try {
      regex = buildRegex(cleanedPattern);
    } catch (error) {
      resultEl.textContent = 'Invalid regex';
      resultEl.dataset.state = 'bad';
      return;
    }
    if (!value.trim()) {
      resultEl.textContent = 'Enter a test string';
      resultEl.dataset.state = 'idle';
      return;
    }
    const match = regex ? regex.test(value) : false;
    resultEl.textContent = match ? 'Match' : 'No match';
    resultEl.dataset.state = match ? 'ok' : 'bad';
  }

  function updateHideRegexStatus() {
    updateRegexResult(inputs.hideOn.value, hideTest.value, hideResult);
  }

  function renderGroups() {
    groupsEl.innerHTML = '';
    config['lobby-groups'].forEach((group, index) => {
      const card = document.createElement('div');
      card.className = 'cb-mini-card';
      card.innerHTML = `
        <div class="cb-mini-head">
          <h3>Group ${index + 1}</h3>
          <button type="button" class="cb-btn cb-btn-danger cb-icon-btn" data-action="remove" aria-label="Remove group" title="Remove group">
            <svg class="cb-trash-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4 7h16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <path d="M9 7V5h6v2" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <path d="M7 7l1 12h8l1-12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <path d="M10 11v6M14 11v6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
        <div class="cb-grid-2">
          <label class="cb-field">
            <span title="Name of the lobby group">Name</span>
            <input type="text" data-field="name">
          </label>
          <label class="cb-field">
            <span title="Lobby names that belong to this group">Lobbies (comma separated)</span>
            <input type="text" data-field="lobbies">
          </label>
        </div>
      `;
      const nameInput = card.querySelector('[data-field="name"]');
      const lobbiesInput = card.querySelector('[data-field="lobbies"]');
      const removeBtn = card.querySelector('[data-action="remove"]');
      nameInput.value = group.name || '';
      lobbiesInput.value = listToText(group.lobbies);
      enhanceHints(card);
      nameInput.addEventListener('input', () => {
        group.name = nameInput.value.trim();
        commitChange();
      });
      lobbiesInput.addEventListener('input', () => {
        group.lobbies = parseList(lobbiesInput.value);
        commitChange();
      });
      removeBtn.addEventListener('click', () => {
        config['lobby-groups'].splice(index, 1);
        renderGroups();
        commitChange();
      });
      groupsEl.appendChild(card);
    });
  }

  function getPrimaryCommand(lobby) {
    const keys = lobby.commands ? Object.keys(lobby.commands) : [];
    if (keys.length === 0) {
      return { key: '', value: null };
    }
    return { key: keys[0], value: lobby.commands[keys[0]] };
  }

  function renderLobbies() {
    lobbiesEl.innerHTML = '';
    if (activeLobby && !config.lobbies.includes(activeLobby)) {
      activeLobby = null;
    }
    config.lobbies.forEach((lobby, index) => {
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
        <div class="cb-grid-2">
          <label class="cb-field">
            <span title="Internal lobby name">Name</span>
            <input type="text" data-field="name">
          </label>
          <label class="cb-field">
            <span title="Regex that matches server names for this lobby">Filter regex</span>
            <input type="text" data-field="filter">
          </label>
          <label class="cb-field">
            <span title="Permission required to access this lobby">Permission</span>
            <input type="text" data-field="permission">
          </label>
          <label class="cb-field">
            <span title="Higher priority lobbies are preferred">Priority</span>
            <input type="number" data-field="priority" min="-10" max="100" step="1">
          </label>
          <label class="cb-field">
            <span title="Fallback parent lobby name">Parent lobby</span>
            <input type="text" data-field="parent">
          </label>
          <label class="cb-field">
            <span title="Fallback parent groups for this lobby">Parent groups (comma separated)</span>
            <input type="text" data-field="parentGroups">
          </label>
          <label class="cb-field">
            <span title="Allow autojoin to this lobby">Autojoin flag</span>
            <input type="checkbox" data-field="autojoin">
          </label>
        </div>
        <details class="cb-details cb-command-section">
          <summary>Commands</summary>
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
        </details>
      <div class="cb-inline-test cb-lobby-perm">
        <div class="cb-pill cb-perm-pill" data-role="perm-result" data-index="${index}" data-state="idle">Select user</div>
      </div>
      <p class="cb-note">First command is editable here. Extra commands stay untouched.</p>
      <details class="cb-details cb-override">
        <summary>Override messages</summary>
        <div class="cb-grid-2 cb-details-grid">
          <label class="cb-field cb-span-2">
            <span title="Override for success message (this lobby only)">Success message</span>
            <textarea rows="2" data-field="ovSuccess"></textarea>
          </label>
          <label class="cb-field cb-span-2">
            <span title="Override for already connected message">Already connected message</span>
            <textarea rows="2" data-field="ovAlready"></textarea>
          </label>
          <label class="cb-field cb-span-2">
            <span title="Override for connection in progress message">Connection in progress message</span>
            <textarea rows="2" data-field="ovProgress"></textarea>
          </label>
          <label class="cb-field cb-span-2">
            <span title="Override for server disconnected message">Server disconnected message</span>
            <textarea rows="2" data-field="ovDisconnected"></textarea>
          </label>
          <label class="cb-field cb-span-2">
            <span title="Override for connection cancelled message">Connection cancelled message</span>
            <textarea rows="2" data-field="ovCancelled"></textarea>
          </label>
        </div>
        <p class="cb-footnote">Leave empty to inherit the global message.</p>
      </details>
    `;

      const title = card.querySelector('[data-role="title"]');
      const removeBtn = card.querySelector('[data-action="remove"]');
      const duplicateBtn = card.querySelector('[data-action="duplicate"]');
      enhanceHints(card);

      const fields = {
        name: card.querySelector('[data-field="name"]'),
        filter: card.querySelector('[data-field="filter"]'),
        permission: card.querySelector('[data-field="permission"]'),
        priority: card.querySelector('[data-field="priority"]'),
        parent: card.querySelector('[data-field="parent"]'),
        parentGroups: card.querySelector('[data-field="parentGroups"]'),
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

      fields.name.value = lobby.name || '';
      fields.filter.value = lobby.filter || '';
      fields.permission.value = lobby.permission || '';
      fields.priority.value = Number.isFinite(Number(lobby.priority)) ? lobby.priority : 0;
      fields.parent.value = lobby.parent || '';
      fields.parentGroups.value = listToText(lobby['parent-groups']);
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

    fields.name.addEventListener('input', () => {
        lobby.name = fields.name.value.trim();
        title.textContent = lobby.name ? lobby.name : `Lobby ${index + 1}`;
        commitChange();
      });

      fields.filter.addEventListener('input', () => {
        lobby.filter = fields.filter.value;
        if (activeLobby === lobby) {
          syncRegexLabWithLobby(lobby, { keepTest: true });
        }
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

      fields.parent.addEventListener('input', () => {
        lobby.parent = fields.parent.value;
        commitChange();
      });

      fields.parentGroups.addEventListener('input', () => {
        lobby['parent-groups'] = parseList(fields.parentGroups.value);
        commitChange();
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
        config.lobbies.splice(index, 1);
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
    });
    refreshPermissionPills();
  }

  function addGroup() {
    config['lobby-groups'].push({ name: `group-${config['lobby-groups'].length + 1}`, lobbies: [] });
    renderGroups();
    commitChange();
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
    const defaultYaml = document.getElementById('cb-default-yaml')?.value?.trim() || '';
    defaultConfig = defaultYaml ? yamlLib.load(defaultYaml) : {};
    applyLoadedConfig(defaultConfig, 'defaults');

    defaultsBtn.addEventListener('click', () => applyLoadedConfig(defaultConfig, 'defaults'));
    resetBtn.addEventListener('click', () => resetToBaseline());
  fileInput.addEventListener('change', async () => {
    const file = fileInput.files && fileInput.files[0];
    if (!file) {
      return;
    }
      try {
        const text = await file.text();
        const parsed = yamlLib.load(text);
        applyLoadedConfig(parsed, file.name);
      } catch (error) {
      showStatus('Failed to parse YAML: ' + error.message, 'bad');
    }
  });

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

  testUserSelect.addEventListener('change', () => {
    selectedUserId = testUserSelect.value;
    refreshPermissionPills();
  });

  activityToggle.addEventListener('click', () => {
    activityState.running = !activityState.running;
    activityToggle.textContent = activityState.running ? 'Pause' : 'Resume';
    activityStatus.dataset.state = activityState.running ? 'live' : 'paused';
    activityLabel.textContent = activityState.running ? 'Live' : 'Paused';
  });

  activityReset.addEventListener('click', () => {
    resetActivityFeed();
  });

    pasteLoadBtn.addEventListener('click', () => {
      const text = pasteArea.value.trim();
      if (!text) {
        showStatus('Paste YAML first', 'warn');
        return;
      }
      try {
        const parsed = yamlLib.load(text);
        applyLoadedConfig(parsed, 'pasted');
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

  document.getElementById('cb-add-group').addEventListener('click', addGroup);
  document.getElementById('cb-add-lobby').addEventListener('click', addLobby);

    inputs.baseCommand.addEventListener('input', () => {
      config['base-hub-command'] = inputs.baseCommand.value.trim();
      commitChange();
    });
    inputs.aliases.addEventListener('input', () => {
      config.aliases = parseList(inputs.aliases.value);
      commitChange();
    });
    inputs.hideOn.addEventListener('input', () => {
      config['hide-hub-command-on-lobby'] = inputs.hideOn.value;
      updateHideRegexStatus();
      commitChange();
    });
    hideTest.addEventListener('input', updateHideRegexStatus);
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
      commitChange();
    });
    inputs.debugPermission.addEventListener('input', () => {
      config.debug = config.debug || {};
      config.debug.permission = inputs.debugPermission.value;
      commitChange();
    });
    inputs.messageSuccess.addEventListener('input', () => {
      config.messages = config.messages || {};
      config.messages['success-message'] = inputs.messageSuccess.value;
      commitChange();
    });
    inputs.messageAlready.addEventListener('input', () => {
      config.messages = config.messages || {};
      config.messages['already-connected-message'] = inputs.messageAlready.value;
      commitChange();
    });
    inputs.messageProgress.addEventListener('input', () => {
      config.messages = config.messages || {};
      config.messages['connection-in-progress-message'] = inputs.messageProgress.value;
      commitChange();
    });
    inputs.messageDisconnected.addEventListener('input', () => {
      config.messages = config.messages || {};
      config.messages['server-disconnected-message'] = inputs.messageDisconnected.value;
      commitChange();
    });
    inputs.messageCancelled.addEventListener('input', () => {
      config.messages = config.messages || {};
      config.messages['connection-cancelled-message'] = inputs.messageCancelled.value;
      commitChange();
    });
    inputs.systemPlayersOnly.addEventListener('input', () => {
      config['system-messages'] = config['system-messages'] || {};
      config['system-messages']['players-only-command-message'] = inputs.systemPlayersOnly.value;
      commitChange();
    });
    inputs.systemNoLobby.addEventListener('input', () => {
      config['system-messages'] = config['system-messages'] || {};
      config['system-messages']['no-lobby-found-message'] = inputs.systemNoLobby.value;
      commitChange();
    });
    inputs.kickEnabled.addEventListener('change', () => {
      config['kick-message'] = config['kick-message'] || {};
      config['kick-message'].enabled = inputs.kickEnabled.checked;
      commitChange();
    });
    inputs.kickPrefix.addEventListener('input', () => {
      config['kick-message'] = config['kick-message'] || {};
      config['kick-message'].prefix = inputs.kickPrefix.value;
      commitChange();
    });
    inputs.kickSuffix.addEventListener('input', () => {
      config['kick-message'] = config['kick-message'] || {};
      config['kick-message'].suffix = inputs.kickSuffix.value;
      commitChange();
    });

    inputs.finderStart.addEventListener('input', () => {
      config.finder = config.finder || {};
      const value = Number.parseInt(inputs.finderStart.value, 10);
      config.finder['start-duration'] = Number.isFinite(value) ? value : 0;
      commitChange();
    });
    inputs.finderIncrement.addEventListener('input', () => {
      config.finder = config.finder || {};
      const value = Number.parseInt(inputs.finderIncrement.value, 10);
      config.finder['increment-duration'] = Number.isFinite(value) ? value : 0;
      commitChange();
    });
    inputs.finderMax.addEventListener('input', () => {
      config.finder = config.finder || {};
      const value = Number.parseInt(inputs.finderMax.value, 10);
      config.finder['max-duration'] = Number.isFinite(value) ? value : 0;
      commitChange();
    });
    inputs.finderRefresh.addEventListener('input', () => {
      config.finder = config.finder || {};
      const value = Number.parseInt(inputs.finderRefresh.value, 10);
      config.finder['refresh-interval-in-ticks'] = Number.isFinite(value) ? value : 0;
      commitChange();
    });
    inputs.dataCollectEnabled.addEventListener('change', () => {
      config['data-collection'] = config['data-collection'] || {};
      config['data-collection'].enabled = inputs.dataCollectEnabled.checked;
      commitChange();
    });
    inputs.dataCollectUuid.addEventListener('change', () => {
      config['data-collection'] = config['data-collection'] || {};
      config['data-collection']['include-uuid'] = inputs.dataCollectUuid.checked;
      commitChange();
    });
    inputs.dataCollectFile.addEventListener('input', () => {
      config['data-collection'] = config['data-collection'] || {};
      config['data-collection']['dump-file'] = inputs.dataCollectFile.value.trim();
      commitChange();
    });
    inputs.dataCollectInterval.addEventListener('input', () => {
      config['data-collection'] = config['data-collection'] || {};
      const value = Number.parseInt(inputs.dataCollectInterval.value, 10);
      config['data-collection']['dump-interval-minutes'] = Number.isFinite(value) ? value : 0;
      commitChange();
    });
    inputs.dataCollectUsers.addEventListener('input', () => {
      config['data-collection'] = config['data-collection'] || {};
      const value = Number.parseInt(inputs.dataCollectUsers.value, 10);
      config['data-collection']['max-users'] = Number.isFinite(value) ? value : 0;
      commitChange();
    });
    inputs.dataCollectServers.addEventListener('input', () => {
      config['data-collection'] = config['data-collection'] || {};
      const value = Number.parseInt(inputs.dataCollectServers.value, 10);
      config['data-collection']['max-servers'] = Number.isFinite(value) ? value : 0;
      commitChange();
    });

  regexInput.addEventListener('input', () => updateRegexResult(regexInput.value, regexTest.value, regexResult));
  regexTest.addEventListener('input', () => updateRegexResult(regexInput.value, regexTest.value, regexResult));

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
  renderZones();
  renderOfflinePlayers();
  enhanceHints();
  startActivityTimer();
}

  init();
});
</script>
