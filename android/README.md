# HTRAM Android resident monitor

Native Android companion for Honeywell HTRAM-V1-W.

## v0.1
- BLE scan for devices whose name starts with HTRAM
- foreground `connectedDevice` service
- persistent notification
- CO2 / temperature / humidity / battery polling every 5 seconds
- automatic reconnect
- Home / Udo awake / Udo sleeping / Custom alarm profiles
- configurable warning, alarm, delay and repeat interval
- sound + vibration alarms
- no HTRAM sensor-parameter writes; only the same BLE-mode, time-sync and realtime-read commands already used by the web project

The service must be started by the user from the visible app. Android shows a persistent notification while monitoring.
