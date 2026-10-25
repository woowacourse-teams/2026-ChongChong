import io
import json
import os
import pathlib
import shutil
import subprocess
import tarfile
import tempfile
import unittest

DEPLOY = pathlib.Path(__file__).resolve().parents[1]
FAKE = r'''#!/usr/bin/env python3
import json, os, pathlib, sys
name, args = pathlib.Path(sys.argv[0]).name, sys.argv[1:]
root = pathlib.Path(os.environ['CHONGCHONG_APP_ROOT'])
failure = os.environ.get('FAIL_AT', '')
with (root / 'calls').open('a') as f: f.write(json.dumps([name, *args]) + '\n')
if name == 'ssh':
    if failure == 'ssh': sys.exit(1)
    sys.stdin.buffer.read()
elif name == 'timeout':
    os.execvp(args[1], args[1:])
elif name == 'flock':
    if failure == 'locked': sys.exit(1)
elif name == 'docker':
    if failure == 'pull' and 'pull' in args: sys.exit(1)
    if failure == 'compose-config' and 'config' in args: sys.exit(1)
    if failure == 'up' and 'up' in args: sys.exit(1)
    if args[0] == 'login': sys.stdin.buffer.read()
    if args[0] == 'top':
        columns = args[args.index('-eo') + 1].split(',')
        if 'pid' not in columns:
            print("Couldn't find PID field in ps output", file=sys.stderr)
            sys.exit(1)
        print('PID COMMAND')
        if failure == 'draining': print('123 nginx: worker process is shutting down')
    if args[-1] == '-t':
        counter = root / 'syntax-count'
        n = int(counter.read_text()) + 1 if counter.exists() else 1
        counter.write_text(str(n))
        if failure == 'syntax' and n == 2: sys.exit(1)
        if failure == 'rollback-syntax' and n == 1: sys.exit(1)
    if args[-1] == 'reload':
        if failure == 'reload' and not (root / 'reload-failed').exists():
            (root / 'reload-failed').touch(); sys.exit(1)
elif name == 'curl':
    if '/readiness' in args[-1]:
        print('{"status":"DOWN"}' if failure == 'readiness' else '{"status":"UP"}')
    else:
        upstream = (root / 'edge-routing/upstream.conf').read_text().split('server ')[1].split(';')[0]
        if failure == 'mismatch' or (failure in ('verify', 'recovery') and upstream == '10.0.0.2:8080'):
            upstream = '10.0.0.99:8080'
        if failure == 'recovery' and (root / 'syntax-count').read_text() == '3': sys.exit(1)
        pathlib.Path(args[args.index('-D')+1]).write_text('HTTP/1.1 200 OK\r\nX-Deploy-Backend: '+upstream+'\r\n')
        print('{"status":"UP"}')
'''


