from datetime import datetime, timedelta, timezone
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts/edge'))
from remote_image_candidates import candidates, fetch_tags

NOW = datetime(2026, 11, 6, tzinfo=timezone.utc)


def tag(index, days, name=None, digest=None):
    return {'name': name or format(index, '040x'), 'digest': digest or 'sha256:' + format(index, '064x'),
            'tag_last_pushed': (NOW - timedelta(days=days)).isoformat()}


class CandidateTest(unittest.TestCase):
    def test_retention_union_and_protected_alias(self):
        tags = [tag(i, i * 10) for i in range(1, 16)]
        tags += [tag(15, 200, 'prod-current'), tag(14, 200, 'prod-rollback')]
        rows = candidates(tags, ['prod-current', 'prod-rollback'], NOW)
        self.assertEqual([r['tag'] for r in rows], [format(i, '040x') for i in (11, 12, 13)])

    def test_ninety_days_preserves_more_than_ten(self):
        tags = [tag(i, i) for i in range(1, 31)] + [tag(40, 300, 'dev-current')]
        self.assertEqual(candidates(tags, ['dev-current'], NOW), [])

    def test_ten_distinct_images_not_ten_aliases_and_unknown_tags_preserved(self):
        tags = [tag(i, 150 + i) for i in range(1, 13)]
        tags += [tag(100 + i, 100, digest=tags[0]['digest']) for i in range(10)]
        tags += [tag(12, 400, 'prod-current'), tag(11, 400, 'manual-keep')]
        self.assertEqual(candidates(tags, ['prod-current'], NOW), [])

    def test_unknown_metadata_missing_protection_and_duplicate_tags_fail_closed(self):
        for tags, required in (([tag(1, 1)], ['missing']), ([tag(1, 1)], []),
                               ([tag(1, 1), tag(1, 1)], ['prod-current']),
                               ([{'name': 'prod-current', 'tag_last_pushed': NOW.isoformat()}], ['prod-current'])):
            with self.subTest(tags=tags):
                with self.assertRaises(ValueError):
                    candidates(tags, required, NOW)

    def test_shared_platform_digest_is_protected(self):
        tags = [tag(i, 200 + i) for i in range(1, 15)]
        tags += [tag(20, 300, 'prod-current')]
        shared = 'sha256:' + 'f' * 64
        tags[-1]['images'] = [{'digest': shared}]
        tags[13]['images'] = [{'digest': shared}]
        rows = candidates(tags, ['prod-current'], NOW)
        self.assertNotIn(format(14, '040x'), [r['tag'] for r in rows])

    def test_pagination_and_cross_host_token_protection(self):
        base = 'https://hub.docker.com/v2/namespaces/team/repositories/app/tags'
        calls = []
        def get(url, token):
            calls.append((url, token))
            return {'results': [tag(len(calls), 1)], 'next': base + '?page=2' if len(calls) == 1 else None}
        self.assertEqual(len(fetch_tags('team/app', 'secret', get)), 2)
        def malicious(url, token):
            return {'results': [], 'next': 'https://evil.example/tags'}
        with self.assertRaises(ValueError):
            fetch_tags('team/app', 'secret', malicious)
        with self.assertRaises(ValueError):
            fetch_tags('team/app', 'secret', lambda url, token: {'results': [], 'next': url})
