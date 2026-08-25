# Health Logger

An Android client for the **Slack to Sheet Health Logger** n8n workflow, so you can log
food, meds, poop and piss entries without opening Slack.

Slack still receives every confirmation, so it stays a complete audit trail.

---

## How it fits together

```
Android app ──POST──► App Webhook ──► Normalize App Input ──┐
                       (header auth)                        │
                                                            ├──► Parse Command ──► … ──► Google Sheet
Slack message ─────────────────────────────────────────────┘                              │
                                                                                          ├──► Slack confirmation
                                                                                          └──► Build App Response ──► app
```

The webhook path reuses your existing `Parse Command` node untouched. `Normalize App Input`
just translates the app's JSON into the `{ text: "!food coffee" }` shape the parser
already expects.

## One-time n8n setup

The workflow changes are already applied. Two things still need doing in the n8n UI:

1. **Check the Header Auth credential.** The `App Webhook` node uses your existing
   *"Header Auth account"* credential. Open it and note the header **name** and **value** —
   you'll type both into the app. If you'd rather not reuse that credential elsewhere,
   create a fresh one and swap it on the node.
2. **Copy the Production URL** from the `App Webhook` node. It'll look like
   `https://jadatoui.app.n8n.cloud/webhook/health-log`. The *Test* URL only listens while
   the editor is open — don't use that one.

Worth verifying with curl before you build anything:

```bash
curl -X POST https://jadatoui.app.n8n.cloud/webhook/health-log \
  -H 'Content-Type: application/json' \
  -H 'YOUR-HEADER-NAME: your-header-value' \
  -d '{"type":"food","entry":"test entry"}'
```

You should get back something like:

```json
{"ok":true,"action":"log","type":"food","range":"August'26!F6","value":"test entry",
 "message":"Logged (food) to August'26!F6: test entry"}
```

Then clear it with `-d '{"type":"undo"}'`.

## Request format

The webhook accepts either shape:

```jsonc
{ "type": "food", "entry": "flat white", "color": "brown" }  // structured
{ "text": "!food flat white color:brown" }                   // raw command
```

`type` is one of `food`, `poop`, `med`, `piss`, `undo`. `entry` is required for `food`,
optional for the rest. `color` is optional and must be one of red, brown, green, yellow,
blue, orange, grey.

## Building

1. Open the `HealthLogger` folder in Android Studio (Ladybug or newer).
2. Let it sync — it will download Gradle and generate the wrapper on first run.
3. Run on a device or emulator (minSdk 26).
4. Tap the gear icon and enter the webhook URL, header name and header value.

If you prefer the command line and have a Gradle install:

```bash
cd HealthLogger
gradle wrapper          # first time only
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

## Known limitation: timestamps

The workflow derives the timestamp and the food time-window row from `$now` on the
**server**, at the moment the request arrives. So there's deliberately no offline queue in
this version — a request that sat in a queue for three hours would be filed into the wrong
row.

If you want offline logging, the fix is to have the app send its own timestamp and change
`Parse Command` to prefer it over `$now`. That's a small edit, but it touches your working
parser, so I left it alone.

## Possible next steps

- **Quick Settings tile** — log coffee from the notification shade without opening the app.
- **Home screen widget** — a 2×1 grid of your most-used entries.
- **Recent entries** — a `GET` branch on the webhook that reads today's column back, so the
  app can show what's already logged.
