import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const globalBackup=readFileSync('supabase/functions/google-drive-backup/index.ts','utf8');
const health=readFileSync('supabase/migrations/20260907194500_add_recovery_health_monitor.sql','utf8');
const ui=readFileSync('recovery-health.js','utf8');
const index=readFileSync('index.html','utf8');
const has=(s,n)=>assert.ok(s.includes(n),`Missing ${n}`);

test('global disaster recovery backup remains backup-only',()=>has(globalBackup,"Restore is deliberately not available from this function"));
test('global backup is downloaded and hash-verified after upload',()=>{has(globalBackup,'verifyUploadedBackup');has(globalBackup,"phase = 'recovery-test'");has(globalBackup,'backup digest mismatch')});
test('recovery monitor watches stale backups, failures and repeated sync conflicts',()=>{has(health,"'stale_backup'");has(health,"'backup_failure'");has(health,"'sync_conflict'");has(health,"morley-recovery-health-hourly")});
test('monitor has no repair or restore authority',()=>{assert.ok(!health.includes('guardian_incidents'));assert.ok(!ui.includes("action:'restore'"))});
test('Nova Guardian recovery monitor is OTA loaded',()=>{has(index,"'recovery-health.js?v=1'");has(ui,'Nova + Guardian Recovery Monitor')});
