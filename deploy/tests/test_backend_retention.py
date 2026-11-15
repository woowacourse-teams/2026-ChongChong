import fcntl
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts/edge'))
import backend_retention as retention


def identity(index):
    digest = format(index, '064x')
    return {'release': digest, 'image': 'sha256:' + digest,
            'reference': 'team/app@sha256:' + digest}


class RetentionTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.release = self.root / 'releases/release.current'
        self.release.mkdir(parents=True)
        self.hash = hashlib.sha256()
        for name in retention.FILES:
            (self.release / name).write_bytes(b'config')
            self.hash.update(name.encode() + b'\0config\0')
        (self.root / 'current-backend-release').symlink_to(self.release)
        self.current = identity(5)
        self.current['release'] = self.hash.hexdigest()
        self.history = self.root / 'releases/successful-images.json'
        self.history.write_text(json.dumps([identity(i) for i in range(1, 5)]))
        self.images = {identity(i)['image']: {'RepoTags': ['team/app:v' + str(i)],
                      'RepoDigests': [identity(i)['reference']]} for i in range(1, 7)}
        self.containers = [self.current['image']]
        self.removed = []
        self.failed_removal = False

    def docker(self, *args):
        if args == ('inspect', 'chongchong-backend'):
            return json.dumps([{'Image': self.current['image'], 'State': {'Running': True},
                                'Config': {'Image': self.current['reference']}}])
        if args == ('ps', '-aq'):
            return 'c1 c2'
        if args[0] == 'inspect':
            return json.dumps([{'Image': image} for image in self.containers])
        if args[:2] == ('image', 'inspect'):
            key = next((key for key, value in self.images.items() if args[2] in value.get('RepoDigests', [])), args[2])
            return json.dumps([{**self.images[key], 'Id': key}])
        if args[:2] == ('image', 'ls'):
            return '\n'.join(self.images)
        if args[:2] == ('image', 'rm'):
            if self.failed_removal:
                raise RuntimeError('failure')
            self.removed.append(args[2])
            return ''
        raise AssertionError(args)

    def test_current_previous_two_and_all_container_images_are_preserved(self):
        self.containers.append(identity(1)['image'])  # 정지 컨테이너도 보호
        with patch.object(retention, 'docker', side_effect=self.docker):
            retention.retain(self.root, self.current, clean=True)
        self.assertEqual(self.removed, ['team/app:v2', 'team/app:v6'])
        self.assertEqual(len(json.loads(self.history.read_text())), 5)

    def test_explicit_rollback_reference_is_kept_even_outside_last_three(self):
        expected = {**self.current, 'protected_references': [identity(1)['reference']]}
        with patch.object(retention, 'docker', side_effect=self.docker):
            retention.retain(self.root, expected, clean=True)
        self.assertNotIn('team/app:v1', self.removed)

    def test_record_does_not_delete_or_count_duplicate_success_twice(self):
        with patch.object(retention, 'docker', side_effect=self.docker):
            retention.retain(self.root, self.current)
            retention.retain(self.root, self.current)
        self.assertEqual(self.removed, [])
        self.assertEqual(len(json.loads(self.history.read_text())), 5)
        self.assertEqual(self.history.stat().st_mode & 0o777, 0o600)

    def test_shared_other_repository_and_dangling_images_are_not_deleted(self):
        self.images[identity(1)['image']]['RepoTags'].append('other/app:keep')
        self.images[identity(2)['image']] = {'RepoTags': [], 'RepoDigests': []}
        with patch.object(retention, 'docker', side_effect=self.docker):
            retention.retain(self.root, self.current, clean=True)
        self.assertEqual(self.removed, ['team/app:v6'])

    def test_mismatched_release_corrupt_history_and_active_lock_fail_closed(self):
        with patch.object(retention, 'docker', side_effect=self.docker):
            with self.assertRaises(ValueError):
                retention.retain(self.root, identity(5), clean=True)
            self.history.write_text('{}')
            with self.assertRaises(ValueError):
                retention.retain(self.root, self.current, clean=True)
            with (self.root / 'releases/deploy.lock').open('a') as lock:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                with self.assertRaises(BlockingIOError):
                    retention.retain(self.root, self.current, clean=True)
        self.assertEqual(self.removed, [])

    def test_retention_allows_identity_reads_but_excludes_deploy_and_other_cleanup(self):
        observed = []

        def inspect_locks(*args):
            if args == ('inspect', 'chongchong-backend'):
                with (self.root / 'releases/deploy.lock').open('a') as reader:
                    fcntl.flock(reader, fcntl.LOCK_SH | fcntl.LOCK_NB)
                    observed.append('identity can read')
                with (self.root / 'releases/deploy.lock').open('a') as deployer:
                    with self.assertRaises(BlockingIOError):
                        fcntl.flock(deployer, fcntl.LOCK_EX | fcntl.LOCK_NB)
                with self.assertRaises(BlockingIOError):
                    retention.retain(self.root, self.current, clean=True)
            return self.docker(*args)

        with patch.object(retention, 'docker', side_effect=inspect_locks):
            retention.retain(self.root, self.current, clean=True)
        self.assertEqual(observed, ['identity can read'])

    def test_deletion_failure_preserves_success_history_for_retry(self):
        self.failed_removal = True
        with patch.object(retention, 'docker', side_effect=self.docker):
            with self.assertRaises(RuntimeError):
                retention.retain(self.root, self.current, clean=True)
        self.assertEqual(json.loads(self.history.read_text())[-1], self.current)
