import pathlib
import subprocess
import unittest

RUNTIME = pathlib.Path(__file__).resolve().parents[1] / 'scripts/legacy/runtime.sh'


class RuntimeTest(unittest.TestCase):
    def run_shell(self, script):
        return subprocess.run(['bash', '-c', f'source "{RUNTIME}"\n{script}'],
                              capture_output=True, text=True)

    def test_dev_uses_overlay(self):
        result = self.run_shell('docker() { printf "%s\\n" "$@"; }; CLOUD_WATCH_APP_ENV=dev; compose config --quiet')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('docker-compose.dev.yml', result.stdout)

    def test_prod_does_not_use_overlay(self):
        result = self.run_shell('docker() { printf "%s\\n" "$@"; }; CLOUD_WATCH_APP_ENV=prod; compose config --quiet')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn('docker-compose.dev.yml', result.stdout)

    def test_collector_is_required_for_dev_without_profile(self):
        result = self.run_shell('CLOUD_WATCH_APP_ENV=dev; docker() { return 1; }; verify_metrics_collector')
        self.assertNotEqual(result.returncode, 0)

    def test_prod_collector_is_opt_in(self):
        result = self.run_shell('CLOUD_WATCH_APP_ENV=prod; unset COMPOSE_PROFILES; docker() { return 1; }; verify_metrics_collector')
        self.assertEqual(result.returncode, 0)

    def test_healthy_collector_default_environment(self):
        result = self.run_shell('''
CLOUD_WATCH_APP_ENV=dev
docker() {
  case "$*" in
    *RestartCount*) printf 'true 0 false\\n' ;;
    *) printf 'METRICS_SCRAPE_INTERVAL=60s\\nMETRICS_SCRAPE_TIMEOUT=10s\\n' ;;
  esac
}
verify_metrics_collector
''')
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_restarting_collector_fails(self):
        result = self.run_shell('CLOUD_WATCH_APP_ENV=dev; docker() { echo "true 1 false"; }; verify_metrics_collector')
        self.assertNotEqual(result.returncode, 0)

    def test_old_collector_environment_fails(self):
        result = self.run_shell('''
CLOUD_WATCH_APP_ENV=dev
docker() {
  case "$*" in
    *RestartCount*) printf 'true 0 false\\n' ;;
    *) printf 'METRICS_SCRAPE_INTERVAL=15s\\nMETRICS_SCRAPE_TIMEOUT=10s\\n' ;;
  esac
}
verify_metrics_collector
''')
        self.assertNotEqual(result.returncode, 0)


if __name__ == '__main__':
    unittest.main()