class EdgeDeployTest(unittest.TestCase):
    def exercise(self, failure='', active='10.0.0.1', rollback=False, pending=False,
                 prepare=None, inspect_state=None):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            shutil.copytree(DEPLOY, root / 'deploy')
            (root / 'deploy/image.env').write_text('BACKEND_IMAGE=test/app\nBACKEND_IMAGE_TAG=new\n')
            (root / 'config').mkdir()
            (root / 'key').touch()
            (root / 'known_hosts').touch()
            (root / 'config/backend.env').write_text('WEB_PUSH_DELIVERY_CONCURRENCY=10\n')
            (root / 'config/edge.env').write_text(
                f'BACKEND_A_HOST=10.0.0.1\nBACKEND_B_HOST=10.0.0.2\nSSH_USER=ubuntu\n'
                f'SSH_KEY={root}/key\nSSH_KNOWN_HOSTS={root}/known_hosts\nSERVER_NAME=dev.example.com\n')
            (root / 'edge-routing').mkdir()
            original = f'upstream chongchong_backend {{ server {active}:8080; }}\n'
            (root / 'edge-routing/upstream.conf').write_text(original)
            (root / 'edge-state').mkdir()
            if rollback or pending:
                name = 'pending-upstream.conf' if pending else 'previous-upstream.conf'
                (root / 'edge-state' / name).write_text('upstream chongchong_backend { server 10.0.0.2:8080; }\n')
            if prepare:
                prepare(root)
            binary = root / 'bin'
            binary.mkdir()
            for command in ('docker', 'ssh', 'curl', 'timeout', 'flock', 'sleep'):
                path = binary / command
                path.write_text(FAKE)
                path.chmod(0o755)
            env = {**os.environ, 'CHONGCHONG_APP_ROOT': str(root), 'FAIL_AT': failure,
                   'PATH': f'{binary}:{os.environ["PATH"]}'}
            script = 'edge-rollback.sh' if rollback else 'edge-deploy.sh'
            result = subprocess.run(['bash', str(root / 'deploy/scripts/edge' / script)],
                                    env=env, capture_output=True, text=True)
            calls = [json.loads(line) for line in (root / 'calls').read_text().splitlines()]
            routing = (root / 'edge-routing/upstream.conf').read_text()
            if inspect_state:
                inspect_state(root)
            return result, calls, routing, original, (root / 'edge-state/pending-upstream.conf').exists()

    def seed_snapshots(self, root):
        state = root / 'edge-state'
        for index in range(6):
            snapshot = state / f'deployment.old{index}'
            snapshot.mkdir()
            (snapshot / 'backend.env').write_text('TEST_SECRET=old\n')
            os.utime(snapshot, (index + 1, index + 1))
        (state / 'last-successful-deployment').write_text(str(state / 'deployment.old0') + '\n')
        with (root / 'config/edge.env').open('a') as config:
            config.write('EDGE_DEPLOYMENT_RETENTION_COUNT=3\n')
        (state / 'unrelated').mkdir()

    def assert_retained_snapshots(self, root):
        state = root / 'edge-state'
        snapshots = [path for path in state.glob('deployment.*') if not path.is_symlink()]
        self.assertEqual(len(snapshots), 3)
        current = pathlib.Path((state / 'last-successful-deployment').read_text().strip())
        self.assertIn(current, snapshots)
        self.assertTrue((state / 'unrelated').is_dir())

    def test_snapshots_are_bounded_after_success_and_pre_switch_failure(self):
        for failure in ('', 'ssh', 'readiness', 'mismatch'):
            with self.subTest(failure=failure):
                result, _, _, _, _ = self.exercise(
                    failure, prepare=self.seed_snapshots, inspect_state=self.assert_retained_snapshots)
                self.assertEqual(result.returncode == 0, failure == '')

    def test_retention_one_preserves_the_older_successful_snapshot(self):
        def prepare(root):
            self.seed_snapshots(root)
            with (root / 'config/edge.env').open('a') as config:
                config.write('EDGE_DEPLOYMENT_RETENTION_COUNT=1\n')

        def inspect_state(root):
            self.assertEqual(list((root / 'edge-state').glob('deployment.*')),
                             [root / 'edge-state/deployment.old0'])

        result, _, _, _, _ = self.exercise('ssh', prepare=prepare, inspect_state=inspect_state)
        self.assertNotEqual(result.returncode, 0)

    def test_cleanup_does_not_follow_symlinked_directories(self):
        def prepare(root):
            self.seed_snapshots(root)
            outside = root / 'outside-snapshot'
            outside.mkdir()
            (outside / 'backend.env').write_text('TEST_SECRET=outside\n')
            (root / 'edge-state/deployment.link').symlink_to(outside)

        def inspect_state(root):
            self.assert_retained_snapshots(root)
            self.assertTrue((root / 'outside-snapshot/backend.env').is_file())
            self.assertTrue((root / 'edge-state/deployment.link').is_symlink())

        result, _, _, _, _ = self.exercise(prepare=prepare, inspect_state=inspect_state)
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_invalid_retention_is_rejected_before_deployment(self):
        for count in ('0', '-1', 'invalid'):
            with self.subTest(count=count):
                def prepare(root):
                    with (root / 'config/edge.env').open('a') as config:
                        config.write(f'EDGE_DEPLOYMENT_RETENTION_COUNT={count}\n')

                result, calls, routing, original, _ = self.exercise(prepare=prepare)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(routing, original)
                self.assertFalse(any(call[0] == 'ssh' for call in calls))

    def test_pending_recovery_preserves_every_snapshot(self):
        def inspect_state(root):
            self.assertEqual(len(list((root / 'edge-state').glob('deployment.*'))), 7)
            for index in range(6):
                self.assertTrue((root / f'edge-state/deployment.old{index}/backend.env').is_file())

        result, _, _, _, pending = self.exercise(
            'recovery', prepare=self.seed_snapshots, inspect_state=inspect_state)
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue(pending)

    def test_successful_manual_recovery_prunes_old_snapshots(self):
        result, _, _, _, pending = self.exercise(
            rollback=True, pending=True, prepare=self.seed_snapshots,
            inspect_state=self.assert_retained_snapshots)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(pending)

    def test_manual_rollback_validation_failure_restores_upstream_file(self):
        for failure in ('rollback-syntax', 'reload'):
            with self.subTest(failure=failure):
                result, _, routing, original, pending = self.exercise(failure, rollback=True)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(routing, original)
                self.assertTrue(pending)

    def test_only_inactive_backend_is_deployed(self):
        for active, target in [('10.0.0.1', '10.0.0.2'), ('10.0.0.2', '10.0.0.1')]:
            with self.subTest(active=active):
                result, calls, routing, _, pending = self.exercise(active=active)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertIn(target, routing)
                ssh = [call for call in calls if call[0] == 'ssh']
                self.assertTrue(ssh)
                self.assertTrue(all(f'ubuntu@{target}' in call for call in ssh))
                self.assertFalse(any('stop' in call for call in calls))
                self.assertFalse(pending)

    def test_failures_before_switch_preserve_routing(self):
        for failure in ('ssh', 'readiness', 'mismatch', 'draining', 'locked'):
            with self.subTest(failure=failure):
                result, calls, routing, original, _ = self.exercise(failure)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(routing, original)
                self.assertFalse(any('reload' in call for call in calls))
                if failure in ('mismatch', 'draining', 'locked'):
                    self.assertFalse(any(call[0] == 'ssh' for call in calls))

    def test_switch_failures_restore_previous_routing(self):
        for failure in ('syntax', 'reload', 'verify'):
            with self.subTest(failure=failure):
                result, calls, routing, original, pending = self.exercise(failure)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(routing, original)
                self.assertIn('Restored previous backend routing', result.stderr)
                self.assertFalse(pending)

    def test_unverified_recovery_keeps_pending_marker(self):
        result, _, _, _, pending = self.exercise('recovery')
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue(pending)

    def test_interrupted_switch_blocks_new_deployment(self):
        result, calls, routing, original, pending = self.exercise(pending=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(routing, original)
        self.assertFalse(any(call[0] == 'ssh' for call in calls))
        self.assertTrue(pending)

    def test_manual_rollback_does_not_recreate_application(self):
        for pending in (False, True):
            result, calls, routing, _, remains = self.exercise(rollback=True, pending=pending)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn('10.0.0.2', routing)
            self.assertFalse(any(call[0] == 'ssh' or 'up' in call for call in calls))
            self.assertFalse(remains)

    def test_unhealthy_previous_backend_is_not_selected(self):
        result, _, routing, original, _ = self.exercise('readiness', rollback=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(routing, original)


class BackendReleaseTest(unittest.TestCase):
    def exercise(self, failure='', prepare=None, inspect_state=None):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            if prepare:
                prepare(root)
            binary = root / 'bin'
            binary.mkdir()
            for command in ('docker', 'flock'):
                (binary / command).write_text(FAKE)
                (binary / command).chmod(0o755)
            bundle = io.BytesIO()
            files = {
                'backend.env': 'DOCKERHUB_USERNAME=test\nDOCKERHUB_TOKEN=private-test-value\nDB_URL=test\nDB_USERNAME=test\nDB_PASSWORD=private-test-value\n',
                'image.env': 'BACKEND_IMAGE=test/app\nBACKEND_IMAGE_TAG=test\n',
                'docker-compose.yml': (DEPLOY / 'config/docker-compose.yml').read_text(),
                'docker-compose.backend.yml': (DEPLOY / 'config/docker-compose.backend.yml').read_text(),
            }
            with tarfile.open(fileobj=bundle, mode='w') as archive:
                for name, contents in files.items():
                    data = contents.encode()
                    info = tarfile.TarInfo(name)
                    info.size = len(data)
                    archive.addfile(info, io.BytesIO(data))
            result = subprocess.run(['bash', str(DEPLOY / 'scripts/backend/backend-release.sh'), 'b', '10.0.0.2'],
                                    input=bundle.getvalue(), capture_output=True,
                                    env={**os.environ, 'CHONGCHONG_APP_ROOT': str(root), 'FAIL_AT': failure,
                                         'PATH': f'{binary}:{os.environ["PATH"]}'})
            calls = [json.loads(line) for line in (root / 'calls').read_text().splitlines()]
            self.assertNotIn(b'private-test-value', result.stdout + result.stderr)
            self.assertNotIn('private-test-value', json.dumps(calls))
            for path in (root / 'releases').glob('*/backend.env'):
                self.assertEqual(path.stat().st_mode & 0o777, 0o600)
            if inspect_state:
                inspect_state(root)
            return result, calls, (root / 'current-backend-release').is_symlink()

    def test_failed_candidate_is_removed_without_removing_current_release(self):
        def prepare(root):
            current = root / 'releases/release.current'
            current.mkdir(parents=True)
            (current / 'backend.env').write_text('TEST_SECRET=current\n')
            (current / 'backend.env').chmod(0o600)
            (root / 'current-backend-release').symlink_to(current)

        def inspect_state(root):
            self.assertEqual(list((root / 'releases').glob('release.*')), [root / 'releases/release.current'])
            self.assertTrue((root / 'current-backend-release/backend.env').is_file())

        for failure in ('compose-config', 'pull', 'up'):
            with self.subTest(failure=failure):
                result, _, current = self.exercise(failure, prepare=prepare, inspect_state=inspect_state)
                self.assertNotEqual(result.returncode, 0)
                self.assertTrue(current)

    def test_pull_or_config_failure_preserves_existing_container(self):
        for failure in ('pull', 'compose-config', 'locked'):
            with self.subTest(failure=failure):
                result, calls, current = self.exercise(failure)
                self.assertNotEqual(result.returncode, 0)
                self.assertFalse(any('rm' in call or 'up' in call or 'stop' in call for call in calls))
                self.assertFalse(current)

    def test_starts_only_backend_and_records_release(self):
        result, calls, current = self.exercise()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(current)
        starts = [call for call in calls if 'up' in call]
        self.assertEqual(len(starts), 1)
        self.assertEqual(starts[0][-4:], ['up', '-d', '--no-deps', 'backend'])


if __name__ == '__main__':
    unittest.main()
