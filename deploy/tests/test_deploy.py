import json
import os
import pathlib
import shutil
import subprocess
import tempfile
import unittest

DEPLOY = pathlib.Path(__file__).resolve().parents[1]
FAKE_DOCKER = '\n'.join([
    '#!/usr/bin/env python3',
    'import json, os, sys',
    'args = sys.argv[1:]',
    'with open(os.environ["CALL_LOG"], "a") as output: output.write(json.dumps(args)+"\\n")',
    'joined = " ".join(args)',
    'if args[0] == "inspect":',
    '    print("old:version" if ".Config.Image" in joined else "true 0 false" if "RestartCount" in joined else "METRICS_SCRAPE_INTERVAL=60s\\nMETRICS_SCRAPE_TIMEOUT=10s" if ".Config.Env" in joined else "true")',
    'if args[0] == "compose":',
    '    if "run" in args and os.environ.get("FAIL_AT") == "preflight": sys.exit(1)',
    '    if "up" in args and "backend" in args and os.environ.get("FAIL_AT") == "app" and "deploy-backups" not in joined: sys.exit(1)',
    '    if "up" in args and "metrics-collector" in args and os.environ.get("FAIL_AT") == "collector": sys.exit(1)',
])


class DeployTest(unittest.TestCase):
    def exercise(self, environment='dev', failure=''):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            shutil.copytree(DEPLOY, root / 'deploy')
            (root / '.env').write_text((DEPLOY / '.env.example').read_text().replace(
                'CLOUD_WATCH_APP_ENV=prod', f'CLOUD_WATCH_APP_ENV={environment}'))
            (root / 'deploy/image.env').write_text('BACKEND_IMAGE=new\nBACKEND_IMAGE_TAG=version\n')
            certs = root / 'certs/live/203-0-113-10.nip.io'
            certs.mkdir(parents=True)
            (certs / 'fullchain.pem').touch()
            (certs / 'privkey.pem').touch()
            backup = root / 'deploy-backups/previous/deploy'
            backup.mkdir(parents=True)
            shutil.copy(DEPLOY / 'docker-compose.yml', backup)
            (root / 'previous-deploy-dir').write_text(str(backup))
            binary = root / 'bin'
            binary.mkdir()
            (binary / 'docker').write_text(FAKE_DOCKER)
            (binary / 'docker').chmod(0o755)
            (binary / 'sleep').write_text('#!/bin/sh\nexit 0\n')
            (binary / 'sleep').chmod(0o755)
            env = {**os.environ, 'PATH': f'{binary}:{os.environ["PATH"]}',
                   'CHONGCHONG_APP_ROOT': str(root), 'CHONGCHONG_LETSENCRYPT_ROOT': str(root / 'certs'),
                   'CALL_LOG': str(root / 'calls'), 'FAIL_AT': failure, 'COMPOSE_PROFILES': ''}
            result = subprocess.run(['bash', str(root / 'deploy/scripts/deploy.sh')], env=env,
                                    capture_output=True, text=True)
            calls = [json.loads(line) for line in (root / 'calls').read_text().splitlines()]
            return result, calls

    def test_invalid_nginx_never_recreates_application(self):
        result, calls = self.exercise(failure='preflight')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any('up' in args for args in calls))

    def test_dev_recreates_collector_separately(self):
        result, calls = self.exercise()
        self.assertEqual(result.returncode, 0, result.stderr)
        collector = [args for args in calls if 'up' in args and 'metrics-collector' in args]
        self.assertEqual(len(collector), 1)
        self.assertIn('--no-deps', collector[0])
        self.assertIn('--force-recreate', collector[0])

    def test_prod_does_not_start_collector(self):
        result, calls = self.exercise(environment='prod')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(any('metrics-collector' in args for args in calls))

    def test_app_failure_uses_configuration_backup(self):
        result, calls = self.exercise(failure='app')
        self.assertNotEqual(result.returncode, 0)
        recreation = [args for args in calls if 'up' in args]
        self.assertEqual(len(recreation), 2)
        self.assertIn('deploy-backups', ' '.join(recreation[1]))

    def test_collector_failure_does_not_restart_application_again(self):
        result, calls = self.exercise(failure='collector')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(sum('up' in args and 'backend' in args for args in calls), 1)


if __name__ == '__main__':
    unittest.main()
