import unittest

from assert_ops import AssertError, resolve_path, run_assert


class AssertOpsTest(unittest.TestCase):
    def test_resolve_dot_and_index(self):
        data = {"a": {"b": [{"c": 1}, {"c": 2}]}}
        ok, v = resolve_path(data, "a.b[1].c")
        self.assertTrue(ok)
        self.assertEqual(v, 2)

    def test_eq_and_contains(self):
        data = {"schemaVersion": 1, "ids": ["demo", "map"]}
        run_assert(data, {"path": "schemaVersion", "eq": 1})
        run_assert(data, {"path": "ids", "contains": "demo"})
        with self.assertRaises(AssertError):
            run_assert(data, {"path": "schemaVersion", "eq": 2})

    def test_exists(self):
        data = {"x": 1}
        run_assert(data, {"path": "x", "exists": True})
        run_assert(data, {"path": "y", "exists": False})


if __name__ == "__main__":
    unittest.main()
